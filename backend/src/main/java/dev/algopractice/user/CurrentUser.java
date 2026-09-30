package dev.algopractice.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the {@link AppUser} making the request.
 * <p>
 * There are no accounts yet: the browser picks a handle and sends it in the
 * {@value UserResolver#HEADER} header. Replacing this with real authentication only
 * requires changing {@link UserResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
