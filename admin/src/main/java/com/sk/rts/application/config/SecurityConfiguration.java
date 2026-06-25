package com.sk.rts.application.config;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.handler.AdminAccessDeniedHandler;
import com.sk.rts.application.handler.AdminLoginHandler;
import com.sk.rts.application.handler.AdminLogoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.publisher.Mono;

@Slf4j
@NullMarked
@AllArgsConstructor
@EnableWebFluxSecurity
@SpringBootConfiguration
public class SecurityConfiguration {

    public static final String PATTERN_DOC = "/doc/**";
    public static final String PATTERN_API_DOC = "/v3/api-docs/**";
    public static final String PATTERN_ERROR = "/error/**";
    public static final String PATTERN_OPEN = "/open/**";
    public static final String PATH_USER_PASSWORD_RESET = "/admin/password/reset";
    public static final String PATTERN_COMMON = "/common/**";
    public static final String PATH_AUTH_LOGIN = "/auth/login";
    public static final String PATH_AUTH_LOGOUT = "/auth/logout";

    private final PathMatcher pathMatcher;
    private final AdminAuthConverter adminAuthConverter;
    private final AdminAuthManager adminAuthManager;
    private final AdminLoginHandler adminLoginHandler;
    private final AdminLogoutHandler adminLogoutHandler;
    private final AdminAccessDeniedHandler adminAccessDeniedHandler;
    private final AdminSecurityContextRepository adminSecurityContextRepository;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors(ServerHttpSecurity.CorsSpec::disable);
        httpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable);
        httpSecurity.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        httpSecurity.securityContextRepository(adminSecurityContextRepository);
        httpSecurity.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(adminAccessDeniedHandler));
        httpSecurity.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler(adminAccessDeniedHandler));
        httpSecurity.authorizeExchange(exchangeSpec -> exchangeSpec
                .pathMatchers(PATTERN_DOC, PATTERN_API_DOC, PATTERN_ERROR, PATTERN_OPEN, PATH_USER_PASSWORD_RESET).permitAll()
                .pathMatchers(PATTERN_COMMON).authenticated()
                .anyExchange().access(this::apiPathAuthorize)
        );
        httpSecurity.logout(logout -> logout
                .logoutUrl(PATH_AUTH_LOGOUT)
                .logoutHandler(adminLogoutHandler)
                .logoutSuccessHandler(adminLogoutHandler)
        );

        httpSecurity.addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION);

        return httpSecurity.build();
    }

    private AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter webFilter = new AuthenticationWebFilter(adminAuthManager);
        webFilter.setRequiresAuthenticationMatcher(new PathPatternParserServerWebExchangeMatcher(PATH_AUTH_LOGIN));
        webFilter.setServerAuthenticationConverter(adminAuthConverter);
        webFilter.setAuthenticationSuccessHandler(adminLoginHandler);
        webFilter.setAuthenticationFailureHandler(adminLoginHandler);
        webFilter.setSecurityContextRepository(adminSecurityContextRepository);
        return webFilter;
    }

    private Mono<AuthorizationResult> apiPathAuthorize(Mono<Authentication> authenticationMono, AuthorizationContext context) {
        String reqPath = context.getExchange().getRequest().getPath().pathWithinApplication().value();
        return authenticationMono.map(authentication -> {
            if (!authentication.isAuthenticated()) {
                log.debug("用户未认证, name={}", authentication.getName());
                return new AuthorizationDecision(false);
            }

            if (!(authentication instanceof AdminAuthToken)) {
                log.debug("用户未登录, name={}", authentication.getName());
                return new AuthorizationDecision(false);
            }

            if (authentication.getAuthorities().stream().anyMatch(authority -> {
                if (authority instanceof ApiPatternAuthority) {
                    return pathMatcher.match(authority.getAuthority(), reqPath);
                }
                return false;
            })) {
                log.debug("用户通过鉴权, name={}", authentication.getName());
                return new AuthorizationDecision(true);
            }

            log.debug("用户权限不足,  name={}", authentication.getName());
            return new AuthorizationDecision(false);
        });
    }
}