/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.filters;

import static pixelitor.utils.ImageUtils.isGrayscale;

import java.awt.image.BufferedImage;
import java.io.Serial;

import pixelitor.utils.ImageUtils;

/**
 * Base class for all filters and color adjustments in Pixelitor.
 * A filter transforms an image into another image.
 */
public abstract class Filter /* implements Serializable, PresetOwner, Debuggable */ {

   @Serial
   private static final long serialVersionUID = 1L;

   // used for making sure that there are no
   // unnecessary filter executions triggered
   public static long       executionCount = 0;

   private transient String name;

   protected Filter() {}

   /**
    * Whether this filter can be used as a smart filter.
    * One condition is that the filter must have a no-arg constructor.
    * Another condition is that is must support user presets.
    */
   public boolean canBeSmart() {
      return true;
   }

   /**
    * Determines if a default destination image should be created
    * before running the filter.
    * Override this method to return false if the filter creates
    * the destination image itself.
    */
   protected boolean createDefaultDestImg() {
      return true;
   }

   public String getName() {
      if (name != null) {
         return name;
      }
      // We cannot assume that a name always exists because the
      // filter can be created directly without being put in a menu.
      return getClass().getSimpleName();
   }

   /**
    * Returns a string representation of the filter's current parameters.
    */
   public String paramsAsString() {
      return ""; //$NON-NLS-1$
   }

   public void setName(final String name) {
      this.name = name;
   }

   /**
    * Whether this filter can process grayscale
    * images (TYPE_BYTE_GRAY) used in layer masks.
    */
   public boolean supportsGray() {
      return true;
   }

   /**
    * The core image transformation logic.
    */
   protected abstract BufferedImage transform(BufferedImage src, BufferedImage dest);

   /**
    * Executes the filter transformation while handling
    * conversion for grayscale images if needed.
    */
   public BufferedImage transformImage(BufferedImage src) {

      boolean grayConversion = false;

      if (isGrayscale(src) && !supportsGray()) {

         // converting the image to RGB, because the filter
         // doesn't support the grayscale image of a layer mask
         grayConversion = true;
         src = ImageUtils.toSysCompatibleImage(src);
      }

      BufferedImage dest = createDefaultDestImg() ? ImageUtils.createImageWithSameCM(src) : null;

      assert src.getType() != BufferedImage.TYPE_CUSTOM;

      dest = transform(src, dest);

      assert dest.getType() != BufferedImage.TYPE_CUSTOM : "filter = " + getName(); //$NON-NLS-1$

      if (grayConversion) { // convert the result back
         dest = ImageUtils.convertToGrayscaleImage(dest);
      }

      executionCount++;

      assert dest != null : getName() + " returned null image"; //$NON-NLS-1$

      return dest;
   }

}
