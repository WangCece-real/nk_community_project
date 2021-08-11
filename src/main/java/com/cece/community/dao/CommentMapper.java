package com.cece.community.dao;

import com.cece.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    //

    /**
     * 根据实体来查询评论
     * @param entityType 实体类型
     * @param entityId 实体id
     * @param offset 偏移-分页用
     * @param limit 每一页的显示数量？
     * @return
     */
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    // 查询数量
    int selectCountByEntity(int entityType, int entityId);

    // 添加评论
    int insertComment(Comment comment);

}
