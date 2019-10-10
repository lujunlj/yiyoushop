package com.yezi.core.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Created with IDEA
 * author: lujun
 * Date:2019/9/23
 * Time:7:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    /**
     * 返回状态码
     * */
    private Integer code;
    /**
     * 消息
     * */
    private String msg;

    /**
     * 数据
     * */
    private T data;

    BaseResponse(Integer code,String msg){
        this.code = code;
        this.msg = msg;
    }

}
