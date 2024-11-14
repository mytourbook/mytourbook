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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.ui.action.TourAction;
import net.tourbook.ui.action.TourActionManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearance_TourActions extends PreferencePage implements IWorkbenchPreferencePage {

   private List<TourAction>    _allSortedActions = new ArrayList<>();

   private CheckboxTableViewer _tourActionViewer;

   /*
    * UI controls
    */
   private Button _btnDown;
   private Button _btnUp;

   @Override
   protected Control createContents(final Composite parent) {

      initUI();

      final Control ui = createUI(parent);

      restoreState();

      enableActions();
      enableUpDownActions();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * label: select info
          */
         final Label label = new Label(container, SWT.WRAP);
         label.setText("???");
         GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

         final Composite viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().indent(0, 10).applyTo(viewerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(viewerContainer);
//         viewerContainer.setBackground(UI.SYS_COLOR_BLUE);
         {
            createUI_10_ActionViewer(viewerContainer);
            createUI_20_ViewerActions(viewerContainer);
         }
      }

      return container;
   }

   private void createUI_10_ActionViewer(final Composite parent) {

      _tourActionViewer = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.TOP);
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

            return ((TourAction) element).getImage();
         }

         @Override
         public String getText(final Object element) {

            return ((TourAction) element).actionText;
         }
      });

      _tourActionViewer.addCheckStateListener(checkStateChangedEvent -> {

         // keep the checked status
         final TourAction item = (TourAction) checkStateChangedEvent.getElement();
         item.isChecked = checkStateChangedEvent.getChecked();

         // select the checked item
         _tourActionViewer.setSelection(new StructuredSelection(item));
      });

      _tourActionViewer.addSelectionChangedListener(selectionChangedEvent -> {
         enableUpDownActions();
      });

//    final Table table = _tourActionViewer.getTable();
//    table.setBackground(UI.SYS_COLOR_GREEN);
   }

   private void createUI_20_ViewerActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
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
   }

   private void enableActions() {

   }

   private void enableControls() {

   }

   /**
    * check if the up/down button are enabled
    */

   private void enableUpDownActions() {

      final Table table = _tourActionViewer.getTable();
      final TableItem[] items = table.getSelection();

      final boolean validSelection = items != null && items.length > 0;
      boolean enableUp = validSelection;
      boolean enableDown = validSelection;

      if (validSelection) {
         final int[] indices = table.getSelectionIndices();
         final int max = table.getItemCount();
         enableUp = indices[0] != 0;
         enableDown = indices[indices.length - 1] < max - 1;
      }

      _btnUp.setEnabled(enableUp);
      _btnDown.setEnabled(enableDown);
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI() {

   }

   /**
    * Moves an entry in the table to the given index.
    */
   private void move(final TableItem item, final int index) {

      this.setValid(true);

      final TourAction action = (TourAction) item.getData();
      item.dispose();

      _tourActionViewer.insert(action, index);
      _tourActionViewer.setChecked(action, action.isChecked);
   }

   /**
    * Move the current selection in the build list down
    */
   private void moveSelection_Down() {

      final Table table = _tourActionViewer.getTable();
      final int[] indices = table.getSelectionIndices();
      if (indices.length < 1) {
         return;
      }

      final int[] newSelection = new int[indices.length];
      final int max = table.getItemCount() - 1;

      for (int i = indices.length - 1; i >= 0; i--) {
         final int index = indices[i];
         if (index < max) {
            move(table.getItem(index), index + 1);
            newSelection[i] = index + 1;
         }
      }
      table.setSelection(newSelection);
   }

   /**
    * Move the current selection in the build list up
    */
   private void moveSelection_Up() {

      final Table table = _tourActionViewer.getTable();
      final int[] indices = table.getSelectionIndices();
      final int[] newSelection = new int[indices.length];

      for (int i = 0; i < indices.length; i++) {
         final int index = indices[i];
         if (index > 0) {
            move(table.getItem(index), index - 1);
            newSelection[i] = index - 1;
         }
      }
      table.setSelection(newSelection);
   }

   @Override
   public boolean okToLeave() {

      return super.okToLeave();
   }

   private void onSelection() {

      enableActions();
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

      onSelection();
   }

   @Override
   public boolean performOk() {

      saveState();

      return super.performOk();
   }

   private void restoreState() {

      // get viewer content
      _allSortedActions.clear();
      _allSortedActions.addAll(TourActionManager.getSortedActions());

      // load viewer
      _tourActionViewer.setInput(this);

      // check actions
      _tourActionViewer.setCheckedElements(TourActionManager.getCheckedActions().toArray());
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

         final Object actionClass = tourAction.actionClass;

         if (actionClass instanceof final Class clazz) {

            stateAllSortedActions[actionIndex] = clazz.getName();
         }
      }

      /*
       * Get all checked actions
       */
      final Object[] allCheckedActions = _tourActionViewer.getCheckedElements();
      final String[] stateAllCheckedActions = new String[allCheckedActions.length];

      for (int actionIndex = 0; actionIndex < allCheckedActions.length; actionIndex++) {

         final TourAction tourAction = (TourAction) allCheckedActions[actionIndex];

         final Object actionClass = tourAction.actionClass;

         if (actionClass instanceof final Class clazz) {

            stateAllCheckedActions[actionIndex] = clazz.getName();
         }
      }

      TourActionManager.saveActions(stateAllSortedActions, stateAllCheckedActions);
   }

}
