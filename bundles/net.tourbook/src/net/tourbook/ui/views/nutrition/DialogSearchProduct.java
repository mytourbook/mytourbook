/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.nutrition;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourFuelProduct;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.nutrition.Product;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
public class DialogSearchProduct extends TitleAreaDialog implements PropertyChangeListener {

   private static final int              MAX_ADJUST_SECONDS     = 120;
   private static final int              MAX_ADJUST_MINUTES     = 120;                                                                      // x 60
   private static final int              MAX_ADJUST_ALTITUDE_1  = 20;
   private static final int              MAX_ADJUST_ALTITUDE_10 = 40;                                                                       // x 10

   private static final int              VH_SPACING             = 2;

   private static final IPreferenceStore _prefStore             = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                 = TourbookPlugin.getState("net.tourbook.ui.views.rawData.DialogMergeTours");//$NON-NLS-1$
   private TableViewer                   _productsViewer;
   private List<Product>                 _products;

   private TourData                      _tourData;

   private Composite                     _dlgContainer;

   private PixelConverter                _pc;
   private boolean                       _isInUIInit;

   /*
    * UI controls
    */
   private Button                _btnAdd;
   private Button                _btnSearch;
   private List<String>          _searchHistory  = new ArrayList<>();

   private Combo                 _cboSearchQuery;
   private ComboViewer           _queryViewer;
   final NutritionQuery          _nutritionQuery = new NutritionQuery();

   private PostSelectionProvider _postSelectionProvider;

   /*
    * vertical adjustment options
    */
   private Group _groupAltitude;

   private Label _lblAltitudeDiff1;
   private Label _lblAltitudeDiff10;

   private Scale _scaleAltitude1;
   private Scale _scaleAltitude10;

   /*
    * horizontal adjustment options
    */
   private Button _chkSynchStartTime;

   private Label  _lblAdjustMinuteValue;
   private Label  _lblAdjustSecondsValue;

   private Scale  _scaleAdjustMinutes;
   private Scale  _scaleAdjustSeconds;

   private Label  _lblAdjustMinuteUnit;
   private Label  _lblAdjustSecondsUnit;



   /*
    * display actions
    */
   private Button  _chkValueDiffScaling;

   private Button  _chkPreviewChart;




   private final Image                   _iconPlaceholder;
   private final HashMap<Integer, Image> _graphImages = new HashMap<>();



   private IPropertyChangeListener       _prefChangeListener;

   public class SearchContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _searchHistory.toArray(new String[_searchHistory.size()]);
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {
         if (_products == null) {
            return new String[] {};
         } else {
            return _products.toArray();
         }
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {
         switch (index) {
         case 0:
            return getImage(obj);

         default:
            return null;
         }
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         return obj.toString();
//         final PointOfInterest poi = (PointOfInterest) obj;
//
//         switch (index) {
//         case 0:
//            return poi.getCategory();
//         case 1:
//
//            final StringBuilder sb = new StringBuilder(poi.getName());
//
//            final List<? extends Waypoint> nearestPlaces = poi.getNearestPlaces();
//            if (nearestPlaces != null && nearestPlaces.size() > 0) {
//
//               // create a string with all nearest waypoints
//               boolean isFirstPoi = true;
//               for (final Waypoint waypoint : nearestPlaces) {
//
//                  if (isFirstPoi) {
//                     isFirstPoi = false;
//                     sb.append("Messages.Poi_View_Label_NearestPlacesPart1");
//                     sb.append("Messages.Poi_View_Label_Near");
//                  } else {
//                     sb.append("Messages.Poi_View_Label_NearestPlacesPart2");
//                  }
//
//                  sb.append("Messages.Poi_View_Label_NearestPlacesPart3");
//                  sb.append(waypoint.getName());
//               }
//               sb.append("Messages.Poi_View_Label_NearestPlacesPart4");
//            }
//            return sb.toString();
//         default:
//            return getText(obj);
//         }
      }

      @Override
      public Image getImage(final Object obj) {

         return null;
      }
   }

