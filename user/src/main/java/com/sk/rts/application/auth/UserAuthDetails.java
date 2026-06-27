package com.sk.rts.application.auth;

import com.sk.rts.application.entity.UserAccount;
import com.sk.rts.application.entity.UserDetails;
import com.sk.rts.application.entity.UserDevice;
import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
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
public class UserAuthDetails implements org.springframework.security.core.userdetails.UserDetails {

    private Long userId;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String nickname;
    private String avatar;
    private Long registerTime;

    private Long deviceId;
    private String platform;
    private String serialNo;
    private String deviceNo;
    private Long deviceTime;
    private String ipAddress;

    public UserAuthDetails(UserDetails details, UserRemoteDetails remoteDetails) {
        this(details, details.getAccount(), details.getDevice(), remoteDetails);
    }

    public UserAuthDetails(UserDetails details, UserAccount account, UserDevice device, UserRemoteDetails remoteDetails) {
        this.userId = details.getId();
        this.username = account.getUsername();
        this.email = account.getEmail();
        this.phone = account.getPhone();
        this.password = account.getPassword();
        this.nickname = details.getNickname();
        this.avatar = details.getAvatar();
        this.registerTime = details.getCreateTime().toEpochSecond();
        this.deviceId = device.getId();
        this.platform = device.getPlatform();
        this.serialNo = device.getSerialNo();
        this.deviceNo = device.getDeviceNo();
        this.deviceTime = device.getCreateTime().toEpochSecond();
        this.ipAddress = remoteDetails.getIpAddress();
    }

    public UserAuthDetails(MsgUserDetails details, MsgUserDevice device, UserRemoteDetails remoteDetails) {
        this.userId = details.getId();
        this.username = details.getUsername();
        this.email = details.getEmail();
        this.phone = details.getPhone();
        this.password = details.getPassword();
        this.nickname = details.getNickname();
        this.avatar = details.getAvatar();
        this.registerTime = details.getCreateTime();
        this.deviceId = device.getId();
        this.platform = device.getPlatform();
        this.serialNo = device.getSerialNo();
        this.deviceNo = device.getDeviceNo();
        this.deviceTime = device.getCreateTime();
        this.ipAddress = remoteDetails.getIpAddress();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptySet();
    }
}
