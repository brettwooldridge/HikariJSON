package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.zaxxer.hikari.json.JsonProperty;

public final class Clazz
{
   private final Class<?> actualClass;
   private final Map<Integer, Phield> fields;

   public Clazz(Class<?> clazz)
   {
      this.actualClass = clazz;
      this.fields = new HashMap<>();
   }

   void parseFields()
   {
      for (Field field : actualClass.getDeclaredFields()) {
         if (!Modifier.isStatic(field.getModifiers())) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            boolean excluded = (jsonProperty != null && jsonProperty.exclude());

            Phield phield = new Phield(field, excluded);
            if (jsonProperty != null) {
               fields.put(jsonProperty.name().hashCode(), phield);
            }
            else {
               fields.put(field.getName().hashCode(), phield);
               fields.put(field.getName().toLowerCase().hashCode(), phield);
            }
         }
      }
   }

   public Class<?> getActualClass() {
      return actualClass;
   }

   public Phield getPhield(final int hashCode) {
      return fields.get(hashCode);
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
