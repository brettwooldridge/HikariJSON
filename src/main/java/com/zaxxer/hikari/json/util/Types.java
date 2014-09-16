package com.zaxxer.hikari.json.util;


public class Types
{
   // Constants, where for numerics the LSB indicates whether it is an integer or decimal type.
   public static final int BYTE    = 0b0001;
   public static final int INT     = 0b0011;
   public static final int SHORT   = 0b0101;
   public static final int LONG    = 0b0111;
   public static final int CHAR    = 0b1001;
   public static final int FLOAT   = 0b0000;
   public static final int DOUBLE  = 0b0010;
   public static final int BOOLEAN = 0b0100;
   public static final int STRING  = 0b0110;
   public static final int OBJECT  = 0b1000;

   public static final int INTEGRAL_TYPE = 0b1;
}
