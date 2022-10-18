package com.nowcoder.community.service;

public interface CommunityConstant {
    /**
     * 激活成功
     */
    int ACTIVATION_SUCCESS=0;
    /**
     * 激活失败
     */
    int ACTIVATION_FAILURE=2;
    /**
     * 重复激活
     */
    int ACTIVATION_REPEAT=1;
    /**
     * 默认状态下记住时间
     */
    int DEFAULT_EXPIRED_SECONDS=3600*12;
    /**
     * 记住状态下时间
     */
    int REMEMBER_EXPIRED_SECOND=3600*24*100;
    /**
     * 实体类型：帖子
     */
    int ENTITY_TYPE_POST = 1;
    /**
     * 实体类型：评论
     */
    int ENTITY_TYPE_COMMENT = 2;
}
