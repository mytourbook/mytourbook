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
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for map models
 */
public class SlideoutMapModel extends ToolbarSlideout {

   private static final String NUMBER_OF_VISIBLE_MODEL_ITEMS   = "NUMBER_OF_VISIBLE_MODEL_ITEMS";//$NON-NLS-1$

   private static final int    NUM_VISIBLE_MODEL_ITEMS_DEFAULT = 20;
   private static final int    NUM_VISIBLE_MODEL_ITEMS_MIN     = 3;
   private static final int    NUM_VISIBLE_MODEL_ITEMS_MAX     = 100;

   private TableViewer         _modelViewer;

//   private SelectionListener  _defaultSelectionListener;
//   private MouseWheelListener _defaultMouseWheelListener;

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Composite       _parent;

   private Button          _btnAdd;
   private Button          _btnDelete;
   private Button          _btnEdit;

   private Spinner         _spinnerNumModelItems;

   private Composite       _viewerLayoutContainer;
   private IDialogSettings _state;
   private int             _numVisibleModelItems;

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
   public SlideoutMapModel(final Control ownerControl,
                           final ToolBar toolBar,
                           final IDialogSettings state) {

      super(ownerControl, toolBar);

      _state = state;
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createActions();

      restoreState_BeforeUI();

      final Composite ui = createUI(parent);

      restoreState();

      // fill viewer
      _modelViewer.setInput(new Object());

      enableActions();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);

            createUI_50_ModelViewer(container);
            createUI_60_ModelActions(container);

            createUI_70_Options(container);

         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_MapModel_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_50_ModelViewer(final Composite parent) {

      _viewerLayoutContainer = new Composite(parent, SWT.NONE);
      updateUI_ViewerContainerLayout(_viewerLayoutContainer);

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
            onModel_Select();
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

   private void createUI_60_ModelActions(final Composite parent) {

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

   private void createUI_70_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .indent(0, 10)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_74_Options_NumItems(container);
      }
   }

   private void createUI_74_Options_NumItems(final Composite parent) {

      {
         /*
          * Number of model list entries
          */

         // Label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Slideout_MapModel_Label_NumModelListItems);
         label.setToolTipText(Messages.Slideout_MapModel_Label_NumModelListItems_Tooltip);

         // Spinner
         _spinnerNumModelItems = new Spinner(parent, SWT.BORDER);
         _spinnerNumModelItems.setMinimum(NUM_VISIBLE_MODEL_ITEMS_MIN);
         _spinnerNumModelItems.setMaximum(NUM_VISIBLE_MODEL_ITEMS_MAX);
         _spinnerNumModelItems.setPageIncrement(5);

         _spinnerNumModelItems.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onChangeUI_ViewerLayout()));
         _spinnerNumModelItems.addMouseWheelListener(mouseEvent -> {

            UI.adjustSpinnerValueOnMouseScroll(mouseEvent);

            onChangeUI_ViewerLayout();
         });
      }
   }

   private void enableActions() {

      final MapModel selectedModel = getSelectedModel();

      final boolean isModelSelected = selectedModel != null;

      _btnAdd.setEnabled(true);
      _btnDelete.setEnabled(isModelSelected);
      _btnEdit.setEnabled(isModelSelected);
   }

   private MapModel getSelectedModel() {

      final IStructuredSelection selection = (IStructuredSelection) _modelViewer.getSelection();
      final MapModel selectedModel = (MapModel) selection.getFirstElement();

      return selectedModel;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

//      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
//
//      _defaultMouseWheelListener = mouseEvent -> {
//
//         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
//
//         onChangeUI();;
//      };
   }

   private void onChangeUI() {

      saveState();

      enableActions();
   }

   private void onChangeUI_ViewerLayout() {

      onChangeUI();

      updateUI_ViewerContainerLayout(_viewerLayoutContainer);

      // set slideout size with new visible rows
      _parent.getShell().pack(true);
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

//      final MapModel selectedModel = getSelectedModel();
//
//      if (selectedModel == null) {
//         return;
//      }
//
//      // update model
//      MapModelManager.onDeleteModel(selectedModel);
//
//      // update UI
//      _modelViewer.refresh();
//
//      enableActions();
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

   private void onModel_Select() {

      final MapModel selectedModel = getSelectedModel();

      if (selectedModel == null) {

         // this happened when deleting a model
         return;
      }

      MapModelManager.setSelectedModel(selectedModel);

      enableActions();
   }

   private void restoreState() {

      _spinnerNumModelItems.setSelection(_numVisibleModelItems);
   }

   private void restoreState_BeforeUI() {

      _numVisibleModelItems = Util.getStateInt(_state, NUMBER_OF_VISIBLE_MODEL_ITEMS, NUM_VISIBLE_MODEL_ITEMS_DEFAULT);
   }

   private void saveState() {

      _numVisibleModelItems = _spinnerNumModelItems.getSelection();

      _state.put(NUMBER_OF_VISIBLE_MODEL_ITEMS, _numVisibleModelItems);
   }

   private void updateUI_ModelViewer(final MapModel selectedModel) {

      // update UI
      _modelViewer.refresh();

      // reselect model
      _modelViewer.setSelection(new StructuredSelection(selectedModel), true);
   }

   private void updateUI_ViewerContainerLayout(final Composite viewerLayoutContainer) {

      GridDataFactory.fillDefaults()

            .hint(
                  _pc.convertWidthInCharsToPixels(50),
                  _pc.convertHeightInCharsToPixels((int) (_numVisibleModelItems * 1.4)))

            .applyTo(viewerLayoutContainer);
   }

}
