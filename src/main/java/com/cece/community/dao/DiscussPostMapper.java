package com.cece.community.dao;

import com.cece.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //userId == 0表示首页，全部查询，否则就查询相关用户
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    // 插入帖子
    int insertDiscussPost(DiscussPost discussPost);

    //选取帖子
    DiscussPost selectDiscussPostById(int id);

    // 更新评论的数量
    int updateCommentCount(int id, int commentCount);

}

