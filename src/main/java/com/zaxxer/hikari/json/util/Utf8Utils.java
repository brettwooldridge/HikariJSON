package com.zaxxer.hikari.json.util;

import java.util.Arrays;

public final class Utf8Utils
{
   private static final int[] CP_LOOKUP;

   static
   {
      CP_LOOKUP = new int[32];
      Arrays.fill(CP_LOOKUP, Integer.MAX_VALUE);

      CP_LOOKUP[0b11000] = 1;
      CP_LOOKUP[0b11001] = 1;
      CP_LOOKUP[0b11010] = 1;
      CP_LOOKUP[0b11011] = 1;
      CP_LOOKUP[0b11100] = 2;
      CP_LOOKUP[0b11101] = 2;
      CP_LOOKUP[0b11110] = 3;
   }

   private Utf8Utils()
   {
      // utility class
   }

   public static int findEndQuoteUTF8(final byte[] array, int index)
   {
      boolean escape = false;

      for (; index < array.length; index++) {
         final int currentChar = array[index];
         if (currentChar >= 0) {
            if (currentChar == 0x22 /* quote */ && !escape) {
               return index;
            }
            else if (currentChar == 0x5c /* backslash */) {
               escape = !escape;
            }
            else if (escape) {
               escape = false;
            }
         }
         else {
            // Skip additional UTF-8 bytes
            index += CP_LOOKUP[currentChar >> 3];
         }
      }

      return -1;  // we ran out of data
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
