package com.zaxxer.hikari.json.serializer;


/**
 * ASCII Members + ASCII Values + Field Access JSON Parser
 *
 * @author Brett Wooldridge
 */
public final class AMAVFieldJsonParser extends AMU8VFieldJsonParser
{
   @Override
   final protected int parseStringValue(final int bufferIndex)
   {
      return parseMemberName(bufferIndex);
   }
}
