/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageGallery;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoLoadManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;

public class GalleryActionBar {

   private static final IPreferenceStore _prefStore = PhotoActivator.getPrefStore();

   private ImageGallery                  _imageGallery;

   private boolean                       _isShowThumbsize;
   private boolean                       _isShowCustomActionBar;

   /*
    * UI controls
    */
   private Composite          _containerActionBar;
   private Composite          _containerCustomActionBar;
   private Composite          _containerImageSize;

   private Spinner            _spinnerThumbSize;

   private ImageSizeIndicator _canvasImageSizeIndicator;

   public GalleryActionBar(final Composite parent,
                           final ImageGallery imageGallery,
                           final boolean isShowThumbsize,
                           final boolean isShowCustomActionBar) {

      _imageGallery = imageGallery;

      _isShowThumbsize = isShowThumbsize;
      _isShowCustomActionBar = isShowCustomActionBar;

      if (isShowThumbsize || isShowCustomActionBar) {
         createUI_10_ActionBar(parent);
      }
   }

   private void createUI_10_ActionBar(final Composite parent) {

      int columns = 0;

      if (_isShowCustomActionBar) {
         columns++;
      }
      if (_isShowThumbsize) {
         columns++;
      }

      final int hSpacing = 3;

      _containerActionBar = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerActionBar);
      GridLayoutFactory.fillDefaults()
            .numColumns(columns)
            .extendedMargins(2, 2, 2, 2)
            .spacing(hSpacing, 0)
            .applyTo(_containerActionBar);
//      _containerActionBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
      {
         /*
          * custom action bar container
          */
         if (_isShowCustomActionBar) {
            _containerCustomActionBar = new Composite(_containerActionBar, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerCustomActionBar);
            GridLayoutFactory.fillDefaults().applyTo(_containerCustomActionBar);
         }

         /*
          * thumb size
          */
         if (_isShowThumbsize) {
            _containerImageSize = new Composite(_containerActionBar, SWT.NONE);
            GridDataFactory.fillDefaults().grab(false, false).applyTo(_containerImageSize);
            GridLayoutFactory.fillDefaults().numColumns(2).spacing(hSpacing, 0).applyTo(_containerImageSize);
            {
               createUI_20_ImageSize(_containerImageSize);
               createUI_30_ImageSizeIndicator(_containerImageSize);
            }
         }
      }
   }

   /**
    * spinner: thumb size
    */
   private void createUI_20_ImageSize(final Composite parent) {

      _spinnerThumbSize = new Spinner(parent, SWT.BORDER);
      _spinnerThumbSize.setMinimum(ImageGallery.MIN_GALLERY_ITEM_WIDTH);
      _spinnerThumbSize.setMaximum(ImageGallery.MAX_GALLERY_ITEM_WIDTH);
      _spinnerThumbSize.setIncrement(1);
      _spinnerThumbSize.setPageIncrement(50);
      _spinnerThumbSize.setToolTipText(UI.IS_OSX
            ? Messages.Pic_Dir_Spinner_ThumbnailSize_Tooltip_OSX
            : Messages.Pic_Dir_Spinner_ThumbnailSize_Tooltip);

      _spinnerThumbSize.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
         _imageGallery.setThumbnailSize(_spinnerThumbSize.getSelection());
      }));

      _spinnerThumbSize.addMouseWheelListener(mouseEvent -> {
         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
         _imageGallery.setThumbnailSize(_spinnerThumbSize.getSelection());
      });

      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_spinnerThumbSize);
   }

   /**
    * canvas: image size indicator
    */
   private void createUI_30_ImageSizeIndicator(final Composite parent) {

      _canvasImageSizeIndicator = new ImageSizeIndicator(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .hint(8, _spinnerThumbSize.getSize().y)
//            .align(SWT.CENTER, SWT.CENTER)
            .applyTo(_canvasImageSizeIndicator);
   }

   public Composite getCustomContainer() {
      return _containerCustomActionBar;
   }

   public int getThumbnailSize() {
      return _spinnerThumbSize.getSelection();
   }

   public void restoreState(final IDialogSettings state, final int stateThumbSize) {

      if (_isShowThumbsize) {

         updateUI_ImageIndicatorTooltip();

         _spinnerThumbSize.setSelection(stateThumbSize);
      }
   }

   public void setThumbnailSizeVisibility(final boolean isVisible) {

      if (_isShowThumbsize) {

         final GridData gd = (GridData) _containerImageSize.getLayoutData();
         gd.widthHint = isVisible ? SWT.DEFAULT : 0;

         _containerImageSize.setVisible(isVisible);
         _spinnerThumbSize.setVisible(isVisible);
         _canvasImageSizeIndicator.setVisible(isVisible);

         _containerActionBar.layout(true, true);
      }
   }

   public void updateColors(final Color fgColor, final Color bgColor) {

      if (_containerActionBar == null) {
         // actions are not created
         return;
      }

      /*
       * set color in action bar only for Linux & Windows, setting color in OSX looks not very
       * good
       */
      if (UI.IS_OSX) {
         return;
      }

      updateColors_Children(_containerActionBar, fgColor, bgColor);
   }

   /**
    * ######################### recursive ###################### <br>
    *
    * @param parent
    * @param fgColor
    * @param bgColor
    */
   private void updateColors_Children(final Composite parent, final Color fgColor, final Color bgColor) {

      parent.setForeground(fgColor);
      parent.setBackground(bgColor);

      final Control[] children = parent.getChildren();
      if (children == null) {
         return;
      }

      for (final Control childControl : children) {
         if (childControl instanceof Composite) {
            updateColors_Children((Composite) childControl, fgColor, bgColor);
         }
      }
   }

   public void updateUI_AfterZoomInOut(final int imageSize) {

      if (_isShowThumbsize) {

         _spinnerThumbSize.setSelection(imageSize);

         final boolean isHqImage = imageSize > PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;

         _canvasImageSizeIndicator.setIndicator(isHqImage);
      }
   }

   public void updateUI_ImageIndicatorTooltip() {

      if (_isShowThumbsize) {

         _canvasImageSizeIndicator.setToolTipText(NLS.bind(
               Messages.Pic_Dir_ImageSizeIndicator_Tooltip,
               _prefStore.getString(IPhotoPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK)));
      }
   }
}
