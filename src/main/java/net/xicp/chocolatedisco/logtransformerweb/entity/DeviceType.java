package net.xicp.chocolatedisco.logtransformerweb.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.AddValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.EditValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.QueryValidationGroup;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by SunYu on 2018/3/1.
 */
@Entity
public class DeviceType {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank(message = "设备类型名称不能为空", groups = {AddValidationGroup.class})
    @Pattern(regexp = "\\w{1,32}", message = "设备类型由字母数字下划线组成", groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Length(message = "设备类型长度不得超过32个字符", min = 1, max = 32, groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Column(nullable = false, unique = true)
    private String type;
    @NotBlank(message = "设备类型描述不能为空", groups = {AddValidationGroup.class})
    @Pattern(regexp = "[^#%<>]{1,32}", message = "设备类型描述不能包含非法字符", groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Length(message = "设备类型长度不得超过32个字符", min = 1, max = 32, groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Column(nullable = true)
    private String description;
    @JsonIgnore
    @JSONField(serialize = false)
    @OneToMany(mappedBy = "deviceType")
    private Set<DeviceTemplate> deviceTemplates = new HashSet<>(0);
    @JsonIgnore
    @JSONField(serialize = false)
    @OneToMany(mappedBy = "deviceType")
    private Set<DeviceValue> deviceValues = new HashSet<>(0);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<DeviceTemplate> getDeviceTemplates() {
        return deviceTemplates;
    }

    public void setDeviceTemplates(Set<DeviceTemplate> deviceTemplates) {
        this.deviceTemplates = deviceTemplates;
    }

    public Set<DeviceValue> getDeviceValues() {
        return deviceValues;
    }

    public void setDeviceValues(Set<DeviceValue> deviceValues) {
        this.deviceValues = deviceValues;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
