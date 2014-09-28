package com.zaxxer.hikari.json.util;


public class Types
{
   // Constants, where for numerics the LSB indicates whether it is an
   // integer or decimal type.
   //
   // ONLY SET THIS BIT FOR INT TYPES ------v
   public static final int BYTE    = 0b0000_1;
   public static final int INT     = 0b0001_1;
   public static final int SHORT   = 0b0010_1;
   public static final int LONG    = 0b0011_1;
   public static final int CHAR    = 0b0100_1;

   public static final int FLOAT   = 0b0000_0;
   public static final int DOUBLE  = 0b0001_0;
   public static final int BOOLEAN = 0b0010_0;
   public static final int STRING  = 0b0011_0;
   public static final int OBJECT  = 0b0100_0;
   public static final int DATE    = 0b0101_0;
   public static final int ENUM    = 0b0110_0;

   public static final int INTEGRAL_TYPE = 0b1;
}
