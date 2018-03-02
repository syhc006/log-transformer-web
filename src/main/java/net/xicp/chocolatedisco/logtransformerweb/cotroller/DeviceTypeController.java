package net.xicp.chocolatedisco.logtransformerweb.cotroller;

import com.alibaba.fastjson.JSONObject;
import io.thekraken.grok.api.Grok;
import io.thekraken.grok.api.Match;
import io.thekraken.grok.api.exception.GrokException;
import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceType;
import net.xicp.chocolatedisco.logtransformerweb.entity.DeviceValue;
import net.xicp.chocolatedisco.logtransformerweb.exception.DuplicateInformationException;
import net.xicp.chocolatedisco.logtransformerweb.exception.MissingInformationException;
import net.xicp.chocolatedisco.logtransformerweb.repository.DeviceTypeRepository;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.AddValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.EditValidationGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by SunYu on 2018/3/1.
 */
@RestController
@RequestMapping("/logtransformer")
public class DeviceTypeController extends BaseController {
    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    @GetMapping("/deviceTypes/{id}")
    public DeviceType getDeviceTypeById(@PathVariable Long id) {
        return deviceTypeRepository.findById(id).get();
    }

    @GetMapping("/deviceTypes")
    public Page<DeviceType> queryPagedDeviceTypesByCondition(DeviceType condition, Integer pageNum, Integer pageSize) {
        Optional<DeviceType> optionalCondition = Optional.ofNullable(condition);
        Optional<Integer> optionalPageNum = Optional.ofNullable(pageNum);
        Optional<Integer> optionalPageSize = Optional.ofNullable(pageSize);
        PageRequest pageRequest = PageRequest.of(optionalPageNum.orElse(0), optionalPageSize.orElse(Integer.MAX_VALUE));
        Specification<DeviceType> specification = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            optionalCondition.map(oc -> oc.getType()).ifPresent(type -> list.add(cb.like(root.get("type").as(String.class), "%" + type + "%")));
            optionalCondition.map(oc -> oc.getDescription()).ifPresent(description -> list.add(cb.like(root.get("description").as(String.class), "%" + description + "%")));
            Predicate[] p = new Predicate[list.size()];
            return cb.and(list.toArray(p));
        };
        return deviceTypeRepository.findAll(specification, pageRequest);
    }

    @PostMapping("/deviceTypes")
    @Transactional
    public DeviceType addDeviceType(@RequestBody @Validated(value = {AddValidationGroup.class}) DeviceType deviceType, BindingResult result) {
        boolean isDuplication = deviceTypeRepository.findAll().stream().anyMatch(deviceType1 -> deviceType1.getType().equals(deviceType.getType()));
        if (isDuplication) {
            throw new DuplicateInformationException("设备类型\"" + deviceType.getType() + "\"已存在");
        }
        DeviceType deviceTypeInDb = deviceTypeRepository.saveAndFlush(deviceType);
        return deviceTypeInDb;
    }

    @PutMapping("/deviceTypes/{id}")
    @Transactional
    public DeviceType editDeviceTypeById(@PathVariable Long id, @RequestBody @Validated(value = {EditValidationGroup.class}) DeviceType deviceType, BindingResult result) {
        DeviceType deviceTypeInDb = deviceTypeRepository.findById(id).orElseThrow(() -> new MissingInformationException("无此设备类型"));
        copyNonNullProperties(deviceType, deviceTypeInDb, "id");
        boolean isDuplication = deviceTypeRepository.findAll().stream().anyMatch(
                deviceType1 -> deviceType1.getId() == deviceTypeInDb.getId() ? false : deviceType1.getType().equals(deviceTypeInDb.getType()));
        if (isDuplication) {
            throw new DuplicateInformationException("设备类型\"" + deviceTypeInDb.getType() + "\"已存在");
        }
        deviceTypeRepository.save(deviceTypeInDb);
        return deviceTypeInDb;
    }

    @DeleteMapping("/deviceTypes/{ids}")
    @Transactional
    public List<Long> removeDeviceTypeById(@PathVariable List<Long> ids) throws Exception {
        List<DeviceType> deviceTypes = deviceTypeRepository.findByIdIn(ids);
        for (DeviceType deviceType : deviceTypes) {
            if (deviceType.getDeviceTemplates().size() != 0 || deviceType.getDeviceValues().size() != 0) {
                throw new Exception("此设备类型存在模版或值映射关系，请先将这些信息删除");
            }
        }
        deviceTypeRepository.deleteInBatch(deviceTypes);
        return ids;
    }

    @GetMapping("/deviceTypes/transform")
    public List<JSONObject> transform(@RequestParam String log, @RequestParam Long deviceTypeId) {
        Set<String> templates = deviceTypeRepository.findById(deviceTypeId)
                .map(deviceType -> deviceType.getDeviceTemplates())
                .map(deviceTemplates -> deviceTemplates.stream().map(deviceTemplate -> deviceTemplate.getTemplate()).collect(Collectors.toSet()))
                .orElse(new HashSet<>());
        Set<DeviceValue> values = deviceTypeRepository.findById(deviceTypeId)
                .map(deviceType -> deviceType.getDeviceValues())
                .orElse(new HashSet<>());
        List<JSONObject> results = new LinkedList<>();
        templates.stream().anyMatch(template -> {
            try {
                Grok grok = Grok.create(this.getClass().getClassLoader().getResource("grokpatterns").getPath(), template);
                grok.compile(template);
                Match gm = grok.match(log);
                gm.captures();
                if (gm.toMap().size() != 0) {
                    JSONObject result = new JSONObject(gm.toMap());
                    values.stream().forEach(value -> {
                        String field = value.getField();
                        String raw = value.getRaw();
                        String store = value.getStore();
                        String real = result.getString(field);
                        if (real != null && real.equals(raw)) {
                            result.put(field, store);
                        }
                    });
                    results.add(result);
                    return true;
                } else {
                    return false;
                }
            } catch (GrokException e) {
                return false;
            }
        });
        return results;
    }
}
