package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Clazz
{
   private Class<?> clazz;

   public Clazz(Class<?> clazz)
   {
      this.clazz = clazz;

      parseFields();
   }

   private void parseFields()
   {
      for (Field field : clazz.getDeclaredFields()) {
         if (!Modifier.isStatic(field.getModifiers())) {
            Phield phield = new Phield(field);
         }
      }
   }
}
