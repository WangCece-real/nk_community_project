package com.cece.community.service;


import com.cece.community.entity.User;
import com.cece.community.util.CommunityConstant;
import com.cece.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId) {
        // 关注和被关注是同一事务，所以要进行事务处理
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                // 用有序集合来存储。 set： 实体ID 当前时间，用来排序
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    /**
     * 取消关注
     * @param userId “我”
     * @param entityType 取消关注的目标类型
     * @param entityId 取消关注的目标ID
     */
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }

    /** 查询关注的实体的数量
     *
     * @param userId “我”
     * @param entityType 类别
     * @return
     */
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        // 统计数量
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /** 查询实体的粉丝的数量
     *
     * @param entityType 实体类别
     * @param entityId 实体的id
     * @return
     */
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /** 查询当前用户是否已关注该实体
     *
     * @param userId “我”
     * @param entityType 实体类型
     * @param entityId 实体id
     * @return
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        // 通过查分数来实现查找是否关注，查到了就是已经关注，null表示未关注
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /** 查询某用户关注的人
     *
     * @param userId 人
     * @param offset 分页的条件
     * @param limit 每页多少
     * @return
     */
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        // 从大到小查，倒序方式
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }
        // map中 user ： 关注时间
        // 把Id 转换成更详细的用户数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            // 通过id转成User
            User user = userService.findUserById(targetId);
            map.put("user", user);
            // 关注的时间
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

    /** 查询某用户的粉丝
     *
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

}
