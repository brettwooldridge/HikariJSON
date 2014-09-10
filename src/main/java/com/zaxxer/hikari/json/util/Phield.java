package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class Phield
{
   private Field field;
   private Clazz fieldClazz;
   private Class<?> fieldClass;
   private boolean isCollection;

   public Phield(Field field)
   {
      this.field = field;
      fieldClass = field.getType();
      if (!fieldClass.getName().startsWith("java.lang")) {
         if (!fieldClass.getName().startsWith("java.util")) {
            fieldClazz = ClassUtils.reflect(fieldClass);
         }
         else {
            if (fieldClass.isInstance(Collection.class)) {
               Type genericType = field.getGenericType();
               System.err.println("Generic type: " + genericType);

               if (genericType instanceof ParameterizedType) {
                  Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                  System.err.println("actualTypeArguments: " + actualTypeArguments[0]);
               }
            }
         }
      }
   }
}
