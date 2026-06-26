package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDevice extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 平台
     */
    private String platform;

    /**
     * 序列号
     */
    private String serialNo;

    /**
     * 设备编号
     */
    private String deviceNo;

    /**
     * 创建时间
     */
    private OffsetDateTime createTime;

    public static UserDevice fromRow(Row row) {
        return fromRow(row, 0);
    }

    public static UserDevice fromRow(Row row, int indexOffset) {
        UserDevice device = new UserDevice();
        device.setId(row.getLong(indexOffset + 0));
        device.setUserId(row.getLong(indexOffset + 1));
        device.setPlatform(row.getString(indexOffset + 2));
        device.setSerialNo(row.getString(indexOffset + 3));
        device.setDeviceNo(row.getString(indexOffset + 4));
        device.setCreateTime(row.getOffsetDateTime(indexOffset + 5));
        return device;
    }
}
