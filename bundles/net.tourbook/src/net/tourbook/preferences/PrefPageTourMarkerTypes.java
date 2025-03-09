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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.tourMarker.TourMarkerType;
import net.tourbook.tourMarker.TourMarkerTypeManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
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
   //
   private Label     _lblDescription;
   private Label     _lblName;

   //
   private Text _txtDescription;
   private Text _txtName;

   private class MapProvider_ContentProvider implements IStructuredContentProvider {

      public MapProvider_ContentProvider() {}

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

      final Composite container = createUI(parent);

      // update viewer
      _allMarkerTypes = createMapProviderClone();
      _markerTypeViewer.setInput(new Object());

      // reselect previous map provider
      restoreState();

      enableControls();

      return container;
   }

   private List<TourMarkerType> createMapProviderClone() {

      /*
       * Clone original data
       */
      final List<TourMarkerType> allClonedMarkerTypes = new ArrayList<>();

      for (final TourMarkerType markerType : TourMarkerTypeManager.getAllTourMarkerTypes()) {
         allClonedMarkerTypes.add(markerType.clone());
      }

      /*
       * Sort by name
       */
      Collections.sort(allClonedMarkerTypes, new Comparator<TourMarkerType>() {

         @Override
         public int compare(final TourMarkerType mp1, final TourMarkerType mp2) {
            return mp1.name.compareTo(mp2.name);
         }
      });

      return allClonedMarkerTypes;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

         final Label label = new Label(container, SWT.WRAP);
         label.setText("Tour marker type defines the layout of tour markers.");

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
      _markerTypeViewer.setContentProvider(new MapProvider_ContentProvider());

      _markerTypeViewer.setComparator(new ViewerComparator() {
         @Override
         public int compare(final Viewer viewer, final Object e1, final Object e2) {

            // compare by name

            final TourMarkerType p1 = (TourMarkerType) e1;
            final TourMarkerType p2 = (TourMarkerType) e2;

            return p1.name.compareTo(p2.name);
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
             * Button: Add
             */
            _btnMarkerType_Add = new Button(container, SWT.NONE);
            _btnMarkerType_Add.setText(Messages.App_Action_Add);
            _btnMarkerType_Add.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onProvider_Add(new TourMarkerType())));
            setButtonLayoutData(_btnMarkerType_Add);
         }
         {
            /*
             * Button: Delete
             */
            _btnMarkerType_Delete = new Button(container, SWT.NONE);
            _btnMarkerType_Delete.setText(Messages.App_Action_Delete);
            _btnMarkerType_Delete.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onProvider_Delete()));
            setButtonLayoutData(_btnMarkerType_Delete);
         }
         {
            /*
             * Button: Copy
             */
            _btnMarkerType_Copy = new Button(container, SWT.NONE);
            _btnMarkerType_Copy.setText(Messages.App_Action_Copy);
            _btnMarkerType_Copy.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onProvider_Copy()));
            setButtonLayoutData(_btnMarkerType_Copy);
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
            _lblName.setText("&Name");
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
            _lblDescription.setText("Description");
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
            _btnMarkerType_Update.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onProviderDetail_Update()));
            setButtonLayoutData(_btnMarkerType_Update);
         }
         {
            /*
             * Button: Cancel
             */
            _btnMarkerTypeDetail_Cancel = new Button(container, SWT.NONE);
            _btnMarkerTypeDetail_Cancel.setText(Messages.App_Action_Cancel);
            _btnMarkerTypeDetail_Cancel.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onProviderDetail_Cancel()));
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
               cell.setText(((TourMarkerType) cell.getElement()).name);
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

      _defaultModifyListener = modifyEvent -> onProvider_Modify(modifyEvent.widget);
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onProvider_Modify(selectionEvent.item));
   }

   private void initUI(final Composite parent) {

   }

   private boolean isDataValid() {

//      if (getSelectedEncoding().__isOffline) {
//
//         // validate offline map
//
//         final String mapFilePathname = _txtOffline_MapFilepath.getText().trim();
//         final String themeFilePathname = _txtOffline_ThemeFilepath.getText().trim();
//
//         final Path mapFilePath = NIO.getPath(mapFilePathname);
//         final Path themeFilePath = NIO.getPath(themeFilePathname);
//
//         if (StringUtils.isNullOrEmpty(mapFilePathname)) {
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_MapFilename_IsRequired);
//            return false;
//
//         } else if (mapFilePath == null || Files.exists(mapFilePath) == false) {
//
//            final String fileErrorText = mapFilePath == null ? UI.NULL : mapFilePath.toString();
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_MapFilename_IsNotValid + UI.SPACE + fileErrorText);
//            return false;
//
//         } else if (StringUtils.isNullOrEmpty(themeFilePathname)) {
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_ThemeFilename_IsRequired);
//            return false;
//
//         } else if (themeFilePath == null || Files.exists(themeFilePath) == false) {
//
//            final String fileErrorText = themeFilePath == null ? UI.NULL : themeFilePath.toString();
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_ThemeFilename_IsNotValid + UI.SPACE + fileErrorText);
//            return false;
//         }
//
//      } else {
//
//         // validate online map
//
//         if (StringUtils.isNullOrEmpty(_txtProviderName.getText().trim())) {
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_ProviderNameIsRequired);
//            return false;
//
//         } else if (StringUtils.isNullOrEmpty(_txtOnline_Url.getText().trim())) {
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_UrlIsRequired);
//            return false;
//
//         } else if (StringUtils.isNullOrEmpty(_txtOnline_TilePath.getText().trim())) {
//
//            setErrorMessage(Messages.Pref_Map25_Provider_Error_TilePathIsRequired);
//            return false;
//         }
//      }
//
//      /*
//       * Check that at least 1 map provider is enabled
//       */
//      final boolean isCurrentEnabled = _chkIsMapProviderEnabled.getSelection();
//      int numEnabledOtherMapProviders = 0;
//
//      for (final Map25Provider map25Provider : _allMapProvider) {
//
//         if (map25Provider.isEnabled && map25Provider != _selectedMapProvider) {
//            numEnabledOtherMapProviders++;
//         }
//      }
//
//      if (isCurrentEnabled || numEnabledOtherMapProviders > 0) {
//
//         // one map provider is enabled
//
//         setErrorMessage(null);
//         return true;
//
//      } else {
//
//         setErrorMessage(Messages.Pref_Map25_Provider_Error_EnableMapProvider);
//         return false;
//      }

      return true;
   }

   private boolean isSaveMapProvider() {

      return (MessageDialog.openQuestion(getShell(),
            Messages.Pref_Map25_Provider_Dialog_SaveModifiedProvider_Title,
            NLS.bind(
                  Messages.Pref_Map25_Provider_Dialog_SaveModifiedProvider_Message,

                  // use name from the ui because it could be modified
                  _txtName.getText())) == false);
   }

   @Override
   public boolean okToLeave() {

      if (_isMarkerTypeModified && validateData()) {

         updateModelAndUI(true);
         saveMapProviders(true);
      }

      saveState();

      return super.okToLeave();
   }

   private void onProvider_Add(final TourMarkerType markerType) {

      _newMarkerType = markerType;

//      _newProvider.isEnabled = true;

      _isModified = true;
      _isMarkerTypeModified = true;

      updateUI_FromMarkerType(_newMarkerType);
      enableControls();

      // edit name
      _txtName.setFocus();
   }

   private void onProvider_Copy() {

      final TourMarkerType clonedMapProvider = _selectedMarkerType.clone();

//      // make the clone unique
//      clonedMapProvider.setUUID();
//
//      // set a unique name
//      clonedMapProvider.name = clonedMapProvider.getId().substring(0, 4) + UI.DASH_WITH_DOUBLE_SPACE + _selectedMapProvider.name;

      onProvider_Add(clonedMapProvider);
   }

   private void onProvider_Delete() {

//    Title:     Delete Map Provider
//    Message:   Are you sure to delete the map provider "{0}" and all it's offline images?

//      if (MessageDialog.openConfirm(
//            getShell(),
//            Messages.Pref_Map25_Provider_Dialog_ConfirmDeleteMapProvider_Title,
//            NLS.bind(
//                  Messages.Pref_Map25_Provider_Dialog_ConfirmDeleteMapProvider_Message,
//                  _selectedMapProvider.name)) == false) {
//         return;
//      }
//
//      _isModified = true;
//      _isMarkerTypeModified = false;
//
//      // get map provider which will be selected when the current will be removed
//      final int selectionIndex = _mapProviderViewer.getTable().getSelectionIndex();
//      Object nextSelectedMapProvider = _mapProviderViewer.getElementAt(selectionIndex + 1);
//      if (nextSelectedMapProvider == null) {
//         nextSelectedMapProvider = _mapProviderViewer.getElementAt(selectionIndex - 1);
//      }
//
//      // delete offline files
//      deleteOfflineMapFiles(_selectedMapProvider);
//
//      // remove from model
//      _allMarkerTypes.remove(_selectedMapProvider);
//
//      // remove from viewer
//      _mapProviderViewer.remove(_selectedMapProvider);
//
//      if (nextSelectedMapProvider == null) {
//
//         _selectedMapProvider = null;
//
//         updateUI_FromProvider(_selectedMapProvider);
//
//      } else {
//
//         // select another map provider at the same position
//
//         _mapProviderViewer.setSelection(new StructuredSelection(nextSelectedMapProvider));
//         _mapProviderViewer.getTable().setFocus();
//      }

      enableControls();
   }

   private void onProvider_Modify(final Widget widget) {

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

   private void onProviderDetail_Cancel() {

      _newMarkerType = null;
      _isMarkerTypeModified = false;

      updateUI_FromMarkerType(_selectedMarkerType);
      enableControls();

      _markerTypeViewer.getTable().setFocus();
   }

   private void onProviderDetail_Update() {

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
//      isDataValid();

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
      saveMapProviders(false);

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

   /**
    * @param isAskToSave
    *
    * @return Returns <code>false</code> when map provider is not saved.
    */
   private void saveMapProviders(final boolean isAskToSave) {

      if (_isModified == false) {

         // nothing is to save
         return;
      }

      boolean isSaveIt = true;

      if (isAskToSave) {
         isSaveIt = isSaveMapProvider();
      }

      if (isSaveIt) {

//         Map25ProviderManager.saveMapProvider_WithNewModel(_allMarkerTypes);
//
//         checkMapProvider();

         _isModified = false;
      }
   }

   private void saveState() {

      if (_selectedMarkerType != null) {

         _state.put(STATE_LAST_SELECTED_MARKER_TYPE, _selectedMarkerType.id);
      }
   }

   private void selectMarkerType(final int lastMarkerTypeId) {

      TourMarkerType lastMarkerType = null;

      for (final TourMarkerType mapProvider : _allMarkerTypes) {

         if (mapProvider.id == lastMarkerTypeId) {

            lastMarkerType = mapProvider;
            break;
         }
      }

      if (lastMarkerType != null) {
         _markerTypeViewer.setSelection(new StructuredSelection(lastMarkerType));
      } else if (_allMarkerTypes.size() > 0) {
         _markerTypeViewer.setSelection(new StructuredSelection(_allMarkerTypes.get(0)));
      } else {
         // nothing can be selected
      }

      // set focus to selected map provider
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

      markerType.name = _txtName.getText();
      markerType.description = _txtDescription.getText();

   }

   private void updateUI_FromMarkerType(final TourMarkerType markerType) {

      _isInUpdateUI = true;
      {
         if (markerType == null) {

            _txtDescription.setText(UI.EMPTY_STRING);
            _txtName.setText(UI.EMPTY_STRING);

         } else {

            _txtDescription.setText(markerType.description);
            _txtName.setText(markerType.name);

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
