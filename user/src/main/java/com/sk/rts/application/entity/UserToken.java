package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import com.sk.rts.application.proto.caching.MsgRefreshToken;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserToken extends BaseEntity {

    /**
     * 用户表ID
     */
    private Long userId;

    /**
     * 设备表ID
     */
    private Long deviceId;

    /**
     * HASH
     */
    private String hash;

    /**
     * 状态，1：有效，0：失效
     */
    private int status;

    /**
     * 发布时间
     */
    private OffsetDateTime issueTime;

    /**
     * 过期时间
     */
    private OffsetDateTime expireTime;

    /**
     * 刷新时间
     */
    private OffsetDateTime refreshTime;

    public static UserToken fromRow(Row row) {
        return fromRow(row, 0);
    }

    public static UserToken fromRow(Row row, int indexOffset) {
        UserToken token = new UserToken();
        token.setId(row.getLong(indexOffset + 0));
        token.setUserId(row.getLong(indexOffset + 1));
        token.setDeviceId(row.getLong(indexOffset + 2));
        token.setHash(row.getString(indexOffset + 3));
        token.setStatus(row.getInteger(indexOffset + 4));
        token.setIssueTime(row.getOffsetDateTime(indexOffset + 5));
        token.setExpireTime(row.getOffsetDateTime(indexOffset + 6));
        token.setRefreshTime(row.getOffsetDateTime(indexOffset + 7));
        return token;
    }
}
