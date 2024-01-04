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
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
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

   private boolean                     _isModified;
   private boolean                     _isLayoutModified;

   private boolean                     _isNavigationKeyPressed;

   private boolean                     _canModifyTourType = true;

   /*
    * UI controls
    */
   private TreeViewer _tourBeverageContainerViewer;

   private Button     _btnAdd;
   private Button     _btnDelete;
   private Button     _btnEdit;

   private class ColorDefinitionContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {
         // Nothing to do
      }

      @Override
      public Object[] getChildren(final Object parentElement) {

         if (parentElement instanceof final ColorDefinition colorDefinition) {

            return colorDefinition.getGraphColorItems();
         }

         return new Object[] {};
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return new String[] {};
      }

      @Override
      public Object getParent(final Object element) {
         return null;
      }

      @Override
      public boolean hasChildren(final Object element) {

         return element instanceof ColorDefinition;
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
         // Nothing to do
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

      if (_dbTourTypes != null) {

         for (final TourBeverageContainer tourType : _dbTourTypes) {

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

      final Display display = parent.getDisplay();

      /*
       * create tree layout
       */
      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(400, 600)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * create viewer
       */
      final Tree tree = new Tree(
            layoutContainer,
            SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.BORDER
                  | SWT.MULTI
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(false);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourBeverageContainerViewer = new TreeViewer(tree);
      defineAllColumns(treeLayout, tree);

      _tourBeverageContainerViewer.setContentProvider(new ColorDefinitionContentProvider());

      _tourBeverageContainerViewer.setUseHashlookup(true);

      _tourBeverageContainerViewer.getTree().addKeyListener(keyPressedAdapter(keyEvent -> {

         _isNavigationKeyPressed = false;

         switch (keyEvent.keyCode) {

         case SWT.ARROW_UP:
         case SWT.ARROW_DOWN:
            _isNavigationKeyPressed = true;
            break;

         case SWT.DEL:

            if (_btnDelete.isEnabled()) {
               onTourType_Delete();
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

         } else {

            // expand/collapse tree item

            if (selection instanceof ColorDefinition) {

               // expand/collapse current item
               final ColorDefinition treeItem = (ColorDefinition) selection;

               if (_tourBeverageContainerViewer.getExpandedState(treeItem)) {

                  // item is expanded -> collapse

                  _tourBeverageContainerViewer.collapseToLevel(treeItem, 1);

               } else {

                  // item is collapsed -> expand

                  if (_expandedItem != null) {
                     _tourBeverageContainerViewer.collapseToLevel(_expandedItem, 1);
                  }
                  _tourBeverageContainerViewer.expandToLevel(treeItem, 1);
                  _expandedItem = treeItem;

                  // expanding the triangle, the layout is correctly done but not with double click
                  layoutContainer.layout(true, true);
               }
            }
         }

         onSelectColorInColorViewer(isNavigationKeyPressed);
         enableActions();
      });

      _tourBeverageContainerViewer.addTreeListener(new ITreeViewerListener() {

         @Override
         public void treeCollapsed(final TreeExpansionEvent event) {

            if (event.getElement() instanceof ColorDefinition) {
               _expandedItem = null;
            }
         }

         @Override
         public void treeExpanded(final TreeExpansionEvent event) {

            final Object element = event.getElement();

            if (element instanceof ColorDefinition) {
               final ColorDefinition treeItem = (ColorDefinition) element;

               /*
                * run not in the treeExpand method, this is blocked by the viewer with the message:
                * Ignored reentrant call while viewer is busy
                */
               display.asyncExec(() -> {

                  if (_expandedItem != null) {
                     _tourBeverageContainerViewer.collapseToLevel(_expandedItem, 1);
                  }

                  _tourBeverageContainerViewer.expandToLevel(treeItem, 1);
                  _expandedItem = treeItem;
               });
            }
         }
      });
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
               onTourType_Add();
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
               onTourType_Delete();
               enableActions();
            }));
            setButtonLayoutData(_btnDelete);
         }
      }
   }

   /**
    * Create columns
    */
   private void defineAllColumns(final TreeColumnLayout treeLayout, final Tree tree) {

      defineColumn_10_TourTypeImage(treeLayout);
      defineColumn_20_UpdatedTourTypeImage(treeLayout);
   }

   private void defineColumn_10_TourTypeImage(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourBeverageContainerViewer, SWT.LEAD);
      final TreeColumn tc = tvc.getColumn();
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               cell.setText(((TourTypeColorDefinition) (element)).getVisibleName());

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());

               cell.setImage(tourTypeImage);

            } else if (element instanceof GraphColorItem) {

               cell.setText(((GraphColorItem) (element)).getName());
               cell.setImage(null);

            } else {

               cell.setText(UI.EMPTY_STRING);
               cell.setImage(null);
            }
         }
      });

      treeLayout.setColumnData(tc, new ColumnWeightData(15, true));
   }

   /**
    * Color definition with fully updated tour type image
    */
   private void defineColumn_20_UpdatedTourTypeImage(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourBeverageContainerViewer, SWT.LEAD);
      final TreeColumn tc = tvc.getColumn();
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage_New(tourType.getTypeId());

               cell.setImage(tourTypeImage);

            } else {

               cell.setImage(null);
            }
         }
      });
      treeLayout.setColumnData(tc, new ColumnWeightData(3, true));
   }

   private boolean deleteTourType(final TourType tourType) {

      if (deleteTourType_10_FromTourData(tourType)) {
         if (deleteTourType_20_FromDb(tourType)) {
            return true;
         }
      }

      return false;
   }

   private boolean deleteTourType_10_FromTourData(final TourType tourType) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em != null) {

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT tourData" //$NON-NLS-1$
               + " FROM TourData AS tourData" //$NON-NLS-1$
               + " WHERE tourData.tourType.typeId=" + tourType.getTypeId()); //$NON-NLS-1$

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

   private boolean deleteTourType_20_FromDb(final TourType tourType) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {
         final TourType tourTypeEntity = em.find(TourType.class, tourType.getTypeId());

         if (tourTypeEntity != null) {

            ts.begin();

            em.remove(tourTypeEntity);

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

   /**
    * @return Returns the selected color definitions in the color viewer
    */
   private List<TourTypeColorDefinition> getSelectedColorDefinitions() {

      final List<TourTypeColorDefinition> allSelectedColorDefinitions = new ArrayList<>();

      final StructuredSelection allSelectedItems = (StructuredSelection) _tourBeverageContainerViewer.getSelection();

      for (final Object selectedItem : allSelectedItems) {

         if (selectedItem instanceof GraphColorItem) {

            final GraphColorItem graphColor = (GraphColorItem) selectedItem;

            allSelectedColorDefinitions.add((TourTypeColorDefinition) graphColor.getColorDefinition());

         } else if (selectedItem instanceof TourTypeColorDefinition) {

            allSelectedColorDefinitions.add((TourTypeColorDefinition) selectedItem);
         }
      }

      return allSelectedColorDefinitions;
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
            _tourBeverageContainerViewer.getTree().getDisplay().asyncExec(() -> {

               // open color selection dialog

            });
         }
      }

      setFocusToViewer();
   }

   private void onTourType_Add() {

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
      final TourType newTourType = new TourType(tourTypeName);

      /*
       * Create a dummy definition to get the default colors
       */
      final TourTypeColorDefinition dummyColorDef = new TourTypeColorDefinition(
            newTourType,
            UI.EMPTY_STRING,
            UI.EMPTY_STRING);

      newTourType.setColor_Gradient_Bright(dummyColorDef.getGradientBright_Default());
      newTourType.setColor_Gradient_Dark(dummyColorDef.getGradientDark_Default());

      newTourType.setColor_Line(dummyColorDef.getLineColor_Default_Light(), dummyColorDef.getLineColor_Default_Dark());
      newTourType.setColor_Text(dummyColorDef.getTextColor_Default_Light(), dummyColorDef.getTextColor_Default_Dark());

      // add new entity to db
      final TourType savedTourType = saveTourType(newTourType);

      if (savedTourType != null) {

         /*
          * Create a color definition WITH THE SAVED tour type, this fixes a VEEEEEEEEEEERY long
          * existing bug that a new tour type is initially not displayed correctly in the color
          * definition image.
          */
         // create the same color definition but with the correct id's
         final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
               savedTourType,
               "tourType." + savedTourType.getTypeId(), //$NON-NLS-1$
               tourTypeName);

         // overwrite tour type object
         newColorDefinition.setTourType(savedTourType);

         createGraphColorItems(newColorDefinition);

         // update internal tour type list
         //_dbTourTypes.add(savedTourType);

         // update UI
         _tourBeverageContainerViewer.add(this, newColorDefinition);

         _tourBeverageContainerViewer.setSelection(new StructuredSelection(newColorDefinition), true);

         _tourBeverageContainerViewer.collapseAll();
         _tourBeverageContainerViewer.expandToLevel(newColorDefinition, 1);

         _isModified = true;

         setFocusToViewer();
      }
   }

   private void onTourType_Delete() {

      final List<TourTypeColorDefinition> allSelectedColorDefinitions = getSelectedColorDefinitions();
      final List<TourType> allSelectedTourTypes = new ArrayList<>();

      allSelectedColorDefinitions.stream()
            .forEach(colorDefinition -> allSelectedTourTypes.add(colorDefinition.getTourType()));

      final List<String> allTourTypeNames = new ArrayList<>();

      allSelectedTourTypes.stream()
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

         for (final TourTypeColorDefinition selectedColorDefinition : allSelectedColorDefinitions) {

            final TourType selectedTourType = selectedColorDefinition.getTourType();

            // remove entity from the db
            if (deleteTourType(selectedTourType)) {

               // update model
               _dbTourTypes.remove(selectedTourType);

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

      final TourType savedTourType = saveTourType(oldTourType);

      selectedColorDef.setTourType(savedTourType);

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

      /*
       * update the tree viewer, the color images will be recreated in the label provider
       */
      _tourBeverageContainerViewer.update(_selectedGraphColor, null);
      _tourBeverageContainerViewer.update(selectedColorDef, null);

      // without a repaint the color def image is not updated
      _tourBeverageContainerViewer.getTree().redraw();

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
      final TourType savedTourType = saveTourType(selectedTourType);

      if (savedTourType != null) {

         // update model
         selectedColorDefinition.setTourType(savedTourType);

         // replace tour type with new one
         _dbTourTypes.remove(selectedTourType);
         // _dbTourTypes.add(savedTourType);

         // update viewer, resort types when necessary
         _tourBeverageContainerViewer.update(selectedColorDefinition, SORT_PROPERTY);

         _isModified = true;
      }

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

   private TourType saveTourType(final TourType tourType) {

      return TourDatabase.saveEntity(
            tourType,
            tourType.getTypeId(),
            TourType.class);
   }

   private void setFocusToViewer() {

      // set focus back to the tree
      _tourBeverageContainerViewer.getTree().setFocus();
   }
}
