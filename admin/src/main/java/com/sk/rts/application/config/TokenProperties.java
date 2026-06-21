package com.sk.rts.application.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "admin.token")
public class TokenProperties {

    /**
     * 安全KEY(32字节)
     */
    private SecretKey secretKey = Keys.hmacShaKeyFor("d6af49e2962ee5700cfe776de1ea7794".getBytes());

    /**
     * 有效时间，默认3个小时
     */
    private Duration expiration = Duration.ofHours(3);

    /**
     * 设置安全KEY
     *
     * @param secretKey 字符串安全KEY
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
