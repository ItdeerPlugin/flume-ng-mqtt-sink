package org.apache.flume.sink.mqtt;

import com.google.common.base.Throwables;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.ConfigurationException;
import org.apache.flume.sink.AbstractSink;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Description :
 * PackageName : org.apache.flume.sink.mqtt
 * ProjectName : flume-parent
 * CreatorName : itdeer.cn
 * CreateTime : 2020/8/29/9:52
 */
public class MqttSink extends AbstractSink implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(MqttSink.class);

    private String topic;
    private String host;
    private String clientId = MqttSinkConstants.getUuid();

    private Integer qos;
    private Integer batchSize;

    private Integer keepAliveInterval;
    private Boolean retryConnection;

    private Boolean cleanSession;
    private Integer connectionTimeout;

    private String username;
    private String password;

    private MqttConnectOptions mqttConnectOptions = null;
    private MqttClient mqttClient = null;
    private MemoryPersistence memoryPersistence = null;

    private Context context;

    private List<MqttMessage> messageList;


    /**
     * Process business
     *
     * @return
     */
    @Override
    public Status process() throws EventDeliveryException {

        Status result = Status.READY;
        Channel channel = getChannel();
        Transaction transaction = null;
        Event event = null;

        try {
            long processedEvents = 0;

            transaction = channel.getTransaction();
            transaction.begin();

            messageList.clear();
            for (; processedEvents < batchSize; processedEvents += 1) {
                event = channel.take();

                if (event == null) {
                    break;
                }

                byte[] eventBody = event.getBody();

                MqttMessage data = new MqttMessage();
                data.setQos(qos);
                data.setPayload(eventBody);

                messageList.add(data);
            }

            if (processedEvents > 0 && null != mqttClient && mqttClient.isConnected()) {
                for (MqttMessage data : messageList) {
                    mqttClient.getTopic(topic).publish(data);
                }
            } else {
                if (retryConnection && !mqttClient.isConnected()) {
                    reConnect();
                }
            }

            transaction.commit();

        } catch (Exception ex) {
            String errorMsg = "Failed to publish events";
            log.error(errorMsg, ex);
            result = Status.BACKOFF;
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception e) {
                    log.error("Transaction rollback failed", e);
                    throw Throwables.propagate(e);
                }
            }
            throw new EventDeliveryException(errorMsg, ex);
        } finally {
            if (transaction != null) {
                transaction.close();
            }
            if (!messageList.isEmpty()) {
                messageList.clear();
            }
        }

        return result;
    }

    /**
     * Mqtt configure
     *
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;

        host = context.getString(MqttSinkConstants.HOST);
        if (host == null) {
            throw new ConfigurationException("Mqtt host must be specified.");
        }

        topic = context.getString(MqttSinkConstants.TOPIC);
        if (topic == null) {
            throw new ConfigurationException("Mqtt topic must be specified.");
        }

        qos = context.getInteger(MqttSinkConstants.QOS, MqttSinkConstants.DEFAULT_QOS);
        batchSize = context.getInteger(MqttSinkConstants.BATCH_SIZE, MqttSinkConstants.DEFAULTBATCH_SIZE);

        cleanSession = context.getBoolean(MqttSinkConstants.IF_SESSION_CLEAN, MqttSinkConstants.DEFAULT_IF_SESSION_CLEAN);
        connectionTimeout = context.getInteger(MqttSinkConstants.CONNECTION_TIMEOUT, MqttSinkConstants.DEFAULT_CONNECTION_TIMEOUT);
        keepAliveInterval = context.getInteger(MqttSinkConstants.KEEP_ALIVE_INTERVAL, MqttSinkConstants.DEFAULT_KEEP_ALIVE_INTERVAL);

        username = context.getString(MqttSinkConstants.USERNAME);
        password = context.getString(MqttSinkConstants.PASSWORD);

        retryConnection = context.getBoolean(MqttSinkConstants.RETRY_CONNECTION, MqttSinkConstants.DEFAULT_RETRY_CONNECTION);

        messageList = new ArrayList<MqttMessage>(batchSize);
    }

    /**
     * Start Job
     */
    @Override
    public synchronized void start() {
        log.info("Starting mqtt sink job ......");

        mqttConnectOptions = new MqttConnectOptions();

        mqttConnectOptions.setAutomaticReconnect(retryConnection);
        mqttConnectOptions.setCleanSession(cleanSession);
        mqttConnectOptions.setConnectionTimeout(connectionTimeout);
        mqttConnectOptions.setKeepAliveInterval(keepAliveInterval);

        if (null != username) {
            mqttConnectOptions.setUserName(username);
        }
        if (null != password) {
            mqttConnectOptions.setPassword(password.toCharArray());
        }

        memoryPersistence = new MemoryPersistence();

        if (null != memoryPersistence && null != clientId) {
            try {
                mqttClient = new MqttClient(host, clientId, memoryPersistence);
            } catch (MqttException e) {
                throw new ConfigurationException("Mqtt host config error and error message : {}", e.fillInStackTrace());
            }
        }

        if (null != mqttClient && !mqttClient.isConnected()) {
            mqttClient.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MqttClient disconnect call back retry connect......");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            try {
                mqttClient.connect(mqttConnectOptions);
            } catch (MqttException e) {
                log.error("Get the MQTT connection exception, exception information is : {}", e.fillInStackTrace());
                reConnect();
            }
        }
    }

    /**
     * 重连
     */
    public void reConnect() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.error("Retry Get the MQTT Thread sleep exception, exception information is : {}", e.fillInStackTrace());
        }

        if (null != mqttClient && !mqttClient.isConnected() && null != mqttConnectOptions) {
            try {
                mqttClient.connect(mqttConnectOptions);
            } catch (MqttException e) {
                log.error("Retry Get the MQTT connection exception, exception information is : {}", e.fillInStackTrace());
            }
        } else {
            start();
        }

    }

    /**
     * close source
     */
    @Override
    public synchronized void stop() {

        if (null != mqttClient && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
            } catch (MqttException e) {
                log.error("mqttClient close an error occurs : {}", e.fillInStackTrace());
            }
        } else {
            try {
                mqttClient.close();
            } catch (MqttException e) {
                log.error("mqttClient close an error occurs : {}", e.fillInStackTrace());
            }
        }

        if (mqttConnectOptions != null) {
            mqttConnectOptions = null;
        }

        if (memoryPersistence != null) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                log.error("memoryPersistence close an error occurs : {}", e.fillInStackTrace());
            }
        }

        log.info("Mqtt Sink {} stopped success.", getName());
        super.stop();
    }
}
