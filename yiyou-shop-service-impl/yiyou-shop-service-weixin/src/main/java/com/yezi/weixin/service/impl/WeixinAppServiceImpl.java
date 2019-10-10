package com.yezi.weixin.service.impl;

import com.yezi.api.weixin.entity.AppEntity;
import com.yezi.core.base.BaseApiService;
import com.yezi.core.base.BaseResponse;
import com.yezi.weixin.service.WeixinAppService;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能:微信服务接口的实现
 * Created with IDEA
 * author: lujun
 * Date:2019/8/17
 * Time:16:25
 */
@RestController
@RefreshScope
@Service
public class WeixinAppServiceImpl extends BaseApiService<AppEntity> implements WeixinAppService {

    @Value("${username:nobody}")
    private String username;

    @Override
    public BaseResponse<AppEntity> getApp() {
        return setResultSuccess(new AppEntity("kt_zero",username+"WeixinId"));
    }
}
