package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;


/**
 * 用于持有用户信息代替session
 */
@Component
public class HostHolder {
    private ThreadLocal<User> users=new ThreadLocal<>();

    public void setUser(User user){
        users.set(user);
    }
    public User getUser(){
        return users.get();
    }

    public void clean(){
        users.remove();
    }
}
