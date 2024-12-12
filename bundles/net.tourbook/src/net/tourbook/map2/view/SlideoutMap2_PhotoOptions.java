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
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapImageSize;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2D map photo options
 */
public class SlideoutMap2_PhotoOptions extends ToolbarSlideout implements
      IActionResetToDefault,
      IColorSelectorListener {

   public static final String      STATE_IS_PRELOAD_HQ_IMAGES            = "STATE_IS_PRELOAD_HQ_IMAGES";    //$NON-NLS-1$
   public static final boolean     STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT    = false;
   public static final String      STATE_IS_SHOW_THUMB_HQ_IMAGES         = "STATE_IS_SHOW_THUMB_HQ_IMAGES"; //$NON-NLS-1$
   public static final boolean     STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT = false;
   static final String             STATE_IS_SHOW_PHOTO_RATING            = "STATE_IS_SHOW_PHOTO_RATING";    //$NON-NLS-1$
   static final boolean            STATE_IS_SHOW_PHOTO_RATING_DEFAULT    = true;
   static final String             STATE_IS_SHOW_PHOTO_TOOLTIP           = "STATE_IS_SHOW_PHOTO_TOOLTIP";   //$NON-NLS-1$
   static final boolean            STATE_IS_SHOW_PHOTO_TOOLTIP_DEFAULT   = true;

   public static final String      STATE_PHOTO_IMAGE_SIZE                = "STATE_PHOTO_IMAGE_SIZE";        //$NON-NLS-1$
   public static final String      STATE_PHOTO_IMAGE_SIZE_TINY           = "STATE_PHOTO_IMAGE_SIZE_TINY";   //$NON-NLS-1$
   public static final String      STATE_PHOTO_IMAGE_SIZE_SMALL          = "STATE_PHOTO_IMAGE_SIZE_SMALL";  //$NON-NLS-1$
   public static final String      STATE_PHOTO_IMAGE_SIZE_MEDIUM         = "STATE_PHOTO_IMAGE_SIZE_MEDIUM"; //$NON-NLS-1$
   public static final String      STATE_PHOTO_IMAGE_SIZE_LARGE          = "STATE_PHOTO_IMAGE_SIZE_LARGE";  //$NON-NLS-1$

   private static final int        MIN_IMAGE_SIZE                        = 3;

   /**
    * This value is small because a map do not yet load large images !!!
    */
   private static final int        MAX_IMAGE_SIZE                        = 2000;

   private IDialogSettings         _state_Map2;

   private Map2View                _map2View;
   private Map2                    _map2;

   private ActionResetToDefaults   _actionRestoreDefaults;

   private MouseWheelListener      _defaultMouseWheelListener;
   private SelectionListener       _defaultSelectedListener;
   private IPropertyChangeListener _propertyChangeListener;

   private int                     _imageSize;

   /*
    * UI controls
    */
   private Button                _btnSwapTourPauseLabel_Color;

   private Button                _chkPreloadHQImages;
   private Button                _chkShowHQImages;
   private Button                _chkShowPhotoRating;

   private Button                _radioImageSize_Tiny;
   private Button                _radioImageSize_Small;
   private Button                _radioImageSize_Medium;
   private Button                _radioImageSize_Large;

   private Spinner               _spinnerImageSize_Tiny;
   private Spinner               _spinnerImageSize_Small;
   private Spinner               _spinnerImageSize_Medium;
   private Spinner               _spinnerImageSize_Large;

   private Label                 _lblHeapSize;

   private Link                  _linkDiscardImages;

   private ColorSelectorExtended _colorTourPauseLabel_Outline;
   private ColorSelectorExtended _colorTourPauseLabel_Fill;

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
      _map2 = map2View.getMap();
      _state_Map2 = map2State;
   }

   public static String formatSize(final long value) {

      return "%,d MB".formatted(value / 1024 / 1024); //$NON-NLS-1$
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
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

      updateUI_Memory();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
         createUI_30_ImageSize(shellContainer);
         createUI_40_Options(shellContainer);
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

   private void createUI_30_ImageSize(final Composite parent) {

      final GridDataFactory gdIndent = GridDataFactory.fillDefaults().indent(16, 0);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Image size
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
            _radioImageSize_Tiny.setText(OtherMessages.APP_SIZE_TINY_LABEL);
            _radioImageSize_Tiny.addSelectionListener(_defaultSelectedListener);
            gdIndent.applyTo(_radioImageSize_Tiny);

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
            _radioImageSize_Small.setText(OtherMessages.APP_SIZE_SMALL_LABEL);
            _radioImageSize_Small.addSelectionListener(_defaultSelectedListener);
            gdIndent.applyTo(_radioImageSize_Small);

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
            _radioImageSize_Medium.setText(OtherMessages.APP_SIZE_MEDIUM_LABEL);
            _radioImageSize_Medium.addSelectionListener(_defaultSelectedListener);
            gdIndent.applyTo(_radioImageSize_Medium);

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
            _radioImageSize_Large.setText(OtherMessages.APP_SIZE_LARGE_LABEL);
            _radioImageSize_Large.addSelectionListener(_defaultSelectedListener);
            gdIndent.applyTo(_radioImageSize_Large);

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

   private void createUI_40_Options(final Composite parent) {

      final String coloTooltipText = Messages.Slideout_Map_PhotoOptions_Label_SymbolColor_Tooltip;

      final GridDataFactory labelGridData = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .indent(0, 5)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Photo color
             */
            {
               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Slideout_Map_PhotoOptions_Label_SymbolColor);
               label.setToolTipText(coloTooltipText);
               labelGridData.applyTo(label);
            }
            {
               final Composite labelContainer = new Composite(container, SWT.NONE);
               GridLayoutFactory.fillDefaults().numColumns(3).applyTo(labelContainer);

               // outline/text color
               _colorTourPauseLabel_Outline = new ColorSelectorExtended(labelContainer);
               _colorTourPauseLabel_Outline.addListener(_propertyChangeListener);
               _colorTourPauseLabel_Outline.addOpenListener(this);
               _colorTourPauseLabel_Outline.setToolTipText(coloTooltipText);

               // background color
               _colorTourPauseLabel_Fill = new ColorSelectorExtended(labelContainer);
               _colorTourPauseLabel_Fill.addListener(_propertyChangeListener);
               _colorTourPauseLabel_Fill.addOpenListener(this);
               _colorTourPauseLabel_Fill.setToolTipText(coloTooltipText);

               // button: swap color
               _btnSwapTourPauseLabel_Color = new Button(labelContainer, SWT.PUSH);
               _btnSwapTourPauseLabel_Color.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
               _btnSwapTourPauseLabel_Color.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
               _btnSwapTourPauseLabel_Color.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                     selectionEvent -> onSwapPhotoColor()));
            }
         }
      }
      {
         /*
          * Show photo rating
          */
         _chkShowPhotoRating = new Button(parent, SWT.CHECK);
         _chkShowPhotoRating.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowPhotoRating);
         _chkShowPhotoRating.addSelectionListener(_defaultSelectedListener);
      }
      {
         /*
          * Show HQ photos
          */
         _chkShowHQImages = new Button(parent, SWT.CHECK);
         _chkShowHQImages.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowHqPhotoImages);
         _chkShowHQImages.setToolTipText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowHqPhotoImages_Tooltip);
         _chkShowHQImages.addSelectionListener(_defaultSelectedListener);
      }
      {
         /*
          * Preload photos
          */
         _chkPreloadHQImages = new Button(parent, SWT.CHECK);
         _chkPreloadHQImages.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_PreloadPhotoImages);
         _chkPreloadHQImages.setToolTipText(Messages.Slideout_Map_PhotoOptions_Checkbox_PreloadPhotoImages_Tooltip);
         _chkPreloadHQImages.addSelectionListener(_defaultSelectedListener);
      }
      {
         /*
          * Discard cached images
          */
         _linkDiscardImages = new Link(parent, SWT.NONE);
         _linkDiscardImages.setText(UI.createLinkText(Messages.Slideout_Map_PhotoOptions_Link_DiscardCachedImages));
         _linkDiscardImages.setToolTipText(Messages.Slideout_Map_PhotoOptions_Link_DiscardCachedImages_Tooltip);
         _linkDiscardImages.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onDiscardImages()));
      }
      {
         /*
          * Memory/heap size
          */
         _lblHeapSize = new Label(parent, SWT.NONE);
         _lblHeapSize.setText(UI.SPACE1);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_lblHeapSize);
      }
   }

   private void enableControls() {

      _spinnerImageSize_Large.setEnabled(_radioImageSize_Large.getSelection());
      _spinnerImageSize_Medium.setEnabled(_radioImageSize_Medium.getSelection());
      _spinnerImageSize_Small.setEnabled(_radioImageSize_Small.getSelection());
      _spinnerImageSize_Tiny.setEnabled(_radioImageSize_Tiny.getSelection());
   }

   private int getSelectedImageSize() {

      if (_radioImageSize_Large.getSelection()) {

         return _spinnerImageSize_Large.getSelection();

      } else if (_radioImageSize_Medium.getSelection()) {

         return _spinnerImageSize_Medium.getSelection();

      } else if (_radioImageSize_Small.getSelection()) {

         return _spinnerImageSize_Small.getSelection();

      } else {

         return _spinnerImageSize_Tiny.getSelection();
      }
   }

   private void initUI() {

      _defaultSelectedListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10, true);
         onChangeUI();
      };

      _propertyChangeListener = propertyChangeEvent -> onChangeUI();
   }

   private void onChangeUI() {

      _imageSize = getSelectedImageSize();

      saveState();

      updateMap();

      updateUI_Memory();

      enableControls();
   }

   private void onDiscardImages() {

      PhotoLoadManager.stopImageLoading(true);
      PhotoLoadManager.removeInvalidImageFiles();

      PhotoImageCache.disposeAll();

      repaintMap();

      System.gc();

      updateUI_Memory();
   }

   private void onSwapPhotoColor() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final RGB fgColor = mapConfig.photoOutline_RGB;
      final RGB bgColor = mapConfig.photoFill_RGB;

      mapConfig.photoOutline_RGB = bgColor;
      mapConfig.photoFill_RGB = fgColor;

      mapConfig.setupColors();

      updateUI_FromConfig();

      repaintMap();
   }

   private void repaintMap() {

      _map2View.getMap().paint();
   }

   @Override
   public void resetToDefaults() {

      _imageSize = _map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM;

      _radioImageSize_Large.setSelection(false);
      _radioImageSize_Medium.setSelection(true);
      _radioImageSize_Small.setSelection(false);
      _radioImageSize_Tiny.setSelection(false);

      _spinnerImageSize_Large.setSelection(_map2.MAP_IMAGE_DEFAULT_SIZE_LARGE);
      _spinnerImageSize_Medium.setSelection(_map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);
      _spinnerImageSize_Small.setSelection(_map2.MAP_IMAGE_DEFAULT_SIZE_SMALL);
      _spinnerImageSize_Tiny.setSelection(_map2.MAP_IMAGE_DEFAULT_SIZE_TINY);

      _chkPreloadHQImages.setSelection(STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT);
      _chkShowHQImages.setSelection(STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT);
      _chkShowPhotoRating.setSelection(STATE_IS_SHOW_PHOTO_RATING_DEFAULT);

      /*
       * Update config
       */
      final Map2Config config = Map2ConfigManager.getActiveConfig();

      config.photoFill_RGB = Map2ConfigManager.DEFAULT_PHOTO_FILL_RGB;
      config.photoOutline_RGB = Map2ConfigManager.DEFAULT_PHOTO_OUTLINE_RGB;

      config.setupColors();

      updateUI_FromConfig();

      saveState();

      enableControls();
      updateMap();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkPreloadHQImages  .setSelection(Util.getStateBoolean(_state_Map2, STATE_IS_PRELOAD_HQ_IMAGES,      STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT));
      _chkShowHQImages     .setSelection(Util.getStateBoolean(_state_Map2, STATE_IS_SHOW_THUMB_HQ_IMAGES,   STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT));
      _chkShowPhotoRating  .setSelection(Util.getStateBoolean(_state_Map2, STATE_IS_SHOW_PHOTO_RATING,      STATE_IS_SHOW_PHOTO_RATING_DEFAULT));

      final Enum<MapImageSize> imageSize = Util.getStateEnum(_state_Map2, STATE_PHOTO_IMAGE_SIZE, MapImageSize.MEDIUM);

      final int imageSizeLarge   = Util.getStateInt(_state_Map2, STATE_PHOTO_IMAGE_SIZE_LARGE,  _map2.MAP_IMAGE_DEFAULT_SIZE_LARGE);
      final int imageSizeMedium  = Util.getStateInt(_state_Map2, STATE_PHOTO_IMAGE_SIZE_MEDIUM, _map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);
      final int imageSizeSmall   = Util.getStateInt(_state_Map2, STATE_PHOTO_IMAGE_SIZE_SMALL,  _map2.MAP_IMAGE_DEFAULT_SIZE_SMALL);
      final int imageSizeTiny    = Util.getStateInt(_state_Map2, STATE_PHOTO_IMAGE_SIZE_TINY,   _map2.MAP_IMAGE_DEFAULT_SIZE_TINY);

      _spinnerImageSize_Large .setSelection(imageSizeLarge);
      _spinnerImageSize_Medium.setSelection(imageSizeMedium);
      _spinnerImageSize_Small .setSelection(imageSizeSmall);
      _spinnerImageSize_Tiny  .setSelection(imageSizeTiny);

// SET_FORMATTING_ON

      if (imageSize.equals(MapImageSize.LARGE)) {

         _imageSize = imageSizeLarge;
         _radioImageSize_Large.setSelection(true);

      } else if (imageSize.equals(MapImageSize.MEDIUM)) {

         _imageSize = imageSizeMedium;
         _radioImageSize_Medium.setSelection(true);

      } else if (imageSize.equals(MapImageSize.SMALL)) {

         _imageSize = imageSizeSmall;
         _radioImageSize_Small.setSelection(true);

      } else {

         _imageSize = imageSizeTiny;
         _radioImageSize_Tiny.setSelection(true);
      }

      Photo.setMap2ImageRequestedSize(_imageSize);

      updateUI_FromConfig();
   }

   private void saveState() {

      final Map2Config config = Map2ConfigManager.getActiveConfig();

      final boolean isShowPhotoRating = _chkShowPhotoRating.getSelection();

      _state_Map2.put(STATE_IS_PRELOAD_HQ_IMAGES, _chkPreloadHQImages.getSelection());
      _state_Map2.put(STATE_IS_SHOW_THUMB_HQ_IMAGES, _chkShowHQImages.getSelection());
      _state_Map2.put(STATE_IS_SHOW_PHOTO_RATING, isShowPhotoRating);

      Map2PainterConfig.isShowPhotoRating = isShowPhotoRating;

// SET_FORMATTING_OFF

      final Enum<MapImageSize> selectedSize =

      _radioImageSize_Large   .getSelection()   ? MapImageSize.LARGE    :
      _radioImageSize_Medium  .getSelection()   ? MapImageSize.MEDIUM   :
      _radioImageSize_Small   .getSelection()   ? MapImageSize.SMALL    :
                                                  MapImageSize.TINY;

      _state_Map2.put(STATE_PHOTO_IMAGE_SIZE_LARGE,  _spinnerImageSize_Large .getSelection());
      _state_Map2.put(STATE_PHOTO_IMAGE_SIZE_MEDIUM, _spinnerImageSize_Medium.getSelection());
      _state_Map2.put(STATE_PHOTO_IMAGE_SIZE_SMALL,  _spinnerImageSize_Small .getSelection());
      _state_Map2.put(STATE_PHOTO_IMAGE_SIZE_TINY,   _spinnerImageSize_Tiny  .getSelection());

      Util.setStateEnum(_state_Map2, STATE_PHOTO_IMAGE_SIZE, selectedSize);

      config.photoFill_RGB       = _colorTourPauseLabel_Fill      .getColorValue();
      config.photoOutline_RGB    = _colorTourPauseLabel_Outline   .getColorValue();

// SET_FORMATTING_ON

      config.setupColors();
   }

   private void updateMap() {

      Photo.setMap2ImageRequestedSize(_imageSize);

      final Map2 map2 = _map2View.getMap();

      map2.updatePhotoOptions();

      map2.paint();
   }

   private void updateUI_FromConfig() {

      final Map2Config config = Map2ConfigManager.getActiveConfig();

      _colorTourPauseLabel_Fill.setColorValue(config.photoFill_RGB);
      _colorTourPauseLabel_Outline.setColorValue(config.photoOutline_RGB);
   }

   private void updateUI_Memory() {

      if (_lblHeapSize.isDisposed()) {
         return;
      }

      final Runtime runtime = Runtime.getRuntime();

      final String heapSize = Messages.Slideout_Map_PhotoOptions_Label_MemoryState.formatted(

            formatSize(runtime.totalMemory()),
            formatSize(runtime.freeMemory()),
            formatSize(runtime.maxMemory()));

      _lblHeapSize.setText(heapSize);

      // update UI when preloading is enabled
      if (_chkPreloadHQImages.getSelection()) {

         _lblHeapSize.getDisplay().timerExec(1000, () -> updateUI_Memory());
      }
   }

}
