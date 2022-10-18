package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@SpringBootTest
@ContextConfiguration(classes=CommunityApplication.class)
public class MailTest {
    @Autowired
    private MailClient mail;
    @Autowired
    private TemplateEngine templateEngine;
    @Test
    public void sendTest(){
        mail.sendMail("1763458587@qq.com","1","1");

    }
    @Test
    public void sendHtml(){
        Context context=new Context();
        context.setVariable("username","sunday");
        String content=templateEngine.process("/mail/demo",context);
        mail.sendMail("1763458587@qq.com","HTML",content);


    }

}
