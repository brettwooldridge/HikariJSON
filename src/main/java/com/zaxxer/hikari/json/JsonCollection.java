package com.zaxxer.hikari.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Brett Wooldridge
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonCollection {
   @SuppressWarnings("rawtypes")
   Class<? extends Collection> collectionClass() default ArrayList.class;   
}
