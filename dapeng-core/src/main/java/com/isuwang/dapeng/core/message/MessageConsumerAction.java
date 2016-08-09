package com.isuwang.dapeng.core.message;

import java.lang.annotation.*;

/**
 * Created by tangliu on 2016/8/3.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MessageConsumerAction {

    String groupId() default "";

    String topic() default "";

    String zkHost() default "";

}
