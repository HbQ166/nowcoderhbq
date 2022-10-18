package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRquired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {

    private static Logger logger= LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    @LoginRquired
    @RequestMapping(path="/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }
    @LoginRquired
    @RequestMapping(path="/upload",method = RequestMethod.POST)
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
    @RequestMapping(path="/header/{fileName}" ,method = RequestMethod.GET)
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

}
