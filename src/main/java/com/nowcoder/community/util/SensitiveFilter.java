package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger= LoggerFactory.getLogger(SensitiveFilter.class);

    //替换符号
    private static final String REPLACEMENT="***";

    //根节点
    private TrieNode rootNode=new TrieNode();
    @PostConstruct
    public void init(){
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader=new BufferedReader(new InputStreamReader(is))
        ) {
            String keyword;
            while((keyword=reader.readLine())!=null){
                //添加到前缀树
                this.addKeyword(keyword);
            }


        }catch(IOException e){
            logger.error("加载敏感词失败"+e.getMessage());
        }

    }
    //将一个敏感词添加到前缀树中
    private void addKeyword(String keyword){
        TrieNode tempNode=rootNode;
        for(int i=0;i<keyword.length();i++){
            char c=keyword.charAt(i);
            TrieNode subNode=tempNode.getSubNode(c);
            if(subNode==null){
                //初始化子节点
                subNode=new TrieNode();
                tempNode.addSubNodes(c,subNode);
            }
            //指向下一个节点，进入下一轮
            tempNode=subNode;
            //设置结束标识
            if(i==keyword.length()-1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 待滤文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        //指针1
        TrieNode tempNode=rootNode;
        //指针2
        int begin=0;
        //指针3
        int position=0;
        //结果
        StringBuilder stringBuilder=new StringBuilder();
        while(position<text.length()){
            Character c=text.charAt(position);
            System.out.println(position+"/"+begin);


            //跳过符号
            if(isSymbol(c)){
                //若指针1指向根节点,让指针2向前走一步
                if(tempNode==rootNode){
                    stringBuilder.append(c);
                    begin++;
                }
                //无论符号在什么位置，指针三都会往下走
                position++;
                continue;
            }
            //检查下级节点
            tempNode=tempNode.getSubNode(c);
            if(tempNode==null){
                //以begin开头的字符串不是敏感词
                stringBuilder.append(text.charAt(begin));
                //进入下一个位置
                position=++begin;
                //归位
                tempNode=rootNode;
            }else if(tempNode.isKeywordEnd()){
                //发现敏感词，将begin到position字符串替换掉
                stringBuilder.append(REPLACEMENT);
                begin=++position;
                //归位
                tempNode=rootNode;

            }else{
                //检查下一个字符
                if(begin<text.length()&&position==text.length()-1){
                    stringBuilder.append(text.charAt(begin));
                    position=++begin;
                    tempNode=rootNode;

                }else{
                    position++;
                }


            }

        }



        return stringBuilder.toString();
    }
    //特殊符号
    private boolean isSymbol(Character c){
        //0x2E80~0x9FFF是东亚文字范围
        return!CharUtils.isAsciiAlphanumeric(c)&&(c>0x9FFF||c<0x2E80);
    }
    //前缀树
    private class TrieNode{

        //关键词结束标识
        private boolean isKeywordEnd =false;
        //子节点（key是节点字符，value是节点）
        private Map<Character,TrieNode> subNodes=new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
        //添加子节点
        public void addSubNodes(Character c,TrieNode node){
            subNodes.put(c,node);
        }
        //获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }

    }
}
