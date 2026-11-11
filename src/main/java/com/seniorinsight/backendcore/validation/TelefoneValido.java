package com.seniorinsight.backendcore.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** O texto precisa ser um telefone válido (libphonenumber); sem código de país vale o Brasil. Nulo é aceito. */
@Documented
@Constraint(validatedBy = TelefoneValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TelefoneValido {

    String message() default "O telefone é inválido. Use DDD e número, por exemplo (11) 98765-4321.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
