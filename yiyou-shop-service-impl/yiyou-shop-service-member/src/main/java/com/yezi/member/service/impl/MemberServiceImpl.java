package com.yezi.member.service.impl;

import com.yezi.core.base.BaseResponse;
import com.yezi.member.service.MemberService;
import com.yezi.weixin.service.WeixinAppService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/8/18
 * Time:0:25
 */
@RestController
public class MemberServiceImpl  implements MemberService {

    @Reference
    private WeixinAppService weixinAppService;

    public BaseResponse memberInvokeWeixin() {
        //会员调用微信
        return weixinAppService.getApp();
    }
}
