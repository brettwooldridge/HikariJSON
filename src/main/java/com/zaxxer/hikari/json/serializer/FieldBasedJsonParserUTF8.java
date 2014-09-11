package com.zaxxer.hikari.json.serializer;

import com.zaxxer.hikari.json.util.Phield;

public class FieldBasedJsonParserUTF8 extends BaseJsonParserUTF8
{
   @Override
   protected void setMember(final Context context, final String memberName, final Object value)
   {
      try {
         final Phield phield = context.targetType.getPhield(memberName);
         phield.field.set(context.target, (value == Void.TYPE ? null : value));
      }
      catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }
}
