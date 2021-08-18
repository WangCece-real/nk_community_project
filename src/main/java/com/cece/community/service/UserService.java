package com.cece.community.service;

import com.cece.community.dao.LoginTicketMapper;
import com.cece.community.dao.UserMapper;
import com.cece.community.entity.LoginTicket;
import com.cece.community.entity.User;
import com.cece.community.util.CommunityConstant;
import com.cece.community.util.CommunityUtil;
import com.cece.community.util.MailClient;
import com.cece.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    // 邮件客户端
    @Autowired
    private MailClient mailClient;

    // 模板引擎
    @Autowired
    private TemplateEngine templateEngine;

    // 注入值使用@Value
//    域名注入
    @Value("${community.path.domain}")
    private String domain;

    //项目名注入
    @Value("${server.servlet.context-path}")
    private String contextPath;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id) {
//        return userMapper.selectById(id);
        // 优先从缓存获取
        User user = getCache(id);
        // 缓存没有，则从数据库读取，然后存入缓存
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    /**
     * 注册用户，返回的map包含的是存在的问题
     * @param user
     * @return 返回的map包含的是存在的问题，map为空表示没有问题
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        // 空值判断处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            //账号不能为空，但是不是一种错误，返回的是提示信息
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号是否已经存在,和数据库中的比较
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱是否已经存在
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户：就是把账户信息存在库里
        // 给用户密码加盐
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        // 生成用户激活码
        user.setActivationCode(CommunityUtil.generateUUID());
        //牛客网的随机头像
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        //设置用户的创建时间
        user.setCreateTime(new Date());
        //插入用户
        userMapper.insertUser(user);

        // 发送激活邮件
        /* 这是thymeleaf的上下文容器
            携带变量：email, url

         */
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/用户ID/激活码
        //调用insert语句之后，就会有userId
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        // 模板引擎输出的是一个html页面, "/mail/activation",是模板页面
        String content = templateEngine.process("/mail/activation", context);
        // 发送邮件
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    /**
     * 激活账号:
     * @param userId 传入userId
     * @param code 激活码
     * @return 返回常量接口的值，表示的是激活状态
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        // status == 1表示已经激活过
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            //激活成功把状态改为1
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            // 激活码不正确，激活失败
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 登录业务处理
     * @param username 用户名
     * @param password 密码
     * @param expiredSeconds 验证码失效时间
     * @return
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }
        // 通过以上的验证，表示登录成功
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        // 设置过期时间
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);

        // 使用Redis来存储登录凭证
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);


        // 要把ticket发送给客户端
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);
        // 修改Redis中的存储为JSON格式的数据
        // 1. 先把数据取出来
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        // 2. 修改status的状态
        loginTicket.setStatus(1);
        // 3. 存入修改后的数据
        redisTemplate.opsForValue().set(redisKey, loginTicket);    }

    /**
     * 查询ticket的方法，返回的是一个LoginTicket对象
     * @param ticket
     * @return
     */
    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 更新用户头像
     * @param userId
     * @param headerUrl
     * @return
     */
    public int updateHeader(int userId, String headerUrl) {
        // 不能更新之前清理缓存，否则如果更新失败了，还是会把缓存清理了
//        clearCache(userId);
//        return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }


    public Map<String, Object> updatePassword( User user, String originPassword, String newPassword){
        Map<String, Object> map = new HashMap<>();
        String s = CommunityUtil.md5( originPassword + user.getSalt());
        if(user.getPassword().equals(s)){
            userMapper.updatePassword(user.getId(),  CommunityUtil.md5( newPassword + user.getSalt()));
        }else{
            map.put("passwordMsg", "原密码错误！");
        }
        return map;
    }

    public User findUserByName(String name){
        return userMapper.selectByName(name);
    }


    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    /**
     * 获取对应User的权限
     * @param userId
     * @return
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
