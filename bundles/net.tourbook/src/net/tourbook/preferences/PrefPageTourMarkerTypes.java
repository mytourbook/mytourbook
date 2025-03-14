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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarkerType;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
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
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTourMarkerTypes extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID                              = "net.tourbook.preferences.PrefPageTourMarkerTypes"; //$NON-NLS-1$

   private static final String           STATE_LAST_SELECTED_MARKER_TYPE = "STATE_LAST_SELECTED_MARKER_TYPE";                  //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                      = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                          = TourbookPlugin.getState(ID);
   //
   private List<TourMarkerType>          _allMarkerTypes;
   //
   private ModifyListener                _defaultModifyListener;
   private SelectionListener             _defaultSelectionListener;
   private IPropertyChangeListener       _prefChangeListener;
   //
   private TourMarkerType                _newMarkerType;
   private TourMarkerType                _selectedMarkerType;
   //
   private boolean                       _isFireModifyEvent;
   private boolean                       _isInUpdateUI;
   private boolean                       _isModified;
   //
   /**
    * Is <code>true</code> when a tour in the tour editor is modified.
    */
   private boolean                       _isNoUI                         = false;
   //
   private TableViewer                   _markerTypeViewer;
   //
   /*
    * UI Controls
    */
   //
   //
   private Button _btnMarkerType_Add;
   private Button _btnMarkerType_Copy;
   private Button _btnMarkerType_Delete;
   private Button _btnMarkerType_Update;
   private Button _btnMarkerTypeDetail_Cancel;
   private Button _btnMarkerType_Save;
   private Button _btnMarkerType_Cancel;
   //
   private Label  _lblDescription;
   private Label  _lblName;

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
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         /*
          * set a new chart configuration when the preferences has changed
          */
