/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.io.File;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.NatTable_LabelProvider;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionDuplicateTour;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TableViewerTourInfoToolTip;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.geoCompare.GeoPartComparerItem;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.Action_Reimport_SubMenu;
import net.tourbook.ui.views.rawData.SubMenu_AdjustTourValues;
import net.tourbook.ui.views.tourBook.natTable.DataProvider;
import net.tourbook.ui.views.tourBook.natTable.DataProvider_ColumnHeader;
import net.tourbook.ui.views.tourBook.natTable.DataProvider_Tour;

import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.hover.HoverLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.CellPainterDecorator;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectRowsCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultRowSelectionLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.theme.ModernNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements ITourProvider2, ITourViewer3, ITourProviderByID, ITreeViewer {

// SET_FORMATTING_OFF

   private static final String           COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP           = net.tourbook.ui.Messages.ColumnFactory_TimeZoneDifference_Tooltip;

   static public final String            ID                                              = "net.tourbook.views.tourListView";         //$NON-NLS-1$

   private final static IPreferenceStore _prefStore                                      = TourbookPlugin.getPrefStore();
   private final static IPreferenceStore _prefStoreCommon                                = CommonActivator.getPrefStore();
   //
   private static final IDialogSettings _state                                          = TourbookPlugin.getState(ID);

// SET_FORMATTING_ON

   private static final IDialogSettings _state_Table                                    = TourbookPlugin.getState(ID + "_TABLE");    //$NON-NLS-1$
   private static final IDialogSettings _state_Tree                                     = TourbookPlugin.getState(ID + "_TREE");     //$NON-NLS-1$
   //
   private static final String          STATE_CSV_EXPORT_PATH                           = "STATE_CSV_EXPORT_PATH";                   //$NON-NLS-1$
   //
   private static final String          STATE_IS_LINK_WITH_OTHER_VIEWS                  = "STATE_IS_LINK_WITH_OTHER_VIEWS";          //$NON-NLS-1$
   private static final String          STATE_IS_SELECT_YEAR_MONTH_TOURS                = "STATE_IS_SELECT_YEAR_MONTH_TOURS";        //$NON-NLS-1$
   static final String                  STATE_IS_SHOW_SUMMARY_ROW                       = "STATE_IS_SHOW_SUMMARY_ROW";               //$NON-NLS-1$
   static final String                  STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS         = "STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS"; //$NON-NLS-1$
   private static final String          STATE_SELECTED_TABLE_INDEX                      = "STATE_SELECTED_TABLE_INDEX";              //$NON-NLS-1$
   private static final String          STATE_SELECTED_MONTH                            = "STATE_SELECTED_MONTH";                    //$NON-NLS-1$
   private static final String          STATE_SELECTED_TOURS                            = "STATE_SELECTED_TOURS";                    //$NON-NLS-1$
   private static final String          STATE_SELECTED_YEAR                             = "STATE_SELECTED_YEAR";                     //$NON-NLS-1$
   private static final String          STATE_VIEW_LAYOUT                               = "STATE_VIEW_LAYOUT";                       //$NON-NLS-1$
   //
   private static final String          STATE_SORT_COLUMN_DIRECTION                     = "STATE_SORT_COLUMN_DIRECTION";             //$NON-NLS-1$
   private static final String          STATE_SORT_COLUMN_ID                            = "STATE_SORT_COLUMN_ID";                    //$NON-NLS-1$
   //
   static final boolean                 STATE_IS_SHOW_SUMMARY_ROW_DEFAULT               = true;
   static final boolean                 STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS_DEFAULT = true;
   //
   private static final String          CSV_EXPORT_DEFAULT_FILE_NAME                    = "TourBook_";                               //$NON-NLS-1$
   //
   /**
    * The header column id needs a different id than the body column otherwise drag&drop or column
    * selection shows the 1st row image :-(
    */
   private static final String          HEADER_COLUMN_ID_POSTFIX                        = "_HEADER";
   //
   private static final NumberFormat    _nf0;
   private static final NumberFormat    _nf1;
   private static final NumberFormat    _nf2;
   //
   static {
      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1 = NumberFormat.getNumberInstance();
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf2 = NumberFormat.getNumberInstance();
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }
   //
   private static TourBookViewLayout      _viewLayout;
   //
   private ColumnManager                  _columnManager_Table;
   private ColumnManager                  _columnManager_Tree;
   //
   private OpenDialogManager              _openDlgMgr                      = new OpenDialogManager();
   //
   private PostSelectionProvider          _postSelectionProvider;
   //
   private SelectionAdapter               _columnSortListener;
   private ISelectionListener             _postSelectionListener;
   private IPartListener2                 _partListener;
   private ITourEventListener             _tourPropertyListener;
   private IPropertyChangeListener        _prefChangeListener;
   private IPropertyChangeListener        _prefChangeListenerCommon;
   //
   private TableViewer                    _tourViewer_Table;
   private TreeViewer                     _tourViewer_Tree;
   private ItemComparator_Table           _tourViewer_Table_Comparator     = new ItemComparator_Table();
   //
   private TableColumnDefinition          _colDef_TimeZoneOffset_Table;
   private TreeColumnDefinition           _colDef_TimeZoneOffset_Tree;
   //
   private LazyTourProvider               _tableTourProvider               = new LazyTourProvider(this);
   private TVITourBookRoot                _rootItem_Tree;
   //
   private NatTable                       _tourViewer_NatTable;
   private DataProvider                   _natTable_DataProvider;
   private ColumnReorderLayer             _natTable_ColumnReorder_Layer;
   private ViewportLayer                  _natTable_Grid_BodyLayer;
   private DataLayer                      _natTable_DataLayer_Body;
   //
   private int                            _selectedYear                    = -1;
   private int                            _selectedYearSub                 = -1;
   private final ArrayList<Long>          _selectedTourIds                 = new ArrayList<>();
   //
   private boolean                        _isCollapseOthers;
   private boolean                        _isInFireSelection;
   private boolean                        _isInReload;
   private boolean                        _isInStartup;
   private boolean                        _isLayoutNatTable;
   private boolean                        _isLayoutTable;
   private boolean                        _isShowSummaryRow;
   private boolean                        _isShowToolTipIn_Date;
   private boolean                        _isShowToolTipIn_Tags;
   private boolean                        _isShowToolTipIn_Time;
   private boolean                        _isShowToolTipIn_Title;
   private boolean                        _isShowToolTipIn_WeekDay;
   //
   private final TourDoubleClickState     _tourDoubleClickState            = new TourDoubleClickState();
   private TableViewerTourInfoToolTip     _tourInfoToolTip_Table;
   private TreeViewerTourInfoToolTip      _tourInfoToolTip_Tree;
   //
   private TagMenuManager                 _tagMenuManager;
   private MenuManager                    _viewerMenuManager_Table;
   private MenuManager                    _viewerMenuManager_Tree;
   private IContextMenuProvider           _viewerContextMenuProvider_Table = new ContextMenuProvider_Table();
   private IContextMenuProvider           _viewerContextMenuProvider_Tree  = new ContextMenuProvider_Tree();
   //
   private SubMenu_AdjustTourValues       _subMenu_AdjustTourValues;
   private Action_Reimport_SubMenu        _subMenu_Reimport;
   //
   private ActionCollapseAll              _actionCollapseAll;
   private ActionCollapseOthers           _actionCollapseOthers;
   private ActionDuplicateTour            _actionDuplicateTour;
   private ActionEditQuick                _actionEditQuick;
   private ActionExpandSelection          _actionExpandSelection;
   private ActionExport                   _actionExportTour;
   private ActionExportViewCSV            _actionExportViewCSV;
   private ActionDeleteTourMenu           _actionDeleteTour;
   private ActionEditTour                 _actionEditTour;
   private ActionJoinTours                _actionJoinTours;
   private ActionLinkWithOtherViews       _actionLinkWithOtherViews;
   private ActionMergeTour                _actionMergeTour;
   private ActionModifyColumns            _actionModifyColumns;
   private ActionOpenTour                 _actionOpenTour;
   private ActionOpenMarkerDialog         _actionOpenMarkerDialog;
   private ActionOpenAdjustAltitudeDialog _actionOpenAdjustAltitudeDialog;
   private ActionPrint                    _actionPrintTour;
   private ActionRefreshView              _actionRefreshView;
   private ActionSelectAllTours           _actionSelectAllTours;
   private ActionSetTourTypeMenu          _actionSetTourType;
   private ActionSetPerson                _actionSetOtherPerson;
   private ActionToggleViewLayout         _actionToggleViewLayout;
   private ActionTourBookOptions          _actionTourBookOptions;
   //
   private PixelConverter                 _pc;
   //
   /*
    * UI controls
    */
   private PageBook  _pageBook;
   //
   private Composite _parent;
   private Composite _viewerContainer_NatTable;
   private Composite _viewerContainer_Table;
   private Composite _viewerContainer_Tree;
   //
   private Menu      _contextMenu_Table;
   private Menu      _contextMenu_Tree;

   private class ActionLinkWithOtherViews extends ActionToolbarSlideout {

      public ActionLinkWithOtherViews() {

         super(TourbookPlugin.getImageDescriptor(Messages.Image__SyncViews), null);

         isToggleAction = true;
         notSelectedTooltip = Messages.Calendar_View_Action_LinkWithOtherViews;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutLinkWithOtherViews(_parent, toolbar, TourBookView.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {
         super.onSelect();
      }
   }

   private class ActionTourBookOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTourBookOptions(_parent, toolbar, TourBookView.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ContentProvider_Table implements ILazyContentProvider {

      @Override
      public void updateElement(final int index) {

         final TreeViewerItem tableItem = _tableTourProvider.getTour(index);

         if (tableItem != null) {
            getTourViewer_Table().replace(tableItem, index);
         }
      }
   }

   private class ContentProvider_Tree implements ITreeContentProvider {

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _rootItem_Tree.getFetchedChildrenAsArray();
      }

      @Override
      public Object getParent(final Object element) {
         return ((TreeViewerItem) element).getParentItem();
      }

      @Override
      public boolean hasChildren(final Object element) {
         return ((TreeViewerItem) element).hasChildren();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private class ContextMenuProvider_Table implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_contextMenu_Table != null) {
            _contextMenu_Table.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _contextMenu_Table;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _contextMenu_Table = createUI_52_CreateViewerContextMenu_Table();

         return _contextMenu_Table;
      }

   }

   private class ContextMenuProvider_Tree implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_contextMenu_Tree != null) {
            _contextMenu_Tree.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _contextMenu_Tree;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _contextMenu_Tree = createUI_52_CreateViewerContextMenu_Tree();

         return _contextMenu_Tree;
      }

   }

   public class ItemComparator_Table /* extends ViewerComparator */ {

      public static final int  ASCENDING  = 0;
      private static final int DESCENDING = 1;

      private String           __sortColumnId;
      private int              __sortDirection;

//      @Override
//      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
//
//         final TVITourBookTour tourItem1 = ((TVITourBookTour) obj1);
//         final TVITourBookTour tourItem2 = ((TVITourBookTour) obj2);
//
//         int result = 0;
//
//         switch (__sortColumnId) {
//
//         case TableColumnFactory.TIME_DATE_ID:
//
//            // 1st Column: Date/time
//
//            result = tourItem1.colDateTime_MS > tourItem2.colDateTime_MS ? 1 : -1;
//            break;
//
//         /*
//          * BODY
//          */
//
//         case TableColumnFactory.BODY_AVG_PULSE_ID:
//            break;
//
//         case TableColumnFactory.BODY_CALORIES_ID:
//            break;
//
//         case TableColumnFactory.BODY_PULSE_MAX_ID:
//            break;
//
//         case TableColumnFactory.BODY_PERSON_ID:
//            break;
//
//         case TableColumnFactory.BODY_RESTPULSE_ID:
//            break;
//
//         case TableColumnFactory.BODY_WEIGHT_ID:
//            break;
//
//         /*
//          * DATA
//          */
//
//         case TableColumnFactory.DATA_DP_TOLERANCE_ID:
//            break;
//
//         case TableColumnFactory.DATA_IMPORT_FILE_NAME_ID:
//            break;
//
//         case TableColumnFactory.DATA_IMPORT_FILE_PATH_ID:
//            break;
//
//         case TableColumnFactory.DATA_NUM_TIME_SLICES_ID:
//            break;
//
//         case TableColumnFactory.DATA_TIME_INTERVAL_ID:
//            break;
//
//         /*
//          * DEVICE
//          */
//         case TableColumnFactory.DEVICE_DISTANCE_ID:
//            break;
//
//         case TableColumnFactory.DEVICE_NAME_ID:
//            break;
//
//         /*
//          * ELEVATION
//          */
//
//         case TableColumnFactory.ALTITUDE_AVG_CHANGE_ID:
//            break;
//
//         case TableColumnFactory.ALTITUDE_MAX_ID:
//            break;
//
//         case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN_ID:
//            break;
//
//         case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP_ID:
//            break;
//
//         /*
//          * MOTION
//          */
//
//         case TableColumnFactory.MOTION_AVG_PACE_ID:
//            break;
//
//         case TableColumnFactory.MOTION_AVG_SPEED_ID:
//            break;
//
//         case TableColumnFactory.MOTION_DISTANCE_ID:
//            break;
//
//         case TableColumnFactory.MOTION_MAX_SPEED_ID:
//            break;
//
//         /*
//          * POWER
//          */
//
//         case TableColumnFactory.POWER_AVG_ID:
//            break;
//
//         case TableColumnFactory.POWER_MAX_ID:
//            break;
//
//         case TableColumnFactory.POWER_NORMALIZED_ID:
//            break;
//
//         case TableColumnFactory.POWER_TOTAL_WORK_ID:
//            break;
//
//         /*
//          * POWERTRAIN
//          */
//
//         case TableColumnFactory.POWERTRAIN_AVG_CADENCE_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES_ID:
//            break;
//
//         case TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER_ID:
//            break;
//
//         /*
//          * RUNNING DYNAMICS
//          */
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STEP_LENGTH_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STEP_LENGTH_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_STEP_LENGTH_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN_ID:
//            break;
//
//         case TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX_ID:
//            break;
//
//         /*
//          * SURFING
//          */
//
//         case TableColumnFactory.SURFING_MIN_DISTANCE_ID:
//            break;
//
//         case TableColumnFactory.SURFING_MIN_SPEED_START_STOP_ID:
//            break;
//
//         case TableColumnFactory.SURFING_MIN_SPEED_SURFING_ID:
//            break;
//
//         case TableColumnFactory.SURFING_MIN_TIME_DURATION_ID:
//            break;
//
//         case TableColumnFactory.SURFING_NUMBER_OF_EVENTS_ID:
//            break;
//
//         /*
//          * TIME
//          */
//
//         case TableColumnFactory.TIME_DRIVING_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_PAUSED_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_PAUSED_TIME_RELATIVE_ID:
//            break;
//
//         case TableColumnFactory.TIME_RECORDING_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_TIME_ZONE_ID:
//            break;
//
//         case TableColumnFactory.TIME_TIME_ZONE_DIFFERENCE_ID:
//            break;
//
//         case TableColumnFactory.TIME_TOUR_START_TIME_ID:
//            break;
//
//         case TableColumnFactory.TIME_WEEK_DAY_ID:
//            break;
//
//         case TableColumnFactory.TIME_WEEK_NO_ID:
//            break;
//
//         case TableColumnFactory.TIME_WEEKYEAR_ID:
//            break;
//
//         /*
//          * TOUR
//          */
//
//         case TableColumnFactory.TOUR_LOCATION_START_ID:
//            break;
//
//         case TableColumnFactory.TOUR_LOCATION_END_ID:
//            break;
//
//         case TableColumnFactory.TOUR_NUM_MARKERS_ID:
//            break;
//
//         case TableColumnFactory.TOUR_NUM_PHOTOS_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TAGS_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TITLE_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TYPE_ID:
//            break;
//
//         case TableColumnFactory.TOUR_TYPE_TEXT_ID:
//            break;
//
//         /*
//          * TRAINING
//          */
//
//         case TableColumnFactory.TRAINING_EFFECT_AEROB_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_EFFECT_ANAEROB_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_FTP_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_INTENSITY_FACTOR_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_POWER_TO_WEIGHT_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_STRESS_SCORE_ID:
//            break;
//
//         case TableColumnFactory.TRAINING_PERFORMANCE_LEVEL_ID:
//            break;
//
//         /*
//          * WEATHER
//          */
//
//         case TableColumnFactory.WEATHER_CLOUDS_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_TEMPERATURE_AVG_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_TEMPERATURE_MIN_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_TEMPERATURE_MAX_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_WIND_DIR_ID:
//            break;
//
//         case TableColumnFactory.WEATHER_WIND_SPEED_ID:
//            break;
//
//         case TableColumnFactory.DATA_SEQUENCE_ID:
//         default:
//
//            result = tourItem1.col_Sequence > tourItem2.col_Sequence ? 1 : -1;
//
//            break;
//         }

//         if (__sortColumnId.equals(_columnId_1stColumn_Date)) {
//
//         } else if (__sortColumnId.equals(_columnId_Title)) {
//
//            // title
//
//            result = tourItem1.getTourTitle().compareTo(tourItem2.getTourTitle());
//
//         } else if (__sortColumnId.equals(_columnId_ImportFileName)) {
//
//            // file name
//
//            final String importFilePath1 = tourItem1.getImportFilePath();
//            final String importFilePath2 = tourItem2.getImportFilePath();
//
//            if (importFilePath1 != null && importFilePath2 != null) {
//
//               result = importFilePath1.compareTo(importFilePath2);
//            }
//
//         } else if (__sortColumnId.equals(_columnId_DeviceName)) {
//
//            // device name
//
//            result = tourItem1.getDeviceName().compareTo(tourItem2.getDeviceName());
//
//         } else if (__sortColumnId.equals(_columnId_TimeZone)) {
//
//            // time zone
//
//            final String timeZoneId1 = tourItem1.getTimeZoneId();
//            final String timeZoneId2 = tourItem2.getTimeZoneId();
//
//            if (timeZoneId1 != null && timeZoneId2 != null) {
//
//               final int zoneCompareResult = timeZoneId1.compareTo(timeZoneId2);
//
//               result = zoneCompareResult;
//
//            } else if (timeZoneId1 != null) {
//
//               result = 1;
//
//            } else if (timeZoneId2 != null) {
//
//               result = -1;
//            }
//         }
//
// do a 2nd sorting by date/time when not yet sorted
//         if (result == 0) {
//            result = tourItem1.colDateTime_MS > tourItem2.colDateTime_MS ? 1 : -1;
//         }
//
//         // if descending order, flip the direction
//         if (__sortDirection == DESCENDING) {
//            result = -result;
//         }
//
//         return result;
//      }

      /**
       * Does the sort. If it's a different column from the previous sort, do an ascending sort. If
       * it's the same column as the last sort, toggle the sort direction.
       *
       * @param widget
       *           Column widget
       */
      private void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort; toggle the direction

            __sortDirection = 1 - __sortDirection;

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_ShowSortDirection(__sortColumnId, __sortDirection);
      }
   }

   private static class ItemComparer_Tree implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {
            return true;
         }

         if (a instanceof TVITourBookYear && b instanceof TVITourBookYear) {

            final TVITourBookYear item1 = (TVITourBookYear) a;
            final TVITourBookYear item2 = (TVITourBookYear) b;
            return item1.tourYear == item2.tourYear;
         }

         if (a instanceof TVITourBookYearCategorized && b instanceof TVITourBookYearCategorized) {

            final TVITourBookYearCategorized item1 = (TVITourBookYearCategorized) a;
            final TVITourBookYearCategorized item2 = (TVITourBookYearCategorized) b;
            return item1.tourYear == item2.tourYear && item1.tourYearSub == item2.tourYearSub;
         }

         if (a instanceof TVITourBookTour && b instanceof TVITourBookTour) {

            final TVITourBookTour item1 = (TVITourBookTour) a;
            final TVITourBookTour item2 = (TVITourBookTour) b;
            return item1.tourId == item2.tourId;
         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }
   }

   private final class NatTable_ConfigField_TourType extends AbstractRegistryConfiguration {

      private IRowDataProvider<TVITourBookTour> _dataProvider;

      private NatTable_ConfigField_TourType(final IRowDataProvider<TVITourBookTour> body_DataProvider) {

         _dataProvider = body_DataProvider;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         final ImagePainter decoratorCellPainter = new ImagePainter() {

            @Override
            protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry) {

               // get the row object

               final int rowIndex = cell.getRowIndex();

               final TVITourBookTour tviTour = _dataProvider.getRowObject(rowIndex);

               if (tviTour == null) {
                  return null;
               }

               final long tourTypeId = tviTour.getTourTypeId();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

               return tourTypeImage;
            }
         };

         // apply painter to the body cells and not to the header cells
         configRegistry.registerConfigAttribute(

               CellConfigAttributes.CELL_PAINTER,
               new CellPainterDecorator(null, CellEdgeEnum.LEFT, decoratorCellPainter),
               DisplayMode.NORMAL,
               TableColumnFactory.TOUR_TYPE_ID);
      }
   }

   private final class NatTable_ConfigField_Weather extends AbstractRegistryConfiguration {

      private IRowDataProvider<TVITourBookTour> _dataProvider;

      private NatTable_ConfigField_Weather(final IRowDataProvider<TVITourBookTour> body_DataProvider) {

         _dataProvider = body_DataProvider;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         final ImagePainter decoratorCellPainter = new ImagePainter() {

            @Override
            protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry) {

               // get the row object

               final int rowIndex = cell.getRowIndex();

               final TVITourBookTour tviTour = _dataProvider.getRowObject(rowIndex);

               if (tviTour == null) {
                  return null;
               }

               final String windClouds = tviTour.colClouds;

               if (windClouds == null) {
                  return null;
               } else {
                  final Image cellImage = net.tourbook.common.UI.IMAGE_REGISTRY.get(windClouds);
                  if (cellImage == null) {
                     return null;
                  } else {
                     return cellImage;
                  }
               }
            }
         };

         configRegistry.registerConfigAttribute(
               CellConfigAttributes.CELL_PAINTER,
               new CellPainterDecorator(null, CellEdgeEnum.LEFT, decoratorCellPainter),
               DisplayMode.NORMAL,
               TableColumnFactory.WEATHER_CLOUDS_ID);
      }
   }

   private class NatTable_Configuration_CellStyle extends AbstractRegistryConfiguration {

      private ArrayList<ColumnDefinition> _allSortedColumns;

      public NatTable_Configuration_CellStyle(final ArrayList<ColumnDefinition> allSortedColumns) {

         _allSortedColumns = allSortedColumns;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         // loop: all displayed columns
         for (final ColumnDefinition colDef : _allSortedColumns) {

            if (!colDef.isColumnDisplayed()) {
               // visible columns are displayed first
               break;
            }

            final String columnId = colDef.getColumnId();

            switch (columnId) {

            case TableColumnFactory.TOUR_TYPE_ID:
            case TableColumnFactory.WEATHER_CLOUDS_ID:

               // images are displayed for these column
               break;

            default:

               Style style;

               final HorizontalAlignmentEnum columnAlignment = natTableConvert_ColumnAlignment(colDef.getColumnStyle());

               // body style
               style = new Style();
               style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, columnAlignment);

               configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                     style,
                     DisplayMode.NORMAL,
                     columnId);

               // header style
               style = style.clone();
               configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                     style,
                     DisplayMode.NORMAL,
                     columnId + HEADER_COLUMN_ID_POSTFIX);
               break;
            }
         }
      }

      /**
       * Convert col def style into nat table style
       *
       * @param columnStyle
       * @return
       */
      private HorizontalAlignmentEnum natTableConvert_ColumnAlignment(final int columnStyle) {

         switch (columnStyle) {

         case SWT.LEFT:
            return HorizontalAlignmentEnum.LEFT;

         case SWT.RIGHT:
            return HorizontalAlignmentEnum.RIGHT;

         default:
            return HorizontalAlignmentEnum.CENTER;
         }
      }
   }

   private final class NatTable_Configuration_Hover extends AbstractRegistryConfiguration {

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         Style style;

         style = new Style();
         style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_YELLOW);

         configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.HOVER);

         style = new Style();
         style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_RED);

         configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.SELECT_HOVER);
      }
   }

   private class NatTable_Configuration_Theme extends ModernNatTableThemeConfiguration {

      public NatTable_Configuration_Theme() {

         super();

         /*
          * Overwrite default modern theme
          */

         // hide grid lines
         this.renderBodyGridLines = false;

         // show selection header with default colors
         this.cHeaderSelectionBgColor = cHeaderBgColor;
         this.cHeaderSelectionFgColor = cHeaderFgColor;

//         public static final Color COLOR_LIST_SELECTION = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
//         public static final Color COLOR_LIST_SELECTION_TEXT = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);

         // default selection style
//         this.defaultSelectionBgColor = GUIHelper.COLOR_LIST_SELECTION;
//         this.defaultSelectionFgColor = GUIHelper.COLOR_LIST_SELECTION_TEXT;
         this.defaultSelectionBgColor = GUIHelper.COLOR_BLACK;
         this.defaultSelectionFgColor = GUIHelper.COLOR_YELLOW;
      }
   }

