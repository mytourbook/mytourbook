/*******************************************************************************
 * Copyright (C) 2020, 2025 Wolfgang Schramm and Contributors
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
import net.tourbook.common.tooltip.AdvancedSlideout;
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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for 2D map photo options
 */
public class SlideoutMap2_PhotoOptions extends AdvancedSlideout implements

      IActionResetToDefault,
      IColorSelectorListener {

   public static final String          STATE_IS_ENLARGE_SMALL_IMAGES           = "STATE_IS_ENLARGE_SMALL_IMAGES";   //$NON-NLS-1$
   public static final boolean         STATE_IS_ENLARGE_SMALL_IMAGES_DEFAULT   = false;
   static final String                 STATE_IS_PHOTO_AUTO_SELECT              = "STATE_IS_PHOTO_AUTO_SELECT";      //$NON-NLS-1$
   static final boolean                STATE_IS_PHOTO_AUTO_SELECT_DEFAULT      = false;
   public static final String          STATE_IS_PRELOAD_HQ_IMAGES              = "STATE_IS_PRELOAD_HQ_IMAGES";      //$NON-NLS-1$
   public static final boolean         STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT      = false;
   public static final String          STATE_IS_SHOW_THUMB_HQ_IMAGES           = "STATE_IS_SHOW_THUMB_HQ_IMAGES";   //$NON-NLS-1$
   public static final boolean         STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT   = false;
   public static final String          STATE_IS_SHOW_PHOTO_ADJUSTMENTS         = "STATE_IS_SHOW_PHOTO_ADJUSTMENTS"; //$NON-NLS-1$
   public static final boolean         STATE_IS_SHOW_PHOTO_ADJUSTMENTS_DEFAULT = false;
   static final String                 STATE_IS_SHOW_PHOTO_ANNOTATIONS         = "STATE_IS_SHOW_PHOTO_ANNOTATIONS"; //$NON-NLS-1$
   static final boolean                STATE_IS_SHOW_PHOTO_ANNOTATIONS_DEFAULT = true;
   static final String                 STATE_IS_SHOW_PHOTO_HISTOGRAM           = "STATE_IS_SHOW_PHOTO_HISTOGRAM";   //$NON-NLS-1$
   static final boolean                STATE_IS_SHOW_PHOTO_HISTOGRAM_DEFAULT   = true;
   public static final String          STATE_IS_SHOW_PHOTO_LABEL               = "STATE_IS_SHOW_PHOTO_LABEL";       //$NON-NLS-1$
   public static final boolean         STATE_IS_SHOW_PHOTO_LABEL_DEFAULT       = false;
   static final String                 STATE_IS_SHOW_PHOTO_RATING              = "STATE_IS_SHOW_PHOTO_RATING";      //$NON-NLS-1$
   static final boolean                STATE_IS_SHOW_PHOTO_RATING_DEFAULT      = true;
   static final String                 STATE_IS_SHOW_PHOTO_TOOLTIP             = "STATE_IS_SHOW_PHOTO_TOOLTIP";     //$NON-NLS-1$
   static final boolean                STATE_IS_SHOW_PHOTO_TOOLTIP_DEFAULT     = true;

   public static final String          STATE_PHOTO_IMAGE_SIZE                  = "STATE_PHOTO_IMAGE_SIZE";          //$NON-NLS-1$
   public static final String          STATE_PHOTO_IMAGE_SIZE_TINY             = "STATE_PHOTO_IMAGE_SIZE_TINY";     //$NON-NLS-1$
   public static final String          STATE_PHOTO_IMAGE_SIZE_SMALL            = "STATE_PHOTO_IMAGE_SIZE_SMALL";    //$NON-NLS-1$
   public static final String          STATE_PHOTO_IMAGE_SIZE_MEDIUM           = "STATE_PHOTO_IMAGE_SIZE_MEDIUM";   //$NON-NLS-1$
   public static final String          STATE_PHOTO_IMAGE_SIZE_LARGE            = "STATE_PHOTO_IMAGE_SIZE_LARGE";    //$NON-NLS-1$

   private static final int            MIN_IMAGE_SIZE                          = 3;

   /**
    * This value is small because a map do not yet load large images !!!
    */
   private static final int            MAX_IMAGE_SIZE                          = 2000;

   private static final MapImageSize[] _allMapImageSize_Enums                  = {

         MapImageSize.TINY,
         MapImageSize.SMALL,
         MapImageSize.MEDIUM,
         MapImageSize.LARGE,
   };

   private static final String[]       _allMapImageSize_Texts                  = {

         OtherMessages.APP_SIZE_TINY_TEXT,
         OtherMessages.APP_SIZE_SMALL_TEXT,
         OtherMessages.APP_SIZE_MEDIUM_TEXT,
         OtherMessages.APP_SIZE_LARGE_TEXT
   };

   private IDialogSettings             _state_Map2View;

   private Map2View                    _map2View;
   private Map2                        _map2;

   private ActionResetToDefaults       _actionRestoreDefaults;

   private MouseWheelListener          _defaultMouseWheelListener;
   private SelectionListener           _defaultSelectedListener;
   private FocusListener               _keepOpenListener;
   private IPropertyChangeListener     _propertyChangeListener;

   private int                         _imageSize;

   /*
    * UI controls
    */
   private Composite             _shellContainer;

   private Button                _btnSwapTourPauseLabel_Color;

   private Button                _chkEnlargeSmallImages;
   private Button                _chkPreloadHQImages;
   private Button                _chkShowHQImages;
   private Button                _chkShowPhotoAdjustments;
   private Button                _chkShowPhotoAnnotations;
   private Button                _chkShowPhotoRating;

   private Combo                 _comboImageSize;

   private Spinner               _spinnerImageSize;

   private Label                 _lblHeapSize;

   private Link                  _linkDiscardImages;

   private ColorSelectorExtended _colorTourPauseLabel_Outline;
   private ColorSelectorExtended _colorTourPauseLabel_Fill;

   private ToolItem              _toolItem;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMap2_PhotoOptions(final ToolItem toolItem,
                                    final IDialogSettings map2State,
                                    final IDialogSettings slideoutState,
                                    final Map2View map2View) {

      super(toolItem.getParent(), slideoutState, new int[] { 325, 400, 325, 400 });

      _map2View = map2View;
      _map2 = map2View.getMap();
      _state_Map2View = map2State;

      _toolItem = toolItem;

      setTitleText(Messages.Slideout_Map_PhotoOptions_Label_Title);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
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
   protected void createSlideoutContent(final Composite parent) {

      createUI(parent);
      fillUI();

      restoreState();

      enableControls();

      updateUI_Memory();
   }

   @Override
   protected void createTitleBarControls(final Composite parent) {

      // this method is called 1st !!!

      initUI();
      createActions();

      {
         /*
          * Actionbar
          */
         final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.END, SWT.BEGINNING)
               .applyTo(toolbar);

         final ToolBarManager tbm = new ToolBarManager(toolbar);
         tbm.add(_actionRestoreDefaults);
         tbm.update(true);
      }
   }

   private void createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_shellContainer);
      {
         createUI_30_ImageSize(_shellContainer);
         createUI_40_Options(_shellContainer);
      }
   }

   private void createUI_30_ImageSize(final Composite parent) {

      final GridDataFactory gd = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Label
             */
            final Label label = new Label(container, SWT.NO_FOCUS);
            label.setText(Messages.Photo_Properties_Label_Size);
            label.setToolTipText(Messages.Photo_Properties_Label_ThumbnailSize_Tooltip);
            gd.applyTo(label);
         }
         {
            /*
             * Combo
             */
            _comboImageSize = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboImageSize.setVisibleItemCount(10);
            _comboImageSize.setToolTipText(Messages.Slideout_PhotoImage_Combo_TooltipSize_Tooltip);
            _comboImageSize.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelectImageSize()));
            _comboImageSize.addFocusListener(_keepOpenListener);

            gd.applyTo(_comboImageSize);
         }
         {
            /*
             * Spinner
             */
            _spinnerImageSize = new Spinner(container, SWT.BORDER);
            _spinnerImageSize.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize.setIncrement(1);
            _spinnerImageSize.setPageIncrement(10);
            _spinnerImageSize.addSelectionListener(_defaultSelectedListener);
            _spinnerImageSize.addMouseWheelListener(_defaultMouseWheelListener);
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
               _btnSwapTourPauseLabel_Color.addSelectionListener(
                     SelectionListener.widgetSelectedAdapter(selectionEvent -> onSwapPhotoColor()));
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
          * Enlarge small images
          */
         _chkEnlargeSmallImages = new Button(parent, SWT.CHECK);
         _chkEnlargeSmallImages.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_EnlargeSmallImages);
         _chkEnlargeSmallImages.setToolTipText(Messages.Slideout_Map_PhotoOptions_Checkbox_EnlargeSmallImages_Tooltip);
         _chkEnlargeSmallImages.addSelectionListener(_defaultSelectedListener);
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
         {
            /*
             * Show photos adjustments, e.g. cropping
             */
            _chkShowPhotoAdjustments = new Button(parent, SWT.CHECK);
            _chkShowPhotoAdjustments.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowPhotoAdjustments);
            _chkShowPhotoAdjustments.setToolTipText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowPhotoAdjustments_Tooltip);
            _chkShowPhotoAdjustments.addSelectionListener(_defaultSelectedListener);
            GridDataFactory.fillDefaults()
                  .indent(16, 0)
                  .applyTo(_chkShowPhotoAdjustments);

         }
         {
            /*
             * Show photo annotations
             */
            _chkShowPhotoAnnotations = new Button(parent, SWT.CHECK);
            _chkShowPhotoAnnotations.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowPhotoAnnotations);
            _chkShowPhotoAnnotations.addSelectionListener(_defaultSelectedListener);
            GridDataFactory.fillDefaults()
                  .indent(16, 0)
                  .applyTo(_chkShowPhotoAnnotations);
         }
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

