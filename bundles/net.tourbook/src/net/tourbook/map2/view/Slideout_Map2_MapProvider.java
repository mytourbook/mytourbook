/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MPCustom;
import de.byteholder.geoclipse.mapprovider.MPPlugin;
import de.byteholder.geoclipse.mapprovider.MPProfile;
import de.byteholder.geoclipse.mapprovider.MPWms;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.preferences.PrefPage_Map2_Providers;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

/**
 * 2D Map provider slideout
 */
public class Slideout_Map2_MapProvider extends ToolbarSlideout implements IColorSelectorListener {

// SET_FORMATTING_OFF

   private static final String PREF_MAP_VIEWER_COLUMN_LBL_MAP_PROVIDER                 = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_Lbl_MapProvider;
   private static final String PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_PLUGIN       = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_ContentServerTypePlugin;
   private static final String PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_MAP_PROFILE  = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_ContentServerTypeMapProfile;
   private static final String PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_CUSTOM       = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_ContentServerTypeCustom;
   private static final String PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_WMS          = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_ContentServerTypeWms;
   private static final String PREF_MAP_VIEWER_COLUMN_LBL_SERVER_TYPE_TOOLTIP          = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_Lbl_ServerType_Tooltip;

   private static final String MAP_ACTION_MANAGE_MAP_PROVIDERS                         = net.tourbook.map2.Messages.Map_Action_ManageMapProviders;

// SET_FORMATTING_ON

   final static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   final private IDialogSettings _state;

   private SelectionAdapter      _defaultState_SelectionListener;

   private ActionOpenPrefDialog  _action_ManageMapProviders;
   private Action                _action_MapProvider_Next;
   private Action                _action_MapProvider_Previous;

   private Map2View              _map2View;
   private TableViewer           _mpViewer;

//   private final MapProviderManager _mpMgr     = MapProviderManager.getInstance();
   private MP             _selectedMP;
   private ArrayList<MP>  _allSortedMP;

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Composite _parent;

