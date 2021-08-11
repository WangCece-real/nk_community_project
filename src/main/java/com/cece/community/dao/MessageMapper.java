package com.cece.community.dao;

import com.cece.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    /**
     * 查询当前用户的会话列表,针对每个会话只返回一条最新的私信.
     * @param userId 用户
     * @param offset 偏移-分页
     * @param limit 数量-分页
     * @return
     */
    List<Message> selectConversations(int userId, int offset, int limit);

    /**
     *  查询当前用户的会话数量.
     * @param userId
     * @return
     */
    int selectConversationCount(int userId);

    /**
     * 查询某个会话所包含的私信列表.
     * @param conversationId 会话的id
     * @param offset 偏移
     * @param limit 数量
     * @return
     */
    List<Message> selectLetters(String conversationId, int offset, int limit);

    /**
     * 查询某个会话所包含的私信数量.
     * @param conversationId
     * @return
     */
    int selectLetterCount(String conversationId);

    // 查询未读私信的数量
    int selectLetterUnreadCount(int userId, String conversationId);

    // 新增消息
    int insertMessage(Message message);

    // 修改消息的状态
    int updateStatus(List<Integer> ids, int status);

}
