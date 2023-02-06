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
package net.tourbook.map.model;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.map25.Map25FPSManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for map models
 */
public class SlideoutMapModel extends AdvancedSlideout {

   private TableViewer    _modelViewer;

   private boolean        _isInUpdateUI;

   private ToolItem       _toolItem;

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerLayoutContainer;

   private Button    _btnAdd;
   private Button    _btnDelete;
   private Button    _btnEdit;

   private class ModelComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final MapModel model1 = (MapModel) e1;
         final MapModel model2 = (MapModel) e2;

         return model1.name.compareTo(model2.name);
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }
   }

   private class ModelProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return MapModelManager.getAllModels().toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param state
    */
   public SlideoutMapModel(final ToolItem toolItem,
                           final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 300, 200 });

      _toolItem = toolItem;

      setTitleText(Messages.Slideout_MapModel_Label_Title);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createUI(parent);

      // fill viewer
      _modelViewer.setInput(new Object());

      // ensure that the animation is running when map is in background
      Map25FPSManager.setBackgroundFPSToAnimationFPS(true);

      restoreState();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         createUI_20_ModelViewer(container);
         createUI_30_ModelActions(container);
      }
   }

   private void createUI_20_ModelViewer(final Composite parent) {

      _viewerLayoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(_pc.convertWidthInCharsToPixels(50), _pc.convertHeightInCharsToPixels(10))
            .applyTo(_viewerLayoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      _viewerLayoutContainer.setLayout(tableLayout);

      /*
       * Create table
       */
      final Table table = new Table(_viewerLayoutContainer, SWT.FULL_SELECTION);

      table.setLayout(new TableLayout());
      table.setHeaderVisible(true);

      _modelViewer = new TableViewer(table);

      /*
       * Create columns
       */
      TableViewerColumn tvc;
      TableColumn tc;

      {
         // Column: Model name

         tvc = new TableViewerColumn(_modelViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_MapModel_Column_Name);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final MapModel model = (MapModel) cell.getElement();

               cell.setText(model.name);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
      }

      /*
       * create table viewer
       */
      _modelViewer.setContentProvider(new ModelProvider());
      _modelViewer.setComparator(new ModelComparator());

      _modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {

         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onModel_Selected();
         }
      });

      _modelViewer.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {
            onModel_Edit(true);
         }
      });

      _modelViewer.getTable().addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent e) {

            switch (e.keyCode) {

            case SWT.DEL:
               onModel_Delete();
               break;

            case SWT.F2:
               onModel_Edit(false);
               break;

            default:
               break;
            }
         }

         @Override
         public void keyReleased(final KeyEvent e) {}
      });
   }

   private void createUI_30_ModelActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Button: Add
             */
            _btnAdd = new Button(container, SWT.PUSH);
            _btnAdd.setText(Messages.App_Action_Add);
            _btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModel_Add()));

            // set button default width
            UI.setButtonLayoutData(_btnAdd);
         }
         {
            /*
             * Button: Edit
             */
            _btnEdit = new Button(container, SWT.PUSH);
            _btnEdit.setText(Messages.App_Action_Edit);
            _btnEdit.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModel_Edit(false)));

            // set button default width
            UI.setButtonLayoutData(_btnEdit);
         }
         {
            /*
             * Button: Delete
             */
            _btnDelete = new Button(container, SWT.PUSH);
            _btnDelete.setText(Messages.App_Action_Delete);
            _btnDelete.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModel_Delete()));

            // set button default width
            UI.setButtonLayoutData(_btnDelete);
         }
      }
   }

   private void enableActions() {

      final MapModel selectedModel = getSelectedModel();

      final boolean isModelSelected = selectedModel != null;
      final boolean isDefaultModel = isModelSelected && selectedModel.isDefaultModel;
      final boolean isUserModel = isDefaultModel == false;

      _btnAdd.setEnabled(true);
      _btnDelete.setEnabled(isModelSelected && isUserModel);
      _btnEdit.setEnabled(isModelSelected);
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private MapModel getSelectedModel() {

      final IStructuredSelection selection = (IStructuredSelection) _modelViewer.getSelection();
      final MapModel selectedModel = (MapModel) selection.getFirstElement();

      return selectedModel;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   protected void onDispose() {

      // reset to default background FPS
      Map25FPSManager.setBackgroundFPSToAnimationFPS(false);
   }

   @Override
   protected void onFocus() {

      Map25FPSManager.setBackgroundFPSToAnimationFPS(true);

      _modelViewer.getTable().setFocus();
   }

   private void onModel_Add() {

      int dialogReturnCode;
      DialogMapModel dialogMapModel;

      setIsAnotherDialogOpened(true);
      {
         dialogMapModel = new DialogMapModel(_parent.getShell());

         dialogReturnCode = dialogMapModel.open();
      }
      setIsAnotherDialogOpened(false);

      if (dialogReturnCode != IDialogConstants.OK_ID) {
         return;
      }

      final MapModel newMapModel = dialogMapModel.getNewMapModel();

      MapModelManager.getAllModels().add(newMapModel);

      updateUI_ModelViewer(newMapModel);
   }

   private void onModel_Delete() {

      final MapModel selectedModel = getSelectedModel();

      setIsAnotherDialogOpened(true);
      int returnCode;
      {
         returnCode = new MessageDialog(
               getToolTipShell(),
               Messages.Slideout_MapModel_Dialog_DeleteModel_Title,

               null, // image

               String.format(Messages.Slideout_MapModel_Dialog_DeleteModel_Message, selectedModel.name),
               MessageDialog.QUESTION,

               0, // default index

               Messages.App_Action_Delete,
               IDialogConstants.CANCEL_LABEL

         ).open();

      }
      setIsAnotherDialogOpened(true);

      if (returnCode != Window.OK) {
         return;
      }

      // get map model which will be selected when the current will be removed
      final int selectionIndex = _modelViewer.getTable().getSelectionIndex();
      Object nextSelectedMapModel = _modelViewer.getElementAt(selectionIndex + 1);
      if (nextSelectedMapModel == null) {
         nextSelectedMapModel = _modelViewer.getElementAt(selectionIndex - 1);
      }

      // update model
      MapModelManager.getAllModels().remove(selectedModel);

      // update UI
      _modelViewer.remove(selectedModel);

      // select another map model at the same position
      if (nextSelectedMapModel != null) {

         _modelViewer.setSelection(new StructuredSelection(nextSelectedMapModel));

         // set focus back to the viewer
         _modelViewer.getTable().setFocus();
      }
   }

   private void onModel_Edit(final boolean isOpenedWithMouse) {

      final MapModel selectedModel = getSelectedModel();

      if (selectedModel == null) {
         return;
      }

      int dialogReturnCode;
      setIsAnotherDialogOpened(true);
      {
         final DialogMapModel dialogMapModel = new DialogMapModel(_parent.getShell());

         dialogMapModel.setMapModel(selectedModel);
         dialogReturnCode = dialogMapModel.open();
      }
      setIsAnotherDialogOpened(false);

      // set focus back to the viewer
      _modelViewer.getTable().setFocus();

      if (dialogReturnCode != IDialogConstants.OK_ID) {
         return;
      }

      updateUI_ModelViewer(selectedModel);
   }

   private void onModel_Selected() {

      if (_isInUpdateUI) {
         return;
      }

      final MapModel selectedModel = getSelectedModel();

      if (selectedModel == null) {

         // this happened when deleting a model
         return;
      }

      MapModelManager.setSelectedModel(selectedModel);

      enableActions();

      Map25FPSManager.setBackgroundFPSToAnimationFPS(true);
   }

   private void restoreState() {

      _isInUpdateUI = true;
      {
         updateUI_ModelViewer(MapModelManager.getSelectedModel());
      }
      _isInUpdateUI = false;

      enableActions();
   }

   @Override
   protected void saveState() {

      // save slideout position/size
      super.saveState();
   }

   private void updateUI_ModelViewer(final MapModel selectedModel) {

      // update UI
      _modelViewer.refresh();

      // reselect model
      _modelViewer.setSelection(new StructuredSelection(selectedModel), true);
   }

}
