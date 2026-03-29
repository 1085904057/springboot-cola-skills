package com.harness.engineering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 应用启动类
 * 
 * @author Harness Engineering Team
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.harness.engineering")
public class HarnessStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(HarnessStartApplication.class, args);
        System.out.println("========================================");
        System.out.println("Harness Engineering Platform Started!");
        System.out.println("========================================");
    }
}