//   private final class NatTable_DataLayer_Body extends DataLayer {
//
//      private NatTable_DataLayer_Body(final IDataProvider dataProvider) {
//         super(dataProvider);
//      }
//
//      private SizeConfig getColumnWidthConfig() {
//         return columnWidthConfig;
//      }
//   }

   void actionExportViewCSV() {

      // get selected items
      final ITreeSelection selection = (ITreeSelection) _tourViewer_Tree.getSelection();

      if (selection.size() == 0) {
         return;
      }

      final String defaultExportFilePath = _state.get(STATE_CSV_EXPORT_PATH);

      final String defaultExportFileName = CSV_EXPORT_DEFAULT_FILE_NAME
            + TimeTools.now().format(TimeTools.Formatter_FileName)
            + UI.SYMBOL_DOT
            + Util.CSV_FILE_EXTENSION;

      /*
       * get export filename
       */
      final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(Messages.dialog_export_file_dialog_text);

      dialog.setFilterPath(defaultExportFilePath);
      dialog.setFilterExtensions(new String[] { Util.CSV_FILE_EXTENSION });
      dialog.setFileName(defaultExportFileName);

      final String selectedFilePath = dialog.open();
      if (selectedFilePath == null) {
         return;
      }

      final File exportFilePath = new Path(selectedFilePath).toFile();

      // keep export path
      _state.put(STATE_CSV_EXPORT_PATH, exportFilePath.getPath());

      if (exportFilePath.exists()) {
         if (net.tourbook.ui.UI.confirmOverwrite(exportFilePath) == false) {
            // don't overwrite file, nothing more to do
            return;
         }
      }

      new CSVExport(selection, selectedFilePath, this);

//      // DEBUGGING: USING DEFAULT PATH
//      final IPath path = new Path(defaultExportFilePath).removeLastSegments(1).append(defaultExportFileName);
//
//      new CSVExport(selection, path.toOSString());
   }

   void actionSelectYearMonthTours() {

      if (_actionSelectAllTours.isChecked()) {

         // reselect selection
         _tourViewer_Tree.setSelection(_tourViewer_Tree.getSelection());
      }
   }

   /**
    * Toggle view layout, when the <Ctrl> key is pressed, then the toggle action is reversed.
    * <p>
    * <code>
    * Forward:    month    -> week        -> table   -> natTable  -> month...<br>
    * Reverse:    month    -> natTable    -> table   -> week      -> month...
    * </code>
    *
    * @param event
    */
   void actionToggleViewLayout(final Event event) {

      final boolean isForwards = UI.isCtrlKey(event) == false;

      if (_viewLayout == TourBookViewLayout.CATEGORY_MONTH) {

         if (isForwards) {

            // month -> week

            toggleLayout_Category_Week();

         } else {

            // month -> natTable

            toggleLayout_NatTable();
         }

      } else if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         if (isForwards) {

            // week -> table

            toggleLayout_Table();

         } else {

            // week -> month

            toggleLayout_Category_Month();
         }

      } else if (_viewLayout == TourBookViewLayout.TABLE) {

         if (isForwards) {

            // table -> natTable

            toggleLayout_NatTable();

         } else {

            // table -> week

            toggleLayout_Category_Week();
         }

      } else if (_viewLayout == TourBookViewLayout.NAT_TABLE) {

         if (isForwards) {

            // natTable -> month

            toggleLayout_Category_Month();

         } else {

            // natTable -> table

            toggleLayout_Table();
         }
      }

      reopenFirstSelectedTour();
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourBookView.this) {

               // ensure the tour tooltip is hidden, it occured that even closing this view did not close the tooltip
               if (_tourInfoToolTip_Tree != null) {
                  _tourInfoToolTip_Tree.hideToolTip();
               }
            }
         }

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               reloadViewer();

            } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

               // update tourbook viewer
               _tourViewer_Tree.refresh();

               // redraw must be done to see modified tour type image colors
               _tourViewer_Tree.getTree().redraw();

            } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

               updateToolTipState();

            } else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               _columnManager_Table.saveState(_state_Table);
               _columnManager_Table.clearColumns();

               _columnManager_Tree.saveState(_state_Tree);
               _columnManager_Tree.clearColumns();

               defineAllColumns();

               _tourViewer_Tree = (TreeViewer) recreateViewer(_tourViewer_Tree);

            } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _tourViewer_Tree.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _tourViewer_Tree.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
                */
               _tourViewer_Tree.getTree().redraw();
            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);

      /*
       * Common preferences
       */
      _prefChangeListenerCommon = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

               _tourViewer_Table = (TableViewer) recreateViewer(_tourViewer_Table);
               _tourViewer_Tree = (TreeViewer) recreateViewer(_tourViewer_Tree);
            }
         }
      };

      // register the listener
      _prefStoreCommon.addPropertyChangeListener(_prefChangeListenerCommon);
   }

   private void addSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = new ISelectionListener() {

         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            // prevent to listen to a selection which is originated by this year chart
            if (part == TourBookView.this) {
               return;
            }

            onSelectionChanged(selection);
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourPropertyListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourBookView.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

               /*
                * it is possible when a tour type was modified, the tour can be hidden or visible in
                * the viewer because of the tour type filter
                */
               reloadViewer();

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
                  || eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

               reloadViewer();
            }
         }
      };
      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {

      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

      _subMenu_AdjustTourValues = new SubMenu_AdjustTourValues(this, this);
      _subMenu_Reimport = new Action_Reimport_SubMenu(this);

      _actionCollapseAll = new ActionCollapseAll(this);
      _actionCollapseOthers = new ActionCollapseOthers(this);
      _actionDuplicateTour = new ActionDuplicateTour(this);
      _actionDeleteTour = new ActionDeleteTourMenu(this);
      _actionEditQuick = new ActionEditQuick(this);
      _actionEditTour = new ActionEditTour(this);
      _actionExpandSelection = new ActionExpandSelection(this);
      _actionExportTour = new ActionExport(this);
      _actionExportViewCSV = new ActionExportViewCSV(this);
      _actionJoinTours = new ActionJoinTours(this);
      _actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
      _actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
      _actionMergeTour = new ActionMergeTour(this);
      _actionModifyColumns = new ActionModifyColumns(this);
      _actionOpenTour = new ActionOpenTour(this);
      _actionPrintTour = new ActionPrint(this);
      _actionRefreshView = new ActionRefreshView(this);
      _actionSetOtherPerson = new ActionSetPerson(this);
      _actionSetTourType = new ActionSetTourTypeMenu(this);
      _actionSelectAllTours = new ActionSelectAllTours(this);
      _actionToggleViewLayout = new ActionToggleViewLayout(this);
      _actionTourBookOptions = new ActionTourBookOptions();

      _actionLinkWithOtherViews = new ActionLinkWithOtherViews();

      fillActionBars();
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);

      _viewerMenuManager_Table = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager_Table.setRemoveAllWhenShown(true);
      _viewerMenuManager_Table.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {

            _tourInfoToolTip_Table.hideToolTip();

            fillContextMenu(manager);
         }
      });

      _viewerMenuManager_Tree = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager_Tree.setRemoveAllWhenShown(true);
      _viewerMenuManager_Tree.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {

            _tourInfoToolTip_Tree.hideToolTip();

            fillContextMenu(manager);
         }
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      initUI(parent);
      restoreState_BeforeUI();

      createMenuManager();

      // define all columns for the viewer
      _columnManager_Table = new ColumnManager(this, _state_Table);
      _columnManager_Table.setIsCategoryAvailable(true);

      _columnManager_Tree = new ColumnManager(this, _state_Tree);
      _columnManager_Tree.setIsCategoryAvailable(true);

      defineAllColumns();

      createUI(parent);
      createActions();

      addSelectionListener();
      addPartListener();
      addPrefListener();
      addTourEventListener();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      restoreState();

      enableActions();

      // update the viewer

      // delay loading, that the app filters are initialized
      Display.getCurrent().asyncExec(() -> {

         if (_tourViewer_Tree.getTree().isDisposed()) {
            return;
         }

         _isInStartup = true;

         setupTourViewerContent();

         reselectTourViewer();

         restoreState_AfterUI();
      });
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _viewerContainer_Tree = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_Tree);
      {
         createUI_20_TourViewer_Tree(_viewerContainer_Tree);
      }

      _viewerContainer_Table = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_Table);
      {
         createUI_30_TourViewer_Table(_viewerContainer_Table);
      }

      _viewerContainer_NatTable = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_NatTable);
      {
         createUI_40_TourViewer_NatTable(_viewerContainer_NatTable);
      }
   }

   private void createUI_20_TourViewer_Tree(final Composite parent) {

      // must be called before the columns are created
      updateUI_TourViewerColumns_Tree();

      // tour tree
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer_Tree = new TreeViewer(tree);
      _columnManager_Tree.createColumns(_tourViewer_Tree);

      _tourViewer_Tree.setComparer(new ItemComparer_Tree());
      _tourViewer_Tree.setUseHashlookup(true);

      _tourViewer_Tree.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_TreeItem(event);
         }
      });

      _tourViewer_Tree.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {

            final Object selection = ((IStructuredSelection) _tourViewer_Tree.getSelection()).getFirstElement();

            if (selection instanceof TVITourBookTour) {

               TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);

            } else if (selection != null) {

               // expand/collapse current item

               final TreeViewerItem tourItem = (TreeViewerItem) selection;

               if (_tourViewer_Tree.getExpandedState(tourItem)) {
                  _tourViewer_Tree.collapseToLevel(tourItem, 1);
               } else {
                  _tourViewer_Tree.expandToLevel(tourItem, 1);
               }
            }
         }
      });

      /*
       * The context menu must be created after the viewer is created which is also done after the
       * measurement system has changed
       */
      createUI_50_ContextMenu_Tree();

      // set tour info tooltip provider
      _tourInfoToolTip_Tree = new TreeViewerTourInfoToolTip(_tourViewer_Tree);
   }

   private void createUI_30_TourViewer_Table(final Composite parent) {

      // must be called before the columns are created
      updateUI_TourViewerColumns_Table();

      // tour tree
      final Table Table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);

      Table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      Table.setHeaderVisible(true);
      Table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer_Table = new TableViewer(Table);
      _columnManager_Table.createColumns(_tourViewer_Table);

      // virtual table do not use the comparator to do the sort, this must be done by myself
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=145061#c1
      // _tourViewer_Table.setComparator(_tourViewer_Table_Comparator);
      _tourViewer_Table.setUseHashlookup(true);

      _tourViewer_Table.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelect_TableItem(event);
         }
      });

      _tourViewer_Table.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {

            final Object selection = ((IStructuredSelection) _tourViewer_Table.getSelection()).getFirstElement();

            if (selection instanceof TVITourBookTour) {

               TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);
            }
         }
      });

      // show the sorting indicator in the viewer
      updateUI_ShowSortDirection(
            _tourViewer_Table_Comparator.__sortColumnId,
            _tourViewer_Table_Comparator.__sortDirection);

      /*
       * The context menu must be created after the viewer is created which is also done after the
       * measurement system has changed
       */
      createUI_50_ContextMenu_Table();

      // set tour info tooltip provider
      _tourInfoToolTip_Table = new TableViewerTourInfoToolTip(_tourViewer_Table);
   }

   private void createUI_40_TourViewer_NatTable(final Composite parent) {

      // for testing
      if (_columnManager_Table == null) {
         _columnManager_Table = new ColumnManager(this, _state_Table);
      }

      _columnManager_Table.setupNatTableColumns();

      _natTable_DataProvider = new DataProvider(this, _columnManager_Table);
      final ArrayList<ColumnDefinition> allSortedColumns = _natTable_DataProvider.allSortedColumns;

      final String sortColumnId = _tourViewer_Table_Comparator.__sortColumnId;
      final int sortDirection = _tourViewer_Table_Comparator.__sortDirection;

      _natTable_DataProvider.setSortColumn(sortColumnId, sortDirection);

      /*
       * Create: Body layer
       */
      final IRowDataProvider<TVITourBookTour> body_DataProvider = new DataProvider_Tour(_natTable_DataProvider);
      _natTable_DataLayer_Body = new DataLayer(body_DataProvider);

      /*
       * Create: Hover layer
       */
//      final HoverLayer body_HoverLayer = new HoverLayer(body_DataLayer, false);
//      // we need to ensure that the hover styling is removed when the mouse
//      // cursor moves out of the cell area
//      body_HoverLayer.addConfiguration(new SimpleHoverStylingBindings(body_HoverLayer));

      final HoverLayer body_HoverLayer = new HoverLayer(_natTable_DataLayer_Body);

      /*
       * Create: Column drag&drop layer
       */
      _natTable_ColumnReorder_Layer = new ColumnReorderLayer(body_HoverLayer);

      /*
       * Create: Selection layer
       */
      // create a SelectionLayer without using the default configuration
      // this enables us to add the row selection configuration cleanly
      // afterwards
      final SelectionLayer selection_Layer = new SelectionLayer(_natTable_ColumnReorder_Layer, false);

      // register the DefaultRowSelectionLayerConfiguration that contains the
      // default styling and functionality bindings (search, tick update)
      // and different configurations for a move command handler that always
      // moves by a row and row only selection bindings
      selection_Layer.addConfiguration(new DefaultRowSelectionLayerConfiguration());

// have not yet understood how this works !!!
//
      // use a RowSelectionModel that will perform row selections and is able
      // to identify a row via unique ID
//      selection_Layer.setSelectionModel(new RowSelectionModel<>(selection_Layer, body_DataProvider, new IRowIdAccessor<Person>() {
//
//         @Override
//         public Serializable getRowId(final Person rowObject) {
//            return rowObject.getId();
//         }
//
//      }));

      /*
       * Create: Grid viewport layer
       */
      _natTable_Grid_BodyLayer = new ViewportLayer(selection_Layer);
      _natTable_Grid_BodyLayer.addConfiguration(new NatTable_ConfigField_TourType(body_DataProvider));
      _natTable_Grid_BodyLayer.addConfiguration(new NatTable_ConfigField_Weather(body_DataProvider));

      /*
       * Create: Column header layer
       */
      final IDataProvider columnHeader_DataProvider = new DataProvider_ColumnHeader(_natTable_DataProvider, _columnManager_Table);
      final DataLayer columnHeader_DataLayer = new DataLayer(columnHeader_DataProvider);
      final ILayer columnHeader_Layer = new ColumnHeaderLayer(columnHeader_DataLayer, _natTable_Grid_BodyLayer, selection_Layer);

      /*
       * Create: Row header layer
       */
      final DefaultRowHeaderDataProvider rowHeader_DataProvider = new DefaultRowHeaderDataProvider(body_DataProvider);
      final DefaultRowHeaderDataLayer rowHeader_DataLayer = new DefaultRowHeaderDataLayer(rowHeader_DataProvider);
      final ILayer rowHeader_Layer = new RowHeaderLayer(rowHeader_DataLayer, _natTable_Grid_BodyLayer, selection_Layer);

      /*
       * Create: Corner layer
       */
      final DefaultCornerDataProvider corner_DataProvider = new DefaultCornerDataProvider(columnHeader_DataProvider, rowHeader_DataProvider);
      final DataLayer corner_DataLayer = new DataLayer(corner_DataProvider);
      final ILayer corner_Layer = new CornerLayer(corner_DataLayer, rowHeader_Layer, columnHeader_Layer);

      /*
       * Create: Grid layer composed with the prior created layer stacks
       */
      final GridLayer gridLayer = new GridLayer(_natTable_Grid_BodyLayer, columnHeader_Layer, rowHeader_Layer, corner_Layer);

      /*
       * Setup other data
       */
      natTable_SetColumnWidths(allSortedColumns, _natTable_DataLayer_Body);
      natTable_RegisterColumnLabels(allSortedColumns, _natTable_DataLayer_Body, columnHeader_DataLayer);

      /*
       * Create: Table
       */
      // turn the auto configuration off as we want to add our hover styling configuration
      _tourViewer_NatTable = new NatTable(parent, gridLayer, false);

      /*
       * Configure table
       */

      // as the autoconfiguration of the NatTable is turned off, we have to add the DefaultNatTableStyleConfiguration manually
      _tourViewer_NatTable.addConfiguration(new DefaultNatTableStyleConfiguration());

      _tourViewer_NatTable.addConfiguration(new NatTable_Configuration_CellStyle(_natTable_DataProvider.allSortedColumns));

      // add the style configuration for hover
      _tourViewer_NatTable.addConfiguration(new NatTable_Configuration_Hover());

      _tourViewer_NatTable.configure();

      // overwrite theme with MT's own theme based on the modern theme
      _tourViewer_NatTable.setTheme(new NatTable_Configuration_Theme());

      GridDataFactory.fillDefaults().grab(true, true).applyTo(_tourViewer_NatTable);
   }

   /**
    * Setup context menu for the viewer
    */
   private void createUI_50_ContextMenu_Table() {

      _contextMenu_Table = createUI_52_CreateViewerContextMenu_Table();

      final Table table = (Table) _tourViewer_Table.getControl();

      _columnManager_Table.createHeaderContextMenu(table, _viewerContextMenuProvider_Table);
   }

   /**
    * Setup context menu for the viewer
    */
   private void createUI_50_ContextMenu_Tree() {

      _contextMenu_Tree = createUI_52_CreateViewerContextMenu_Tree();

      final Tree tree = (Tree) _tourViewer_Tree.getControl();

      _columnManager_Tree.createHeaderContextMenu(tree, _viewerContextMenuProvider_Tree);
   }

   /**
    * Creates context menu for the viewer
    *
    * @return Returns the {@link Menu} widget
    */
   private Menu createUI_52_CreateViewerContextMenu_Table() {

      final Table table = (Table) _tourViewer_Table.getControl();

      final Menu treeContextMenu = _viewerMenuManager_Table.createContextMenu(table);

      treeContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, table, Display.getCurrent().getCursorLocation(), _tourInfoToolTip_Tree);
         }
      });

      return treeContextMenu;
   }

   /**
    * Creates context menu for the viewer
    *
    * @return Returns the {@link Menu} widget
    */
   private Menu createUI_52_CreateViewerContextMenu_Tree() {

      final Tree tree = (Tree) _tourViewer_Tree.getControl();

      final Menu treeContextMenu = _viewerMenuManager_Tree.createContextMenu(tree);

      treeContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip_Tree);
         }
      });

      return treeContextMenu;
   }

   /**
    * Defines all columns for the table viewer in the column manager, the sequence defines the
    * default columns
    * <p>
    * All columns <b>MUST</b> also be defined in {@link CSVExport}
    *
    * @param parent
    */
   private void defineAllColumns() {

      defineColumn_0_RowNumbering();

      // Time
      defineColumn_1_Date();
      defineColumn_Time_WeekDay();
      defineColumn_Time_TourStartTime();
      defineColumn_Time_TimeZoneDifference();
      defineColumn_Time_TimeZone();
      defineColumn_Time_MovingTime();
      defineColumn_Time_RecordingTime();
      defineColumn_Time_PausedTime();
      defineColumn_Time_PausedTime_Relative();
      defineColumn_Time_WeekNo();
      defineColumn_Time_WeekYear();

      // Tour
      defineColumn_Tour_TypeImage();
      defineColumn_Tour_TypeText();
      defineColumn_Tour_Title();
      defineColumn_Tour_Marker();
      defineColumn_Tour_Photos();
      defineColumn_Tour_Tags();
      defineColumn_Tour_Location_Start();
      defineColumn_Tour_Location_End();
//    defineColumn_Tour_TagIds();            // for debugging

      // Motion / Bewegung
      defineColumn_Motion_Distance();
      defineColumn_Motion_MaxSpeed();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();

      // Elevation
      defineColumn_Elevation_Up();
      defineColumn_Elevation_Down();
      defineColumn_Elevation_Max();
      defineColumn_Elevation_AvgChange();

      // Weather
      defineColumn_Weather_Clouds();
      defineColumn_Weather_Temperature_Avg();
      defineColumn_Weather_Temperature_Min();
      defineColumn_Weather_Temperature_Max();
      defineColumn_Weather_WindSpeed();
      defineColumn_Weather_WindDirection();

      // Body
      defineColumn_Body_Calories();
      defineColumn_Body_RestPulse();
      defineColumn_Body_MaxPulse();
      defineColumn_Body_AvgPulse();
      defineColumn_Body_Weight();
      defineColumn_Body_Person();

      // Power - Leistung
      defineColumn_Power_Avg();
      defineColumn_Power_Max();
      defineColumn_Power_Normalized();
      defineColumn_Power_TotalWork();

      // Powertrain - Antrieb/Pedal
      defineColumn_Powertrain_AvgCadence();
      defineColumn_Powertrain_SlowVsFastCadencePercentage();
      defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter();
      defineColumn_Powertrain_CadenceMultiplier();
      defineColumn_Powertrain_Gear_FrontShiftCount();
      defineColumn_Powertrain_Gear_RearShiftCount();
      defineColumn_Powertrain_AvgLeftPedalSmoothness();
      defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
      defineColumn_Powertrain_AvgRightPedalSmoothness();
      defineColumn_Powertrain_AvgRightTorqueEffectiveness();
      defineColumn_Powertrain_PedalLeftRightBalance();

      // Training - Trainingsanalyse
      defineColumn_Training_FTP();
      defineColumn_Training_PowerToWeightRatio();
      defineColumn_Training_IntensityFactor();
      defineColumn_Training_StressScore();
      defineColumn_Training_TrainingEffect();
      defineColumn_Training_TrainingEffect_Anaerobic();
      defineColumn_Training_TrainingPerformance();

      // Running dynamics
      defineColumn_RunDyn_StanceTime_Min();
      defineColumn_RunDyn_StanceTime_Max();
      defineColumn_RunDyn_StanceTime_Avg();

      defineColumn_RunDyn_StanceTimeBalance_Min();
      defineColumn_RunDyn_StanceTimeBalance_Max();
      defineColumn_RunDyn_StanceTimeBalance_Avg();

      defineColumn_RunDyn_StepLength_Min();
      defineColumn_RunDyn_StepLength_Max();
      defineColumn_RunDyn_StepLength_Avg();

      defineColumn_RunDyn_VerticalOscillation_Min();
      defineColumn_RunDyn_VerticalOscillation_Max();
      defineColumn_RunDyn_VerticalOscillation_Avg();

      defineColumn_RunDyn_VerticalRatio_Min();
      defineColumn_RunDyn_VerticalRatio_Max();
      defineColumn_RunDyn_VerticalRatio_Avg();

      // Surfing
      defineColumn_Surfing_NumberOfEvents();
      defineColumn_Surfing_MinSpeed_StartStop();
      defineColumn_Surfing_MinSpeed_Surfing();
      defineColumn_Surfing_MinTimeDuration();
      defineColumn_Surfing_MinDistance();

      // Device
      defineColumn_Device_Name();
      defineColumn_Device_Distance();

      // Data
      defineColumn_Data_DPTolerance();
      defineColumn_Data_ImportFilePath();
      defineColumn_Data_ImportFileName();
      defineColumn_Data_TimeInterval();
      defineColumn_Data_NumTimeSlices();
   }

   /**
    * Column: #
    * <p>
    * This is only used for the table view.
    */
   private void defineColumn_0_RowNumbering() {

//      {
//         // Column: 1st column will be hidden because the alignment for the first column is always to the left
//
//         final ColumnDefinition colDef = TableColumnFactory.DATA_FIRST_COLUMN.createColumn(_columnManager_Table, _pc);
//
//         colDef.setIsDefaultColumn();
//         colDef.setCanModifyVisibility(true);
//         colDef.setIsColumnMoveable(true);
//         colDef.setHideColumn();
//         colDef.setLabelProvider(new CellLabelProvider() {
//            @Override
//            public void update(final ViewerCell cell) {}
//         });
//      }

//      {
//         // Column: #
//
//         final ColumnDefinition colDef = TableColumnFactory.DATA_SEQUENCE.createColumn(_columnManager_Table, _pc);
//
//         colDef.setIsDefaultColumn();
//         colDef.setCanModifyVisibility(true);
//         colDef.setIsColumnMoveable(true);
//         colDef.setLabelProvider(new CellLabelProvider() {
//            @Override
//            public void update(final ViewerCell cell) {
//
//               final int tourSequence = ((TVITourBookItem) cell.getElement()).col_Sequence;
//
//               cell.setText(Integer.toString(tourSequence));
//            }
//         });
//      }
   }

   /**
    * Column: Date
    */
   private void defineColumn_1_Date() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_DATE.createColumn(_columnManager_Table, _pc);

      colDef_Table.setIsDefaultColumn();
      colDef_Table.setCanModifyVisibility(false);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Date == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookItem) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITourBookTour tourItem = (TVITourBookTour) (TVITourBookItem) element;

            // show full date
            cell.setText(tourItem.colDateTime_Text);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final TVITourBookTour tourItem = (TVITourBookTour) (TVITourBookItem) element;

            // show full date
            return tourItem.colDateTime_Text;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_DATE.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setCanModifyVisibility(false);

      colDef_Tree.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Date == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookItem) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITourBookItem tviItem = (TVITourBookItem) element;

            if (element instanceof TVITourBookTour) {

               // tour item

               final TVITourBookTour tourItem = (TVITourBookTour) tviItem;

               if (_isLayoutTable) {

                  // show full date
                  cell.setText(tourItem.colDateTime_Text);

               } else {

                  // show day only
                  cell.setText(tourItem.treeColumn);
               }

            } else {

               // year/month or week item

               final StyledString styledString = new StyledString();

               boolean isShowSummaryRow = false;
               if (element instanceof TVITourBookYear && _isShowSummaryRow) {
                  isShowSummaryRow = ((TVITourBookYear) element).isRowSummary;
               }

               if (isShowSummaryRow) {

                  // show summary row

                  styledString.append(Messages.Tour_Book_Label_Total);
               } else {
                  styledString.append(tviItem.treeColumn);
               }

               styledString.append(UI.SPACE3);
               styledString.append(Long.toString(tviItem.colCounter), StyledString.QUALIFIER_STYLER);

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Body - Avg pulse
    */
   private void defineColumn_Body_AvgPulse() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.BODY_AVG_PULSE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgPulse;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colAvgPulse;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgPulse;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Calories
    */
   private void defineColumn_Body_Calories() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.BODY_CALORIES.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colCalories / 1000.0;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colCalories / 1000.0;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_CALORIES.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colCalories / 1000.0;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Max pulse
    */
   private void defineColumn_Body_MaxPulse() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colMaxPulse;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colMaxPulse;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colMaxPulse;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Person
    */
   private void defineColumn_Body_Person() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long dbPersonId = ((TVITourBookTour) element).colPersonId;

               cell.setText(PersonManager.getPersonName(dbPersonId));
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.BODY_PERSON.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbPersonId = ((TVITourBookTour) element).colPersonId;

            return PersonManager.getPersonName(dbPersonId);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_PERSON.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Body - Rest pulse
    */
   private void defineColumn_Body_RestPulse() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int restPulse = ((TVITourBookItem) element).colRestPulse;

            if (restPulse == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(restPulse));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.BODY_RESTPULSE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int restPulse = ((TVITourBookItem) element).colRestPulse;

            if (restPulse == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(restPulse);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_RESTPULSE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Body - Body weight
    */
   private void defineColumn_Body_Weight() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double weight = UI.convertBodyWeightFromMetric(((TVITourBookItem) element).colBodyWeight);

            if (weight == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(weight));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.BODY_WEIGHT.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double weight = UI.convertBodyWeightFromMetric(((TVITourBookItem) element).colBodyWeight);

            if (weight == 0) {
               return UI.EMPTY_STRING;
            } else {
               return _nf1.format(weight);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_WEIGHT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Data - DP tolerance
    */
   private void defineColumn_Data_DPTolerance() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int dpTolerance = ((TVITourBookItem) element).colDPTolerance;

            if (dpTolerance == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(dpTolerance / 10.0));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.DATA_DP_TOLERANCE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int dpTolerance = ((TVITourBookItem) element).colDPTolerance;

            if (dpTolerance == 0) {
               return UI.EMPTY_STRING;
            } else {
               return _nf1.format(dpTolerance / 10.0);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_DP_TOLERANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Data - Import filename
    */
   private void defineColumn_Data_ImportFileName() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(((TVITourBookTour) element).col_ImportFileName);

               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.DATA_IMPORT_FILE_NAME.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            return ((TVITourBookTour) element).col_ImportFileName;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_IMPORT_FILE_NAME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Data - Import filepath
    */
   private void defineColumn_Data_ImportFilePath() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(((TVITourBookTour) element).col_ImportFilePath);
               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.DATA_IMPORT_FILE_PATH.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            return ((TVITourBookTour) element).col_ImportFilePath;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_IMPORT_FILE_PATH.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Data - Number of time slices
    */
   private void defineColumn_Data_NumTimeSlices() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.DATA_NUM_TIME_SLICES.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colNumberOfTimeSlices;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colNumberOfTimeSlices;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_NUM_TIME_SLICES.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colNumberOfTimeSlices;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Data - Time interval
    */

   private void defineColumn_Data_TimeInterval() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final short dbTimeInterval = ((TVITourBookTour) element).getColumnTimeInterval();
               if (dbTimeInterval == 0) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(Long.toString(dbTimeInterval));
               }

               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short dbTimeInterval = ((TVITourBookTour) element).getColumnTimeInterval();
            if (dbTimeInterval == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Long.toString(dbTimeInterval);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Device - Device distance
    */
   private void defineColumn_Device_Distance() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();
               final double value = dbStartDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

               colDef_Table.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();
            final double value = dbStartDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();
               final double value = dbStartDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

               colDef_Tree.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Device - Device name
    */
   private void defineColumn_Device_Name() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String dbValue = ((TVITourBookItem) element).col_DeviceName;

            if (dbValue == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(dbValue);
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.DEVICE_NAME.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String dbValue = ((TVITourBookItem) element).col_DeviceName;

            if (dbValue == null) {
               return UI.EMPTY_STRING;
            } else {
               return dbValue;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DEVICE_NAME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Elevation - Average elevation change (m/km or ft/mi)
    */
   private void defineColumn_Elevation_AvgChange() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.ALTITUDE_AVG_CHANGE.createColumn(_columnManager_Table, _pc);

      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);

      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float dbAvgAltitudeChange = ((TVITourBookItem) element).colAltitude_AvgChange
                  / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Table.printValue_0(cell, dbAvgAltitudeChange);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final float value = ((TVITourBookItem) element).colAltitude_AvgChange
                  / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_AVG_CHANGE.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setIsDefaultColumn();

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float dbAvgAltitudeChange = ((TVITourBookItem) element).colAltitude_AvgChange / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printValue_0(cell, dbAvgAltitudeChange);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Elevation down (m)
    */
   private void defineColumn_Elevation_Down() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;
            final double value = -dbAltitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;
            final double value = -dbAltitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;
            final double value = -dbAltitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Max elevation
    */
   private void defineColumn_Elevation_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.ALTITUDE_MAX.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;
            final double value = dbMaxAltitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;
            final double value = dbMaxAltitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_MAX.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;
            final double value = dbMaxAltitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Elevation up (m)
    */
   private void defineColumn_Elevation_Up() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP.createColumn(_columnManager_Table, _pc);

      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;
            final double value = dbAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;
            final double value = dbAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setIsDefaultColumn();

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;
            final double value = dbAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Avg pace min/km - min/mi
    */
   private void defineColumn_Motion_AvgPace() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double pace = ((TVITourBookItem) element).colAvgPace * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            if (pace == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(UI.format_mm_ss((long) pace));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double pace = ((TVITourBookItem) element).colAvgPace * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            if (pace == 0) {
               return UI.EMPTY_STRING;
            } else {
               return UI.format_mm_ss((long) pace);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Motion - Avg speed km/h - mph
    */
   private void defineColumn_Motion_AvgSpeed() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colAvgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Distance (km/miles)
    */
   private void defineColumn_Motion_Distance() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.MOTION_DISTANCE.createColumn(_columnManager_Table, _pc);

      colDef_Table.setIsDefaultColumn();

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTourDistance
                  / 1000.0
                  / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTourDistance
                  / 1000.0
                  / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setIsDefaultColumn();

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTourDistance
                  / 1000.0
                  / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Max speed
    */
   private void defineColumn_Motion_MaxSpeed() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colMaxSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colMaxSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colMaxSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Avg power
    */
   private void defineColumn_Power_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWER_AVG.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_Avg;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_Avg;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_AVG.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_Avg;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Max power
    */
   private void defineColumn_Power_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWER_MAX.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_Max;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colPower_Max;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_MAX.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_Max;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Normalized power
    */
   private void defineColumn_Power_Normalized() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWER_NORMALIZED.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_Normalized;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colPower_Normalized;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_NORMALIZED.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_Normalized;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Total work
    */
   private void defineColumn_Power_TotalWork() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWER_TOTAL_WORK.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_TotalWork / 1000_000.0;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_TotalWork / 1000_000.0;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_TOTAL_WORK.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_TotalWork / 1000_000.0;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Avg cadence
    */
   private void defineColumn_Powertrain_AvgCadence() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgCadence;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colAvgCadence;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgCadence;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgLeftPedalSmoothness
    */
   private void defineColumn_Powertrain_AvgLeftPedalSmoothness() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgLeftPedalSmoothness;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgLeftPedalSmoothness;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgLeftPedalSmoothness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgLeftTorqueEffectiveness
    */
   private void defineColumn_Powertrain_AvgLeftTorqueEffectiveness() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgLeftTorqueEffectiveness;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgLeftTorqueEffectiveness;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgLeftTorqueEffectiveness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgRightPedalSmoothness
    */
   private void defineColumn_Powertrain_AvgRightPedalSmoothness() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgRightPedalSmoothness;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgRightPedalSmoothness;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgRightPedalSmoothness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgRightTorqueEffectiveness
    */
   private void defineColumn_Powertrain_AvgRightTorqueEffectiveness() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgRightTorqueEffectiveness;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgRightTorqueEffectiveness;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgRightTorqueEffectiveness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Cadence multiplier
    */
   private void defineColumn_Powertrain_CadenceMultiplier() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double dbCadenceMultiplier = ((TVITourBookItem) element).colCadenceMultiplier;

            if (dbCadenceMultiplier == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(dbCadenceMultiplier));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double dbCadenceMultiplier = ((TVITourBookItem) element).colCadenceMultiplier;

            if (dbCadenceMultiplier == 0) {
               return UI.EMPTY_STRING;
            } else {
               return _nf1.format(dbCadenceMultiplier);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Powertrain - Front shift count.
    */
   private void defineColumn_Powertrain_Gear_FrontShiftCount() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colFrontShiftCount;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colFrontShiftCount;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colFrontShiftCount;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Rear shift count.
    */
   private void defineColumn_Powertrain_Gear_RearShiftCount() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colRearShiftCount;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colRearShiftCount;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colRearShiftCount;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Pedal left/right balance
    */

   private void defineColumn_Powertrain_PedalLeftRightBalance() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_PedalLeftRightBalance;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colPower_PedalLeftRightBalance;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_PedalLeftRightBalance;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Slow vs fast cadence Percentage
    */
   private void defineColumn_Powertrain_SlowVsFastCadencePercentage() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String value = ((TVITourBookItem) element).colSlowVsFastCadence;

            cell.setText(value);

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES.createColumn(_columnManager_Table,
            _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String value = ((TVITourBookItem) element).colSlowVsFastCadence;

            return value;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Powertrain - Cadence zones delimiter value
    */
   private void defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER.createColumn(_columnManager_Table,
            _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colCadenceZonesDelimiter;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colCadenceZonesDelimiter;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER.createColumn(_columnManager_Tree,
            _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colCadenceZonesDelimiter;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time max
    */
   private void defineColumn_RunDyn_StanceTime_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STANCE_TIME_AVG.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Avg;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Avg;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Avg;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time max
    */
   private void defineColumn_RunDyn_StanceTime_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STANCE_TIME_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Max;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Max;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Max;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time min
    */
   private void defineColumn_RunDyn_StanceTime_Min() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STANCE_TIME_MIN.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Min;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Min;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Min;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time balance avg
    */
   private void defineColumn_RunDyn_StanceTimeBalance_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Avg;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Avg;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Avg;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time balance max
    */
   private void defineColumn_RunDyn_StanceTimeBalance_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Max;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Max;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Max;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time balance min
    */
   private void defineColumn_RunDyn_StanceTimeBalance_Min() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Min;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Min;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Min;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Step length avg
    */
   private void defineColumn_RunDyn_StepLength_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STEP_LENGTH_AVG.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Avg
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Table.printValue_0(cell, value);
            } else {
               colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Avg
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               return colDef_Table.printValue_0(value);
            } else {
               return colDef_Table.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STEP_LENGTH_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Avg
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Step length max
    */
   private void defineColumn_RunDyn_StepLength_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STEP_LENGTH_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Max
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Table.printValue_0(cell, value);
            } else {
               colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Max
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               return colDef_Table.printValue_0(value);
            } else {
               return colDef_Table.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STEP_LENGTH_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Max
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Step length min
    */
   private void defineColumn_RunDyn_StepLength_Min() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_STEP_LENGTH_MIN.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Min
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Table.printValue_0(cell, value);
            } else {
               colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Min
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               return colDef_Table.printValue_0(value);
            } else {
               return colDef_Table.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STEP_LENGTH_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Min
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical oscillation avg
    */
   private void defineColumn_RunDyn_VerticalOscillation_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Avg
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Table.printValue_0(cell, value);
            } else {
               colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Avg
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               return colDef_Table.printValue_0(value);
            } else {
               return colDef_Table.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Avg
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical oscillation max
    */
   private void defineColumn_RunDyn_VerticalOscillation_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Max
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Table.printValue_0(cell, value);
            } else {
               colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Max
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               return colDef_Table.printValue_0(value);
            } else {
               return colDef_Table.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Max
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical oscillation min
    */
   private void defineColumn_RunDyn_VerticalOscillation_Min() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Min
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Table.printValue_0(cell, value);
            } else {
               colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Min
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               return colDef_Table.printValue_0(value);
            } else {
               return colDef_Table.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Min
                  * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_METRIC) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical ratio avg
    */
   private void defineColumn_RunDyn_VerticalRatio_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Avg;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Avg;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Avg;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical ratio max
    */
   private void defineColumn_RunDyn_VerticalRatio_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Max;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Max;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Max;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical ratio min
    */
   private void defineColumn_RunDyn_VerticalRatio_Min() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Min;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Min;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Min;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Surfing - Min distance
    */
   private void defineColumn_Surfing_MinDistance() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinDistance;
            final boolean isMinDistance = ((TVITourBookItem) element).col_Surfing_IsMinDistance;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET || isMinDistance == false) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               int minSurfingDistance = value;

               // convert imperial -> metric
               if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == net.tourbook.ui.UI.UNIT_MILE) {
                  minSurfingDistance = (int) (minSurfingDistance / net.tourbook.ui.UI.UNIT_YARD + 0.5);
               }

               cell.setText(Integer.toString(minSurfingDistance));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.SURFING_MIN_DISTANCE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinDistance;
            final boolean isMinDistance = ((TVITourBookItem) element).col_Surfing_IsMinDistance;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET || isMinDistance == false) {
               return UI.EMPTY_STRING;
            } else {

               int minSurfingDistance = value;

               // convert imperial -> metric
               if (net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == net.tourbook.ui.UI.UNIT_MILE) {
                  minSurfingDistance = (int) (minSurfingDistance / net.tourbook.ui.UI.UNIT_YARD + 0.5);
               }

               return Integer.toString(minSurfingDistance);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_MIN_DISTANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Surfing - Min start/stop speed
    */
   private void defineColumn_Surfing_MinSpeed_StartStop() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_StartStop;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.SURFING_MIN_SPEED_START_STOP.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_StartStop;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_MIN_SPEED_START_STOP.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Surfing - Min surfing speed
    */
   private void defineColumn_Surfing_MinSpeed_Surfing() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_Surfing;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.SURFING_MIN_SPEED_SURFING.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_Surfing;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_MIN_SPEED_SURFING.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Surfing - Min surfing time duration
    */
   private void defineColumn_Surfing_MinTimeDuration() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinTimeDuration;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.SURFING_MIN_TIME_DURATION.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinTimeDuration;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDefTree = TreeColumnFactory.SURFING_MIN_TIME_DURATION.createColumn(_columnManager_Tree, _pc);
      colDefTree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Surfing - Number of surfing events
    */
   private void defineColumn_Surfing_NumberOfEvents() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).col_Surfing_NumberOfEvents;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Long.toString(value));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.SURFING_NUMBER_OF_EVENTS.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).col_Surfing_NumberOfEvents;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Long.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_NUMBER_OF_EVENTS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Time - Moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_DRIVING_TIME.createColumn(_columnManager_Table, _pc);

      colDef_Table.setIsDefaultColumn();

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourDrivingTime;

            colDef_Table.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourDrivingTime;

            return colDef_Table.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_DRIVING_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setIsDefaultColumn();

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourDrivingTime;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - paused time (h)
    */
   private void defineColumn_Time_PausedTime() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_PAUSED_TIME.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colPausedTime;

            colDef_Table.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colPausedTime;

            return colDef_Table.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_PAUSED_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITourBookItem item = (TVITourBookItem) element;

            final long value = item.colPausedTime;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Relative paused time %
    */
   private void defineColumn_Time_PausedTime_Relative() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            /*
             * display paused time relative to the recording time
             */

            final Object element = cell.getElement();
            final TVITourBookItem item = (TVITourBookItem) element;

            final long dbPausedTime = item.colPausedTime;
            final long dbRecordingTime = item.colTourRecordingTime;

            final double relativePausedTime = dbRecordingTime == 0
                  ? 0
                  : (double) dbPausedTime / dbRecordingTime * 100;

            cell.setText(_nf1.format(relativePausedTime));

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_PAUSED_TIME_RELATIVE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final TVITourBookItem item = (TVITourBookItem) element;

            final long dbPausedTime = item.colPausedTime;
            final long dbRecordingTime = item.colTourRecordingTime;

            final double relativePausedTime = dbRecordingTime == 0
                  ? 0
                  : (double) dbPausedTime / dbRecordingTime * 100;

            return _nf1.format(relativePausedTime);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_PAUSED_TIME_RELATIVE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Time - Recording time (h)
    */
   private void defineColumn_Time_RecordingTime() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_RECORDING_TIME.createColumn(_columnManager_Table, _pc);

      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourRecordingTime;

            colDef_Table.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourRecordingTime;

            return colDef_Table.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_RECORDING_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourRecordingTime;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Timezone
    */
   private void defineColumn_Time_TimeZone() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final String timeZoneId = ((TVITourBookTour) element).colTimeZoneId;

               cell.setText(timeZoneId == null ? UI.EMPTY_STRING : timeZoneId);

               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_TIME_ZONE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String timeZoneId = ((TVITourBookTour) element).colTimeZoneId;

            return timeZoneId == null ? UI.EMPTY_STRING : timeZoneId;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_TIME_ZONE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Time - Timezone difference
    */
   private void defineColumn_Time_TimeZoneDifference() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;

               cell.setText(tourDateTime.timeZoneOffsetLabel);

               setCellColor(cell, element);
            }
         }
      };

      _colDef_TimeZoneOffset_Table = TableColumnFactory.TIME_TIME_ZONE_DIFFERENCE.createColumn(_columnManager_Table, _pc);
      _colDef_TimeZoneOffset_Table.setLabelProvider(cellLabelProvider);

      _colDef_TimeZoneOffset_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;

            return tourDateTime.timeZoneOffsetLabel;
         }
      });

      _colDef_TimeZoneOffset_Tree = TreeColumnFactory.TIME_TIME_ZONE_DIFFERENCE.createColumn(_columnManager_Tree, _pc);
      _colDef_TimeZoneOffset_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Time - Tour start time
    */
   private void defineColumn_Time_TourStartTime() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_TOUR_START_TIME.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Time == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;
               final ZonedDateTime tourStartDateTime = tourDateTime.tourZonedDateTime;

               final ValueFormat valueFormatter = colDef_Table.getValueFormat_Detail();

               if (valueFormatter.equals(ValueFormat.TIME_HH_MM_SS)) {
                  cell.setText(tourStartDateTime.format(TimeTools.Formatter_Time_M));
               } else {
                  cell.setText(tourStartDateTime.format(TimeTools.Formatter_Time_S));
               }

               setCellColor(cell, element);
            }
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;
            final ZonedDateTime tourStartDateTime = tourDateTime.tourZonedDateTime;

            final ValueFormat valueFormatter = colDef_Table.getValueFormat_Detail();

            if (valueFormatter.equals(ValueFormat.TIME_HH_MM_SS)) {
               return tourStartDateTime.format(TimeTools.Formatter_Time_M);
            } else {
               return tourStartDateTime.format(TimeTools.Formatter_Time_S);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_TOUR_START_TIME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Time == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;
               final ZonedDateTime tourStartDateTime = tourDateTime.tourZonedDateTime;

               final ValueFormat valueFormatter = colDef_Tree.getValueFormat_Detail();

               if (valueFormatter.equals(ValueFormat.TIME_HH_MM_SS)) {
                  cell.setText(tourStartDateTime.format(TimeTools.Formatter_Time_M));
               } else {
                  cell.setText(tourStartDateTime.format(TimeTools.Formatter_Time_S));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Time - Week day
    */
   private void defineColumn_Time_WeekDay() {

      final TourInfoToolTipCellLabelProvider cellLabelProvider = new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_WeekDay == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(((TVITourBookTour) element).colWeekDay);
               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_WEEK_DAY.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            return ((TVITourBookTour) element).colWeekDay;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_WEEK_DAY.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Time - Week
    */
   private void defineColumn_Time_WeekNo() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int week = ((TVITourBookItem) element).colWeekNo;

            if (week == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(week));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_WEEK_NO.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int week = ((TVITourBookItem) element).colWeekNo;

            if (week == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(week);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_WEEK_NO.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Time - Week year
    */
   private void defineColumn_Time_WeekYear() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int week = ((TVITourBookItem) element).colWeekYear;

            if (week == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(week));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TIME_WEEKYEAR.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int week = ((TVITourBookItem) element).colWeekYear;

            if (week == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(week);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_WEEKYEAR.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Tour end location
    */
   private void defineColumn_Tour_Location_End() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String tourLocation = ((TVITourBookItem) element).colTourLocation_End;

            if (tourLocation == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(tourLocation);
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_LOCATION_END.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String tourLocation = ((TVITourBookItem) element).colTourLocation_End;

            if (tourLocation == null) {
               return UI.EMPTY_STRING;
            } else {
               return tourLocation;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_LOCATION_END.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Tour start location
    */
   private void defineColumn_Tour_Location_Start() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String tourLocation = ((TVITourBookItem) element).colTourLocation_Start;

            if (tourLocation == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(tourLocation);
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_LOCATION_START.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String tourLocation = ((TVITourBookItem) element).colTourLocation_Start;

            if (tourLocation == null) {
               return UI.EMPTY_STRING;
            } else {
               return tourLocation;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_LOCATION_START.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Markers
    */
   private void defineColumn_Tour_Marker() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final ArrayList<Long> markerIds = ((TVITourBookTour) element).getMarkerIds();
               if (markerIds == null) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(_nf0.format(markerIds.size()));
               }

               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_NUM_MARKERS.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final ArrayList<Long> markerIds = ((TVITourBookTour) element).getMarkerIds();
            if (markerIds == null) {
               return UI.EMPTY_STRING;
            } else {
               return _nf0.format(markerIds.size());
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_NUM_MARKERS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Number of photos
    */
   private void defineColumn_Tour_Photos() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_NUM_PHOTOS.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colNumberOfPhotos;

            colDef_Table.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colNumberOfPhotos;

            return colDef_Table.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_NUM_PHOTOS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colNumberOfPhotos;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column for debugging: Tag ids
    */
   @SuppressWarnings("unused")
   private void defineColumn_Tour_TagIds() {

//      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "TOUR_TAG_IDS", SWT.TRAIL); //$NON-NLS-1$
//
//      colDef.setColumnCategory(net.tourbook.ui.Messages.ColumnFactory_Category_Tour);
//      colDef.setColumnLabel("Tag ID");
//      colDef.setColumnHeaderText("Tag ID");
//
//      colDef.setDefaultColumnWidth(30);
//
//      colDef.setLabelProvider(new CellLabelProvider() {
//
//         @Override
//         public void update(final ViewerCell cell) {
//            final Object element = cell.getElement();
//            if (element instanceof TVITourBookTour) {
//
//               final ArrayList<Long> tagIds = ((TVITourBookTour) element).getTagIds();
//               if (tagIds == null) {
//                  cell.setText(UI.EMPTY_STRING);
//               } else {
//
//                  cell.setText(tagIds.stream()
////                      .map(Object::toString)
////                      .sorted()
//                        .map(n -> Long.toString(n))
//                        .collect(Collectors.joining(",")));
//
//                  setCellColor(cell, element);
//               }
//            }
//         }
//      });
   }

   /**
    * Column: Tour - Tags
    */
   private void defineColumn_Tour_Tags() {

      final TourInfoToolTipCellLabelProvider cellLabelProvider = new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Tags == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(TourDatabase.getTagNames(((TVITourBookTour) element).getTagIds()));
               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_TAGS.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {
            return "this is not yet supported";
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Title
    */
   private void defineColumn_Tour_Title() {

      final TourInfoToolTipCellLabelProvider cellLabelProvider = new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Title == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final String colTourTitle = ((TVITourBookTour) element).colTourTitle;

               if (colTourTitle == null) {
                  cell.setText("<NULL>");
               } else {
                  cell.setText(colTourTitle);
               }

               setCellColor(cell, element);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_TITLE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String colTourTitle = ((TVITourBookTour) element).colTourTitle;

            if (colTourTitle == null) {
               return "<NULL>";
            } else {
               return colTourTitle;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Tour type image
    */
   private void defineColumn_Tour_TypeImage() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

               /*
                * when a tour type image is modified, it will keep the same image resource only the
                * content is modified but in the rawDataView the modified image is not displayed
                * compared with the tourBookView which displays the correct image
                */
               cell.setImage(tourTypeImage);
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            /**
             * Tour type image for the NatTable is implemented in
             * net.tourbook.ui.views.tourBook.TourBookView.NatTable_Configuration_TourType
             * <p>
             * When a label provider is not defined then a warning message is displayed from the
             * data provider !
             */
            return UI.EMPTY_STRING;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Tour - Tour type text
    */
   private void defineColumn_Tour_TypeText() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
               cell.setText(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
            }
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
            return net.tourbook.ui.UI.getTourTypeLabel(tourTypeId);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Training - FTP
    */
   private void defineColumn_Training_FTP() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int dbValue = ((TVITourBookItem) element).colTraining_FTP;

            if (dbValue == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(dbValue));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_FTP.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int dbValue = ((TVITourBookItem) element).colTraining_FTP;

            if (dbValue == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(dbValue);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_FTP.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Training - PowerIntensityFactor
    */
   private void defineColumn_Training_IntensityFactor() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_INTENSITY_FACTOR.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_IntensityFactor;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_IntensityFactor;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_INTENSITY_FACTOR.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_IntensityFactor;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   private void defineColumn_Training_PowerToWeightRatio() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_POWER_TO_WEIGHT.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_PowerToWeight;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_PowerToWeight;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_POWER_TO_WEIGHT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_PowerToWeight;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training - PowerTrainingStressScore
    */
   private void defineColumn_Training_StressScore() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_STRESS_SCORE.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingStressScore;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingStressScore;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_STRESS_SCORE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingStressScore;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training: Training Effect
    */
   private void defineColumn_Training_TrainingEffect() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_EFFECT_AEROB.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Aerob;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Aerob;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_TRAINING_EFFECT_AEROB.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Aerob;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training: Training effect anaerobic
    */
   private void defineColumn_Training_TrainingEffect_Anaerobic() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_EFFECT_ANAEROB.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Anaerobic;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Anaerobic;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_TRAINING_EFFECT_ANAEROB.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Anaerobic;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training - Training Performance
    */
   private void defineColumn_Training_TrainingPerformance() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.TRAINING_PERFORMANCE_LEVEL.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingPerformance;

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingPerformance;

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_TRAINING_PERFORMANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingPerformance;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Clouds
    */
   private void defineColumn_Weather_Clouds() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String windClouds = ((TVITourBookItem) element).colClouds;

            if (windClouds == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               final Image img = net.tourbook.common.UI.IMAGE_REGISTRY.get(windClouds);
               if (img != null) {
                  cell.setImage(img);
               } else {
                  cell.setText(windClouds);
               }
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.WEATHER_CLOUDS.createColumn(_columnManager_Table, _pc);
      colDef_Table.setIsDefaultColumn();
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {
            return "image is not yet displayed";
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_CLOUDS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Weather - Avg temperature
    */
   private void defineColumn_Weather_Temperature_Avg() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.WEATHER_TEMPERATURE_AVG.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Avg);

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Avg);

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_TEMPERATURE_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Avg);

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Max temperature
    */
   private void defineColumn_Weather_Temperature_Max() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.WEATHER_TEMPERATURE_MAX.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Max);

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Max);

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_TEMPERATURE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Max);

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Min temperature
    */
   private void defineColumn_Weather_Temperature_Min() {

      final TableColumnDefinition colDef_Table = TableColumnFactory.WEATHER_TEMPERATURE_MIN.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Min);

            colDef_Table.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Min);

            return colDef_Table.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_TEMPERATURE_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Min);

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Wind direction
    */
   private void defineColumn_Weather_WindDirection() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int windDir = ((TVITourBookItem) element).colWindDir;

            if (windDir == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(windDir));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.WEATHER_WIND_DIR.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int windDir = ((TVITourBookItem) element).colWindDir;

            if (windDir == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(windDir);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_WIND_DIR.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   /**
    * Column: Weather - Wind speed
    */
   private void defineColumn_Weather_WindSpeed() {

      final CellLabelProvider cellLabelProvider = new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int windSpeed = (int) (((TVITourBookItem) element).colWindSpd / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

            if (windSpeed == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(windSpeed));
            }

            setCellColor(cell, element);
         }
      };

      final TableColumnDefinition colDef_Table = TableColumnFactory.WEATHER_WIND_SPEED.createColumn(_columnManager_Table, _pc);
      colDef_Table.setColumnSelectionListener(_columnSortListener);
      colDef_Table.setLabelProvider(cellLabelProvider);

      colDef_Table.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int windSpeed = (int) (((TVITourBookItem) element).colWindSpd / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

            if (windSpeed == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(windSpeed);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_WIND_SPEED.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(cellLabelProvider);
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);
      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStoreCommon.removePropertyChangeListener(_prefChangeListenerCommon);

      if (_tableTourProvider != null) {
         _tableTourProvider.dispose();
      }
      if (_rootItem_Tree != null) {
         _rootItem_Tree.clearChildren();
      }

      super.dispose();
   }

   private void enableActions() {

      int numTourItems = 0;
      int numSelectedItems = 0;

      boolean firstElementHasChildren = false;

      TVITourBookItem firstElement = null;
      TVITourBookTour firstTour = null;

      if (_isLayoutTable) {

         final IStructuredSelection selection = _tourViewer_Table.getStructuredSelection();

         numTourItems = numSelectedItems = selection.size();

         firstTour = (TVITourBookTour) (firstElement = (TVITourBookItem) selection.getFirstElement());

      } else if (_isLayoutNatTable) {

      } else {

         final ITreeSelection selection = (ITreeSelection) _tourViewer_Tree.getSelection();

         /*
          * count number of selected items
          */

         for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

            final Object treeItem = iter.next();
            if (treeItem instanceof TVITourBookTour) {
               if (numTourItems == 0) {
                  firstTour = (TVITourBookTour) treeItem;
               }
               numTourItems++;
            }
         }

         firstElement = (TVITourBookItem) selection.getFirstElement();
         firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();
         numSelectedItems = selection.size();
      }

      final boolean isTourSelected = numTourItems > 0;
      final boolean isOneTour = numTourItems == 1;
      final boolean isAllToursSelected = _actionSelectAllTours.isChecked();
      boolean isDeviceTour = false;
      boolean canMergeTours = false;

      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

      TourData firstSavedTour = null;

      if (isOneTour) {
         firstSavedTour = TourManager.getInstance().getTourData(firstTour.getTourId());
      }

      if (firstSavedTour != null) {

         isDeviceTour = firstSavedTour.isManualTour() == false;
         canMergeTours = isOneTour && isDeviceTour && firstSavedTour.getMergeSourceTourId() != null;
      }

      final boolean useWeatherRetrieval = _prefStore.getBoolean(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL) &&
            !_prefStore.getString(ITourbookPreferences.WEATHER_API_KEY).equals(UI.EMPTY_STRING);

      /*
       * enable actions
       */
      _tourDoubleClickState.canEditTour = isOneTour;
      _tourDoubleClickState.canOpenTour = isOneTour;
      _tourDoubleClickState.canQuickEditTour = isOneTour;
      _tourDoubleClickState.canEditMarker = isOneTour;
      _tourDoubleClickState.canAdjustAltitude = isOneTour;

      _subMenu_AdjustTourValues.setEnabled(isTourSelected || isAllToursSelected);
      _subMenu_AdjustTourValues.getActionRetrieveWeatherData().setEnabled(useWeatherRetrieval);

      _subMenu_Reimport.setEnabled(isTourSelected);

      _actionDeleteTour.setEnabled(isTourSelected);
      _actionDuplicateTour.setEnabled(isOneTour && !isDeviceTour);
      _actionEditQuick.setEnabled(isOneTour);
      _actionEditTour.setEnabled(isOneTour);
      _actionExportTour.setEnabled(isTourSelected);
      _actionExportViewCSV.setEnabled(numSelectedItems > 0);
      _actionJoinTours.setEnabled(numTourItems > 1);
      _actionMergeTour.setEnabled(canMergeTours);
      _actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);
      _actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
      _actionOpenTour.setEnabled(isOneTour);
      _actionPrintTour.setEnabled(isTourSelected);
      _actionSetOtherPerson.setEnabled(isTourSelected);
      _actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

      _actionCollapseOthers.setEnabled(numSelectedItems == 1 && firstElementHasChildren);
      _actionExpandSelection.setEnabled(
            firstElement == null
                  ? false
                  : numSelectedItems == 1
                        ? firstElementHasChildren
                        : true);

      _actionSelectAllTours.setEnabled(true);
      _actionToggleViewLayout.setEnabled(true);

      _tagMenuManager.enableTagActions(isTourSelected, isOneTour, firstTour == null ? null : firstTour.getTagIds());

      TourTypeMenuManager.enableRecentTourTypeActions(
            isTourSelected,
            isOneTour
                  ? firstTour.getTourTypeId()
                  : TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   private void fillActionBars() {

      /*
       * fill view menu
       */
      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

      menuMgr.add(_actionRefreshView);
      menuMgr.add(new Separator());
      menuMgr.add(_actionModifyColumns);

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionSelectAllTours);
      tbm.add(_actionToggleViewLayout);

      tbm.add(new Separator());
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll);
      tbm.add(_actionLinkWithOtherViews);
      tbm.add(_actionTourBookOptions);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionEditQuick);
      menuMgr.add(_actionEditTour);
      menuMgr.add(_actionOpenMarkerDialog);
      menuMgr.add(_actionOpenAdjustAltitudeDialog);
      menuMgr.add(_actionOpenTour);
      menuMgr.add(_actionDuplicateTour);
      menuMgr.add(_actionMergeTour);
      menuMgr.add(_actionJoinTours);

      _tagMenuManager.fillTagMenu(menuMgr, true);

      // tour type actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionSetTourType);
      TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

      menuMgr.add(new Separator());
      menuMgr.add(_actionCollapseOthers);
      menuMgr.add(_actionExpandSelection);
      menuMgr.add(_actionCollapseAll);

      menuMgr.add(new Separator());
      menuMgr.add(_actionExportTour);
      menuMgr.add(_actionExportViewCSV);
      menuMgr.add(_actionPrintTour);

      menuMgr.add(new Separator());
      menuMgr.add(_subMenu_AdjustTourValues);
      menuMgr.add(_subMenu_Reimport);
      menuMgr.add(_actionSetOtherPerson);
      menuMgr.add(_actionDeleteTour);

      enableActions();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager_Tree;
   }

   @Override
   public PostSelectionProvider getPostSelectionProvider() {
      return _postSelectionProvider;
   }

   @Override
   public Set<Long> getSelectedTourIDs() {

      final Set<Long> tourIds = new HashSet<>();

      IStructuredSelection selectedTours;
      if (_isLayoutTable) {
         selectedTours = _tourViewer_Table.getStructuredSelection();

      } else if (_isLayoutNatTable) {

         return tourIds;

      } else {
         selectedTours = _tourViewer_Tree.getStructuredSelection();
      }

      for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {

         final Object viewItem = tourIterator.next();

         if (viewItem instanceof TVITourBookYear) {

            // one year is selected

            if (_actionSelectAllTours.isChecked()) {

               // loop: all months
               for (final TreeViewerItem viewerItem : ((TVITourBookYear) viewItem).getFetchedChildren()) {
                  if (viewerItem instanceof TVITourBookYearCategorized) {
                     getYearSubTourIDs((TVITourBookYearCategorized) viewerItem, tourIds);
                  }
               }
            }

         } else if (viewItem instanceof TVITourBookYearCategorized) {

            // one month/week is selected

            if (_actionSelectAllTours.isChecked()) {
               getYearSubTourIDs((TVITourBookYearCategorized) viewItem, tourIds);
            }

         } else if (viewItem instanceof TVITourBookTour) {

            // one tour is selected

            tourIds.add(((TVITourBookTour) viewItem).getTourId());
         }
      }

      return tourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tour id's
      final Set<Long> tourIds = getSelectedTourIDs();

      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      BusyIndicator.showWhile(Display.getCurrent(), () -> {
         TourManager.loadTourData(new ArrayList<>(tourIds), selectedTourData, false);
      });

      return selectedTourData;
   }

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _tourViewer_Table.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   IDialogSettings getState() {
      return _state;
   }

   /**
    * @return the {@link #_tourViewer_NatTable}
    */
   public NatTable getTourViewer_NatTable() {
      return _tourViewer_NatTable;
   }

   /**
    * @return the _tourViewer_Table
    */
   public TableViewer getTourViewer_Table() {
      return _tourViewer_Table;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourViewer_Tree;
   }

   @Override
   public ColumnViewer getViewer() {

      if (_isLayoutTable) {

         return _tourViewer_Table;

      } else if (_isLayoutNatTable) {

         return null;

      } else {

         return _tourViewer_Tree;
      }
   }

   /**
    * @return Returns the layout of the view
    */
   public TourBookViewLayout getViewLayout() {
      return _viewLayout;
   }

   /**
    * @param yearSubItem
    * @param tourIds
    * @return Return all tours for one yearSubItem
    */
   private void getYearSubTourIDs(final TVITourBookYearCategorized yearSubItem, final Set<Long> tourIds) {

      // get all tours for the month item
      for (final TreeViewerItem viewerItem : yearSubItem.getFetchedChildren()) {
         if (viewerItem instanceof TVITourBookTour) {

            final TVITourBookTour tourItem = (TVITourBookTour) viewerItem;
            tourIds.add(tourItem.getTourId());
         }
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelect_SortColumn(e);
         }
      };
   }

   boolean isShowSummaryRow() {

      return Util.getStateBoolean(_state, TourBookView.STATE_IS_SHOW_SUMMARY_ROW, TourBookView.STATE_IS_SHOW_SUMMARY_ROW_DEFAULT);
   }

   /**
    * Register column labels for the body and header -> this is necessary to apply styling, images,
    * ...
    *
    * @param allSortedColumns
    * @param body_DataLayer
    * @param columnHeader_DataLayer
    */
   private void natTable_RegisterColumnLabels(final ArrayList<ColumnDefinition> allSortedColumns,
                                              final DataLayer body_DataLayer,
                                              final DataLayer columnHeader_DataLayer) {

      final ColumnOverrideLabelAccumulator body_ColumnLabelAccumulator = new ColumnOverrideLabelAccumulator(body_DataLayer);
      final ColumnOverrideLabelAccumulator columnHeader_ColumnLabelAccumulator = new ColumnOverrideLabelAccumulator(columnHeader_DataLayer);

      body_DataLayer.setConfigLabelAccumulator(body_ColumnLabelAccumulator);
      columnHeader_DataLayer.setConfigLabelAccumulator(columnHeader_ColumnLabelAccumulator);

      for (int colIndex = 0; colIndex < allSortedColumns.size(); colIndex++) {

         final ColumnDefinition colDef = allSortedColumns.get(colIndex);

         if (!colDef.isColumnDisplayed()) {
            // ignore hidden colums
            return;
         }

         final String columnId = colDef.getColumnId();

         columnHeader_ColumnLabelAccumulator.registerColumnOverrides(colIndex, columnId + HEADER_COLUMN_ID_POSTFIX);

         body_ColumnLabelAccumulator.registerColumnOverrides(colIndex, columnId);
      }
   }

   /**
    * @param allSortedColumns
    * @param body_DataLayer
    */
   private void natTable_SetColumnWidths(final ArrayList<ColumnDefinition> allSortedColumns, final DataLayer body_DataLayer) {

      // set column widths
      for (int colIndex = 0; colIndex < allSortedColumns.size(); colIndex++) {

         final ColumnDefinition colDef = allSortedColumns.get(colIndex);

         if (!colDef.isColumnDisplayed()) {
            // visible columns are displayed first
            continue;
//            break;
         }

         body_DataLayer.setColumnWidthByPosition(colIndex, colDef.getColumnWidth(), false);
      }
   }

   private void onSelect_CreateTourSelection(final HashSet<Long> tourIds) {

      ISelection selection;
      if (tourIds.size() == 0) {

         // fire selection that nothing is selected

         selection = new SelectionTourIds(new ArrayList<Long>());

      } else {

         // keep selected tour id's
         _selectedTourIds.clear();
         _selectedTourIds.addAll(tourIds);

         selection = tourIds.size() == 1 //
               ? new SelectionTourId(_selectedTourIds.get(0))
               : new SelectionTourIds(_selectedTourIds);

      }

      _isInFireSelection = true;
      {
         // _postSelectionProvider should be removed when all parts are listening to the TourManager event
         if (_isInStartup) {

            _isInStartup = false;

            // this view can be inactive -> selection is not fired with the SelectionProvider interface

            TourManager.fireEventWithCustomData(TourEventId.TOUR_SELECTION, selection, this);

         } else {

            _postSelectionProvider.setSelection(selection);
         }
      }
      _isInFireSelection = false;

      enableActions();
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer_Table.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = _tourViewer_Table.getSelection();

         // update viewer with new sorting
         _tourViewer_Table_Comparator.setSortColumn(e.widget);

         final String sortColumnId = _tourViewer_Table_Comparator.__sortColumnId;
         final int sortDirection = _tourViewer_Table_Comparator.__sortDirection;

         _tableTourProvider.setSortColumn(sortColumnId, sortDirection);
         _natTable_DataProvider.setSortColumn(sortColumnId, sortDirection);

         _tourViewer_Table.refresh();

         // reselect
         _isInReload = true;
         {
            _tourViewer_Table.setSelection(selectionBackup, true);
            _tourViewer_Table.getTable().showSelection();
         }
         _isInReload = false;
      }
      _viewerContainer_Table.setRedraw(true);
   }

   private void onSelect_TableItem(final SelectionChangedEvent event) {

      if (_isInReload) {
         return;
      }

      final HashSet<Long> tourIds = new HashSet<>();

      final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());

      // loop: all selected items
      for (final Iterator<?> itemIterator = selectedTours.iterator(); itemIterator.hasNext();) {

         final Object treeItem = itemIterator.next();

         if (treeItem instanceof TVITourBookTour) {

            final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

            tourIds.add(tourItem.getTourId());
         }
      }

      onSelect_CreateTourSelection(tourIds);
   }

   private void onSelect_TreeItem(final SelectionChangedEvent event) {

      if (_isInReload) {
         return;
      }

      final boolean isSelectAllChildren = _actionSelectAllTours.isChecked();

      final HashSet<Long> tourIds = new HashSet<>();

      boolean isFirstYear = true;
      boolean isFirstYearSub = true;
      boolean isFirstTour = true;

      final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
      // loop: all selected items
      for (final Iterator<?> itemIterator = selectedTours.iterator(); itemIterator.hasNext();) {

         final Object treeItem = itemIterator.next();

         if (isSelectAllChildren) {

            // get ALL tours from all selected tree items (year/month/tour)

            if (treeItem instanceof TVITourBookYear) {

               // year is selected

               final TVITourBookYear yearItem = ((TVITourBookYear) treeItem);
               if (isFirstYear) {
                  // keep selected year
                  isFirstYear = false;
                  _selectedYear = yearItem.tourYear;
               }

               // get all tours for the selected year
               for (final TreeViewerItem viewerItem : yearItem.getFetchedChildren()) {
                  if (viewerItem instanceof TVITourBookYearCategorized) {
                     getYearSubTourIDs((TVITourBookYearCategorized) viewerItem, tourIds);
                  }
               }

            } else if (treeItem instanceof TVITourBookYearCategorized) {

               // month/week/day is selected

               final TVITourBookYearCategorized yearSubItem = (TVITourBookYearCategorized) treeItem;
               if (isFirstYearSub) {
                  // keep selected year/month/week/day
                  isFirstYearSub = false;
                  _selectedYear = yearSubItem.tourYear;
                  _selectedYearSub = yearSubItem.tourYearSub;
               }

               // get all tours for the selected month
               getYearSubTourIDs(yearSubItem, tourIds);

            } else if (treeItem instanceof TVITourBookTour) {

               // tour is selected

               final TVITourBookTour tourItem = (TVITourBookTour) treeItem;
               if (isFirstTour) {
                  // keep selected tour
                  isFirstTour = false;
                  _selectedYear = tourItem.tourYear;
                  _selectedYearSub = tourItem.tourYearSub;
               }

               tourIds.add(tourItem.getTourId());
            }

         } else {

            // get only selected tours

            if (treeItem instanceof TVITourBookTour) {

               final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

               if (isFirstTour) {
                  // keep selected tour
                  isFirstTour = false;
                  _selectedYear = tourItem.tourYear;
                  _selectedYearSub = tourItem.tourYearSub;
               }

               tourIds.add(tourItem.getTourId());
            }
         }
      }

      onSelect_CreateTourSelection(tourIds);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isInFireSelection) {
         return;
      }

      // show and select the selected tour
      if (selection instanceof SelectionTourId) {

         final long newTourId = ((SelectionTourId) selection).getTourId();

         selectTour(newTourId);

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof GeoPartComparerItem) {

            // show selected compared tour

            final GeoPartComparerItem comparerItem = (GeoPartComparerItem) firstElement;

            selectTour(comparerItem.tourId);
         }

      } else if (selection instanceof SelectionDeletedTours) {

         reloadViewer();
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      if (_isLayoutTable) {

         _viewerContainer_Table.setRedraw(false);
         {
            final ISelection selection = _tourViewer_Table.getSelection();

            final Table table_Old = _tourViewer_Table.getTable();
            table_Old.dispose();

            createUI_30_TourViewer_Table(_viewerContainer_Table);
            _viewerContainer_Table.layout();

            setupTourViewerContent();

            _tourViewer_Table.setSelection(selection);

            // ensure that the selected item also has the focus, these are 2 different things !
            final Table table_New = _tourViewer_Table.getTable();
            table_New.setSelection(table_New.getSelectionIndex());
            table_New.setFocus();
         }
         _viewerContainer_Table.setRedraw(true);

         return _tourViewer_Table;

      } else if (_isLayoutNatTable) {

         setupTourViewerContent();

         return null;

      } else {

         _viewerContainer_Tree.setRedraw(false);
         {
            final Object[] expandedElements = _tourViewer_Tree.getExpandedElements();
            final ISelection selection = _tourViewer_Tree.getSelection();

            _tourViewer_Tree.getTree().dispose();

            createUI_20_TourViewer_Tree(_viewerContainer_Tree);
            _viewerContainer_Tree.layout();

            setupTourViewerContent();

            _tourViewer_Tree.setExpandedElements(expandedElements);
            _tourViewer_Tree.setSelection(selection);
         }
         _viewerContainer_Tree.setRedraw(true);

         return _tourViewer_Tree;
      }
   }

   @Override
   public void reloadViewer() {

      if (_isInReload) {
         return;
      }

      _tableTourProvider.resetTourItems();
      _natTable_DataProvider.resetTourItems();

      if (_isLayoutTable) {

         final Table table = _tourViewer_Table.getTable();
         table.setRedraw(false);
         _isInReload = true;
         {
            final ISelection selection = _tourViewer_Table.getSelection();

            setupTourViewerContent();

            _tourViewer_Table.setSelection(selection, true);
         }
         _isInReload = false;
         table.setRedraw(true);

      } else if (_isLayoutNatTable) {

         _natTable_DataProvider.cleanup_Tours();

         setupTourViewerContent();

      } else {

         final Tree tree = _tourViewer_Tree.getTree();
         tree.setRedraw(false);
         _isInReload = true;
         {
            final Object[] expandedElements = _tourViewer_Tree.getExpandedElements();
            final ISelection selection = _tourViewer_Tree.getSelection();

            setupTourViewerContent();

            _tourViewer_Tree.setExpandedElements(expandedElements);
            _tourViewer_Tree.setSelection(selection, true);
         }
         _isInReload = false;
         tree.setRedraw(true);
      }
   }

   void reopenFirstSelectedTour() {

      if (_isLayoutTable) {

         setupTourViewerContent();

      } else if (_isLayoutNatTable) {

         setupTourViewerContent();

      } else {

         _selectedYear = -1;
         _selectedYearSub = -1;
         TVITourBookTour selectedTourItem = null;

         final ISelection oldSelection = _tourViewer_Tree.getSelection();
         if (oldSelection != null) {

            final Object selection = ((IStructuredSelection) oldSelection).getFirstElement();
            if (selection instanceof TVITourBookTour) {

               selectedTourItem = (TVITourBookTour) selection;

               _selectedYear = selectedTourItem.tourYear;

               if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {
                  _selectedYearSub = selectedTourItem.colWeekNo;
               } else {
                  _selectedYearSub = selectedTourItem.tourMonth;
               }
            }
         }

         reloadViewer();
         reselectTourViewer();

         final IStructuredSelection newSelection = (IStructuredSelection) _tourViewer_Tree.getSelection();
         if (newSelection != null) {

            final Object selection = newSelection.getFirstElement();
            if (selection instanceof TVITourBookTour) {

               selectedTourItem = (TVITourBookTour) selection;

               _tourViewer_Tree.collapseAll();
               _tourViewer_Tree.expandToLevel(selectedTourItem, 0);
               _tourViewer_Tree.setSelection(new StructuredSelection(selectedTourItem), false);
            }
         }
      }
   }

   private void reselectTourViewer() {

      if (_isLayoutTable) {

         final int[] stateSelectedTourIndices = Util.getStateIntArray(_state, STATE_SELECTED_TABLE_INDEX, null);

         final Table table = _tourViewer_Table.getTable();

         if (stateSelectedTourIndices != null) {

            table.setSelection(stateSelectedTourIndices);
            table.setFocus();
         }

         // move the horizontal scrollbar to the left border
         final ScrollBar horizontalBar = table.getHorizontalBar();
         if (horizontalBar != null) {
            horizontalBar.setSelection(0);
         }

      } else if (_isLayoutNatTable) {

         // select first row -> needs to be improved
         _tourViewer_NatTable.doCommand(new SelectRowsCommand(_natTable_Grid_BodyLayer, 0, 0, false, false));

      } else {

         // find the old selected year/[month/week] in the new tour items
         TreeViewerItem reselectYearItem = null;
         TreeViewerItem reselectYearSubItem = null;
         final ArrayList<TreeViewerItem> reselectTourItems = new ArrayList<>();

         /*
          * get the year/month/tour item in the data model
          */
         final ArrayList<TreeViewerItem> rootItems = _rootItem_Tree.getChildren();

         for (final TreeViewerItem rootItem : rootItems) {

            if (rootItem instanceof TVITourBookYear) {

               final TVITourBookYear tourBookYear = ((TVITourBookYear) rootItem);
               if (tourBookYear.tourYear == _selectedYear) {

                  reselectYearItem = rootItem;

                  final Object[] yearSubItems = tourBookYear.getFetchedChildrenAsArray();
                  for (final Object yearSub : yearSubItems) {

                     final TVITourBookYearCategorized tourBookYearSub = ((TVITourBookYearCategorized) yearSub);
                     if (tourBookYearSub.tourYearSub == _selectedYearSub) {

                        reselectYearSubItem = tourBookYearSub;

                        final Object[] tourItems = tourBookYearSub.getFetchedChildrenAsArray();
                        for (final Object tourItem : tourItems) {

                           final TVITourBookTour tourBookTour = ((TVITourBookTour) tourItem);
                           final long treeTourId = tourBookTour.tourId;

                           for (final Long tourId : _selectedTourIds) {
                              if (treeTourId == tourId) {
                                 reselectTourItems.add(tourBookTour);
                                 break;
                              }
                           }
                        }
                        break;
                     }
                  }
                  break;
               }
            }
         }

         // select year/month/tour in the viewer
         if (reselectTourItems.size() > 0) {

            _tourViewer_Tree.setSelection(new StructuredSelection(reselectTourItems) {}, false);

         } else if (reselectYearSubItem != null) {

            _tourViewer_Tree.setSelection(new StructuredSelection(reselectYearSubItem) {}, false);

         } else if (reselectYearItem != null) {

            _tourViewer_Tree.setSelection(new StructuredSelection(reselectYearItem) {}, false);

         } else if (rootItems.size() > 0)

         {

            // the old year was not found, select the newest year

//         final TreeViewerItem yearItem = rootItems.get(rootItems.size() - 1);

//         _tourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
         }

         // move the horizontal scrollbar to the left border
         final ScrollBar horizontalBar = _tourViewer_Tree.getTree().getHorizontalBar();
         if (horizontalBar != null) {
            horizontalBar.setSelection(0);
         }
      }
   }

   private void restoreState() {

      // set tour viewer reselection data
      try {
         _selectedYear = _state.getInt(STATE_SELECTED_YEAR);
      } catch (final NumberFormatException e) {
         _selectedYear = -1;
      }

      try {
         _selectedYearSub = _state.getInt(STATE_SELECTED_MONTH);
      } catch (final NumberFormatException e) {
         _selectedYearSub = -1;
      }

      final String[] selectedTourIds = _state.getArray(STATE_SELECTED_TOURS);
      _selectedTourIds.clear();

      if (selectedTourIds != null) {
         for (final String tourId : selectedTourIds) {
            try {
               _selectedTourIds.add(Long.valueOf(tourId));
            } catch (final NumberFormatException e) {
               // ignore
            }
         }
      }

      _actionSelectAllTours.setChecked(_state.getBoolean(STATE_IS_SELECT_YEAR_MONTH_TOURS));
      _isCollapseOthers = Util.getStateBoolean(_state,
            STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS,
            STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS_DEFAULT);

      /*
       * View layout
       */
      _viewLayout = (TourBookViewLayout) Util.getStateEnum(_state, STATE_VIEW_LAYOUT, TourBookViewLayout.CATEGORY_MONTH);

      String viewLayoutImage = null;

      if (_viewLayout == TourBookViewLayout.CATEGORY_MONTH) {

         viewLayoutImage = Messages.Image__TourBook_Month;

         _isLayoutTable = false;
         _isLayoutNatTable = false;

      } else if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         viewLayoutImage = Messages.Image__TourBook_Week;

         _isLayoutTable = false;
         _isLayoutNatTable = false;

      } else if (_viewLayout == TourBookViewLayout.TABLE) {

         viewLayoutImage = Messages.Image__TourBook_Table;

         _isLayoutTable = true;
         _isLayoutNatTable = false;

      } else if (_viewLayout == TourBookViewLayout.NAT_TABLE) {

         viewLayoutImage = Messages.Image__TourBook_NatTable;

         _isLayoutTable = false;
         _isLayoutNatTable = true;
      }

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(viewLayoutImage));

      /*
       * View options
       */
      _isShowSummaryRow = isShowSummaryRow();

      updateToolTipState();
   }

   private void restoreState_AfterUI() {

      /*
       * This must be selected lately otherwise the selection state is set but is not visible
       * (button is not pressed). Could not figure out why this occures after debugging this issue
       */
      _actionLinkWithOtherViews.setSelection(_state.getBoolean(STATE_IS_LINK_WITH_OTHER_VIEWS));
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, TableColumnFactory.TIME_DATE_ID);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, ItemComparator_Table.DESCENDING);

      // update comparator
      _tourViewer_Table_Comparator.__sortColumnId = sortColumnId;
      _tourViewer_Table_Comparator.__sortDirection = sortDirection;

      // setup tour provider with the correct sorting
      _tableTourProvider.setSortColumn(sortColumnId, sortDirection);
   }

   @PersistState
   private void saveState() {

      // save selection in the tour viewer
      _state.put(STATE_SELECTED_YEAR, _selectedYear);
      _state.put(STATE_SELECTED_MONTH, _selectedYearSub);

      // convert tour id's into string
      final ArrayList<String> selectedTourIds = new ArrayList<>();
      for (final Long tourId : _selectedTourIds) {
         selectedTourIds.add(tourId.toString());
      }
      _state.put(STATE_SELECTED_TOURS, selectedTourIds.toArray(new String[selectedTourIds.size()]));

      // action: select tours for year/yearSub
      _state.put(STATE_IS_SELECT_YEAR_MONTH_TOURS, _actionSelectAllTours.isChecked());

      _state.put(STATE_IS_LINK_WITH_OTHER_VIEWS, _actionLinkWithOtherViews.getSelection());
      _state.put(STATE_VIEW_LAYOUT, _viewLayout.name());

      Util.setState(_state, STATE_SELECTED_TABLE_INDEX, _tourViewer_Table.getTable().getSelectionIndices());

      // viewer columns
      _state.put(STATE_SORT_COLUMN_ID, _tourViewer_Table_Comparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _tourViewer_Table_Comparator.__sortDirection);

      _columnManager_Table.saveState(_state_Table);
      _columnManager_Tree.saveState(_state_Tree);

      _columnManager_Table.saveState(_state_Table, _natTable_DataLayer_Body, _natTable_ColumnReorder_Layer);

   }

   private void selectTour(final long tourId) {

      if (_isLayoutTable || _isLayoutNatTable) {

         // for performance reasons a tour cannot be selected by it's ID only by table index
         // todo: get table index from db

         return;
      }

      // check if enabled
      if (_actionLinkWithOtherViews.getSelection() == false) {

         // linking is disabled

         return;
      }

      // check with old id
      final long oldTourId = _selectedTourIds != null && _selectedTourIds.size() == 1
            ? _selectedTourIds.get(0)
            : -1;

      if (tourId == oldTourId) {

         // tour id is the same

         return;
      }

      // link with other views

      final TourData tourData = TourManager.getTour(tourId);

      if (tourData == null) {
         return;
      }

      _selectedTourIds.clear();
      _selectedTourIds.add(tourId);

      if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         _selectedYear = tourData.getStartWeekYear();
         _selectedYearSub = tourData.getStartWeek();

      } else {

         final ZonedDateTime tourStartTime = tourData.getTourStartTime();

         _selectedYear = tourStartTime.getYear();
         _selectedYearSub = tourStartTime.getMonthValue();
      }

      // run async otherwise an internal NPE occures
      _parent.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            final Tree tree = _tourViewer_Tree.getTree();
            tree.setRedraw(false);
            _isInReload = true;
            {
               if (_isCollapseOthers) {

                  try {

                     _tourViewer_Tree.collapseAll();

                  } catch (final Exception e) {

                     /**
                      * <code>

                        Caused by: java.lang.NullPointerException
                        at org.eclipse.jface.viewers.AbstractTreeViewer.getSelection(AbstractTreeViewer.java:2956)
                        at org.eclipse.jface.viewers.StructuredViewer.handleSelect(StructuredViewer.java:1211)
                        at org.eclipse.jface.viewers.StructuredViewer$4.widgetSelected(StructuredViewer.java:1241)
                        at org.eclipse.jface.util.OpenStrategy.fireSelectionEvent(OpenStrategy.java:239)
                        at org.eclipse.jface.util.OpenStrategy.access$4(OpenStrategy.java:233)
                        at org.eclipse.jface.util.OpenStrategy$1.handleEvent(OpenStrategy.java:403)
                        at org.eclipse.swt.widgets.EventTable.sendEvent(EventTable.java:84)
                        at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1053)
                        at org.eclipse.swt.widgets.Widget.sendEvent(Widget.java:1077)
                        at org.eclipse.swt.widgets.Widget.sendSelectionEvent(Widget.java:1094)
                        at org.eclipse.swt.widgets.TreeItem.setExpanded(TreeItem.java:1385)
                        at org.eclipse.jface.viewers.TreeViewer.setExpanded(TreeViewer.java:332)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.internalCollapseToLevel(AbstractTreeViewer.java:1571)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.internalCollapseToLevel(AbstractTreeViewer.java:1586)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.collapseToLevel(AbstractTreeViewer.java:751)
                        at org.eclipse.jface.viewers.AbstractTreeViewer.collapseAll(AbstractTreeViewer.java:733)

                        at net.tourbook.ui.views.tourBook.TourBookView$70.run(TourBookView.java:3406)

                        at org.eclipse.swt.widgets.RunnableLock.run(RunnableLock.java:35)
                        at org.eclipse.swt.widgets.Synchronizer.runAsyncMessages(Synchronizer.java:135)
                        ... 22 more

                      * </code>
                      */

                     // this occures sometimes but it seems that it's an eclipse internal problem
                     StatusUtil.log("This is a known issue when a treeviewer do a collapseAll()", e); //$NON-NLS-1$
                  }
               }

               reselectTourViewer();
            }
            _isInReload = false;
            tree.setRedraw(true);
         }
      });
   }

   public void setActiveYear(final int activeYear) {
      _selectedYear = activeYear;
   }

   private void setCellColor(final ViewerCell cell, final Object element) {

      boolean isShowSummaryRow = false;

      if (element instanceof TVITourBookYear && _isShowSummaryRow) {
         isShowSummaryRow = ((TVITourBookYear) element).isRowSummary;
      }

      if (isShowSummaryRow) {

         // show no other color

      } else {

         if (element instanceof TVITourBookYear) {
            cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB));
         } else if (element instanceof TVITourBookYearCategorized) {
            cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB_SUB));
