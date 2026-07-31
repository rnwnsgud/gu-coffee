package com.coffee.gu.auth;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;


@Component
public class PrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PRINCIPAL_ID_HEADER = "Gu-Coffee-com.coffee.gu.Principal-Id";
    private static final String PRINCIPAL_TYPE_HEADER = "Gu-Coffee-com.coffee.gu.Principal-Type";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(Principal.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) throw new CoreException(ErrorType.INVALID_REQUEST, null);
        Authenticated annotation = parameter.getParameterAnnotation(Authenticated.class);

        String id = request.getHeader(PRINCIPAL_ID_HEADER);
        String type = request.getHeader(PRINCIPAL_TYPE_HEADER);

        validatePrincipal(annotation, id, type);

        return new Principal(id, type);

    }

    private void validatePrincipal(Authenticated annotation, String id, String type) {
        boolean isRequired = (annotation != null && annotation.required());

        if (isRequired) {
            if (id == null) throw new CoreException(ErrorType.UNAUTHORIZED, null);
            if (!PrincipalType.USER.name().equals(type)) {
                throw new CoreException(ErrorType.UNAUTHORIZED, "회원 전용 서비스입니다.");
            }
        }
        if (type == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST, null);
        }

    }
}
