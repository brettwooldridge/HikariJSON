package com.zaxxer.hikari.json;

import java.util.Arrays;
import java.util.HashSet;

import com.zaxxer.hikari.json.serializer.FieldBasedJsonMapper;


public class JsonFactory
{
   public static enum Option
   {
      CONSISTENT_STRUCTURE,
      MEMBERS_ASCII,
      MEMBERS_UTF8,
      VALUES_ASCII,
      VALUES_UTF8,
      FIELD_ACCESS,
      BEAN_ACCESS
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
      return new FactoryOptions(Option.FIELD_ACCESS, Option.MEMBERS_ASCII, Option.VALUES_UTF8).create();
   }

   /**
    *
    */
   public static class FactoryOptions
   {
      private HashSet<Option> options = new HashSet<>();

      private FactoryOptions(Option...options)
      {
         this.options.addAll(Arrays.asList(options));
      }

      public FactoryOptions option(Option option)
      {
         this.options.add(option);
         return this;
      }

      /**
       * Create an ObjectMapper based on the options provided to the factory.
       *
       * @return a custom ObjectMapper supporting the requested options
       */
      public ObjectMapper create()
      {
         if (options.contains(Option.FIELD_ACCESS))
         {
               return new FieldBasedJsonMapper(options.toArray(new Option[0]));               
         }

         throw new UnsupportedOperationException();
      }
   }
}
