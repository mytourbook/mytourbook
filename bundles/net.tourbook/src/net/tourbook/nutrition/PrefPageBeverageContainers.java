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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.GraphColorPainter;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.TourTypeColorDefinition;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.tourType.TourTypeImageConfig;
import net.tourbook.tourType.TourTypeManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageBeverageContainers extends PreferencePage implements IWorkbenchPreferencePage {

   //todo fb hide the resore defaults button if possible
   public static final String          ID                 = "net.tourbook.cloud.PrefPageBeverageContainers";            //$NON-NLS-1$
   private static final String[]       SORT_PROPERTY      = new String[] { "this property is needed for sorting !!!" }; //$NON-NLS-1$

   private final IPreferenceStore      _prefStore         = TourbookPlugin.getPrefStore();

   private GraphColorPainter           _graphColorPainter;

   private ColorDefinition             _expandedItem;

   private GraphColorItem              _selectedGraphColor;
   private List<TourBeverageContainer> _dbTourTypes;

   /**
    * This is the model of the tour type viewer.
    */
   private List<TourBeverageContainer> _beverageContainers;

   private boolean                     _isModified;
   private boolean                     _isLayoutModified;

   private boolean                     _isNavigationKeyPressed;

   private boolean                     _canModifyTourType = true;

   /*
    * UI controls
    */
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

         if (index == 0) {
            return getImage(obj);
         }
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

      restoreState();
      enableActions();

      /*
       * MUST be run async otherwise the background color is NOT themed !!!
       */
      parent.getDisplay().asyncExec(() -> _tourBeverageContainerViewer.setInput(this));

      return ui;
   }

   /**
    * Create the different color names (childs) for the color definition.
    */
   private void createGraphColorItems(final ColorDefinition colorDefinition) {

      // use the first 4 color, the mapping color is not used in tour types
      final int graphNamesLength = GraphColorManager.colorNames.length - 1;

      final GraphColorItem[] graphColors = new GraphColorItem[graphNamesLength];

      for (int nameIndex = 0; nameIndex < graphNamesLength; nameIndex++) {

         graphColors[nameIndex] = new GraphColorItem(
               colorDefinition,
               GraphColorManager.colorNames[nameIndex][0],
               GraphColorManager.colorNames[nameIndex][1],
               false);
      }

      colorDefinition.setColorItems(graphColors);
   }

   private Composite createUI(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
      label.setText(Messages.Pref_TourTypes_Title);

      // container
      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_10_ColorViewer(container);
         createUI_20_Actions(container);
      }

      // must be set after the viewer is created
      return container;
   }

   private void createUI_10_ColorViewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table beverageContainersTable = new Table(parent, /* SWT.BORDER | */SWT.SINGLE | SWT.FULL_SELECTION);
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

         _isNavigationKeyPressed = false;

         switch (keyEvent.keyCode) {

         case SWT.ARROW_UP:
         case SWT.ARROW_DOWN:
            _isNavigationKeyPressed = true;
            break;

         case SWT.DEL:

            if (_btnDelete.isEnabled()) {
               onTourBeverageContainer_Delete();
            }

            break;

         case SWT.F2:

            if (_btnEdit.isEnabled()) {
               onTourType_Rename();
            }

            break;
         }
      }));

      _tourBeverageContainerViewer.addSelectionChangedListener(selectionChangedEvent -> {

         final Object selection = ((IStructuredSelection) _tourBeverageContainerViewer.getSelection()).getFirstElement();

         final boolean isNavigationKeyPressed = _isNavigationKeyPressed;

         if (_isNavigationKeyPressed) {

            // don't expand when navigation key is pressed

            _isNavigationKeyPressed = false;

         }

         onSelectColorInColorViewer(isNavigationKeyPressed);
         enableActions();
      });