// SET_FORMATTING_OFF

      final boolean isShowHQImages     = _chkShowHQImages.getSelection();
      final boolean isShowAdjustments  = _chkShowPhotoAdjustments.getSelection();

      _chkShowPhotoAdjustments   .setEnabled(isShowHQImages);
      _chkShowPhotoAnnotations   .setEnabled(isShowHQImages && isShowAdjustments);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      for (final String imageSizeText : _allMapImageSize_Texts) {
         _comboImageSize.add(imageSizeText);
      }

   }

   private int getImageSize_Index(final Enum<MapImageSize> imageSize) {

      for (int enumIndex = 0; enumIndex < _allMapImageSize_Enums.length; enumIndex++) {

         if (_allMapImageSize_Enums[enumIndex].equals(imageSize)) {

            return enumIndex;
         }
      }

      return 0;
   }

   private MapImageSize getImageSize_SelectedEnum() {

      int selectionIndex = _comboImageSize.getSelectionIndex();

      selectionIndex = selectionIndex < 0 ? 0 : selectionIndex;

      return _allMapImageSize_Enums[selectionIndex];
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI() {

      _defaultSelectedListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10, true);
         onChangeUI();
      };

      _propertyChangeListener = propertyChangeEvent -> onChangeUI();

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {

            setIsAnotherDialogOpened(false);
         }
      };
   }

   private void onChangeUI() {

      _imageSize = _spinnerImageSize.getSelection();

      saveStateAndConfig();

      updateMap();

      updateUI_Memory();

      enableControls();
   }

   private void onDiscardImages() {

      PhotoLoadManager.stopImageLoading(true);
      PhotoLoadManager.removeInvalidImageFiles();

      PhotoImageCache.disposeAll();

      _map2View.getMap().photoTooltip_OnDiscardImages();

      repaintMap();

      System.gc();

      updateUI_Memory();
   }

   @Override
   protected void onFocus() {

   }

   @Override
   protected Point onResize(final int newContentWidth, final int newContentHeight) {

      if (_shellContainer.isDisposed()) {

         // this happened during debugging

         return null;
      }

      // there is no need to resize this dialog
      final Point defaultSize = _shellContainer.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

      return defaultSize;
   }

   private void onSelectImageSize() {

      final MapImageSize imageSize_SelectedEnum = getImageSize_SelectedEnum();
      int imageSize = 222;

// SET_FORMATTING_OFF

      switch (imageSize_SelectedEnum) {
      case LARGE:  imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_LARGE,  _map2.MAP_IMAGE_DEFAULT_SIZE_LARGE);   break;
      case MEDIUM: imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_MEDIUM, _map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);  break;
      case SMALL:  imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_SMALL,  _map2.MAP_IMAGE_DEFAULT_SIZE_SMALL);   break;
      case TINY:   imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_TINY,   _map2.MAP_IMAGE_DEFAULT_SIZE_TINY);    break;
      }

