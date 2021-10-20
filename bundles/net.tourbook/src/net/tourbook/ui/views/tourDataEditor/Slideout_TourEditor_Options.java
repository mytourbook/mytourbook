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
package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor options.
 */
public class Slideout_TourEditor_Options extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private final IDialogSettings _state = TourbookPlugin.getState(TourDataEditorView.ID);

   private TourDataEditorView    _tourEditorView;

   private ActionResetToDefaults _actionRestoreDefaults;

   private PixelConverter        _pc;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Spinner   _spinnerLatLonDigits;
   private Spinner   _spinnerDescriptionNumLines;

   private int       _hintValueFieldWidth;

   public Slideout_TourEditor_Options(final Control ownerControl,
                                      final ToolBar toolBar,
                                      final IDialogSettings state,
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

      restoreState();

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
          * Number of description lines
          */

         // label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_tour_editor_description_height);
         label.setToolTipText(Messages.pref_tour_editor_description_height_tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(label);

         // spinner
         _spinnerDescriptionNumLines = new Spinner(parent, SWT.BORDER);
         _spinnerDescriptionNumLines.setMinimum(1);
         _spinnerDescriptionNumLines.setMaximum(100);
         _spinnerDescriptionNumLines.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               UI.adjustSpinnerValueOnMouseScroll(event);
               onSelect_NumDescriptionLines();
            }
         });
         _spinnerDescriptionNumLines.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect_NumDescriptionLines();
            }
         });
         spinnerGridData.applyTo(_spinnerDescriptionNumLines);
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
         _spinnerLatLonDigits.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(final MouseEvent event) {
               UI.adjustSpinnerValueOnMouseScroll(event);
               onSelect_LatLonDigits();
            }
         });
         _spinnerLatLonDigits.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelect_LatLonDigits();
            }
         });
         spinnerGridData.applyTo(_spinnerLatLonDigits);
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _hintValueFieldWidth = _pc.convertWidthInCharsToPixels(3);
   }

   private void onSelect_LatLonDigits() {

      final int latLonDigits = _spinnerLatLonDigits.getSelection();

      _state.put(TourDataEditorView.STATE_LAT_LON_DIGITS, latLonDigits);

      _tourEditorView.updateUI_LatLonDigits(latLonDigits);
   }

   private void onSelect_NumDescriptionLines() {

      final int numLines = _spinnerDescriptionNumLines.getSelection();

      _state.put(TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES, numLines);

      _tourEditorView.updateUI_DescriptionNumLines(numLines);
   }

   @Override
   public void resetToDefaults() {

      final int descriptionNumberOfLines = TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES_DEFAULT;
      final int latLonDigits = TourDataEditorView.STATE_LAT_LON_DIGITS_DEFAULT;

      // update model
      _state.put(TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES, descriptionNumberOfLines);
      _state.put(TourDataEditorView.STATE_LAT_LON_DIGITS, latLonDigits);

      // update UI
      _spinnerDescriptionNumLines.setSelection(descriptionNumberOfLines);
      _spinnerLatLonDigits.setSelection(latLonDigits);

      _tourEditorView.updateUI_DescriptionNumLines(descriptionNumberOfLines);
      _tourEditorView.updateUI_LatLonDigits(latLonDigits);
   }

   private void restoreState() {

      _spinnerDescriptionNumLines.setSelection(Util.getStateInt(
            _state,
            TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES,
            TourDataEditorView.STATE_DESCRIPTION_NUMBER_OF_LINES_DEFAULT));

      _spinnerLatLonDigits.setSelection(Util.getStateInt(
            _state,
            TourDataEditorView.STATE_LAT_LON_DIGITS,
            TourDataEditorView.STATE_LAT_LON_DIGITS_DEFAULT));
   }

}
