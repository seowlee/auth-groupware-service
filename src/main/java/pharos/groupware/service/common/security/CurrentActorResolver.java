package pharos.groupware.service.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import pharos.groupware.service.common.annotation.CurrentActor;

@Component
@RequiredArgsConstructor
public class CurrentActorResolver implements HandlerMethodArgumentResolver {

    private final CurrentActorProvider provider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentActor.class)
                && parameter.getParameterType().equals(AppUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return provider.get();
    }
}
