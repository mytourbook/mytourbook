/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor options.
 */
public class SlideoutQuickEditor_Options extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final IDialogSettings _state = TourbookPlugin.getState(TourDataEditorView.ID);

   private DialogQuickEdit              _dialogQuickEdit;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Spinner   _spinnerWeatherDescriptionNumLines;

   public SlideoutQuickEditor_Options(final Control ownerControl,
                                      final ToolBar toolBar,
                                      final DialogQuickEdit dialogQuickEdit) {

      super(ownerControl, toolBar);

      _dialogQuickEdit = dialogQuickEdit;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

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
            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourEditor_Label_WeatherDescription_Height);
            label.setToolTipText(Messages.Slideout_TourEditor_Label_WeatherDescription_Height_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);

            // spinner
            _spinnerWeatherDescriptionNumLines = new Spinner(container, SWT.BORDER);
            _spinnerWeatherDescriptionNumLines.setMinimum(1);
            _spinnerWeatherDescriptionNumLines.setMaximum(100);
            _spinnerWeatherDescriptionNumLines.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_NumLines()));
            _spinnerWeatherDescriptionNumLines.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
               onSelect_NumLines();
            });
         }
      }

      return _shellContainer;
   }


   @Override
   protected boolean isAlignLeft() {

      return true;
   }

   private void onSelect_NumLines() {

      final int numLines_WeatherDescription = _spinnerWeatherDescriptionNumLines.getSelection();

      _state.put(TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES, numLines_WeatherDescription);

      _dialogQuickEdit.updateUI_DescriptionNumLines(numLines_WeatherDescription);
   }

   @Override
   public void resetToDefaults() {

      /*
       * Get default values
       */
      final int weatherDescriptionNumberOfLines = TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES_DEFAULT;

      /*
       * Update UI
       */
      _spinnerWeatherDescriptionNumLines.setSelection(weatherDescriptionNumberOfLines);

   }

   private void restoreState() {

      _spinnerWeatherDescriptionNumLines.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES,
            TourDataEditorView.STATE_WEATHERDESCRIPTION_NUMBER_OF_LINES_DEFAULT));
   }

}
