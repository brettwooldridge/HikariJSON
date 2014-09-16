package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class Phield
{
   public final Field field;
   public final Clazz clazz;
   public final Clazz collectionParameterClazz1;
   public final Clazz collectionParameterClazz2;
   public final boolean isCollection;
   public final boolean isMap;
   public final boolean isArray;
   public final boolean isPrimitive;
   public final long fieldOffset;
   public final int type;

   @SuppressWarnings("restriction")
   public Phield(final Field field) {
      Class<?> fieldClass = field.getType();
      this.field = field;
      this.field.setAccessible(true);
      this.fieldOffset = UnsafeHelper.getUnsafe().objectFieldOffset(field);
      this.isCollection = Collection.class.isAssignableFrom(fieldClass);
      this.isMap = Map.class.isAssignableFrom(fieldClass);
      this.isArray = fieldClass.isArray();
      this.isPrimitive = fieldClass.isPrimitive() || fieldClass == String.class;
      this.type = getType(field);

      if (isCollection || isMap) {
         clazz = null;
         Type genericType = field.getGenericType();
         if (genericType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            collectionParameterClazz1 = ClassUtils.reflect((Class<?>) actualTypeArguments[0]);
            collectionParameterClazz2 = (actualTypeArguments.length == 2) ? ClassUtils.reflect((Class<?>) actualTypeArguments[1]) : null;
            return;
         }
      }
      else if (isArray) {
         clazz = null;
         Type genericType = field.getGenericType();
         if (genericType instanceof Class<?>) {
            collectionParameterClazz1 = ClassUtils.reflect((Class<?>) genericType);
            collectionParameterClazz2 = null;
            return;
         }
      }
      else if (!fieldClass.getName().startsWith("java.")) {
         clazz = ClassUtils.reflect(fieldClass);
      }
      else {
         clazz = null;
      }

      collectionParameterClazz1 = null;
      collectionParameterClazz2 = null;
   }

   public Object newInstance() throws InstantiationException, IllegalAccessException
   {
      if (clazz != null) {
         return clazz.newInstance();
      }
      else if (isCollection || isArray) {
         return new ArrayList<Object>();
      }
      else if (isMap) {
         return new HashMap<Object, Object>();
      }

      return field.getClass().newInstance();
   }

   public Clazz getCollectionParameterClazz1()
   {
      return collectionParameterClazz1;
   }

   public Clazz getCollectionParameterClazz2()
   {
      return collectionParameterClazz2;
   }

   private int getType(Field field)
   {
      Class<?> type = field.getType();
      if (type == byte.class) {
         return Types.BYTE;
      }
      else if (type == char.class) {
         return Types.CHAR;
      }
      else if (type == short.class) {
         return Types.SHORT;
      }
      else if (type == int.class) {
         return Types.INT;
      }
      else if (type == long.class) {
         return Types.LONG;
      }
      else if (type == String.class) {
         return Types.STRING;
      }
      else if (type == boolean.class) {
         return Types.BOOLEAN;
      }
      else {
         return Types.OBJECT;
      }
   }

   @Override
   public String toString()
   {
      return (clazz != null ? clazz + " " + field : field.toString());
   }
}
