/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourDataEditor;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.tag.TagContentLayout;
import net.tourbook.tag.TagManager;
import net.tourbook.tag.TagManager.TagContentLayoutItem;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor options.
 */
public class SlideoutTourEditor_Options extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final IDialogSettings _state              = TourbookPlugin.getState(TourDataEditorView.ID);

   private static final String[]        ALL_ELEVATION_OPTIONS = {

         Messages.Slideout_TourEditor_Combo_ElevationFromDevice,
         Messages.Slideout_TourEditor_Combo_ElevationFromSRTM,
   };

   private TourDataEditorView           _tourEditorView;

   private ActionResetToDefaults        _actionRestoreDefaults;

   private SelectionListener            _defaultSelectionListener;
   private FocusListener                _keepOpenListener;

   private PixelConverter               _pc;
   private int                          _hintValueFieldWidth;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Button    _chkDelete_KeepDistance;
   private Button    _chkDelete_KeepTime;
   private Button    _chkRecomputeElevation;
   
   private Combo     _comboElevationOptions;
   private Combo     _comboTagContent;
   
   private Label     _lblNumTagContentColumns;
   private Label     _lblTagImageSize;
   private Label     _lblTagContentWidth;

   private Spinner   _spinnerLatLonDigits;
   private Spinner   _spinnerTag_NumContentColumns;
   private Spinner   _spinnerTag_ImageSize;
   private Spinner   _spinnerTag_TextWidth;
   private Spinner   _spinnerTourDescriptionNumLines;
   private Spinner   _spinnerWeatherDescriptionNumLines;

   public SlideoutTourEditor_Options(final Control ownerControl,
                                     final ToolBar toolBar,
                                     final TourDataEditorView tourEditorView) {

      super(ownerControl, toolBar);

      _tourEditorView = tourEditorView;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

      /*
       * Action: Restore default
       */
      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      fillUI();
      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);

            createUI_20_Options(container);
            createUI_30_Tags(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_TourEditor_Label_Title);
      GridDataFactory.fillDefaults().applyTo(label);
      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);

      tbm.update(true);
   }

   private void createUI_20_Options(final Composite parent) {

      final GridDataFactory spinnerGridData = GridDataFactory.fillDefaults()
            .hint(_hintValueFieldWidth, SWT.DEFAULT)
            .align(SWT.END, SWT.CENTER);
      {
         /*
          * Number of lines for the tour's description
          */

         // label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_tour_editor_description_height);
         label.setToolTipText(Messages.pref_tour_editor_description_height_tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         // spinner
         _spinnerTourDescriptionNumLines = new Spinner(parent, SWT.BORDER);
         _spinnerTourDescriptionNumLines.setMinimum(1);
         _spinnerTourDescriptionNumLines.setMaximum(100);
         _spinnerTourDescriptionNumLines.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_NumDescriptionLines()));
         _spinnerTourDescriptionNumLines.addMouseWheelListener(mouseEvent -> {
            UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
            onSelect_NumDescriptionLines();
         });
         spinnerGridData.applyTo(_spinnerTourDescriptionNumLines);
      }
      {
         /*
          * Number of lines for the weather's description
          */

         // label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Slideout_TourEditor_Label_WeatherDescription_Height);
         label.setToolTipText(Messages.Slideout_TourEditor_Label_WeatherDescription_Height_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         // spinner
         _spinnerWeatherDescriptionNumLines = new Spinner(parent, SWT.BORDER);
         _spinnerWeatherDescriptionNumLines.setMinimum(1);
         _spinnerWeatherDescriptionNumLines.setMaximum(100);
         _spinnerWeatherDescriptionNumLines.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_NumDescriptionLines()));
         _spinnerWeatherDescriptionNumLines.addMouseWheelListener(mouseEvent -> {
            UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
            onSelect_NumDescriptionLines();
         });
         spinnerGridData.applyTo(_spinnerWeatherDescriptionNumLines);
      }
      {
         /*
          * Lat/lon digits
          */

         // label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Slideout_TourEditor_Label_LatLonDigits);
         label.setToolTipText(Messages.Slideout_TourEditor_Label_LatLonDigits_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         // spinner
         _spinnerLatLonDigits = new Spinner(parent, SWT.BORDER);
         _spinnerLatLonDigits.setMinimum(0);
         _spinnerLatLonDigits.setMaximum(20);
         _spinnerLatLonDigits.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_LatLonDigits()));
         _spinnerLatLonDigits.addMouseWheelListener(mouseEvent -> {
            UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
            onSelect_LatLonDigits();
         });
         spinnerGridData.applyTo(_spinnerLatLonDigits);
      }
      {
         /*
          * DEL key actions
          */
         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourEditor_Label_DeleteTimeSlices);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

            // checkbox: Keep time
            _chkDelete_KeepTime = new Button(container, SWT.CHECK);
            _chkDelete_KeepTime.setText(Messages.Slideout_TourEditor_Checkbox_KeepTime);
            _chkDelete_KeepTime.setToolTipText(Messages.Slideout_TourEditor_Checkbox_KeepTime_Tooltip);
            _chkDelete_KeepTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().indent(16, 0).applyTo(_chkDelete_KeepTime);

            // radio: solid
            _chkDelete_KeepDistance = new Button(container, SWT.CHECK);
            _chkDelete_KeepDistance.setText(Messages.Slideout_TourEditor_Checkbox_KeepDistance);
            _chkDelete_KeepDistance.setToolTipText(Messages.Slideout_TourEditor_Checkbox_KeepDistance_Tooltip);
            _chkDelete_KeepDistance.addSelectionListener(_defaultSelectionListener);
         }
      }
      {
         /*
          * Recompute elevation up/down when saved
          */
         _chkRecomputeElevation = new Button(parent, SWT.CHECK);
         _chkRecomputeElevation.setText(Messages.Slideout_TourEditor_Checkbox_RecomputeElevationUpDown);
         _chkRecomputeElevation.setToolTipText(Messages.Slideout_TourEditor_Checkbox_RecomputeElevationUpDown_Tooltip);
         _chkRecomputeElevation.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkRecomputeElevation);

         // combo
         _comboElevationOptions = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
         _comboElevationOptions.setVisibleItemCount(20);
         _comboElevationOptions.addSelectionListener(_defaultSelectionListener);
         _comboElevationOptions.addFocusListener(_keepOpenListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .grab(true, false)
               .indent(16, 0)
               .applyTo(_comboElevationOptions);
      }
   }

   private void createUI_30_Tags(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_TourEditor_Group_Tags);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
