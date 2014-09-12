package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import com.zaxxer.hikari.json.util.Utf8Utils;

/**
 * ASCII Members + UTF-8 Values + Field Access JSON Parser
 *
 * @author Brett Wooldridge
 */
public class AMU8VFieldJsonParser extends Utf8FieldJsonParser
{
   @Override
   final protected int parseMemberName(int bufferIndex)
   {
      try {
         final int startIndex = bufferIndex;
         while (true) {
            final int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               valueDeque.add(Utf8Utils.fastTrackAsciiDecode(byteBuffer, startIndex, (newIndex - startIndex)));
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
