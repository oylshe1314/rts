package com.sk.rts.application.aop;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@Aspect
@Component
@NullMarked
@AllArgsConstructor
public class I18nAspect {

    private final JsonMapper jsonMapper;

    public String translateValue(String value, List<Locale.LanguageRange> ranges) {
        if (StringUtils.hasText(value) || value.startsWith("{")) {
            try {
                Map<String, String> supported = jsonMapper.readValue(value, new TypeReference<LinkedHashMap<String, String>>() {
                });

                if (supported.isEmpty()) {
                    return "";
                }

                if (supported.size() == 1) {
                    return supported.values().iterator().next();
                }

                List<String> filtered = Locale.filterTags(ranges, supported.keySet());
                for (String tag : filtered) {
                    String localizationValue = supported.get(tag);
                    if (localizationValue != null) {
                        return localizationValue;
                    }
                }
                return supported.values().iterator().next();
            } catch (JacksonException e) {
                return value;
            }
        }
        return value;
    }

    private void translateDto(@Nullable Object dto, List<Locale.LanguageRange> ranges) {
        if (dto == null) {
            return;
        }

        Class<?> dtoClass = dto.getClass();
        for (Method method : dtoClass.getMethods()) {
            I18nField i18nField = method.getAnnotation(I18nField.class);
            if (i18nField == null) {
                continue;
            }

            String methodName = method.getName();
            if (methodName.startsWith("set")) {
                Type[] parameterTypes = method.getGenericParameterTypes();
                if (parameterTypes.length != 1) {
                    continue;
                }

                Type parameterType = parameterTypes[0];
                try {
                    if (parameterType instanceof Class<?> parameterClass) {
                        if (String.class.isAssignableFrom(parameterClass)) {
                            Method getter = dtoClass.getMethod(methodName.replace("set", "get"));
                            String value = (String) getter.invoke(dto);
                            if (value != null) {
                                method.invoke(dto, translateValue(value, ranges));
                            }
                        }
                    } else {
                        if (parameterType instanceof ParameterizedType parameterizedType) {
                            parameterType = parameterizedType.getRawType();
                            if (parameterType instanceof Class<?> parameterClass && Collection.class.isAssignableFrom(parameterClass)) {
                                Type[] argTypes = parameterizedType.getActualTypeArguments();
                                if (argTypes.length == 1 && argTypes[0] instanceof Class<?> elementClass && String.class.isAssignableFrom(elementClass)) {
                                    Method getter = dtoClass.getMethod(methodName.replace("set", "get"));
                                    Collection<String> values = (Collection<String>) getter.invoke(dto);
                                    if (!CollectionUtils.isEmpty(values)) {
                                        method.invoke(dto, values.stream().map(value -> translateValue(value, ranges)).toList());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            } else {
                if (methodName.startsWith("get")) {
                    Type returnType = method.getGenericReturnType();
                    try {
                        if (returnType instanceof Class<?>) {
                            translateDto(method.invoke(dto), ranges);
                        } else {
                            if (returnType instanceof ParameterizedType parameterizedType) {
                                returnType = parameterizedType.getRawType();
                                if (returnType instanceof Class<?> returnClass && Collection.class.isAssignableFrom(returnClass)) {
                                    translateDtoList((Collection<?>) method.invoke(dto), ranges);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void translateDtoList(@Nullable Collection<?> dtoList, List<Locale.LanguageRange> ranges) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return;
        }

        for (Object dto : dtoList) {
            translateDto(dto, ranges);
        }
    }

    private Locale getLocale(ContextView contextView) {
        Locale locale = contextView.get(ServerWebExchange.class).getLocaleContext().getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    /**
     * 国际化处理
     *
     * @param joinPoint 切入点参数
     * @return 返回结果
     */
    @Around("@annotation(com.bds.user.application.aop.I18nMethod)")
    public Mono<?> i18nProcess(ProceedingJoinPoint joinPoint) throws Throwable {
        Mono<?> result = (Mono<?>) joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (log.isDebugEnabled()) {
            log.debug("返回数据国际化处理, method={}.{}", method.getDeclaringClass().getSimpleName(), method.getName());
        }

        I18nMethod i18nMethod = method.getAnnotation(I18nMethod.class);
        if (i18nMethod != null) {
            return Mono.deferContextual(contextView -> {
                Locale locale = this.getLocale(contextView);
                List<Locale.LanguageRange> ranges = List.of(
                        new Locale.LanguageRange(locale.toLanguageTag(), 1.0),
                        new Locale.LanguageRange("en", 0.1)
                );
                return result.doOnNext(value -> {
                    if (value instanceof Collection<?> values) {
                        this.translateDtoList(values, ranges);
                    } else {
                        this.translateDto(value, ranges);
                    }
                });
            });
        }
        return result;
    }
}
