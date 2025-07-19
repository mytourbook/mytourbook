/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.Objects;

import net.tourbook.common.UI;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

public class TourAction implements Cloneable {

   public String             actionClassName;
   public String             actionText;

   public TourActionCategory actionCategory;
   public IActionProvider    actionProvider;

   private Image             _image;
   private Image             _image_Disabled;

   private ImageDescriptor   _imageDescriptor;
   private ImageDescriptor   _imageDescriptor_Disabled;

   public boolean            isChecked;
   public boolean            isCategory;

   public TourAction(final String className,
                     final String text,
                     final ImageDescriptor imageDescriptor,
                     final ImageDescriptor imageDescriptor_Disabled,
                     final TourActionCategory category) {

      actionClassName = className;
      actionCategory = category;

      _imageDescriptor = imageDescriptor;
      _imageDescriptor_Disabled = imageDescriptor_Disabled;

      final String unescapedText = text.replaceAll(UI.SYMBOL_AMPERSAND, UI.EMPTY_STRING);

      actionText = unescapedText;
   }

   public TourAction(final String className,
                     final String text,
                     final TourActionCategory category) {

      this(className,

            text,

            null,
            null,

            category);
   }

   /**
    * This is an action separator
    *
    * @param text
    * @param category
    */
   public TourAction(final String text,
                     final TourActionCategory category) {

      actionText = text;
      actionCategory = category;

      isCategory = true;
   }

   @Override
   public TourAction clone() {

      TourAction newTourAction = null;

      try {

         newTourAction = (TourAction) super.clone();

      } catch (final CloneNotSupportedException e) {

         e.printStackTrace();
      }

      return newTourAction;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final TourAction other = (TourAction) obj;

      return actionCategory == other.actionCategory
            && Objects.equals(actionClassName, other.actionClassName);
   }

   public String getCategoryClassName() {

      return actionCategory.getClass().getCanonicalName()
            + '#'
            + actionCategory.name();
   }

   public Image getImage() {

      if (_imageDescriptor != null && _image == null) {

         _image = _imageDescriptor.createImage();
      }

      return _image;
   }

   public Image getImageDisabled() {

      if (_imageDescriptor_Disabled != null && _image_Disabled == null) {

         _image_Disabled = _imageDescriptor_Disabled.createImage();
      }

      if (_image_Disabled == null) {

         // create disabled image from the enabled image

         final Image image = getImage();

         if (image != null && image.isDisposed() == false) {

            _image_Disabled = new Image(_image.getDevice(), _image, SWT.IMAGE_DISABLE);
         }
      }

      return _image_Disabled;
   }

   @Override
   public int hashCode() {

      return Objects.hash(actionCategory, actionClassName);
   }

   @Override
   public String toString() {

      return "TourAction" + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + " actionText     = " + actionText + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + " actionCategory = " + actionCategory + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "\n"; //$NON-NLS-1$
   }
}