//         if (property.equals(ITourbookPreferences.HR_ZONES_ARE_MODIFIED)) {
//
//            onEditHrZonesIsOK(getCurrentPerson());
//            performOK10();
//         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
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

      // check: if a tour is modified in the tour editor
      if (TourManager.isTourEditorModified()) {

         _isNoUI = true;

         return createUI_01_NoUI(parent);
      }

      initUI(parent);

      final Composite container = createUI(parent);

      // update people viewer
      _allMarkerTypes = TourDatabase.getAllTourMarkerTypes();
      _markerTypeViewer.setInput(new Object());

      // reselect previous person and tabfolder
      restoreState();

      enableControls();
      addPrefListener();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite prefPageContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(prefPageContainer);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(prefPageContainer);
      {

         final Label label = new Label(prefPageContainer, SWT.WRAP);
         label.setText("The tour &marker type defines the layout of tour markers");

         final Composite innerContainer = new Composite(prefPageContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(innerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(innerContainer);
         {
            createUI_10_MarkerType_Viewer(innerContainer);
            createUI_20_MarkerType_Actions(innerContainer);

            createUI_30_MarkerType_Details(innerContainer);
         }

         // placeholder
         new Label(prefPageContainer, SWT.NONE);
      }

      return prefPageContainer;
   }

   private Control createUI_01_NoUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         final Label label = new Label(container, SWT.WRAP);
         label.setText(Messages.Pref_App_Label_TourEditorIsModified);
         GridDataFactory.fillDefaults().grab(true, false).hint(350, SWT.DEFAULT).applyTo(label);
      }

      return container;
   }

   private void createUI_10_MarkerType_Viewer(final Composite parent) {

      final TableColumnLayout tableLayout = new TableColumnLayout();

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      layoutContainer.setLayout(tableLayout);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(5))
            .applyTo(layoutContainer);

      /*
       * create table
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

            // compare by last + first name

            final TourMarkerType p1 = (TourMarkerType) e1;
            final TourMarkerType p2 = (TourMarkerType) e2;

            final int compareName = p1.getName().compareTo(p2.getName());

            return compareName;
         }
      });

      _markerTypeViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectMarkerType());

//      _markerTypeViewer.addDoubleClickListener(doubleClickEvent -> {
//         _tabFolderPerson.setSelection(0);
//         _txtFirstName.setFocus();
//         _txtFirstName.selectAll();
//      });

   }

   private void createUI_20_MarkerType_Actions(final Composite parent) {

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
            _btnMarkerType_Add.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Add()));
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
             * Button: Update
             */
            _btnMarkerType_Save = new Button(container, SWT.NONE);
            _btnMarkerType_Save.setText(Messages.App_Action_Save);
            _btnMarkerType_Save.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Save()));
            setButtonLayoutData(_btnMarkerType_Save);
            final GridData gd = (GridData) _btnMarkerType_Save.getLayoutData();
            gd.verticalAlignment = SWT.BOTTOM;
            gd.grabExcessVerticalSpace = true;
         }
         {
            /*
             * Button: Cancel
             */
            _btnMarkerType_Cancel = new Button(container, SWT.NONE);
            _btnMarkerType_Cancel.setText(Messages.App_Action_Cancel);
            _btnMarkerType_Cancel.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMarkerType_Cancel()));
            setButtonLayoutData(_btnMarkerType_Cancel);
         }
      }
   }

   private void createUI_30_MarkerType_Details(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).extendedMargins(0, 0, 7, 0).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Field: Marker type name
             */
            _lblName = new Label(container, SWT.NONE);
            _lblName.setText("Na&me");

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

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
            _txtDescription.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults()
                  .hint(convertWidthInCharsToPixels(20), convertHeightInCharsToPixels(4))
                  .grab(true, false)
                  .applyTo(_txtDescription);
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

   private boolean deleteTourMarkerType(final TourMarkerType selectedMarkerType) {

      if (deleteTourMarkerType_10_FromAllTourMarkers(selectedMarkerType)) {

         if (deleteTourMarkerType_20_FromDB(selectedMarkerType)) {
            return true;
         }
      }

      return false;
   }

   private boolean deleteTourMarkerType_10_FromAllTourMarkers(final TourMarkerType selectedMarkerType) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em != null) {

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT TourMarker" //$NON-NLS-1$
               + " FROM  TourMarker AS TourMarker" //$NON-NLS-1$
               + " WHERE TourMarker.tourMarkerType.markerTypeID = " + selectedMarkerType.getId()); //$NON-NLS-1$

         final List<?> allTourMarker = query.getResultList();
         if (allTourMarker.size() > 0) {

            final EntityTransaction ts = em.getTransaction();

            try {

               ts.begin();

               // remove tour marker type from all tour markers
               for (final Object listItem : allTourMarker) {

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

   private boolean deleteTourMarkerType_20_FromDB(final TourMarkerType markerType) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {

         final TourMarkerType tourTypeEntity = em.find(TourMarkerType.class, markerType.getId());

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

      if (_prefChangeListener != null) {
         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }

      if (_isNoUI) {
         super.dispose();
         return;
      }

      super.dispose();
   }

   private void enableControls() {

      enableControls(true);
   }

   private void enableControls(final boolean isValidateFields) {

// SET_FORMATTING_OFF

      final boolean isValid               = isValidateFields ? isMarkerTypeValid() : true;

      final boolean isMarkerTypeSelected  = _selectedMarkerType != null;
      final boolean isNewMarkerType       = _newMarkerType != null;
      final boolean canEditFields         = isMarkerTypeSelected || isNewMarkerType;

      _lblDescription         .setEnabled(canEditFields);
      _lblName                .setEnabled(canEditFields);

      _txtDescription         .setEnabled(canEditFields);
      _txtName                .setEnabled(canEditFields);

      _btnMarkerType_Add      .setEnabled(_isModified == false && isValid);
      _btnMarkerType_Cancel   .setEnabled(_isModified);
      _btnMarkerType_Delete   .setEnabled(isMarkerTypeSelected && isNewMarkerType == false);
      _btnMarkerType_Save     .setEnabled(_isModified && isValid);

      _markerTypeViewer.getTable().setEnabled(_isModified == false && isValid);

// SET_FORMATTING_ON
   }

   private void fireModifyEvent() {

      if (_isFireModifyEvent) {

//         TourManager.getInstance().clearTourDataCache();
//
//         // fire event that person is modified
//         getPreferenceStore().setValue(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED, Math.random());

         _isFireModifyEvent = false;
      }
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
      noDefaultAndApplyButton();
   }

   private void initUI(final Composite parent) {

      initializeDialogUnits(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModifyPerson());

      _defaultModifyListener = modifyEvent -> onModifyPerson();
   }

   /**
    * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
    */
   private boolean isMarkerTypeValid() {

      if (_selectedMarkerType == null && _newMarkerType == null) {

         return true;
      }

      if (StringUtils.isNullOrEmpty(_txtName.getText())) {

         setErrorMessage("Name is required");

         // don't set focus because another field could be edited
//       _txtName.setFocus();

         return false;
      }

      setErrorMessage(null);

      return true;
   }

   @Override
   public boolean okToLeave() {

      if (_isNoUI) {
         super.okToLeave();
         return true;
      }

      if (isMarkerTypeValid() == false) {
         return false;
      }

      saveState();
      saveMarkerType(true, true);

      // enable action because the user can go back to this pref page
      enableControls();

      return super.okToLeave();
   }

   private void onMarkerType_Add() {

      _newMarkerType = new TourMarkerType();
      _isModified = true;

      updateUIFromModel(_newMarkerType);
      enableControls(false);

      // edit name
      _txtName.setFocus();
   }

   private void onMarkerType_Cancel() {

      _newMarkerType = null;
      _isModified = false;

      // update error message
      setErrorMessage(null);

      updateUIFromModel(_selectedMarkerType);
      enableControls();

      setFocusToViewer();
   }

   private void onMarkerType_Delete() {

      // confirm deletion
      final MessageDialog dialog = new MessageDialog(

            getShell(),

            "Delete Tour Marker Types",
            null,

            "Are you sure you want to delete the tour marker type \"%s\" and reset the tour marker type in ALL related tour markers?".formatted(
                  _selectedMarkerType.getName()),

            MessageDialog.QUESTION,

            new String[] { Messages.App_Action_Delete, IDialogConstants.CANCEL_LABEL },
            1);

      if (dialog.open() != Window.OK) {

         setFocusToViewer();
         return;
      }

      BusyIndicator.showWhile(getShell().getDisplay(), () -> {

         // remove entity from the db
         if (deleteTourMarkerType(_selectedMarkerType)) {

            reloadMarkerTypes();

            // update states
            _isFireModifyEvent = true;
            _isModified = false;

            enableControls();

            setFocusToViewer();
         }
      });

   }

   private void onMarkerType_Save() {

      if (isMarkerTypeValid() == false) {
         return;
      }

      saveMarkerType(false, false);
      enableControls();

      setFocusToViewer();
   }

   /**
    * set person modified and enable actions accordingly
    */
   private void onModifyPerson() {

      if (_isInUpdateUI) {
         return;
      }

      _isModified = true;

      enableControls();
   }

   private void onSelectMarkerType() {

      final IStructuredSelection selection = (IStructuredSelection) _markerTypeViewer.getSelection();
      final TourMarkerType markerType = (TourMarkerType) selection.getFirstElement();

      if (markerType != null) {

         _selectedMarkerType = markerType;

         updateUIFromModel(_selectedMarkerType);

      } else {

         // ignore, this can happen when a refresh() of the table viewer is done

         _selectedMarkerType = null;
      }

      enableControls();
   }

   @Override
   public boolean performCancel() {

      if (_isNoUI) {
         super.performCancel();
         return true;
      }

      saveState();
      fireModifyEvent();

      return super.performCancel();
   }

   @Override
   public boolean performOk() {

      if (_isNoUI) {
         super.performOk();
         return true;
      }

      if (performOK10() == false) {
         return false;
      }

      return super.performOk();
   }

   private boolean performOK10() {

      if (isMarkerTypeValid() == false) {
         return false;
      }

      saveMarkerType(false, false);

      /*
       * update UI because it's possible that other dialog boxes are displayed before the pref
       * dialog is closed
       */
      enableControls();

      saveState();
      fireModifyEvent();

      return true;
   }

   private void reloadMarkerTypes() {

      TourDatabase.clearTourMarkerTypes();

      _allMarkerTypes = TourDatabase.getAllTourMarkerTypes();

      _markerTypeViewer.refresh();
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
    * @param isRevert
    */
   private void saveMarkerType(final boolean isAskToSave, final boolean isRevert) {

      final boolean isNewMarkerType = _newMarkerType != null;

      final TourMarkerType markerType = isNewMarkerType
            ? _newMarkerType
            : _selectedMarkerType;

      _newMarkerType = null;

      if (_isModified == false) {

         return;
      }

      if (isAskToSave) {

         if (MessageDialog.openQuestion(
               Display.getCurrent().getActiveShell(),
               Messages.Pref_People_Dialog_SaveModifiedPerson_Title,
               NLS.bind(Messages.Pref_People_Dialog_SaveModifiedPerson_Message,

                     // use name from the ui because it could be modified
                     _txtName.getText())) == false) {

            // revert person

            if (isRevert) {

               // update state
               _isModified = false;

               // update ui from the previous selected marker type
               updateUIFromModel(_selectedMarkerType);
            }

            return;
         }
      }

      updateModelFromUI(markerType);

      // save model
      TourDatabase.saveEntity(markerType, markerType.getId(), TourType.class);

      reloadMarkerTypes();

      // update states
      _isFireModifyEvent = true;
      _isModified = false;

      // select marker type
      _markerTypeViewer.setSelection(new StructuredSelection(markerType), true);
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
    * Set to the viewer
    *
    */
   private void setFocusToViewer() {

      _markerTypeViewer.getTable().setFocus();
   }

   private void updateModelFromUI(final TourMarkerType markerType) {

      markerType.setName(_txtName.getText());
      markerType.setDescription(_txtDescription.getText());
   }

   private void updateUIFromModel(final TourMarkerType markerType) {

      if (markerType == null) {
         return;
      }

      _isInUpdateUI = true;
      {
         _txtDescription.setText(markerType.getDescription());
         _txtName.setText(markerType.getName());
      }
      _isInUpdateUI = false;
   }
}
