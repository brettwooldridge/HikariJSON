package com.zaxxer.hikari.json.util;

import java.util.HashMap;
import java.util.Map;

public final class ClassUtils
{
   public static final Map<Class<?>, Clazz> clazzMap;

   static
   {
      clazzMap = new HashMap<>();
   }

   private ClassUtils()
   {
      // private constructor
   }

   public static Clazz reflect(Class<?> targetClass)
   {
      Clazz clazz = clazzMap.get(targetClass);
      if (clazz == null) {
         clazz = new Clazz(targetClass);
         clazzMap.put(targetClass, clazz);

         clazz.parseFields();
      }

      return clazz;
   }
}
