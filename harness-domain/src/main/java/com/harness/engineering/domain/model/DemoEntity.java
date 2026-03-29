package com.harness.engineering.domain.model;

import lombok.Data;

/**
 * 示例领域实体
 * 
 * @author Harness Engineering Team
 */
@Data
public class DemoEntity {
    
    /**
     * 实体 ID
     */
    private Long id;
    
    /**
     * 名称
     */
    private String name;
    
    /**
     * 描述
     */
    private String description;
}
