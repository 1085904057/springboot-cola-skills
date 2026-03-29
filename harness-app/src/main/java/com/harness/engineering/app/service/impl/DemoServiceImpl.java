package com.harness.engineering.app.service.impl;

import com.harness.engineering.app.executor.DemoCmdExe;
import com.harness.engineering.client.api.DemoServiceI;
import com.harness.engineering.client.dto.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 示例服务实现类
 * 
 * @author Harness Engineering Team
 */
@Service
public class DemoServiceImpl implements DemoServiceI {
    
    @Autowired
    private DemoCmdExe demoCmdExe;
    
    @Override
    public Response<String> getWelcomeMessage() {
        return demoCmdExe.execute();
    }
}