// SET_FORMATTING_ON

      _spinnerImageSize.setSelection(imageSize);

      onChangeUI();
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

// SET_FORMATTING_OFF

      _chkEnlargeSmallImages  .setSelection(STATE_IS_ENLARGE_SMALL_IMAGES_DEFAULT);
      _chkPreloadHQImages     .setSelection(STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT);
      _chkShowHQImages        .setSelection(STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT);
      _chkShowPhotoAdjustments.setSelection(STATE_IS_SHOW_PHOTO_ADJUSTMENTS_DEFAULT);
      _chkShowPhotoAnnotations.setSelection(STATE_IS_SHOW_PHOTO_ANNOTATIONS_DEFAULT);
      _chkShowPhotoRating     .setSelection(STATE_IS_SHOW_PHOTO_RATING_DEFAULT);

      _spinnerImageSize       .setSelection(_map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);

      _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_LARGE,  _map2.MAP_IMAGE_DEFAULT_SIZE_LARGE);
      _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_MEDIUM, _map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);
      _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_SMALL,  _map2.MAP_IMAGE_DEFAULT_SIZE_SMALL);
      _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_TINY,   _map2.MAP_IMAGE_DEFAULT_SIZE_TINY);

      _comboImageSize.select(getImageSize_Index(MapImageSize.MEDIUM));
      _spinnerImageSize.setSelection(_map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);

