/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.mapprovider.IMapProviderListener;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.preferences.PrefPage_Map2_Providers;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.EmptyContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapProvider_InfoToolTip;
import net.tourbook.photo.IPhotoPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * 2D Map provider slideout
 */
public class SlideoutMap2_MapProvider extends AdvancedSlideout implements ITourViewer, IMapProviderListener {

// SET_FORMATTING_OFF

   private static final String MAP_ACTION_MANAGE_MAP_PROVIDERS                = net.tourbook.map2.Messages.Map_Action_ManageMapProviders;

   private static final String PREF_MAP2_VIEWER_COLUMN_CATEGORY               = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_Category;
   private static final String PREF_MAP2_VIEWER_COLUMN_IS_TRANSPARENT         = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_IsTransparent;
   private static final String PREF_MAP2_VIEWER_COLUMN_IS_TRANSPARENT_TOOLTIP = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_IsTransparent_Tooltip;
   private static final String PREF_MAP2_VIEWER_COLUMN_IS_HILLSHADING         = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_IsHillshading;
   private static final String PREF_MAP2_VIEWER_COLUMN_IS_HILLSHADING_TOOLTIP = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_IsHillshading_Tooltip;
   private static final String PREF_MAP2_VIEWER_COLUMN_IS_VISIBLE             = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_IsVisible;
   private static final String PREF_MAP2_VIEWER_COLUMN_IS_VISIBLE_TOOLTIP     = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_IsVisible_Tooltip;
   private static final String PREF_MAP2_VIEWER_COLUMN_MAP_PROVIDER           = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_MapProvider;
   private static final String PREF_MAP2_VIEWER_COLUMN_MODIFIED               = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_Modified;
   private static final String PREF_MAP2_VIEWER_COLUMN_MP_TYPE                = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_MPType;
   private static final String PREF_MAP2_VIEWER_COLUMN_MP_TYPE_TOOLTIP        = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_MPType_Tooltip;
   private static final String PREF_MAP2_VIEWER_COLUMN_TILE_URL               = de.byteholder.geoclipse.preferences.Messages.Pref_Map2_Viewer_Column_TileUrl;

// SET_FORMATTING_ON

   private static final String              COLUMN_CATEGORY                    = "Category";                         //$NON-NLS-1$
   private static final String              COLUMN_IS_CONTAINS_HILLSHADING     = "ContainsHillshading";              //$NON-NLS-1$
   private static final String              COLUMN_IS_TRANSPARENT_LAYER        = "IsTransparentLayer";               //$NON-NLS-1$
   private static final String              COLUMN_IS_VISIBLE                  = "IsVisible";                        //$NON-NLS-1$
   private static final String              COLUMN_MAP_MODIFIED                = "Modified";                         //$NON-NLS-1$
   private static final String              COLUMN_MAP_PROVIDER_NAME           = "MapProviderName";                  //$NON-NLS-1$
   private static final String              COLUMN_MP_TYPE                     = "MPType";                           //$NON-NLS-1$
   private static final String              COLUMN_TILE_URL                    = "TileUrl";                          //$NON-NLS-1$

   private static final String              STATE_IS_SHOW_HIDDEN_MAP_PROVIDER  = "STATE_IS_SHOW_HIDDEN_MAP_PROVIDER";//$NON-NLS-1$
   private static final String              STATE_SORT_COLUMN_DIRECTION        = "STATE_SORT_COLUMN_DIRECTION";      //$NON-NLS-1$
   private static final String              STATE_SORT_COLUMN_ID               = "STATE_SORT_COLUMN_ID";             //$NON-NLS-1$

   private static final IPreferenceStore    _prefStore                         = TourbookPlugin.getPrefStore();

   private static IDialogSettings           _state_MapProvider;

   private ActionOpenMapProviderPreferences _action_ManageMapProviders;
   private Action                           _action_MapProvider_Next;
   private Action                           _action_MapProvider_Previous;

