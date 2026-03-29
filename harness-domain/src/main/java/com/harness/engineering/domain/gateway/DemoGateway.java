package com.harness.engineering.domain.gateway;

import com.harness.engineering.domain.model.DemoEntity;

import java.util.List;

/**
 * 示例领域网关接口
 * 
 * @author Harness Engineering Team
 */
public interface DemoGateway {
    
    /**
     * 根据 ID 获取实体
     * 
     * @param id 实体 ID
     * @return 领域实体
     */
    DemoEntity getById(Long id);
    
    /**
     * 保存实体
     * 
     * @param entity 领域实体
     */
    void save(DemoEntity entity);
    
    /**
     * 删除实体
     * 
     * @param id 实体 ID
     */
    void delete(Long id);
    
    /**
     * 查询所有实体
     * 
     * @return 实体列表
     */
    List<DemoEntity> listAll();
}
