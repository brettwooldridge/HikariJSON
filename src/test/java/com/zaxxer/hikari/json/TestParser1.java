package com.zaxxer.hikari.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.json.serializer.FieldBasedJsonParserUTF8;

public class TestParser1
{
   @Test
   public void testParser01() throws Exception
   {
      FieldBasedJsonParserUTF8 jsonParser = new FieldBasedJsonParserUTF8();

      File file = new File("src/test/resources/menu.json");
      try (InputStream is = new FileInputStream(file)) {
         MenuBar menuBar = jsonParser.parseObject(is, MenuBar.class);
         Assert.assertEquals(menuBar.menu.id, "file");
         Assert.assertEquals(menuBar.menu.value, "File");
         Assert.assertNotNull(menuBar.menu.popup);
      }
   }
}
