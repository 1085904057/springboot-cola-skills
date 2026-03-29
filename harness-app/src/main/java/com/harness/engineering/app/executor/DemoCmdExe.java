package com.harness.engineering.app.executor;

import com.harness.engineering.client.dto.Response;
import org.springframework.stereotype.Component;

/**
 * 示例命令执行器
 * 
 * @author Harness Engineering Team
 */
@Component
public class DemoCmdExe {
    
    /**
     * 执行示例命令
     * 
     * @return 响应结果
     */
    public Response<String> execute() {
        return Response.success("Hello from COLA Architecture!");
    }
}
