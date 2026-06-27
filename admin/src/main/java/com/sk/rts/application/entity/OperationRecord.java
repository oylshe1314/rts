package com.sk.rts.application.entity;

import com.sk.rts.application.entity.base.BaseEntity;
import io.vertx.sqlclient.Row;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class OperationRecord extends BaseEntity {

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员(用户名)
     */
    private String operator;

    /**
     * 操作, login, logout, add, update, delete, changeState等
     */
    private String operation;

    /**
     * 操作参数，表名，登录账号等
     */
    private String arguments;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作员登录IP
     */
    private String ipAddress;

    /**
     * 操作时间
     */
    private OffsetDateTime createTime;

    public static OperationRecord fromRow(Row row) {
        OperationRecord record = new OperationRecord();
        record.setId(row.getLong(0));
        record.setOperatorId(row.getLong(1));
        record.setOperator(row.getString(2));
        record.setOperation(row.getString(3));
        record.setArguments(row.getString(4));
        record.setRemark(row.getString(5));
        record.setIpAddress(row.getString(6));
        record.setCreateTime(row.getOffsetDateTime(7));
        return record;
    }
}
