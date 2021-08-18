package com.cece.community.dao;

import com.cece.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //userId == 0表示首页，全部查询，否则就查询相关用户
//    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);


    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    // 插入帖子
    int insertDiscussPost(DiscussPost discussPost);

    //选取帖子
    DiscussPost selectDiscussPostById(int id);

    // 更新评论的数量
    int updateCommentCount(int id, int commentCount);

    // 修改类型 0 普通 1 置顶
    int updateType(int id, int type);
    // 修改状态 0 普通， 1 加精， 2 黑名单
    int updateStatus(int id, int status);

    int updateScore(int id, double score);

}

