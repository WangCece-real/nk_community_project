package com.cece.community.service;

import com.cece.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 点赞操作
     * @param userId 谁点赞
     * @param entityType 点赞的实体
     * @param entityId 实体id 用来记录实体被点了多少赞
     * @param entityUserId 实体属于谁 用来记录“我”，用户收到多少赞
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                // 查询要放在事务之外
                // 检查userid在不在集合中，在集合中就表示点过赞了，再点就要取消
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                operations.multi();

                if (isMember) {
                    // 点过赞了再点就删除
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                }

                return operations.exec();
            }
        });
    }

    /** 查询某实体点赞的数量
     *
     * @param entityType 实体类型
     * @param entityId 实体id
     * @return
     */
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /** 查询某人对某实体的点赞状态
     *  返回1 表示点赞，0 表示没有点赞
     * @param userId 用户id
     * @param entityType
     * @param entityId
     * @return
     */
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    /** 查询某个用户获得的赞
     *
     * @param userId
     * @return
     */
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        // 默认得到的是Object，
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        // 如果是null表示获取不到， 则显示0， 否则就是显示查到的数
        return count == null ? 0 : count;
    }

}
