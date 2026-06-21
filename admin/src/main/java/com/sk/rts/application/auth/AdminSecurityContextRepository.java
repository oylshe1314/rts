package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import com.sk.rts.application.proto.caching.MsgAdminToken;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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

    private final Redis redis;
    private final TokenUtil tokenUtil;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, @Nullable SecurityContext context) {
        if (context == null) {
            return Mono.error(new StandardStatusException(ResponseStatus.not_logged_in));
        }

        if (context.getAuthentication() instanceof AdminAuthToken authToken) {
            AdminAuthDetails authDetails = (AdminAuthDetails) authToken.getPrincipal();
            AdminAccessToken accessToken = (AdminAccessToken) authToken.getCredentials();

            MsgAdminToken.Builder msgAdminTokenBuilder = MsgAdminToken.newBuilder();
            msgAdminTokenBuilder.setUsername(accessToken.getUsername());
            msgAdminTokenBuilder.setToken(accessToken.getToken());
            msgAdminTokenBuilder.setIssueTime(accessToken.getIssueTime());
            msgAdminTokenBuilder.setExpiration(accessToken.getExpiration());
            msgAdminTokenBuilder.setRoleId(authDetails.getRoleId());
            msgAdminTokenBuilder.setAdminId(authDetails.getAdminId());

            MsgAdminToken msgAdminToken = msgAdminTokenBuilder.build();

            Request request = Request.cmd(Command.SET);
            request.arg(AdminAuthDetails.buildTokenKey(msgAdminToken.getUsername()));
            request.arg(msgAdminToken.toByteArray());
            request.arg(AdminAuthDetails.buildDetailsKey(authDetails.getUsername()));
            request.arg(authDetails.getAdminDetails().toByteArray());

            return Mono.create(sink -> redis.send(request).onSuccess(_ -> sink.success()).onFailure(sink::error));
        } else {
            return Mono.error(new StandardStatusException(ResponseStatus.access_denied));
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String token = TokenUtil.getTokenFromRequest(exchange.getRequest());
        if (token == null) {
            return Mono.empty();
        }

        try {
            String username = tokenUtil.extractSubject(token);

            Request request = Request.cmd(Command.GET);
            request.arg(AdminAuthDetails.buildTokenKey(username));
            request.arg(AdminAuthDetails.buildDetailsKey(username));

            return Mono.<SecurityContext>create(sink -> redis.send(request).onFailure(sink::error).onSuccess(response -> {
                try {
                    MsgAdminToken msgAdminToken = MsgAdminToken.parseFrom(response.get(0).toBytes());
                    if (msgAdminToken.getToken().equals(token)) {
                        MsgAdminDetails msgAdminDetails = MsgAdminDetails.parseFrom(response.get(1).toBytes());

                        sink.success(new SecurityContextImpl(new AdminAuthToken(new AdminAuthDetails(msgAdminDetails), new AdminAccessToken(msgAdminToken))));
                    } else {
                        sink.error(new StandardStatusException(ResponseStatus.token_expired));
                    }
                } catch (Exception e) {
                    sink.error(e);
                }
            })).cache();
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }
}
