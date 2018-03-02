package net.xicp.chocolatedisco.logtransformerweb.repository;

import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceType;
import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Created by SunYu on 2017/9/12.
 */
public interface DeviceValueRepository extends JpaRepository<DeviceValue, Long>, JpaSpecificationExecutor<DeviceValue> {
    List<DeviceValue> findByIdIn(List<Long> ids);

    List<DeviceValue> findByDeviceType(DeviceType deviceType);
}
