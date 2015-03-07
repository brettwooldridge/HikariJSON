package com.zaxxer.hikari.json.serializer;

import static com.zaxxer.hikari.json.util.Utf8Utils.fastTrackAsciiDecode;
import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuote;
import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteUTF8;
import static com.zaxxer.hikari.json.util.Utf8Utils.findEndQuoteAndHash;
import static com.zaxxer.hikari.json.util.Utf8Utils.seekBackUtf8Boundary;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import sun.misc.Unsafe;

import com.zaxxer.hikari.json.JsonFactory.Option;
import com.zaxxer.hikari.json.ObjectMapper;
import com.zaxxer.hikari.json.util.MutableBoolean;
import com.zaxxer.hikari.json.util.MutableInteger;
import com.zaxxer.hikari.json.util.Phield;
import com.zaxxer.hikari.json.util.Types;
import com.zaxxer.hikari.json.util.UnsafeHelper;

public final class FieldBasedJsonMapper implements ObjectMapper
{
   protected static final int CR = '\r';
   protected static final int TAB = '\t';
   protected static final int SPACE = ' ';
   protected static final int QUOTE = '"';
   protected static final int COLON = ':';
   protected static final int COMMA = ',';
   protected static final int HYPHEN = '-';
   protected static final int NEWLINE = '\n';
   protected static final int OPEN_CURLY = '{';
   protected static final int CLOSE_CURLY = '}';
   protected static final int OPEN_BRACKET = '[';
   protected static final int CLOSE_BRACKET = ']';

   private static final Unsafe UNSAFE = UnsafeHelper.getUnsafe();
   private final boolean isAsciiValues;
   private final int BUFFER_SIZE = 16384;

   protected InputStream source;
   protected byte[] byteBuffer;
   protected int bufferLimit;
   private Class<?> collectionClass;

   public FieldBasedJsonMapper(Map<Option, Object> options) {
      byteBuffer = new byte[BUFFER_SIZE];

      isAsciiValues = options.containsKey(Option.VALUES_ASCII);
      Object collClass = options.get(Option.COLLECTION_CLASS);
      if (collClass instanceof Class && Collection.class.isAssignableFrom((Class<?>) collClass)) {
         collectionClass = (Class<?>) collClass;
      }
   }

   @Override
   @SuppressWarnings("unchecked")
   final public <T> T readValue(final InputStream src, final Class<T> valueType)
   {
      source = src;

      ParseContext context = new ParseContext(valueType);

      parseObject(0, context);
      return (T) context.target;
   }

   private int parseObject(int bufferIndex, final ParseContext context)
   {
      do {
         bufferIndex = fillBuffer(bufferIndex);

         final int b = byteBuffer[bufferIndex];
         if (b == OPEN_CURLY) {
            bufferIndex = parseMembers(bufferIndex + 1, context);
         }
         else if (b == CLOSE_CURLY) {
            return bufferIndex + 1;
         }
         else {
            bufferIndex++;
         }
      } while (true);
   }

   private int parseMembers(int bufferIndex, final ParseContext context)
   {
      int limit = bufferLimit;
top:  do {
         final byte[] buffer = byteBuffer;
         int b;
         while (true) {
            if (bufferIndex == limit) {
               bufferIndex = fillBuffer(bufferIndex);
               limit = bufferLimit;
               continue top;
            }

            b = buffer[bufferIndex];
            if (b > SPACE) {
               break;
            }

            bufferIndex++;
         }

         if (b == QUOTE) {
            bufferIndex = parseMember(bufferIndex, context);
         }
         else if (b == CLOSE_CURLY) {
            return bufferIndex;
         }
         else {
            bufferIndex++;
         }
      } while (true);
   }

   private int parseMember(int bufferIndex, final ParseContext context)
   {
      // Parse the member name
      bufferIndex = parseMemberHashOnly(bufferIndex + 1, context);

      // Next character better be a colon
      byte[] buffer = byteBuffer;
      do {
         if (bufferIndex == bufferLimit) {
            bufferIndex = fillBuffer(bufferIndex);
            buffer = byteBuffer;
         }
      } while (buffer[bufferIndex++] != COLON);

      // Now the value
      final Phield phield = context.clazz.getPhield(context.lookupKey);
      context.holderType = phield.type;
      if (phield.type == Types.OBJECT) {
         final ParseContext nextContext;
         if ((phield.isCollection || phield.isArray) && (phield.collectionClass == null && collectionClass != null)) {
            nextContext = new ParseContext(phield, collectionClass);
         }
         else {
            nextContext = new ParseContext(phield);
         }
         context.objectHolder = nextContext.target;
         bufferIndex = parseValue(bufferIndex, context, nextContext);
      }
      else {
         bufferIndex = parseValue(bufferIndex, context, null);
      }

      if (!phield.excluded) {
         setMember(phield, context);
      }

      return bufferIndex;
   }

