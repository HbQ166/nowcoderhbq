package com.nowcoder.community.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.alibaba.fastjson2.JSON.toJSONString;

@Service
public class DiscussPostService {

    private static final Logger logger= LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    @Value("${caffeine.posts.expire-second}")
    private int expireSecond;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    //Caffeine核心接口Cache,loadingCache,AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;
    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache= Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSecond, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(String key) throws Exception {
                        if(key==null&&key.length()==0){
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] params=key.split(":");
                        if(params==null&&params.length!=2){
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset=Integer.valueOf(params[0]);
                        int limit=Integer.valueOf(params[1]);
                        //二级缓存Redis->mysql
                        redisTemplate.expire(key,600,TimeUnit.SECONDS);
                        List<DiscussPost> postList=(List<DiscussPost>)redisTemplate.opsForValue().get(key);
                        if(postList!=null) {
                            return postList;
                        }
                        postList=discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                        redisTemplate.opsForValue().set(key,postList);
                        logger.info("load post list from DB.");
                        return postList;
                    }
                });
        //初始化帖子总数缓存
        postRowsCache= Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSecond, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(Integer key) throws Exception {
                        //二级缓存Redis->mysql
                        String redisKey=key.toString();
                        redisTemplate.expire(redisKey,600,TimeUnit.SECONDS);
                        if(redisTemplate.opsForValue().get(redisKey)!=null){
                            return (Integer) redisTemplate.opsForValue().get(redisKey);
                        };
                        int rows=discussPostMapper.selectDiscussPostRows(key);
                        redisTemplate.opsForValue().set(redisKey,rows);
                        logger.info("load post rows from DB.");
                        return rows;
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit,int orderMode){
        if(userId==0&&orderMode==1){
            return postListCache.get(offset+":"+limit);
        }
        logger.info("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }
    public int findDiscussPostRows(int userId){
        if(userId==0){
            return postRowsCache.get(userId);
        }
        logger.info("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }
    public int addDiscussPost(DiscussPost post){
        if(post==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        return discussPostMapper.insertDiscussPost(post);
    }
    public DiscussPost findDiscussPost(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
