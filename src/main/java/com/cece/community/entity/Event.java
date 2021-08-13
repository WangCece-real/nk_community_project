package com.cece.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {

    // 主题，也就是事件的分类
    private String topic;
    // 谁操作事件
    private int userId;
    // 操作的对象
    private int entityType;
    private int entityId;
    // 实体的作者
    private int entityUserId;
    //额外的数据存入data， map中，便于扩展
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    // 修改set方法，可以进行链式操作
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    // 外界传入key和value
    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
