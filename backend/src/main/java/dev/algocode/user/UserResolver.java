package dev.algocode.user;

import java.util.regex.Pattern;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/** Resolves {@link CurrentUser} parameters from the {@value #HEADER} header, creating users on first sight. */
@Component
public class UserResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Algocode-User";
    public static final String DEFAULT_HANDLE = "guest";
    private static final Pattern HANDLE = Pattern.compile("[A-Za-z0-9_.-]{2,32}");

    private final AppUserRepository users;

    public UserResolver(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AppUser.class);
    }

    @Override
    public AppUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String handle = request.getHeader(HEADER);
        return resolve(handle == null || handle.isBlank() ? DEFAULT_HANDLE : handle.strip());
    }

    public AppUser resolve(String handle) {
        if (!HANDLE.matcher(handle).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Handle must be 2-32 letters, digits, '.', '_' or '-'");
        }
        return users.findByHandle(handle).orElseGet(() -> {
            try {
                return users.saveAndFlush(new AppUser(handle));
            } catch (DataIntegrityViolationException raced) {
                return users.findByHandle(handle).orElseThrow();
            }
        });
    }
}
