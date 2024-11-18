/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.ui.action.TourAction;
import net.tourbook.ui.action.TourActionManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PrefPageAppearance_TourActions extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String  ID                = "net.tourbook.preferences.PrefPageAppearance_TourActions"; //$NON-NLS-1$

   private List<TourAction>    _allSortedActions = new ArrayList<>();

   private CheckboxTableViewer _tourActionViewer;

   private SelectionListener   _defaultSelectionListener;

   private PixelConverter      _pc;

   /*
    * UI controls
    */
   private Button _btnCheckAll;
   private Button _btnUncheckAll;
   private Button _btnUp;
   private Button _btnDown;
   private Button _rdoShowAllActions;
   private Button _rdoShowCustomActions;

   private Link   _linkTagOptions;
   private Link   _linkTourTypeOptions;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Control ui = createUI(parent);

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         /*
          * Info
          */
         final Label label = new Label(container, SWT.WRAP);
         label.setText("Customize the tour contextual menu actions, some actions are not available in all context menus.");
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
               .applyTo(label);

         {
            /*
             * Show all values
             */
            _rdoShowAllActions = new Button(container, SWT.RADIO);
            _rdoShowAllActions.setText("Show &all actions");
            _rdoShowAllActions.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show custom values
             */
            _rdoShowCustomActions = new Button(container, SWT.RADIO);
            _rdoShowCustomActions.setText("&Customize actions");
            _rdoShowCustomActions.addSelectionListener(_defaultSelectionListener);
         }

         /*
          * Viewer & Actions
          */
         final Composite viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .indent(8, 0)
               .grab(true, true)
               .applyTo(viewerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(viewerContainer);
//         viewerContainer.setBackground(UI.SYS_COLOR_BLUE);
         {
            createUI_10_ActionViewer(viewerContainer);
            createUI_20_ViewerActions(viewerContainer);
            createUI_30_Options(viewerContainer);
         }
      }

      return container;
   }

   private void createUI_10_ActionViewer(final Composite parent) {

      final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(50, 100)
            .applyTo(layouter);

      final Table table = new Table(
            layouter,
            (SWT.CHECK
                  | SWT.SINGLE
//                | SWT.H_SCROLL
//                | SWT.V_SCROLL
                  | SWT.FULL_SELECTION));

      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      _tourActionViewer = new CheckboxTableViewer(table);

//      _tourActionViewer = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.TOP);
//      _tourActionViewer.getTable().setBackground(UI.SYS_COLOR_RED);

      _tourActionViewer.setContentProvider(new IStructuredContentProvider() {
         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            return _allSortedActions.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _tourActionViewer.setLabelProvider(new LabelProvider() {
         @Override
         public Image getImage(final Object element) {

            final boolean isCustomizeActions = _rdoShowCustomActions.getSelection();

            if (isCustomizeActions) {

               return ((TourAction) element).getImage();

            } else {

               return ((TourAction) element).getImageDisabled();
            }
         }

         @Override
         public String getText(final Object element) {

            final TourAction tourAction = (TourAction) element;

            if (tourAction.isCategory) {

               return tourAction.actionText;

            } else {

               return "       " + tourAction.actionText;
            }

         }
      });

      _tourActionViewer.addSelectionChangedListener(event -> enableControls());

      _tourActionViewer.addCheckStateListener(event -> {

         // keep the checked status
         final TourAction item = (TourAction) event.getElement();
         item.isChecked = event.getChecked();

         // select the checked item
         _tourActionViewer.setSelection(new StructuredSelection(item));
      });

//      _tourActionViewer.addCheckStateListener(event -> {
//
//         // If the checkEvent is on a locked update element, uncheck it and select it.
//
//         if (event.getElement() instanceof AvailableUpdateElement) {
//
//            final AvailableUpdateElement checkedElement = (AvailableUpdateElement) event.getElement();
//
//            if (checkedElement.isLockedForUpdate()) {
//
//               event.getCheckable().setChecked(checkedElement, false);
//
//               // Select the element so that the locked description is displayed
//               final CheckboxTableViewer viewer = ((CheckboxTableViewer) event.getSource());
//               final int itemCount = viewer.getTable().getItemCount();
//
//               for (int i = 0; i < itemCount; i++) {
//
//                  if (viewer.getElementAt(i).equals(checkedElement)) {
//                     viewer.getTable().deselectAll();
//                     viewer.getTable().select(i);
//                     setDetailText(resolvedOperation);
//                     break;
//                  }
//               }
//            }
//         }
//         updateSelection();
//      });

//    final Table table = _tourActionViewer.getTable();
//    table.setBackground(UI.SYS_COLOR_GREEN);
   }

   private void createUI_20_ViewerActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Button: Up
             */
            _btnUp = new Button(container, SWT.NONE);
            _btnUp.setText(Messages.Pref_Graphs_Button_up);
            _btnUp.setEnabled(false);
            _btnUp.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

               moveSelection_Up();
               enableUpDownActions();
            }));

            setButtonLayoutData(_btnUp);
         }
         {
            /*
             * Button: Down
             */
            _btnDown = new Button(container, SWT.NONE);
            _btnDown.setText(Messages.Pref_Graphs_Button_down);
            _btnDown.setEnabled(false);
            _btnDown.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

               moveSelection_Down();
               enableUpDownActions();

            }));

            setButtonLayoutData(_btnDown);
         }
         {
            /*
             * Button: Check all
             */
            _btnCheckAll = new Button(container, SWT.PUSH);
            _btnCheckAll.setText(Messages.App_Action_CheckAll);
            _btnCheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(true)));
            setButtonLayoutData(_btnCheckAll);
         }
         {
            /*
             * Button: Uncheck all
             */
            _btnUncheckAll = new Button(container, SWT.PUSH);
            _btnUncheckAll.setText(Messages.App_Action_UncheckAll);
            _btnUncheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(false)));
            setButtonLayoutData(_btnUncheckAll);
         }
      }
   }

   private void createUI_30_Options(final Composite parent) {

      final GridDataFactory gd = GridDataFactory.fillDefaults()
            .span(2, 1)
            .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
            .grab(true, false);

      {
         /*
          * Link to tag options
          */
         _linkTagOptions = new Link(parent, SWT.WRAP);
         _linkTagOptions.setText(Messages.Pref_TourTag_Link_AppearanceOptions);
         _linkTagOptions.addSelectionListener(widgetSelectedAdapter(selectionEvent ->

         PreferencesUtil.createPreferenceDialogOn(getShell(),
               PrefPageAppearance.ID,
               null,
               null)));

         gd.applyTo(_linkTagOptions);
      }
      {
         /*
          * Link to tour type options
          */
         _linkTourTypeOptions = new Link(parent, SWT.WRAP);
         _linkTourTypeOptions.setText("Further tour type options are available in the page <a>Tour Type / Groups</a>");
         _linkTourTypeOptions.addSelectionListener(widgetSelectedAdapter(selectionEvent ->

         PreferencesUtil.createPreferenceDialogOn(getShell(),
               PrefPageTourTypeFilterList.ID,
               null,
               null)));

         gd.applyTo(_linkTourTypeOptions);
      }

      UI.createSpacer_Horizontal(parent, 2);
   }

   private void enableControls() {

      final boolean isCustomizeActions = _rdoShowCustomActions.getSelection();

      final IStructuredSelection structuredSelection = _tourActionViewer.getStructuredSelection();
      final TourAction selectedAction = (TourAction) structuredSelection.getFirstElement();
      final boolean isActionCategory = selectedAction != null && selectedAction.isCategory;

      // update icons
      _tourActionViewer.refresh();

      _tourActionViewer.getTable().setEnabled(isCustomizeActions);

      _btnCheckAll.setEnabled(isCustomizeActions);
      _btnUncheckAll.setEnabled(isCustomizeActions);

      _linkTagOptions.setEnabled(isCustomizeActions);
      _linkTourTypeOptions.setEnabled(isCustomizeActions);

      if (isCustomizeActions && isActionCategory == false) {

         enableUpDownActions();

      } else {

         _btnUp.setEnabled(false);
         _btnDown.setEnabled(false);
      }
   }

   /**
    * check if the up/down button are enabled
    */

   private void enableUpDownActions() {

      final Table table = _tourActionViewer.getTable();
      final TableItem[] items = table.getSelection();

      final boolean isValidSelection = items.length > 0;

      boolean isEnableUp = isValidSelection;
      boolean isEnableDown = isValidSelection;

      if (isValidSelection) {

         final int[] allIndices = table.getSelectionIndices();
         final int numItems = table.getItemCount();

         isEnableUp = allIndices[0] != 0;
         isEnableDown = allIndices[allIndices.length - 1] < numItems - 1;
      }

      _btnUp.setEnabled(isEnableUp);
      _btnDown.setEnabled(isEnableDown);
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Control parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModified());
   }

   /**
    * Move the current selection in the build list down
    */
   private void moveSelection_Down() {

      final Table table = _tourActionViewer.getTable();
      final int[] allSelectedIndices = table.getSelectionIndices();

      if (allSelectedIndices.length > 0) {

         final int selectedIndex = allSelectedIndices[0];
         Collections.swap(_allSortedActions, selectedIndex, selectedIndex + 1);

         _tourActionViewer.refresh();
      }
   }

   /**
    * Move the current selection in the build list up
    */
   private void moveSelection_Up() {

      final Table table = _tourActionViewer.getTable();
      final int[] allSelectedIndices = table.getSelectionIndices();

      if (allSelectedIndices.length > 0) {

         final int selectedIndex = allSelectedIndices[0];

         Collections.swap(_allSortedActions, selectedIndex, selectedIndex - 1);

         _tourActionViewer.refresh();
      }
   }

   private void onCheckAll(final boolean isChecked) {

      if (isChecked) {

         _tourActionViewer.setCheckedElements(TourActionManager.getSortedActions().toArray());

      } else {

         _tourActionViewer.setCheckedElements(new Object[] {});
      }
   }

   private void onModified() {

      enableControls();
   }

   @Override
   public boolean performCancel() {

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      super.performDefaults();

      _allSortedActions.clear();
      _allSortedActions.addAll(TourActionManager.getDefinedActions());

      // load viewer
      _tourActionViewer.setInput(this);

      enableControls();
   }

   @Override
   public boolean performOk() {

      saveState();

      return super.performOk();
   }

   private void restoreState() {

      final boolean isCustomizeActions = TourActionManager.isCustomizeActions();

      _rdoShowAllActions.setSelection(isCustomizeActions == false);
      _rdoShowCustomActions.setSelection(isCustomizeActions);

      // get viewer content
      _allSortedActions.clear();
      _allSortedActions.addAll(TourActionManager.getSortedActions());

      // load viewer
      _tourActionViewer.setInput(this);

      // check only the visible actions
      _tourActionViewer.setCheckedElements(TourActionManager.getVisibleActions().toArray());
   }

   private void saveState() {

      /*
       * Get sorting
       */
      final TableItem[] tableItems = _tourActionViewer.getTable().getItems();
      final int numActions = tableItems.length;
      final String[] stateAllSortedActions = new String[numActions];

      for (int actionIndex = 0; actionIndex < numActions; actionIndex++) {

         final TourAction tourAction = (TourAction) tableItems[actionIndex].getData();

         if (tourAction.isCategory) {

            stateAllSortedActions[actionIndex] = tourAction.getCategoryClassName();

         } else {

            stateAllSortedActions[actionIndex] = tourAction.actionClassName;
         }
      }

      /*
       * Get all checked actions
       */
      final Object[] allCheckedActions = _tourActionViewer.getCheckedElements();
      final String[] stateAllCheckedActions = new String[allCheckedActions.length];

      for (int actionIndex = 0; actionIndex < allCheckedActions.length; actionIndex++) {

         final TourAction tourAction = (TourAction) allCheckedActions[actionIndex];

         stateAllCheckedActions[actionIndex] = tourAction.actionClassName;
      }

      TourActionManager.saveActions(

            _rdoShowCustomActions.getSelection(),

            stateAllSortedActions,
            stateAllCheckedActions);
   }

}
