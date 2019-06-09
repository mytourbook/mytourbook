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
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.EmptyContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * 2D Map provider slideout
 */
public class Slideout_Map2_MapProvider extends AdvancedSlideout implements IColorSelectorListener, ITourViewer {

// SET_FORMATTING_OFF

   private static final String MAP_ACTION_MANAGE_MAP_PROVIDERS             = net.tourbook.map2.Messages.Map_Action_ManageMapProviders;
   private static final String PREF_MAP_VIEWER_COLUMN_LBL_MAP_PROVIDER     = de.byteholder.geoclipse.preferences.Messages.Pref_Map_Viewer_Column_Lbl_MapProvider;

// SET_FORMATTING_ON

   final static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   final private IDialogSettings _state;

   private ActionOpenPrefDialog  _action_ManageMapProviders;
   private Action                _action_MapProvider_Next;
   private Action                _action_MapProvider_Previous;

   private Map2View              _map2View;
   private TableViewer           _mpViewer;
   private ColumnManager         _columnManager;
   private TableColumnDefinition _colDef_IsVisible;

   private long                  _dndDragStartViewerLeft;

   private MP                    _selectedMP;
   private ArrayList<MP>         _allVisibleAndSortedMP;

   /** Ignore selection event */
   private boolean               _isInUpdate;
   private boolean               _isShowHiddenMapProvider;

   private Button                _btnHideUnhideMP;

   private PixelConverter        _pc;

   /*
    * UI controls
    */
   private Composite _viewerContainer;
   private ToolItem  _toolItem;

   private class ActionOpenMapProviderPreferences extends ActionOpenPrefDialog {

      public ActionOpenMapProviderPreferences(final String text, final String prefPageId) {
         super(text, prefPageId);
      }
   }

   private final class MapProviderVisible_EditingSupport extends EditingSupport {

      private final CheckboxCellEditor __cellEditor;

      private MapProviderVisible_EditingSupport() {

         super(_mpViewer);

         __cellEditor = new CheckboxCellEditor(_mpViewer.getTable());
      }

      @Override
      protected boolean canEdit(final Object element) {
         return true;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {

         return __cellEditor;
      }

      @Override
      protected Object getValue(final Object element) {

         return ((MP) element).isVisibleInUI() ? Boolean.TRUE : Boolean.FALSE;
      }

      @Override
      protected void setValue(final Object element, final Object value) {

         final MP mp = (MP) element;
         final Boolean isVisible = (Boolean) value;

         // update model
         mp.setIsVisibleInUI(isVisible);

         // update UI
         _mpViewer.update(mp, null);
      }
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

      /*
       * Create map provider list very early as this list will be used to get the selected or
       * default map provider for the map.
       */
      createSortedMapProviders();
   }

   private void action_HideUnhideMapProvider() {

      _isShowHiddenMapProvider = !_isShowHiddenMapProvider;

      updateUI_HideUnhideMapProviders();

      _mpViewer.refresh();
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

      final ArrayList<String> storedMpIds = Util.convertStringToList(_prefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

      _allVisibleAndSortedMP = new ArrayList<>();

      int mpSortIndex = 0;

      // put all map providers into the viewer which are defined in the pref store
      for (final String storeMpId : storedMpIds) {

         // find the stored map provider in the available map providers
         for (final MP mp : allMapProvider) {
            if (mp.getId().equals(storeMpId)) {

               mp.setSortIndex(mpSortIndex++);

               _allVisibleAndSortedMP.add(mp);
               break;
            }
         }
      }

      // make sure that all available map providers are in the viewer
      for (final MP mp : allMapProvider) {
         if (!_allVisibleAndSortedMP.contains(mp)) {
            _allVisibleAndSortedMP.add(mp);
         }
      }

      // ensure that one mp is available
      if (_allVisibleAndSortedMP.size() == 0) {
         _allVisibleAndSortedMP.add(MapProviderManager.getDefaultMapProvider());
      }
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {

         _viewerContainer = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_MapViewer(_viewerContainer);
         }

         createUI_30_Hint(shellContainer);
         createUI_90_Actions(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_20_MapViewer(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(false);

      _mpViewer = new TableViewer(table);

      // very important: the editing support must be set BEFORE the columns are created
      _colDef_IsVisible.setEditingSupport(new MapProviderVisible_EditingSupport());

      _columnManager.createColumns(_mpViewer);
      _columnManager.setSlideoutShell(this);

      _mpViewer.setUseHashlookup(true);

      /*
       * create table viewer
       */
      _mpViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public Object[] getElements(final Object inputElement) {
            return _allVisibleAndSortedMP.toArray();
         }
      });

      _mpViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_MapProvider(event);
         }
      });

