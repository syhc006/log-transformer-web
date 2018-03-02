package net.xicp.chocolatedisco.logtransformerweb.cotroller;

import com.alibaba.fastjson.JSONObject;
import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceValue;
import net.xicp.chocolatedisco.logtransformerweb.exception.DuplicateInformationException;
import net.xicp.chocolatedisco.logtransformerweb.exception.MissingInformationException;
import net.xicp.chocolatedisco.logtransformerweb.repository.DeviceTypeRepository;
import net.xicp.chocolatedisco.logtransformerweb.repository.DeviceValueRepository;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.AddValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.EditValidationGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by SunYu on 2017/9/14.
 */
@RestController
@RequestMapping("/logtransformer")
public class DeviceValueController extends BaseController {
    @Autowired
    private DeviceValueRepository deviceValueRepository;
    @Autowired
    private DeviceTypeRepository deviceTypeRepository;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/deviceValues/{id}")
    public DeviceValue getDeviceValueById(@PathVariable(name = "id") Long id) {
        return deviceValueRepository.findById(id).get();
    }

    @GetMapping("/deviceValues")
    public Page<DeviceValue> queryPagedDeviceValuesByType(DeviceValue condition, Integer pageNum, Integer pageSize) {
        Optional<DeviceValue> optionalCondition = Optional.ofNullable(condition);
        Optional<Integer> optionalPageNum = Optional.ofNullable(pageNum);
        Optional<Integer> optionalPageSize = Optional.ofNullable(pageSize);
        PageRequest pageRequest = PageRequest.of(optionalPageNum.orElse(0), optionalPageSize.orElse(Integer.MAX_VALUE));
        Specification<DeviceValue> specification = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            optionalCondition.map(oc -> oc.getStore()).ifPresent(nv -> list.add(cb.like(root.get("store").as(String.class), "%" + nv + "%")));
            optionalCondition.map(oc -> oc.getRaw()).ifPresent(rv -> list.add(cb.like(root.get("raw").as(String.class), "%" + rv + "%")));
            optionalCondition.map(oc -> oc.getField()).ifPresent(pn -> list.add(cb.like(root.get("field").as(String.class), "%" + pn + "%")));
            optionalCondition.map(oc -> oc.getDeviceType()).map(deviceType -> deviceType.getId()).ifPresent(id -> list.add(cb.equal(root.get("deviceType").get("id").as(Long.class), id)));
            optionalCondition.map(oc -> oc.getDeviceType()).map(deviceType -> deviceType.getType()).ifPresent(type -> list.add(cb.like(root.get("deviceType").get("type").as(String.class), "%" + type + "%")));
            Predicate[] p = new Predicate[list.size()];
            return cb.and(list.toArray(p));
        };
        return deviceValueRepository.findAll(specification, pageRequest);
    }

    @PostMapping("/deviceValues")
    @Transactional
    public DeviceValue addDeviceValue(@RequestBody @Validated(value = {AddValidationGroup.class}) DeviceValue deviceValue, BindingResult result) throws Exception {
        Long deviceTypeId = Optional.ofNullable(deviceValue.getDeviceType().getId()).orElseThrow(() -> new MissingInformationException("请先选择设备类型"));
        boolean isDuplication = deviceTypeRepository.findById(deviceTypeId)
                .map(deviceType -> deviceType.getDeviceValues())
                .map(deviceValues -> deviceValues.stream().anyMatch(
                        deviceValue1 -> deviceValue1.getField().equals(deviceValue.getField()) && deviceValue1.getRaw().equals(deviceValue.getRaw())
                ))
                .orElse(false);
        if (isDuplication) {
            throw new DuplicateInformationException("\"" + deviceValue.getField() + "\"已存在\"" + deviceValue.getRaw() + "\"对应的转换值");
        }
        DeviceValue deviceValueInDb = deviceValueRepository.saveAndFlush(deviceValue);
        updateRedis(deviceTypeId);
        return deviceValueInDb;
    }

    @PutMapping("/deviceValues/{id}")
    @Transactional
    public DeviceValue editDeviceValueById(@PathVariable Long id, @RequestBody @Validated(value = {EditValidationGroup.class}) DeviceValue deviceValue, BindingResult result) throws Exception {
        DeviceValue deviceValueInDb = deviceValueRepository.findById(id).orElseThrow(() -> new MissingInformationException("无此值"));
        copyNonNullProperties(deviceValue, deviceValueInDb, "id", "deviceType");
        boolean isDuplication = deviceTypeRepository.findById(deviceValueInDb.getDeviceType().getId())
                .map(deviceType -> deviceType.getDeviceValues())
                .map(deviceValues -> deviceValues.stream().anyMatch(
                        deviceValue1 -> deviceValue1.getId() == deviceValueInDb.getId() ? false :
                                deviceValue1.getField().equals(deviceValueInDb.getField()) &&
                                        deviceValue1.getRaw().equals(deviceValueInDb.getRaw())
                ))
                .orElse(false);
        if (isDuplication) {
            throw new DuplicateInformationException("\"" + deviceValueInDb.getField() + "\"已存在\"" + deviceValueInDb.getRaw() + "\"对应的转换值");
        }
        deviceValueRepository.saveAndFlush(deviceValueInDb);
        updateRedis(deviceValueInDb.getDeviceType().getId());
        return deviceValueInDb;
    }

    @DeleteMapping("/deviceValues/{ids}")
    @Transactional
    public List<Long> removeDeviceValueById(@PathVariable List<Long> ids) throws Exception {
        List<DeviceValue> deviceValues = deviceValueRepository.findByIdIn(ids);
        Set<Long> deviceTypeIds = deviceValues.stream().map(deviceValue -> deviceValue.getDeviceType().getId()).collect(Collectors.toSet());
        deviceValueRepository.deleteInBatch(deviceValues);
        deviceTypeIds.stream().forEach(deviceTypeId -> updateRedis(deviceTypeId));
        return ids;
    }

    private void updateRedis(Long deviceTypeId) {
        deviceTypeRepository.findById(deviceTypeId).ifPresent(deviceType -> {
            List<JSONObject> jsonObjects = deviceValueRepository.findByDeviceType(deviceType).stream().map(deviceValue -> {
                JSONObject json = (JSONObject) JSONObject.toJSON(deviceValue);
                json.remove("deviceType");
                json.remove("id");
                return json;
            }).collect(Collectors.toList());
            redisTemplate.opsForHash().put(deviceType.getType(), "values", jsonObjects);
        });
    }

}
