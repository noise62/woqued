package sweetie.evaware.api.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    Category category();
    int bind() default -999;
}
