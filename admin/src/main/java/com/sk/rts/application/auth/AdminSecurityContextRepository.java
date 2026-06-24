package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import com.sk.rts.application.proto.caching.MsgAdminToken;
import com.sk.rts.application.util.CodecUtil;
import com.sk.rts.application.util.FeistelUtil;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@NullMarked
@AllArgsConstructor
public class AdminSecurityContextRepository implements ServerSecurityContextRepository {

    private static final String QUERY_SCRIPT = """
            local token_data = redis.call('GET', KEYS[1])
            local admin_data = redis.call('GET', KEYS[2])
            if token_data and admin_data then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                redis.call('EXPIRE', KEYS[2], ARGV[1])
                return {token_data, admin_data}
            else
                if token_data then
                    redis.call('DEL', KEYS[1])
                end
                if admin_data then
                    redis.call('DEL', KEYS[2])
                end
                return nil
            end
            """;

    private final Redis redis;

    private final TokenUtil tokenUtil;
    private final TokenProperties tokenProperties;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.error(new StandardStatusException(ResponseStatus.not_logged_in));
        }

        if (context.getAuthentication() instanceof AdminAuthToken authToken) {
            AdminAuthDetails authDetails = (AdminAuthDetails) authToken.getPrincipal();
            AdminAccessToken accessToken = (AdminAccessToken) authToken.getCredentials();

            MsgAdminToken.Builder msgAdminTokenBuilder = MsgAdminToken.newBuilder();
            msgAdminTokenBuilder.setSubject(accessToken.getSubject());
            msgAdminTokenBuilder.setToken(accessToken.getToken());
            msgAdminTokenBuilder.setIssueTime(accessToken.getIssueTime());
            msgAdminTokenBuilder.setExpiration(accessToken.getExpiration());

            MsgAdminToken msgAdminToken = msgAdminTokenBuilder.build();

            MsgAdminDetails.Builder msgAdminDetailsBuilder = MsgAdminDetails.newBuilder();
            msgAdminDetailsBuilder.setId(authDetails.getId());
            msgAdminDetailsBuilder.setRoleId(authDetails.getRoleId());
            msgAdminDetailsBuilder.setRoleName(authDetails.getRoleName());
            msgAdminDetailsBuilder.setUsername(authDetails.getUsername());
            msgAdminDetailsBuilder.setPassword(authDetails.getPassword());
            msgAdminDetailsBuilder.setPhone(authDetails.getPhone());
            msgAdminDetailsBuilder.setEmail(authDetails.getEmail());
            msgAdminDetailsBuilder.setNickname(authDetails.getNickname());
            msgAdminDetailsBuilder.setAvatar(authDetails.getAvatar());
            for (ApiPathAuthority authority : authDetails.getAuthorities()) {
                msgAdminDetailsBuilder.addAuthority(authority.getAuthority());
            }

            Request request = Request.cmd(Command.MSETEX);
            request.arg(2);
            request.arg(AdminAuthDetails.buildTokenKey(msgAdminToken.getSubject()));
            request.arg(msgAdminTokenBuilder.build().toByteArray());
            request.arg(AdminAuthDetails.buildDetailsKey(authDetails.getId()));
            request.arg(msgAdminDetailsBuilder.build().toByteArray());
            request.arg("EX");
            request.arg(tokenProperties.getExpiration().toSeconds());

            return Mono.create(sink -> redis.send(request).onSuccess(_ -> sink.success()).onFailure(sink::error));
        } else {
            return Mono.error(new StandardStatusException(ResponseStatus.access_denied));
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = TokenUtil.getTokenFromRequest(request);
        if (token == null) {
            return Mono.empty();
        }

        try {
            String subject = tokenUtil.extractSubject(token);
            long adminId = FeistelUtil.decode(CodecUtil.decode64(subject));

            Request redisRequest = Request.cmd(Command.GET);
            redisRequest.arg(AdminAuthDetails.buildTokenKey(subject));
            redisRequest.arg(AdminAuthDetails.buildDetailsKey(adminId));

            return Mono.<SecurityContext>create(sink -> redis.send(redisRequest).onFailure(sink::error).onSuccess(response -> {
                try {
                    if (response.get(0) != null) {
                        MsgAdminToken msgAdminToken = MsgAdminToken.parseFrom(response.get(0).toBytes());
                        if (msgAdminToken.getToken().equals(token)) {
                            if (response.get(1) != null) {
                                MsgAdminDetails msgAdminDetails = MsgAdminDetails.parseFrom(response.get(1).toBytes());

                                AdminAuthToken authResult = new AdminAuthToken(new AdminAuthDetails(msgAdminDetails), new AdminAccessToken(msgAdminToken));
                                authResult.setDetails(new AdminRemoteDetails(request));

                                sink.success(new SecurityContextImpl(authResult));
                            }
                        }
                    }
                    sink.error(new StandardStatusException(ResponseStatus.token_expired));
                } catch (Exception e) {
                    sink.error(e);
                }
            })).cache();
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }
}
