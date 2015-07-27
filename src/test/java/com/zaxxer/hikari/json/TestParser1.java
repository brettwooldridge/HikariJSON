package com.zaxxer.hikari.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.json.JsonFactory.Option;

public class TestParser1
{
   private ByteArrayInputStream bais;

   @Before
   public void before() throws IOException
   {
      File file = new File("src/test/resources/menu.json");
      byte[] bytes = IOUtils.toByteArray(new FileInputStream(file));
      bais = new ByteArrayInputStream(bytes);
   }

   @Test
   public void testParser01() throws Exception
   {
      ObjectMapper objectMapper = JsonFactory.create();
      MenuBar menuBar = objectMapper.readValue(bais, MenuBar.class);
      Assert.assertEquals(menuBar.menu.id, "file");
      Assert.assertEquals(menuBar.menu.value, "File");
      Assert.assertNotNull(menuBar.menu.popup);
      Assert.assertNotNull(menuBar.menu.popup.menuitem);
      Assert.assertTrue(menuBar.menu.popup.menuitem instanceof HashSet);
      Assert.assertSame(3, menuBar.menu.popup.menuitem.size());
   }

   @Test
   public void testParser02() throws Exception
   {
      ObjectMapper objectMapper = JsonFactory.option(Option.FIELD_ACCESS, Option.VALUES_ASCII).option(Option.COLLECTION_CLASS, LinkedList.class).create();

      MenuBar2 menuBar = objectMapper.readValue(bais, MenuBar2.class);
      Assert.assertEquals(menuBar.menu.id, "file");
      Assert.assertEquals(menuBar.menu.value, "File");
      Assert.assertNotNull(menuBar.menu.popup);
      Assert.assertNotNull(menuBar.menu.popup.menuitem);
      Assert.assertTrue(menuBar.menu.popup.menuitem instanceof LinkedList);
      Assert.assertSame(3, menuBar.menu.popup.menuitem.size());
   }

   @Test
   public void testParser03() throws Exception
   {
      ObjectMapper objectMapper = JsonFactory.option(Option.FIELD_ACCESS, Option.VALUES_ASCII).create();

      MenuBar2 menuBar = objectMapper.readValue(bais, MenuBar2.class);
      Assert.assertEquals(menuBar.menu.id, "file");
      Assert.assertEquals(menuBar.menu.value, "File");
      Assert.assertNotNull(menuBar.menu.popup);
      Assert.assertNotNull(menuBar.menu.popup.menuitem);
      Assert.assertTrue(menuBar.menu.popup.menuitem instanceof ArrayList);
      Assert.assertSame(3, menuBar.menu.popup.menuitem.size());
   }

   @Test
   public void testParser04() throws Exception
   {
      ObjectMapper objectMapper = JsonFactory.create();

      File file = new File("src/test/resources/AllTypes.json");
      try (InputStream is = new FileInputStream(file)) {
         AllType allType = objectMapper.readValue(is, AllType.class);
         Assert.assertTrue(allType.myDouble == 1.2);
      }
   }

   public void loadTest() throws Exception
   {
      for (int i = 0; i < 10_000_000; i++) {
         ObjectMapper objectMapper = JsonFactory.create();
         MenuBar2 menuBar = objectMapper.readValue(bais, MenuBar2.class);
         Assert.assertEquals(menuBar.menu.id, "file");
         bais.reset();
      }
   }
}
