package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

public abstract class BaseJsonParserUTF8
{
   protected static final char CR = '\r';
   protected static final char TAB = '\t';
   protected static final char SPACE = ' ';
   protected static final char QUOTE = '"';
   protected static final char COLON = ':';
   protected static final char NEWLINE = '\n';
   protected static final char OPEN_CURLY = '{';
   protected static final char CLOSE_CURLY = '}';
   protected static final char OPEN_BRACKET = '[';
   protected static final char CLOSE_BRACKET = ']';

   private final int BUFFER_SIZE = 16384;

   private InputStream source;
   private byte[] byteBuffer;
   private int bufferLimit;

   protected ArrayDeque<Object> valueDeque;

   public BaseJsonParserUTF8() {
      byteBuffer = new byte[BUFFER_SIZE];
      valueDeque = new ArrayDeque<>(32);
   }

   @SuppressWarnings("unchecked")
   public <T> T parseObject(InputStream src, Class<T> valueType)
   {
      source = src;

      parseObject(0, valueType);
      return (T) valueDeque.removeLast();
   }

   private int parseObject(int bufferIndex, Class<?> targetType)
   {
      try {
         Context context = new Context(targetType);
         context.createInstance();
         valueDeque.add(context);
         bufferIndex = parseObject(bufferIndex, context);
         return bufferIndex;
      }
      catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private int parseObject(int bufferIndex, Context context)
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
            case OPEN_CURLY:
               bufferIndex++;
               bufferIndex = parseMembers(bufferIndex, context);
               continue;
            case CLOSE_CURLY:
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

   private int parseMembers(int bufferIndex, Context context)
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
            case QUOTE:
               bufferIndex++;
               bufferIndex = parseString(bufferIndex); // member name
               continue;
            case COLON:
               bufferIndex++;
               String memberName = (String) valueDeque.removeLast();
//               Class<?> memberType = getMemberType(memberName, context.getClass());
               bufferIndex = parseValue(bufferIndex, memberName, context);
               setMember(context, valueDeque.removeLast() /* member value */, memberName);
               continue;
            case CLOSE_CURLY:
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

   protected abstract void setMember(Object target, Object value, Object member);

   protected abstract Class<?> getMemberType(String memberName, Class<?> valueType);
   
   private int parseValue(int bufferIndex, String memberName, Context context) //, Class<?> targetType)
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
            case QUOTE:
               bufferIndex++;
               bufferIndex = parseString(bufferIndex);
               return bufferIndex;
            case 't':
               bufferIndex++;
               valueDeque.add(true);
               break loop;
            case 'f':
               bufferIndex++;
               valueDeque.add(false);
               break loop;
            case 'n':
               bufferIndex++;
               valueDeque.add(null);
               break loop;
            case OPEN_CURLY:
               bufferIndex = parseObject(bufferIndex, context);
               break loop;
            case OPEN_BRACKET:
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
      // valueDeque.add(Void.TYPE);
      for (; bufferIndex < bufferLimit; bufferIndex++) {
         if (byteBuffer[bufferIndex] == CLOSE_BRACKET) {
            return bufferIndex;
         }
      }

      throw new RuntimeException("Insufficent data.");
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
               valueDeque.add(new String(byteBuffer, startIndex, (newIndex - startIndex), "UTF-8"));
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
         case TAB:
         case NEWLINE:
         case CR:
         case SPACE: // skip whitespace
            bufferIndex++;
            continue;
         default:
            return bufferIndex;
         }
      }
   }
}
