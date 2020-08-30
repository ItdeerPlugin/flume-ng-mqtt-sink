flume-ng-mqtt-sink
================

本项目是对Apache Flume Sink的扩展，支持MQTT服务器为输出源，基于Flume1.6.0版本扩展

编译打包
----------

```shell
$ mvn clean package
```

部署
----------

> 复制flume-ng-mqtt-sink-<version>.jar到flume插件目录

> lib目录放自定义组件的jar包 libext目录下放自定义组件的依赖包

```shell
  $ mkdir -p $FLUME_HOME/plugins.d/mqtt-sink/lib 
  $ mkdir -p $FLUME_HOME/plugins.d/mqtt-sink/libext
  $ cp flume-ng-mqtt-sink-<version>.jar $FLUME_HOME/plugins.d/mqtt-sink/lib/
  $ cp org.eclipse.paho.client.mqttv3-1.2.4.jar $FLUME_HOME/plugins.d/mqtt-sink/libext/
```

Sink说明
----------

属性名称 | 是否必须 | 默认值 | 说明 | 示例
-- | -- | -- | -- | -- |
<b>type</b> | 是 |  | 自定义Sink类型 |  org.apache.flume.sink.mqtt.MqttSink
<b>host</b> | 是 |  | MQTT主机 |  tcp://127.0.0.1:1883
<b>topic</b> | 是 |  | 订阅主题 |  demo
qos | 否 | 1 | 确保消息被传到，可能会传多次 |  1
batchSize | 否 | 1 | 批大小 |  100
cleanSession | 否 | false | 设置是否清空session  |  false
connectionTimeout | 否 | 30 | 设置连接超时时间 |  30
keepAliveInterval | 否 | 60 | 设置会话心跳时间 |  60
username | 否 |   | 连接用户名 |  demo
password | 否 |   | 连接密码 |  demo
retryConnection | 否 | false  | 是否重连Mqtt服务器 | true


配置示例
--------------------

```properties
# 配置文件名称为 mqtt.conf
# 指定Sink的类型

agent.sources = log-file
agent.channels = memory-channel
agent.sinks = mqtt

agent.sources.log-file.type = exec
agent.sources.log-file.command = tail -f ./a.log
agent.sources.log-file.shell = /bin/sh -c

agent.channels.memory-channel.type = memory

agent.sinks.mqtt.type = org.apache.flume.sink.mqtt.MqttSink
agent.sinks.mqtt.host = tcp://127.0.0.1:1883
agent.sinks.mqtt.topic = demo
agent.sinks.mqtt.qos = 1
agent.sinks.mqtt.batchSize = 1000
agent.sinks.mqtt.cleanSession = true
agent.sinks.mqtt.connectionTimeout = 10
agent.sinks.mqtt.keepAliveInterval = 100
agent.sinks.mqtt.username = demo
agent.sinks.mqtt.password = demo
agent.sinks.mqtt.retryConnection = true

agent.sources.log-file.channels = memory-channel
agent.sinks.mqtt.channel = memory-channel
```

启动任务
--------------------

```shell
flume-ng agent --name agent --conf $FLUME_HOME/conf --conf-file /opt/mqtt.conf -Dflume.root.logger=INFO,console

--name      : 指定的任务名称，和配置中的名称一致
--conf      : flume的配置
--conf-file : 自定义的配置文件
```