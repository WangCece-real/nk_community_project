package com.cece.community.controller;

import com.cece.community.entity.Event;
import com.cece.community.entity.User;
import com.cece.community.event.EventProducer;
import com.cece.community.service.LikeService;
import com.cece.community.util.CommunityConstant;
import com.cece.community.util.CommunityUtil;
import com.cece.community.util.HostHolder;
import com.cece.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;



@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 处理点赞操作的异步请求
     * @param entityType
     * @param entityId
     * @param entityUserId
     * @return
     */
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();

        // 点赞操作
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 实体点赞的数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 触发点赞事件 如果是取消点赞就不需要触发事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    // 设置被点赞的用户
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }
        // 点赞的是帖子才会参与帖子分数计算
        if(entityType == ENTITY_TYPE_POST) {
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }
        return CommunityUtil.getJSONString(0, null, map);
    }

}
