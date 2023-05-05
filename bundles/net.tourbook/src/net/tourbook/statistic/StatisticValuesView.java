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
package net.tourbook.statistic;

import static org.eclipse.swt.events.FocusListener.focusGainedAdapter;
import static org.eclipse.swt.events.FocusListener.focusLostAdapter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearance;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class StatisticValuesView extends ViewPart {

   public static final String                 ID                             = "net.tourbook.statistic.StatisticValuesView"; //$NON-NLS-1$

   private static final String                STATE_IS_GROUP_VALUES          = "STATE_IS_GROUP_VALUES";                      //$NON-NLS-1$
   private static final String                STATE_IS_SHOW_CSV_FORMAT       = "STATE_IS_SHOW_CSV_FORMAT";                   //$NON-NLS-1$
   private static final String                STATE_IS_SHOW_ZERO_VALUES      = "STATE_IS_SHOW_ZERO_VALUES";                  //$NON-NLS-1$
   private static final String                STATE_IS_SHOW_SEQUENCE_NUMBERS = "STATE_IS_SHOW_SEQUENCE_NUMBERS";             //$NON-NLS-1$

   private static final IPreferenceStore      _prefStore                     = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore      _prefStore_Common              = CommonActivator.getPrefStore();
   private static final IDialogSettings       _state                         = TourbookPlugin.getState(ID);

   private IPartListener2                     _partListener;
   private IPropertyChangeListener            _prefChangeListener;
   private IPropertyChangeListener            _prefChangeListener_Common;

   private ITourEventListener                 _tourEventListener;

   private Action_CopyStatValuesIntoClipboard _action_CopyIntoClipboard;
   private Action_GroupValues                 _action_GroupValues;
   private ActionOpenPrefDialog               _action_PrefDialog;
   private Action_ShowCSVFormat               _action_ShowCSVFormat;
   private Action_ShowSequenceNumbers         _action_ShowSequenceNumbers;
   private Action_ShowZeroValues              _action_ShowZeroValues;

   private boolean                            _isShowRawData;

   /*
    * UI controls
    */
   private PageBook   _pageBook;

   private Composite  _pageNoData;
   private Composite  _pageContent;

   /**
    * Using {@link Text} or {@link StyledText}
    * <p>
    * {@link Text}
    * <p>
    * + Could hide scrollbars when not necessary<br>
    * - Cannot keep horizontal position when content is replaced
    * <p>
    * {@link StyledText}<br>
    * + Can keep horizontal position when content is replaced<br>
    * - Cannot hide scrollbars when not necessary
    * <p>
    * -> Using {@link StyledText}, horizontal position is more important.
    */
   private StyledText _txtStatValues;

   private class Action_CopyStatValuesIntoClipboard extends Action {

      Action_CopyStatValuesIntoClipboard() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Tour_StatisticValues_Action_CopyIntoClipboard_Tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy_Disabled));
      }

      @Override
      public void run() {
         onAction_CopyIntoClipboard();
      }
   }

   private class Action_GroupValues extends Action {

      Action_GroupValues() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_StatisticValues_Action_GroupValues_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.GroupValues));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.GroupValues_Disabled));
      }

      @Override
      public void run() {
         enableActions();
         updateUI();
      }
   }

   private class Action_ShowCSVFormat extends Action {

      Action_ShowCSVFormat() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_StatisticValues_Action_CSVFormat_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.CSVFormat));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.CSVFormat_Disabled));
      }

      @Override
      public void run() {}

      @Override
      public void runWithEvent(final Event event) {

         _isShowRawData = UI.isCtrlKey(event);

         enableActions();
         updateUI();
      }
   }

   private class Action_ShowSequenceNumbers extends Action {

      Action_ShowSequenceNumbers() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_StatisticValues_Action_ShowSequenceNumbers_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_SequenceNumber));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_SequenceNumber_Disabled));
      }

      @Override
      public void run() {
         enableActions();
         updateUI();
      }
   }

   private class Action_ShowZeroValues extends Action {

      Action_ShowZeroValues() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_StatisticValues_Action_ShowZeroValued_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ZeroValues));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.ZeroValues_Disabled));
      }

      @Override
      public void run() {
         enableActions();
         updateUI();
      }
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == StatisticValuesView.this) {
               fixThemedColors();
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == StatisticValuesView.this) {
               fixThemedColors();
            }
         }

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.FONT_LOGGING_IS_MODIFIED)) {

            // update font

            final Font logFont = net.tourbook.ui.UI.getLogFont();

            // ensure the font is valid, this case occurred in Ubuntu
            if (logFont != null) {

               _txtStatValues.setFont(logFont);
            }

         } else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

            updateUI();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == StatisticValuesView.this) {
            return;
         }

         if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

            clearView();

         } else if ((tourEventId == TourEventId.STATISTIC_VALUES)) {

            // new statistic values are retrieved from the StatisticManager

            updateUI();
            enableActions();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      showInvalidPage();
   }

   private void createActions() {

      _action_PrefDialog = new ActionOpenPrefDialog(Messages.Tour_StatisticValues_Action_OpenPreferences_Tooltip, PrefPageAppearance.ID, ID);
      _action_PrefDialog.setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions));
      _action_PrefDialog.setDisabledImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.TourOptions_Disabled));

      _action_CopyIntoClipboard = new Action_CopyStatValuesIntoClipboard();
      _action_ShowCSVFormat = new Action_ShowCSVFormat();
      _action_ShowSequenceNumbers = new Action_ShowSequenceNumbers();
      _action_ShowZeroValues = new Action_ShowZeroValues();
      _action_GroupValues = new Action_GroupValues();

      fillActionBars();
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI();

      createUI(parent);
      createActions();

      addPartListener();
      addTourEventListener();
      addPrefListener();

      restoreState();
      enableActions();

      showInvalidPage();

      updateUI();
      enableActions();
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.Tour_StatisticValues_Label_NoStatistic);

      _pageContent = new Composite(_pageBook, SWT.NONE);
      _pageContent.setLayout(new FillLayout());
      {
         createUI_10_Container(_pageContent);
      }

      fixThemedColors();
   }

   private void createUI_10_Container(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {

         _txtStatValues = new StyledText(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
         _txtStatValues.setFont(net.tourbook.ui.UI.getLogFont());
         _txtStatValues.setTabs(1);
         _txtStatValues.addFocusListener(focusLostAdapter(focusEvent -> fixThemedColors()));
         _txtStatValues.addFocusListener(focusGainedAdapter(focusEvent -> fixThemedColors()));
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtStatValues);
      }
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private void enableActions() {

      final boolean isCSVFormat = _action_ShowCSVFormat.isChecked();
      final boolean isShowSequenceNumbers = _action_ShowSequenceNumbers.isChecked();

      boolean isCreateSequenceNumbers = isShowSequenceNumbers;
      if (isCSVFormat) {

         // CSV do not need sequence numbers
         isCreateSequenceNumbers = false;
      }

      final String rawStatisticValues = StatisticManager.getRawStatisticValues(isCreateSequenceNumbers);

      final boolean isStatValuesAvailable = rawStatisticValues != null;

      _action_CopyIntoClipboard.setEnabled(isStatValuesAvailable);
      _action_ShowCSVFormat.setEnabled(isStatValuesAvailable);

      _action_GroupValues.setEnabled(isStatValuesAvailable && !isCSVFormat);
      _action_ShowSequenceNumbers.setEnabled(isStatValuesAvailable && !isCSVFormat);
      _action_ShowZeroValues.setEnabled(isStatValuesAvailable && !isCSVFormat);
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_action_ShowSequenceNumbers);
      tbm.add(_action_ShowZeroValues);
      tbm.add(_action_GroupValues);
      tbm.add(_action_ShowCSVFormat);
      tbm.add(_action_CopyIntoClipboard);
      tbm.add(_action_PrefDialog);

      // setup actions
      tbm.update(true);
   }

   private void fixThemedColors() {

      _pageBook.getDisplay().asyncExec(() -> {

         if (_pageBook.isDisposed()) {
            return;
         }

         _txtStatValues.setBackground(ThemeUtil.getDarkestBackgroundColor());
      });
   }

   private void initUI() {

   }

   private void onAction_CopyIntoClipboard() {

      final String rawStatValues = StatisticManager.getRawStatisticValues(false);

      // ensure data are available
      if (rawStatValues == null) {
         return;
      }

      StatisticManager.copyStatisticValuesToTheClipboard(rawStatValues);
   }

   private void restoreState() {

      _action_GroupValues.setChecked(Util.getStateBoolean(_state, STATE_IS_GROUP_VALUES, true));
      _action_ShowCSVFormat.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_CSV_FORMAT, false));
      _action_ShowZeroValues.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_ZERO_VALUES, false));
      _action_ShowSequenceNumbers.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_SEQUENCE_NUMBERS, false));
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_GROUP_VALUES, _action_GroupValues.isChecked());
      _state.put(STATE_IS_SHOW_CSV_FORMAT, _action_ShowCSVFormat.isChecked());
      _state.put(STATE_IS_SHOW_ZERO_VALUES, _action_ShowZeroValues.isChecked());
      _state.put(STATE_IS_SHOW_SEQUENCE_NUMBERS, _action_ShowSequenceNumbers.isChecked());
   }

   @Override
   public void setFocus() {

      _txtStatValues.setFocus();
   }

   private void showInvalidPage() {

      _pageBook.showPage(_pageNoData);
   }

   private void updateUI() {

      final boolean isCSVFormat = _action_ShowCSVFormat.isChecked();
      final boolean isShowSequenceNumbers = _action_ShowSequenceNumbers.isChecked();

      boolean isCreateSequenceNumbers = isShowSequenceNumbers;
      if (isCSVFormat) {

         // CSV do not need sequence numbers
         isCreateSequenceNumbers = false;
      }

      final String rawStatValues = StatisticManager.getRawStatisticValues(isCreateSequenceNumbers);

      if (rawStatValues == null) {

         showInvalidPage();
         return;
      }

      _pageBook.showPage(_pageContent);

      final boolean isShowRawData = _isShowRawData;
      final boolean isRemoveZeros = _action_ShowZeroValues.isChecked() == false;
      final boolean isGroupValues = _action_GroupValues.isChecked();

      // reset state
      _isShowRawData = false;

      final String statValues = StatisticManager.formatStatValues(rawStatValues,
            isCSVFormat,
            isRemoveZeros,
            isGroupValues,
            isShowRawData);

      // keep scroll positions
      final int topIndex = _txtStatValues.getTopIndex();
      final int hIndex = _txtStatValues.getHorizontalIndex();

      // prevent flickering
      _txtStatValues.setRedraw(false);
      {
         _txtStatValues.setText(statValues);

         // restore scroll positions
         _txtStatValues.setTopIndex(topIndex);
         _txtStatValues.setHorizontalIndex(hIndex);
      }
      _txtStatValues.setRedraw(true);

      fixThemedColors();
   }

}
