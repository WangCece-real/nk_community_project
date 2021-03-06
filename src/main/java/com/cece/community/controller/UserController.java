package com.cece.community.controller;

import com.cece.community.annotation.LoginRequired;
import com.cece.community.entity.User;
import com.cece.community.service.FollowService;
import com.cece.community.service.LikeService;
import com.cece.community.service.UserService;
import com.cece.community.util.CommunityConstant;
import com.cece.community.util.CommunityUtil;
import com.cece.community.util.HostHolder;
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

import javax.management.monitor.MonitorSettingException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private FollowService followService;

    @Value("${community.path.upload}")
    private String uploadPath;

//    @Value("${community.path.domain}")
    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }


    /**
     *  修改用户的头像
     * @param headerImage SpringMVC用来处理上传文件
     * @param model
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        //SpringMVC的 MultipartFile用来处理上传文件
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }
        // 得到文件名
        String fileName = headerImage.getOriginalFilename();
        // 得到文件名的后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 判断后缀是空的，表示文件格式有误
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);
        // 更新成功，重定向到首页
        return "redirect:/index";
    }

    /**
     *  向浏览器输出图片，因为图片是二进制文件，不是直接return的，所以空返回值
     * @param fileName
     * @param response
     */
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 获取文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        // 写入括号中，会自动关闭资源
        try (
                // 输入流，从文件存放位置读入文件
                FileInputStream fis = new FileInputStream(fileName);
                // 输出流，输出到浏览器
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(Model model, String originPassword, String newPassword){
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user, originPassword, newPassword);
        if(map == null || map.isEmpty()){
            return "redirect:/index";
        }else{
            for(String key : map.keySet()){
                model.addAttribute(key, map.get(key));
            }
            return "/site/setting";
        }
    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // "我"的关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // “我”的粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // “我”是否已关注这个用户
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

}
