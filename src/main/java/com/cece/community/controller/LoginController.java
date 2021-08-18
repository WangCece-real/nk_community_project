package com.cece.community.controller;


import com.cece.community.entity.User;
import com.cece.community.service.UserService;
import com.cece.community.util.CommunityConstant;
import com.cece.community.util.CommunityUtil;
import com.cece.community.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    // 注入业务层
    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    // 获取到注册页面
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    //访问登录页面
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    /**
     * 注册controller
     * @param model 用于携带数据
     * @param user 从页面-->User对象，只要传入的值和user的属性相匹配，
     *             springMVC就会把值注入给user对象的属性
     * @return
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        // 传入User， 调用service来注册
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            //为空表示注册成功，跳转到操作结果页面，发送激活邮件
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            //
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            // 注册失败，把错误信息发送给页面，然后进行
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }
//      激活账号的地址
//    // http://localhost:8080/community/activation/101/code

    /**
     * 激活账号，从邮箱点进来的路径
     * @param model 携带激活成功还是失败信息；
     *              成功则携带目标登录页面，失败目标到首页
     * @param userId 从路径中取到用户的id
     * @param code 得到路径中的激活码
     * @return 返回处理界面
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    // PathVariable 表示从路径中取这个值
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }
//
//    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
//    public void getKaptcha(HttpServletResponse response, HttpSession session) {
//        // 生成验证码
//        // 生成字符串
//        String text = kaptchaProducer.createText();
//        // 通过字符串生成图片
//        BufferedImage image = kaptchaProducer.createImage(text);
//
//        // 将验证码存入session
//        session.setAttribute("kaptcha", text);
//
//        // 将图片输出给浏览器
//        // 这个流是springMVC管理，可以不用手动关闭
//        response.setContentType("image/png");
//        try {
//            OutputStream os = response.getOutputStream();
//            ImageIO.write(image, "png", os);
//        } catch (IOException e) {
//            // 出现异常捕获日志
//            logger.error("响应验证码失败:" + e.getMessage());
//        }
//    }

    /**
     * 验证码方法使用Redis重构
     * @param response
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 验证码的归属
        // 随机生成一个临时凭证
        String kaptchaOwner = CommunityUtil.generateUUID();
        // 给浏览器发送一个cookies存储这个临时凭证
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        // cookies生存时间60秒
        cookie.setMaxAge(60);
        // 设置cookies的可用路径
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        // Redis的验证码存60秒
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }



    /**
     *  如果输入的参数存在User这样的实体类，spring会自动装进Model中，
     *  但是普通的类就不会装进去
     * @param username 用户名
     * @param password 密码
     * @param code 验证码 生成存在session中---> 存在Redis中，
     * @param rememberme 是否记住我
     * @param model
     * @param response
     * @String kaptchaOwner 从cookies中获取的临时凭证
     * @return
     */
    // 可以写两个方法的请求路径相同， 但是请求的方法必须不一样
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model,/* HttpSession session,*/ HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // 检查验证码是否正确，不正确则返回login界面
//        String kaptcha = (String) session.getAttribute("kaptcha");

        // 使用Redis来检查验证码
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }

        // 检查账号,密码
        // 用户勾选了 记住我 则记住时间更长
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        // 判断登录成功与否
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            // 包含ticket则成功
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            // 设置cookie的有效路径
            cookie.setPath(contextPath);
            // 设置cookies的有效时间
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            //登录失败
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }
    //登出
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        // 退出要重定向到登录页面
        return "redirect:/login";
    }

}
