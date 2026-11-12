package com.sk.rts.application.auth;

import com.sk.rts.application.component.builder.RedisKeyBuilder;
import com.sk.rts.application.component.token.TokenProperties;
import com.sk.rts.application.component.token.TokenUtil;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.util.ReactiveUtil;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
@NullMarked
@AllArgsConstructor
public class UserSecurityContextRepository implements ServerSecurityContextRepository {

    private final Redis redis;

    private final TokenUtil tokenUtil;
    private final TokenProperties tokenProperties;

    private final JsonMapper jsonMapper;

    private final RedisKeyBuilder redisKeyBuilder;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.empty();
        }

        if (context.getAuthentication() instanceof UserAuthToken authToken && authToken.isAuthenticated()) {
            AuthDetails authDetails = (AuthDetails) authToken.getPrincipal();
            TokenDetails tokenDetails = (TokenDetails) authToken.getCredentials();

            AuthUser authUser = authDetails.getUser();
            AuthDevice authDevice = authDetails.getDevice();

            UserAccessToken accessToken = tokenDetails.getAccessToken();
            UserRefreshToken refreshToken = tokenDetails.getRefreshToken();

            String hash = refreshToken.getHash();
            refreshToken.setHash(passwordEncoder.encode(hash));

            Request request1 = Request.cmd(Command.MSETEX);
            request1.arg(3);

            request1.arg(redisKeyBuilder.build("authUser", authUser.getId()));
            request1.arg(jsonMapper.writeValueAsString(authUser));

            request1.arg(redisKeyBuilder.build("authDevice", authDevice.getId()));
            request1.arg(jsonMapper.writeValueAsString(authDevice));

            request1.arg(redisKeyBuilder.build("accessToken", accessToken.getSubject()));
            request1.arg(jsonMapper.writeValueAsString(accessToken));

            request1.arg("EXAT");
            request1.arg(accessToken.getExpireTime());

            Request request2 = Request.cmd(Command.MSETEX);
            request2.arg(1);

            request2.arg(redisKeyBuilder.build("refreshToken", refreshToken.getSubject()));
            request2.arg(jsonMapper.writeValueAsString(refreshToken));

            request2.arg("EXAT");
            request2.arg(refreshToken.getExpireTime());

            refreshToken.setHash(hash);

            return ReactiveUtil.toMono(redis.batch(List.of(request1, request2)).mapEmpty());
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = TokenUtil.getTokenFromRequest(request);
        if (token == null) {
            return Mono.empty();
        }

        String subject;
        try {
            subject = tokenUtil.extractSubject(token);
        } catch (StandardStatusException e) {
            return Mono.error(e);
        }

        UserAuthDetails authDetails = new UserAuthDetails();
        UserAuthUtils.parseSubject(subject, authDetails);

        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setToken(subject);
        accessToken.setToken(token);

        return Mono.create(sink -> cacheService.queryUserAuthDetails(authDetails, accessToken).onFailure(sink::error).onSuccess(_ -> {
            UserRemoteDetails remoteDetails = new UserRemoteDetails(request);
            authDetails.setIpAddress(remoteDetails.getIpAddress());
            sink.success(new SecurityContextImpl(new UserAuthToken(authDetails, accessToken)));
        }));
    }
}
