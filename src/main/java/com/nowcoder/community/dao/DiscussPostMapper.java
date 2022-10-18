package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);
    //注解用于给参数取别名
    //如果只有一个参数且用于<if>中，必须使用别名
    int selectDiscussPostRows(@Param("userId")int userId);
    int insertDiscussPost(DiscussPost discussPost);
    DiscussPost selectDiscussPostById(int id);

}
