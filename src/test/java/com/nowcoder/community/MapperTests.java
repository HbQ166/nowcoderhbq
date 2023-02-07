package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;


@SpringBootTest
@ContextConfiguration(classes=CommunityApplication.class)
@RunWith(SpringRunner.class)
public class MapperTests {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper dpMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(101);
        System.out.println(user);
        user = userMapper.selectByName("liubei");
        System.out.println(user);
        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsert() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("www.niusnc.com");
        user.setCreateTime(new Date());
        int row = userMapper.insertUser(user);
        System.out.println(row);
        System.out.println(user.getId());
    }

    @Test
    public void testUpdate() {
        int rows = userMapper.updateStatus(150, 1);
        System.out.println(rows);
    }

    @Test
    public void testDiscussPosts() {
        List<DiscussPost> list = dpMapper.selectDiscussPosts(0, 0, 10,0);
        for (DiscussPost post : list) {
            System.out.println(post);

        }
        int count=dpMapper.selectDiscussPostRows(0);
        System.out.println(count);
    }
    @Test
    public void testLoginTicketMapper(){
        LoginTicket lg=new LoginTicket();
        lg.setUserId(1);
        lg.setTicket("abc");
        lg.setExpired(new Date());
        lg.setStatus(1);
        System.out.println(loginTicketMapper.insertLoginTicket(lg));
        System.out.println(loginTicketMapper.selectByTicket("abc"));
        loginTicketMapper.updateStatus("abc",0);
        System.out.println(loginTicketMapper.selectByTicket("abc"));

    }
    @Test
    public void selectTestMessage(){
        List<Message> lm=messageMapper.selectConversations(111,0,20);
        for(Message m:lm){
            System.out.println(m);
        }
        int count1=messageMapper.selectConversationCount(111);
        System.out.println(count1);
        List<Message> lm2=messageMapper.selectLetters("111_112",0,20);
        for(Message m:lm2){
            System.out.println(m);
        }
        int count2=messageMapper.selectLetterCount("111_112");
        System.out.println(count2);
        count2=messageMapper.selectLetterUnreadCount(131,"111_131");
        System.out.println(count2);
    }

}
