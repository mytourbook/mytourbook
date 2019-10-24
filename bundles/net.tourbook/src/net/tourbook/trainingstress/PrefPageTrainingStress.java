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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTrainingStress extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String    ID           = "net.tourbook.trainingstress.PrefPageTrainingStress"; //$NON-NLS-1$

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

   /*
    * UI controls
    */
   private TableViewer _tourTypesViewer;
   private TabFolder   _tabFolder;

   /*
    * private Button _chkConvertWayPoints;
    * private Button _chkOneTour;
    * private Button _rdoDistanceRelative;
    * private Button _rdoDistanceAbsolute;
    */
   private Label _labelCriticalVelocity;
   private Text  _textApiKey;

   private class Action_TourType extends Action {

      private TourType _tourType;

      /**
       * @param tourType
       */
      public Action_TourType(final TourType tourType, final boolean isChecked) {

         super(tourType.getName(), AS_CHECK_BOX);

         if (isChecked == false) {

            // show image when tour type can be selected, disabled images look ugly on win
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

         setToolTipText(Messages.Dialog_ImportConfig_Action_AddSpeed_Tooltip);
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
         // add all tour types to the menu
         final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
         final MenuManager menuManager = new MenuManager();
         for (final TourType tourType : tourTypes) {

            final boolean isChecked = false;
            //TODO if tour type is already in the list, then we don't add it or gray it. Is that what tge isChecked is for ?
            //TODO make a function isTourTypeAlreadySelected()
            //final TourType[] toto = _tourTypesViewer.getTable().getItems();

            final Action_TourType action = new Action_TourType(tourType, isChecked);

            menuManager.add(action);
         }
         // TODO add your menu items

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

         setToolTipText(Messages.Dialog_ImportConfig_Action_AddSpeed_Tooltip);
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
      // final Menu ttContextMenu = menuMgr.createContextMenu(_linkTT_One_TourType);
      // _action_TourType_Add.setMenuCreator(ttContextMenu);
      //  _linkTT_One_TourType.setMenu(ttContextMenu);
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
          * label: info
          */
         final Label label = new Label(container, SWT.WRAP);
         GridDataFactory.fillDefaults().hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT).applyTo(label);
         label.setText(Messages.Compute_Values_Label_Info);

         /*
          * tab folder: computed values
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
            tabGovss.setText("GOVSS");//Messages.Compute_Values_Group_Smoothing);
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
         createUI_110_CriticalVelocity(container);
         createUI_120_TourTypesList(container);
      }

      return container;

   }

   /**
    * UI for the critical velocity group
    */
   private void createUI_110_CriticalVelocity(final Composite parent) {

      final Group container = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText(Messages.Pref_Appearance_Group_Tagging);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         {
            // label
            _labelCriticalVelocity = new Label(container, SWT.NONE);
            _labelCriticalVelocity.setText("Critical velocity");//Messages.Pref_Weather_Label_ApiKey);
            GridDataFactory.fillDefaults()
                  .applyTo(_labelCriticalVelocity);

            // text
            _textApiKey = new Text(container, SWT.BORDER);
            _textApiKey.setToolTipText(Messages.Pref_Weather_Label_ApiKey_Tooltip);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_textApiKey);

            // label: min/mile or km/h
            final Label label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_LABEL_PACE);
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
            GridDataFactory.fillDefaults().grab(true, true).hint(200, SWT.DEFAULT).applyTo(layouter);

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

   @Override
   protected void performDefaults() {

      // merge all tracks into one tour
      //	_chkOneTour.setSelection(RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);

      // convert waypoints
      //	_chkConvertWayPoints.setSelection(RawDataView.STATE_IS_CONVERT_WAYPOINTS_DEFAULT);

      // relative/absolute distance
      //final boolean isRelativeDistance = _prefStore.getDefaultBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

      //_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
      //	_rdoDistanceRelative.setSelection(isRelativeDistance);

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

      /*
       * // merge all tracks into one tour
       * final boolean isMergeIntoOneTour = Util.getStateBoolean(
       * _importState,
       * RawDataView.STATE_IS_MERGE_TRACKS,
       * RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);
       * //_chkOneTour.setSelection(isMergeIntoOneTour);
       * // convert waypoints
       * final boolean isConvertWayPoints = Util.getStateBoolean(
       * _importState,
       * RawDataView.STATE_IS_CONVERT_WAYPOINTS,
       * RawDataView.STATE_IS_CONVERT_WAYPOINTS_DEFAULT);
       */
      //_chkConvertWayPoints.setSelection(isConvertWayPoints);

      // relative/absolute distance
      //final boolean isRelativeDistance = _prefStore.getBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

      //	_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
      //	_rdoDistanceRelative.setSelection(isRelativeDistance);
   }

   private void saveState() {

      // merge all tracks into one tour
      //	final boolean isMergeIntoOneTour = _chkOneTour.getSelection();
      //	_importState.put(RawDataView.STATE_IS_MERGE_TRACKS, isMergeIntoOneTour);
      //	_rawDataMgr.setMergeTracks(isMergeIntoOneTour);

      // convert waypoints
      //	final boolean isConvertWayPoints = _chkConvertWayPoints.getSelection();
//		_importState.put(RawDataView.STATE_IS_CONVERT_WAYPOINTS, isConvertWayPoints);
      //	_rawDataMgr.setState_ConvertWayPoints(isConvertWayPoints);

      // relative/absolute distance
      //_prefStore.setValue(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE, _rdoDistanceRelative.getSelection());
   }
}