// SET_FORMATTING_ON

      /*
       * Update config
       */
      final Map2Config config = Map2ConfigManager.getActiveConfig();

      config.photoFill_RGB = Map2ConfigManager.DEFAULT_PHOTO_FILL_RGB;
      config.photoOutline_RGB = Map2ConfigManager.DEFAULT_PHOTO_OUTLINE_RGB;

      config.setupColors();

      updateUI_FromConfig();

      saveStateAndConfig();

      enableControls();
      updateMap();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      final MapImageSize imageSizeEnum = (MapImageSize) Util.getStateEnum(_state_Map2View, STATE_PHOTO_IMAGE_SIZE, MapImageSize.MEDIUM);

      switch (imageSizeEnum) {
      case LARGE:    _imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_LARGE,  _map2.MAP_IMAGE_DEFAULT_SIZE_LARGE);   break;
      case SMALL:    _imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_SMALL,  _map2.MAP_IMAGE_DEFAULT_SIZE_SMALL);   break;
      case TINY:     _imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_TINY,   _map2.MAP_IMAGE_DEFAULT_SIZE_TINY);    break;

      case MEDIUM:
      default:       _imageSize = Util.getStateInt(_state_Map2View, STATE_PHOTO_IMAGE_SIZE_MEDIUM, _map2.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);  break;
      }

      _chkEnlargeSmallImages     .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_ENLARGE_SMALL_IMAGES,   STATE_IS_ENLARGE_SMALL_IMAGES_DEFAULT));
      _chkPreloadHQImages        .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_PRELOAD_HQ_IMAGES,      STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT));
      _chkShowHQImages           .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_SHOW_THUMB_HQ_IMAGES,   STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT));
      _chkShowPhotoAdjustments   .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_SHOW_PHOTO_ADJUSTMENTS, STATE_IS_SHOW_PHOTO_ADJUSTMENTS_DEFAULT));
      _chkShowPhotoAnnotations   .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_SHOW_PHOTO_ANNOTATIONS, STATE_IS_SHOW_PHOTO_ANNOTATIONS_DEFAULT));
      _chkShowPhotoRating        .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_SHOW_PHOTO_RATING,      STATE_IS_SHOW_PHOTO_RATING_DEFAULT));

      _comboImageSize            .select(getImageSize_Index(imageSizeEnum));
      _spinnerImageSize          .setSelection(_imageSize);

