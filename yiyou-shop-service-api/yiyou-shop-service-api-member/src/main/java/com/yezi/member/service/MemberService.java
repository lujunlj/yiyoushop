package com.yezi.member.service;

import com.yezi.core.base.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 会员服务接口
 * Created with IDEA
 * author: lujun
 * Date:2019/8/18
 * Time:0:12
 */
@Api(tags = "会员服务")
public interface MemberService {

    /**
     * 会员服务调用微信接口
     */
    @ApiOperation(value = "会员服务调用微信服务")
    @GetMapping("/invokeWeixin")
    public BaseResponse memberInvokeWeixin();
}
