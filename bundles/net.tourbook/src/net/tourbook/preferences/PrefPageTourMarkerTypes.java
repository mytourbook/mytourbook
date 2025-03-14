/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourMarkerType;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTourMarkerTypes extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String       ID                              = "net.tourbook.preferences.PrefPageTourMarkerTypes"; //$NON-NLS-1$

   private static final String      STATE_LAST_SELECTED_MARKER_TYPE = "STATE_LAST_SELECTED_MARKER_TYPE";                  //$NON-NLS-1$

   private final IDialogSettings    _state                          = TourbookPlugin.getState(ID);
   private List<TourMarkerType>     _allMarkerTypes;
   //
   private ModifyListener           _defaultModifyListener;
   private SelectionListener        _defaultSelectionListener;
   //
   private TourMarkerType           _newMarkerType;
   private TourMarkerType           _selectedMarkerType;
   //
   private boolean                  _isModified;
   private boolean                  _isMarkerTypeModified;
   private boolean                  _isInUpdateUI;
   //
   private TableViewer              _markerTypeViewer;
   //
   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the differenct section to the same width
    */
   private final ArrayList<Control> _firstColumnControls            = new ArrayList<>();
   //
   /*
    * UI Controls
    */
   //
   private Composite _uiInnerContainer;
   //
   private Button    _btnMarkerType_Add;
   private Button    _btnMarkerType_Copy;
   private Button    _btnMarkerType_Delete;
   private Button    _btnMarkerType_Update;
   private Button    _btnMarkerTypeDetail_Cancel;
   private Button    _btnSavePerson;
   private Button    _btnCancel;
   //
   private Label     _lblDescription;
   private Label     _lblName;

   //
   private Text _txtDescription;
   private Text _txtName;

   private class ContentProvider implements IStructuredContentProvider {

      public ContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {
         return _allMarkerTypes.toArray(new TourMarkerType[_allMarkerTypes.size()]);
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

      }
   }

   @Override
   public void applyData(final Object data) {

//      if (data instanceof TourMarkerType) {
//
//         final TourMarkerType mapProvider = (TourMarkerType) data;
//
//         selectMapProvider(mapProvider.getId());
//      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      // update viewer
      _allMarkerTypes = TourDatabase.getAllTourMarkerTypes();
      _markerTypeViewer.setInput(new Object());

      // reselect previous marker type
      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

         final Label label = new Label(container, SWT.WRAP);
         label.setText("Tour &marker type defines the layout of tour markers");

         _uiInnerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_uiInnerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_uiInnerContainer);
         {
            createUI_10_Provider_Viewer(_uiInnerContainer);
            createUI_20_Provider_Actions(_uiInnerContainer);

            createUI_30_Details(_uiInnerContainer);
            createUI_90_Details_Actions(_uiInnerContainer);
         }

         // placeholder
         new Label(container, SWT.NONE);
      }

      // with e4 the layouts are not yet set -> NPE's -> run async which worked
      parent.getShell().getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            // compute width for all controls and equalize column width for the different sections
            container.layout(true, true);
            UI.setEqualizeColumWidths(_firstColumnControls);

            // this must be layouted otherwise the initial layout is not as is should be
            container.layout(true, true);
         }
      });

      return container;
   }

   private void createUI_10_Provider_Viewer(final Composite parent) {

      final TableColumnLayout tableLayout = new TableColumnLayout();

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      layoutContainer.setLayout(tableLayout);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(10))
            .applyTo(layoutContainer);

      /*
       * Create table
       */
      final Table table = new Table(
            layoutContainer,
            (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));

      table.setHeaderVisible(true);

      _markerTypeViewer = new TableViewer(table);
      defineAllColumns(tableLayout);

      _markerTypeViewer.setUseHashlookup(true);
      _markerTypeViewer.setContentProvider(new ContentProvider());

      _markerTypeViewer.setComparator(new ViewerComparator() {
         @Override
         public int compare(final Viewer viewer, final Object e1, final Object e2) {

            // compare/sort by name

            final TourMarkerType p1 = (TourMarkerType) e1;
            final TourMarkerType p2 = (TourMarkerType) e2;

            return p1.getName().compareTo(p2.getName());
         }
      });

      _markerTypeViewer.addSelectionChangedListener(selectionChangedEvent -> onSelect_MarkerType());

      _markerTypeViewer.addDoubleClickListener(doubleClickEvent -> {

         _txtName.setFocus();
         _txtName.selectAll();
      });

   }

   private void createUI_20_Provider_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Button: Add...
             */
            _btnMarkerType_Add = new Button(container, SWT.NONE);
            _btnMarkerType_Add.setText(OtherMessages.APP_ACTION_ADD_WITH_CONFIRM);
            _btnMarkerType_Add.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_New()));
            setButtonLayoutData(_btnMarkerType_Add);
         }
         {
            /*
             * Button: Delete...
             */
            _btnMarkerType_Delete = new Button(container, SWT.NONE);
            _btnMarkerType_Delete.setText(Messages.App_Action_Delete_WithConfirm);
            _btnMarkerType_Delete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Delete()));
            setButtonLayoutData(_btnMarkerType_Delete);
         }
         {
            /*
             * Button: Save
             */
            _btnSavePerson = new Button(container, SWT.NONE);
            _btnSavePerson.setText(Messages.App_Action_Save);
            _btnSavePerson.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Save()));
            setButtonLayoutData(_btnSavePerson);
            final GridData gd = (GridData) _btnSavePerson.getLayoutData();
            gd.verticalAlignment = SWT.BOTTOM;
            gd.grabExcessVerticalSpace = true;
         }
         {
            /*
             * Button: Cancel
             */
            _btnCancel = new Button(container, SWT.NONE);
            _btnCancel.setText(Messages.App_Action_Cancel);
            _btnCancel.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Cancel()));
            setButtonLayoutData(_btnCancel);
         }
      }
   }

   private void createUI_30_Details(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         {
            /*
             * Field: Marker type name
             */
            _lblName = new Label(container, SWT.NONE);
            _lblName.setText("Na&me");
            _firstColumnControls.add(_lblName);

            _txtName = new Text(container, SWT.BORDER);
            _txtName.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_txtName);
         }
         {
            /*
             * Field: Marker type description
             */
            _lblDescription = new Label(container, SWT.NONE);
            _lblDescription.setText("Descri&ption");
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_lblDescription);
            _firstColumnControls.add(_lblDescription);

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
            _txtDescription.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults()
                  .hint(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(4))
                  .grab(true, false)
                  .applyTo(_txtDescription);
         }
      }

   }

   private void createUI_90_Details_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Button: Update
             */
            _btnMarkerType_Update = new Button(container, SWT.NONE);
            // !!! set initially the longest text that the layout is properly !!!
            _btnMarkerType_Update.setText(Messages.App_Action_UpdateNew);
            _btnMarkerType_Update.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerTypeDetail_Update()));
            setButtonLayoutData(_btnMarkerType_Update);
         }
         {
            /*
             * Button: Cancel
             */
            _btnMarkerTypeDetail_Cancel = new Button(container, SWT.NONE);
            _btnMarkerTypeDetail_Cancel.setText(Messages.App_Action_Cancel);
            _btnMarkerTypeDetail_Cancel.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerTypeDetail_Cancel()));
            setButtonLayoutData(_btnMarkerTypeDetail_Cancel);
         }
      }
   }

   private void defineAllColumns(final TableColumnLayout tableLayout) {

      final int minWidth = convertWidthInCharsToPixels(5);

      TableViewerColumn tvc;
      TableColumn tc;

      {
         /*
          * Column: Marker type name
          */
         tvc = new TableViewerColumn(_markerTypeViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText("Name");
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {
               cell.setText(((TourMarkerType) cell.getElement()).getName());
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(5, minWidth));
      }
   }

   @Override
   public void dispose() {

      _firstColumnControls.clear();

      super.dispose();
   }

   private void enableControls() {

   }

   @Override
   public void init(final IWorkbench workbench) {

      noDefaultAndApplyButton();
   }

   private void initUI(final Composite parent) {

      _defaultModifyListener = modifyEvent -> onMarkerType_Modify(modifyEvent.widget);
      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Modify(selectionEvent.item));
   }

   private boolean isDataValid() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean okToLeave() {

      if (_isMarkerTypeModified && validateData()) {

         updateModelAndUI(true);
      }

      saveState();

      return super.okToLeave();
   }

   private void onMarkerType_Cancel() {
      // TODO Auto-generated method stub
   }

   private void onMarkerType_Delete() {

      enableControls();
   }

   private void onMarkerType_Modify(final Widget widget) {

      if (_isInUpdateUI) {
         return;
      }

      if (widget != null && UI.isLinuxAsyncEvent(widget)) {
         return;
      }

      _isModified = true;
      _isMarkerTypeModified = true;

      enableControls();
   }

   private void onMarkerType_New() {

      _newMarkerType = markerType;

      _isModified = true;
      _isMarkerTypeModified = true;

      updateUI_FromMarkerType(_newMarkerType);
      enableControls();

      // edit name
      _txtName.setFocus();
   }

   private void onMarkerType_Save() {
      // TODO Auto-generated method stub
   }

   private void onMarkerTypeDetail_Cancel() {

      _newMarkerType = null;
      _isMarkerTypeModified = false;

      updateUI_FromMarkerType(_selectedMarkerType);
      enableControls();

      _markerTypeViewer.getTable().setFocus();
   }

   private void onMarkerTypeDetail_Update() {

      final boolean isValid = validateData();

      if (isValid) {

         updateModelAndUI(true);
         enableControls();

      } else {

         updateModelAndUI(false);
         enableControls();
      }

      _markerTypeViewer.getTable().setFocus();
   }

   private void onSelect_MarkerType() {

      final IStructuredSelection selection = (IStructuredSelection) _markerTypeViewer.getSelection();
      final TourMarkerType mapProvider = (TourMarkerType) selection.getFirstElement();

      if (mapProvider != null) {

         _selectedMarkerType = mapProvider;

         updateUI_FromMarkerType(_selectedMarkerType);

      } else {

         // irgnore, this can happen when a refresh() of the table viewer is done
      }

      // show error message when selected map provider is not valid
      isDataValid();

      enableControls();
   }

   @Override
   public boolean performCancel() {

      saveState();

      return super.performCancel();
   }

   @Override
   public boolean performOk() {

      updateModelAndUI(false);

      saveState();

      return true;
   }

   private void restoreState() {

      /*
       * Select last selected marker type
       */
      final int lastMarkerType = Util.getStateInt(_state, STATE_LAST_SELECTED_MARKER_TYPE, -1);

      selectMarkerType(lastMarkerType);
   }

   private TourMarkerType saveMarkerType(final TourMarkerType tourMarkerType) {

      return TourDatabase.saveEntity(
            tourMarkerType,
            tourMarkerType.getId(),
            TourMarkerType.class);
   }

   private void saveState() {

      if (_selectedMarkerType != null) {

         _state.put(STATE_LAST_SELECTED_MARKER_TYPE, _selectedMarkerType.getId());
      }
   }

   private void selectMarkerType(final int lastMarkerTypeId) {

      TourMarkerType lastMarkerType = null;

      for (final TourMarkerType mapProvider : _allMarkerTypes) {

         if (mapProvider.getId() == lastMarkerTypeId) {

            lastMarkerType = mapProvider;
            break;
         }
      }

      if (lastMarkerType != null) {

         _markerTypeViewer.setSelection(new StructuredSelection(lastMarkerType));

      } else if (_allMarkerTypes.size() > 0) {

         // select first marker type

         _markerTypeViewer.setSelection(new StructuredSelection(_allMarkerTypes.get(0)));

      } else {

         // nothing can be selected
      }

      // set focus to selected marker type
      final Table table = _markerTypeViewer.getTable();
      table.setSelection(table.getSelectionIndex());
   }

   /**
    * @param IsCheckValidation
    *           When <code>false</code> then do not check validation. This is used that invalid map
    *           provider can be saved, e.g. when they are invalid but disabled.
    */
   private void updateModelAndUI(final boolean IsCheckValidation) {

      final boolean isNewMarkerType = _newMarkerType != null;
      final TourMarkerType currentMarkerType = isNewMarkerType ? _newMarkerType : _selectedMarkerType;

      final boolean isValid = IsCheckValidation ? validateData() : true;

      if (_isMarkerTypeModified && isValid) {

         updateModelData(currentMarkerType);

         // update ui
         if (isNewMarkerType) {
            _allMarkerTypes.add(currentMarkerType);
            _markerTypeViewer.add(currentMarkerType);

         } else {
            // !!! refreshing a map provider do not resort the table when sorting has changed so we refresh the viewer !!!
            _markerTypeViewer.refresh();
         }

         // select updated/new map provider
         _markerTypeViewer.setSelection(new StructuredSelection(currentMarkerType), true);
      }

      // update state
      _isMarkerTypeModified = false;
      _newMarkerType = null;
   }

   /**
    * Update map provider
    */
   private void updateModelData(final TourMarkerType markerType) {

      markerType.setName(_txtName.getText());
      markerType.setDescription(_txtDescription.getText());

   }

   private void updateUI_FromMarkerType(final TourMarkerType markerType) {

      _isInUpdateUI = true;
      {
         if (markerType == null) {

            _txtDescription.setText(UI.EMPTY_STRING);
            _txtName.setText(UI.EMPTY_STRING);

         } else {

            _txtDescription.setText(markerType.getDescription());
            _txtName.setText(markerType.getName());

         }
      }
      _isInUpdateUI = false;
   }

   /**
    * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
    */
   private boolean validateData() {

      final boolean isNewProvider = _newMarkerType != null;

      if (isNewProvider || _isMarkerTypeModified) {

         return isDataValid();
      }

      setErrorMessage(null);

      return true;
   }

}
