package com.yezi;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/8/17
 * Time:16:29
 */
@SpringBootApplication
@EnableDiscoveryClient
@DubboComponentScan
@EnableSwagger2Doc
public class AppWeixin {

    public static void main(String[] args) {
        SpringApplication.run(AppWeixin.class);
    }
}
