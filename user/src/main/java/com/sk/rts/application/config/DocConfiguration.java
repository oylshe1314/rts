package com.sk.rts.application.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.PathMatcher;

@Configuration
@AllArgsConstructor
public class DocConfiguration {

    private final PathMatcher pathMatcher;

    @Bean
    public OpenAPI customOpenAPI(BuildProperties buildProperties) {
        OpenAPI openAPI = new OpenAPI();

        // 设置API基本信息
        openAPI.info(new Info().title("用户服务端接口").version(buildProperties.getVersion()).description("用户服务端相关接口"));
        openAPI.components(new Components()
                        // 定义全局头部语言参数：Accept-Language
                        .addParameters("Accept-Language", new Parameter()
                                .name("Accept-Language")  // HTTP header名称
                                .in("header")  // 参数位置：请求头
                                .description("客户端语言偏好，如：zh, en")  // 参数描述
                                .required(false))  // 非必填
                        // 定义JWT认证安全方案
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .scheme("bearer")  // 认证方案：bearer token
                                .name("Authorization")   // HTTP header名称
                                .bearerFormat("JWT")  // token格式：JWT
                                .description("JWT认证TOKEN")  // 认证描述
                                .type(SecurityScheme.Type.HTTP)
                                .in(SecurityScheme.In.HEADER)))  // token位置：HTTP header
                .addSecurityItem(new SecurityRequirement().addList("JWT"));

        return openAPI;
    }

    @Bean
    public OpenApiCustomizer applyGlobalHeaders() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {

            // 为所有接口添加Accept-Language头部参数（引用Components中定义的参数）
            operation.addParametersItem(new Parameter().$ref("#/components/parameters/Accept-Language"));
            if (pathMatcher.match(SecurityConfiguration.PATTERN_DOC, path)) {
                return; //文档
            }
            if (pathMatcher.match(SecurityConfiguration.PATTERN_API_DOCS, path)) {
                return; //接口文档
            }
            if (pathMatcher.match(SecurityConfiguration.PATTERN_ERROR, path)) {
                return; //错误
            }
            if (pathMatcher.match(SecurityConfiguration.PATTERN_OPEN, path)) {
                return; //分开接口
            }
            if (pathMatcher.match(SecurityConfiguration.PATH_AUTH_PASSWORD_LOGIN, path)) {
                return; //密码登录
            }
            if (pathMatcher.match(SecurityConfiguration.PATH_AUTH_CAPTCHA_LOGIN, path)) {
                return; //验证码登录
            }
            if (pathMatcher.match(SecurityConfiguration.PATH_AUTH_TOKEN_REFRESH, path)) {
                return; //TOKEN刷新
            }
            if (pathMatcher.match(SecurityConfiguration.PATH_AUTH_LOGOUT, path)) {
                return; //用户登出
            }
            if (pathMatcher.match(SecurityConfiguration.PATH_USER_PASSWORD_RESET, path)) {
                return; //用户密码重置
            }
            // 以上接口不需要认证

            // 以下为需要认证的接口添加JWT安全要求
            operation.addSecurityItem(new SecurityRequirement().addList("JWT"));  // 引用Components中定义的安全方案
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Authorization")
                    .description("JWT认证TOKEN，格式：Bearer {token}, 当前字段仅表示需要此参数，实际认证请点击右上角Authorize按钮")
                    .required(false)
                    .schema(new StringSchema())
            );
        }));
    }
}
