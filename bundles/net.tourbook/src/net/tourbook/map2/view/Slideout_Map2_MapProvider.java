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
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.EmptyContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * 2D Map provider slideout
 */
public class Slideout_Map2_MapProvider extends AdvancedSlideout implements IColorSelectorListener, ITourViewer {

// SET_FORMATTING_OFF

   private static final String MAP_ACTION_MANAGE_MAP_PROVIDERS                         = net.tourbook.map2.Messages.Map_Action_ManageMapProviders;
   private static final String PREF_MAP_VIEWER_COLUMN_LBL_MAP_PROVIDER                 = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_Lbl_MapProvider;

// SET_FORMATTING_ON

   final static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   final private IDialogSettings _state;
   private SelectionAdapter      _defaultState_SelectionListener;

   private ActionOpenPrefDialog  _action_ManageMapProviders;

   private Action                _action_MapProvider_Next;
   private Action                _action_MapProvider_Previous;
   private Map2View              _map2View;

   private CheckboxTableViewer   _mpViewer;
   private ColumnManager         _columnManager;
   //   private final MapProviderManager _mpMgr     = MapProviderManager.getInstance();
   private MP                    _selectedMP;

   private ArrayList<MP>         _allSortedMP;
   private PixelConverter        _pc;

   /** Ignore selection event */
   private boolean               _isInUpdate;

   /*
    * UI controls
    */
   private Composite _parent;

   private Composite _viewerContainer;
   private ToolItem  _toolItem;

   public class ActionOpenMapProviderPreferences extends ActionOpenPrefDialog {

      public ActionOpenMapProviderPreferences(final String text, final String prefPageId) {
         super(text, prefPageId);
      }
   }

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
   public Slideout_Map2_MapProvider(final ToolItem toolItem, final Map2View map2View, final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 100, 500, 100, 500 });

      _toolItem = toolItem;
      _map2View = map2View;
      _state = state;

      setTitleText(Messages.Slideout_Map_Provider_Label_Title);
   }

   @Override
   protected void beforeShellVisible(final boolean isVisible) {

      /**
       * Context menu must be set lately, otherwise an "Widget has the wrong parent" exception
       * occures
       */
      if (isVisible) {
         _columnManager.createHeaderContextMenu(_mpViewer.getTable(), null, getRRShellWithResize());
      }
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
         _action_ManageMapProviders = new ActionOpenMapProviderPreferences(MAP_ACTION_MANAGE_MAP_PROVIDERS, PrefPage_Map2_Providers.ID) {

            @Override
            public void run() {

               // set the currently displayed map provider so that this mp will be selected in the pref page
               _prefStore.setValue(
                     IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER,
                     _map2View.getMap().getMapProvider().getId());

               super.run();
            }
         };

         _action_ManageMapProviders.closeThisTooltip(this);
         _action_ManageMapProviders.setShell(_map2View.getMap().getShell());
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

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createActions();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      // load viewer
      createSortedMapProviders();
      _mpViewer.setInput(new Object());

      restoreState();
      enableControls();

      // set focus to map viewer
      _mpViewer.getTable().setFocus();
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

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
//         createUI_10_SlideoutHeader(shellContainer);

         _viewerContainer = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_MapViewer(_viewerContainer);
         }
      }

      return shellContainer;
   }

   private void createUI_10_SlideoutHeader(final Composite parent) {

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
      GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * create table
       */
      final Table table = new Table(layoutContainer, SWT.CHECK | SWT.FULL_SELECTION);

//      table.setLayout(new TableLayout());
      table.setHeaderVisible(true);
      table.setLinesVisible(false);

      _mpViewer = new CheckboxTableViewer(table);
      _mpViewer.setUseHashlookup(true);

      _columnManager.setColumnLayout(tableLayout);
      _columnManager.createColumns(_mpViewer);
      _columnManager.setSlideoutShell(this);

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

      createUI_22_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_22_ContextMenu() {

      final Table table = _mpViewer.getTable();

      _columnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());
   }

   private void defineAllColumns() {

      defineColumn_10_MapProvider();
      defineColumn_20_ServerType();
   }

   /**
    * Column: Map Provider, this is the first column with a checkbox
    */
   private void defineColumn_10_MapProvider() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, "MapProvider", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnName(PREF_MAP_VIEWER_COLUMN_LBL_MAP_PROVIDER);

      colDef.setCanModifyVisibility(false);
      colDef.setIsColumnMoveable(false);
      colDef.setIsDefaultColumn();
      colDef.setColumnWeightData(new ColumnWeightData(20));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.getName());
         }
      });
   }

   /**
    * Column: Server type
    */
   private void defineColumn_20_ServerType() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, "ServerType", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnName(Messages.Slideout_Map2Provider_Column_ServerType);

      colDef.setIsDefaultColumn();
      colDef.setColumnWeightData(new ColumnWeightData(5));
