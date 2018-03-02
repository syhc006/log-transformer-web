package net.xicp.chocolatedisco.logtransformerweb.cotroller;

import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceTemplate;
import net.xicp.chocolatedisco.logtransformerweb.exception.DuplicateInformationException;
import net.xicp.chocolatedisco.logtransformerweb.exception.MissingInformationException;
import net.xicp.chocolatedisco.logtransformerweb.repository.DeviceTemplateRepository;
import net.xicp.chocolatedisco.logtransformerweb.repository.DeviceTypeRepository;
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
 * Created by SunYu on 2017/9/12.
 */
@RestController
@RequestMapping("/logtransformer")
public class DeviceTemplateController extends BaseController {
    @Autowired
    private DeviceTemplateRepository deviceTemplateRepository;
    @Autowired
    private DeviceTypeRepository deviceTypeRepository;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/deviceTemplates/{id}")
    public DeviceTemplate getDeviceTemplateById(@PathVariable(name = "id") Long id) {
        return deviceTemplateRepository.findById(id).get();
    }

    @GetMapping("/deviceTemplates")
    public Page<DeviceTemplate> queryPagedDeviceTemplatesByType(DeviceTemplate condition, Integer pageNum, Integer pageSize) {
        Optional<DeviceTemplate> optionalCondition = Optional.ofNullable(condition);
        Optional<Integer> optionalPageNum = Optional.ofNullable(pageNum);
        Optional<Integer> optionalPageSize = Optional.ofNullable(pageSize);
        PageRequest pageRequest = PageRequest.of(optionalPageNum.orElse(0), optionalPageSize.orElse(Integer.MAX_VALUE));
        Specification<DeviceTemplate> specification = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            optionalCondition.map(oc -> oc.getTemplate()).ifPresent(template -> list.add(cb.like(root.get("template").as(String.class), "%" + template + "%")));
            optionalCondition.map(oc -> oc.getDeviceType()).map(deviceType -> deviceType.getId()).ifPresent(id -> list.add(cb.equal(root.get("deviceType").get("id").as(Long.class), id)));
            optionalCondition.map(oc -> oc.getDeviceType()).map(deviceType -> deviceType.getType()).ifPresent(type -> list.add(cb.like(root.get("deviceType").get("type").as(String.class), "%" + type + "%")));
            Predicate[] p = new Predicate[list.size()];
            return cb.and(list.toArray(p));
        };
        return deviceTemplateRepository.findAll(specification, pageRequest);
    }

    @PostMapping("/deviceTemplates")
    @Transactional
    public DeviceTemplate addDeviceTemplate(@RequestBody @Validated(value = {AddValidationGroup.class}) DeviceTemplate deviceTemplate, BindingResult result) {
        Long deviceTypeId = Optional.ofNullable(deviceTemplate.getDeviceType().getId()).orElseThrow(() -> new MissingInformationException("请先选择设备类型"));
        boolean isDuplication = deviceTypeRepository.findById(deviceTypeId)
                .map(deviceType -> deviceType.getDeviceTemplates())
                .map(deviceTemplates -> deviceTemplates.stream().anyMatch(
                        deviceTemplate1 -> deviceTemplate1.getTemplate().equals(deviceTemplate.getTemplate())
                ))
                .orElse(false);
        if (isDuplication) {
            throw new DuplicateInformationException("模版已存在");
        }
        DeviceTemplate deviceTemplateInDb = deviceTemplateRepository.saveAndFlush(deviceTemplate);
        updateRedis(deviceTypeId);
        return deviceTemplateInDb;
    }

    @PutMapping("/deviceTemplates/{id}")
    @Transactional
    public DeviceTemplate editDeviceTemplateById(@PathVariable Long id, @RequestBody @Validated(value = {EditValidationGroup.class}) DeviceTemplate deviceTemplate, BindingResult result) {
        DeviceTemplate deviceTemplateInDb = deviceTemplateRepository.findById(id).orElseThrow(() -> new MissingInformationException("无此模版"));
        copyNonNullProperties(deviceTemplate, deviceTemplateInDb, "id", "deviceType");
        boolean isDuplication = deviceTypeRepository.findById(deviceTemplateInDb.getDeviceType().getId())
                .map(deviceType -> deviceType.getDeviceTemplates())
                .map(deviceTemplates -> deviceTemplates.stream().anyMatch(
                        deviceTemplate1 -> deviceTemplate1.getId() == deviceTemplateInDb.getId() ? false : deviceTemplate1.getTemplate().equals(deviceTemplateInDb.getTemplate())
                ))
                .orElse(false);
        if (isDuplication) {
            throw new DuplicateInformationException("模版已存在");
        }
        deviceTemplateRepository.saveAndFlush(deviceTemplateInDb);
        updateRedis(deviceTemplateInDb.getDeviceType().getId());
        return deviceTemplateInDb;
    }

    @DeleteMapping("/deviceTemplates/{ids}")
    @Transactional
    public List<Long> removeDeviceTemplateById(@PathVariable List<Long> ids) {
        List<DeviceTemplate> deviceTemplates = deviceTemplateRepository.findByIdIn(ids);
        Set<Long> deviceTypeIds = deviceTemplates.stream().map(deviceTemplate -> deviceTemplate.getDeviceType().getId()).collect(Collectors.toSet());
        deviceTemplateRepository.deleteInBatch(deviceTemplates);
        deviceTypeIds.stream().forEach(deviceTypeId -> updateRedis(deviceTypeId));
        return ids;
    }

    private void updateRedis(Long deviceTypeId) {
        deviceTypeRepository.findById(deviceTypeId).ifPresent(deviceType -> {
            List<String> templates = deviceTemplateRepository.findByDeviceType(deviceType).stream().map(template -> template.getTemplate()).collect(Collectors.toList());
            redisTemplate.opsForHash().put(deviceType.getType(), "templates", templates);
        });
    }
}
