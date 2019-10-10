package com.yezi.member;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/8/19
 * Time:0:32
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableSwagger2Doc
public class MemberApp {
    public static void main(String[] args) {
        SpringApplication.run(MemberApp.class);
    }
}
