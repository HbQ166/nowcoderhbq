package com.nowcoder.community;

import org.junit.Test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes=CommunityApplication.class)
public class TestLogger {

    private static final Logger logger= LoggerFactory.getLogger(TestLogger.class);
    @Test
    public void testLogger(){
        logger.info("debug log");
    }
}
