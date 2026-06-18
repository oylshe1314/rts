package com.sk.rts.application.auth;

import com.sk.rts.application.entity.UserAccount;
import com.sk.rts.application.entity.UserDetails;
import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NullMarked
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthDetails implements org.springframework.security.core.userdetails.UserDetails {

    private MsgUserDetails details;
    private MsgUserDevice device;

    public long getUserId() {
        return details.getId();
    }

    public long getDeviceId() {
        return details.getId();
    }

    @Override
    public String getUsername() {
        return details.getUsername();
    }

    @Override
    public String getPassword() {
        return details.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    public UserAuthDetails(UserDetails details, UserAccount account, UserDevice device) {
        MsgUserDetails.Builder detailsBuilder = MsgUserDetails.newBuilder();
        detailsBuilder.setId(details.getId());
        detailsBuilder.setUsername(account.getUsername());
        detailsBuilder.setEmail(account.getEmail());
        detailsBuilder.setPhone(account.getPhone());
        detailsBuilder.setPassword(account.getPassword());
        detailsBuilder.setNickname(details.getNickname());
        detailsBuilder.setAvatar(details.getAvatar());
        detailsBuilder.setCreateTime(details.getCreateTime());
        this.details = detailsBuilder.build();

        MsgUserDevice.Builder deviceBuilder = MsgUserDevice.newBuilder();
        deviceBuilder.setId(device.getId());
        deviceBuilder.setDeviceNo(device.getDeviceNo());
        deviceBuilder.setCaller(device.getCaller());
        deviceBuilder.setVersion(device.getVersion());
        deviceBuilder.setChannel(device.getChannel());
        deviceBuilder.setPlatform(device.getPlatform());
        deviceBuilder.setSerialNo(device.getSerialNo());
    }
}
