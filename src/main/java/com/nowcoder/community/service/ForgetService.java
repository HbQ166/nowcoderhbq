package com.nowcoder.community.service;


import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ForgetService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private UserService userService;


    public Map<String,String> sendCode(String email){
        Map<String,String> map=new HashMap<>();
        if(StringUtils.isBlank(email)){
            map.put("msg","邮箱不能为空");
            return map;
        }
        if(userMapper.selectByEmail(email)==null){
            map.put("msg","错误的邮箱，请检查后再次输入");
            return map;
        }
        //发送邮件验证码
        String code="";
        Random random=new Random();
        for(int i=1;i<=6;i++){
            int r=random.nextInt(10);
            code=code+r;
        }
        String content="您的验证码为："+code;
        mailClient.sendMail(email,"重置密码验证码",content);
        map.put("code",code);
        return map;
    }
    public void updatePassword(String email,String password){
        User user=userMapper.selectByEmail(email);
        userService.updatePassword(user.getId(),password);
    }
}
