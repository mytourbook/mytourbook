/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.ui.IChangeUIListener;
import net.tourbook.tour.filter.TourFilterFieldOperator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class TourPauseUI implements IColorSelectorListener {

   private static final String STATE_IS_FILTER_TOUR_PAUSES        = "STATE_IS_FILTER_TOUR_PAUSES";        //$NON-NLS-1$
   private static final String STATE_IS_PAUSE_FILTER_DURATION     = "STATE_IS_PAUSE_FILTER_DURATION";     //$NON-NLS-1$
   private static final String STATE_IS_SHOW_PAUSE_AUTO_PAUSES    = "STATE_IS_SHOW_PAUSE_AUTO_PAUSES";    //$NON-NLS-1$
   private static final String STATE_IS_SHOW_PAUSE_USER_INITIATED = "STATE_IS_SHOW_PAUSE_USER_INITIATED"; //$NON-NLS-1$
   //

   /**
    * Filter operator MUST be in sync with filter labels
    */
   private static TourFilterFieldOperator[] _allFilter_Value = {

         TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
         TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
         TourFilterFieldOperator.EQUALS,

   };

   /**
    * Filter labels MUST be in sync with filter operator
    */
   private static String[]                  _allFilter_Label = {

         Messages.Tour_Filter_Operator_GreaterThanOrEqual,
         Messages.Tour_Filter_Operator_LessThanOrEqual,
         Messages.Tour_Filter_Operator_Equals,

   };

   private IDialogSettings                  _state;

   private SelectionListener                _defaultSelectionListener;
   private MouseWheelListener               _defaultMouseWheelListener;
   private FocusListener                    _keepOpenListener;

   private PixelConverter                   _pc;

   private int                              _firstColumnIndent;
   private GridDataFactory                  _firstColoumLayoutData;
   private GridDataFactory                  _secondColoumLayoutData;

   private boolean                          _isShowTourPauses;

   /*
    * UI controls
    */
   private Button               _chkIsFilterTourPauses;
   private Button               _chkIsShowPauses_AutoPause;
   private Button               _chkIsPauseFilter_Duration;
   private Button               _chkIsShowPauses_UserInitiated;

   private Combo                _comboPauseFilter_Duration;
   private AnimatedToolTipShell _tooltipShell;
   private IChangeUIListener    _changeUIListener;

   /**
    * @param state
    * @param tooltipShell
    * @param changeUIListener
    */
   public TourPauseUI(final IDialogSettings state,
                      final AnimatedToolTipShell tooltipShell,
                      final IChangeUIListener changeUIListener) {

      _state = state;
      _tooltipShell = tooltipShell;
      _changeUIListener = changeUIListener;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      _tooltipShell.setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

   }

   public Composite createContent(final Composite parent, final boolean isShowTourPauses) {

      _isShowTourPauses = isShowTourPauses;

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      setupUI();

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

         createUI_30_TourPauseFilter(container);
      }

      return container;
   }

   private void createUI_30_TourPauseFilter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Fiter tour pauses
             */
            _chkIsFilterTourPauses = new Button(container, SWT.CHECK);
            _chkIsFilterTourPauses.setText(Messages.Slideout_Map_Options_Checkbox_TourPauseFilter);
            _chkIsFilterTourPauses.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsFilterTourPauses);
         }
         {
            /*
             * Pauses Filter: Auto pause
             */
            _chkIsShowPauses_AutoPause = new Button(container, SWT.CHECK);
            _chkIsShowPauses_AutoPause.setText(Messages.Slideout_Map_Options_Checkbox_PauseFilter_AutoPause);
            _chkIsShowPauses_AutoPause.addSelectionListener(_defaultSelectionListener);
            _firstColoumLayoutData.span(2, 1).applyTo(_chkIsShowPauses_AutoPause);
         }
         {
            /*
             * Pauses Filter: User started/stopped
             */
            _chkIsShowPauses_UserInitiated = new Button(container, SWT.CHECK);
            _chkIsShowPauses_UserInitiated.setText(Messages.Slideout_Map_Options_Checkbox_PauseFilter_User);
            _chkIsShowPauses_UserInitiated.addSelectionListener(_defaultSelectionListener);
            _firstColoumLayoutData.span(2, 1).applyTo(_chkIsShowPauses_UserInitiated);
         }
         {
            /*
             * Pauses Filter: Duration
             */
            _chkIsPauseFilter_Duration = new Button(container, SWT.CHECK);
            _chkIsPauseFilter_Duration.setText(Messages.Slideout_Map_Options_Checkbox_PauseFilter_Duration);
            _chkIsPauseFilter_Duration.addSelectionListener(_defaultSelectionListener);
            _firstColoumLayoutData.applyTo(_chkIsPauseFilter_Duration);

            final Composite containerDuration = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerDuration);
            GridLayoutFactory.fillDefaults().numColumns(5).applyTo(containerDuration);
            {
               // combo
               _comboPauseFilter_Duration = new Combo(containerDuration, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboPauseFilter_Duration.setVisibleItemCount(5);
               _comboPauseFilter_Duration.addSelectionListener(_defaultSelectionListener);
               _comboPauseFilter_Duration.addFocusListener(_keepOpenListener);
               _secondColoumLayoutData.applyTo(_comboPauseFilter_Duration);
            }
         }
      }
   }

   public void enableControls() {

      final boolean isFilterTourPauses = _chkIsFilterTourPauses.getSelection();
      final boolean isDurationFilter = _chkIsPauseFilter_Duration.getSelection();
      final boolean isPausesFilter = _isShowTourPauses && isFilterTourPauses;

      _chkIsFilterTourPauses.setEnabled(_isShowTourPauses);
      _chkIsShowPauses_AutoPause.setEnabled(isPausesFilter);
      _chkIsShowPauses_UserInitiated.setEnabled(isPausesFilter);
      _chkIsPauseFilter_Duration.setEnabled(isPausesFilter);
      _comboPauseFilter_Duration.setEnabled(isPausesFilter && isDurationFilter);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            _tooltipShell.setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            _tooltipShell.setIsAnotherDialogOpened(false);
         }
      };

      _firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

      _firstColoumLayoutData = GridDataFactory.fillDefaults()
            .indent(_firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);

      _secondColoumLayoutData = GridDataFactory.fillDefaults()
            .indent(2 * _firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      _changeUIListener.onChangeUI_External();
   }

   public void resetToDefaults() {

   }

   public void restoreState() {

   }

   public void saveState() {

   // SET_FORMATTING_OFF

         /*
          * Tour filter
          */
         _state.put(STATE_IS_FILTER_TOUR_PAUSES,         _chkIsFilterTourPauses.getSelection());
         _state.put(STATE_IS_PAUSE_FILTER_DURATION,      _chkIsPauseFilter_Duration.getSelection());
         _state.put(STATE_IS_SHOW_PAUSE_AUTO_PAUSES,     _chkIsShowPauses_AutoPause.getSelection());
         _state.put(STATE_IS_SHOW_PAUSE_USER_INITIATED,  _chkIsShowPauses_UserInitiated.getSelection());

   // SET_FORMATTING_ON
   }

   private void setupUI() {

      for (final String label : _allFilter_Label) {
         _comboPauseFilter_Duration.add(label);
      }
   }
}
