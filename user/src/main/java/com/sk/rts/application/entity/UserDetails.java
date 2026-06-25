package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDetails extends BaseEntity {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 创建时间
     */
    private OffsetDateTime createTime;

    private UserAccount account;

    private UserDevice device;

    public static UserDetails fromRow(Row row) {
        return fromRow(row, 0);
    }

    public static UserDetails fromRow(Row row, int indexOffset) {
        UserDetails details = new UserDetails();
        details.setId(row.getLong(indexOffset + 0));
        details.setNickname(row.getString(indexOffset + 1));
        details.setAvatar(row.getString(indexOffset + 2));
        details.setGender(row.getInteger(indexOffset + 3));
        details.setBirthday(row.getLocalDate(indexOffset + 4));
        details.setCreateTime(row.getOffsetDateTime(indexOffset + 5));
        return details;
    }
}