   private int parseValue(int bufferIndex, final ParseContext context, final ParseContext nextContext)
   {
      do {
         int limit = bufferLimit;
         while (bufferIndex < limit) {

            final int b = byteBuffer[bufferIndex++];
            if (b <= SPACE) {
               continue;
            }

            if (b == QUOTE) {
               return skipCommaOrUptoCurly((isAsciiValues ? parseAsciiString(bufferIndex, context) : parseString(bufferIndex, context)), limit);
            }
            else if ((b > '1' - 1 && b < '9' + 1) || b == HYPHEN) {
               return skipCommaOrUptoCurly(((context.holderType & Types.INTEGRAL_TYPE) > 0) ? parseInteger(bufferIndex - 1, context) : parseDecimal(bufferIndex - 1, context), limit);
            }
            else if (b == OPEN_CURLY) {
               bufferIndex = parseMembers(bufferIndex, nextContext);
               // fall-thru
            }
            else if (b == CLOSE_CURLY) {
               return bufferIndex;
            }
            else if (b == OPEN_BRACKET) {
               return parseArray(bufferIndex, nextContext);
            }
            else if (b == 't') {
               context.booleanHolder = true;
               return skipCommaOrUptoCurly(bufferIndex, limit);
            }
            else if (b == 'f') {
               context.booleanHolder = false;
               return skipCommaOrUptoCurly(bufferIndex, limit);
            }
            else if (b == 'n') {
               context.objectHolder = null;
               return skipCommaOrUptoCurly(bufferIndex, limit);
            }
         }

         bufferIndex = fillBuffer(bufferIndex);
      } while (true);
   }

   private int parseArray(int bufferIndex, final ParseContext context)
   {
      int limit = bufferLimit;
      do {
         for (final byte[] buffer = byteBuffer; bufferIndex < limit && buffer[bufferIndex] <= SPACE; bufferIndex++)
            ; // skip whitespace

         if (bufferIndex == limit) {
            bufferIndex = fillBuffer(bufferIndex);
            limit = bufferLimit;
         }

         switch (byteBuffer[bufferIndex]) {
         case CLOSE_BRACKET:
            return bufferIndex + 1;
         default:
            ParseContext nextContext = context;
            final Phield phield = context.phield;
            if (phield != null) {
               if (phield.isCollection || phield.isArray) {
                  nextContext = new ParseContext(phield.getCollectionParameterClazz1());
               }
            }

            bufferIndex = parseValue(bufferIndex, context, nextContext);
            @SuppressWarnings("unchecked")
            Collection<Object> collection = ((Collection<Object>) context.target);
            collection.add(nextContext.target);
         }
      } while (true);
   }

