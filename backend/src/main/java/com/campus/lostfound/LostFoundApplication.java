package com.campus.lostfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园失物招领平台 - 启动类
 */
@SpringBootApplication
@MapperScan("com.campus.lostfound.mapper")
public class LostFoundApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
        System.out.println("""

                ========================================================
                  校园失物招领平台后端启动成功
                  接口文档  http://localhost:8080/doc.html
                ========================================================
                """);
    }
}
