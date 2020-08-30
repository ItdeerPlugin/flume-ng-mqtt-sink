package org.apache.flume.sink.mqtt;

import java.util.UUID;

/**
 * Description :
 * PackageName : org.apache.flume.sink.mqtt
 * ProjectName : flume-parent
 * CreatorName : itdeer.cn
 * CreateTime : 2020/8/29/9:52
 */
public class MqttSinkConstants {

    public static final String HOST = "host";
    public static final String TOPIC = "topic";

    public static final String QOS = "qos";
    public static final String BATCH_SIZE = "batchSize";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String IF_SESSION_CLEAN = "cleanSession";
    public static final String CONNECTION_TIMEOUT = "connectionTimeout";
    public static final String KEEP_ALIVE_INTERVAL = "keepAliveInterval";

    public static final String RETRY_CONNECTION = "retryConnection";

    public static final Integer DEFAULT_QOS = 1;

    public static final Integer DEFAULTBATCH_SIZE = 1;
    public static final Boolean DEFAULT_IF_SESSION_CLEAN = false;
    public static final Integer DEFAULT_CONNECTION_TIMEOUT = 30;
    public static final Integer DEFAULT_KEEP_ALIVE_INTERVAL = 60;

    public static final Boolean DEFAULT_RETRY_CONNECTION = false;

    public static String getUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }



}
