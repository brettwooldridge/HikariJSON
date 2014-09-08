package com.zaxxer.hikari.json;

import com.zaxxer.hikari.json.impl.ObjectMapperImpl;

public class JsonFactory
{
   public static final String STRICT = "strict";

   public static ObjectMapper create(String...options)
   {
      return new ObjectMapperImpl();
   }
}
