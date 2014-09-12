package com.zaxxer.hikari.json.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;

import com.zaxxer.hikari.json.ObjectMapper;
import com.zaxxer.hikari.json.util.Phield;

public abstract class BaseJsonParser implements ObjectMapper
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

   private final int BUFFER_SIZE = 16384;

   protected InputStream source;
   protected byte[] byteBuffer;
   protected int bufferLimit;

   protected final ArrayDeque<Object> valueDeque;

   public BaseJsonParser() {
      byteBuffer = new byte[BUFFER_SIZE];
      valueDeque = new ArrayDeque<>(32);
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

   abstract protected void setMember(final Context context, final String memberName, final Object value);

   abstract protected int parseStringValue(int bufferIndex);
   
   abstract protected int parseMemberName(int bufferIndex);

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
               bufferIndex = parseMemberName(++bufferIndex);
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
               return parseStringValue(++bufferIndex);
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
