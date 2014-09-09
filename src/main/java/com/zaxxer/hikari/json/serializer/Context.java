package com.zaxxer.hikari.json.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static com.zaxxer.hikari.json.util.ClassUtils.JAVA_COLLECTION_TO_IMPL;

public class Context
{
   public Object target;
   public Class<?> targetType;
   
   public Context(Class<?> targetType)
   {
      this.targetType = targetType;
   }

   public void createInstance() throws InstantiationException, IllegalAccessException
   {
      Class<?> instanceClass = JAVA_COLLECTION_TO_IMPL.get(targetType);
      if (instanceClass != null) {
         target = instanceClass.newInstance();
      }
      else {
         target = targetType.newInstance();
      }
   }

   public Class<?> getFieldType(String fieldName)
   {
      try {
         Field declaredField = targetType.getDeclaredField(fieldName);
         Type genericType = declaredField.getGenericType();
         if (genericType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) genericType).getRawType();
         }
         return declaredField.getType();
      }
      catch (NoSuchFieldException | SecurityException e) {
         throw new RuntimeException(e);
      }
   }
}