   private Map2View                         _map2View;
   private TableViewer                      _mpViewer;
   private ColumnManager                    _columnManager;
   private TableColumnDefinition            _colDef_IsMPVisible;
   private MapProviderComparator            _mpComparator                      = new MapProviderComparator();

   /**
    * Index of the column with the image, index can be changed when the columns are reordered with
    * the mouse or the column manager
    */
   private int                              _columnIndex_ForColumn_IsMPVisible = -1;
   private int                              _columnWidth_ForColumn_IsVisible;

   private long                             _dndDragStartViewerLeft;

   private MP                               _selectedMP;
   private ArrayList<MP>                    _allMapProvider;
   private MapProvider_InfoToolTip          _mpTooltip;

   private SelectionListener                _columnSortListener;

   /** Ignore selection event */
   private boolean                          _isInUpdate;
   private boolean                          _isShowHiddenMapProvider;

   private PixelConverter                   _pc;

   /*
    * UI controls
    */
   private Composite _viewerContainer;
   private ToolItem  _toolItem;

   private Button    _btnHideUnhideMP;

   private Image     _imageYes;
   private Image     _imageNo;

   private class ActionOpenMapProviderPreferences extends ActionOpenPrefDialog {

      public ActionOpenMapProviderPreferences(final String text, final String prefPageId) {

         super(text, prefPageId);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.MapOptions_Dark));
      }
   }

   private class MapProviderComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;
      private static final int MANUAL_SORTING  = 2;

      private String           __sortColumnId  = COLUMN_MAP_PROVIDER_NAME;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final MP mp1 = (MP) e1;
         final MP mp2 = (MP) e2;

         if (__sortDirection == MANUAL_SORTING) {

            return mp1.getSortIndex() - mp2.getSortIndex();

         } else {

            double rc = 0;

            // Determine which column and do the appropriate sort
            switch (__sortColumnId) {

            case COLUMN_CATEGORY:
               rc = mp1.getCategory().compareTo(mp2.getCategory());
               break;

            case COLUMN_IS_CONTAINS_HILLSHADING:

               rc = Boolean.compare(mp2.isIncludesHillshading(), mp1.isIncludesHillshading());
               break;

            case COLUMN_IS_TRANSPARENT_LAYER:

               rc = Boolean.compare(mp2.isTransparentLayer(), mp1.isTransparentLayer());
               break;

            case COLUMN_IS_VISIBLE:

               /*
                * Comparison is mp2->mp1 that the map providers are ascending sorted when visible is
                * ascending sorted
                */
               rc = Boolean.compare(mp2.isVisibleInUI(), mp1.isVisibleInUI());
               break;

            case COLUMN_MAP_MODIFIED:
               rc = mp1.getDateTimeModified_Long() - mp2.getDateTimeModified_Long();
               break;

            case COLUMN_MP_TYPE:
               rc = MapProviderManager.getMapProvider_TypeLabel(mp1).compareTo(MapProviderManager.getMapProvider_TypeLabel(mp2));
               break;

            case COLUMN_TILE_URL:
               rc = MapProviderManager.getTileLayerInfo(mp1).compareTo(MapProviderManager.getTileLayerInfo(mp2));
               break;

            case COLUMN_MAP_PROVIDER_NAME:
            default:
               rc = mp1.getName().compareTo(mp2.getName());
            }

            if (rc == 0) {

               // subsort by map provider

               rc = mp1.getName().compareTo(mp2.getName());
            }

            // if descending order, flip the direction
            if (__sortDirection == DESCENDING) {
               rc = -rc;
            }

            /*
             * MUST return 1 or -1 otherwise long values are not sorted correctly.
             */
            return rc > 0 //
                  ? 1
                  : rc < 0 //
                        ? -1
                        : 0;
         }
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }

      public void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort -> select next sorting

            switch (__sortDirection) {
            case ASCENDING:
               __sortDirection = DESCENDING;
               break;

            case DESCENDING:
               __sortDirection = MANUAL_SORTING;
               break;

            case MANUAL_SORTING:
            default:
               __sortDirection = ASCENDING;
               break;
            }

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
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

         // redraw to show changed image
         _mpViewer.getTable().redraw();
      }
   }

   /**
    * This class is used to show a tooltip only when this cell is hovered
    */
   public abstract class TooltipLabelProvider extends CellLabelProvider {

   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMap2_MapProvider(final ToolItem toolItem, final Map2View map2View, final IDialogSettings state_MapProvider) {

      super(toolItem.getParent(), state_MapProvider, new int[] { 325, 400, 325, 400 });

      _toolItem = toolItem;
      _map2View = map2View;
      _state_MapProvider = state_MapProvider;

      setTitleText(Messages.Slideout_Map2Provider_Label_Title);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      MapProviderManager.getInstance().addMapProviderListener(this);

      /*
       * Create map provider list very early as this list will be used to get the selected or
       * default map provider for the map.
       */
      createSortedMapProviders();
   }

   private void action_HideUnhideMapProvider() {

      // toggle visibility
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

         _action_MapProvider_Next.setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ArrowDown_Blue));
         _action_MapProvider_Next.setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ArrowDown_Blue_Disabled));
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

         _action_MapProvider_Previous.setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ArrowUp_Blue));
         _action_MapProvider_Previous.setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ArrowUp_Blue_Disabled));
         _action_MapProvider_Previous.setToolTipText(Messages.Slideout_Map2Provider_MapProvider_Previous_Tooltip);
      }
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);
      restoreState_BeforeUI();

      createActions();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state_MapProvider);
      defineAllColumns();

      createUI(parent);

      // load viewer
      _mpViewer.setInput(new Object());

      restoreState();
      enableControls();

      _mpViewer.getTable().setFocus();
   }

   /**
    * Create a list with all available map providers, sorted by preference settings
    */
   private void createSortedMapProviders() {

      final ArrayList<MP> allMapProvider = MapProviderManager.getInstance().getAllMapProviders(true);

      final ArrayList<String> storedMpIds = Util.convertStringToList(_prefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

      _allMapProvider = new ArrayList<>();

      int mpSortIndex = 0;

      /*
       * Set sortindex for manual sorted map provider
       */
      for (final String storeMpId : storedMpIds) {

         // find the stored map provider in the available map providers

         for (final MP mp : allMapProvider) {
            if (mp.getId().equals(storeMpId)) {

               mp.setSortIndex(mpSortIndex++);

               _allMapProvider.add(mp);

               break;
            }
         }
      }

      // make sure that all available map providers are in the list
      for (final MP mp : allMapProvider) {
         if (!_allMapProvider.contains(mp)) {
            _allMapProvider.add(mp);
         }
      }

      // ensure that one mp is available
      if (_allMapProvider.isEmpty()) {
         _allMapProvider.add(MapProviderManager.getDefaultMapProvider());
      }
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         _viewerContainer = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_Viewer(_viewerContainer);
         }

         createUI_30_Tips(shellContainer);
         createUI_90_Actions(shellContainer);
      }

      // set colors for all controls
      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
      final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

      UI.setChildColors(shellContainer.getShell(), fgColor, bgColor);

      return shellContainer;
   }

   private void createUI_20_Viewer(final Composite parent) {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
      final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(false);
      table.setHeaderBackground(bgColor);
      table.setHeaderForeground(fgColor);

      net.tourbook.ui.UI.setTableSelectionColor(table);

      // set colors for all cells
      UI.setChildColors(table, fgColor, bgColor);

      /*
       * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is critical
       * for performance that these methods be as efficient as possible.
       */
      final Listener paintListener = event -> {

         if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {
            onPaint_Viewer(event);
         }
      };
      table.addListener(SWT.MeasureItem, paintListener);
      table.addListener(SWT.PaintItem, paintListener);

      table.addControlListener(controlResizedAdapter(controlEvent -> setWidth_ForColumn_IsVisible()));

      table.addMouseWheelListener(mouseEvent -> {

         final boolean isCtrlKey = UI.isCtrlKey(mouseEvent);

         if (isCtrlKey) {

            /*
             * Select next/previous mp but only when ctrl key is pressed, so that vertical
             * scrolling do not select another mp
             */

            if (mouseEvent.count < 0) {
               onSelect_MapProvider_Next();
            } else {
               onSelect_MapProvider_Previous();
            }
         }
      });

      table.addKeyListener(keyPressedAdapter(keyEvent -> {

         if (keyEvent.character == ' ') {
            toggleMPVisibility();
         }
      }));

      /*
       * Create table viewer
       */
      _mpViewer = new TableViewer(table);

      // very important: the editing support must be set BEFORE the columns are created
      _colDef_IsMPVisible.setEditingSupport(new MapProviderVisible_EditingSupport());
      _columnManager.createColumns(_mpViewer);
      _columnManager.setSlideoutShell(this);

      // set initial width
      setWidth_ForColumn_IsVisible();

      _colDef_IsMPVisible.setControlListener(controlResizedAdapter(controlEvent -> setWidth_ForColumn_IsVisible()));

      _mpViewer.setUseHashlookup(true);

      _mpViewer.setContentProvider((IStructuredContentProvider) inputElement -> _allMapProvider.toArray());

      _mpViewer.addSelectionChangedListener(this::onSelect_MapProvider);

      _mpViewer.addDoubleClickListener(doubleClickEvent -> _action_ManageMapProviders.run());

      _mpViewer.setFilters(new ViewerFilter() {

         @Override
         public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

            if (_isShowHiddenMapProvider) {

               // all mp are displayed

               return true;
            }

            return ((MP) element).isVisibleInUI();
         }
      });

      _mpViewer.setComparator(_mpComparator);

      // set info tooltip provider
      _mpTooltip = new MapProvider_InfoToolTip(this, _mpViewer);

      updateUI_SetSortDirection(_mpComparator.__sortColumnId, _mpComparator.__sortDirection);

      createUI_21_DragAndDrop();
      createUI_22_ContextMenu();
   }

   private void createUI_21_DragAndDrop() {

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

                  if (_mpComparator.__sortDirection == MapProviderComparator.MANUAL_SORTING) {

                     // drag & drop can only be done with manual sorting

                     final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
                     final ISelection selection = _mpViewer.getSelection();

                     transfer.setSelection(selection);
                     transfer.setSelectionSetTime(_dndDragStartViewerLeft = event.time & 0xFFFFFFFFL);

                     event.doit = !selection.isEmpty();

                  } else {

                     // column is sorted ascending/descending -> no d&d

                     event.doit = false;
                  }
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

                  // prevent selection, this occurred and mapprovider was null
                  _isInUpdate = true;
                  {
                     final MP droppedMapProvider = (MP) selection.getFirstElement();

                     final int location = getCurrentLocation();
                     final Table mpTable = _mpViewer.getTable();

                     /*
                      * Check if drag was started from this item, remove the item before the new
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

                     // save sort order
                     saveState_MapProviders_SortOrder();
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
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_22_ContextMenu() {

      final Table table = _mpViewer.getTable();

      _columnManager.createHeaderContextMenu(table, new EmptyContextMenuProvider());
   }

   private void createUI_30_Tips(final Composite parent) {

      // label: Hint
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map2Provider_Label_Tip);
      label.setToolTipText(Messages.Slideout_Map2Provider_Label_Tip_Tooltip);
      GridDataFactory.fillDefaults()

            // this width forces the min width of the slideout to be smaller than the default !!!
            .hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
            .applyTo(label);
   }

   private void createUI_90_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults()//
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

            _btnHideUnhideMP.addSelectionListener(widgetSelectedAdapter(selectionEvent -> action_HideUnhideMapProvider()));

            UI.setButtonLayoutData(_btnHideUnhideMP);
         }
      }
   }

   private void defineAllColumns() {

      defineColumn_00_IsVisible();
      defineColumn_10_MapProvider();
      defineColumn_20_Category();
      defineColumn_22_MPType();
      defineColumn_30_Hillshading();
      defineColumn_40_TransparentLayer();
      defineColumn_50_TileUrl();
      defineColumn_80_Modified();
   }

   /**
    * Column: Is visible
    */
   private void defineColumn_00_IsVisible() {

      _colDef_IsMPVisible = new TableColumnDefinition(_columnManager, COLUMN_IS_VISIBLE, SWT.CENTER);

      _colDef_IsMPVisible.setColumnName(PREF_MAP2_VIEWER_COLUMN_IS_VISIBLE);
      _colDef_IsMPVisible.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_IS_VISIBLE_TOOLTIP);
      _colDef_IsMPVisible.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      _colDef_IsMPVisible.setColumnSelectionListener(_columnSortListener);

      _colDef_IsMPVisible.setLabelProvider(new CellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occures !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Map provider
    */
   private void defineColumn_10_MapProvider() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MAP_PROVIDER_NAME, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_MAP_PROVIDER);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_MAP_PROVIDER);

      colDef.setCanModifyVisibility(false);
      colDef.setIsColumnMoveable(false);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(40));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new TooltipLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setImage(MapProviderManager.getMapProvider_TypeImage(mapProvider));
            cell.setText(mapProvider.getName());
         }
      });
   }

   /**
    * Column: Category
    */
   private void defineColumn_20_Category() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_CATEGORY, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_CATEGORY);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_CATEGORY);
      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.getCategory());
         }
      });
   }

   /**
    * Column: MP type
    */
   private void defineColumn_22_MPType() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MP_TYPE, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_MP_TYPE);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_MP_TYPE_TOOLTIP);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(MapProviderManager.getMapProvider_TypeLabel(mapProvider));
         }
      });
   }

   /**
    * Column: Includes hillshading
    */
   private void defineColumn_30_Hillshading() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_IS_CONTAINS_HILLSHADING, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_IS_HILLSHADING);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_IS_HILLSHADING_TOOLTIP);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.isIncludesHillshading() ? Messages.App__True : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Is transparent layer
    */
   private void defineColumn_40_TransparentLayer() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_IS_TRANSPARENT_LAYER, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_IS_TRANSPARENT);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_IS_TRANSPARENT_TOOLTIP);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(mapProvider.isTransparentLayer() ? Messages.App__True : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Tile url
    */
   private void defineColumn_50_TileUrl() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_TILE_URL, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_TILE_URL);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_TILE_URL);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(40));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final MP mapProvider = (MP) cell.getElement();

            cell.setText(MapProviderManager.getTileLayerInfo(mapProvider));
         }
      });
   }

   /**
    * Column: Modified
    */
   private void defineColumn_80_Modified() {

      final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MAP_MODIFIED, SWT.LEAD);

      colDef.setColumnName(PREF_MAP2_VIEWER_COLUMN_MODIFIED);
      colDef.setColumnHeaderToolTipText(PREF_MAP2_VIEWER_COLUMN_MODIFIED);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(18));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ZonedDateTime dtModified = ((MP) cell.getElement()).getDateTimeModified();

            if (dtModified == null) {
               cell.setText(UI.SPACE1);
            } else {
               cell.setText(dtModified.format(TimeTools.Formatter_DateTime_S));
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

   /**
    * @return Returns all {@link MP}'s which are displayed in {@link #_mpViewer}
    */
   private ArrayList<MP> getAllMapProviderInMPViewer() {

      final ArrayList<MP> allMPInMPViewer = new ArrayList<>();

      final TableItem[] items = _mpViewer.getTable().getItems();

      for (final TableItem item : items) {
         allMPInMPViewer.add((MP) item.getData());
      }

      return allMPInMPViewer;
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

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _mpViewer.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   @Override
   public ColumnViewer getViewer() {
      return _mpViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _mpViewer.getSelection();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _imageYes = CommonActivator.getImageDescriptor(CommonImages.App_Yes).createImage();
      _imageNo = CommonActivator.getImageDescriptor(CommonImages.App_No).createImage();

      _columnSortListener = widgetSelectedAdapter(this::onSelect_SortColumn);
   }

   @Override
   public void mapProviderListChanged() {

      if (_selectedMP != null) {

         // map profile tile offline images are deleted, reset state
         _selectedMP.resetTileImageAvailability();
      }

      // update model
      createSortedMapProviders();

      // update UI
      // -> UI is disposed as when the pref page opens to modify map provider's then this slideout is closed
      // _mpViewer.refresh();
//      selectMapProvider(_selectedMP == null ? null : _selectedMP.getId());
   }

   @Override
   protected void onDispose() {

      UI.disposeResource(_imageYes);
      UI.disposeResource(_imageNo);

      MapProviderManager.getInstance().removeMapProviderListener(this);

      super.onDispose();
   }

   @Override
   protected void onFocus() {

      _mpViewer.getTable().setFocus();
   }

   private void onPaint_Viewer(final Event event) {

      // paint images at the correct column

      final int columnIndex = event.index;

      if (columnIndex == _columnIndex_ForColumn_IsMPVisible) {

         onPaint_Viewer_GraphImage(event);
      }

   }

   private void onPaint_Viewer_GraphImage(final Event event) {

      switch (event.type) {
      case SWT.MeasureItem:

         break;

      case SWT.PaintItem:

         final TableItem item = (TableItem) event.item;
         final Object itemData = item.getData();

         final MP mp = (MP) itemData;

         final Image image = mp.isVisibleInUI() ? _imageYes : _imageNo;

         if (image != null) {

            final Rectangle imageRect = image.getBounds();

            // center horizontal
            final int xOffset = Math.max(0, (_columnWidth_ForColumn_IsVisible - imageRect.width) / 2);

            // center vertical
            final int yOffset = Math.max(0, (event.height - imageRect.height) / 2);

            final int devX = event.x + xOffset;
            final int devY = event.y + yOffset;

            event.gc.drawImage(image, devX, devY);
         }

         break;
      }
   }

   private void onSelect_MapProvider(final SelectionChangedEvent event) {

      // hide tooltip which can display content from the previous map provider
      _mpTooltip.hide();

      MP selectedMapProvider = (MP) event.getStructuredSelection().getFirstElement();

      if (selectedMapProvider == null) {

         // this can occur when the last selected mp is not available or filtered out

         selectedMapProvider = _allMapProvider.get(0);
      }

      /*
       * Show map provider name in the slideout title
       */
      String titleText = selectedMapProvider.getName();
      // replace & with && otherwise it is displayed
      titleText = UI.escapeAmpersand(titleText);
      updateTitleText(titleText);

      if (_isInUpdate) {
         return;
      }

      selectMapProviderInTheMap(selectedMapProvider);
   }

   public void onSelect_MapProvider_Next() {

      if (_mpViewer.getTable().isDisposed()) {

         // this can occures when the action is pressed with the keyboard and the slideout is closed

         return;
      }

      final ArrayList<MP> allMPInViewer = getAllMapProviderInMPViewer();
      final int numMP = allMPInViewer.size();

      for (int mpIndex = 0; mpIndex < numMP; mpIndex++) {

         final MP mp = allMPInViewer.get(mpIndex);

         if (mp.equals(_selectedMP)) {

            // current mp is found -> move to the next

            if (mpIndex >= numMP - 1) {

               // the last mp is currently displayed -> move to the first

               selectMapProvider_Internal(allMPInViewer.get(0));

            } else {

               selectMapProvider_Internal(allMPInViewer.get(mpIndex + 1));
            }

            return;
         }
      }

      // this case can occur when switched from show all to only visible map providers
      selectMapProvider_Internal(allMPInViewer.get(0));
   }

   public void onSelect_MapProvider_Previous() {

      if (_mpViewer.getTable().isDisposed()) {

         // this can occur when the action is pressed with the keyboard and the slideout is closed

         return;
      }

      final ArrayList<MP> allMPInViewer = getAllMapProviderInMPViewer();
      final int numMP = allMPInViewer.size();

      for (int mpIndex = 0; mpIndex < numMP; mpIndex++) {

         final MP mp = allMPInViewer.get(mpIndex);

         if (mp.equals(_selectedMP)) {

            // current mp is found -> move to the previous

            if (mpIndex == 0) {

               // the first mp is currently displayed -> move to the last

               selectMapProvider_Internal(allMPInViewer.get(numMP - 1));

            } else {

               selectMapProvider_Internal(allMPInViewer.get(mpIndex - 1));
            }

            return;
         }
      }

      // this case can occur when switched from show all to only visible map providers
      selectMapProvider_Internal(allMPInViewer.get(0));
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();

         // toggle sorting
         _mpComparator.setSortColumn(e.widget);
         _mpViewer.refresh();

         // reselect selection
         _mpViewer.setSelection(selectionBackup, true);
         _mpViewer.getTable().showSelection();
      }
      _viewerContainer.setRedraw(true);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _mpViewer.getSelection();
         {
            _mpViewer.getTable().dispose();

            // update column index which is needed for repainting
            final ColumnProfile activeProfile = _columnManager.getActiveProfile();
            _columnIndex_ForColumn_IsMPVisible = activeProfile.getColumnIndex(_colDef_IsMPVisible.getColumnId());

            createUI_20_Viewer(_viewerContainer);

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

      _isShowHiddenMapProvider = Util.getStateBoolean(_state_MapProvider, STATE_IS_SHOW_HIDDEN_MAP_PROVIDER, true);

      /*
       * Check all map providers which are defined in the pref store
       */
      final ArrayList<String> storeProviderIds = Util.convertStringToList(_prefStore.getString(IMappingPreferences.MAP_PROVIDER_VISIBLE_IN_UI));

      for (final MP mapProvider : _allMapProvider) {

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

      if (currentMP != null) {

         /*
          * Ensure that the map provider is also visible in the viewer
          */
         // update model
         currentMP.setIsVisibleInUI(true);

         // update UI
         _mpViewer.refresh();

         _mpViewer.setSelection(new StructuredSelection(currentMP), true);
      }

      updateUI_HideUnhideMapProviders();
   }

   private void restoreState_BeforeUI() {

      // update sorting comparator
      final String sortColumnId = Util.getStateString(_state_MapProvider, STATE_SORT_COLUMN_ID, COLUMN_MAP_PROVIDER_NAME);
      final int sortDirection = Util.getStateInt(_state_MapProvider, STATE_SORT_COLUMN_DIRECTION, MapProviderComparator.ASCENDING);

      _mpComparator.__sortColumnId = sortColumnId;
      _mpComparator.__sortDirection = sortDirection;
   }

   @Override
   protected void saveState() {

      _state_MapProvider.put(STATE_IS_SHOW_HIDDEN_MAP_PROVIDER, _isShowHiddenMapProvider);

      _state_MapProvider.put(STATE_SORT_COLUMN_ID, _mpComparator.__sortColumnId);
      _state_MapProvider.put(STATE_SORT_COLUMN_DIRECTION, _mpComparator.__sortDirection);

      _columnManager.saveState(_state_MapProvider);

      super.saveState();
   }

   @Override
   protected void saveState_BeforeDisposed() {

      MapProviderManager.saveState_MapProviders_VisibleInUI();
   }

   /**
    * Save the check state and order of the map providers
    */
   private void saveState_MapProviders_SortOrder() {

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

         for (final MP mp : _allMapProvider) {

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
      for (final MP mp : _allMapProvider) {

         if (mp.getId().equals(MapProviderManager.DEFAULT_MAP_PROVIDER_ID)) {

            // map provider is available
            selectMapProvider_Internal(mp);

            return;
         }
      }

      /*
       * if map provider is not set, get first one
       */
      final MP mp = _allMapProvider.get(0);
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

      final Map2 map = _map2View.getMap();

      map.setMapProvider(mp);

      // set map dim level
      final IDialogSettings state_Map2 = Map2View.getState();
      final boolean isMapDimmed = Util.getStateBoolean(state_Map2, Map2View.STATE_IS_MAP_DIMMED, Map2View.STATE_IS_MAP_DIMMED_DEFAULT);
      final int mapDimValue = Util.getStateInt(state_Map2, Map2View.STATE_DIM_MAP_VALUE, Map2View.STATE_DIM_MAP_VALUE_DEFAULT);
      final RGB mapDimColor = Util.getStateRGB(state_Map2, Map2View.STATE_DIM_MAP_COLOR, Map2View.STATE_DIM_MAP_COLOR_DEFAULT);
      map.setDimLevel(isMapDimmed, mapDimValue, mapDimColor, _map2View.isBackgroundDark());
   }

   private void setWidth_ForColumn_IsVisible() {

      if (_colDef_IsMPVisible != null) {

         final TableColumn tableColumn = _colDef_IsMPVisible.getTableColumn();

         if (tableColumn != null && tableColumn.isDisposed() == false) {
            _columnWidth_ForColumn_IsVisible = tableColumn.getWidth();
         }
      }
   }

   private void toggleMPVisibility() {

      final MP mp = (MP) _mpViewer.getStructuredSelection().getFirstElement();

      if (mp == null) {

         // this occured

         return;
      }

      if (_colDef_IsMPVisible.isColumnCheckedInContextMenu()) {

         // toggle only when visible column is displayed

         // update model - toggle visibility
         mp.setIsVisibleInUI(!mp.isVisibleInUI());

         // update UI
         _mpViewer.update(mp, null);

         // redraw to show changed image
         _mpViewer.getTable().redraw();
      }
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void updateUI_HideUnhideMapProviders() {

      if (_isShowHiddenMapProvider) {

         // show hidden map provider

         _columnManager.setColumnVisible(_colDef_IsMPVisible, true);

         _btnHideUnhideMP.setText(Messages.Slideout_Map2Provider_Button_HideMP);
         _btnHideUnhideMP.setToolTipText(Messages.Slideout_Map2Provider_Button_HideMP_Tooltip);

      } else {

         // hide map provider which should not be visible

         _columnManager.setColumnVisible(_colDef_IsMPVisible, false);

         _btnHideUnhideMP.setText(Messages.Slideout_Map2Provider_Button_UnhideMP);
         _btnHideUnhideMP.setToolTipText(Messages.Slideout_Map2Provider_Button_UnhideMP_Tooltip);
      }
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

   /**
    * Set the sort column direction indicator for a column
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final int direction =
            sortDirection == MapProviderComparator.ASCENDING ? SWT.UP
                  : sortDirection == MapProviderComparator.DESCENDING ? SWT.DOWN
                        : SWT.NONE;

      final Table table = _mpViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(direction);
   }

   private void updateUI_SetViewerInput() {

      _isInUpdate = true;
      {
         _mpViewer.setInput(new Object[0]);
      }
      _isInUpdate = false;
   }
}
