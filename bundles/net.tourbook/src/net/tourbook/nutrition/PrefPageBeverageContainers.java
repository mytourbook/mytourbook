/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package net.tourbook.nutrition;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageBeverageContainers extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String          ID         = "net.tourbook.cloud.PrefPageBeverageContainers"; //$NON-NLS-1$

   private final IPreferenceStore      _prefStore = TourbookPlugin.getPrefStore();

   private List<TourBeverageContainer> _beverageContainers;

   private boolean                     _isModified;

   /*
    * UI controls
    */
   private Image       _imageDelete;

   private TableViewer _tourBeverageContainerViewer;

   private Button      _btnDelete;
   private Button      _btnEdit;

   private class BeverageContainerContentProvider implements IStructuredContentProvider {

      @Override
      public Object[] getElements(final Object inputElement) {
         return _beverageContainers.toArray(new Object[_beverageContainers.size()]);
      }
   }

   private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {

         return null;
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         final TourBeverageContainer tourBeverageContainer = (TourBeverageContainer) obj;

         switch (index) {
         case 0:
            return tourBeverageContainer.getName();

         case 1:
            return String.valueOf(tourBeverageContainer.getCapacity());

         default:
            return getText(obj);
         }
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      // read beverage containers from the database
      _beverageContainers = TourDatabase.getTourBeverageContainers();

      enableActions();

      parent.getDisplay().asyncExec(() -> _tourBeverageContainerViewer.setInput(this));

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Label label = UI.createLabel(parent, Messages.PrefPage_Nutrition_BeverageContainers_Title, SWT.WRAP);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         createUI_10_BeverageContainersViewer(container);
         createUI_20_Actions(container);
      }

      return container;
   }

   private void createUI_10_BeverageContainersViewer(final Composite parent) {

      /*
       * Table viewer: beverage containers
       */
      final Table beverageContainersTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(beverageContainersTable);
      beverageContainersTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      beverageContainersTable.setHeaderVisible(true);

      defineAllColumns(beverageContainersTable);
      _tourBeverageContainerViewer = new TableViewer(beverageContainersTable);

      _tourBeverageContainerViewer.setContentProvider(new BeverageContainerContentProvider());
      _tourBeverageContainerViewer.setLabelProvider(new ViewLabelProvider());

      _tourBeverageContainerViewer.setUseHashlookup(true);

      _tourBeverageContainerViewer.getTable().addKeyListener(keyPressedAdapter(keyEvent -> {

         switch (keyEvent.keyCode) {

         case SWT.DEL:

            if (_btnDelete.isEnabled()) {
               onTourBeverageContainer_Delete();
            }

            break;

         case SWT.F2:

            if (_btnEdit.isEnabled()) {
               onTourBeverageContainer_Edit();
            }

            break;
         }
      }));

      _tourBeverageContainerViewer.addSelectionChangedListener(selectionChangedEvent -> enableActions());
   }

   private void createUI_20_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Add
             */
            final Button btnAdd = new Button(container, SWT.NONE);
            btnAdd.setText(Messages.App_Action_Add);
            btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
               onTourBeverageContainer_Add();
               enableActions();
            }));
            setButtonLayoutData(btnAdd);
         }
         {
            /*
             * Edit
             */
            _btnEdit = new Button(container, SWT.NONE);
            _btnEdit.setText(Messages.App_Action_Edit);
            _btnEdit.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onTourBeverageContainer_Edit()));
            setButtonLayoutData(_btnEdit);
         }
         {
            /*
             * Delete
             */
            _btnDelete = new Button(container, SWT.NONE);
            _btnDelete.setText(Messages.App_Action_Delete);

            _imageDelete = TourbookPlugin.getImageDescriptor(Images.App_Trash).createImage();
            _btnDelete.setImage(_imageDelete);
            _btnDelete.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
               onTourBeverageContainer_Delete();
               enableActions();
            }));
            setButtonLayoutData(_btnDelete);
         }
      }
   }

   /**
    * Create columns
    */
   private void defineAllColumns(final Table productsTable) {

      // Column: Name
      final TableColumn columnServings = new TableColumn(productsTable, SWT.LEFT);
      columnServings.setText(Messages.Tour_Nutrition_Column_Code);
      columnServings.setWidth(300);

      // Column: Capacity
      final TableColumn columnName = new TableColumn(productsTable, SWT.LEFT);
      columnName.setText(Messages.Tour_Nutrition_Column_Capacity);
      columnName.setWidth(100);
   }

   private boolean deleteTourBeverageContainer(final TourBeverageContainer tourBeverageContainer) {

      boolean returnResult = false;

      String sql;

      PreparedStatement prepStmt_TourData = null;
      PreparedStatement prepStmt_TourBeverageContainer = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove container from TABLE_TOUR_BEVERAGE_CONTAINER
         sql = "UPDATE " + TourDatabase.TABLE_TOUR_NUTRITION_PRODUCT //          //$NON-NLS-1$
               + " SET " + TourDatabase.KEY_BEVERAGE_CONTAINER + "=null" //$NON-NLS-1$ //$NON-NLS-2$
               + " WHERE " + TourDatabase.KEY_BEVERAGE_CONTAINER + "=?"; //                      //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TourData = conn.prepareStatement(sql);

         // remove tag from TABLE_TOUR_BEVERAGE_CONTAINER
         sql = "DELETE" //                                                       //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_BEVERAGE_CONTAINER //                       //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_BEVERAGECONTAINER + "=?"; //               //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TourBeverageContainer = conn.prepareStatement(sql);

         int[] returnValue_TourData;

         conn.setAutoCommit(false);
         {
            final long containerId = tourBeverageContainer.getContainerId();

            prepStmt_TourData.setLong(1, containerId);
            prepStmt_TourData.addBatch();

            prepStmt_TourBeverageContainer.setLong(1, containerId);
            prepStmt_TourBeverageContainer.addBatch();

            returnValue_TourData = prepStmt_TourData.executeBatch();
            prepStmt_TourBeverageContainer.executeBatch();
         }
         conn.commit();

         // log result
         TourLogManager.showLogView(AutoOpenEvent.DELETE_SOMETHING);

         TourLogManager.log_INFO(String.format(Messages.PrefPage_Nutrition_BeverageContainers_LogInfo_DeletedContainer,
               returnValue_TourData[0],
               tourBeverageContainer.getName()));

         returnResult = true;

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_TourData);
         Util.closeSql(prepStmt_TourBeverageContainer);
      }

      return returnResult;
   }

   @Override
   public void dispose() {

      UI.disposeResource(_imageDelete);

      super.dispose();
   }

   private void enableActions() {

      final StructuredSelection selectedItems = (StructuredSelection) _tourBeverageContainerViewer.getSelection();
      final int numSelectedItems = selectedItems.size();
      final boolean canDeleteContainer = selectedItems.size() > 0;

      _btnDelete.setEnabled(canDeleteContainer);
      _btnEdit.setEnabled(numSelectedItems == 1);
   }

   private void fireModifyEvent() {

      if (_isModified) {

         _isModified = false;

         TourManager.getInstance().clearTourDataCache();

         // fire modify event
         _prefStore.setValue(ITourbookPreferences.NUTRITION_BEVERAGECONTAINERS_HAVE_CHANGED, Math.random());
      }
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);

      noDefaultAndApplyButton();
   }

   @Override
   public boolean okToLeave() {

      fireModifyEvent();

      return super.okToLeave();
   }

   private void onTourBeverageContainer_Add() {

      final DialogBeverageContainer dialogBeverageContainer = new DialogBeverageContainer(getShell(), false);

      if (dialogBeverageContainer.open() != Window.OK) {

         setFocusToViewer();
         return;
      }

      // get the new values from the dialog
      final String name = dialogBeverageContainer.getName();
      final float capacity = dialogBeverageContainer.getCapacity();

      // create new tour beverage container
      final TourBeverageContainer newTourBeverageContainer = new TourBeverageContainer(name, capacity);

      // add new entity to db
      final TourBeverageContainer savedTourBeverageContainer = saveTourBeverageContainer(newTourBeverageContainer);

      if (savedTourBeverageContainer != null) {

         // update model
         _beverageContainers.add(savedTourBeverageContainer);

         // update UI
         _tourBeverageContainerViewer.add(savedTourBeverageContainer);
         _tourBeverageContainerViewer.setSelection(new StructuredSelection(savedTourBeverageContainer), true);

         _isModified = true;

         setFocusToViewer();
      }
   }

   private void onTourBeverageContainer_Delete() {

      final Object selectedItem = ((IStructuredSelection) _tourBeverageContainerViewer.getSelection()).getFirstElement();
      if (selectedItem == null) {
         return;
      }

      final TourBeverageContainer selectedTourBeverageContainer = (TourBeverageContainer) selectedItem;

      // confirm deletion
      final MessageDialog dialog = new MessageDialog(
            getShell(),
            Messages.PrefPage_Nutrition_BeverageContainers_Dialog_Delete_BeverageContainer_Title,
            null,
            NLS.bind(Messages.PrefPage_Nutrition_BeverageContainers_Dialog_Delete_BeverageContainer_Message, selectedTourBeverageContainer.getName()),
            MessageDialog.QUESTION,
            new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
            1);

      if (dialog.open() != Window.OK) {

         setFocusToViewer();
         return;
      }

      BusyIndicator.showWhile(getShell().getDisplay(), () -> {

         // remove entity from the db
         if (deleteTourBeverageContainer(selectedTourBeverageContainer)) {

            // update UI
            _tourBeverageContainerViewer.remove(selectedTourBeverageContainer);

            _isModified = true;
         }
      });

      setFocusToViewer();
   }

   private void onTourBeverageContainer_Edit() {

      final Object selectedItem = ((IStructuredSelection) _tourBeverageContainerViewer.getSelection()).getFirstElement();
      if (selectedItem == null) {
         return;
      }

      final TourBeverageContainer selectedBeverageContainer = (TourBeverageContainer) selectedItem;

      final DialogBeverageContainer dialogBeverageContainer = new DialogBeverageContainer(getShell(), true);
      dialogBeverageContainer.setName(selectedBeverageContainer.getName());
      dialogBeverageContainer.setCapacity(selectedBeverageContainer.getCapacity());

      if (dialogBeverageContainer.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      // update tour beverage container
      selectedBeverageContainer.setName(dialogBeverageContainer.getName());
      selectedBeverageContainer.setCapacity(dialogBeverageContainer.getCapacity());

      // update entity in the db
      final TourBeverageContainer savedTourBeverageContainer = saveTourBeverageContainer(selectedBeverageContainer);

      if (savedTourBeverageContainer != null) {

         // update model
         _beverageContainers.remove(selectedBeverageContainer);
         _beverageContainers.add(savedTourBeverageContainer);

         // update UI
         _tourBeverageContainerViewer.remove(selectedBeverageContainer);
         _tourBeverageContainerViewer.add(savedTourBeverageContainer);
         _tourBeverageContainerViewer.setSelection(new StructuredSelection(savedTourBeverageContainer), true);

         _isModified = true;
      }

      setFocusToViewer();
   }

   @Override
   public boolean performCancel() {

      fireModifyEvent();

      return super.performCancel();
   }

   @Override
   public boolean performOk() {

      fireModifyEvent();

      return super.performOk();
   }

   private TourBeverageContainer saveTourBeverageContainer(final TourBeverageContainer tourBeverageContainer) {

      return TourDatabase.saveEntity(
            tourBeverageContainer,
            tourBeverageContainer.getContainerId(),
            TourBeverageContainer.class);
   }

   private void setFocusToViewer() {

      // set focus back to the table
      _tourBeverageContainerViewer.getTable().setFocus();
   }
}