//         } else if (element instanceof TVITourBookTour) {
//            cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
         }
      }
   }

   @Override
   public void setFocus() {

      if (_isLayoutTable) {

         final Table table = _tourViewer_Table.getTable();

         if (table.isDisposed()) {
            return;
         }

         table.setFocus();

      } else if (_isLayoutNatTable) {

// this do not work, the workaround is to select a row:
//
//         _tourViewer_NatTable.doCommand(new SelectRowsCommand(_natTable_Grid_BodyLayer, 0, 80, false, false));
//
//         _tourViewer_NatTable.getDisplay().asyncExec(() -> {
//
//            if (!_tourViewer_NatTable.isDisposed()) {
//               _tourViewer_NatTable.setFocus();
//            }
//         });

         _tourViewer_NatTable.setFocus();

      } else {

         final Tree tree = _tourViewer_Tree.getTree();

         if (tree.isDisposed()) {
            return;
         }

         tree.setFocus();
      }
   }

   void setLinkAndCollapse(final boolean isCollapseOthers) {

      _isCollapseOthers = isCollapseOthers;
   }

   private void setupTourViewerContent() {

      if (_isLayoutTable) {

         /*
          * There have been different exceptions depending on the sequence of the viewer methods
          */

         _tourViewer_Table.getTable().setRedraw(false);
         {
            /*
             * Set number of items into the ILazyContentProvider, this MUST be set before the
             * content provider otherwise when tour filter returns less items there will be an out
             * of bounds exception !
             */
            _tourViewer_Table.setItemCount(_tableTourProvider.getNumberOfItems());

            _tourViewer_Table.setContentProvider(new ContentProvider_Table());

            _tourViewer_Table.setInput(new Object());

            _pageBook.showPage(_viewerContainer_Table);
         }
         _tourViewer_Table.getTable().setRedraw(true);

      } else if (_isLayoutNatTable) {

         _pageBook.showPage(_viewerContainer_NatTable);

      } else {

         if (_rootItem_Tree != null) {
            _rootItem_Tree.clearChildren();
         }

         _rootItem_Tree = new TVITourBookRoot(this, _viewLayout);

         _tourViewer_Tree.getTree().setRedraw(false);
         {
            _tourViewer_Tree.setContentProvider(new ContentProvider_Tree());
            _tourViewer_Tree.setInput(_rootItem_Tree);

            _pageBook.showPage(_viewerContainer_Tree);
         }
         _tourViewer_Tree.getTree().setRedraw(true);
      }

   }

   private void toggleLayout_Category_Month() {

      _viewLayout = TourBookViewLayout.CATEGORY_MONTH;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Month));

      _isLayoutTable = false;
      _isLayoutNatTable = false;
   }

   private void toggleLayout_Category_Week() {

      _viewLayout = TourBookViewLayout.CATEGORY_WEEK;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Week));

      _isLayoutTable = false;
      _isLayoutNatTable = false;
   }

   private void toggleLayout_NatTable() {

      _viewLayout = TourBookViewLayout.NAT_TABLE;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_NatTable));

      _isLayoutNatTable = true;
      _isLayoutTable = false;
   }

   private void toggleLayout_Table() {

      _viewLayout = TourBookViewLayout.TABLE;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Table));

      _isLayoutTable = true;
      _isLayoutNatTable = false;
   }

   @Override
   public void toursAreModified(final ArrayList<TourData> modifiedTours) {

      // do a reselection of the selected tours to fire the multi tour data selection

      actionSelectYearMonthTours();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   private void updateToolTipState() {

      _isShowToolTipIn_Date = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE);
      _isShowToolTipIn_Time = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME);
      _isShowToolTipIn_WeekDay = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY);
      _isShowToolTipIn_Title = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE);
      _isShowToolTipIn_Tags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS);
   }

   void updateTourBookOptions() {

      _isShowSummaryRow = isShowSummaryRow();

      reloadViewer();
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_ShowSortDirection(final String sortColumnId, final int sortDirection) {

      final Table table = _tourViewer_Table.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == ItemComparator_Table.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_TourViewerColumns_Table() {

      // set tooltip text

      final String timeZone = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
      final String timeZoneTooltip = NLS.bind(COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP, timeZone);

      _colDef_TimeZoneOffset_Table.setColumnHeaderToolTipText(timeZoneTooltip);
   }

   private void updateUI_TourViewerColumns_Tree() {

      // set tooltip text

      final String timeZone = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
      final String timeZoneTooltip = NLS.bind(COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP, timeZone);

      _colDef_TimeZoneOffset_Tree.setColumnHeaderToolTipText(timeZoneTooltip);
   }

}
