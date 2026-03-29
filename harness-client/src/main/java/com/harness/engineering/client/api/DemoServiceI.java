package com.harness.engineering.client.api;

import com.harness.engineering.client.dto.Response;

/**
 * 服务接口定义示例
 * 
 * @author Harness Engineering Team
 */
public interface DemoServiceI {
    
    /**
     * 示例方法：获取欢迎信息
     * 
     * @return 响应结果
     */
    Response<String> getWelcomeMessage();
}
