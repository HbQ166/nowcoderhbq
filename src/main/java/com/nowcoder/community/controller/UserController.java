package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRquired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static Logger logger= LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private FollowService followService;
    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ForgetService forgetService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;



    @Autowired
    private RedisTemplate redisTemplate;

    @LoginRquired
    @RequestMapping(path="/setting",method = RequestMethod.GET)
    public String getSettingPage(Model model){
        //上传文件名称
        String fileName=CommunityUtil.generateUUID();
        //设置响应信息
        StringMap policy=new StringMap();
        policy.put("returnBody",CommunityUtil.getJSONString(0));
        //生成上传凭证
        Auth auth=Auth.create(accessKey,secretKey);
        String uploadToken=auth.uploadToken(headerBucketName,fileName,3600,policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);
        return "/site/setting";
    }

    //更新头像路径
    @RequestMapping(path="/header/url",method = RequestMethod.POST)
    @ResponseBody
    public String uploadHeaderUrl(String fileName){
        if(StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1,"文件名不能为空！");
        }

        String url=headerBucketUrl+"/"+fileName;
        userService.updateHeader(hostHolder.getUser().getId(),url);

        return CommunityUtil.getJSONString(0);

    }

    //废用
    @RequestMapping(path="/upload",method = RequestMethod.POST)
    @LoginRquired
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","您还没有选择图片");
            return "/site/setting";
        }
        String fileName=headerImage.getOriginalFilename();
        String suffix=fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确");
            return "/site/setting";
        }
        //生成随机文件名
        fileName= CommunityUtil.generateUUID()+suffix;
        //确认文件路径
        File dest=new File(uploadPath+"/"+fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败："+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常："+e.getMessage());
        }
        //更新当前用户头像路径（web访问路径）
        //http://localhost:8080/community/user/header/xxx.png
        User user=hostHolder.getUser();
        String headerUrl=domain+contextPath+"/user/header/"+fileName;
        userService.updateHeader(user.getId(),headerUrl);
        return "redirect:/index";

    }
    //废用
    @RequestMapping(path="/header/{fileName}" ,method = RequestMethod.GET)
    @LoginRquired
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName=uploadPath+"/"+fileName;
        //文件后缀
        String suffix=fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);
        try(FileInputStream fis=new FileInputStream(fileName);
            OutputStream os=response.getOutputStream();) {


            byte[] buffer=new byte[1024];
            int b=0;
            while ((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败："+e.getMessage());
        }

    }
    @RequestMapping(path="/upload/password",method = RequestMethod.POST)
    @LoginRquired
    public String updatePassword(String password, String newPassword, Model model, HttpServletRequest request){
        User user=hostHolder.getUser();
        //判断空值
        if(password==null){
            model.addAttribute("passwordMsg","您还没有输入原密码");
            return "/site/setting";
        }
        password=CommunityUtil.md5(password+user.getSalt());
        if(!user.getPassword().equals(password)){
            model.addAttribute("passwordMsg","您输入的原密码不正确");
            return "/site/setting";
        }
        if(newPassword==null){
            model.addAttribute("newPasswordMsg","您好没有输入新密码");
            return "/site/setting";

        }

        userService.updatePassword(user.getId(),newPassword);
        String ticket= CookieUtil.getValue(request,"ticket");
        userService.logout(ticket);
        return "redirect:/login";

    }
    //个人主页
    @RequestMapping(path="/profile/{userId}",method=RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model){
        User user=userService.findById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在");
        }
        //用户
        model.addAttribute("user",user);
        //点赞数量
        int likeCount=likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        //关注数量
        long followeeCount=followService.findFolloweeCount(userId,ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount=followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount",followerCount);
        //是否已关注
        boolean hasFollowed=false;
        if(hostHolder.getUser()!=null){
            hasFollowed=followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }
    @RequestMapping(path="/profile/{userId}/discussPosts",method = RequestMethod.GET)
    public String getProfilePosts(@PathVariable("userId") int userId, Model model, Page page){
        page.setRows(discussPostService.findDiscussPostRows(userId));
        page.setPath("/user/profile/"+userId+"/discussPosts");
        page.setLimit(5);

        List<DiscussPost> list=discussPostService.findDiscussPosts(userId,page.getOffset(),page.getLimit(),0);
        List<Map<String,Object>> discussPost=new ArrayList<>();
        if(list!=null){
            for(DiscussPost post:list){
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                map.put("createTime",post.getCreateTime());
                discussPost.add(map);
            }
        }
        model.addAttribute("discussPost",discussPost);
        model.addAttribute("discussPostCount",discussPostService.findDiscussPostRows(userId));
        model.addAttribute("userId",userId);
        return "/site/my-post";
    }
    @RequestMapping(path="/profile/{userId}/reply",method = RequestMethod.GET)
    public String getProfileComments(@PathVariable("userId") int userId, Model model, Page page){
        page.setRows(commentService.findCommentCountByUserId(userId));
        page.setPath("/user/profile/"+userId+"/reply");
        page.setLimit(5);

        List<Comment> list=commentService.findCommentsByUserId(userId,page.getOffset(),page.getLimit());
        List<Map<String,Object>> comments=new ArrayList<>();
        if(list!=null){
            for(Comment comment:list){
                Map<String,Object> map=new HashMap<>();
                if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
                    Comment entityComment=commentService.findCommentById(comment.getEntityId());
                    DiscussPost post=discussPostService.findDiscussPost(entityComment.getEntityId());
                    map.put("postId",entityComment.getEntityId());
                    map.put("title",post.getTitle());
                }else {
                    DiscussPost post=discussPostService.findDiscussPost(comment.getEntityId());
                    map.put("postId",post.getId());
                    map.put("title",post.getTitle());
                }
                map.put("comment",comment);
                comments.add(map);
            }
        }
        model.addAttribute("comment",comments);
        model.addAttribute("count",commentService.findCommentCountByUserId(userId));
        model.addAttribute("userId",userId);
        return "/site/my-reply";
    }
    @RequestMapping(path="/forget", method=RequestMethod.GET)
    public String getForgetPage(){
        return "/site/forget";
    }
    @RequestMapping(path="/forget/send", method=RequestMethod.POST)
    @ResponseBody
    public String sendCode(String email){


        Map<String,String> map=forgetService.sendCode(email);

        if(!StringUtils.isBlank(map.get("msg"))){
            return CommunityUtil.getJSONString(1,map.get("msg"));
        }
        if(StringUtils.isBlank(map.get("code"))){
            return CommunityUtil.getJSONString(1,"验证码生成失败");
        }
        String redisKey=RedisKeyUtil.getForgetKey(email);
        redisTemplate.expire(redisKey,60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(redisKey,map.get("code"));
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path="/forget", method=RequestMethod.POST)
    public String updateForgetPassword(String email,String code,String password,Model model){
        if(email==null){
            model.addAttribute("emailMsg","邮箱不能为空");
            return "/site/forget";
        }
        if(code==null){
            model.addAttribute("codeMsg","验证码不能为空");
            return "/site/forget";
        }
        String redisKey=RedisKeyUtil.getForgetKey(email);
        if(code.equals(redisTemplate.opsForValue().get(redisKey))){
            forgetService.updatePassword(email,password);
            return "redirect:/login";
        }else{
            model.addAttribute("codeMsg","验证码不正确");
            return "/site/forget";
        }
    }
}
