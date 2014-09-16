package com.zaxxer.hikari.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class TestParser1
{
   @Test
   public void testParser01() throws Exception
   {
      ObjectMapper objectMapper = JsonFactory.create();

      File file = new File("src/test/resources/menu.json");
      try (InputStream is = new FileInputStream(file)) {
         MenuBar menuBar = objectMapper.readValue(is, MenuBar.class);
         Assert.assertEquals(menuBar.menu.id, "file");
         Assert.assertEquals(menuBar.menu.value, "File");
         Assert.assertNotNull(menuBar.menu.popup);
         Assert.assertNotNull(menuBar.menu.popup.menuitem);
         Assert.assertSame(3, menuBar.menu.popup.menuitem.size());
      }
   }

   @Test
   public void testParser02() throws Exception
   {
      ObjectMapper objectMapper = JsonFactory.create();

      File file = new File("src/test/resources/AllTypes.json");
      try (InputStream is = new FileInputStream(file)) {
         AllType allType = objectMapper.readValue(is, AllType.class);
      }
   }
}
