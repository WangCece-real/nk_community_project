package com.cece.community.event;

import com.alibaba.fastjson.JSONObject;
import com.cece.community.entity.Event;
import com.cece.community.entity.Message;
import com.cece.community.service.MessageService;
import com.cece.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    /**
     * 监听三个主题 评论，点赞，关注
     * @param record 用来接收监听的数据
     */
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 把消息的JSON 恢复成Event类
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        // 发送站内通知， 就是往message表中插入数据，假设系统为1，
        // message中的conversation_id就用来存储主题
        // content 存储字符串
        Message message = new Message();
        // from 是系统用户 1
        message.setFromId(SYSTEM_USER_ID);
        // to 实体的拥有者
        message.setToId(event.getEntityUserId());
        // conversation_id 就是存着主题
        message.setConversationId(event.getTopic());
        // 设置消息创建时间
        message.setCreateTime(new Date());

        //content存储的是消息的需要的一些信息
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        // 把event中的map,存到content中
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        // 存入数据库
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }
}
