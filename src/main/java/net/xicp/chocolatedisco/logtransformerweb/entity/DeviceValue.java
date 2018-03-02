package net.xicp.chocolatedisco.logtransformerweb.entity;

import net.xicp.chocolatedisco.logtransformerweb.validator.group.AddValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.EditValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.QueryValidationGroup;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.Pattern;

/**
 * Created by SunYu on 2017/9/12.
 */
@Entity
public class DeviceValue {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    @NotBlank(message = "待映射值不能为空", groups = {AddValidationGroup.class})
    @Pattern(regexp = "[^#%<>]{1,32}", message = "设备类型不能包含非法字符", groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Length(message = "设备类型长度不得超过32个字符", min = 1, max = 32, groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    private String raw;
    @Column(nullable = false)
    @NotBlank(message = "映射值不能为空", groups = {AddValidationGroup.class})
    @Pattern(regexp = "[^#%<>]{1,32}", message = "映射值不能包含非法字符", groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Length(message = "映射值长度不得超过32个字符", min = 1, max = 32, groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    private String store;
    @Column(nullable = false)
    @NotBlank(message = "字段不能为空", groups = {AddValidationGroup.class})
    @Pattern(regexp = "\\w{1,32}", message = "字段由字母数字下划线组成", groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    @Length(message = "字段长度不得超过32个字符", min = 1, max = 32, groups = {AddValidationGroup.class, EditValidationGroup.class, QueryValidationGroup.class})
    private String field;
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private DeviceType deviceType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}
