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
@ConfigurationProperties(prefix = "rts.user.token")
public class TokenProperties {

    /**
     * 访问TOKEN
     */
    private AccessToken accessToken = new AccessToken();

    /**
     * 刷新TOKEN
     */
    private RefreshToken refreshToken = new RefreshToken();

    @Getter
    @Setter
    public static class AccessToken {

        /**
         * 安全KEY
         */
        private SecretKey secretKey = Keys.hmacShaKeyFor("d6af49e2962ee5700cfe776de1ea7794".getBytes());

        /**
         * 有效时间(小时)
         */
        private Duration expiration = Duration.ofHours(24);

        public void setSecretKey(String secretKey) {
            this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        }
    }

    @Getter
    @Setter
    public static class RefreshToken {

        /**
         * 是否启用刷新TOKEN
         */
        private Boolean enabled = false;

        /**
         * 重复刷新窗口期(秒)
         */
        private Duration repeatPhase = Duration.ofSeconds(60);

        /**
         * 有效时间(天)
         */
        private Duration expiration = Duration.ofDays(14);

        /**
         * 相对于有效时间提前刷新的时间(天), 需要小于有效时间，如值为3即提前3天刷新。
         */
        private Duration refreshAdvance = Duration.ofDays(3);
    }
}
