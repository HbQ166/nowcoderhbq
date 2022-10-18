package com.nowcoder.community;

import com.nowcoder.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes=CommunityApplication.class)
public class SensitivetTest {
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Test
    public void testSensitiveFilter(){
        String text="这里可以赌博，可以♥嫖♥娼，可以开票，我嫖娼了，哈哈，我嫖娼";
        System.out.println(sensitiveFilter.filter(text));
    }
}
