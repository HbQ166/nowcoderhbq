package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {
    //查询当前用户会话列表，针对每一个会话只返回最新一条消息
    List<Message> selectConversations(int userId,int offset,int limit);
    //查询当前用户会话数量
    int selectConversationCount(int userId);
    //查询某个会话所包含消息列表
    List<Message> selectLetters(String conversationId,int offset,int limit);
    //查询某个会话所包含的私信数量
    int selectLetterCount(String conversationId);
    //查询未读消息数量
    int selectLetterUnreadCount(int userId,String conversationId);
    //新增消息
    int insertMessage(Message message);
    //更改消息状态
    int updateStatus(List<Integer> ids,int status);
    //查询某个主题下最新通知
    Message selectLatestNotice(int userId,String topic);
    //查询某个主题下包含通知数量
    int selectNoticeCount(int userId,String topic);
    //查询某个主题下未读通知数量
    int selectNoticeUnreadCount(int userId,String topic);
    //查询某个主题包含的通知列表
    List<Message> selectNotices(int userId,String topic,int offset,int limit);
}