//      colDef.setColumnWidth(_pc.convertWidthInCharsToPixels(4));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            if (mapProvider instanceof MPWms) {
               cell.setText(Messages.Slideout_Map2Provider_Column_ServerType_WMS);
            } else if (mapProvider instanceof MPCustom) {
               cell.setText(Messages.Slideout_Map2Provider_Column_ServerType_Custom);
            } else if (mapProvider instanceof MPProfile) {
               cell.setText(Messages.Slideout_Map2Provider_Column_ServerType_Profile);
            } else if (mapProvider instanceof MPPlugin) {
               cell.setText(Messages.Slideout_Map2Provider_Column_ServerType_Internal);
            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   private void enableControls() {

   }

   @Override
   protected void fillHeaderToolbar(final ToolBarManager toolbarManager) {

      toolbarManager.add(_action_MapProvider_Next);
      toolbarManager.add(_action_MapProvider_Previous);
      toolbarManager.add(_action_ManageMapProviders);

      toolbarManager.add(new Separator());
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   @Override
   public ColumnViewer getViewer() {
      return _mpViewer;
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

   @Override
   protected void onFocus() {

      _mpViewer.getTable().setFocus();
   }

   private void onSelect_MapProvider(final SelectionChangedEvent event) {

      if (_isInUpdate) {
         return;
      }

      final MP selectedMapProvider = (MP) event.getStructuredSelection().getFirstElement();

      selectMapProviderInTheMap(selectedMapProvider);
   }

   private void onSelect_MapProvider_Next() {

   }

   private void onSelect_MapProvider_Previous() {

   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _mpViewer.getSelection();
         final Object[] checkedElements = _mpViewer.getCheckedElements();
         {
            _mpViewer.getTable().getParent().dispose();

            createUI_20_MapViewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            reloadViewer();
         }
         updateUI_ReselectItems(selectionBackup, checkedElements);
      }
      _viewerContainer.setRedraw(true);

      _mpViewer.getTable().setFocus();

      return _mpViewer;
   }

   @Override
   public void reloadViewer() {

      updateUI_SetViewerInput();
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

   @Override
   protected void saveState() {

      // save UI

      _columnManager.saveState(_state);

      super.saveState();
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

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Select and reveal the previous items.
    *
    * @param selection
    * @param checkedElements
    */
   private void updateUI_ReselectItems(final ISelection selection, final Object[] checkedElements) {

      _isInUpdate = true;
      {
         _mpViewer.setSelection(selection, true);

         if (checkedElements != null && checkedElements.length > 0) {
            _mpViewer.setCheckedElements(checkedElements);
         }

         final Table table = _mpViewer.getTable();
         table.showSelection();
      }
      _isInUpdate = false;
   }

   private void updateUI_SetViewerInput() {

      _isInUpdate = true;
      {
         _mpViewer.setInput(new Object[0]);
      }
      _isInUpdate = false;
   }
}
