package com.nowcoder.community.event;


import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.CommunityConstant;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger= LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.command}")
    private String command;

    @Value("${wk.image.storage}")
    private String storage;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_FOLLOW,TOPIC_LIKE})
    public void handleMessage(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误！");
            return;
        }
        Message message=new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String,Object> content=new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());

        if(!event.getData().isEmpty()){
            for(Map.Entry<String,Object> entry:event.getData().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

    }
    //消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误！");
            return;
        }
        DiscussPost post=discussPostService.findDisscussPost(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);

    }
    //消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误！");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());

    }
    //消费分享事件
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误！");
            return;
        }
        String htmlUrl=(String)event.getData().get("htmlUrl");
        String fileName=(String)event.getData().get("fileName");
        String suffix=(String)event.getData().get("suffix");

        String cmd=command+" --quality 75 "+htmlUrl+" "+storage+"/"+fileName+suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功："+cmd);
        } catch (IOException e) {
            logger.info("生成长图失败："+e.getMessage());
        }
        //启用定时器，监视该图片，一旦生成了，则上传七牛云
        UploadTask uploadTask=new UploadTask(fileName,suffix);
        Future future=taskScheduler.scheduleAtFixedRate(uploadTask,500);
        uploadTask.setFuture(future);
    }
    class UploadTask implements Runnable{

        //文件名称
        private String fileName;
        //文件后缀
        private String suffix;
        //开始时间
        private long startTime;
        //上传次数
        private int uploadtimes;
        public void setFuture(Future future) {
            this.future = future;
        }

        //启动任务的返回值
        private Future future;

        public UploadTask(String fileName,String suffix){
            this.fileName=fileName;
            this.suffix=suffix;
            this.startTime=System.currentTimeMillis();
        }

        @Override
        public void run() {
            //生成失败
            if(System.currentTimeMillis()-startTime>30000){
                logger.error("执行时间过长，终止任务"+fileName);
                future.cancel(true);
                return;
            }
            if(uploadtimes>=3){
                logger.error("上传次数过多，终止任务"+fileName);
                future.cancel(true);
                return;
            }
            String path=storage+"/"+fileName+suffix;
            File file=new File(path);
            if(file.exists()){
                logger.info(String.format("开始第%d次上传[%s]。",++uploadtimes,fileName));
                //设置响应信息
                StringMap policy=new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                //生成上传凭证
                Auth auth=Auth.create(accessKey,secretKey);
                String uploadToken=auth.uploadToken(shareBucketName,fileName,3600,policy);
                //指定上传机房
                UploadManager manager=new UploadManager(new Configuration(Region.region2()));
                try{
                    //开始上传图片
                    Response response=manager.put(
                            path,fileName,uploadToken,null,"image/"+suffix.replace(".",""),false);
                    //处理响应结果
                    JSONObject jsonObject=JSONObject.parseObject(response.bodyString());
                    if(jsonObject==null||jsonObject.get("code")==null||!jsonObject.get("code").toString().equals("0")){
                        logger.info(String.format("第%d次上传失败[%s]。",uploadtimes,fileName));
                    }else{
                        logger.info(String.format("第%d次上传成功[%s]。",uploadtimes,fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%s]。",uploadtimes,fileName));
                }
            }else{
                logger.info("等待图片生成["+fileName+"].");
            }
        }
    }

}
