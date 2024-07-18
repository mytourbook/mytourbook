/*******************************************************************************
 * Copyright (C) 2020, 2024 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.photo.Photo;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2D map photo options
 */
public class SlideoutMap2_PhotoOptions extends ToolbarSlideout implements IActionResetToDefault {

   public static final String    STATE_IS_PRELOAD_HQ_IMAGES         = "STATE_IS_PRELOAD_HQ_IMAGES";    //$NON-NLS-1$
   public static final boolean   STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT = false;

   static final String           STATE_PHOTO_IMAGE_SIZE             = "STATE_PHOTO_IMAGE_SIZE";        //$NON-NLS-1$
   static final String           STATE_PHOTO_IMAGE_SIZE_TINY        = "STATE_PHOTO_IMAGE_SIZE_TINY";   //$NON-NLS-1$
   static final String           STATE_PHOTO_IMAGE_SIZE_SMALL       = "STATE_PHOTO_IMAGE_SIZE_SMALL";  //$NON-NLS-1$
   static final String           STATE_PHOTO_IMAGE_SIZE_MEDIUM      = "STATE_PHOTO_IMAGE_SIZE_MEDIUM"; //$NON-NLS-1$
   static final String           STATE_PHOTO_IMAGE_SIZE_LARGE       = "STATE_PHOTO_IMAGE_SIZE_LARGE";  //$NON-NLS-1$

   static final int              MAP_IMAGE_DEFAULT_SIZE_TINY        = 20;
   static final int              MAP_IMAGE_DEFAULT_SIZE_SMALL       = 50;
   static final int              MAP_IMAGE_DEFAULT_SIZE_MEDIUM      = 100;
   static final int              MAP_IMAGE_DEFAULT_SIZE_LARGE       = 200;

   private static final int      MIN_IMAGE_SIZE                     = 3;

   /**
    * This value is small because a map do not yet load large images !!!
    */
   private static final int      MAX_IMAGE_SIZE                     = 200;

   private IDialogSettings       _state;

   private ActionResetToDefaults _actionRestoreDefaults;

   private Map2View              _map2View;

   private MouseWheelListener    _defaultMouseWheelListener;
   private SelectionListener     _defaultSelectedListener;

   private int                   _imageSize;

   /*
    * UI controls
    */
   private Button  _chkPreloadHQImages;

   private Button  _radioImageSize_Tiny;
   private Button  _radioImageSize_Small;
   private Button  _radioImageSize_Medium;
   private Button  _radioImageSize_Large;

   private Spinner _spinnerImageSize_Tiny;
   private Spinner _spinnerImageSize_Small;
   private Spinner _spinnerImageSize_Medium;
   private Spinner _spinnerImageSize_Large;

   enum ImageSize {

