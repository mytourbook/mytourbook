/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourBike;
import net.tourbook.database.TourBikeManager;
import net.tourbook.tour.TourManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
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
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageBikes extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID                        = "net.tourbook.preferences.PrefPageBikesId"; //$NON-NLS-1$
   //
   private static final String    STATE_SELECTED_BIKE       = "selectedBikeId";                          //$NON-NLS-1$


   private final IPreferenceStore _prefStore                = TourbookPlugin.getPrefStore();
   private final IDialogSettings  _state                    = TourbookPlugin.getState(ID);

   private ArrayList<TourBike>    _bikes;


   private final NumberFormat     _nf1 = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private SelectionListener         _defaultSelectionListener;
   private MouseWheelListener        _defaultMouseWheelListener;
   private ModifyListener            _defaultModifyListener;

   private boolean                   _isFireModifyEvent         = false;
   private boolean                   _isBikeModified            = false;
   private boolean                   _isUpdatingUI              = false;

   private TourBike                  _selectedBike;
   private TourBike                  _newBike;

   /**
    * Is <code>true</code> when a tour in the tour editor is modified.
    */
   private boolean                   _isNoUI                    = false;

   /*
    * UI controls
    */
   private TableViewer          _bikesViewer;

   private Button               _btnAddBike;
   private Button               _btnSaveBike;
   private Button               _btnCancel;

   private Text                 _txtBikeBrand;
   private Text                 _txtBikeModel;
   private Spinner              _spinnerBikeWeight;
   private DateTime             _dtBikePurchaseDate;

   private class ClientsContentProvider implements IStructuredContentProvider {

      public ClientsContentProvider() {}

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {
         return _bikes.toArray();
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      _isUpdatingUI = true;
      // check: if a tour is modified in the tour editor
      if (TourManager.isTourEditorModified()) {

         _isNoUI = true;

         return createNoUI(parent);
      }

      initUI(parent);

      final Composite container = createUI(parent);

      // update bikes viewer
      _bikes = TourBikeManager.getTourBikes();
      if (_bikes.isEmpty()) {
         createInitialBike();
      } else {
         _bikesViewer.setInput(new Object());

         // reselect previous bike
         restoreState();

         enableActions();
      }

      _isUpdatingUI = false;

      return container;
   }

   private TourBike createDefaultBike() {

      final TourBike newBike = new TourBike();

      newBike.setBrand(Messages.App_Default_BikeManufacturer);
      newBike.setModel(UI.EMPTY_STRING);
      newBike.setWeight(9.9f);
      newBike.setPurchaseDate(TourBike.DEFAULT_PURCHASE_DATE.toInstant().toEpochMilli());

      return newBike;
   }

   private void createInitialBike() {

      // this is a request, to create a new bike

      final TourBike newBike = createDefaultBike();

      newBike.persist();

      // update model
      _bikes.add(newBike);

      // update state
      _isFireModifyEvent = true;
      _isBikeModified = false;

      // update ui viewer and bike ui
      _bikesViewer.add(newBike);
      _bikesViewer.setSelection(new StructuredSelection(newBike));

      enableActions();

      // for the first bike, disable Add.. button and bike list that the user is not confused
      _btnAddBike.setEnabled(false);
      _bikesViewer.getTable().setEnabled(false);

      // select bike brand
      _txtBikeBrand.selectAll();
      _txtBikeModel.setFocus();
   }

   private Composite createUI(final Composite parent) {

      /*
       * UI controls
       */
      Composite prefPageContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(prefPageContainer);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(prefPageContainer);

      final Label label = new Label(prefPageContainer, SWT.WRAP);
      label.setText(Messages.Pref_Bikes_Title);

      final Composite innerContainer = new Composite(prefPageContainer, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(innerContainer);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(innerContainer);

      createBikeTable(innerContainer);
      createBikeActionButtons(innerContainer);
      createBikeEditor(innerContainer);

      return prefPageContainer;

   }

   private void defineAllColumns(final TableColumnLayout tableLayout) {

      TableViewerColumn tvc;
      TableColumn tc;

      /*
       * column: bike brand
       */
      tvc = new TableViewerColumn(_bikesViewer, SWT.LEAD);
      tc = tvc.getColumn();
      tc.setText(Messages.Pref_Bikes_Column_bike_brand);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((TourBike) cell.getElement()).getBrand());
         }
      });
      tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

      /*
       * column: bike model
       */
      tvc = new TableViewerColumn(_bikesViewer, SWT.LEAD);
      tc = tvc.getColumn();
      tc.setText(Messages.Pref_Bikes_Column_bike_model);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((TourBike) cell.getElement()).getModel());
         }
      });
      tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

      /*
       * column: weight
       */
      tvc = new TableViewerColumn(_bikesViewer, SWT.TRAIL);
      tc = tvc.getColumn();
      tc.setText(UI.UNIT_LABEL_WEIGHT);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final float weight = UI.convertBodyWeightFromMetric(((TourBike) cell.getElement()).getWeight());
            cell.setText(_nf1.format(weight));
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(7 * 4), true));

      /*
       * column: purchase date
       */
      tvc = new TableViewerColumn(_bikesViewer, SWT.TRAIL);
      tc = tvc.getColumn();
      tc.setText(Messages.Pref_Bikes_Column_bike_purchase_date);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final long purchaseDateValue = ((TourBike) cell.getElement()).getPurchaseDate();

            if (purchaseDateValue == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(TimeTools.getZonedDateTime(purchaseDateValue).format(TimeTools.Formatter_Date_S));
            }
         }
      });
      tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

   }

   private Control createNoUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

      final Label label = new Label(container, SWT.WRAP);
      label.setText(Messages.Pref_App_Label_TourEditorIsModified);
      GridDataFactory.fillDefaults().grab(true, false).hint(350, SWT.DEFAULT).applyTo(label);

      return container;
   }

   private void createBikeTable(final Composite parent) {
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
          (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));

      table.setHeaderVisible(true);

      _bikesViewer = new TableViewer(table);
      defineAllColumns(tableLayout);

      _bikesViewer.setUseHashlookup(true);
      _bikesViewer.setContentProvider(new ClientsContentProvider());

      _bikesViewer.setComparator(new ViewerComparator() {
         @Override
         public int compare(final Viewer viewer, final Object e1, final Object e2) {

            // compare by brand + model

            final TourBike b1 = (TourBike) e1;
            final TourBike b2 = (TourBike) e2;

            final int compareBikeBrand = b1.getBrand().compareTo(b2.getBrand());

            if (compareBikeBrand != 0) {
               return compareBikeBrand;
            }

            return b1.getModel().compareTo(b2.getModel());
         }
      });

      _bikesViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectBike());

      _bikesViewer.addDoubleClickListener(doubleClickEvent -> {
         _txtBikeBrand.setFocus();
         _txtBikeBrand.selectAll();
      });

   }

   private void createBikeActionButtons(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      /*
       * button: add
       */
      _btnAddBike = new Button(container, SWT.NONE);
      _btnAddBike.setText(Messages.Pref_Bikes_Action_add_bike);
      setButtonLayoutData(_btnAddBike);
      _btnAddBike.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAddBike()));

      /*
       * button: update
       */
      _btnSaveBike = new Button(container, SWT.NONE);
      _btnSaveBike.setText(Messages.App_Action_Save);
      _btnSaveBike.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSaveBike()));
      setButtonLayoutData(_btnSaveBike);
      final GridData gd = (GridData) _btnSaveBike.getLayoutData();
      gd.verticalAlignment = SWT.BOTTOM;
      gd.grabExcessVerticalSpace = true;

      /*
       * button: cancel
       */
      _btnCancel = new Button(container, SWT.NONE);
      _btnCancel.setText(Messages.App_Action_Cancel);
      _btnCancel.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onCancelEdit()));
      setButtonLayoutData(_btnCancel);
   }

   private void createBikeEditor(final Composite parent) {
      final Composite editorArea = new Composite(parent, SWT.NONE);

      GridDataFactory.fillDefaults().applyTo(editorArea);
      GridLayoutFactory.swtDefaults().numColumns(5).extendedMargins(0, 0, 7, 0).applyTo(editorArea);

      // brand
      final Label lbBrand = new Label(editorArea, SWT.NONE);
      lbBrand.setText(Messages.Pref_Bikes_Label_bike_brand);

      _txtBikeBrand = new Text(editorArea, SWT.BORDER);
      _txtBikeBrand.addModifyListener(_defaultModifyListener);
      GridDataFactory.fillDefaults()
          .grab(true, false)
          .span(4, 1)
          .applyTo(_txtBikeBrand);

      // model
      final Label lbModel = new Label(editorArea, SWT.NONE);
      lbModel.setText(Messages.Pref_Bikes_Label_bike_model);

      _txtBikeModel = new Text(editorArea, SWT.BORDER);
      _txtBikeModel.addModifyListener(_defaultModifyListener);
      GridDataFactory.fillDefaults()
          .grab(true, false)
          .span(4, 1)
          .applyTo(_txtBikeModel);

      // weight
      final Label lbWeight = new Label(editorArea, SWT.NONE);
      lbWeight.setText(Messages.Pref_Bikes_Label_bike_weight);

      _spinnerBikeWeight = new Spinner(editorArea, SWT.BORDER);
      _spinnerBikeWeight.setDigits(1);
      _spinnerBikeWeight.setMinimum(0);
      _spinnerBikeWeight.setMaximum(177); // 80.0 kg, 176.4 lbs
      _spinnerBikeWeight.addSelectionListener(_defaultSelectionListener);
      _spinnerBikeWeight.addMouseWheelListener(_defaultMouseWheelListener);
      GridDataFactory.fillDefaults()
          .align(SWT.BEGINNING, SWT.FILL)
          .applyTo(_spinnerBikeWeight);

      final Label label = new Label(editorArea, SWT.NONE);
      label.setText(UI.UNIT_LABEL_WEIGHT);
      UI.createSpacer_Horizontal(editorArea, 2);

      // purchase date
      final Label lbPurchaseDate = new Label(editorArea, SWT.NONE);
      lbPurchaseDate.setText(Messages.Pref_Bikes_Label_purchase_date);

      _dtBikePurchaseDate = new DateTime(editorArea, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
      // There seems to be no way to reliably detect modifications made by the user
      // only and filter out the events being triggered by setDate()
      // adding a selection listener didn't work out

      GridDataFactory.fillDefaults()
          .align(SWT.FILL, SWT.FILL)
          .span(3, 1)
          .applyTo(_dtBikePurchaseDate);

      editorArea.layout(true, true);
   }

   private void enableActions() {

      final boolean isValid = isBikeValid();

      _btnAddBike.setEnabled(!_isBikeModified && isValid);
      _bikesViewer.getTable().setEnabled(!_isBikeModified && isValid);

      _btnSaveBike.setEnabled(_isBikeModified && isValid);
      _btnCancel.setEnabled(_isBikeModified);

   }

   private void fireModifyEvent() {

      if (_isFireModifyEvent) {

         TourManager.getInstance().clearTourDataCache();

         // fire event that bike is modified
         getPreferenceStore().setValue(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED, Math.random());

         _isFireModifyEvent = false;
      }
   }

   private ZonedDateTime getPurchaseDateFromUI() {

      return ZonedDateTime.of(
          _dtBikePurchaseDate.getYear(),
          _dtBikePurchaseDate.getMonth() + 1,
          _dtBikePurchaseDate.getDay(),
          0,
          0,
          0,
          0,
          TimeTools.getDefaultTimeZone());
   }

   /**
    * @return Returns the bike which currently is displayed, one bike is at least available
    *         therefore this should never return <code>null</code> but it can be <code>null</code>
    *         when the application is started the first time and bikes are not yet created.
    */
   private TourBike getCurrentBike() {

      final boolean isNewBike = _newBike != null;
      return isNewBike ? _newBike : _selectedBike;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(_prefStore);
      noDefaultAndApplyButton();
   }

   private void initUI(final Composite parent) {

      initializeDialogUnits(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onModifyBike());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onModifyBike();
      };

      _defaultModifyListener = modifyEvent -> onModifyBike();

   }

   /**
    * @return Returns <code>true</code> when bike inside editor is valid, otherwise <code>false</code>.
    */
   private boolean isBikeValid() {

      if (StringUtils.isNullOrEmpty(_txtBikeBrand.getText())) {

         setErrorMessage(Messages.Pref_Bikes_Error_bike_brand_required);

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

      if (!isBikeValid()) {
         return false;
      }

      saveState();
      saveBike(true, true);

      // enable action because the user can go back to this pref page
      enableActions();

      return super.okToLeave();
   }

   private void onAddBike() {

      _newBike = createDefaultBike();
      _isBikeModified = true;

      updateEditorFromBike(_newBike);
      enableActions();

      // select bike brand
      _txtBikeBrand.selectAll();
      _txtBikeBrand.setFocus();
   }

   private void onCancelEdit() {

      _newBike = null;
      _isBikeModified = false;

      updateEditorFromBike(_selectedBike);
      enableActions();

      _bikesViewer.getTable().setFocus();
   }

   /**
    * set bike modified and enable actions accordingly
    */
   private void onModifyBike() {

      if (_isUpdatingUI) {
         return;
      }

      _isBikeModified = true;

      enableActions();
   }

   private void onSaveBike() {

      if (!isBikeValid()) {
         return;
      }

      saveBike(false, false);
      enableActions();

      _bikesViewer.getTable().setFocus();
   }

   private void onSelectBike() {

      final IStructuredSelection selection = (IStructuredSelection) _bikesViewer.getSelection();
      final TourBike bike = (TourBike) selection.getFirstElement();

      // bike may be null  when a refresh() of the table viewer is done - ignore it
      if (bike != null) {

         _selectedBike = bike;

         updateEditorFromBike(_selectedBike);

      }
      enableActions();
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

      return super.performOk();
   }

   private void restoreState() {

      /*
       * selected bike
       */
      final long bikeId = Util.getStateLong(_state, STATE_SELECTED_BIKE, -1);
      StructuredSelection bikeSelection = null;
      if (bikeId != -1) {

         for (final TourBike bike : _bikes) {
            if (bike.getBikeId() == bikeId) {
               bikeSelection = new StructuredSelection(bike);
               break;
            }
         }
      }
      if (bikeSelection == null && !_bikes.isEmpty()) {

         // previous bike could not be reselected, select first bike
         bikeSelection = new StructuredSelection(_bikesViewer.getTable().getItem(0).getData());
      }

      if (bikeSelection != null) {
         _bikesViewer.setSelection(bikeSelection);
      }

   }

   /**
    * @param isAskToSave
    * @param isRevert
    */
   private void saveBike(final boolean isAskToSave, final boolean isRevert) {

      final boolean isNewBike = _newBike != null;
      final TourBike bike = getCurrentBike();
      _newBike = null;

      if (_isBikeModified) {

         if (isAskToSave &&
             !MessageDialog.openQuestion(
                Display.getCurrent().getActiveShell(),
                Messages.Pref_Bikes_Dialog_SaveModifiedBike_Title,
                NLS.bind(Messages.Pref_Bikes_Dialog_SaveModifiedBike_Message,
                 // use brand from the ui because it could be modified
                 _txtBikeBrand.getText()))) {

            // revert bike

            if (isRevert) {

               // update state
               _isBikeModified = false;

               // update ui from the previous selected bike
               updateEditorFromBike(_selectedBike);
            }

            return;
         }

         updateBikeFromUI(bike);
         bike.persist();

         // call to persist() updates the bike list, the model, retrieve updated bike list
         _bikes = TourBikeManager.getTourBikes();

         // update state
         _isFireModifyEvent = true;
         _isBikeModified = false;

         // update ui
         if (isNewBike) {
            _bikesViewer.add(bike);

         } else {
            // !!! refreshing a bike does not trigger resorting the table when sorting has changed !!!
            _bikesViewer.refresh();
         }

         // select updated/new bike
         _bikesViewer.setSelection(new StructuredSelection(bike), true);
      }

   }

   private void saveState() {

      // selected bike
      final Object firstElement = ((IStructuredSelection) _bikesViewer.getSelection()).getFirstElement();
      if (firstElement instanceof final TourBike tourBike) {
         _state.put(STATE_SELECTED_BIKE, tourBike.getBikeId());
      }

   }

   /*
    * Update bike from UI controls
    */
   private void updateBikeFromUI(final TourBike bike) {
      bike.setBrand(_txtBikeBrand.getText());
      bike.setModel(_txtBikeModel.getText());
      bike.setWeight(_spinnerBikeWeight.getSelection() / 10.0f);
      bike.setPurchaseDate(getPurchaseDateFromUI().toInstant().toEpochMilli());
      bike.setTypeId(0); // not implemented
   }

  /*
   * Update UI controls from bike
   */
   private void updateEditorFromBike(final TourBike bike) {
      _isUpdatingUI = true;
      _txtBikeBrand.setText(bike.getBrand());
      if (bike.getModel() != null) {
         _txtBikeModel.setText(bike.getModel());
      }
      _spinnerBikeWeight.setSelection((int) (bike.getWeight() * 10));
      final var purchaseDate = bike.getZonedPurchaseDateWithDefault();
      _dtBikePurchaseDate.setDate(purchaseDate.getYear(), purchaseDate.getMonthValue() - 1, purchaseDate.getDayOfMonth());
      _isUpdatingUI = false;
   }

}