   private class MapContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allSortedMP.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public Slideout_Map2_MapProvider(final Control ownerControl,
                                    final ToolBar toolBar,
                                    final Map2View map2View,
                                    final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {
      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      {
         /*
          * Action: Manage map providers
          */
         _action_ManageMapProviders = new ActionOpenPrefDialog(
               MAP_ACTION_MANAGE_MAP_PROVIDERS,
               PrefPage_Map2_Providers.ID);

         _action_ManageMapProviders.closeThisTooltip(this);
         _action_ManageMapProviders.setShell(_map2View.getMap().getShell());

         // set the currently displayed map provider so that this mp will be selected in the pref page
         _prefStore.setValue(IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER, _map2View.getMap().getMapProvider().getId());
      }

      {
         /*
          * Action: Next map provider
          */
         _action_MapProvider_Next = new Action() {
            @Override
            public void run() {
               onSelect_MapProvider_Next();
            }
         };

         _action_MapProvider_Next.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown));
         _action_MapProvider_Next.setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown_Disabled));
         _action_MapProvider_Next.setToolTipText(Messages.Slideout_Map2Provider_MapProvider_Next_Tooltip);
      }
      {
         /*
          * Action: Previous map provider
          */
         _action_MapProvider_Previous = new Action() {
            @Override
            public void run() {
               onSelect_MapProvider_Previous();
            }
         };

         _action_MapProvider_Previous.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowUp));
         _action_MapProvider_Previous.setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowUp_Disabled));
         _action_MapProvider_Previous.setToolTipText(Messages.Slideout_Map2Provider_MapProvider_Previous_Tooltip);
      }
   }

   /**
    * Create a list with all available map providers, sorted by preference settings
    */
   private void createSortedMapProviders() {

      final ArrayList<MP> allMapProvider = MapProviderManager.getInstance().getAllMapProviders(true);

      final String[] storedMpIds = StringToArrayConverter.convertStringToArray(_prefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

      _allSortedMP = new ArrayList<>();

      // put all map providers into the viewer which are defined in the pref store
      for (final String storeMpId : storedMpIds) {

         // find the stored map provider in the available map providers
         for (final MP mp : allMapProvider) {
            if (mp.getId().equals(storeMpId)) {
               _allSortedMP.add(mp);
               break;
            }
         }
      }

      // make sure that all available map providers are in the viewer
      for (final MP mp : allMapProvider) {
         if (!_allSortedMP.contains(mp)) {
            _allSortedMP.add(mp);
         }
      }
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      // load viewer
      createSortedMapProviders();
      _mpViewer.setInput(new Object());

      restoreState();
      enableControls();

      // set focus to map viewer
      _mpViewer.getTable().setFocus();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
//         createUI_20_MapOptions(shellContainer);
         createUI_20_MapViewer(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_Map_Provider_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_action_MapProvider_Next);
            tbm.add(_action_MapProvider_Previous);
            tbm.add(_action_ManageMapProviders);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_MapViewer(final Composite parent) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()
            .hint(
                  _pc.convertWidthInCharsToPixels(50),
                  _pc.convertHeightInCharsToPixels((int) (20 * 1.4)))
            .applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * create table
       */
      final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

      table.setLayout(new TableLayout());
      table.setHeaderVisible(true);
      table.setLinesVisible(false);

      _mpViewer = new TableViewer(table);
      _mpViewer.setUseHashlookup(true);

      /*
       * create columns
       */
      TableViewerColumn tvc;
      TableColumn tc;

      /*
       * Column: Server type
       */
      tvc = new TableViewerColumn(_mpViewer, SWT.LEAD);
      tc = tvc.getColumn();
      tc.setToolTipText(PREF_MAP_VIEWER_COLUMN_LBL_SERVER_TYPE_TOOLTIP);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            if (mapProvider instanceof MPWms) {
               cell.setText(PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_WMS);
            } else if (mapProvider instanceof MPCustom) {
               cell.setText(PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_CUSTOM);
            } else if (mapProvider instanceof MPProfile) {
               cell.setText(PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_MAP_PROFILE);
            } else if (mapProvider instanceof MPPlugin) {
               cell.setText(PREF_MAP_VIEWER_COLUMN_CONTENT_SERVER_TYPE_PLUGIN);
            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      tableLayout.setColumnData(tvc.getColumn(), new ColumnPixelData(_pc.convertWidthInCharsToPixels(4)));

      /*
       * Column: Map provider
       */
      tvc = new TableViewerColumn(_mpViewer, SWT.LEAD);
      tc = tvc.getColumn();
      tc.setText(PREF_MAP_VIEWER_COLUMN_LBL_MAP_PROVIDER);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.getName());
         }
      });
      tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(20));

      /*
       * create table viewer
       */
      _mpViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public Object[] getElements(final Object inputElement) {
            return _allSortedMP.toArray();
         }
      });

      _mpViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_MapProvider(event);
         }
      });

   }

   private void createUI_30_MapOptions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {}
   }

   private void enableControls() {

   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _defaultState_SelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();

//      _map2View.restoreState_Map2_Options();
   }

   private void onSelect_MapProvider(final SelectionChangedEvent event) {

      final MP selectedMapProvider = (MP) event.getStructuredSelection().getFirstElement();

      selectMapProviderInTheMap(selectedMapProvider);
   }

   private void onSelect_MapProvider_Next() {

   }

   private void onSelect_MapProvider_Previous() {

   }

   private void resetToDefaults() {

// SET_FORMATTING_OFF

//      _chkIsShowHoveredTour.setSelection(          Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);
//      _chkIsZoomWithMousePosition.setSelection(    Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT);

// SET_FORMATTING_ON

      onChangeUI();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

//      _chkIsShowHoveredTour.setSelection(       Util.getStateBoolean(_state,    Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT));
//      _chkIsZoomWithMousePosition.setSelection( Util.getStateBoolean(_state,    Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));

// SET_FORMATTING_ON

      // select map provider
      final MP currentMP = _map2View.getMap().getMapProvider();

      _mpViewer.setSelection(new StructuredSelection(currentMP), true);
   }

   private void saveState() {

// SET_FORMATTING_OFF

//      _state.put(Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, _chkIsShowHoveredTour.getSelection());
//      _state.put(Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   _chkIsZoomWithMousePosition.getSelection());

// SET_FORMATTING_ON
   }

   private void selectMapProviderInTheMap(final MP mp) {

      // check if a new map provider is selected
      if (_selectedMP != null && _selectedMP == mp) {
         return;
      }

      _selectedMP = mp;

      final Map map = _map2View.getMap();

      map.setMapProvider(mp);

      // set map dim level
      final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
      final RGB dimColor = PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR);
      map.setDimLevel(_map2View.getMapDimLevel(), dimColor);
   }

}
