package com.lxr.iot.bootstrap;

import com.lxr.iot.bootstrap.Bean.SendMqttMessage;
import com.lxr.iot.util.MessageId;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 操作api 处理主动发送请求
 *
 * @author lxr
 * @create 2018-01-10 9:36
 **/
@Slf4j
public class MqttApi {




    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    protected  void pubMessage(Channel channel, SendMqttMessage mqttMessage){
        log.info("成功发送消息:"+new String(mqttMessage.getPayload()));
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,mqttMessage.isDup(), MqttQoS.valueOf(mqttMessage.getQos()),mqttMessage.isRetained(),0);
        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(mqttMessage.getTopic(),mqttMessage.getMessageId());
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader,mqttPublishVariableHeader, Unpooled.wrappedBuffer(mqttMessage.getPayload()));
        channel.writeAndFlush(mqttPublishMessage);

    }

    protected  int  subMessage(Channel channel, List<MqttTopicSubscription> mqttTopicSubscriptions){
        int messageId = MessageId.messageId();
        MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(mqttTopicSubscriptions);
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE,false, MqttQoS.AT_LEAST_ONCE,false,0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader =MqttMessageIdVariableHeader.from(messageId);
        MqttSubscribeMessage mqttSubscribeMessage = new MqttSubscribeMessage(mqttFixedHeader,mqttMessageIdVariableHeader,mqttSubscribePayload);
        channel.writeAndFlush(mqttSubscribeMessage);
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            subMessage(channel, mqttTopicSubscriptions);
        }, 10, 10, TimeUnit.SECONDS);
        channel.attr(getKey(Integer.toString(messageId))).setIfAbsent(scheduledFuture);
        return messageId;

    }

    private AttributeKey<ScheduledFuture<?>> getKey(String id){
       return   AttributeKey.valueOf(id);
    }




}
