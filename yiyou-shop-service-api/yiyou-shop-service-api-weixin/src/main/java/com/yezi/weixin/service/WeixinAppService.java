package com.yezi.weixin.service;

import com.yezi.core.base.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/8/17
 * Time:16:17
 */
@Api(tags = "微信接口服务")
public interface WeixinAppService {

    /**
     * 功能说明:应用服务接口
     */
    @GetMapping("/getApp")
    @ApiOperation(value = "微信相关信息")
    public BaseResponse getApp();
}
