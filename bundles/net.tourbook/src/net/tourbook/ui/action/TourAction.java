/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class TourAction {

   public Object           actionClass;
   public String           actionText;

   private Image           _image;
   private ImageDescriptor _imageDescriptor;

   public boolean          isChecked;

   public TourAction(final Object actionObject, final String text, final ImageDescriptor imageDescriptor) {

      actionClass = actionObject;
      _imageDescriptor = imageDescriptor;

      final String unescapedText = text.replaceAll(UI.SYMBOL_AMPERSAND, UI.EMPTY_STRING);

      actionText = unescapedText;
   }

   public Image getImage() {

      if (_imageDescriptor != null && _image == null) {

         _image = _imageDescriptor.createImage();
      }

      return _image;
   }
}
