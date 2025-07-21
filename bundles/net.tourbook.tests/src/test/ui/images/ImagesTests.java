/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package ui.images;

import java.lang.reflect.Field;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test if all declared image files are available
 */
public class ImagesTests {

   @Test
   void checkImages_Common() throws IllegalAccessException {

      System.out.println("\nMissing image files in %s\n".formatted(CommonImages.class.getName()));

      String assertMessage = null;

      for (final Field field : CommonImages.class.getFields()) {

         final String fieldName = field.getName();
         final Object fieldValue = field.get(null);

         if (fieldValue instanceof final String imageFileName) {

            final ImageDescriptor imageDescriptor = CommonActivator.getImageDescriptor(imageFileName);

            if (imageDescriptor == null) {

               /*
                * The created java code makes it easier to find failures when this code is copied
                * into the java class
                */
               final String message = "   public static final String %-40s = \"%s\";".formatted(fieldName, imageFileName);

               if (assertMessage == null) {
                  assertMessage = message;
               }

               System.out.println(message);
            }
         }
      }

      // log first assertion
      if (assertMessage != null) {
         Assertions.fail(assertMessage);
      }

   }

   @Test
   void checkImages_Tourbook() throws IllegalAccessException {

      System.out.println("\nMissing image files in %s\n".formatted(Images.class.getName()));

      String assertMessage = null;

      for (final Field field : Images.class.getFields()) {

         final String fieldName = field.getName();
         final Object fieldValue = field.get(null);

         if (fieldValue instanceof final String imageFileName) {

            final ImageDescriptor imageDescriptor = TourbookPlugin.getImageDescriptor(imageFileName);

            if (imageDescriptor == null) {

               /*
                * The created java code makes it easier to find failures when this code is copied
                * into the java class
                */
               final String message = "   public static final String %-40s = \"%s\";".formatted(fieldName, imageFileName);

               if (assertMessage == null) {
                  assertMessage = message;
               }

               System.out.println(message);
            }
         }
      }

      // log first assertion
      if (assertMessage != null) {
         Assertions.fail(assertMessage);
      }
   }
}