// SET_FORMATTING_ON

      Photo.setMap2ImageRequestedSize(_imageSize);

      updateUI_FromConfig();
   }

   private void saveStateAndConfig() {

// SET_FORMATTING_OFF

      final boolean isShowPhotoAnnotations      = _chkShowPhotoAnnotations.getSelection();
      final boolean isShowPhotoRating           = _chkShowPhotoRating.getSelection();
      final int selectedImageSize_Value         = _spinnerImageSize.getSelection();
      final MapImageSize selectedImageSize_Enum = getImageSize_SelectedEnum();

      _state_Map2View.put(STATE_IS_ENLARGE_SMALL_IMAGES,    _chkEnlargeSmallImages.getSelection());
      _state_Map2View.put(STATE_IS_PRELOAD_HQ_IMAGES,       _chkPreloadHQImages.getSelection());
      _state_Map2View.put(STATE_IS_SHOW_PHOTO_ADJUSTMENTS,  _chkShowPhotoAdjustments.getSelection());
      _state_Map2View.put(STATE_IS_SHOW_PHOTO_ANNOTATIONS,  isShowPhotoAnnotations);
      _state_Map2View.put(STATE_IS_SHOW_PHOTO_RATING,       isShowPhotoRating);
      _state_Map2View.put(STATE_IS_SHOW_THUMB_HQ_IMAGES,    _chkShowHQImages.getSelection());

      switch (selectedImageSize_Enum) {
      case LARGE:    _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_LARGE,  selectedImageSize_Value);  break;
      case MEDIUM:   _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_MEDIUM, selectedImageSize_Value);  break;
      case SMALL:    _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_SMALL,  selectedImageSize_Value);  break;
      case TINY:     _state_Map2View.put(STATE_PHOTO_IMAGE_SIZE_TINY,   selectedImageSize_Value);  break;
      }

      Util.setStateEnum(_state_Map2View, STATE_PHOTO_IMAGE_SIZE, selectedImageSize_Enum);

      Map2PainterConfig.isShowPhotoAnnotations  = isShowPhotoAnnotations;
      Map2PainterConfig.isShowPhotoRating       = isShowPhotoRating;

      final Map2Config map2Config = Map2ConfigManager.getActiveConfig();

      map2Config.photoFill_RGB       = _colorTourPauseLabel_Fill      .getColorValue();
      map2Config.photoOutline_RGB    = _colorTourPauseLabel_Outline   .getColorValue();
      map2Config.setupColors();

// SET_FORMATTING_ON

      super.saveState();
   }

   private void updateMap() {

      Photo.setMap2ImageRequestedSize(_imageSize);

      final Map2 map2 = _map2View.getMap();

      map2.updatePhotoOptions();
      map2.updateTooltips();

      map2.paint();
   }

   private void updateUI_FromConfig() {

      final Map2Config config = Map2ConfigManager.getActiveConfig();

      _colorTourPauseLabel_Fill.setColorValue(config.photoFill_RGB);
      _colorTourPauseLabel_Outline.setColorValue(config.photoOutline_RGB);
   }

   public void updateUI_FromState() {

      if (_chkShowPhotoAnnotations == null || _chkShowPhotoAnnotations.isDisposed()) {
         return;
      }

// SET_FORMATTING_OFF

      _chkShowPhotoAnnotations.setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_SHOW_PHOTO_ANNOTATIONS, STATE_IS_SHOW_PHOTO_ANNOTATIONS_DEFAULT));
      _chkShowPhotoRating     .setSelection(Util.getStateBoolean(_state_Map2View, STATE_IS_SHOW_PHOTO_RATING,      STATE_IS_SHOW_PHOTO_RATING_DEFAULT));

// SET_FORMATTING_ON

      enableControls();
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
