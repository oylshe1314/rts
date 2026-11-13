package com.sk.rts.application.util;

import com.sk.rts.application.entity.enums.Platform;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import lombok.extern.slf4j.Slf4j;
import org.passay.*;
import org.passay.data.EnglishCharacterData;
import org.passay.data.EnglishSequenceData;
import org.passay.rule.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public final class CheckUtil {
    private static final PasswordValidator PASSWORD_VALIDATOR = new DefaultPasswordValidator(
            new LengthRule(8, 40), // 长度规则：8-40位
            new CharacterRule(EnglishCharacterData.UpperCase, 1), // 至少包含1个大写字母
            new CharacterRule(EnglishCharacterData.LowerCase, 1), // 至少包含1个小写字母
            new CharacterRule(EnglishCharacterData.Digit, 1), // 至少包含1个数字
            new CharacterRule(EnglishCharacterData.Special, 1), // 至少包含1个特殊字符
            new RepeatCharactersRule(4), // 不允许有4个连续相同字符
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 4, false), // 不允许4个连续字母（如"abcd"）
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 4, false), // 不允许4个连续数字（如"1234"）
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 4, false), // 不允许键盘上相邻的4个键（如"qwer"）
            new WhitespaceRule() // 不允许包含空格
            // new DictionaryRule(new WordListDictionary()) // 自定义字典规则：禁止常见弱密码
    );

    public static void checkDevice(String platform, String deviceId) throws AuthenticationException {
        if (Platform.parse(platform) == null) {
            log.debug("设备平台错误，platform={}", platform);
            throw new StandardStatusException(ResponseStatus.parameter_error);
        }

        if (!StringUtils.hasText(deviceId)) {
            log.debug("设备序列号为空，deviceId={}", deviceId);
            throw new StandardStatusException(ResponseStatus.parameter_error);
        }

        try {
            UUID.fromString(deviceId);
        } catch (Exception e) {
            log.debug("设备序列号错误，deviceId={}", deviceId);
            throw new StandardStatusException(ResponseStatus.parameter_error);
        }
    }

    public static void checkNickname(String nickname) throws StandardStatusException {
        if (nickname.getBytes(StandardCharsets.UTF_8).length > 18) {
            log.debug("昵称格式不正确，nickname={}", nickname);
            throw new StandardStatusException("user.nickname.format.incorrect", "昵称格式不正确");
        }
    }

    public static void checkUsername(String username) throws StandardStatusException {
        if (!username.matches("^[_0-9A-Za-z]{8,32}$")) {
            log.debug("用户名格式不正确，username={}", username);
            throw new StandardStatusException("user.username.format.incorrect", "用户名格式不正确");
        }
    }

    public static void checkPassword(String password) throws StandardStatusException {
        if (password.length() < 8 || password.length() > 40) {
            log.debug("密码格式不正确，password={}", password);
            throw new StandardStatusException("user.password.format.incorrect", "密码格式不正确");
        }
    }

    public static void safetyPassword(String password) throws StandardStatusException {
        ValidationResult result = PASSWORD_VALIDATOR.validate(new PasswordData(password));
        if (!result.isValid()) {
            log.debug("密码安全性过低，password={}, message={}", password, String.join(", ", result.getMessages()));
            throw new StandardStatusException("user.password.security.insufficient", "密码安全性过低");
        }
    }

    public static void checkPhone(String phone) throws StandardStatusException {
        if (!phone.matches("^\\+?\\d{1,4}:\\d{5,15}$")) {
            log.debug("手机号码格式不正确，phone={}", phone);
            throw new StandardStatusException("user.phone.format.incorrect", "手机号码格式不正确");
        }
    }

    public static void checkEmail(String email) throws StandardStatusException {
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            log.debug("邮箱格式不正确，email={}", email);
            throw new StandardStatusException("user.email.format.incorrect", "邮箱格式不正确");
        }
    }

    public static void checkAccount(String account) throws StandardStatusException {
        try {
            if (account.contains("@")) {
                checkEmail(account);
            } else {
                checkPhone(account);
            }
        } catch (StandardStatusException _) {
            try {
                checkUsername(account);
            } catch (StandardStatusException _) {
                throw new StandardStatusException("user.account.format.incorrect", "账号格式不正确");
            }
        }
    }

    public static boolean isEmail(String account) {
        return account.contains("@");
    }
}