//      group.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Tag content layout
             */

            // label
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Slideout_TourEditor_Label_TagContent);

            // combo
            _comboTagContent = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboTagContent.setVisibleItemCount(20);
            _comboTagContent.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_TagContent()));
            _comboTagContent.addFocusListener(_keepOpenListener);
            GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_comboTagContent);
         }
         {
            /*
             * Tag image size
             */

            // label
            _lblTagImageSize = new Label(group, SWT.NONE);
            _lblTagImageSize.setText(Messages.Slideout_TourEditor_Label_TagImageSize);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblTagImageSize);

            // spinner
            _spinnerTag_ImageSize = new Spinner(group, SWT.BORDER);
            _spinnerTag_ImageSize.setMinimum(TourDataEditorView.STATE_TAG_IMAGE_SIZE_MIN);
            _spinnerTag_ImageSize.setMaximum(TourDataEditorView.STATE_TAG_IMAGE_SIZE_MAX);
            _spinnerTag_ImageSize.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_TagContent()));
            _spinnerTag_ImageSize.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
               onSelect_TagContent();
            });
            GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).applyTo(_spinnerTag_ImageSize);
         }
         {
            /*
             * Number of tag content columns
             */

            // label
            _lblNumTagContentColumns = new Label(group, SWT.NONE);
            _lblNumTagContentColumns.setText(Messages.Slideout_TourEditor_Label_NumberOfTagColumns);
            _lblNumTagContentColumns.setToolTipText(Messages.Slideout_TourEditor_Label_NumberOfTagColumns_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.TRAIL, SWT.CENTER)
                  .grab(true, false)
                  .indent(16, 0) // show more space between "columns"
                  .applyTo(_lblNumTagContentColumns);

            // spinner
            _spinnerTag_NumContentColumns = new Spinner(group, SWT.BORDER);
            _spinnerTag_NumContentColumns.setToolTipText(Messages.Slideout_TourEditor_Label_NumberOfTagColumns_Tooltip);
            _spinnerTag_NumContentColumns.setMinimum(TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_MIN);
            _spinnerTag_NumContentColumns.setMaximum(TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_MAX);
            _spinnerTag_NumContentColumns.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_TagContent()));
            _spinnerTag_NumContentColumns.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
               onSelect_TagContent();
            });
         }
         {
            /*
             * Tag content width
             */

            // label
            _lblTagContentWidth = new Label(group, SWT.NONE);
            _lblTagContentWidth.setText(Messages.Slideout_TourEditor_Label_TagTextWidth);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblTagContentWidth);

            // spinner
            _spinnerTag_TextWidth = new Spinner(group, SWT.BORDER);
            _spinnerTag_TextWidth.setMinimum(TourDataEditorView.STATE_TAG_TEXT_WIDTH_MIN);
            _spinnerTag_TextWidth.setMaximum(TourDataEditorView.STATE_TAG_TEXT_WIDTH_MAX);
            _spinnerTag_TextWidth.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_TagContent()));
            _spinnerTag_TextWidth.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
               onSelect_TagContent();
            });
            GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).applyTo(_spinnerTag_TextWidth);
         }
      }
   }

   private void enableControls() {

      final TagContentLayout selectedTagContentLayout = getSelectedTagContentLayout();

// SET_FORMATTING_OFF

      final boolean isTagContentWithImage = TagContentLayout.IMAGE_AND_DATA.equals(selectedTagContentLayout);
      final boolean isRecomputeElevation  = _chkRecomputeElevation.getSelection();

      _lblNumTagContentColumns      .setEnabled(isTagContentWithImage);
      _lblTagContentWidth           .setEnabled(isTagContentWithImage);
      _lblTagImageSize              .setEnabled(isTagContentWithImage);

      _comboElevationOptions         .setEnabled(isRecomputeElevation);

      _spinnerTag_NumContentColumns .setEnabled(isTagContentWithImage);
      _spinnerTag_TextWidth         .setEnabled(isTagContentWithImage);
      _spinnerTag_ImageSize         .setEnabled(isTagContentWithImage);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      for (final String elevationOption : ALL_ELEVATION_OPTIONS) {
         _comboElevationOptions.add(elevationOption);
      }

      for (final TagContentLayoutItem layoutItem : TagManager.ALL_TAG_CONTENT_LAYOUT) {
         _comboTagContent.add(layoutItem.label);
      }
   }

   private int getElevationFromDeviceIndex(final boolean isElevationFromDevice) {

      return isElevationFromDevice ? 0 : 1;
   }

   private boolean getIsElevationFromDevice() {

      // get valid index
      final int selectionIndex = Math.max(0, _comboElevationOptions.getSelectionIndex());

      return selectionIndex == 0;
   }

   private TagContentLayout getSelectedTagContentLayout() {

      // get valid index
      final int selectionIndex = Math.max(0, _comboTagContent.getSelectionIndex());

      return TagManager.ALL_TAG_CONTENT_LAYOUT[selectionIndex].tagContentLayout;
   }

   private int getTagContentLayoutIndex(final TagContentLayout legendUnitLayout) {

      final TagContentLayoutItem[] allTagContentLayout = TagManager.ALL_TAG_CONTENT_LAYOUT;

      for (int layoutIndex = 0; layoutIndex < allTagContentLayout.length; layoutIndex++) {

         final TagContentLayoutItem layoutItem = allTagContentLayout[layoutIndex];

         if (legendUnitLayout == layoutItem.tagContentLayout) {
            return layoutIndex;
         }
      }

      return 0;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _hintValueFieldWidth = _pc.convertWidthInCharsToPixels(3);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

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

      enableControls();

      saveState();
   }

   private void onSelect_LatLonDigits() {

      final int latLonDigits = _spinnerLatLonDigits.getSelection();

      _state.put(TourDataEditorView.STATE_LAT_LON_DIGITS, latLonDigits);

      _tourEditorView.updateUI_LatLonDigits(latLonDigits);
   }

   private void onSelect_NumDescriptionLines() {

      final int tourDescriptionNumberOfLines = _spinnerTourDescriptionNumLines.getSelection();
      final int weatherDescriptionNumberOfLines = _spinnerWeatherDescriptionNumLines.getSelection();

      _state.put(TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES, tourDescriptionNumberOfLines);
      _state.put(TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES, weatherDescriptionNumberOfLines);

      _tourEditorView.updateUI_DescriptionNumLines(tourDescriptionNumberOfLines, weatherDescriptionNumberOfLines);
   }

   private void onSelect_TagContent() {

      _state.put(TourDataEditorView.STATE_TAG_TEXT_WIDTH, _spinnerTag_TextWidth.getSelection());
      _state.put(TourDataEditorView.STATE_TAG_IMAGE_SIZE, _spinnerTag_ImageSize.getSelection());
      _state.put(TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS, _spinnerTag_NumContentColumns.getSelection());

      Util.setStateEnum(_state, TourDataEditorView.STATE_TAG_CONTENT_LAYOUT, getSelectedTagContentLayout());

      enableControls();

      // run async because it can take time to reload the tag images
      _shellContainer.getDisplay().asyncExec(() -> TagManager.updateTagContent());
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      /*
       * Get default values
       */
      final int descriptionNumberOfLines        = TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES_DEFAULT;
      final int latLonDigits                    = TourDataEditorView.STATE_LAT_LON_DIGITS_DEFAULT;
      final int weatherDescriptionNumberOfLines = TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES_DEFAULT;

      final boolean isDeleteKeepDistance        = TourDataEditorView.STATE_IS_DELETE_KEEP_DISTANCE_DEFAULT;
      final boolean isDeleteKeepTime            = TourDataEditorView.STATE_IS_DELETE_KEEP_TIME_DEFAULT;
      final boolean isElevationFromDevice       = TourDataEditorView.STATE_IS_ELEVATION_FROM_DEVICE_DEFAULT;
      final boolean isRecomputeElevation        = TourDataEditorView.STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN_DEFAULT;

      final int tagContentWidth                 = TourDataEditorView.STATE_TAG_TEXT_WIDTH_DEFAULT;
      final int tagImageSize                    = TourDataEditorView.STATE_TAG_IMAGE_SIZE_DEFAULT;
      final int tagNumContentColumns            = TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_DEFAULT;
      final TagContentLayout tagContentLayout   = TourDataEditorView.STATE_TAG_CONTENT_LAYOUT_DEFAULT;

      /*
       * Update model
       */
      _state.put(TourDataEditorView.STATE_IS_DELETE_KEEP_DISTANCE,            isDeleteKeepDistance);
      _state.put(TourDataEditorView.STATE_IS_DELETE_KEEP_TIME,                isDeleteKeepTime);
      _state.put(TourDataEditorView.STATE_IS_ELEVATION_FROM_DEVICE,           isElevationFromDevice);
      _state.put(TourDataEditorView.STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN,     isRecomputeElevation);
      _state.put(TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES,        descriptionNumberOfLines);
      _state.put(TourDataEditorView.STATE_LAT_LON_DIGITS,                     latLonDigits);

      // tags
      _state.put(TourDataEditorView.STATE_TAG_TEXT_WIDTH,                     tagContentWidth);
      _state.put(TourDataEditorView.STATE_TAG_IMAGE_SIZE,                     tagImageSize);
      _state.put(TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS,            tagNumContentColumns);
      Util.setStateEnum(_state, TourDataEditorView.STATE_TAG_CONTENT_LAYOUT,  tagContentLayout);

      /*
       * Update UI
       */
      _chkDelete_KeepDistance             .setSelection(isDeleteKeepDistance);
      _chkDelete_KeepTime                 .setSelection(isDeleteKeepTime);
      _chkRecomputeElevation              .setSelection(isRecomputeElevation);
      _comboElevationOptions              .select(getElevationFromDeviceIndex(isElevationFromDevice));
      _spinnerLatLonDigits                .setSelection(latLonDigits);
      _spinnerTourDescriptionNumLines     .setSelection(descriptionNumberOfLines);
      _spinnerWeatherDescriptionNumLines  .setSelection(weatherDescriptionNumberOfLines);

      // tags
      _comboTagContent                    .select(getTagContentLayoutIndex(tagContentLayout));
      _spinnerTag_TextWidth               .setSelection(tagContentWidth);
      _spinnerTag_ImageSize               .setSelection(tagImageSize);
      _spinnerTag_NumContentColumns       .setSelection(tagNumContentColumns);

// SET_FORMATTING_ON

      _tourEditorView.updateUI_DescriptionNumLines(descriptionNumberOfLines, weatherDescriptionNumberOfLines);
      _tourEditorView.updateUI_LatLonDigits(latLonDigits);

      TagManager.updateTagContent();

      enableControls();
   }

   private void restoreState() {

      final boolean isElevationFromDevice = Util.getStateBoolean(_state,
            TourDataEditorView.STATE_IS_ELEVATION_FROM_DEVICE,
            TourDataEditorView.STATE_IS_ELEVATION_FROM_DEVICE_DEFAULT);

      _comboElevationOptions.select(getElevationFromDeviceIndex(isElevationFromDevice));

      _chkDelete_KeepDistance.setSelection(Util.getStateBoolean(_state,
            TourDataEditorView.STATE_IS_DELETE_KEEP_DISTANCE,
            TourDataEditorView.STATE_IS_DELETE_KEEP_DISTANCE_DEFAULT));

      _chkDelete_KeepTime.setSelection(Util.getStateBoolean(_state,
            TourDataEditorView.STATE_IS_DELETE_KEEP_TIME,
            TourDataEditorView.STATE_IS_DELETE_KEEP_TIME_DEFAULT));

      _chkRecomputeElevation.setSelection(Util.getStateBoolean(_state,
            TourDataEditorView.STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN,
            TourDataEditorView.STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN_DEFAULT));

      _spinnerLatLonDigits.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_LAT_LON_DIGITS,
            TourDataEditorView.STATE_LAT_LON_DIGITS_DEFAULT));

      _spinnerTourDescriptionNumLines.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES,
            TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES_DEFAULT));

      _spinnerWeatherDescriptionNumLines.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES,
            TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES_DEFAULT));

      /*
       * Tags
       */
      _spinnerTag_TextWidth.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH_DEFAULT,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH_MIN,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH_MAX));

      _spinnerTag_ImageSize.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE_DEFAULT,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE_MIN,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE_MAX));

      _spinnerTag_NumContentColumns.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_DEFAULT,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_MIN,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_MAX));

      _comboTagContent.select(getTagContentLayoutIndex((TagContentLayout) Util.getStateEnum(_state,
            TourDataEditorView.STATE_TAG_CONTENT_LAYOUT,
            TourDataEditorView.STATE_TAG_CONTENT_LAYOUT_DEFAULT)));
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(TourDataEditorView.STATE_IS_DELETE_KEEP_DISTANCE,         _chkDelete_KeepDistance.getSelection());
      _state.put(TourDataEditorView.STATE_IS_DELETE_KEEP_TIME,             _chkDelete_KeepTime.getSelection());
      _state.put(TourDataEditorView.STATE_IS_RECOMPUTE_ELEVATION_UP_DOWN,  _chkRecomputeElevation.getSelection());

      _state.put(TourDataEditorView.STATE_IS_ELEVATION_FROM_DEVICE,        getIsElevationFromDevice());

// SET_FORMATTING_ON

   }

}
