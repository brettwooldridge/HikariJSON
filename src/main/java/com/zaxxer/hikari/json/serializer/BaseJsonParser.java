package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import com.zaxxer.hikari.json.JsonFactory.Option;
import com.zaxxer.hikari.json.ObjectMapper;
import com.zaxxer.hikari.json.util.Phield;
import com.zaxxer.hikari.json.util.Utf8Utils;

public final class BaseJsonParser implements ObjectMapper
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

   private final boolean isAsciiMembers;
   private final boolean isAsciiValues;

   private final int BUFFER_SIZE = 16384;

   protected InputStream source;
   protected byte[] byteBuffer;
   protected int bufferLimit;

   protected final ArrayDeque<Object> valueDeque;

   public BaseJsonParser(Option...options) {
      byteBuffer = new byte[BUFFER_SIZE];
      valueDeque = new ArrayDeque<>(32);

      HashSet<Option> set = new HashSet<>(Arrays.asList(options));
      isAsciiMembers = set.contains(Option.MEMBERS_ASCII);
      isAsciiValues = set.contains(Option.VALUES_ASCII);
   }

   @Override
   @SuppressWarnings("unchecked")
   final public <T> T readValue(final InputStream src, final Class<T> valueType)
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
               bufferIndex = (isAsciiMembers ? parseAsciiString(++bufferIndex) : parseString(++bufferIndex));
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
                  bufferIndex = parseValue(++bufferIndex, context, nextContext);
                  setMember(context.target, phield, valueDeque.removeLast() /* member value */);
               }
               else {
                  bufferIndex = parseValue(++bufferIndex, context, nextContext);
                  setMember(context, memberName, valueDeque.removeLast() /* member value */);
               }
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
               return (isAsciiValues ? parseAsciiString(++bufferIndex) : parseString(++bufferIndex));
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

   private int parseString(int bufferIndex)
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

   private int parseAsciiString(int bufferIndex)
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

   private void setMember(final Context context, final String memberName, final Object value)
   {
      try {
         final Phield phield = context.clazz.getPhield(memberName);
         phield.field.set(context.target, (value == Void.TYPE ? null : value));
      }
      catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private void setMember(final Object target, final Phield phield, final Object value)
   {
      try {
         phield.field.set(target, (value == Void.TYPE ? null : value));
      }
      catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   final protected int fillBuffer() throws IOException
   {
      int read = source.read(byteBuffer);
      if (read > 0) {
         bufferLimit = read;
         return 0;
      }

      return -1;
   }
}
