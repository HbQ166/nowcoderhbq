package com.nowcoder.community.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//@Service
//@Scope("prototype")
public class AlphaService {
    public AlphaService(){
        System.out.println("构造AlphaService");
    }
    @PostConstruct
    public void init(){
      System.out.println("初始化AlphaService");
    }
    @PreDestroy
    public void destroy(){
        System.out.println("销毁AlphaService");
    }
}
