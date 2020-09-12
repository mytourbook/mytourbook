/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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

import java.util.regex.Pattern;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class StatisticValuesView extends ViewPart {

   public static final String            ID                        = "net.tourbook.statistic.StatisticValuesView"; //$NON-NLS-1$

   private static final String           STATE_IS_SHOW_CSV_FORMAT  = "STATE_IS_SHOW_CSV_FORMAT";                   //$NON-NLS-1$
   private static final String           STATE_IS_SHOW_ZERO_VALUES = "STATE_IS_SHOW_ZERO_VALUES";                  //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                    = TourbookPlugin.getState(ID);

// SET_FORMATTING_OFF

   private static final Pattern FIELD_PATTERN             = Pattern.compile(","); //$NON-NLS-1$
   private static final Pattern SPACE_PATTERN             = Pattern.compile("  *"); //$NON-NLS-1$

   private static final Pattern NUMBER_PATTERN_0          = Pattern.compile(" 0 ");                         //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_END      = Pattern.compile(" 0$",     Pattern.MULTILINE);  //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_0        = Pattern.compile(" 0.0 ");                       //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_0_END    = Pattern.compile(" 0.0$",   Pattern.MULTILINE);  //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_00       = Pattern.compile(" 0.00 ");                      //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_00_END   = Pattern.compile(" 0.00$",  Pattern.MULTILINE);  //$NON-NLS-1$

//SET_FORMATTING_ON

   private IPropertyChangeListener   _prefChangeListener;
   private ITourEventListener        _tourEventListener;

   private Selection_StatisticValues _selection_StatisticValues;

   private Action_CopyIntoClipboard  _action_CopyIntoClipboard;
   private Action_ShowCSVFormat      _action_ShowCSVFormat;
   private Action_ShowZeroValues     _action_ShowZeroValues;

   /*
    * UI controls
    */
   private PageBook   _pageBook;

   private Composite  _pageNoData;
   private Composite  _pageContent;

   private Clipboard  _clipBoard;

   /**
    * Usinge {@link Text} or {@link StyledText}
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

   private class Action_CopyIntoClipboard extends Action {

      Action_CopyIntoClipboard() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Tour_StatisticValues_Action_CopyIntoClipboard_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Copy));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Copy_Disabled));
      }

      @Override
      public void run() {
         onAction_CopyIntoClipboard();
      }
   }

   private class Action_ShowCSVFormat extends Action {

      Action_ShowCSVFormat() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_StatisticValues_Action_CSVFormat_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__CSVFormat));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__CSVFormat_Disabled));
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

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZeroValues));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZeroValues_Disabled));
      }

      @Override
      public void run() {
         enableActions();
         updateUI();
      }
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

            } else if (property.equals(ITourbookPreferences.FONT_LOGGING_IS_MODIFIED)) {

               // update font

               _txtStatValues.setFont(net.tourbook.ui.UI.getLogFont());

            } else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

               updateUI();
            }
         }
      };
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == StatisticValuesView.this) {
               return;
            }

            if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if ((eventId == TourEventId.STATISTIC_VALUES) && eventData instanceof Selection_StatisticValues) {

               onSelectionChanged((Selection_StatisticValues) eventData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      showInvalidPage();
   }

   private void createActions() {

      _action_CopyIntoClipboard = new Action_CopyIntoClipboard();
      _action_ShowCSVFormat = new Action_ShowCSVFormat();
      _action_ShowZeroValues = new Action_ShowZeroValues();

      fillActionBars();
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI();

      createUI(parent);
      createActions();

      addTourEventListener();
      addPrefListener();

      restoreState();
      enableActions();

      showInvalidPage();

      // get last statistic values from the statistic view
      final StatisticView statisticView = StatisticManager.getStatisticView();
      if (statisticView != null && statisticView.getStatisticValuesRaw() != null) {

         final String statisticValuesRaw = statisticView.getStatisticValuesRaw();

         _selection_StatisticValues = new Selection_StatisticValues(statisticValuesRaw);
      }

      updateUI();
      enableActions();
   }

   private void createUI(final Composite parent) {

      _clipBoard = new Clipboard(parent.getDisplay());

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.Tour_StatisticValues_Label_NoData);

      _pageContent = new Composite(_pageBook, SWT.NONE);
      _pageContent.setLayout(new FillLayout());
      {
         createUI_10_Container(_pageContent);
      }
   }

   private void createUI_10_Container(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {

         _txtStatValues = new StyledText(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
         _txtStatValues.setFont(net.tourbook.ui.UI.getLogFont());
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtStatValues);
      }
   }

   @Override
   public void dispose() {

      _clipBoard.dispose();

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableActions() {

      final boolean isCSVFormat = _action_ShowCSVFormat.isChecked();
      final boolean isStatValuesAvailable = _selection_StatisticValues != null && _selection_StatisticValues.statisticValuesRaw != null;

      _action_CopyIntoClipboard.setEnabled(isStatValuesAvailable);
      _action_ShowCSVFormat.setEnabled(isStatValuesAvailable);
      _action_ShowZeroValues.setEnabled(isStatValuesAvailable && !isCSVFormat);
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_action_ShowZeroValues);
      tbm.add(_action_ShowCSVFormat);
      tbm.add(_action_CopyIntoClipboard);

      // setup actions
      tbm.update(true);
   }

   private String formatStatValues(String statValues, final boolean isCSVFormat, final boolean isRemoveZeros) {

      if (isCSVFormat) {

         // remove spaces
         statValues = SPACE_PATTERN.matcher(statValues).replaceAll(UI.EMPTY_STRING);

      } else {

         // remove field separator
         statValues = FIELD_PATTERN.matcher(statValues).replaceAll(UI.EMPTY_STRING);

         // remove zeros
         if (isRemoveZeros) {

// SET_FORMATTING_OFF

            statValues = NUMBER_PATTERN_0.          matcher(statValues).replaceAll("   ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_END.      matcher(statValues).replaceAll("  ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_0.        matcher(statValues).replaceAll("     ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_0_END.    matcher(statValues).replaceAll("    ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_00.       matcher(statValues).replaceAll("      ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_00_END.   matcher(statValues).replaceAll("     ");//$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }

      return statValues;
   }

   private void initUI() {

   }

   private void onAction_CopyIntoClipboard() {

      // ensure data are available
      if (_selection_StatisticValues == null || _selection_StatisticValues.statisticValuesRaw == null) {
         return;
      }

      final boolean isCSVFormat = true;
      final boolean isRemoveZeros = false;

      final String statValues = formatStatValues(_selection_StatisticValues.statisticValuesRaw, isCSVFormat, isRemoveZeros);

      if (statValues.length() > 0) {

         final TextTransfer textTransfer = TextTransfer.getInstance();

         _clipBoard.setContents(

               new Object[] { statValues },
               new Transfer[] { textTransfer }

         );

         // show info that data are copied
         final IStatusLineManager statusLineMgr = getViewSite().getActionBars().getStatusLineManager();
         statusLineMgr.setMessage(Messages.Tour_StatisticValues_Info_DataAreCopied);

         _pageBook.getDisplay().timerExec(2000,
               () -> {

                  // cleanup message
                  statusLineMgr.setMessage(null);
               });
      }
   }

   private void onSelectionChanged(final Selection_StatisticValues selection) {

      _selection_StatisticValues = selection;

      updateUI();
      enableActions();
   }

   private void restoreState() {

      _action_ShowCSVFormat.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_CSV_FORMAT, false));
      _action_ShowZeroValues.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_ZERO_VALUES, false));
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_SHOW_CSV_FORMAT, _action_ShowCSVFormat.isChecked());
      _state.put(STATE_IS_SHOW_ZERO_VALUES, _action_ShowZeroValues.isChecked());
   }

   @Override
   public void setFocus() {

      _txtStatValues.setFocus();
   }

   private void showInvalidPage() {

      _pageBook.showPage(_pageNoData);
   }

   private void updateUI() {

      if (_selection_StatisticValues == null || _selection_StatisticValues.statisticValuesRaw == null) {

         showInvalidPage();
         return;
      }

      _pageBook.showPage(_pageContent);

      final boolean isCSVFormat = _action_ShowCSVFormat.isChecked();
      final boolean isRemoveZeros = _action_ShowZeroValues.isChecked() == false;

      final String statValues = formatStatValues(_selection_StatisticValues.statisticValuesRaw, isCSVFormat, isRemoveZeros);

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
   }

}
