package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;

import com.zaxxer.hikari.json.util.Phield;

public abstract class BaseJsonParserUTF8
{
   protected static final char CR = '\r';
   protected static final char TAB = '\t';
   protected static final char SPACE = ' ';
   protected static final char QUOTE = '"';
   protected static final char COLON = ':';
   protected static final char COMMA = ',';
   protected static final char NEWLINE = '\n';
   protected static final char OPEN_CURLY = '{';
   protected static final char CLOSE_CURLY = '}';
   protected static final char OPEN_BRACKET = '[';
   protected static final char CLOSE_BRACKET = ']';

   private static final Charset UTF8;

   private final int BUFFER_SIZE = 16384;

   private InputStream source;
   private byte[] byteBuffer;
   private int bufferLimit;

   protected final ArrayDeque<Object> valueDeque;

   static
   {
      UTF8 = Charset.forName("UTF-8");
   }

   public BaseJsonParserUTF8() {
      byteBuffer = new byte[BUFFER_SIZE];
      valueDeque = new ArrayDeque<>(32);
   }

   @SuppressWarnings("unchecked")
   final public <T> T parseObject(final InputStream src, final Class<T> valueType)
   {
      source = src;

      try {
         Context context = new Context(valueType);
         context.createInstance();
         valueDeque.add(context.target);

         parseObject(0, context);
         return (T) valueDeque.removeLast();
      }
      catch (InstantiationException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   protected abstract void setMember(final Context context, final String memberName, final Object value);

   private int parseObject(int bufferIndex, final Context context)
   {
      try {
         while (true) {
            if (bufferIndex == bufferLimit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
            }

            switch (byteBuffer[bufferIndex]) {
            case OPEN_CURLY:
               bufferIndex = parseMembers(++bufferIndex, context);
               continue;
            case CLOSE_CURLY:
               return ++bufferIndex;
            default:
               bufferIndex++;
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private int parseMembers(int bufferIndex, final Context context)
   {
      int limit = bufferLimit;
      try {
         while (true) {
            if (bufferIndex == limit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
               limit = bufferLimit;
            }

            switch (byteBuffer[bufferIndex]) {
            case QUOTE:
               bufferIndex = parseString(++bufferIndex); // member name
               break;
            case COLON:
               final String memberName = (String) valueDeque.removeLast();

               Context nextContext = null;
               if (context.clazz != null) {
                  final Phield phield = context.clazz.getPhield(memberName);
                  if (!phield.isPrimitive) {
                     nextContext = new Context(phield);
                     nextContext.createInstance();
                     valueDeque.add(nextContext.target);
                  }                  
               }

               bufferIndex = parseValue(++bufferIndex, context, nextContext);
               setMember(context, memberName, valueDeque.removeLast() /* member value */);
               break;
            case CLOSE_CURLY:
               return bufferIndex;
            default:
               bufferIndex++;
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private int parseValue(int bufferIndex, final Context context, final Context nextContext)
   {
      try {
         while (true) {
            if (bufferIndex == bufferLimit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
            }

            switch (byteBuffer[bufferIndex]) {
            case QUOTE:
               return parseString(++bufferIndex);
            case 't':
               valueDeque.add(true);
               return ++bufferIndex;
            case 'f':
               valueDeque.add(false);
               return ++bufferIndex;
            case 'n':
               valueDeque.add(null);
               return ++bufferIndex;
            case OPEN_CURLY:
               return parseObject(bufferIndex, nextContext);
            case OPEN_BRACKET:
               return parseArray(++bufferIndex, nextContext);
            default:
               bufferIndex++;
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private int parseArray(int bufferIndex, final Context context)
   {
      try {
         while (true) {
            if (bufferIndex == bufferLimit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
            }

            switch (byteBuffer[bufferIndex]) {
            case TAB:
            case NEWLINE:
            case CR:
            case SPACE:
            case COMMA:
               bufferIndex++;
               break;
            case CLOSE_BRACKET:
               return ++bufferIndex;
            default:
               Context nextContext = null;
               final Phield phield = context.phield;
               if (phield != null) {
                  if (phield.isCollection || phield.isArray) {
                     nextContext = new Context(phield.getCollectionParameterClazz1());
                     nextContext.createInstance();
                     valueDeque.add(nextContext.target);
                  }                  
               }

               bufferIndex = parseValue(bufferIndex, context, nextContext);
               @SuppressWarnings("unchecked")
               Collection<Object> collection = ((Collection<Object>) context.target);
               collection.add(valueDeque.removeLast() /* member value */);
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

   }

   public int parseString(int bufferIndex)
   {
      try {
         final int startIndex = bufferIndex;
         while (true) {
            if (bufferIndex == bufferLimit) {
               final byte[] newArray = new byte[bufferLimit * 2];
               System.arraycopy(byteBuffer, 0, newArray, 0, byteBuffer.length);
               byteBuffer = newArray;

               int read = source.read(byteBuffer, bufferIndex, byteBuffer.length - bufferIndex);
               if (read < 0) {
                  throw new RuntimeException("Insufficent data.");
               }

               bufferIndex = seekBackUtf8Boundary(byteBuffer, bufferIndex);
            }

            final int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               valueDeque.add(new String(byteBuffer, startIndex, (newIndex - startIndex), UTF8));
               bufferIndex = newIndex + 1;
               return bufferIndex;
            }
            else {
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
      byte[] localBuffer = byteBuffer;
      while (true) {
         if (bufferIndex == limit) {
            if ((bufferIndex = fillBuffer()) == -1) {
               throw new RuntimeException("Insufficent data.");
            }
            limit = bufferLimit;
            localBuffer = byteBuffer;
         }

         if (localBuffer[bufferIndex] <= SPACE) {
            bufferIndex++;
         }
         else {
            return bufferIndex;
         }
      }
   }
}
