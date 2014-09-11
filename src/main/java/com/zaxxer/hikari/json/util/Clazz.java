package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Clazz
{
   private Class<?> actualClass;
   private final Map<String, Phield> fields;

   public Clazz(Class<?> clazz)
   {
      this.actualClass = clazz;
      this.fields = new HashMap<>();
   }

   void parseFields()
   {
      for (Field field : actualClass.getDeclaredFields()) {
         if (!Modifier.isStatic(field.getModifiers())) {
            fields.put(field.getName(), new Phield(field));
         }
      }
   }

   public Class<?> getActualClass() {
      return actualClass;
   }

   public Phield getPhield(String name) {
      return fields.get(name);
   }

   @Override
   public String toString()
   {
      return "Clazz [" + actualClass.getCanonicalName() + "]";
   }

   public Object newInstance() throws InstantiationException, IllegalAccessException
   {
      return actualClass.newInstance();
   }
}
