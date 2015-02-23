package com.zaxxer.hikari.json;

import java.util.ArrayList;
import java.util.HashMap;

import com.zaxxer.hikari.json.serializer.FieldBasedJsonMapper;


public class JsonFactory
{
   public static enum Option
   {
      CONSISTENT_STRUCTURE,
      MEMBERS_UTF8,
      VALUES_ASCII,
      VALUES_UTF8,
      FIELD_ACCESS,
      BEAN_ACCESS,
      COLLECTION_CLASS;
   }

   public static FactoryOptions option(Option...options)
   {
      return new FactoryOptions(options);
   }

   /**
    * Create a default ObjectMapper.  The default ObjectMapper implementation uses
    * field access, ASCII members, and UTF-8 values.  If you <i>know</i> that your
    * values are always ASCII instead of UTF-8 it is strongly recommended to
    * configure a custom ObjectMapper using the {@link #option(Option...)} method.
    *
    * @return an ObjectMapper with default options
    */
   public static ObjectMapper create()
   {
      return new FactoryOptions(Option.FIELD_ACCESS, Option.VALUES_UTF8)
         .option(Option.COLLECTION_CLASS, ArrayList.class)
         .create();
   }

   /**
    *
    */
   public static class FactoryOptions
   {
      private HashMap<Option, Object> options = new HashMap<>();

      private FactoryOptions(final Option...options)
      {
         for (Option option : options)
         {
            this.options.put(option, null);
         }
      }

      public FactoryOptions option(final Option option, final Object value)
      {
         this.options.put(option, value);
         return this;
      }

      /**
       * Create an ObjectMapper based on the options provided to the factory.
       *
       * @return a custom ObjectMapper supporting the requested options
       */
      public ObjectMapper create()
      {
         if (options.containsKey(Option.FIELD_ACCESS))
         {
               return new FieldBasedJsonMapper(options);               
         }

         throw new UnsupportedOperationException();
      }
   }
}
