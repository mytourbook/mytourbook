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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
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

   public static final String          ID                 = "net.tourbook.cloud.PrefPageBeverageContainers"; //$NON-NLS-1$

   private final IPreferenceStore      _prefStore         = TourbookPlugin.getPrefStore();

   private List<TourBeverageContainer> _dbTourTypes;

   /**
    * This is the model of the tour type viewer.
    */
   private List<TourBeverageContainer> _beverageContainers;

   private boolean                     _isModified;

   private boolean                     _canModifyTourType = true;

   /*
    * UI controls
    */
   private Image       _imageDelete;

   private TableViewer _tourBeverageContainerViewer;

   private Button      _btnAdd;
   private Button      _btnDelete;
   private Button      _btnEdit;

   private class BeverageContainerContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {
         // Nothing to do
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _beverageContainers.toArray(new Object[_beverageContainers.size()]);
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
         // Nothing to do
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

      @Override
      public Image getImage(final Object obj) {

         return null;
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      /*
       * Ensure that a tour is NOT modified because changing the tour type needs an app restart
       * because the tour type images are DISPOSED
       */
      if (_canModifyTourType == false) {

         final Label label = new Label(parent, SWT.WRAP);
         label.setText(Messages.Pref_TourTypes_Label_TourIsDirty);
         GridDataFactory.fillDefaults().applyTo(label);

         return label;
      }

      final Composite ui = createUI(parent);

      //fillUI();

      // read tour types from the database
      _dbTourTypes = TourDatabase.getTourBeverageContainers();

      /*
       * create color definitions for all tour types
       */
      _beverageContainers = new ArrayList<>();

      if (_dbTourTypes != null) {

         for (final TourBeverageContainer tourType : _dbTourTypes) {

            _beverageContainers.add(tourType);
         }
      }

      enableActions();

      /*
       * MUST be run async otherwise the background color is NOT themed !!!
       */
      parent.getDisplay().asyncExec(() -> _tourBeverageContainerViewer.setInput(this));

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
      label.setText(Messages.PrefPage_TourBeverageContainers_Title);

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

      final String[] tableColumns = { "Name", "Capacity" };

      defineAllColumns(beverageContainersTable, tableColumns);
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
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Add
             */
            _btnAdd = new Button(container, SWT.NONE);
            _btnAdd.setText(Messages.PrefPage_TourBeverageContainers_Button_Add);
            _btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
               onTourBeverageContainer_Add();
               enableActions();
            }));
            setButtonLayoutData(_btnAdd);
         }
         {
            /*
             * Edit
             */
            _btnEdit = new Button(container, SWT.NONE);
            _btnEdit.setText(Messages.PrefPage_TourBeverageContainers_Button_Edit);
            _btnEdit.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onTourBeverageContainer_Edit()));
            setButtonLayoutData(_btnEdit);
         }
         {
            /*
             * Delete
             */
            _btnDelete = new Button(container, SWT.NONE);
            _btnDelete.setText(Messages.PrefPage_TourBeverageContainers_Button_Delete);

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
   private void defineAllColumns(final Table productsTable, final String[] tableColumns) {

      int index = 0;

      // Column: Name
      final TableColumn columnServings = new TableColumn(productsTable, SWT.LEFT);
      columnServings.setText(tableColumns[index++]);
      columnServings.setWidth(300);

      // Column: Capacity
      final TableColumn columnName = new TableColumn(productsTable, SWT.LEFT);
      columnName.setText(tableColumns[index++]);
      columnName.setWidth(100);
   }

   private boolean deleteTourBeverageContainer(final TourBeverageContainer tourBeverageContainer) {

      if (deleteTourBeverageContainer_10_FromTourData(tourBeverageContainer) &&
            deleteTourBeverageContainer_20_FromDb(tourBeverageContainer)) {
         return true;
      }

      return false;
   }

   private boolean deleteTourBeverageContainer_10_FromTourData(final TourBeverageContainer tourBeverageContainer) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em != null) {

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT tourData" //$NON-NLS-1$
               + " FROM TourData AS tourData" //$NON-NLS-1$
               + " WHERE tourData.tourType.typeId=" + tourBeverageContainer.getContainerId()); //$NON-NLS-1$

         final List<?> tourDataList = query.getResultList();
         if (tourDataList.size() > 0) {

            final EntityTransaction ts = em.getTransaction();

            try {

               ts.begin();

               // remove tour beverage container from all tour nutrition products
               for (final Object listItem : tourDataList) {

                  if (listItem instanceof final TourData tourData) {

                     tourData.setTourType(null);
                     em.merge(tourData);
                  }
               }

               ts.commit();

            } catch (final Exception e) {
               StatusUtil.showStatus(e);
            } finally {
               if (ts.isActive()) {
                  ts.rollback();
               }
            }
         }

         returnResult = true;
         em.close();
      }

      return returnResult;
   }

   private boolean deleteTourBeverageContainer_20_FromDb(final TourBeverageContainer tourBeverageContainer) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {
         final TourBeverageContainer tourBeverageContainerEntity = em.find(TourBeverageContainer.class, tourBeverageContainer.getContainerId());

         if (tourBeverageContainerEntity != null) {

            ts.begin();

            em.remove(tourBeverageContainerEntity);

            ts.commit();
         }

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            returnResult = true;
         }
         em.close();
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
         _prefStore.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
      }
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);

      noDefaultAndApplyButton();
   }

   @Override
   public boolean okToLeave() {

      if (_canModifyTourType) {

         fireModifyEvent();
      }

      return super.okToLeave();
   }

   private void onTourBeverageContainer_Add() {

      final DialogBeverageContainer dialogBeverageContainer = new DialogBeverageContainer(getShell());

      // get the new values from the dialog
      if (dialogBeverageContainer.open() != Window.OK) {
         // ask for the tour type name
//      final InputDialog inputDialog = new InputDialog(
//            getShell(),
//            Messages.Pref_TourTypes_Dlg_new_tour_type_title,
//            Messages.Pref_TourTypes_Dlg_new_tour_type_msg,
//            UI.EMPTY_STRING,
//            null);

         setFocusToViewer();
         return;
      }

      final String name = dialogBeverageContainer.getName();
      final float capacity = dialogBeverageContainer.getCapacity();

      // create new tour type
      final TourBeverageContainer newTourBeverageContainer = new TourBeverageContainer(name, capacity);

      // add new entity to db
      final TourBeverageContainer savedTourBeverageContainer = saveTourBeverageContainer(newTourBeverageContainer);

      if (savedTourBeverageContainer != null) {

         /*
          * Create a color definition WITH THE SAVED tour type, this fixes a VEEEEEEEEEEERY long
          * existing bug that a new tour type is initially not displayed correctly in the color
          * definition image.
          */
         // create the same color definition but with the correct id's
//         final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
//               savedTourType,
//               "tourType." + savedTourType.getTypeId(), //$NON-NLS-1$
//               tourTypeName);
//
//         // overwrite tour type object
//         newColorDefinition.setTourType(savedTourType);
//
//         createGraphColorItems(newColorDefinition);

         // update model
         _beverageContainers.add(savedTourBeverageContainer);

         // update internal tour type list
         _dbTourTypes.add(savedTourBeverageContainer);
         //  Collections.sort(_dbTourTypes);

         // update UI
         _tourBeverageContainerViewer.add(savedTourBeverageContainer);

         _tourBeverageContainerViewer.setSelection(new StructuredSelection(savedTourBeverageContainer), true);

         _isModified = true;

         setFocusToViewer();
      }
   }

   private void onTourBeverageContainer_Delete() {

      final List<String> allTourTypeNames = new ArrayList<>();

      _beverageContainers.stream()
            .forEach(tourType -> allTourTypeNames.add(tourType.getName()));

      final String allTourTypeNamesJoined = StringUtils
            .join(allTourTypeNames.stream().toArray(String[]::new), UI.COMMA_SPACE);

      // confirm deletion
      final MessageDialog dialog = new MessageDialog(
            getShell(),
            Messages.Pref_TourTypes_Dlg_delete_tour_type_title,
            null,
            NLS.bind(Messages.Pref_TourTypes_Dlg_delete_tour_type_msg, allTourTypeNamesJoined),
            MessageDialog.QUESTION,
            new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
            1);

      if (dialog.open() != Window.OK) {

         setFocusToViewer();
         return;
      }

      BusyIndicator.showWhile(getShell().getDisplay(), () -> {

         for (final TourBeverageContainer selectedBeverageContainers : _beverageContainers) {

            // remove entity from the db
            if (deleteTourBeverageContainer(selectedBeverageContainers)) {

               // update model
               _dbTourTypes.remove(selectedBeverageContainers);

               // update UI
               _tourBeverageContainerViewer.remove(selectedBeverageContainers);

// a tour type image cannot be deleted otherwise an image dispose exception can occur
//             TourTypeImage.deleteTourTypeImage(selectedTourType.getTypeId());

               _isModified = true;
            }
         }
      });

      setFocusToViewer();
   }

   private void onTourBeverageContainer_Edit() {

      final StructuredSelection structuredSelection = (StructuredSelection) _tourBeverageContainerViewer.getSelection();

      final TourBeverageContainer selectedBeverageContainer = (TourBeverageContainer) structuredSelection.getFirstElement();

      final DialogBeverageContainer dialogBeverageContainer = new DialogBeverageContainer(getShell());
      dialogBeverageContainer.setName(selectedBeverageContainer.getName());
      dialogBeverageContainer.setCapacity(selectedBeverageContainer.getCapacity());
      dialogBeverageContainer.open();
      return;

//      final TourType selectedTourType = selectedColorDefinition.getTourType();
//
//      // ask for the tour type name
//      final InputDialog dialog = new InputDialog(
//            getShell(),
//            Messages.Pref_TourTypes_Dlg_rename_tour_type_title,
//            NLS.bind(Messages.Pref_TourTypes_Dlg_rename_tour_type_msg, selectedTourType.getName()),
//            selectedTourType.getName(),
//            null);
//
//      if (dialog.open() != Window.OK) {
//         setFocusToViewer();
//         return;
//      }
//
//      // update tour type name
//      final String newTourTypeName = dialog.getValue();
//
//      selectedTourType.setName(newTourTypeName);
//      selectedColorDefinition.setVisibleName(newTourTypeName);
//
//      // update entity in the db
//      final TourBeverageContainer savedTourType;/// = saveTourBeverageContainer(selectedTourType);

//      if (savedTourType != null) {
//
//         // update model
//         //   selectedColorDefinition.setTourType(savedTourType);
//
//         // replace tour type with new one
//         _dbTourTypes.remove(selectedTourType);
//         // _dbTourTypes.add(savedTourType);
//
//         // update viewer, resort types when necessary
//         _tourBeverageContainerViewer.update(selectedColorDefinition, SORT_PROPERTY);
//
//         _isModified = true;
//      }

      //  setFocusToViewer();
   }

   @Override
   public boolean performCancel() {

      if (_canModifyTourType) {

         fireModifyEvent();
      }

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      if (_canModifyTourType) {

      }

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      if (_canModifyTourType) {

         fireModifyEvent();
      }

      return super.performOk();
   }

   private TourBeverageContainer saveTourBeverageContainer(final TourBeverageContainer tourBeverageContainer) {

      return TourDatabase.saveEntity(
            tourBeverageContainer,
            tourBeverageContainer.getContainerId(),
            TourBeverageContainer.class);
   }

   private void setFocusToViewer() {

      // set focus back to the tree
      _tourBeverageContainerViewer.getTable().setFocus();
   }
}
