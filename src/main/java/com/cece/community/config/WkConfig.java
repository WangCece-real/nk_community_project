package com.cece.community.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

// Configuration注解会让SpringBoot认为它是一个配置类，
// 在服务启动的时候，先初始化配置类，在new 出来配置类之后，就运行了init()初始化方法，
// 检查是否存在这样的路径，不存在则创建路径
@Configuration
public class WkConfig {

    @PostConstruct
    public void init() {
        // 创建WK图片目录
        File file = new File(wkImageStorage);
        if (!file.exists()) {
            file.mkdir();
            logger.info("创建WK图片目录: " + wkImageStorage);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

}
