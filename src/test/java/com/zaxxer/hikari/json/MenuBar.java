package com.zaxxer.hikari.json;

import java.util.List;

@SuppressWarnings("unused")
public class MenuBar
{
   public Menu menu;

   public static class Menu
   {
      public String id;
      public String value;
      public Popup popup;
   }

   public static class Popup
   {
      public List<MenuItem> menuitem;
   }

   public static class MenuItem
   {
      public String value;
      public String onclick;
   }
}