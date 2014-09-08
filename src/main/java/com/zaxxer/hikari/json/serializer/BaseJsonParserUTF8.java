package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

public abstract class BaseJsonParserUTF8
{
   private final int BUFFER_SIZE = 16384;

   private InputStream source;
   private byte[] byteBuffer;
   private int bufferLimit;

   protected ArrayDeque<Object> parseDeque;

   public BaseJsonParserUTF8() {
      byteBuffer = new byte[BUFFER_SIZE];
      parseDeque = new ArrayDeque<>(32);
   }

   public <T> T parseObject(InputStream src, Class<T> valueType)
   {
      source = src;

      try {
         T instance = valueType.newInstance();
         parseDeque.add(instance);
         parseObject(0);
         return instance;
      }
      catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private int parseObject(int bufferIndex)
   {
      try {
loop:    while (true) {
            bufferIndex = skipWhitespace(bufferIndex);

            if (bufferIndex == bufferLimit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  break;
               }
            }

            switch (byteBuffer[bufferIndex]) {
            case '{':
               bufferIndex++;
               bufferIndex = parseMembers(bufferIndex);
               continue;
            case '}':
               bufferIndex++;
               break loop;
            default:
               bufferIndex++;
            }
         }

         return bufferIndex;
      }
      catch (Exception e) {
         throw new RuntimeException();
      }      
   }

   private int parseMembers(int bufferIndex)
   {
      try {
         while (true) {
            bufferIndex = skipWhitespace(bufferIndex);

            if (bufferIndex == bufferLimit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
            }

            switch (byteBuffer[bufferIndex]) {
            case '"':
               bufferIndex++;
               bufferIndex = parseString(bufferIndex); // member name
               continue;
            case ':':
               bufferIndex++;
               bufferIndex = parseValue(bufferIndex);  // member value
               setMember(parseDeque.removeLast(), parseDeque.removeLast());
               continue;
            case '}':
               return bufferIndex;
            default:
               bufferIndex++;
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException();
      }      
   }

   protected abstract void setMember(Object value, Object member);

   private int parseValue(int bufferIndex)
   {
      try {
loop:    while (true) {
            bufferIndex = skipWhitespace(bufferIndex);

            if (bufferIndex == bufferLimit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
            }

            switch (byteBuffer[bufferIndex]) {
            case '"':
               bufferIndex++;
               bufferIndex = parseString(bufferIndex);
               return bufferIndex;
            case 't':
               bufferIndex++;
               parseDeque.add(true);
               break loop;
            case 'f':
               bufferIndex++;
               parseDeque.add(false);
               break loop;
            case 'n':
               bufferIndex++;
               parseDeque.add(null);
               break loop;
            case '{':
               bufferIndex = parseObject(bufferIndex);
               break loop;
            case '[':
               bufferIndex++;
               bufferIndex = parseArray(bufferIndex);
               break loop;
            default:
               bufferIndex++;
            }
         }

         return bufferIndex;
      }
      catch (Exception e) {
         throw new RuntimeException();
      }      
   }

   private int parseArray(int bufferIndex)
   {
      return bufferIndex;
   }

   public int parseString(int bufferIndex)
   {
      try {
         if (bufferIndex >= bufferLimit) {
            if ((bufferIndex = fillBuffer()) == -1) {
               throw new RuntimeException("Insufficent data.");
            }
         }
         
         int startIndex = bufferIndex;
         while (true) {
            if (bufferIndex >= bufferLimit) {
               final byte[] newArray = new byte[bufferLimit * 2];
               System.arraycopy(byteBuffer, 0, newArray, 0, byteBuffer.length);
               byteBuffer = newArray;

               int read = source.read(byteBuffer, bufferIndex, byteBuffer.length - bufferIndex);
               if (read < 0) {
                  throw new RuntimeException("Insufficent data.");
               }

               bufferIndex = seekBackUtf8Boundary(byteBuffer, bufferIndex);
            }

            int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               bufferIndex = newIndex + 1;
               parseDeque.add(new String(byteBuffer, startIndex, (newIndex - startIndex), "UTF-8"));
               return bufferIndex;
            }
            else if (newIndex == -1) {
               bufferIndex = bufferLimit;
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException();
      }
   }

   private int fillBuffer() throws IOException
   {
      int read = source.read(byteBuffer);
      if (read > 0) {
         bufferLimit = read;
         return 0;
      }

      return -1;
   }

   private int skipWhitespace(int bufferIndex) throws IOException
   {
      int limit = bufferLimit;
      while (true) {
         if (bufferIndex == limit) {
            if ((bufferIndex = fillBuffer()) == -1) {
               throw new RuntimeException("Insufficent data.");
            }
            limit = bufferLimit;
         }

         switch (byteBuffer[bufferIndex]) {
         case '\t':
         case '\n':
         case '\r':
         case ' ': // skip whitespace
            bufferIndex++;
            continue;
         default:
            return bufferIndex;
         }
      }
   }
}
