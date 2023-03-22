package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.standard.expression.MessageExpression;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

@Component
public class MailClient {
    private static final Logger logger= LoggerFactory.getLogger(MailClient.class);

    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    private JavaMailSender mail;

    public void sendMail(String to,String subject,String content){
        try{
            MimeMessage mimeMailMessage=mail.createMimeMessage();
            MimeMessageHelper helper=new MimeMessageHelper(mimeMailMessage);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content,true);
            mail.send(helper.getMimeMessage());

        }catch(MessagingException e){
            logger.error("发送失败："+e.getMessage());

        }
    }
}

