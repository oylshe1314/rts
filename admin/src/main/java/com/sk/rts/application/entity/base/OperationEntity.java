package com.sk.rts.application.entity.base;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public abstract class OperationEntity extends BaseEntity {

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者(管理员)
     */
    private String createBy;

    /**
     * 创建时间
     */
    private OffsetDateTime createTime;

    /**
     * 修改者(管理员)
     */
    private String updateBy;

    /**
     * 修改时间
     */
    private OffsetDateTime updateTime;

    public void initOperation(String remark, String createBy) {
        this.setRemark(remark == null ? "" : remark);
        this.setCreateBy(createBy);
        this.setCreateTime(OffsetDateTime.now());
        this.setUpdateBy(this.getCreateBy());
        this.setUpdateTime(this.getCreateTime());
    }

    public void updateOperation(String remark, String updateBy) {
        if (remark != null) {
            this.setRemark(remark);
        }
        this.setUpdateBy(updateBy);
        this.setUpdateTime(OffsetDateTime.now());
    }
}
