/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.levels;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

import java.awt.Color;

import pixelitor.filters.lookup.LuminanceLookup;

/**
 * Represents the currently edited color channels.
 */
public enum Channel {

   RGB("RGB", "rgb", BLACK) { //$NON-NLS-1$ //$NON-NLS-2$

      @Override
      public Color getDarkColor() {
         return BLACK;
      }

      @Override
      public Color getDrawColor(final boolean active, final boolean darkTheme) {
         if (darkTheme) {
            return active ? WHITE : FADED_WHITE;
         } else {
            return super.getDrawColor(active, darkTheme);
         }
      }

      @Override
      public double getIntensity(final int r, final int g, final int b) {
         return LuminanceLookup.from(r, g, b);
      }

      @Override
      public Color getLightColor() {
         return WHITE;
      }
   },

   RED(("red"), "red", Color.RED) { //$NON-NLS-1$ //$NON-NLS-2$

      @Override
      public Color getDarkColor() {
         return DARK_CYAN;
      }

      @Override
      public double getIntensity(final int r, final int g, final int b) {
         return r;
      }

      @Override
      public Color getLightColor() {
         return LIGHT_PINK;
      }
   },

   GREEN(("green"), "green", Color.GREEN) { //$NON-NLS-1$ //$NON-NLS-2$

      @Override
      public Color getDarkColor() {
         return DARK_PURPLE;
      }

      @Override
      public double getIntensity(final int r, final int g, final int b) {
         return g;
      }

      @Override
      public Color getLightColor() {
         return LIGHT_GREEN;
      }
   },

   BLUE(("blue"), "blue", Color.BLUE) { //$NON-NLS-1$ //$NON-NLS-2$

      @Override
      public Color getDarkColor() {
         return DARK_YELLOW_GREEN;
      }

      @Override
      public double getIntensity(final int r, final int g, final int b) {
         return b;
      }

      @Override
      public Color getLightColor() {
         return LIGHT_BLUE;
      }
   };

   private static final Color FADED_WHITE       = new Color(0x64_FF_FF_FF, true);
   private static final Color LIGHT_PINK        = new Color(255, 128, 128);
   private static final Color LIGHT_GREEN       = new Color(128, 255, 128);
   private static final Color LIGHT_BLUE        = new Color(128, 128, 255);

   private static final Color DARK_YELLOW_GREEN = new Color(128, 128, 0);
   private static final Color DARK_CYAN         = new Color(0, 128, 128);
   private static final Color DARK_PURPLE       = new Color(128, 0, 128);

   private final String       name;
   private final String       presetKey;
   private final Color        color;
   private final Color        inactiveColor;

   Channel(final String name, final String presetKey, final Color color) {

      this.name = name;
      this.presetKey = presetKey;
      this.color = color;

      inactiveColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
   }

//   public static EnumParam<Channel> asParam() {
//      return new EnumParam<>("Channel", Channel.class);
//   }

   public abstract Color getDarkColor();

   public Color getDrawColor(final boolean active, final boolean darkTheme) {
      return active ? color : inactiveColor;
   }

   /**
    * Calculates the intensity of this channel based on the given RGB values.
    */
   public abstract double getIntensity(int r, int g, int b);

   public abstract Color getLightColor();

   public String getName() {
      return name;
   }

   public String getPresetKey() {
      return presetKey;
   }

   @Override
   public String toString() {
      return name;
   }
}
