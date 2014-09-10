package com.zaxxer.hikari.json.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassUtils
{
   public static final Class<?> LIST_TYPE = List.class;
   public static final Class<?> MAP_TYPE = Map.class;
   public static final Class<?> SET_TYPE = Set.class;

   public static final Map<Class<?>, Class<?>> JAVA_COLLECTION_TO_IMPL;

   static
   {
      JAVA_COLLECTION_TO_IMPL = new HashMap<>();
      JAVA_COLLECTION_TO_IMPL.put(LIST_TYPE, ArrayList.class);
      JAVA_COLLECTION_TO_IMPL.put(MAP_TYPE, HashMap.class);
      JAVA_COLLECTION_TO_IMPL.put(SET_TYPE, HashSet.class);
   }

   private ClassUtils()
   {
      // private constructor
   }

   public static Clazz reflect(Class<?> targetClass)
   {
      Clazz clazz = new Clazz(targetClass);
      return clazz;
   }
}
