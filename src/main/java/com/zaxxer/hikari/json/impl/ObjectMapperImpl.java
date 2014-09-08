package com.zaxxer.hikari.json.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;

import com.zaxxer.hikari.json.ObjectMapper;

public class ObjectMapperImpl implements ObjectMapper
{
   @Override
   public <T> T readValue(String src, Class<T> valueType)
   {
      try {
         return readValue(new ByteArrayInputStream(src.getBytes("UTF-8")), valueType);
      }
      catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public <T> T readValue(InputStream src, Class<T> valueType)
   {
      return null;
   }

   @Override
   public <T extends Collection<C>, C> T readValue(String src, Class<T> valueType, Class<C> componentType)
   {
      return null;
   }

   @Override
   public <T extends Collection<C>, C> T readValue(InputStream src, Class<T> valueType, Class<C> componentType)
   {
      return null;
   }

   @Override
   public void writeValue(Writer dest, Object value)
   {

   }

   @Override
   public String writeValueAsString(Object value)
   {
      return null;
   }
}
