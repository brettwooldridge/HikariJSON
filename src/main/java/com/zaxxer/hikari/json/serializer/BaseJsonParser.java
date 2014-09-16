package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import sun.misc.Unsafe;

import com.zaxxer.hikari.json.JsonFactory.Option;
import com.zaxxer.hikari.json.ObjectMapper;
import com.zaxxer.hikari.json.util.Phield;
import com.zaxxer.hikari.json.util.Types;
import com.zaxxer.hikari.json.util.UnsafeHelper;
import com.zaxxer.hikari.json.util.Utf8Utils;

@SuppressWarnings("restriction")
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

   private static final Unsafe UNSAFE = UnsafeHelper.getUnsafe(); 
   private final boolean isAsciiMembers;
   private final boolean isAsciiValues;
   private final int BUFFER_SIZE = 16384;

   protected InputStream source;
   protected byte[] byteBuffer;
   protected int bufferLimit;

   public BaseJsonParser(Option...options) {
      byteBuffer = new byte[BUFFER_SIZE];

      HashSet<Option> set = new HashSet<>(Arrays.asList(options));
      isAsciiMembers = set.contains(Option.MEMBERS_ASCII);
      isAsciiValues = set.contains(Option.VALUES_ASCII);
   }

   @Override
   @SuppressWarnings("unchecked")
   final public <T> T readValue(final InputStream src, final Class<T> valueType)
   {
      source = src;

      Context context = new Context(valueType);
      context.createInstance();

      parseObject(0, context);
      return (T) context.target;
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
            for (final byte[] buffer = byteBuffer; bufferIndex < buffer.length && buffer[bufferIndex] <= SPACE; bufferIndex++); // skip whitespace

            if (bufferIndex == limit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
               limit = bufferLimit;
            }

            switch (byteBuffer[bufferIndex]) {
            case QUOTE:
               bufferIndex = parseMember(bufferIndex, context);
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

   private int parseMember(int bufferIndex, final Context context)
   {
      // Parse the member name
      bufferIndex = (isAsciiMembers ? parseAsciiString(++bufferIndex, context) : parseString(++bufferIndex, context));

      // Next character better be a colon
      while (true) {
         if (bufferIndex == bufferLimit && (bufferIndex = fillBuffer()) == -1) {
            throw new RuntimeException("Insufficent data.");
         }

         if (byteBuffer[bufferIndex++] == COLON) {
            break;
         }
      }

      final String memberName = context.stringHolder;
      Context nextContext = null;
      final Phield phield = context.clazz.getPhield(memberName);
      if (phield.type == Types.OBJECT) {
         nextContext = new Context(phield);
         nextContext.createInstance();
         context.objectHolder = nextContext.target;
      }

      bufferIndex = parseValue(bufferIndex, context, nextContext);
      setMember(phield, context);

      return bufferIndex;
   }

   private int parseValue(int bufferIndex, final Context context, final Context nextContext)
   {
      try {
         while (true) {
            for (final byte[] buffer = byteBuffer; bufferIndex < buffer.length; bufferIndex++) {

               final int b = buffer[bufferIndex];
               if (b <= SPACE) {
                  continue;
               }

               switch (b) {
               case QUOTE:
                  return (isAsciiValues ? parseAsciiString(++bufferIndex, context) : parseString(++bufferIndex, context));
               case 't':
                  context.booleanHolder = true;
                  return ++bufferIndex;
               case 'f':
                  context.booleanHolder = false;
                  return ++bufferIndex;
               case 'n':
                  context.objectHolder = null;
                  return ++bufferIndex;
               case '0':
               case '1':
               case '2':
               case '3':
               case '4':
               case '5':
               case '6':
               case '7':
               case '8':
               case '9':
                  break;
               case OPEN_CURLY:
                  return parseObject(bufferIndex, nextContext);
               case OPEN_BRACKET:
                  return parseArray(++bufferIndex, nextContext);
               default:
                  break;
               }
            }

            if (bufferIndex == bufferLimit && (bufferIndex = fillBuffer()) == -1) {
               throw new RuntimeException("Insufficent data.");
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private int parseArray(int bufferIndex, final Context context)
   {
      int limit = bufferLimit;
      try {
         while (true) {
            for (final byte[] buffer = byteBuffer; bufferIndex < limit && buffer[bufferIndex] <= SPACE; bufferIndex++);  // skip whitespace

            if (bufferIndex == limit) {
               if ((bufferIndex = fillBuffer()) == -1) {
                  throw new RuntimeException("Insufficent data.");
               }
               limit = bufferLimit;
            }

            switch (byteBuffer[bufferIndex]) {
            case CLOSE_BRACKET:
               return ++bufferIndex;
            default:
               Context nextContext = context;
               final Phield phield = context.phield;
               if (phield != null) {
                  if (phield.isCollection || phield.isArray) {
                     nextContext = new Context(phield.getCollectionParameterClazz1());
                     nextContext.createInstance();
                     // valueDeque.add(nextContext.target);
                  }                  
               }

               bufferIndex = parseValue(bufferIndex, context, nextContext);
               @SuppressWarnings("unchecked")
               Collection<Object> collection = ((Collection<Object>) context.target);
               collection.add(nextContext.target);
               // collection.add(valueDeque.removeLast() /* member value */);
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private int parseString(int bufferIndex, final Context context)
   {
      try {
         final int startIndex = bufferIndex;
         while (true) {
            final int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               context.stringHolder = new String(byteBuffer, startIndex, (newIndex - startIndex), "UTF-8");
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

   private int parseAsciiString(int bufferIndex, final Context context)
   {
      try {
         final int startIndex = bufferIndex;
         while (true) {
            final int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               context.stringHolder = Utf8Utils.fastTrackAsciiDecode(byteBuffer, startIndex, (newIndex - startIndex));
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

   private void setMember(final Phield phield, final Context context)
   {
      try {
         switch (phield.type) {
            case Types.INT:
               break;
            case Types.STRING:
               UNSAFE.putObject(context.target, phield.fieldOffset, context.stringHolder);
               break;
            case Types.OBJECT:
               UNSAFE.putObject(context.target, phield.fieldOffset, (context.objectHolder == Void.TYPE ? null : context.objectHolder));
               break;
         }
      }
      catch (SecurityException | IllegalArgumentException e) {
         throw new RuntimeException(e);
      }
   }

   final protected int fillBuffer()
   {
      try {
         int read = source.read(byteBuffer);
         if (read > 0) {
            bufferLimit = read;
            return 0;
         }
   
         return -1;
      }
      catch (IOException io) {
         throw new RuntimeException(io);
      }
   }
}
