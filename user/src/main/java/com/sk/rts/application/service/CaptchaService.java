package com.sk.rts.application.service;

import com.sk.rts.application.component.builder.RedisKeyBuilder;
import com.sk.rts.application.config.SettingProperties;
import com.sk.rts.application.dto.CaptchaVerifyDto;
import com.sk.rts.application.entity.enums.CaptchaType;
import com.sk.rts.application.entity.redis.CaptchaData;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.util.RandomUtil;
import com.sk.rts.application.util.TimeUtil;
import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Slf4j
@Service
@NullMarked
@AllArgsConstructor
public class CaptchaService {

    private final Redis redis;

    private final RedisKeyBuilder redisKeyBuilder;

    private final JsonMapper jsonMapper;

    private final SettingProperties settingProperties;
    private final PasswordEncoder passwordEncoder;

    private Integer getCaptchaLength(CaptchaType captchaType) {
        Integer captchaLength = settingProperties.getCaptchaLength().get(captchaType);
        if (captchaLength == null) {
            captchaLength = settingProperties.getCaptchaDefaultLength();
        }

        if (captchaLength < 4 || captchaLength > 10) {
            log.error("验证码长度配置错误, captchaLength={}", captchaLength);
            throw new StandardStatusException(ResponseStatus.internal_error);
        }

        return captchaLength;
    }

    private Duration getCaptchaExpiration(CaptchaType captchaType) {
        Duration captchaExpiration = settingProperties.getCaptchaExpiration().get(captchaType);
        if (captchaExpiration == null) {
            captchaExpiration = settingProperties.getCaptchaDefaultExpiration();
        }

        long seconds = captchaExpiration.toSeconds();
        if (seconds < 60 || seconds > 1800) {
            log.error("验证码有效期配置错误, captchaExpiration={}s", seconds);
            throw new StandardStatusException(ResponseStatus.internal_error);
        }

        return captchaExpiration;
    }

    public Future<String> generate(Integer captchaType, String account) {
        CaptchaType type = CaptchaType.valueOf(captchaType);
        if (type == null) {
            return Future.failedFuture(new StandardStatusException(ResponseStatus.parameter_error));
        }

        Integer captchaLength = getCaptchaLength(type);
        Duration captchaExpiration = getCaptchaExpiration(type);

        String captcha = RandomUtil.randomNumbers(captchaLength);

        long now = TimeUtil.nowSeconds();

        CaptchaData data = new CaptchaData(        );
        data.setType(type.value());
        data.setKey(account);
        data.setValue(passwordEncoder.encode(captcha));
        data.setExpireTime(now + captchaExpiration.toSeconds());

        Request request = Request.cmd(Command.SET);
        request.arg(redisKeyBuilder.build("captcha", data.getType(), data.getKey()));
        request.arg(jsonMapper.writeValueAsString(data));
        request.arg("EXAT");
        request.arg(data.getExpireTime());

        if (!"dev".equals(settingProperties.getActive())) {
            captcha = "";
        }

        return redis.send(request).map(captcha);
    }

    public Future<Void> verify(Integer captchaType, String account, String captcha) {
        CaptchaType type = CaptchaType.valueOf(captchaType);
        if (type == null) {
            return Future.failedFuture(new StandardStatusException(ResponseStatus.parameter_error));
        }

        Request request = Request.cmd(Command.GET);
        request.arg(redisKeyBuilder.build("captcha", type.value(), account));

        long now = TimeUtil.nowSeconds();
        return redis.send(request).flatMap(response -> {
            if (response == null) {
                return Future.failedFuture(new StandardStatusException("user.captcha.incorrect", "验证码错误"));
            }

            CaptchaData data = jsonMapper.readValue(response.toBytes(), CaptchaData.class);
            if (now >= data.getExpireTime()) {
                return Future.failedFuture(new StandardStatusException("user.captcha.incorrect", "验证码错误"));
            }

            if (!passwordEncoder.matches(captcha, data.getValue())) {
                return Future.failedFuture(new StandardStatusException("user.captcha.incorrect", "验证码错误"));
            }

            return Future.succeededFuture();
        });
    }

    public Future<String> verify(CaptchaVerifyDto verifyDto) {
        return this.verify(verifyDto.getType(), verifyDto.getAccount(), verifyDto.getCaptcha()).flatMap(_ -> {
            String hash = RandomUtil.randomHash(9527L, 12138L);

            CaptchaData data = new CaptchaData();
            data.setType(verifyDto.getType());
            data.setKey(verifyDto.getAccount());
            data.setValue(passwordEncoder.encode(hash));
            data.setExpireTime(TimeUtil.nowMillis() + 120);

            Request request = Request.cmd(Command.SET);
            request.arg(redisKeyBuilder.build("verification", data.getType(), data.getKey()));
            request.arg(jsonMapper.writeValueAsString(data));
            request.arg("EXAT");
            request.arg(data.getExpireTime());

            return redis.send(request).map(hash);
        });
    }
}
