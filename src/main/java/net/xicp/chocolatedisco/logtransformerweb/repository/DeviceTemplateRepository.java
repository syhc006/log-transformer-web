package net.xicp.chocolatedisco.logtransformerweb.repository;

import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceTemplate;
import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Created by SunYu on 2017/9/12.
 */
public interface DeviceTemplateRepository extends JpaRepository<DeviceTemplate, Long>, JpaSpecificationExecutor<DeviceTemplate> {
    List<DeviceTemplate> findByIdIn(List<Long> ids);

    List<DeviceTemplate> findByDeviceType(DeviceType deviceType);
}
