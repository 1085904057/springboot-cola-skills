package com.harness.engineering.client.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应对象
 * 
 * @param <T> 数据类型
 */
@Data
public class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 状态码
     */
    private String code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;
    
    /**
     * 成功标志
     */
    private boolean success;
    
    public static <T> Response<T> success() {
        Response<T> response = new Response<>();
        response.setSuccess(true);
        response.setCode("SUCCESS");
        response.setMessage("操作成功");
        return response;
    }
    
    public static <T> Response<T> success(T data) {
        Response<T> response = success();
        response.setData(data);
        return response;
    }
    
    public static <T> Response<T> failure(String code, String message) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
