package com.sk.rts.application.component;

import com.sk.rts.application.auth.UserAccessToken;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.util.TimeUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.function.Function;

@Component
@NullMarked
@AllArgsConstructor
public class TokenUtil {

    private final TokenProperties tokenProperties;

    public static @Nullable String getTokenFromRequest(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(token)) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }

        if (token == null) {
            HttpCookie cookie = request.getCookies().getFirst("token");
            if (cookie != null) {
                token = cookie.getValue();
            }
        }

        if (token == null) {
            token = request.getQueryParams().getFirst("token");
        }

        if (token == null || token.length() < 100) {
            return null;
        }

        return token;
    }

    public String generate(String subject, OffsetDateTime issueTime, OffsetDateTime expireTime) {
        return Jwts.builder()
                .issuer("rts")
                .subject(subject)
                .issuedAt(TimeUtil.toDate(issueTime))
                .expiration(TimeUtil.toDate(expireTime))
                .signWith(tokenProperties.getAccessToken().getSecretKey())
                .compact();
    }

    private Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(tokenProperties.getAccessToken().getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> function) throws StandardStatusException {
        try {
            return function.apply(parse(token));
        } catch (ExpiredJwtException e) {
            throw new StandardStatusException(ResponseStatus.token_expired);
        } catch (JwtException e) {
            throw new StandardStatusException(ResponseStatus.token_invalid);
        }
    }

    public String extractSubject(String token) throws StandardStatusException {
        return extractClaim(token, Claims::getSubject);
    }
}
