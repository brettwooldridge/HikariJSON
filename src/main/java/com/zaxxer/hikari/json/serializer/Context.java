package com.zaxxer.hikari.json.serializer;

import com.zaxxer.hikari.json.util.ClassUtils;
import com.zaxxer.hikari.json.util.Clazz;
import com.zaxxer.hikari.json.util.Phield;

public class Context
{
   public Object target;
   public Clazz targetType;
   public Phield targetPhield;
   
   public Context(Class<?> targetType)
   {
      this.targetType = ClassUtils.reflect(targetType);
   }

   public Context(Clazz clazz)
   {
      this.targetType = clazz;
   }

   public Context(Phield phield) {
      this.targetPhield = phield;
      this.targetType = phield.clazz;
   }

   public void createInstance() throws InstantiationException, IllegalAccessException
   {
      if (targetType != null) {
         target = targetType.newInstance();
      }
      else {
         target = targetPhield.newInstance();
      }
   }
}
