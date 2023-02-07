package com.nowcoder.community.service;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {
    @Autowired
    private DiscussPostRepository discussPostRepository;
    @Qualifier("client")
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    public void saveDiscussPost(DiscussPost post){
        discussPostRepository.save(post);
    }
    public void deleteDiscussPost(int id){
        discussPostRepository.deleteById(id);
    }
    public Map<String,Object> searchDiscussPost(String keyword, int current, int limit) throws IOException {
        SearchRequest searchRequest = new SearchRequest("discusspost");
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.field("content");
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<em>");
        highlightBuilder.postTags("</em>");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                //matchQuery()是模糊匹配，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value))
                //termQuery()是精确查询：searchSourceBuilder.query(QuertBuilders.termQuery(key,value))
                .query(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //一个可选，可用于控制搜索时间 searchSourceBuilder.timeOut(new TimeValue(60,TimeUnit.SECONDS))
                .from(current)//从哪条开始差
                .size(limit)//需要查出总记录条数
                .highlighter(highlightBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<DiscussPost> list = new ArrayList<>();
        long total = searchResponse.getHits().getTotalHits().value;
        for (SearchHit hit : searchResponse.getHits()) {
            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
            //处理高亮结果
            HighlightField highlightFieldtitle = hit.getHighlightFields().get("title");
            if (highlightFieldtitle != null) {
                discussPost.setTitle(highlightFieldtitle.getFragments()[0].toString());
            }
            HighlightField highlightFieldcontent = hit.getHighlightFields().get("content");
            if (highlightFieldcontent != null) {
                discussPost.setContent(highlightFieldcontent.getFragments()[0].toString());
            }
            list.add(discussPost);
        }
        Map<String,Object> map=new HashMap<>();
        map.put("list",list);
        map.put("total",total);
        return map;
    }

}
