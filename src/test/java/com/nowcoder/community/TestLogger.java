package com.nowcoder.community;

import org.junit.Test;


import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@ContextConfiguration(classes=CommunityApplication.class)
@RunWith(SpringRunner.class)
public class TestLogger {

    private static final Logger logger= LoggerFactory.getLogger(TestLogger.class);
    @Test
    public void testLogger(){
        logger.info("debug log");
    }
}
