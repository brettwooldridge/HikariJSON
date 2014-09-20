package com.zaxxer.hikari.json.serializer;

import com.zaxxer.hikari.json.util.ClassUtils;
import com.zaxxer.hikari.json.util.Clazz;
import com.zaxxer.hikari.json.util.Phield;

public final class ParseContext
{
   public final Clazz clazz;
   public final Phield phield;

   public final Object target;

   public int holderType;
   public String stringHolder;
   public Object objectHolder;
   public boolean booleanHolder;
   public long longHolder;
   public double doubleHolder;

   public ParseContext(final Class<?> targetType) {
      this.clazz = ClassUtils.reflect(targetType);
      this.phield = null;
      try {
         target = clazz.newInstance();
      }
      catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   public ParseContext(final Clazz clazz) {
      this.clazz = clazz;
      this.phield = null;
      try {
         target = clazz.newInstance();
      }
      catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   public ParseContext(final Phield phield) {
      this.phield = phield;
      this.clazz = phield.clazz;
      try {
         if (clazz != null) {
            target = clazz.newInstance();
         }
         else {
            target = phield.newInstance();
         }
      }
      catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }
}
