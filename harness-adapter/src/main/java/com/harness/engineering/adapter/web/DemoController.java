package com.harness.engineering.adapter.web;

import com.harness.engineering.client.api.DemoServiceI;
import com.harness.engineering.client.dto.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例 Controller
 * 
 * @author Harness Engineering Team
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    @Autowired
    private DemoServiceI demoService;
    
    /**
     * 获取欢迎信息
     */
    @GetMapping("/welcome")
    public Response<String> getWelcome() {
        return demoService.getWelcomeMessage();
    }
}
