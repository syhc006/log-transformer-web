package net.xicp.chocolatedisco.logtransformerweb.repository;

import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Created by SunYu on 2018/3/1.
 */
public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long>, JpaSpecificationExecutor<DeviceType> {
    List<DeviceType> findByIdIn(List<Long> ids);
}