   /**
    * @param parentShell
    * @param tourData
    *           {@link TourData} for the tour
    */
   public DialogSearchProduct(final Shell parentShell, final TourData tourData) {

      super(parentShell);

      // make dialog resizable and display maximize button
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      // set icon for the window
      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.MergeTours).createImage());

      _tourData = tourData;
      _iconPlaceholder = TourbookPlugin.getImageDescriptor(Images.App_EmptyIcon_Placeholder).createImage();

   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _productsViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _productsViewer.refresh();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);

      _nutritionQuery.addPropertyChangeListener(this);
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }



   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.tour_merger_dialog_title);

      shell.addDisposeListener(disposeEvent -> onDispose());
   }

   @Override
   public void create() {

      addPrefListener();

      // create UI widgets
      super.create();

      _dlgContainer.layout(true, true);

      createActions();
//      _isInUIInit = true;
//      {
//         restoreState();
//      }
//      _isInUIInit = false;

      setTitle("search for a fuel item");

      setMessage("search for a fuel item");

      enableActions();

   }

   private void createActions() {


   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);
      getButton(IDialogConstants.OK_ID).setVisible(false);
      getButton(IDialogConstants.CANCEL_ID).setVisible(false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider("ID");

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
      {
         createUI_10_Header(container);
         createUI_20_Viewer(container);
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite queryContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(queryContainer);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 5, 2, 2)
            .spacing(5, 0)
            .numColumns(3)
            .applyTo(queryContainer);
      {
         {
            /*
             * label: POI
             */
            final Label label = new Label(queryContainer, SWT.NONE);
            label.setText("Messages.Poi_View_Label_POI");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
         }
         {
            /*
             * combo: search
             */
            _cboSearchQuery = new Combo(queryContainer, SWT.NONE);
            _cboSearchQuery.setVisibleItemCount(30);
            _cboSearchQuery.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
               // start searching when ENTER is pressed
               onSearchPoi();
            }));
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_cboSearchQuery);
         }
         {
            /*
             * button: search
             */
            _btnSearch = new Button(queryContainer, SWT.PUSH);
            _btnSearch.setText("Messages.Poi_View_Button_Search");
            _btnSearch.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchPoi()));
         }
         {
            /*
             * Button: Add
             */
            _btnAdd = new Button(queryContainer, SWT.PUSH);
            _btnAdd.setText("Add");
            _btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAddProduct()));
         }
      }

      _queryViewer = new ComboViewer(_cboSearchQuery);
      _queryViewer.setContentProvider(new SearchContentProvider());
      _queryViewer.setComparator(new ViewerComparator());
   }

   private void createUI_20_Viewer(final Composite parent) {

      /*
       * table viewer: poi items
       */
      final Table poiTable = new Table(parent, /* SWT.BORDER | */SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(poiTable);
      poiTable.setLinesVisible(true);
      poiTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      poiTable.setHeaderVisible(true);

      // column: category
      final TableColumn columnCategory = new TableColumn(poiTable, SWT.LEFT);
      columnCategory.setText("Category"); //$NON-NLS-1$
      columnCategory.setWidth(75);

      // column: name
      final TableColumn columnName = new TableColumn(poiTable, SWT.LEFT);
      columnName.setText("Name"); //$NON-NLS-1$
      columnName.setWidth(300);

      _productsViewer = new TableViewer(poiTable);

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setLabelProvider(new ViewLabelProvider());

      _productsViewer.addPostSelectionChangedListener(selectionChangedEvent -> {

         final ISelection selection = selectionChangedEvent.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final Product selectedPoi = (Product) firstElement;

         _postSelectionProvider.setSelection(selectedPoi);
      });
   }

   /**
    * group: adjust time
    */
   private void createUIGroupHorizontalAdjustment(final Composite parent) {

      final int valueWidth = _pc.convertWidthInCharsToPixels(4);
      Label label;

      final Group groupTime = new Group(parent, SWT.NONE);
      groupTime.setText(Messages.tour_merger_group_adjust_time);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(groupTime);
      GridLayoutFactory.swtDefaults()//
            .numColumns(1)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(groupTime);

      /*
       * Checkbox: keep horizontal and vertical adjustments
       */
      _chkSynchStartTime = new Button(groupTime, SWT.CHECK);
      GridDataFactory.fillDefaults().applyTo(_chkSynchStartTime);
      _chkSynchStartTime.setText(Messages.tour_merger_chk_use_synced_start_time);
      _chkSynchStartTime.setToolTipText(Messages.tour_merger_chk_use_synced_start_time_tooltip);

      /*
       * container: seconds scale
       */
      final Composite timeContainer = new Composite(groupTime, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(timeContainer);
      GridLayoutFactory.fillDefaults().numColumns(4).spacing(0, 0).applyTo(timeContainer);

      /*
       * scale: adjust seconds
       */
      _lblAdjustSecondsValue = new Label(timeContainer, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(valueWidth, SWT.DEFAULT)
            .applyTo(_lblAdjustSecondsValue);

      label = new Label(timeContainer, SWT.NONE);
      label.setText(UI.SPACE1);

      _lblAdjustSecondsUnit = new Label(timeContainer, SWT.NONE);
      _lblAdjustSecondsUnit.setText(Messages.tour_merger_label_adjust_seconds);

      _scaleAdjustSeconds = new Scale(timeContainer, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAdjustSeconds);
      _scaleAdjustSeconds.setMinimum(0);
      _scaleAdjustSeconds.setMaximum(MAX_ADJUST_SECONDS * 2);
      _scaleAdjustSeconds.setPageIncrement(20);

      /*
       * scale: adjust minutes
       */
      _lblAdjustMinuteValue = new Label(timeContainer, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(valueWidth, SWT.DEFAULT)
            .applyTo(_lblAdjustMinuteValue);

      label = new Label(timeContainer, SWT.NONE);
      label.setText(UI.SPACE1);

      _lblAdjustMinuteUnit = new Label(timeContainer, SWT.NONE);
      _lblAdjustMinuteUnit.setText(Messages.tour_merger_label_adjust_minutes);

      _scaleAdjustMinutes = new Scale(timeContainer, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAdjustMinutes);
      _scaleAdjustMinutes.setMinimum(0);
      _scaleAdjustMinutes.setMaximum(MAX_ADJUST_MINUTES * 2);
      _scaleAdjustMinutes.setPageIncrement(20);
   }

   /**
    * group: adjust altitude
    */
   private void createUIGroupVerticalAdjustment(final Composite parent) {

      _groupAltitude = new Group(parent, SWT.NONE);
      _groupAltitude.setText(Messages.tour_merger_group_adjust_altitude);
      GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(_groupAltitude);
      GridLayoutFactory.swtDefaults().numColumns(4)
//				.extendedMargins(0, 0, 0, 0)
//				.spacing(0, 0)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(_groupAltitude);

      /*
       * scale: altitude 20m
       */
      _lblAltitudeDiff1 = new Label(_groupAltitude, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
            .applyTo(_lblAltitudeDiff1);

      _scaleAltitude1 = new Scale(_groupAltitude, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAltitude1);
      _scaleAltitude1.setMinimum(0);
      _scaleAltitude1.setMaximum(MAX_ADJUST_ALTITUDE_1 * 2);
      _scaleAltitude1.setPageIncrement(5);

      /*
       * scale: altitude 100m
       */
      _lblAltitudeDiff10 = new Label(_groupAltitude, SWT.TRAIL);
      GridDataFactory
            .fillDefaults()
            .align(SWT.END, SWT.CENTER)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
            .applyTo(_lblAltitudeDiff10);

      _scaleAltitude10 = new Scale(_groupAltitude, SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAltitude10);
      _scaleAltitude10.setMinimum(0);
      _scaleAltitude10.setMaximum(MAX_ADJUST_ALTITUDE_10 * 2);
   }

   private void createUISectionDisplayOptions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(container);
      GridLayoutFactory.fillDefaults()//
            .numColumns(1)
            .spacing(VH_SPACING, VH_SPACING)
            .applyTo(container);

      /*
       * checkbox: display relative or absolute scale
       */
      _chkValueDiffScaling = new Button(container, SWT.CHECK);
      GridDataFactory.swtDefaults()/* .indent(5, 5) .span(4, 1) */.applyTo(_chkValueDiffScaling);
      _chkValueDiffScaling.setText(Messages.tour_merger_chk_alti_diff_scaling);
      _chkValueDiffScaling.setToolTipText(Messages.tour_merger_chk_alti_diff_scaling_tooltip);

      /*
       * checkbox: preview chart
       */
      _chkPreviewChart = new Button(container, SWT.CHECK);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(_chkPreviewChart);
      _chkPreviewChart.setText(Messages.tour_merger_chk_preview_graphs);
      _chkPreviewChart.setToolTipText(Messages.tour_merger_chk_preview_graphs_tooltip);

   }



   private void enableActions() {

   }



   private void enableMergeButton(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
//		return null;
      return _state;
   }

   private int getFromUIAltitudeOffset() {

      final int altiDiff1 = _scaleAltitude1.getSelection() - MAX_ADJUST_ALTITUDE_1;
      final int altiDiff10 = (_scaleAltitude10.getSelection() - MAX_ADJUST_ALTITUDE_10) * 10;

      final float localAltiDiff1 = altiDiff1 / UI.UNIT_VALUE_ELEVATION;
      final float localAltiDiff10 = altiDiff10 / UI.UNIT_VALUE_ELEVATION;

      _lblAltitudeDiff1.setText(Integer.toString((int) localAltiDiff1) + UI.SPACE + UI.UNIT_LABEL_ELEVATION);
      _lblAltitudeDiff10.setText(Integer.toString((int) localAltiDiff10) + UI.SPACE + UI.UNIT_LABEL_ELEVATION);

      return altiDiff1 + altiDiff10;
   }

   /**
    * @return tour time offset which is set in the UI
    */
   private int getFromUITourTimeOffset() {

      final int seconds = _scaleAdjustSeconds.getSelection() - MAX_ADJUST_SECONDS;
      final int minutes = _scaleAdjustMinutes.getSelection() - MAX_ADJUST_MINUTES;

      _lblAdjustSecondsValue.setText(Integer.toString(seconds));
      _lblAdjustMinuteValue.setText(Integer.toString(minutes));

      return minutes * 60 + seconds;
   }

   @Override
   protected void okPressed() {

      super.okPressed();
   }

   private void onAddProduct() {

      final ISelection selection = _productsViewer.getSelection();
      final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
      final Product selectedPoi = (Product) firstElement;
      final TourFuelProduct tfp = new TourFuelProduct(selectedPoi);
      _tourData.addFuelProduct(tfp);

      TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(_tourData));

   }

   private void onDispose() {

      _iconPlaceholder.dispose();

      _graphImages.values().forEach(Image::dispose);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
   }

   private void onSearchPoi() {

      // disable search controls
      _cboSearchQuery.setEnabled(false);
      _btnSearch.setEnabled(false);

      final String searchText = _cboSearchQuery.getText();

      // remove same search text
      if (_searchHistory.contains(searchText) == false) {

         // update model
         // _searchHistory.add(searchText);

         // update viewer
         _queryViewer.add(searchText);
      }

      // start product search

      _nutritionQuery.asyncFind(searchText);
   }




   @Override
   public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {

      @SuppressWarnings("unchecked")
      final List<Product> searchResults = (List<Product>) propertyChangeEvent.getNewValue();

      if (searchResults != null) {
         _products = searchResults;
      }

      Display.getDefault().asyncExec(() -> {

         // check if view is closed
         if (_btnSearch.isDisposed()) {
            return;
         }

         // refresh viewer
         _productsViewer.setInput(new Object());

         // select first entry, if there is one
         final Table poiTable = _productsViewer.getTable();
         if (poiTable.getItemCount() > 0) {

            final Object firstData = poiTable.getItem(0).getData();
            if (firstData instanceof Product) {

               _productsViewer.setSelection(new StructuredSelection(firstData));
               setViewerFocus();
            }
         }

         _cboSearchQuery.setEnabled(true);
         _btnSearch.setEnabled(true);
      });

   }

   private void restoreState() {

   }

   private void saveState() {

   }

   /**
    * set focus to selected item, selection and focus are not the same !!!
    */
   private void setViewerFocus() {

      final Table table = _productsViewer.getTable();

      table.setSelection(table.getSelectionIndex());
      table.setFocus();
   }


   private void updateUITourTimeOffset(final int tourTimeOffset) {

      final int seconds = tourTimeOffset % 60;
      final int minutes = tourTimeOffset / 60;

      _scaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
      _scaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      /*
       * validate fields
       */
      final boolean enableMergeButton = false;

      enableMergeButton(enableMergeButton);
   }
}
