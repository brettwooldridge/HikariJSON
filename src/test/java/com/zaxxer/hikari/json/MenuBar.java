package com.zaxxer.hikari.json;

import java.util.List;

@SuppressWarnings("unused")
public class MenuBar
{
   private Menu menu;

   public static class Menu
   {
      private String id;
      private String value;
      private Popup popup;
   }

   public static class Popup
   {
      private List<MenuItem> menuitem;
   }

   public static class MenuItem
   {
      String value;
      String onclick;
   }
}