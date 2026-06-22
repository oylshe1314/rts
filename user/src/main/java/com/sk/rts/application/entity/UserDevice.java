package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import com.sk.rts.application.proto.caching.MsgUserDevice;
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
     * 设备编号
     */
    private String deviceNo;

    /**
     * 平台
     */
    private String platform;

    /**
     * 序列号
     */
    private String serialNo;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 客户端
     */
    private String caller;

    /**
     * 版本号
     */
    private String version;

    /**
     * 创建时间
     */
    private OffsetDateTime createTime;

    public MsgUserDevice toProto() {
        MsgUserDevice.Builder deviceBuilder = MsgUserDevice.newBuilder();
        deviceBuilder.setId(this.getId());
        deviceBuilder.setDeviceNo(this.getDeviceNo());
        deviceBuilder.setCaller(this.getCaller());
        deviceBuilder.setVersion(this.getVersion());
        deviceBuilder.setChannel(this.getChannel());
        deviceBuilder.setPlatform(this.getPlatform());
        deviceBuilder.setSerialNo(this.getSerialNo());
        deviceBuilder.setCreateTime(this.getCreateTime().toEpochSecond());
        return deviceBuilder.build();
    }

    public static UserDevice fromRow(Row row) {
        return fromRow(row, 0);
    }

    public static UserDevice fromRow(Row row, int indexOffset) {
        UserDevice device = new UserDevice();
        device.setId(row.getLong(indexOffset + 0));
        device.setUserId(row.getLong(indexOffset + 1));
        device.setDeviceNo(row.getString(indexOffset + 2));
        device.setPlatform(row.getString(indexOffset + 3));
        device.setSerialNo(row.getString(indexOffset + 4));
        device.setChannel(row.getString(indexOffset + 5));
        device.setCaller(row.getString(indexOffset + 6));
        device.setVersion(row.getString(indexOffset + 7));
        device.setCreateTime(row.getOffsetDateTime(indexOffset + 8));
        return device;
    }
}
