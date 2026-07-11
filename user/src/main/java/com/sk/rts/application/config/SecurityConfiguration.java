package com.sk.rts.application.config;

import com.sk.rts.application.auth.*;
import com.sk.rts.application.handler.UserAccessDeniedHandler;
import com.sk.rts.application.handler.UserLoginHandler;
import com.sk.rts.application.handler.UserLogoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

@Slf4j
@NullMarked
@AllArgsConstructor
@EnableWebFluxSecurity
@SpringBootConfiguration
public class SecurityConfiguration {

    public static final String PATTERN_DOC = "/doc/**";
    public static final String PATTERN_API_DOCS = "/v3/api-docs/**";
    public static final String PATTERN_ERROR = "/error/**";
    public static final String PATTERN_OPEN = "/open/**";
    public static final String PATH_AUTH_PASSWORD_LOGIN = "/auth/password/login";
    public static final String PATH_AUTH_CAPTCHA_LOGIN = "/auth/captcha/login";
    public static final String PATH_AUTH_LOGOUT = "/auth/logout";
    public static final String PATH_USER_PASSWORD_RESET = "/user/password/reset";

    private final UserPasswordAuthConverter userPasswordAuthConverter;
    private final UserPasswordAuthManager userPasswordAuthManager;

    private final UserCaptchaAuthConverter userCaptchaAuthConverter;
    private final UserCaptchaAuthManager userCaptchaAuthManager;

    private final UserLoginHandler userLoginHandler;
    private final UserLogoutHandler userLogoutHandler;
    private final UserAccessDeniedHandler userAccessDeniedHandler;
    private final UserSecurityContextRepository userSecurityContextRepository;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors(ServerHttpSecurity.CorsSpec::disable);
        httpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable);
        httpSecurity.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        httpSecurity.securityContextRepository(userSecurityContextRepository);
        httpSecurity.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(userAccessDeniedHandler));
        httpSecurity.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler(userAccessDeniedHandler));
        httpSecurity.authorizeExchange(exchangeSpec -> exchangeSpec
                .pathMatchers(PATTERN_DOC, PATTERN_API_DOCS, PATTERN_ERROR, PATTERN_OPEN, PATH_USER_PASSWORD_RESET).permitAll()
                .anyExchange().authenticated()

        );
        httpSecurity.logout(logout -> logout
                .logoutUrl(PATH_AUTH_LOGOUT)
                .logoutHandler(userLogoutHandler)
                .logoutSuccessHandler(userLogoutHandler)
        );

        httpSecurity.addFilterAt(authenticationWebFilter(PATH_AUTH_PASSWORD_LOGIN, userPasswordAuthManager, userPasswordAuthConverter), SecurityWebFiltersOrder.AUTHENTICATION);
        httpSecurity.addFilterAt(authenticationWebFilter(PATH_AUTH_CAPTCHA_LOGIN, userCaptchaAuthManager, userCaptchaAuthConverter), SecurityWebFiltersOrder.AUTHENTICATION);

        return httpSecurity.build();
    }

    private AuthenticationWebFilter authenticationWebFilter(String authPath, ReactiveAuthenticationManager manager, ServerAuthenticationConverter converter) {
        AuthenticationWebFilter webFilter = new AuthenticationWebFilter(manager);
        webFilter.setRequiresAuthenticationMatcher(new PathPatternParserServerWebExchangeMatcher(authPath));
        webFilter.setServerAuthenticationConverter(converter);
        webFilter.setAuthenticationSuccessHandler(userLoginHandler);
        webFilter.setAuthenticationFailureHandler(userLoginHandler);
        webFilter.setSecurityContextRepository(userSecurityContextRepository);
        return webFilter;
    }
}