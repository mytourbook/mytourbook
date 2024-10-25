/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
 * Copyright (C) 2019 Thomas Theussing
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
package net.tourbook.map25.ui;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapImageSize;
import net.tourbook.map2.view.SlideoutMap2_PhotoOptions;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25View;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * 2.5D map photo properties slideout
 */
public class SlideoutMap25_PhotoOptions extends ToolbarSlideout {

   private static final int   MIN_IMAGE_SIZE = 20;
   private static final int   MAX_IMAGE_SIZE = 2000;

   private IDialogSettings    _state;

   private MouseWheelListener _defaultMouseWheelListener;
   private SelectionListener  _defaultSelectionListener;

   private Map25View          _map25View;

   private int                _imageSize;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _chkShowHQImages;
   private Button    _chkShowPhotoTitle;

   private Button    _radioImageSize_Tiny;
   private Button    _radioImageSize_Small;
   private Button    _radioImageSize_Medium;
   private Button    _radioImageSize_Large;

   private Label     _lblHeapSize;

   private Link      _linkDiscardImages;

   private Spinner   _spinnerImageSize_Tiny;
   private Spinner   _spinnerImageSize_Small;
   private Spinner   _spinnerImageSize_Medium;
   private Spinner   _spinnerImageSize_Large;

   /**
    * @param ownerControl
    * @param toolBar
    * @param state
    * @param map25View
    */
   public SlideoutMap25_PhotoOptions(final Control ownerControl,
                                     final ToolBar toolBar,
                                     final IDialogSettings state,
                                     final Map25View map25View) {

      super(ownerControl, toolBar);

      _state = state;
      _map25View = map25View;
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

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
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_20_ImageSize(container);
            createUI_30_Options(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText("Photo Options");
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_20_ImageSize(final Composite parent) {

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
            _radioImageSize_Tiny.addSelectionListener(_defaultSelectionListener);
            gdIndent.applyTo(_radioImageSize_Tiny);

            _spinnerImageSize_Tiny = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Tiny.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Tiny.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Tiny.setIncrement(1);
            _spinnerImageSize_Tiny.setPageIncrement(10);
            _spinnerImageSize_Tiny.addSelectionListener(_defaultSelectionListener);
            _spinnerImageSize_Tiny.addMouseWheelListener(_defaultMouseWheelListener);
         }
         {
            /*
             * Image size: Small
             */
            _radioImageSize_Small = new Button(container, SWT.RADIO);
            _radioImageSize_Small.setText(OtherMessages.APP_SIZE_SMALL_LABEL);
            _radioImageSize_Small.addSelectionListener(_defaultSelectionListener);
            gdIndent.applyTo(_radioImageSize_Small);

            _spinnerImageSize_Small = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Small.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Small.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Small.setIncrement(1);
            _spinnerImageSize_Small.setPageIncrement(10);
            _spinnerImageSize_Small.addSelectionListener(_defaultSelectionListener);
            _spinnerImageSize_Small.addMouseWheelListener(_defaultMouseWheelListener);
         }
         {
            /*
             * Image size: Medium
             */
            _radioImageSize_Medium = new Button(container, SWT.RADIO);
            _radioImageSize_Medium.setText(OtherMessages.APP_SIZE_MEDIUM_LABEL);
            _radioImageSize_Medium.addSelectionListener(_defaultSelectionListener);
            gdIndent.applyTo(_radioImageSize_Medium);

            _spinnerImageSize_Medium = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Medium.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Medium.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Medium.setIncrement(1);
            _spinnerImageSize_Medium.setPageIncrement(10);
            _spinnerImageSize_Medium.addSelectionListener(_defaultSelectionListener);
            _spinnerImageSize_Medium.addMouseWheelListener(_defaultMouseWheelListener);
         }
         {
            /*
             * Image size: Large
             */
            _radioImageSize_Large = new Button(container, SWT.RADIO);
            _radioImageSize_Large.setText(OtherMessages.APP_SIZE_LARGE_LABEL);
            _radioImageSize_Large.addSelectionListener(_defaultSelectionListener);
            gdIndent.applyTo(_radioImageSize_Large);

            _spinnerImageSize_Large = new Spinner(container, SWT.BORDER);
            _spinnerImageSize_Large.setMinimum(MIN_IMAGE_SIZE);
            _spinnerImageSize_Large.setMaximum(MAX_IMAGE_SIZE);
            _spinnerImageSize_Large.setIncrement(1);
            _spinnerImageSize_Large.setPageIncrement(10);
            _spinnerImageSize_Large.addSelectionListener(_defaultSelectionListener);
            _spinnerImageSize_Large.addMouseWheelListener(_defaultMouseWheelListener);
         }
      }
   }

