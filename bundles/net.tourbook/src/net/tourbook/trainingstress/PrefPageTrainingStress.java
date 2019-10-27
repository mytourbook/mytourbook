/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
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
package net.tourbook.trainingstress;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Activator;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTrainingStress extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String    ID           = "net.tourbook.trainingstress.PrefPageTrainingStress";                //$NON-NLS-1$

   private IPreferenceStore      _prefStore   = Activator.getDefault().getPreferenceStore();
   private final IDialogSettings _importState = TourbookPlugin.getState(RawDataView.ID);

   private RawDataManager        _rawDataMgr  = RawDataManager.getInstance();

   private PixelConverter        _pc;
   private int                   _hintDefaultSpinnerWidth;
   private int                   DEFAULT_DESCRIPTION_WIDTH;

   private boolean               _isUpdateUI;

   private ActionTourType_Add    _action_TourType_Add;
   private ActionTourType_Remove _action_TourType_Remove;
   private ActionOpenPrefDialog  _actionOpenTourTypePrefs;

   private Font                  _boldFont    = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   /*
    * UI controls
    */
   private TableViewer _tourTypesViewer;
   private TabFolder   _tabFolder;

   /*
    * private Button _chkConvertWayPoints;
    */
   private Label    _labelThresholdPower_Value;

   private DateTime _textThresholdPower_Duration;

   private Spinner  _spinnerThresholdPower_Distance;
   private Spinner  _spinnerThresholdPower_AverageSlope;

   private class Action_TourType extends Action {

      private TourType _tourType;

      /**
       * @param tourType
       */
      public Action_TourType(final TourType tourType, final boolean isChecked) {

         super(tourType.getName(), AS_CHECK_BOX);

         if (isChecked == false) {

            final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
            setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
         }

         setChecked(isChecked);
         setEnabled(isChecked == false);

         _tourType = tourType;

      }

      @Override
      public void run() {
         _tourTypesViewer.add(_tourType);
         enableControls();
      }
   }

   private class ActionTourType_Add extends Action implements IMenuCreator {

      private Menu _menu;

      public ActionTourType_Add() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Pref_TrainingStress_Govss_AddTourTypes);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Add));

         setMenuCreator(this);
      }

      @Override
      public void dispose() {
         if (_menu != null) {
            _menu.dispose();
            _menu = null;
         }
      }

      @Override
      public Menu getMenu(final Control parent) {
         if (_menu != null) {
            _menu.dispose();
         }

         //Getting the current selected tour types
         final TableItem[] selectedTours = _tourTypesViewer.getTable().getItems();

         // add the tour types that have not been added already to the menu
         final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
         final MenuManager menuManager = new MenuManager();
         for (final TourType tourType : tourTypes) {

            boolean isChecked = false;
            for (final TableItem currentItem : selectedTours) {
               if (currentItem.getText().equals(tourType.getName())) {

                  isChecked = true;
                  break;
               }
            }

            final Action_TourType action = new Action_TourType(tourType, isChecked);

            menuManager.add(action);
         }

         menuManager.add(new Separator());
         menuManager.add(_actionOpenTourTypePrefs);

         _menu = menuManager.createContextMenu(parent);
         return _menu;
      }

      @Override
      public Menu getMenu(final Menu arg0) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public void runWithEvent(final Event event) {
         if (event.widget instanceof ToolItem) {
            final ToolItem toolItem = (ToolItem) event.widget;
            final Control control = toolItem.getParent();
            final Menu menu = getMenu(control);

            final Rectangle bounds = toolItem.getBounds();
            final Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
            menu.setLocation(control.toDisplay(topLeft));
            menu.setVisible(true);
         }
      }
   }

   private class ActionTourType_Remove extends Action {

      public ActionTourType_Remove() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Pref_TrainingStress_Govss_RemoveTourType);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
      }

      @Override
      public void run() {

         final int selectedIndex = _tourTypesViewer.getTable().getSelectionIndex();

         final TourType _selectedTourType = (TourType) _tourTypesViewer.getElementAt(selectedIndex);
         _tourTypesViewer.remove(_selectedTourType);

         final int listSize = _tourTypesViewer.getTable().getItemCount();
         final int newSelectedIndex = selectedIndex >= listSize ? listSize - 1 : selectedIndex;
         _tourTypesViewer.getTable().setSelection(newSelectedIndex);

         enableControls();
      }

   }

   private void createActions() {

      _action_TourType_Add = new ActionTourType_Add();
      _action_TourType_Remove = new ActionTourType_Remove();

      _actionOpenTourTypePrefs = new ActionOpenPrefDialog(
            Messages.action_tourType_modify_tourTypes,
            ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);
      createActions();

      final Composite ui = createUI(parent);
      createMenus();

      enableControls();

      restoreState();

      return ui;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */
   private void createMenus() {

      /*
       * Context menu: Tour type
       */
      final MenuManager menuMgr = new MenuManager();
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager menuMgr) {
            fillTourTypeMenu(menuMgr);
         }
      });
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 15).applyTo(container);
      {
         /*
          * label: Training stress info
          */
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory.fillDefaults().hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT).applyTo(label);
         label.setText(Messages.Training_Stress_Label_Info);

         /*
          * tab folder: training stress
          */
         _tabFolder = new TabFolder(container, SWT.TOP);
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            //tab GOVSS
            final TabItem tabGovss = new TabItem(_tabFolder, SWT.NONE);
            tabGovss.setControl(createUI_100_Govss(_tabFolder));
            tabGovss.setText(Messages.Training_Stress_Group_Govss);
         }
      }

      return _tabFolder;
   }

   /**
    * UI for the GOVSS tab
    */
   private Control createUI_100_Govss(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(container);
      {
         createUI_110_ThresholdPower(container);
         createUI_120_TourTypesList(container);
      }

      return container;

   }

   /**
    * UI for the threshold power group
    */
   private void createUI_110_ThresholdPower(final Composite parent) {

      final Group container = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText(Messages.Pref_TrainingStress_Group_ThresholdPower);
      GridLayoutFactory.swtDefaults().numColumns(6).applyTo(container);
      {
         {
            // label : Time
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_ThresholdPower_Label_Duration);
            GridDataFactory.fillDefaults()
                  .applyTo(label);

            // text
            _textThresholdPower_Duration = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
            _textThresholdPower_Duration.setToolTipText("");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _textThresholdPower_Duration.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onComputeThresholdPower();
               }
            });
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_textThresholdPower_Duration);

            // label:
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_LABEL_TIME);

            // label : Time
            label = new Label(container, SWT.NONE);
            label.setText("Threshold velocity");//Messages.Pref_ThresholdPower_Label_Duration);
            label.setFont(_boldFont);
            GridDataFactory.fillDefaults()
                  .indent(60, 0)
                  .applyTo(label);

            // text
            label = new Label(container, SWT.NONE);
            label.setText("6");//Messages.Pref_ThresholdPower_Label_Duration);
            label.setFont(_boldFont);
            GridDataFactory.fillDefaults()
                  .applyTo(label);

            // label:
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_LABEL_PACE);
            label.setFont(_boldFont);

            // label : Distance
            label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_ThresholdPower_Label_Distance);
            GridDataFactory.fillDefaults()
                  .applyTo(label);

            // text
            _spinnerThresholdPower_Distance = new Spinner(container, SWT.BORDER);
            _spinnerThresholdPower_Distance.setToolTipText("");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _spinnerThresholdPower_Distance.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onComputeThresholdPower();
               }
            });
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_spinnerThresholdPower_Distance);

            // label: m or mi
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_LABEL_DISTANCE);

            // label : Time
            label = new Label(container, SWT.NONE);
            label.setText("Threshold Power");//Messages.Pref_ThresholdPower_Label_Duration);
            GridDataFactory.fillDefaults()
                  .indent(60, 0)
                  .applyTo(label);

            // text
            _labelThresholdPower_Value = new Label(container, SWT.CENTER);
            _labelThresholdPower_Value.setText("345W");//Messages.Pref_ThresholdPower_Label_Duration);
            GridDataFactory.fillDefaults()
                  .indent(60, 0)
                  .applyTo(_labelThresholdPower_Value);

            // label:
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_POWER);

            // label : average slope
            label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_ThresholdPower_Label_AverageSlope);
            GridDataFactory.fillDefaults()
                  .applyTo(label);

            // text
            _spinnerThresholdPower_AverageSlope = new Spinner(container, SWT.BORDER);
            _spinnerThresholdPower_AverageSlope.setToolTipText("");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _spinnerThresholdPower_AverageSlope.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onComputeThresholdPower();
               }
            });
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_spinnerThresholdPower_AverageSlope);
         }
      }

   }

   /**
    * UI for the list of tour types
    */
   private void createUI_120_TourTypesList(final Composite parent) {

      final Group container = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText("Tour types");//Messages.Pref_Appearance_Group_Tagging);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {
         {
            // Toolbar
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_action_TourType_Add);
            tbm.add(_action_TourType_Remove);

            tbm.update(true);

            // Table
            final TableLayoutComposite layouter = new TableLayoutComposite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).hint(100, 100).applyTo(layouter);

            final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
            table.setHeaderVisible(false);
            table.setLinesVisible(false);
            _tourTypesViewer = new TableViewer(table);

            TableViewerColumn tvc;

            // column: image + name
            tvc = new TableViewerColumn(_tourTypesViewer, SWT.NONE);
            tvc.setLabelProvider(new CellLabelProvider() {
               @Override
               public void update(final ViewerCell cell) {

                  final TourType tourType = ((TourType) cell.getElement());

                  final String filterName = tourType.getName();
                  final Image filterImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());

                  cell.setText(filterName);
                  cell.setImage(filterImage);
               }
            });
            layouter.addColumnData(new ColumnWeightData(1));

            _tourTypesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
               @Override
               public void selectionChanged(final SelectionChangedEvent event) {
                  final StructuredSelection selection = (StructuredSelection) event.getSelection();
                  if (selection != null) {
                     enableControls();
                  }
               }
            });
         }
      }

   }

   private void enableControls() {

      final StructuredSelection currentTourTypeViewerSelection = (StructuredSelection) _tourTypesViewer.getSelection();
      final boolean isTourTypeSelected = currentTourTypeViewerSelection.isEmpty() ? false : true;
      _action_TourType_Remove.setEnabled(isTourTypeSelected);
   }

   private void fillTourTypeMenu(final IMenuManager menuMgr) {

      // add all tour types to the menu
      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

      for (final TourType tourType : tourTypes) {

         final boolean isChecked = false;
         final Action_TourType action = new Action_TourType(tourType, isChecked);

         menuMgr.add(action);
      }

      menuMgr.add(new Separator());
      menuMgr.add(_actionOpenTourTypePrefs);
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      DEFAULT_DESCRIPTION_WIDTH = _pc.convertWidthInCharsToPixels(80);
      _hintDefaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(UI.IS_OSX ? 10 : 5);

   }

   private void onComputeThresholdPower() {

      final TourPerson tourPerson = TourbookPlugin.getActivePerson();
      final Running_Govss _unning_Govss = new Running_Govss(tourPerson);
      final double thresholdPower = _unning_Govss.ComputePower(_spinnerThresholdPower_Distance.getSelection(),
            _spinnerThresholdPower_AverageSlope.getSelection(),
            0f,
            4.13f);

      _labelThresholdPower_Value.setText(String.valueOf(thresholdPower));
   }

   @Override
   protected void performDefaults() {
      _textThresholdPower_Duration.setHours(_prefStore.getDefaultInt(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_HOURS));
      _textThresholdPower_Duration.setMinutes(_prefStore.getDefaultInt(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_MINUTES));
      _textThresholdPower_Duration.setSeconds(_prefStore.getDefaultInt(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_SECONDS));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         saveState();
      }

      return isOK;
   }

   private void restoreState() {

      _textThresholdPower_Duration.setHours(_prefStore.getInt(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_HOURS));
      _textThresholdPower_Duration.setMinutes(_prefStore.getInt(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_MINUTES));
      _textThresholdPower_Duration.setSeconds(_prefStore.getInt(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_SECONDS));

   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_HOURS, _textThresholdPower_Duration.getHours());
      _prefStore.setValue(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_MINUTES, _textThresholdPower_Duration.getMinutes());
      _prefStore.setValue(ITourbookPreferences.TRAININGSTRESS_GOVSS_THRESHOLD_POWER_DURATION_SECONDS, _textThresholdPower_Duration.getSeconds());
   }
}
