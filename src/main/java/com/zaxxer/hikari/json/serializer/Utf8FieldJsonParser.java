package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;
import sun.misc.Unsafe;

import com.zaxxer.hikari.json.util.Phield;
import com.zaxxer.hikari.json.util.UnsafeHelper;

/**
 * UTF-8 Members + UTF-8 Values + Field Access JSON Parser
 *
 * @author Brett Wooldridge
 */
public class Utf8FieldJsonParser extends BaseJsonParser
{
   private static final Unsafe unsafe = UnsafeHelper.getUnsafe();

   @Override
   final protected void setMember(final Context context, final String memberName, final Object value)
   {
      try {
         final Phield phield = context.clazz.getPhield(memberName);
         phield.field.set(context.target, (value == Void.TYPE ? null : value));
      }
      catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected int parseMemberName(int bufferIndex)
   {
      return parseStringValue(bufferIndex);
   }

   @Override
   protected int parseStringValue(int bufferIndex)
   {
      try {
         final int startIndex = bufferIndex;
         while (true) {
            final int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               valueDeque.add(new String(byteBuffer, startIndex, (newIndex - startIndex), "UTF-8"));
               return newIndex + 1;
            }

            final byte[] newArray = new byte[bufferLimit * 2];
            System.arraycopy(byteBuffer, 0, newArray, 0, byteBuffer.length);
            byteBuffer = newArray;

            int read = source.read(byteBuffer, bufferIndex, byteBuffer.length - bufferIndex);
            if (read < 0) {
               throw new RuntimeException("Insufficent data.");
            }

            bufferIndex = seekBackUtf8Boundary(byteBuffer, bufferIndex);
         }
      }
      catch (Exception e) {
         throw new RuntimeException();
      }
   }
}
