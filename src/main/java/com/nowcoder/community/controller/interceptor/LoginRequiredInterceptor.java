package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRquired;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            HandlerMethod handlerMethod=(HandlerMethod) handler;
            Method method=handlerMethod.getMethod();
            LoginRquired loginRquired=method.getAnnotation(LoginRquired.class);
            if(loginRquired!=null&&hostHolder.getUser()==null){
                String xRequestedWith=request.getHeader("x-requested-with");
                if("XMLHttpRequest".equals(xRequestedWith)){
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer=response.getWriter();
                    writer.write(CommunityUtil.getJSONString(1,"您还没有登录，请先登录！"));
                }else{
                response.sendRedirect(request.getContextPath()+"/login");
                }
                return false;
            }
        }
        return true;
    }
}
