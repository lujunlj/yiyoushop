package com.yezi.weixin.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yezi.core.base.BaseApiService;
import com.yezi.core.base.BaseResponse;
import com.yezi.core.constants.Constants;
import com.yezi.core.utils.RedisUtil;
import com.yezi.weixin.service.VerificaCodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/9/23
 * Time:8:12
 */
@RestController
public class VerificaCodeServiceImpl extends BaseApiService<JSONObject> implements VerificaCodeService {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public BaseResponse<JSONObject> verificaWeixinCode(String phone, String weixinCode) {
        //1. 验证参数是否为空
        if(StringUtils.isBlank(phone)){
            return setResultError("手机号不能为空!");
        }
        if(StringUtils.isBlank(weixinCode)){
            return setResultError("注册码不能为空!");
        }
        //从redis中取得手机号对应的验证码
        String weixinCodeKey = Constants.WEIXINCODE_KEY+phone;
        String redisCode = redisUtil.getString(weixinCodeKey);
        if(StringUtils.isEmpty(redisCode)){
            return setResultError("注册码可能已经过期");
        }
        if(!redisCode.equals(weixinCode)){
            return setResultError("注册码不正确!");
        }
        //移除注册码
        redisUtil.delKey(weixinCodeKey);
        return setResultSuccess("验证码比对正确!");
    }
}