   private int parseString(int bufferIndex, final ParseContext context)
   {
      try {
         final int startIndex = bufferIndex;
         do {
            final MutableBoolean utf8Detected = new MutableBoolean();
            final int newIndex = findEndQuoteUTF8(byteBuffer, bufferIndex, utf8Detected);
            if (newIndex > 0) {
               if (utf8Detected.bool) {
                  context.stringHolder = new String(byteBuffer, startIndex, (newIndex - startIndex), "UTF-8");
               }
               else {
                  context.stringHolder = fastTrackAsciiDecode(byteBuffer, startIndex, (newIndex - startIndex));
               }
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
         } while (true);
      }
      catch (Exception e) {
         throw new RuntimeException();
      }
   }

   private int parseMemberHashOnly(int bufferIndex, final ParseContext context)
   {
      try {
         final MutableInteger hash = new MutableInteger();
         do {
            final int newIndex = findEndQuoteAndHash(byteBuffer, bufferIndex, hash);
            if (newIndex > 0) {
               context.lookupKey = hash.value;
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
         } while (true);
      }
      catch (Exception e) {
         throw new RuntimeException();
      }
   }

   private int parseAsciiString(int bufferIndex, final ParseContext context)
   {
      try {
         final int startIndex = bufferIndex;
         do {
            final int newIndex = findEndQuote(byteBuffer, bufferIndex);
            if (newIndex > 0) {
               context.stringHolder = fastTrackAsciiDecode(byteBuffer, startIndex, (newIndex - startIndex));
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
         } while (true);
      }
      catch (Exception e) {
         throw new RuntimeException();
      }
   }

   private int parseInteger(int bufferIndex, ParseContext context)
   {
      boolean neg = (byteBuffer[bufferIndex] == '-');
      if (neg) {
         ++bufferIndex;
      }

      int limit = bufferLimit;

      // integer part
      long part = 0;
      outer1: while (true) {
         try {
            for (final byte[] buffer = byteBuffer; bufferIndex < limit; bufferIndex++) {
               final int b = buffer[bufferIndex];
               if (b >= '0' && b <= '9') {
                  part = part * 10 + (b - '0');
               }
               else {
                  break outer1;
               }
            }
            limit = bufferLimit;
         }
         finally {
            bufferIndex = fillBuffer(bufferIndex);
         }
      }

      if (neg) {
         part *= -1;
      }

      context.longHolder = part;
      return bufferIndex;
   }

   private int parseDecimal(int bufferIndex, ParseContext context)
   {
      double d = 0.0;      // value
      long part  = 0;      // the current part (int, float and sci parts of the number)
      
      boolean neg = (byteBuffer[bufferIndex] == '-');
      if (neg) {
         ++bufferIndex;
      }

      // integer part
      long shift = 0;
      outer1: while (true) {
         final int limit = bufferLimit;
         try {
            for (final byte[] buffer = byteBuffer; bufferIndex < limit; bufferIndex++) {
               final int b = buffer[bufferIndex];
               if (b >= '0' && b <= '9') {
                  shift *= 10;
                  part = part * 10 + (b - '0');
               }
               else if (b == '.') {
                  shift = 1;
               }
               else {
                  break outer1;
               }
            }
         }
         finally {
            bufferIndex = fillBuffer(bufferIndex);
         }
      }

      if (neg) {
         part *= -1;
      }

      d = shift != 0 ? (double)part / (double)shift : part;

      // scientific part
      if (byteBuffer[bufferIndex] == 'e' || byteBuffer[bufferIndex] == 'E') {
         ++bufferIndex;
         part = 0;
         neg = byteBuffer[bufferIndex] == '-';
         bufferIndex = neg ? ++bufferIndex : bufferIndex;
         outer1: while (true) {
            final int limit = bufferLimit;
            for (final byte[] buffer = byteBuffer; bufferIndex < limit; bufferIndex++) {
               final int b = buffer[bufferIndex];
               if (b >= '0' && b <= '9') {
                  part = part * 10 + (b - '0');
                  continue;
               }

               break outer1;
            }

            bufferIndex = fillBuffer(bufferIndex);
         }

         d = (neg) ? d / (double)Math.pow(10, part) : d * (double)Math.pow(10, part);
      }

      context.doubleHolder = d;
      return bufferIndex;
   }

   private void setMember(final Phield phield, final ParseContext context)
   {
      try {
         final int type = phield.type;
         if (phield.isIntegralType) {
            if (type == Types.INT) {
               UNSAFE.putInt(context.target, phield.fieldOffset, (int) context.longHolder);
            }
            else if (type == Types.LONG) {
               UNSAFE.putLong(context.target, phield.fieldOffset, context.longHolder);
            }
            else if (type == Types.SHORT) {
               UNSAFE.putShort(context.target, phield.fieldOffset, (short) context.longHolder);
            }
            else if (type == Types.BYTE) {
               UNSAFE.putByte(context.target, phield.fieldOffset, (byte) context.longHolder);
            }
            else if (type == Types.CHAR) {
               UNSAFE.putChar(context.target, phield.fieldOffset, (char) context.longHolder);
            }
         }
         else {
            if (type == Types.STRING) {
               UNSAFE.putObject(context.target, phield.fieldOffset, context.stringHolder);
            }
            else if (type == Types.DOUBLE) {
               UNSAFE.putDouble(context.target, phield.fieldOffset, context.doubleHolder);
            }
            else if (type == Types.OBJECT) {
               UNSAFE.putObject(context.target, phield.fieldOffset, (context.objectHolder == Void.TYPE ? null : context.objectHolder));
            }
            else if (type == Types.BOOLEAN) {
               UNSAFE.putBoolean(context.target, phield.fieldOffset, context.booleanHolder);
            }
            else if (type == Types.FLOAT) {
               UNSAFE.putFloat(context.target, phield.fieldOffset, (float) context.doubleHolder);
            }
            else if (type == Types.DATE) {
               UNSAFE.putObject(context.target, phield.fieldOffset, parseDate(context.stringHolder));
            }
            else if (type == Types.ENUM) {
            }
         }
      }
      catch (SecurityException | IllegalArgumentException e) {
         throw new RuntimeException(e);
      }
   }

   private int skipCommaOrUptoCurly(int bufferIndex, final int limit)
   {
      for (final byte[] buffer = byteBuffer; bufferIndex < limit; bufferIndex++)
      {
         if (buffer[bufferIndex] == COMMA) {
            return bufferIndex + 1;
         }
         else if (buffer[bufferIndex] == CLOSE_CURLY) {
            return bufferIndex;
         }
      }

      return bufferIndex;
   }

   private HashMap<String, Date> dateCache = new HashMap<>();
   private Date parseDate(String stringHolder)
   {
      return dateCache.computeIfAbsent(stringHolder, (k) -> {
         final OffsetDateTime dateTime = OffsetDateTime.parse(stringHolder);
         return GregorianCalendar.from(dateTime.toZonedDateTime()).getTime();
      });
   }

   final protected int fillBuffer(final int bufferIndex)
   {
      if (bufferIndex == bufferLimit) {
         try {
            int read = source.read(byteBuffer);
            if (read > 0) {
               bufferLimit = read;
               return 0;
            }
   
            throw new RuntimeException("Insufficient data during parsing");
         }
         catch (IOException io) {
            throw new RuntimeException(io);
         }
      }

      return bufferIndex;
   }
}
