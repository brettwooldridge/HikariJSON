package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.zaxxer.hikari.json.JsonProperty;

public final class Clazz
{
   private final Class<?> actualClass;

   private final Phield[] fields;
   private final int[] fieldHashes;
   private final int[] quickLookup;

   public Clazz(Class<?> clazz)
   {
      this.actualClass = clazz;

      int fieldCount = 0;
      for (Field field : actualClass.getDeclaredFields()) {
         fieldCount += (Modifier.isStatic(field.getModifiers()) ? 0 : 1); 
      }

      fields = new Phield[fieldCount];
      fieldHashes = new int[fieldCount];
      quickLookup = new int[32];
   }

   void parseFields()
   {
      int ndx = 0;
      for (Field field : actualClass.getDeclaredFields()) {
         if (!Modifier.isStatic(field.getModifiers())) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            int hash = (jsonProperty != null ? jsonProperty.name().hashCode() : field.getName().hashCode());
            fieldHashes[ndx] = hash;
            ndx++;
         }
      }

      Arrays.sort(fieldHashes);
      for (Field field : actualClass.getDeclaredFields()) {
         if (!Modifier.isStatic(field.getModifiers())) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            int hash = (jsonProperty != null ? jsonProperty.name().hashCode() : field.getName().hashCode());
            boolean excluded = (jsonProperty != null && jsonProperty.exclude());

            Phield phield = new Phield(field, excluded);

            // Hash slot
            int slot = (hash & quickLookup.length - 1);
            for (int i = 0; i < fieldHashes.length; i++) {

               if (fieldHashes[i] == hash) {
                  fields[i] = phield;
                  quickLookup[slot] = i + 1;
                  break;
               }
            }
         }
      }
   }

   public Class<?> getActualClass() {
      return actualClass;
   }

   public Phield getPhield(final int hashCode) {
      final int ndx = quickLookup[(hashCode & quickLookup.length - 1)] - 1; // Hash slot lookup
      if (ndx != -1 && fieldHashes[ndx] == hashCode) {
         return fields[ndx];
      }

      for (int i = 0; i < fieldHashes.length; i++) {
         if (fieldHashes[i] == hashCode) {
            return fields[i];
         }
      }

      throw new RuntimeException("No method found for hashCode " + hashCode);
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
