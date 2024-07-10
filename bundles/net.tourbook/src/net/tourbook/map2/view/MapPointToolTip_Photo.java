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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.PaintedMapPoint;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * Slideout for all 2D map locations and marker
 */
public class MapPointToolTip_Photo extends AdvancedSlideout {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.map2.view.MapPointToolTip_Photo"); //$NON-NLS-1$

   private Map2                         _map2;

   private PaintedMapPoint              _hoveredMapPoint;

   /*
    * UI controls
    */
   private Composite   _contentContainer;

   private ImageCanvas _photoImageCanvas;

   private class PhotoImageLoaderCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         if (isUpdateUI) {

            if (_hoveredMapPoint != null) {

               _map2.getShell().getDisplay().asyncExec(() -> {

                  if (_hoveredMapPoint != null) {

                     updateUI_Photo(_hoveredMapPoint);
                  }
               });
            }
         }
      }
   }

   public MapPointToolTip_Photo(final Map2 map2) {

      super(map2, _state, null);

      _map2 = map2;

      setTitleText("Photo");

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      super.close();
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      createUI(parent);

      updateUI_Photo(_hoveredMapPoint);
   }

   private Composite createUI(final Composite parent) {

      _contentContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_contentContainer);
      GridLayoutFactory.fillDefaults()
            .spacing(0, 0)
            .applyTo(_contentContainer);
//      _shellContainer.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         _photoImageCanvas = new ImageCanvas(_contentContainer, SWT.NONE);
         _photoImageCanvas.setIsSmoothImages(true);
         _photoImageCanvas.setStyle(SWT.CENTER);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_photoImageCanvas);
      }

      return _contentContainer;
   }

   @Override
   protected Rectangle getParentBounds() {

      // ignore, is overwritten with getToolTipLocation()
      return null;
   }

   /**
    * @param photo
    * @param map
    * @param tile
    *
    * @return Returns the photo image or <code>null</code> when image is not loaded.
    */
   private Image getPhotoImage(final Photo photo) {

      Image photoImage = null;

      final ImageQuality requestedImageQuality = ImageQuality.HQ;

      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not yet loaded

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new PhotoImageLoaderCallback();

            PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
         }
      }

      return photoImage;
   }

   @Override
   public Point getToolTipLocation(final Point slideoutSize) {

      final Rectangle labelBounds = _hoveredMapPoint.labelRectangle;
      final int labelWidth = labelBounds.width;
      final int labelWidth2 = labelWidth / 2;
      final int labelHeight = labelBounds.height;

      final int labelLeft = labelBounds.x;
      final int labelRight = labelLeft + labelWidth;
      final int labelTop = labelBounds.y;
      final int labelBottom = labelBounds.y + labelHeight;

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;
      final int geoPointDevX = mapPoint.geoPointDevX;
      final int geoPointDevY = mapPoint.geoPointDevY;

      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      final int tooltipWidth = slideoutSize.x;
      final int tooltipHeight = slideoutSize.y;

      final int devX = mapDisplayPosition.x + labelLeft - tooltipWidth;
      final int devY = mapDisplayPosition.y + labelTop + labelHeight - tooltipHeight;

      System.out.println(UI.timeStamp() + " labelBounds: " + labelBounds);
// TODO remove SYSTEM.OUT.PRINTLN

      return new Point(devX, devY);
   }

   @Override
   protected void onFocus() {

   }

   public void setupPhoto(final PaintedMapPoint hoveredMapPoint) {

      if (hoveredMapPoint == null) {

         _hoveredMapPoint = null;

         hideNow();

         return;
      }

      final boolean isOtherMapPoint = _hoveredMapPoint != hoveredMapPoint;

      if (isOtherMapPoint && isVisible()) {
         hideNow();
      }

      if (isOtherMapPoint == false) {
         return;
      }

      System.out.println(UI.timeStamp() + " setupPhoto: " + hoveredMapPoint);
// TODO remove SYSTEM.OUT.PRINTLN

      _hoveredMapPoint = hoveredMapPoint;

      open(false);

      updateUI_Photo(hoveredMapPoint);
   }

   private void updateUI_Photo(final PaintedMapPoint hoveredMapPoint) {

      if (hoveredMapPoint == null) {
         return;
      }

      if (_photoImageCanvas.isDisposed()) {
         return;
      }

      final Image photoImage = getPhotoImage(hoveredMapPoint.mapPoint.photo);

      _photoImageCanvas.setImage(photoImage, false);
   }

}