   private void createUI_30_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Show HQ photos
             */
            _chkShowHQImages = new Button(parent, SWT.CHECK);
            _chkShowHQImages.setText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowHqPhotoImages);
            _chkShowHQImages.setToolTipText(Messages.Slideout_Map_PhotoOptions_Checkbox_ShowHqPhotoImages_Tooltip);
            _chkShowHQImages.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Photo Title
             */
            _chkShowPhotoTitle = new Button(container, SWT.CHECK);
            _chkShowPhotoTitle.setText(Messages.Slideout_Map25PhotoOptions_Checkbox_Photo_Title);
            _chkShowPhotoTitle.addSelectionListener(_defaultSelectionListener);
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

   private void initUI(final Composite parent) {

      _parent = parent;

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> {

         onChangeUI(selectionEvent);
      });

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10, true);
         onChangeUI(mouseEvent);
      };
   }

   private void onChangeUI(final TypedEvent selectionEvent) {

      /**
       * Very strange:
       * <p>
       * The radio buttons are fireing this event twice, first the unselected then the selected
       * radio buttons
       */
      if (selectionEvent.widget instanceof final Button button) {

         if (button == _radioImageSize_Large
               || button == _radioImageSize_Medium
               || button == _radioImageSize_Small
               || button == _radioImageSize_Tiny) {

            if (button.getSelection() == false) {

               // skip the unselected event

               return;
            }
         }
      }

      _imageSize = getSelectedImageSize();

      // updade model
      saveState();

      // update UI
      updateUI();

      enableControls();
   }

   private void onDiscardImages() {

      PhotoLoadManager.stopImageLoading(true);
      PhotoLoadManager.removeInvalidImageFiles();

      PhotoImageCache.disposeAll();

      updateUI();

      System.gc();

      updateUI_Memory();
   }

   private void restoreState() {

   // SET_FORMATTING_OFF

         _chkShowHQImages     .setSelection(Util.getStateBoolean(_state, Map25View.STATE_IS_SHOW_THUMB_HQ_IMAGES, Map25View.STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT));
         _chkShowPhotoTitle   .setSelection(Util.getStateBoolean(_state, Map25View.STATE_IS_SHOW_PHOTO_TITLE,     Map25View.STATE_IS_SHOW_PHOTO_TITLE_DEFAULT));

         final Enum<MapImageSize> imageSizeCategory = Util.getStateEnum(_state, Map25View.STATE_PHOTO_IMAGE_SIZE, MapImageSize.MEDIUM);

         final int imageSizeLarge   = Util.getStateInt(_state, Map25View.STATE_PHOTO_IMAGE_SIZE_LARGE,  Map25App.MAP_IMAGE_DEFAULT_SIZE_LARGE);
         final int imageSizeMedium  = Util.getStateInt(_state, Map25View.STATE_PHOTO_IMAGE_SIZE_MEDIUM, Map25App.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);
         final int imageSizeSmall   = Util.getStateInt(_state, Map25View.STATE_PHOTO_IMAGE_SIZE_SMALL,  Map25App.MAP_IMAGE_DEFAULT_SIZE_SMALL);
         final int imageSizeTiny    = Util.getStateInt(_state, Map25View.STATE_PHOTO_IMAGE_SIZE_TINY,   Map25App.MAP_IMAGE_DEFAULT_SIZE_TINY);

         _spinnerImageSize_Large .setSelection(imageSizeLarge);
         _spinnerImageSize_Medium.setSelection(imageSizeMedium);
         _spinnerImageSize_Small .setSelection(imageSizeSmall);
         _spinnerImageSize_Tiny  .setSelection(imageSizeTiny);

   // SET_FORMATTING_ON

      if (imageSizeCategory.equals(MapImageSize.LARGE)) {

         _imageSize = imageSizeLarge;
         _radioImageSize_Large.setSelection(true);

      } else if (imageSizeCategory.equals(MapImageSize.MEDIUM)) {

         _imageSize = imageSizeMedium;
         _radioImageSize_Medium.setSelection(true);

      } else if (imageSizeCategory.equals(MapImageSize.SMALL)) {

         _imageSize = imageSizeSmall;
         _radioImageSize_Small.setSelection(true);

      } else {

         _imageSize = imageSizeTiny;
         _radioImageSize_Tiny.setSelection(true);
      }

      Photo.setMap25ImageRequestedSize(_imageSize);
   }

   private void saveState() {

   // SET_FORMATTING_OFF

      _state.put(Map25View.STATE_IS_SHOW_PHOTO_TITLE,     _chkShowPhotoTitle      .getSelection());
      _state.put(Map25View.STATE_IS_SHOW_THUMB_HQ_IMAGES, _chkShowHQImages        .getSelection());

      final Enum<MapImageSize> selectedSize =

      _radioImageSize_Large   .getSelection()   ? MapImageSize.LARGE    :
      _radioImageSize_Medium  .getSelection()   ? MapImageSize.MEDIUM   :
      _radioImageSize_Small   .getSelection()   ? MapImageSize.SMALL    :
                                                  MapImageSize.TINY;

      _state.put(Map25View.STATE_PHOTO_IMAGE_SIZE_LARGE,  _spinnerImageSize_Large .getSelection());
      _state.put(Map25View.STATE_PHOTO_IMAGE_SIZE_MEDIUM, _spinnerImageSize_Medium.getSelection());
      _state.put(Map25View.STATE_PHOTO_IMAGE_SIZE_SMALL,  _spinnerImageSize_Small .getSelection());
      _state.put(Map25View.STATE_PHOTO_IMAGE_SIZE_TINY,   _spinnerImageSize_Tiny  .getSelection());

      Util.setStateEnum(_state, Map25View.STATE_PHOTO_IMAGE_SIZE, selectedSize);

   // SET_FORMATTING_ON

      _map25View.getMapApp().getPhotoToolkit().restoreState();
   }

   private void updateUI() {

      // run async that the slideout UI is updated immediately

      Photo.setMap25ImageRequestedSize(_imageSize);

      _parent.getDisplay().asyncExec(() -> {

         final Map25App mapApp = _map25View.getMapApp();

         mapApp.updateLayer_Photos();
         mapApp.updateMap();
      });
   }

   private void updateUI_Memory() {

      if (_lblHeapSize.isDisposed()) {
         return;
      }

      final Runtime runtime = Runtime.getRuntime();

      final String heapSize = Messages.Slideout_Map_PhotoOptions_Label_MemoryState.formatted(

            SlideoutMap2_PhotoOptions.formatSize(runtime.totalMemory()),
            SlideoutMap2_PhotoOptions.formatSize(runtime.freeMemory()),
            SlideoutMap2_PhotoOptions.formatSize(runtime.maxMemory()));

      _lblHeapSize.setText(heapSize);
   }

}
