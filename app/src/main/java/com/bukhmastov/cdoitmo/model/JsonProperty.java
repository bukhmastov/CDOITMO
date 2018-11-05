package com.bukhmastov.cdoitmo.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonProperty {
    String value();
    /**
     * Задает порядок элементов для json сериализации.
     * Если указан 0, то порядок не применяется (будет случайный порядок).
     * Если указано число больше нуля, то число - номер в порядке элементов json.
     * Если для какого-либо элемента указан порядок, то все элементы без порядка будут добавлены в
     * конце в случайном порядке после всех элементов с указанным порядком.
     * Если для нескольких элементов будет указано одинаковое значение порядка, то будет
     * сериализовано только одно значение (случайным образом).
     */
    int order() default 0;
}
