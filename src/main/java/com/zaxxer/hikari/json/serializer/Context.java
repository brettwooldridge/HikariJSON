package com.zaxxer.hikari.json.serializer;

import com.zaxxer.hikari.json.util.ClassUtils;
import com.zaxxer.hikari.json.util.Clazz;
import com.zaxxer.hikari.json.util.Phield;

public final class Context
{
   public final Clazz clazz;
   public final Phield phield;

   public Object target;
   public String stringHolder;
   public Object objectHolder;
   public boolean booleanHolder;
   public int intHolder;
   public long longHolder;
   
   public Context(final Class<?> targetType)
   {
      this.clazz = ClassUtils.reflect(targetType);
      this.phield = null;
   }

   public Context(final Clazz clazz)
   {
      this.clazz = clazz;
      this.phield = null;
   }

   public Context(final Phield phield) {
      this.phield = phield;
      this.clazz = phield.clazz;
   }

   public void createInstance() throws InstantiationException, IllegalAccessException
   {
      if (clazz != null) {
         target = clazz.newInstance();
      }
      else {
         target = phield.newInstance();
      }
   }
}
