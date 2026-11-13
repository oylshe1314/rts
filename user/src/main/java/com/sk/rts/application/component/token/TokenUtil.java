package com.sk.rts.application.component.token;

import com.sk.rts.application.auth.AuthDetails;
import com.sk.rts.application.auth.UserAccessToken;
import com.sk.rts.application.auth.UserAuthUtil;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collection;

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

    public String generate(AuthDetails authDetails, OffsetDateTime issueTime, OffsetDateTime expireTime) {
        return Jwts.builder()
                .issuer("rts")
                .subject(authDetails.getSubject())
                .claim("n", authDetails.getUsername())
                .claim("a", authDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
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

    public AuthDetails verify(UserAccessToken accessToken) throws StandardStatusException {
        try {
            Claims claims = parse(accessToken.getToken());
            String subject = claims.getSubject();
            Long[] ids = UserAuthUtil.parseSubject(subject);

            AuthDetails authDetails = new AuthDetails();
            authDetails.setUserId(ids[0]);
            authDetails.setDeviceId(ids[1]);
            authDetails.setSubject(subject);
            authDetails.setUsername(claims.get("n", String.class));
            authDetails.setAuthorities(claims.get("a", Collection.class));

            accessToken.setUserId(authDetails.getUserId());
            accessToken.setDeviceId(authDetails.getDeviceId());
            accessToken.setSubject(authDetails.getSubject());
            accessToken.setIssueTime(TimeUtil.toEpochSeconds(claims.getIssuedAt()));
            accessToken.setExpireTime(TimeUtil.toEpochSeconds(claims.getExpiration()));

            return authDetails;
        } catch (ExpiredJwtException e) {
            throw new StandardStatusException(ResponseStatus.token_expired);
        } catch (Exception e) {
            throw new StandardStatusException(ResponseStatus.token_invalid);
        }
    }
}
