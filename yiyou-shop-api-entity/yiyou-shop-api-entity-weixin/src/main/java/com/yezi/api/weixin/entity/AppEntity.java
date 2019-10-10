package com.yezi.api.weixin.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/8/17
 * Time:16:15
 */
@Data
@AllArgsConstructor
public class AppEntity implements Serializable {
    /**
     * appid
     */
    private String appId;
    /**
     * 应用名称
     */
    private String appName;

}
