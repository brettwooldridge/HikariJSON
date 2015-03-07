package com.zaxxer.hikari.json.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class Utf8Utils
{
   private static final Unsafe unsafe;
   private static final long fieldOffset;

   static
   {
      unsafe = UnsafeHelper.getUnsafe();
      try {
         Field valueField = String.class.getDeclaredField("value");
         fieldOffset = unsafe.objectFieldOffset(valueField);
      }
      catch (NoSuchFieldException | SecurityException e) {
         throw new RuntimeException("sun.misc.Unsafe not available");
      }
   }

   private Utf8Utils() {
      // utility class
   }

   public static int findEndQuote(final byte[] array, int index)
   {
      for (; index < array.length; index++) {
         if (array[index] == 0x22 /* quote */ && array[index - 1] != 0x5c /* backslash */) {
            return index;
         }
      }

      return -1; // we ran out of data
   }

   public static int findEndQuoteAndHash(final byte[] array, int index, final MutableInteger hash)
   {
      for (;index < array.length; index++) {
         final int c = array[index];
         if (c == 0x22 /* quote */ && array[index - 1] != 0x5c /* backslash */) {
            return index;
         }
         hash.value = 31 * hash.value + c;
      }

      return -1; // we ran out of data
   }

   public static int findEndQuoteUTF8(final byte[] array, int index, final MutableBoolean utf8Detected)
   {
      for (; index < array.length; index++) {
         if (array[index] == 0x22 /* quote */ && array[index - 1] != 0x5c /* backslash */) {
            return index;
         }
         else if (array[index] < 0 && !utf8Detected.bool) {
            utf8Detected.bool = true;
         }
      }

      return -1; // we ran out of data
   }

   public static int seekBackUtf8Boundary(final byte[] array, int index)
   {
      for (; index > 0; index--) {
         final int currentChar = array[index];
         if (currentChar >= 0) {
            return index;
         }
         else if (currentChar >> 5 == 0b110 || currentChar >> 4 == 0b1110 || currentChar >> 5 == 0b11110) {
            return index;
         }
      }

      return 0;
   }

   public static String fastTrackAsciiDecode(final byte[] buf, final int offset, final int length)
   {
      final char[] chars = new char[length];
      for (int i = 0; i < length; i++) {
         chars[i] = (char) buf[offset + i];
      }

      final String s = new String();
      unsafe.putObject(s, fieldOffset, chars);
      return s;
   }
}
