package com.zaxxer.hikari.json.util;


public final class Utf8Utils
{
   private Utf8Utils() {
      // utility class
   }

   public static int findEndQuoteUTF8(final byte[] array, int index)
   {
      for (; index < array.length; index++) {
         if (array[index] == 0x22 /* quote */ && array[index - 1] != 0x5c /* backslash */) {
            return index;
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

}
