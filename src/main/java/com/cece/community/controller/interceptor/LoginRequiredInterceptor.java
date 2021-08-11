package com.cece.community.controller.interceptor;

import com.cece.community.annotation.LoginRequired;
import com.cece.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

// 拦截器，用于判断是否登录，不是登录状态不能访问一些功能
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    // 用于获取当前用户
    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断拦截到的是不是方法，是方法的话才拦截
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            // 尝试获取注解
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            // 不等于空，表示这个方法需要登录才能访问，不能getUser说明没有登录
            if (loginRequired != null && hostHolder.getUser() == null) {
                // 拦截后转向登录页面
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        return true;
    }
}