      _mpViewer.setFilters(new ViewerFilter() {

         @Override
         public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

            if (_isShowHiddenMapProvider) {
               return true;
            }

            return ((MP) element).isVisibleInUI();
         }
      });

      /**
       * When a viewer filter is used, then a comparator MUST be set,
       * otherwise it is wrongly sorted !!!
       */
      _mpViewer.setComparator(new ViewerComparator() {

         @Override
         public int compare(final Viewer viewer, final Object e1, final Object e2) {

            final MP mp1 = (MP) e1;
            final MP mp2 = (MP) e2;

            return mp1.getSortIndex() - mp2.getSortIndex();
         }
      });

      /*
       * Set drag adapter
       */
      _mpViewer.addDragSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            new DragSourceListener() {

               @Override
               public void dragFinished(final DragSourceEvent event) {

                  final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

                  if (event.doit == false) {
                     return;
                  }

                  transfer.setSelection(null);
                  transfer.setSelectionSetTime(0);
               }

               @Override
               public void dragSetData(final DragSourceEvent event) {
                  // data are set in LocalSelectionTransfer
               }

               @Override
               public void dragStart(final DragSourceEvent event) {

                  final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
                  final ISelection selection = _mpViewer.getSelection();

                  transfer.setSelection(selection);
                  transfer.setSelectionSetTime(_dndDragStartViewerLeft = event.time & 0xFFFFFFFFL);

                  event.doit = !selection.isEmpty();
               }
            });

      /*
       * Set drop adapter
       */
      final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_mpViewer) {

         private Widget __dragOverItem;

         @Override
         public void dragOver(final DropTargetEvent dropEvent) {

            // keep table item
            __dragOverItem = dropEvent.item;

            super.dragOver(dropEvent);
         }

         @Override
         public boolean performDrop(final Object data) {

            if (data instanceof StructuredSelection) {

               final StructuredSelection selection = (StructuredSelection) data;

               if (selection.getFirstElement() instanceof MP) {

                  // prevent selection, this occured and mapprovider was null
                  _isInUpdate = true;
                  {
                     final MP droppedMapProvider = (MP) selection.getFirstElement();

                     final int location = getCurrentLocation();
                     final Table mpTable = _mpViewer.getTable();

                     /*
                      * Check if drag was startet from this item, remove the item before the new
                      * item is inserted
                      */
                     if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dndDragStartViewerLeft) {
                        _mpViewer.remove(droppedMapProvider);
                     }

                     int mpIndex;

                     if (__dragOverItem == null) {

                        // drop mp at the end

                        // update sort index before viewer is modified, this index will be adjusted when state is saved
                        droppedMapProvider.setSortIndex(Integer.MAX_VALUE);

                        _mpViewer.add(droppedMapProvider);

                        mpIndex = mpTable.getItemCount() - 1;

                     } else {

                        // get index of the target in the table
                        mpIndex = mpTable.indexOf((TableItem) __dragOverItem);
                        if (mpIndex == -1) {
                           return false;
                        }

                        if (location == LOCATION_BEFORE) {

                           // update sort index before viewer is modified which runs the sorter

                           int sortIndex = 0;

                           final TableItem[] items = _mpViewer.getTable().getItems();

                           for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {

                              final TableItem item = items[itemIndex];
                              final MP tableItemMP = (MP) item.getData();

                              if (itemIndex == mpIndex) {
                                 droppedMapProvider.setSortIndex(sortIndex++);
                              }

                              tableItemMP.setSortIndex(sortIndex++);
                           }

                           _mpViewer.insert(droppedMapProvider, mpIndex);

                        } else if (location == LOCATION_AFTER || location == LOCATION_ON) {

                           // update sort index before viewer is modified which runs the sorter
                           int sortIndex = 0;

                           final TableItem[] items = _mpViewer.getTable().getItems();

                           for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {

                              final TableItem item = items[itemIndex];
                              final MP tableItemMP = (MP) item.getData();

                              tableItemMP.setSortIndex(sortIndex++);

                              if (itemIndex == mpIndex) {
                                 droppedMapProvider.setSortIndex(sortIndex++);
                              }
                           }

                           _mpViewer.insert(droppedMapProvider, ++mpIndex);
                        }
                     }

                     // reselect mp item
                     _mpViewer.setSelection(new StructuredSelection(droppedMapProvider));

                     // set focus to selection
                     mpTable.setSelection(mpIndex);
                     mpTable.setFocus();

                     // save state
                     saveState_MapProviders();

                  }
                  _isInUpdate = false;

                  return true;
               }
            }

            return false;
         }

         @Override
         public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

            final LocalSelectionTransfer transferData = LocalSelectionTransfer.getTransfer();

            // check if dragged item is the target item
            final ISelection selection = transferData.getSelection();
            if (selection instanceof StructuredSelection) {
               final Object dragMP = ((StructuredSelection) selection).getFirstElement();
               if (target == dragMP) {
                  return false;
               }
            }

            if (transferData.isSupportedType(transferType) == false) {
               return false;
            }

            // check if target is between two items
            if (getCurrentLocation() == LOCATION_ON) {
               return false;
            }

            return true;
         }

      };

      _mpViewer.addDropSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            viewerDropAdapter);

      createUI_22_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_22_ContextMenu() {

      final Table table = _mpViewer.getTable();

      _columnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());
   }

   private void createUI_30_Hint(final Composite parent) {

      // label: Hint
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map2Provider_Label_Tip);
   }

   private void createUI_90_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults()//
            .extendedMargins(5, 0, 10, 0)
            .numColumns(4)
            .applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * button: Hide/unhide map provider
             */
            _btnHideUnhideMP = new Button(container, SWT.NONE);
            _btnHideUnhideMP.setText(Messages.Slideout_Map2Provider_Button_UnhideMP);
            _btnHideUnhideMP.setToolTipText(Messages.Slideout_Map2Provider_Button_UnhideMP_Tooltip);
            _btnHideUnhideMP.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  action_HideUnhideMapProvider();
               }
            });
            UI.setButtonLayoutData(_btnHideUnhideMP);
         }
      }
   }

   private void defineAllColumns() {

      defineColumn_10_MapProvider();
      defineColumn_20_MPType();
      defineColumn_30_IsVisible();
   }

   /**
    * Column: Map Provider, this is the first column with a checkbox
    */
   private void defineColumn_10_MapProvider() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, "MapProvider", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnName(PREF_MAP_VIEWER_COLUMN_LBL_MAP_PROVIDER);

      colDef.setCanModifyVisibility(false);
      colDef.setIsColumnMoveable(false);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(40));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.getName());
         }
      });
   }

   /**
    * Column: MP type
    */
   private void defineColumn_20_MPType() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, "MPType", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnName(Messages.Slideout_Map2Provider_Column_MPType);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_Map2Provider_Column_MPType_Tooltip);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            if (mapProvider instanceof MPWms) {
               cell.setText(Messages.Slideout_Map2Provider_Column_MPType_WMS);

            } else if (mapProvider instanceof MPCustom) {
               cell.setText(Messages.Slideout_Map2Provider_Column_MPType_Custom);

            } else if (mapProvider instanceof MPProfile) {
               cell.setText(Messages.Slideout_Map2Provider_Column_MPType_Profile);

            } else if (mapProvider instanceof MPPlugin) {
               cell.setText(Messages.Slideout_Map2Provider_Column_MPType_Internal);

            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Is visible
    */
   private void defineColumn_30_IsVisible() {

      _colDef_IsVisible = new TableColumnDefinition(_columnManager, "IsVisible", SWT.LEAD); //$NON-NLS-1$

      _colDef_IsVisible.setColumnName(Messages.Slideout_Map2Provider_Column_IsVisible);
      _colDef_IsVisible.setColumnHeaderToolTipText(Messages.Slideout_Map2Provider_Column_IsVisible_Tooltip);
      _colDef_IsVisible.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      _colDef_IsVisible.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mp = (MP) cell.getElement();

            cell.setText(mp.isVisibleInUI()
                  ? Messages.App_Label_BooleanYes
                  : Messages.App_Label_BooleanNo);
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

   public MP getSelectedMapProvider() {
      return _selectedMP;
   }

   @Override
   public ColumnViewer getViewer() {
      return _mpViewer;
   }

   private void initUI(final Composite parent) {

//      _parent = parent;
//
      _pc = new PixelConverter(parent);
//
//      _defaultState_SelectionListener = new SelectionAdapter() {
//         @Override
//         public void widgetSelected(final SelectionEvent e) {
//            onChangeUI();
//         }
//      };
   }

   @Override
   protected void onFocus() {

      _mpViewer.getTable().setFocus();
   }

//   private void onChangeUI() {
//
//      saveState();
//
//      enableControls();
//   }

   private void onSelect_MapProvider(final SelectionChangedEvent event) {

      if (_isInUpdate) {
         return;
      }

      MP selectedMapProvider = (MP) event.getStructuredSelection().getFirstElement();

      if (selectedMapProvider == null) {

         // this can occure when the last selected mp is not available or filtered out

         selectedMapProvider = _allVisibleAndSortedMP.get(0);
      }

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
         {
            _mpViewer.getTable().dispose();

            createUI_20_MapViewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            reloadViewer();
         }
         updateUI_ReselectItems(selectionBackup);
      }
      _viewerContainer.setRedraw(true);

      _mpViewer.getTable().setFocus();

      return _mpViewer;
   }

   @Override
   public void reloadViewer() {

      updateUI_SetViewerInput();
   }

   private void restoreState() {

      /*
       * Check all map providers which are defined in the pref store
       */
      final ArrayList<String> storeProviderIds = Util.convertStringToList(_prefStore.getString(IMappingPreferences.MAP_PROVIDER_VISIBLE_IN_UI));

      for (final MP mapProvider : _allVisibleAndSortedMP) {

         final String mpId = mapProvider.getId();

         for (final String storedProviderId : storeProviderIds) {

            if (mpId.equals(storedProviderId)) {

               mapProvider.setIsVisibleInUI(true);
               break;
            }
         }
      }

      // select map provider in the viewer
      final MP currentMP = _map2View.getMap().getMapProvider();

      /*
       * Ensure that the map provider is also visible in the viewer
       */
      // update model
      currentMP.setIsVisibleInUI(true);

      // update UI
      _mpViewer.refresh();

      _mpViewer.setSelection(new StructuredSelection(currentMP), true);

      updateUI_HideUnhideMapProviders();
   }

   @Override
   protected void saveState() {

      // save UI

      _columnManager.saveState(_state);

      super.saveState();
   }

   /**
    * Save the ckeck state and order of the map providers
    */
   private void saveState_MapProviders() {

      /*
       * Save all checked map providers
       */

      final ArrayList<String> allVisibleMP = new ArrayList<>();

      for (final MP mp : _allVisibleAndSortedMP) {
         if (mp.isVisibleInUI()) {
            allVisibleMP.add(mp.getId());
         }
      }

      _prefStore.setValue(IMappingPreferences.MAP_PROVIDER_VISIBLE_IN_UI, Util.convertListToString(allVisibleMP));

      /*
       * Save order of all map providers
       */
      final TableItem[] items = _mpViewer.getTable().getItems();
      final ArrayList<String> allSortedMapProviderIds = new ArrayList<>();

      for (final TableItem item : items) {
         final MP mapProvider = (MP) item.getData();
         allSortedMapProviderIds.add(mapProvider.getId());
      }

      _prefStore.setValue(IMappingPreferences.MAP_PROVIDER_SORT_ORDER, Util.convertListToString(allSortedMapProviderIds));

      // update internal sorted map provider list with new sorting
      createSortedMapProviders();
   }

   /**
    * Select a map provider by it's map provider ID
    *
    * @param selectedMpId
    *           map provider id or <code>null</code> to select the default factory (OSM)
    */
   public void selectMapProvider(final String selectedMpId) {

      /*
       * Find map provider by it's id
       */
      if (selectedMpId != null) {

         for (final MP mp : _allVisibleAndSortedMP) {

            // check mp ID
            if (mp.getId().equals(selectedMpId)) {

               // map provider is available
               selectMapProvider_Internal(mp);

               return;
            }
         }
      }

      /*
       * If map provider is not set, get default map provider
       */
      for (final MP mp : _allVisibleAndSortedMP) {

         if (mp.getId().equals(MapProviderManager.DEFAULT_MAP_PROVIDER_ID)) {

            // map provider is available
            selectMapProvider_Internal(mp);

            return;
         }
      }

      /*
       * if map provider is not set, get first one
       */
      final MP mp = _allVisibleAndSortedMP.get(0);
      if (mp != null) {

         // map provider is available
         selectMapProvider_Internal(mp);
      }
   }

   private void selectMapProvider_Internal(final MP mp) {

      if (_mpViewer != null) {

         // _mpViewer is null when map is started

         _isInUpdate = true;
         {
            if (mp.isVisibleInUI() == false) {

               // map provider must be visible

               // update model
               mp.setIsVisibleInUI(true);

               // update UI
               _mpViewer.refresh();
            }

            _mpViewer.setSelection(new StructuredSelection(mp), true);
         }
         _isInUpdate = false;
      }

      selectMapProviderInTheMap(mp);
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

   private void updateUI_HideUnhideMapProviders() {

      if (_isShowHiddenMapProvider) {

         // show hidden map provider

         see net.tourbook.common.util.ColumnManager.setVisibleColumnIds_HideCurrentColumn(ColumnDefinition)
         _colDef_IsVisible.setIsColumnDisplayed(true);

         _btnHideUnhideMP.setText(Messages.Slideout_Map2Provider_Button_HideMP);
         _btnHideUnhideMP.setToolTipText(Messages.Slideout_Map2Provider_Button_HideMP_Tooltip);

      } else {

         // hide map provider which should not be visible

         _colDef_IsVisible.setIsColumnDisplayed(false);

         _btnHideUnhideMP.setText(Messages.Slideout_Map2Provider_Button_UnhideMP);
         _btnHideUnhideMP.setToolTipText(Messages.Slideout_Map2Provider_Button_UnhideMP_Tooltip);
      }

      // show/hide visible column
      recreateViewer(_mpViewer);
   }

   /**
    * Select and reveal the previous items.
    *
    * @param selection
    */
   private void updateUI_ReselectItems(final ISelection selection) {

      _isInUpdate = true;
      {
         _mpViewer.setSelection(selection, true);

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