//      _tourBeverageContainerViewer.addTableListener(new ITreeViewerListener() {
//
//         @Override
//         public void treeCollapsed(final TreeExpansionEvent event) {
//
//            if (event.getElement() instanceof ColorDefinition) {
//               _expandedItem = null;
//            }
//         }
//
//         @Override
//         public void treeExpanded(final TreeExpansionEvent event) {
//
//            final Object element = event.getElement();
//
//            if (element instanceof ColorDefinition) {
//               final ColorDefinition treeItem = (ColorDefinition) element;
//
//               /*
//                * run not in the treeExpand method, this is blocked by the viewer with the message:
//                * Ignored reentrant call while viewer is busy
//                */
//               display.asyncExec(() -> {
//
//                  if (_expandedItem != null) {
//                     _tourBeverageContainerViewer.collapseToLevel(_expandedItem, 1);
//                  }
//
//                  _tourBeverageContainerViewer.expandToLevel(treeItem, 1);
//                  _expandedItem = treeItem;
//               });
//            }
//         }
//      });
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
            _btnAdd.setText(Messages.Pref_TourTypes_Button_add);
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
            _btnEdit.setText(Messages.Pref_TourTypes_Button_rename);
            _btnEdit.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onTourType_Rename()));
            setButtonLayoutData(_btnEdit);
         }
         {
            /*
             * Delete
             */
            _btnDelete = new Button(container, SWT.NONE);
            _btnDelete.setText(Messages.Pref_TourTypes_Button_delete);
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
      columnServings.setWidth(25);

      // Column: Capacity
      final TableColumn columnName = new TableColumn(productsTable, SWT.LEFT);
      columnName.setText(tableColumns[index++]);
      columnName.setWidth(300);
   }

   private boolean deleteTourBeverageContainer(final TourBeverageContainer tourBeverageContainer) {

      if (deleteTourBeverageContainer_10_FromTourData(tourBeverageContainer)) {
         if (deleteTourBeverageContainer_20_FromDb(tourBeverageContainer)) {
            return true;
         }
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

               // remove tour type from all tour data
               for (final Object listItem : tourDataList) {

                  if (listItem instanceof TourData) {

                     final TourData tourData = (TourData) listItem;

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

      TourTypeImage.disposeRecreatedImages();

      super.dispose();
   }

   private void enableActions() {

      final StructuredSelection selectedItems = (StructuredSelection) _tourBeverageContainerViewer.getSelection();
      final Object firstSelectedItem = selectedItems.getFirstElement();
      final int numSelectedItems = selectedItems.size();

      boolean canDeleteColor = false;
      boolean isGraphSelected = false;

      if (firstSelectedItem instanceof GraphColorItem) {

         isGraphSelected = true;
         canDeleteColor = true;

      } else if (firstSelectedItem instanceof TourTypeColorDefinition) {

         canDeleteColor = true;
      }

      _btnDelete.setEnabled(canDeleteColor);
      _btnEdit.setEnabled(numSelectedItems == 1);
   }

   private void fireModifyEvent() {

      if (_isModified) {

         _isModified = false;

         TourTypeManager.saveState();

         TourManager.getInstance().clearTourDataCache();

         // fire modify event
         _prefStore.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());

         if (_isLayoutModified) {

            _isLayoutModified = false;

            // show restart info
            final MessageDialog messageDialog = new MessageDialog(
                  getShell(),
                  Messages.Pref_TourTypes_Dialog_Restart_Title,
                  null,
                  Messages.Pref_TourTypes_Dialog_Restart_Message_2,
                  MessageDialog.INFORMATION,
                  new String[] { Messages.App_Action_RestartApp, IDialogConstants.NO_LABEL },
                  1);

            if (messageDialog.open() == Window.OK) {
               getShell().getDisplay().asyncExec(() -> PlatformUI.getWorkbench().restart());
            }
         }
      }
   }

   /**
    * @return Returns the first selected color definition in the color viewer
    */
   private TourTypeColorDefinition getFirstSelectedColorDefinition() {

      TourTypeColorDefinition selectedColorDefinition = null;

      final Object selectedItem = ((IStructuredSelection) _tourBeverageContainerViewer.getSelection()).getFirstElement();

      if (selectedItem instanceof GraphColorItem) {

         final GraphColorItem graphColor = (GraphColorItem) selectedItem;

         selectedColorDefinition = (TourTypeColorDefinition) graphColor.getColorDefinition();

      } else if (selectedItem instanceof TourTypeColorDefinition) {

         selectedColorDefinition = (TourTypeColorDefinition) selectedItem;
      }

      return selectedColorDefinition;
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

   /**
    * Is called when a color in the color viewer is selected.
    *
    * @param isNavigationKeyPressed
    */
   private void onSelectColorInColorViewer(final boolean isNavigationKeyPressed) {

      _selectedGraphColor = null;

      final Object firstElement = _tourBeverageContainerViewer.getStructuredSelection().getFirstElement();

      if (firstElement instanceof GraphColorItem) {

         final GraphColorItem graphColor = (GraphColorItem) firstElement;

         _selectedGraphColor = graphColor;

         if (isNavigationKeyPressed == false) {

            // open color dialog only when not navigated with the keyboard

            /*
             * Run async that the UI do display the selected color in the color button when the
             * color dialog is opened
             */
            _tourBeverageContainerViewer.getTable().getDisplay().asyncExec(() -> {

               // open color selection dialog

            });
         }
      }

      setFocusToViewer();
   }

   private void onTourBeverageContainer_Add() {

      // ask for the tour type name
      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.Pref_TourTypes_Dlg_new_tour_type_title,
            Messages.Pref_TourTypes_Dlg_new_tour_type_msg,
            UI.EMPTY_STRING,
            null);

      if (inputDialog.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      final String tourTypeName = inputDialog.getValue();

      // create new tour type
      final TourBeverageContainer newTourBeverageContainer = new TourBeverageContainer(tourTypeName, 0);

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
         //_tourBeverageContainerViewer.add(this, savedTourBeverageContainer);

         _tourBeverageContainerViewer.setSelection(new StructuredSelection(savedTourBeverageContainer), true);

         _isModified = true;

         setFocusToViewer();
      }
   }

   private void onTourBeverageContainer_Delete() {

      final List<TourBeverageContainer> allSelectedColorDefinitions = _beverageContainers;

      final List<String> allTourTypeNames = new ArrayList<>();

      allSelectedColorDefinitions.stream()
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

         for (final TourBeverageContainer selectedColorDefinition : allSelectedColorDefinitions) {

            // remove entity from the db
            if (deleteTourBeverageContainer(selectedColorDefinition)) {

               // update model
               _dbTourTypes.remove(selectedColorDefinition);

               // update UI
               _tourBeverageContainerViewer.remove(selectedColorDefinition);

// a tour type image cannot be deleted otherwise an image dispose exception can occur
//             TourTypeImage.deleteTourTypeImage(selectedTourType.getTypeId());

               _isModified = true;
            }
         }
      });

      setFocusToViewer();
   }

   /**
    * This is called when the color in the color selector is modified
    *
    * @param event
    */
   private void onTourType_Modify(final PropertyChangeEvent event) {

      final RGB oldRGB = (RGB) event.getOldValue();
      final RGB newRGB = (RGB) event.getNewValue();

      if (_selectedGraphColor == null || oldRGB.equals(newRGB)) {
         return;
      }

      // color has changed

      // update model
      _selectedGraphColor.setRGB(newRGB);

      final TourTypeColorDefinition selectedColorDef = (TourTypeColorDefinition) _selectedGraphColor.getColorDefinition();

      /*
       * update tour type in the db
       */
      final TourType oldTourType = selectedColorDef.getTourType();

      oldTourType.setColor_Gradient_Bright(selectedColorDef.getGradientBright_New());
      oldTourType.setColor_Gradient_Dark(selectedColorDef.getGradientDark_New());

      oldTourType.setColor_Line(selectedColorDef.getLineColor_New_Light(), selectedColorDef.getLineColor_New_Dark());
      oldTourType.setColor_Text(selectedColorDef.getTextColor_New_Light(), selectedColorDef.getTextColor_New_Dark());

      //final TourBeverageContainer savedTourBeverageContainer = saveTourBeverageContainer(oldTourType);

      // replace tour type with new one
      _dbTourTypes.remove(oldTourType);
      //_dbTourTypes.add(savedTourType);

      /*
       * Update UI
       */
      // invalidate old color/image from the graph and color definition to force the recreation
      _graphColorPainter.invalidateResources(
            _selectedGraphColor.getColorId(),
            selectedColorDef.getColorDefinitionId());

      // update UI
      TourTypeImage.setTourTypeImagesDirty();

      _isModified = true;
   }

   private void onTourType_Rename() {

      final TourTypeColorDefinition selectedColorDefinition = getFirstSelectedColorDefinition();
      if (selectedColorDefinition == null) {
         return;
      }

      final TourType selectedTourType = selectedColorDefinition.getTourType();

      // ask for the tour type name
      final InputDialog dialog = new InputDialog(
            getShell(),
            Messages.Pref_TourTypes_Dlg_rename_tour_type_title,
            NLS.bind(Messages.Pref_TourTypes_Dlg_rename_tour_type_msg, selectedTourType.getName()),
            selectedTourType.getName(),
            null);

      if (dialog.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      // update tour type name
      final String newTourTypeName = dialog.getValue();

      selectedTourType.setName(newTourTypeName);
      selectedColorDefinition.setVisibleName(newTourTypeName);

      // update entity in the db
      final TourBeverageContainer savedTourType;/// = saveTourBeverageContainer(selectedTourType);

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

      setFocusToViewer();
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

   private void restoreState() {

      final TourTypeImageConfig imageConfig = TourTypeManager.getImageConfig();

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
