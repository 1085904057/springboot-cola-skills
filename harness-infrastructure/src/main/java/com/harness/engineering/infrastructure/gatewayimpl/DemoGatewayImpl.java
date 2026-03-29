package com.harness.engineering.infrastructure.gatewayimpl;

import com.harness.engineering.domain.gateway.DemoGateway;
import com.harness.engineering.domain.model.DemoEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 示例网关实现类
 * 
 * @author Harness Engineering Team
 */
@Component
public class DemoGatewayImpl implements DemoGateway {
    
    /**
     * 模拟数据库存储
     */
    private final ConcurrentHashMap<Long, DemoEntity> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public DemoEntity getById(Long id) {
        return storage.get(id);
    }
    
    @Override
    public void save(DemoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        storage.put(entity.getId(), entity);
    }
    
    @Override
    public void delete(Long id) {
        storage.remove(id);
    }
    
    @Override
    public List<DemoEntity> listAll() {
        return new ArrayList<>(storage.values());
    }
}
