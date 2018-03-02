package net.xicp.chocolatedisco.logtransformerweb.entity;

import net.xicp.chocolatedisco.logtransformerweb.validator.group.AddValidationGroup;
import net.xicp.chocolatedisco.logtransformerweb.validator.group.EditValidationGroup;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;

/**
 * Created by SunYu on 2017/9/12.
 */
@Entity
public class DeviceTemplate {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank(message = "模板不能为空", groups = {AddValidationGroup.class, EditValidationGroup.class})
    @Lob
    @Column(nullable = false)
    private String template;
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private DeviceType deviceType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}
