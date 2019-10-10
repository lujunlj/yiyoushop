package com.yezi.member.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yezi.core.base.BaseResponse;
import com.yezi.member.entity.UserEntity;
import com.yezi.member.service.MemberRegisterService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/9/23
 * Time:16:24
 */
@RestController
public class MemberRegisterServiceImpl implements MemberRegisterService {

    @Override
    public BaseResponse<JSONObject> register(UserEntity userEntity, String registCode) {

        return null;
    }
}