      TINY, SMALL, MEDIUM, LARGE
   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMap2_PhotoOptions(final Control ownerControl,
                                    final ToolBar toolBar,
                                    final Map2View map2View,
                                    final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;

   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
         createUI_20_ImageSize(shellContainer);
         createUI_30_Options(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_Map_PhotoOptions_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_ImageSize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * label: displayed photos
             */
            final Label label = new Label(container, SWT.NO_FOCUS);
            label.setText(Messages.Photo_Properties_Label_Size);
            label.setToolTipText(Messages.Photo_Properties_Label_ThumbnailSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .span(2, 1)
                  .applyTo(label);
         }
         {
            /*
             * Image size: Tiny
             */
            _radioImageSize_Tiny = new Button(container, SWT.RADIO);
            _radioImageSize_Tiny.setText(OtherMessages.APP_SIZE_TINY_NAME);
            _radioImageSize_Tiny.addSelectionListener(_defaultSelectedListener);

            _spinnerImageSize_Tiny = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Tiny.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Tiny.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Tiny.setIncrement(1);
            _spinnerImageSize_Tiny.setPageIncrement(10);
            _spinnerImageSize_Tiny.addSelectionListener(_defaultSelectedListener);
            _spinnerImageSize_Tiny.addMouseWheelListener(_defaultMouseWheelListener);
         }
         {
            /*
             * Image size: Small
             */
            _radioImageSize_Small = new Button(container, SWT.RADIO);
            _radioImageSize_Small.setText(OtherMessages.APP_SIZE_SMALL_NAME);
            _radioImageSize_Small.addSelectionListener(_defaultSelectedListener);

            _spinnerImageSize_Small = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Small.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Small.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Small.setIncrement(1);
            _spinnerImageSize_Small.setPageIncrement(10);
            _spinnerImageSize_Small.addSelectionListener(_defaultSelectedListener);
            _spinnerImageSize_Small.addMouseWheelListener(_defaultMouseWheelListener);
         }
         {
            /*
             * Image size: Medium
             */
            _radioImageSize_Medium = new Button(container, SWT.RADIO);
            _radioImageSize_Medium.setText(OtherMessages.APP_SIZE_MEDIUM_NAME);
            _radioImageSize_Medium.addSelectionListener(_defaultSelectedListener);

            _spinnerImageSize_Medium = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Medium.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Medium.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Medium.setIncrement(1);
            _spinnerImageSize_Medium.setPageIncrement(10);
            _spinnerImageSize_Medium.addSelectionListener(_defaultSelectedListener);
            _spinnerImageSize_Medium.addMouseWheelListener(_defaultMouseWheelListener);
         }
         {
            /*
             * Image size: Large
             */
            _radioImageSize_Large = new Button(container, SWT.RADIO);
            _radioImageSize_Large.setText(OtherMessages.APP_SIZE_LARGE_NAME);
            _radioImageSize_Large.addSelectionListener(_defaultSelectedListener);

            _spinnerImageSize_Large = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Large.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Large.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Large.setIncrement(1);
            _spinnerImageSize_Large.setPageIncrement(10);
            _spinnerImageSize_Large.addSelectionListener(_defaultSelectedListener);
            _spinnerImageSize_Large.addMouseWheelListener(_defaultMouseWheelListener);
         }
      }
   }

   private void createUI_30_Options(final Composite parent) {

      _chkPreloadHQImages = new Button(parent, SWT.CHECK);
      _chkPreloadHQImages.setText("&Preload photo tooltip images");
      _chkPreloadHQImages.setToolTipText("This can be very very CPU intensive");
      _chkPreloadHQImages.addSelectionListener(_defaultSelectedListener);
      GridDataFactory.fillDefaults()
            .indent(0, 20)
            .applyTo(_chkPreloadHQImages);

   }

   private void enableControls() {

      _spinnerImageSize_Large.setEnabled(_radioImageSize_Large.getSelection());
      _spinnerImageSize_Medium.setEnabled(_radioImageSize_Medium.getSelection());
      _spinnerImageSize_Small.setEnabled(_radioImageSize_Small.getSelection());
      _spinnerImageSize_Tiny.setEnabled(_radioImageSize_Tiny.getSelection());
   }

   private int getSelectedImageSize() {

      int imageSize;

      if (_radioImageSize_Large.getSelection()) {

         imageSize = _spinnerImageSize_Large.getSelection();

      } else if (_radioImageSize_Medium.getSelection()) {

         imageSize = _spinnerImageSize_Medium.getSelection();

      } else if (_radioImageSize_Small.getSelection()) {

         imageSize = _spinnerImageSize_Small.getSelection();

      } else {

         imageSize = _spinnerImageSize_Tiny.getSelection();
      }

      return imageSize;
   }

