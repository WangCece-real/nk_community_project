package com.cece.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    // 某一个实体 的赞（帖子，评论，回复）
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    // 某一个用户的赞
    private static final String PREFIX_USER_LIKE = "like:user";
    // 关注的目标
    private static final String PREFIX_FOLLOWEE = "followee";
    // 粉丝
    private static final String PREFIX_FOLLOWER = "follower";

    // 验证码
    private static final String PREFIX_KAPTCHA = "kaptcha";

    // 登录凭证
    private static final String PREFIX_TICKET = "ticket";

    // 存储用户
    private static final String PREFIX_USER = "user";

    // uv：访问人数
    private static final String PREFIX_UV = "uv";
    // dau: 日活用户
    private static final String PREFIX_DAU = "dau";

    // 帖子
    private static final String PREFIX_POST = "post";



    /**
     * 某个实体的赞 用set来存储
     * @param entityType 类型？帖子？评论？
     * @param entityId id
     * @return
     */
    // like:entity:entityType:entityId -> set(userId)
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    // 某个用户的赞
    // like:user:userId -> int
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    /** 某个用户关注的实体
     * 存储在有序集合中
     * @param userId 谁关注
     * @param entityType 关注的类型
     * @return
     */
    // followee:userId:entityType -> zset(entityId,now)
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /** 某个实体拥有的粉丝
     * 也要存储在有序集合中
     * @param entityType
     * @param entityId
     * @return
     */
    // follower:entityType:entityId -> zset(userId,now)
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    /** 登录验证码
     *  KEY 的组成：kaptcha:临时凭证
     * @param owner 用户的临时凭证
     * @return
     */
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    // 登录的凭证
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    // 用户
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    // 单日UV
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    // 区间UV
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    // 单日活跃用户
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    // 区间活跃用户
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    // 用于计算帖子分数，存的是帖子id
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }
}
