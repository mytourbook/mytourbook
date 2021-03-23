/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;

class ActionRemovePhoto extends Action {

   private final PhotoGallery _photoGallery;
   private boolean            _isInModifyTour;

   public ActionRemovePhoto(final PhotoGallery photoGallery) {

      super(Messages.Action_PhotosAndTours_RemovePhoto, Action.AS_PUSH_BUTTON);

      _photoGallery = photoGallery;

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
   }

   public boolean isInModifyTour() {
      return _isInModifyTour;
   }

   @Override
   public void run() {

      _isInModifyTour = true;
      {
         TourManager.tourPhoto_Remove(_photoGallery);
      }
      _isInModifyTour = false;
   }
}