   private void initUI() {

      _defaultSelectedListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10, true);
         onChangeUI();
      };
   }

   @Override
   protected boolean isCenterHorizontal() {
      return true;
   }

   private void onChangeUI() {

      _imageSize = getSelectedImageSize();

      saveState();

      updateMap();

      enableControls();
   }

   @Override
   public void resetToDefaults() {

      _imageSize = MAP_IMAGE_DEFAULT_SIZE_MEDIUM;

      _radioImageSize_Large.setSelection(false);
      _radioImageSize_Medium.setSelection(true);
      _radioImageSize_Small.setSelection(false);
      _radioImageSize_Tiny.setSelection(false);

      _spinnerImageSize_Large.setSelection(MAP_IMAGE_DEFAULT_SIZE_LARGE);
      _spinnerImageSize_Medium.setSelection(MAP_IMAGE_DEFAULT_SIZE_MEDIUM);
      _spinnerImageSize_Small.setSelection(MAP_IMAGE_DEFAULT_SIZE_SMALL);
      _spinnerImageSize_Tiny.setSelection(MAP_IMAGE_DEFAULT_SIZE_TINY);

      _chkPreloadHQImages.setSelection(STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT);

      enableControls();

      updateMap();
   }

   private void restoreState() {

      _chkPreloadHQImages.setSelection(Util.getStateBoolean(_state, STATE_IS_PRELOAD_HQ_IMAGES, STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT));

      final Enum<ImageSize> imageSize = Util.getStateEnum(_state, STATE_PHOTO_IMAGE_SIZE, ImageSize.MEDIUM);

      final int imageSizeLarge = Util.getStateInt(_state, STATE_PHOTO_IMAGE_SIZE_LARGE, MAP_IMAGE_DEFAULT_SIZE_LARGE);
      final int imageSizeMedium = Util.getStateInt(_state, STATE_PHOTO_IMAGE_SIZE_MEDIUM, MAP_IMAGE_DEFAULT_SIZE_MEDIUM);
      final int imageSizeSmall = Util.getStateInt(_state, STATE_PHOTO_IMAGE_SIZE_SMALL, MAP_IMAGE_DEFAULT_SIZE_SMALL);
      final int imageSizeTiny = Util.getStateInt(_state, STATE_PHOTO_IMAGE_SIZE_TINY, MAP_IMAGE_DEFAULT_SIZE_TINY);

      _spinnerImageSize_Large.setSelection(imageSizeLarge);
      _spinnerImageSize_Medium.setSelection(imageSizeMedium);
      _spinnerImageSize_Small.setSelection(imageSizeSmall);
      _spinnerImageSize_Tiny.setSelection(imageSizeTiny);

      if (imageSize.equals(ImageSize.LARGE)) {

         _imageSize = imageSizeLarge;
         _radioImageSize_Large.setSelection(true);

      } else if (imageSize.equals(ImageSize.MEDIUM)) {

         _imageSize = imageSizeMedium;
         _radioImageSize_Medium.setSelection(true);

      } else if (imageSize.equals(ImageSize.SMALL)) {

         _imageSize = imageSizeSmall;
         _radioImageSize_Small.setSelection(true);

      } else {

         _imageSize = imageSizeTiny;
         _radioImageSize_Tiny.setSelection(true);
      }

      Photo.setMapImageRequestedSize(_imageSize);
   }

   private void saveState() {

      _state.put(STATE_IS_PRELOAD_HQ_IMAGES, _chkPreloadHQImages.getSelection());

// SET_FORMATTING_OFF

      final Enum<ImageSize> selectedSize =

      _radioImageSize_Large   .getSelection()   ? ImageSize.LARGE    :
      _radioImageSize_Medium  .getSelection()   ? ImageSize.MEDIUM   :
      _radioImageSize_Small   .getSelection()   ? ImageSize.SMALL    :
                                                  ImageSize.TINY;

      _state.put(STATE_PHOTO_IMAGE_SIZE_LARGE,  _spinnerImageSize_Large.getSelection());
      _state.put(STATE_PHOTO_IMAGE_SIZE_MEDIUM, _spinnerImageSize_Medium.getSelection());
      _state.put(STATE_PHOTO_IMAGE_SIZE_SMALL,  _spinnerImageSize_Small.getSelection());
      _state.put(STATE_PHOTO_IMAGE_SIZE_TINY,   _spinnerImageSize_Tiny.getSelection());

      Util.setStateEnum(_state, STATE_PHOTO_IMAGE_SIZE, selectedSize);

// SET_FORMATTING_ON

   }

   private void updateMap() {

      Photo.setMapImageRequestedSize(_imageSize);

      final Map2 map2 = _map2View.getMap();

      map2.updatePhotoOptions();

      map2.paint();
   }

}
