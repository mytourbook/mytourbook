/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.INatTable_PropertiesProvider;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.NatTable_LabelProvider;
import net.tourbook.common.util.NatTable_LabelProvider_WithLocationTooltip;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.ViewContext;
import net.tourbook.search.SearchView;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.DialogCreateTourMarkers;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationToolTip;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.INatTable_TourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionDuplicateTour;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetStartEndLocation;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.NatTableViewer_TourInfo_ToolTip;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.ViewNames;
import net.tourbook.ui.views.geoCompare.GeoComparedTour;
import net.tourbook.ui.views.rawData.ActionDeleteTourValues;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.ActionReimportTours;
import net.tourbook.ui.views.rawData.SubMenu_AdjustTourValues;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.tourBook.natTable.DataProvider_ColumnHeader;
import net.tourbook.ui.views.tourBook.natTable.NatTable_DataLoader;
import net.tourbook.ui.views.tourBook.natTable.NatTable_DummyColumnViewer;
import net.tourbook.ui.views.tourBook.natTable.NatTable_Header_Tooltip;
import net.tourbook.ui.views.tourBook.natTable.NatTable_SortModel;
import net.tourbook.ui.views.tourBook.natTable.SingleClickSortConfiguration_MT;
import net.tourbook.ui.views.tourBook.natTable.TourRowDataProvider;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.freeze.CompositeFreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.hover.HoverLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.painter.cell.BackgroundPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.PaddingDecorator;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.reorder.event.ColumnReorderEvent;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectRowsCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultRowSelectionLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.event.SortColumnEvent;
import org.eclipse.nebula.widgets.nattable.sort.painter.SortIconPainter;
import org.eclipse.nebula.widgets.nattable.sort.painter.SortableHeaderTextPainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.theme.DarkNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.style.theme.ModernNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.style.theme.ThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.ui.action.IKeyAction;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.KeyEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements

      ITourProvider2,
      ITourViewer3,
      ITourProviderByID,
      ITreeViewer,
      INatTable_PropertiesProvider,
      INatTable_TourProvider {

   public static final String ID = "net.tourbook.views.tourListView"; //$NON-NLS-1$

   //
   private static final IPreferenceStore _prefStore                                      = TourbookPlugin.getPrefStore();

   private static final IPreferenceStore _prefStore_Common                               = CommonActivator.getPrefStore();
   //
   private static final IDialogSettings  _state                                          = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _state_NatTable                                 = TourbookPlugin.getState(ID + "_NAT_TABLE"); //$NON-NLS-1$
   private static final IDialogSettings  _state_Tree                                     = TourbookPlugin.getState(ID + "_TREE");      //$NON-NLS-1$
   //
   private static final String           STATE_CSV_EXPORT_PATH                           = "STATE_CSV_EXPORT_PATH";                    //$NON-NLS-1$
   //
   private static final String           STATE_IS_LINK_WITH_OTHER_VIEWS                  = "STATE_IS_LINK_WITH_OTHER_VIEWS";           //$NON-NLS-1$
   private static final String           STATE_IS_SELECT_YEAR_MONTH_TOURS                = "STATE_IS_SELECT_YEAR_MONTH_TOURS";         //$NON-NLS-1$
   private static final String           STATE_IS_SELECTED_TOUR_COLLECTION_FILTER        = "STATE_IS_SELECTED_TOUR_COLLECTION_FILTER"; //$NON-NLS-1$
   static final String                   STATE_IS_SHOW_SUMMARY_ROW                       = "STATE_IS_SHOW_SUMMARY_ROW";                //$NON-NLS-1$
   private static final String           STATE_IS_ON_SELECT_EXPAND_COLLAPSE              = "STATE_IS_ON_SELECT_EXPAND_COLLAPSE";       //$NON-NLS-1$
   private static final String           STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS          = "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS";   //$NON-NLS-1$
   static final String                   STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS         = "STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS";  //$NON-NLS-1$
   private static final String           STATE_SELECTED_MONTH                            = "STATE_SELECTED_MONTH";                     //$NON-NLS-1$
   private static final String           STATE_SELECTED_TOURS                            = "STATE_SELECTED_TOURS";                     //$NON-NLS-1$
   private static final String           STATE_SELECTED_YEAR                             = "STATE_SELECTED_YEAR";                      //$NON-NLS-1$
   private static final String           STATE_TOUR_COLLECTION_FILTER                    = "STATE_TOUR_COLLECTION_FILTER";             //$NON-NLS-1$
   private static final String           STATE_VIEW_LAYOUT                               = "STATE_VIEW_LAYOUT";                        //$NON-NLS-1$
   //
   private static final String           STATE_SORT_COLUMN_DIRECTION                     = "STATE_SORT_COLUMN_DIRECTION";              //$NON-NLS-1$
   private static final String           STATE_SORT_COLUMN_ID                            = "STATE_SORT_COLUMN_ID";                     //$NON-NLS-1$
   //
   static final boolean                  STATE_IS_SHOW_SUMMARY_ROW_DEFAULT               = true;
   static final boolean                  STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS_DEFAULT = true;
   //
   //
   private static final String CSV_EXPORT_DEFAULT_FILE_NAME           = "TourBook_";                                                       //$NON-NLS-1$
   private static final String SYS_PROP__USE_SIMPLE_CSV_EXPORT_FORMAT = "useSimpleCSVExportFormat";                                        //$NON-NLS-1$
   private static boolean      USE_SIMPLE_CSV_EXPORT_FORMAT           = System.getProperty(SYS_PROP__USE_SIMPLE_CSV_EXPORT_FORMAT) != null;
   static {

      if (USE_SIMPLE_CSV_EXPORT_FORMAT) {

         Util.logSystemProperty_IsEnabled(TourManager.class,
               SYS_PROP__USE_SIMPLE_CSV_EXPORT_FORMAT,
               "Using simple format when exporting tours in CSV format"); //$NON-NLS-1$
      }
   }
   //
   /**
    * The header column id needs a different id than the body column otherwise drag&drop or column
    * selection shows the 1st row image :-(
    */
   private static final String HEADER_COLUMN_ID_POSTFIX = "_HEADER"; //$NON-NLS-1$

   //
   private static TourBookViewLayout _viewLayout;

   //
   private TourBook_ColumnFactory             _columnFactory;
   private ColumnManager                      _columnManager_NatTable;
   private ColumnManager                      _columnManager_Tree;
   //
   private OpenDialogManager                  _openDlgMgr                         = new OpenDialogManager();
   //
   private PostSelectionProvider              _postSelectionProvider;
   //
   private IPartListener2                     _partListener;
   private ISelectionListener                 _postSelectionListener;
   private IPropertyChangeListener            _prefChangeListener;
   private IPropertyChangeListener            _prefChangeListener_Common;
   private ITourEventListener                 _tourPropertyListener;
   //
   private TreeViewer                         _tourViewer_Tree;
   private TreeColumnDefinition               _colDef_TourTypeImage_Tree;
   private TreeColumnDefinition               _colDef_WeatherClouds_Tree;
   //
   /**
    * Index of the column with the image, index can be changed when the columns are reordered with
    * the mouse or the column manager
    */
   private int                                _columnIndex_TourTypeImage          = -1;
   private int                                _columnIndex_WeatherClouds          = -1;
   private int                                _columnWidth_TourTypeImage;
   private int                                _columnWidth_WeatherClouds;
   //
   private NatTable                           _tourViewer_NatTable;
   private NatTable_DummyColumnViewer         _natTable_DummyColumnViewer;
   //
   private TVITourBookRoot                    _rootItem_Tree;
   //
   private DataLayer                          _natTable_ColumnHeader_DataLayer;
   private ColumnHeaderLayer                  _natTable_ColumnHeader_Layer;
   private ColumnHideShowLayer                _natTable_Body_ColumnHideShowLayer;
   private ColumnReorderLayer                 _natTable_Body_ColumnReorderLayer;
   private DataLayer                          _natTable_Body_DataLayer;
   private HoverLayer                         _natTable_Body_HoverLayer;
   private SelectionLayer                     _natTable_Body_SelectionLayer;
   private ViewportLayer                      _natTable_Body_ViewportLayer;
   //
   private NatTable_DataLoader                _natTable_DataLoader;
   private TourRowDataProvider                _natTable_DataProvider;
   private NatTable_SortModel                 _natTable_SortModel;
   private NatTableContentTooltip             _natTable_Tooltip;
   //
   /**
    * Contains {@link SWT#MENU_MOUSE} or {@link SWT#MENU_KEYBOARD} when context menu is being opened
    */
   private int                                _natTable_ContextMenuActivator;
   //
   private int                                _selectedYear                       = -1;
   private int                                _selectedYearSub                    = -1;
   private final ArrayList<Long>              _selectedTourIds                    = new ArrayList<>();
   //
   private TourCollectionFilter               _tourCollectionFilter               = TourCollectionFilter.COLLECTED_TOURS;
   //
   private boolean                            _isCollapseOthers;
   private boolean                            _isInFireSelection;
   private boolean                            _isInSelection;
   private boolean                            _isInStartup;
   private boolean                            _isLayoutNatTable;
   // auto expand/collapse
   private boolean                            _isBehaviour_OnSelect_ExpandCollapse;                                      // is disabled by default
   private boolean                            _isBehaviour_SingleExpand_CollapseOthers;                                  // is disabled by default
   private boolean                            _isInCollapseAll;
   private boolean                            _isInExpandingSelection;
   private boolean                            _isSelectedWithKeyboard;
   private int                                _expandRunnableCounter;
   private long                               _lastExpandSelectionTime;
   //
   private final TourDoubleClickState         _tourDoubleClickState               = new TourDoubleClickState();
   //
   private NatTableViewer_TourInfo_ToolTip    _tourInfoToolTip_NatTable;
   private TreeViewerTourInfoToolTip          _tourInfoToolTip_Tree;
   private TourLocationToolTip                _tourLocationTooltip_NatTable;
   private TourLocationToolTip                _tourLocationTooltip_Tree;
   //
   private TagMenuManager                     _tagMenuManager;
   private TourTypeMenuManager                _tourTypeMenuManager;
   private MenuManager                        _viewerMenuManager_NatTable;
   private MenuManager                        _viewerMenuManager_Tree;
   private IContextMenuProvider               _viewerContextMenuProvider_NatTable = new ContextMenuProvider_NatTable();
   private IContextMenuProvider               _viewerContextMenuProvider_Tree     = new ContextMenuProvider_Tree();
   //
   private HashMap<String, Object>            _allTourActions_Adjust;
   private HashMap<String, Object>            _allTourActions_Edit;
   private HashMap<String, Object>            _allTourActions_Export;
   //
   private ActionCollapseAll                  _actionCollapseAll;
   private ActionCollapseOthers               _actionCollapseOthers;
   private ActionCreateTourMarkers            _actionCreateTourMarkers;
   private ActionDuplicateTour                _actionDuplicateTour;
   private ActionEditQuick                    _actionEditQuick;
   private ActionExpandSelection              _actionExpandSelection;
   private ActionExport                       _actionExportTour;
   private ActionExportViewCSV                _actionExportViewCSV;
   private ActionDeleteTour                   _actionDeleteTour;
   private ActionDeleteTourMenu               _actionDeleteTourMenu;
   private ActionDeleteTourValues             _actionDeleteTourValues;
   private ActionEditTour                     _actionEditTour;
   private ActionGotoLastTour                 _actionGotoToday;
   private ActionJoinTours                    _actionJoinTours;
   private ActionLinkWithOtherViews           _actionLinkWithOtherViews;
   private ActionMergeTour                    _actionMergeTour;
   private ActionOpenTour                     _actionOpenTour;
   private ActionOpenMarkerDialog             _actionOpenMarkerDialog;
   private ActionOpenAdjustAltitudeDialog     _actionOpenAdjustAltitudeDialog;
   private ActionPrint                        _actionPrintTour;
   private ActionRefreshView                  _actionRefreshView;
   private ActionReimportTours                _actionReimport_Tours;
   private ActionSelectAllTours               _actionSelectAllTours;
   private ActionSetPerson                    _actionSetOtherPerson;
   private ActionSetStartEndLocation          _actionSetStartEndLocation;
   private ActionToggleViewLayout             _actionToggleViewLayout;
   private ActionTourBookOptions              _actionTourBookOptions;
   private ActionTourCollectionFilter         _actionTourCollectionFilter;
   private ActionUpload                       _actionUploadTour;
   //
   private SubMenu_AdjustTourValues           _actionAdjustTourValues;
   //
   private ActionOnMouseSelect_ExpandCollapse _actionContext_OnMouseSelect_ExpandCollapse;
   private ActionSingleExpand_CollapseOthers  _actionContext_SingleExpand_CollapseOthers;
   //
   private PixelConverter                     _pc;
   //
   /*
    * UI controls
    */
   private PageBook  _pageBook;
   //
   private Composite _parent;
   private Composite _viewerContainer_NatTable;
   private Composite _viewerContainer_Tree;
   //
   private Menu      _contextMenu_NatTable;
   private Menu      _contextMenu_Tree;

   private class ActionCollapseAll_WithoutSelection extends ActionCollapseAll {

      public ActionCollapseAll_WithoutSelection(final TourBookView tourBookView) {

         super(tourBookView);
      }

      @Override
      public void run() {

         _isInCollapseAll = true;
         {
            super.run();
         }
         _isInCollapseAll = false;
      }
   }

   private class ActionCreateTourMarkers extends Action {

      public ActionCreateTourMarkers() {

         super(Messages.Tour_Action_CreateTourMarkers, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_New));
      }

      @Override
      public void run() {

         if (TourManager.isTourEditorModified()) {
            return;
         }

         new DialogCreateTourMarkers(_parent.getShell(), getSelectedTourIDs()).open();
      }
   }

   private class ActionGotoLastTour extends Action {

      public ActionGotoLastTour() {

         super(Messages.Tour_Book_Action_GoToLastTour_Tooltip2, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Today));
      }

      @Override
      public void run() {
         actionGoToLastTour();
      }
   }

   private class ActionLinkWithOtherViews extends ActionToolbarSlideout {

      public ActionLinkWithOtherViews() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.SyncViews), null);

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
   }

   private class ActionOnMouseSelect_ExpandCollapse extends Action {

      public ActionOnMouseSelect_ExpandCollapse() {

         super(Messages.Tour_Tags_Action_OnMouseSelect_ExpandCollapse, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         _isBehaviour_OnSelect_ExpandCollapse = _actionContext_OnMouseSelect_ExpandCollapse.isChecked();
      }
   }

   private class ActionSingleExpand_CollapseOthers extends Action {

      public ActionSingleExpand_CollapseOthers() {

         super(Messages.Tour_Tags_Action_SingleExpand_CollapseOthers, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         _isBehaviour_SingleExpand_CollapseOthers = _actionContext_SingleExpand_CollapseOthers.isChecked();
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

   class ActionTourCollectionFilter extends ActionToolbarSlideout {

      private SlideoutTourCollectionFilter slideoutTourSelectionFilter;

      public ActionTourCollectionFilter() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Collected_All));

         isToggleAction = true;
         isShowSlideoutAlways = true;

         /*
          * Register other action images
          */

         // image 0: all tours
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Collected_All));

         // image 1: selected tours
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Collected_Selected));

         // image 2: not selected tours
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Collected_Not));

//         TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Selected_Not_Disabled);
//         TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Selected_Selected_Disabled);
//         TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Selected_All_Disabled));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         slideoutTourSelectionFilter = new SlideoutTourCollectionFilter(_parent, toolbar, TourBookView.this);

         return slideoutTourSelectionFilter;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         updateTourSelectionFilter(TourCollectionFilter.ALL_TOURS, false);
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

   private class ContextMenuProvider_NatTable implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_contextMenu_NatTable != null) {
            _contextMenu_NatTable.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _contextMenu_NatTable;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _contextMenu_NatTable = createUI_24_NatTable_ViewerContextMenu();

         return _contextMenu_NatTable;
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

         _contextMenu_Tree = createUI_62_Tree_ViewerContextMenu();

         return _contextMenu_Tree;
      }

   }

   private static class ItemComparer_Tree implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {
            return true;
         }

         if (a instanceof final TVITourBookYear item1 && b instanceof final TVITourBookYear item2) {

            return item1.tourYear == item2.tourYear;
         }

         if (a instanceof final TVITourBookYearCategorized item1 && b instanceof final TVITourBookYearCategorized item2) {

            return item1.tourYear == item2.tourYear && item1.tourYearSub == item2.tourYearSub;
         }

         if (a instanceof final TVITourBookTour item1 && b instanceof final TVITourBookTour item2) {

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

         final ImagePainter imagePainter = new ImagePainter() {

            @Override
            protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry) {

               // get the row object

               final int rowIndex = cell.getRowIndex();

               final TVITourBookTour tviTour = _dataProvider.getRowObject(rowIndex);

               if (tviTour == null) {
                  return null;
               }

               final long tourTypeId = tviTour.getTourTypeId();
               if (tourTypeId == -1) {

                  // this can occur when the dummy tour is displayed

                  return null;
               }

               return TourTypeImage.getTourTypeImage(tourTypeId);
            }
         };

         // apply painter to the body cells and not to the header cells
         configRegistry.registerConfigAttribute(

               CellConfigAttributes.CELL_PAINTER,
               imagePainter,
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

         final ImagePainter imagePainter = new ImagePainter() {

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
               imagePainter,
               DisplayMode.NORMAL,
               TableColumnFactory.WEATHER_CLOUDS_ID);
      }
   }

   private class NatTable_Configuration_CellStyle extends AbstractRegistryConfiguration {

      private List<ColumnDefinition> _allSortedColumns;

      public NatTable_Configuration_CellStyle(final List<ColumnDefinition> allSortedColumns) {

         _allSortedColumns = allSortedColumns;
      }

      @Override
      public void configureRegistry(final IConfigRegistry configRegistry) {

         // loop: all displayed columns
         for (final ColumnDefinition colDef : _allSortedColumns) {

            // setup style for body + header, the style also applies to images !

            Style style = new Style();
            style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT,
                  convertColumnAlignment(colDef.getColumnStyle()));

            // apply style:

            final String columnId = colDef.getColumnId();

            // body style
            configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                  style,
                  DisplayMode.NORMAL,
                  columnId);

            // clone header style
            style = new Style(style);
            configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                  style,
                  DisplayMode.NORMAL,
                  columnId + HEADER_COLUMN_ID_POSTFIX);
         }
      }

      /**
       * Convert col def style -> nat table style
       *
       * @param columnStyle
       *
       * @return
       */
      private HorizontalAlignmentEnum convertColumnAlignment(final int columnStyle) {

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
         style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR,
               UI.IS_DARK_THEME
                     ? Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW)
                     : GUIHelper.COLOR_YELLOW);

         configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.HOVER);

         style = new Style();
         style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_RED);

         configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.SELECT_HOVER);
      }
   }

   private class NatTable_Configuration_Theme_Dark extends DarkNatTableThemeConfiguration {

      public NatTable_Configuration_Theme_Dark() {

         super();

         /*
          * Overwrite default modern theme
          */

// SET_FORMATTING_OFF

         final Color defaultBackgroundColor_Table        = ThemeUtil.getDefaultBackgroundColor_Table();
         final Color defaultBackgroundColor_TableHeader  = ThemeUtil.getDefaultBackgroundColor_TableHeader();

         final Color defaultForegroundColor_Table        = ThemeUtil.getDefaultForegroundColor_Table();
         final Color defaultForegroundColor_TableHeader  = ThemeUtil.getDefaultForegroundColor_TableHeader();

         this.evenRowBgColor                    = defaultBackgroundColor_Table;
         this.oddRowBgColor                     = defaultBackgroundColor_Table;

         this.evenRowFgColor                    = defaultForegroundColor_Table;
         this.oddRowFgColor                     = defaultForegroundColor_Table;

         // column header styling
         this.cHeaderBgColor                    = defaultBackgroundColor_TableHeader;
         this.cHeaderFgColor                    = defaultForegroundColor_TableHeader;
         this.cHeaderGradientBgColor            = defaultBackgroundColor_TableHeader;
         this.cHeaderGradientFgColor            = defaultBackgroundColor_TableHeader;

         // column header selection style
         this.cHeaderSelectionGradientBgColor   = defaultBackgroundColor_TableHeader;
         this.cHeaderSelectionGradientFgColor   = defaultBackgroundColor_TableHeader;

         // row header styling
         this.rHeaderGradientBgColor            = defaultBackgroundColor_TableHeader;
         this.rHeaderGradientFgColor            = defaultBackgroundColor_TableHeader;

         // row header selection style
         this.rHeaderSelectionGradientBgColor   = defaultBackgroundColor_TableHeader;
         this.rHeaderSelectionGradientFgColor   = defaultBackgroundColor_TableHeader;

         // hide grid lines
         this.renderBodyGridLines               = false;

         // show selection header with default colors
         this.cHeaderSelectionBgColor           = cHeaderBgColor;
         this.cHeaderSelectionFgColor           = cHeaderFgColor;

         // default selection style
         this.defaultSelectionBgColor           = GUIHelper.COLOR_LIST_SELECTION;
         this.defaultSelectionFgColor           = GUIHelper.COLOR_LIST_SELECTION_TEXT;

// SET_FORMATTING_ON

         // show sort column indicator in black than in white
         final SortableHeaderTextPainter interiorPainter = new SortableHeaderTextPainter(
               new TextPainter(false, false),
               CellEdgeEnum.RIGHT,
               new SortIconPainter(false, false),
               false,
               0,
               true // with this fix, the sort column indicator is not written over the column label
         );

         this.selectedSortHeaderCellPainter = new BackgroundPainter(new PaddingDecorator(interiorPainter,
               0, // top
               2, // right
               0, // bottom
               5, // left
               false // is paint bg
         ));

         // freeze column separator
         this.freezeSeparatorColor = GUIHelper.COLOR_WIDGET_BORDER;
      }
   }

   private class NatTable_Configuration_Theme_Light extends ModernNatTableThemeConfiguration {

      public NatTable_Configuration_Theme_Light() {

         super();

         /*
          * Overwrite default modern theme
          */

         // hide grid lines
         this.renderBodyGridLines = false;

         // show selection header with default colors
         this.cHeaderSelectionBgColor = cHeaderBgColor;
         this.cHeaderSelectionFgColor = cHeaderFgColor;

//       public static final Color COLOR_LIST_SELECTION = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
//       public static final Color COLOR_LIST_SELECTION_TEXT = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);

         // default selection style
         this.defaultSelectionBgColor = GUIHelper.COLOR_LIST_SELECTION;
         this.defaultSelectionFgColor = GUIHelper.COLOR_LIST_SELECTION_TEXT;
//       this.defaultSelectionBgColor = GUIHelper.COLOR_BLACK;
//       this.defaultSelectionFgColor = GUIHelper.COLOR_YELLOW;

         // show sort column indicator in black than in white
         final SortableHeaderTextPainter interiorPainter = new SortableHeaderTextPainter(
               new TextPainter(false, false),
               CellEdgeEnum.RIGHT,
//             new SortIconPainter(false, true),
               new SortIconPainter(false, false),
               false,
               0,
//             false);
               true // with this fix, the sort column indicator is not written over the column label
         );

         this.selectedSortHeaderCellPainter = new BackgroundPainter(new PaddingDecorator(interiorPainter,
               0, // top
               2, // right
               0, // bottom
               5, // left
               false // is paint bg
         ));

         // freeze column separator
         this.freezeSeparatorColor = GUIHelper.COLOR_WIDGET_BORDER;
      }
   }

   private class NatTable_KeyAction_DeleteTours implements IKeyAction {

      @Override
      public void run(final NatTable natTable, final KeyEvent event) {

         // call action which is deleting selected tours
         _actionDeleteTour.run();
      }
   }

   private class NatTable_ReorderListener implements ILayerListener {

      @Override
      public void handleLayerEvent(final ILayerEvent event) {

         if (event instanceof ColumnReorderEvent) {

            _parent.getDisplay().asyncExec(() -> {

//               // update MT column manager with the reordered columns
//               _columnManager_NatTable.setVisibleColumnIds_FromViewer();
//
//               final ArrayList<ColumnDefinition> allSortedColumns = _natTable_DataLoader.allSortedColumns;
//
////               natTable_SetColumnWidths(allSortedColumns, _natTable_Body_DataLayer);
//               natTable_RegisterColumnLabels(allSortedColumns, _natTable_Body_DataLayer, _natTable_ColumnHeader_DataLayer);

            });
         }
      }
   }

   public enum TourCollectionFilter {

      /**
       * All tours but they are filtered by the app filters
       */
      ALL_TOURS,

      /**
       * Only tours are displayed which are selected/collected
       */
      COLLECTED_TOURS,

      /**
       * Only tours are displayed which are not selected/collected
       */
      NOT_COLLECTED_TOURS
   }

   void actionExportViewCSV() {

      /*
       * Get selected items
       */
      final ISelection selection;

      if (_isLayoutNatTable) {

         // flat view

         final RowSelectionModel<TVITourBookTour> rowSelectionModel = getNatTable_SelectionModel();
         final List<TVITourBookTour> selectedTVITours = rowSelectionModel.getSelectedRowObjects();

         final List<TVITourBookTour> sortedItems = new ArrayList<>();

         for (final TVITourBookTour tviTour : selectedTVITours) {

            // collect only fetched items, the others are "empty" !!!
            if (tviTour.colTourDateTime != null) {

               sortedItems.add(tviTour);
            }
         }

         Collections.sort(sortedItems);

         selection = new StructuredSelection(sortedItems);

      } else {

         // tree view

         selection = _tourViewer_Tree.getSelection();
      }

      if (selection.isEmpty()) {
         return;
      }

      /*
       * Get export filename
       */
      final String defaultExportFilePath = _state.get(STATE_CSV_EXPORT_PATH);

      final String defaultExportFileName = CSV_EXPORT_DEFAULT_FILE_NAME
            + TimeTools.now().format(TimeTools.Formatter_FileName)
            + UI.SYMBOL_DOT
            + Util.CSV_FILE_EXTENSION;

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

      if (exportFilePath.exists() && net.tourbook.ui.UI.confirmOverwrite(exportFilePath) == false) {
         // don't overwrite file, nothing more to do
         return;
      }

      new CSVExport(
            selection,
            selectedFilePath,
            this,
            USE_SIMPLE_CSV_EXPORT_FORMAT);

//      // DEBUGGING: USING DEFAULT PATH
//      final IPath path = new Path(defaultExportFilePath).removeLastSegments(1).append(defaultExportFileName);
//
//      new CSVExport(selection, path.toOSString());
   }

   private void actionGoToLastTour() {

      if (_isLayoutNatTable) {

         // flat view

         final int numRows = _natTable_DataProvider.getRowCount();

         if (numRows > 0) {

            final int[] lastRowPosition = { numRows - 1 };

            selectTours_NatTable(lastRowPosition, true, true, true);
         }

      } else {

         // tree view

         final List<TreeViewerItem> allLastChildren = new ArrayList<>();

         actionGotoToday_GetLastChild(_rootItem_Tree, allLastChildren);

         final TreeViewerItem[] treePathArray = allLastChildren.toArray(new TreeViewerItem[allLastChildren.size()]);
         final TreePath lastChildTreePath = new TreePath(treePathArray);
         final TreeSelection treeSelection = new TreeSelection(lastChildTreePath);

         final Tree tree = _tourViewer_Tree.getTree();

         if (_isBehaviour_SingleExpand_CollapseOthers) {

            _isInExpandingSelection = true;
            {
               /*
                * Collapse all tree paths
                */
               tree.setRedraw(false);
               {
                  _isInSelection = true;
                  {
                     final TreePath[] allExpandedTreePaths = _tourViewer_Tree.getExpandedTreePaths();

                     for (final TreePath treePath : allExpandedTreePaths) {

                        // skip last child when it is already open to prevent flickering

                        if (lastChildTreePath.startsWith(treePath, null)) {
                           continue;
                        }

                        _tourViewer_Tree.setExpandedState(treePath, false);
                     }
                  }
                  _isInSelection = false;
               }
               tree.setRedraw(true);
            }
            _isInExpandingSelection = false;
         }

         /*
          * Goto/expand last item
          */
         tree.getDisplay().asyncExec(() -> {

            if (tree.isDisposed()) {
               return;
            }

            tree.setRedraw(false);
            {
               _tourViewer_Tree.setSelection(treeSelection, true);
            }
            tree.setRedraw(true);
         });
      }
   }

   /**
    * !!! Recursive !!!
    *
    * @param parentItem
    * @param allLastChildren
    *
    * @return
    */
   private TreeViewerItem actionGotoToday_GetLastChild(final TreeViewerItem parentItem,
                                                       final List<TreeViewerItem> allLastChildren) {

      final List<TreeViewerItem> allChildren = parentItem.getFetchedChildren();

      final int numChildren = allChildren.size();

      if (numChildren > 0) {

         TreeViewerItem lastChild = allChildren.get(numChildren - 1);

         if (lastChild instanceof final TVITourBookYear yearItem) {

            // skip summary item

            if (yearItem.isRowSummary) {

               // use the last available year

               if (numChildren > 1) {

                  // use previous child
                  lastChild = allChildren.get(numChildren - 2);

               } else {

                  // there is only a total row

                  return null;
               }
            }
         }

         // keep child tree
         allLastChildren.add(lastChild);

         return actionGotoToday_GetLastChild(lastChild, allLastChildren);
      }

      return null;
   }

   void actionSelectYearMonthTours() {

      if (_actionSelectAllTours.isChecked()) {

         // reselect selection
         _tourViewer_Tree.setSelection(_tourViewer_Tree.getSelection());
      }
   }

   /**
    * Toggle view layout, when the Ctrl-key is pressed, then the toggle action is reversed.
    * <p>
    * <code>
    * Forward:    month    -> week        -> natTable  -> month...<br>
    * Reverse:    month    -> natTable    -> week      -> month...
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

            // week -> natTable

            toggleLayout_NatTable();

         } else {

            // week -> month

            toggleLayout_Category_Month();
         }

      } else if (_viewLayout == TourBookViewLayout.NAT_TABLE) {

         if (isForwards) {

            // natTable -> month

            toggleLayout_Category_Month();

         } else {

            // natTable -> week

            toggleLayout_Category_Week();
         }
      }

      reopenFirstSelectedTour();

      // must be called AFTER the view content is set otherwise a NPE could occur
      enableActions();
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

               // ensure the tour tooltip is hidden, it occurred that even closing this view did not close the tooltip
               if (_tourInfoToolTip_NatTable != null) {
                  _tourInfoToolTip_NatTable.hideToolTip();
               }

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

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)

               // when a tour type is deleted then the tours from the db must be reloaded
               || property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)

               || property.equals(ITourbookPreferences.VIEW_PREFERRED_TEMPERATURE_VALUE)) {

            /*
             * Flat view do not preserve column reordering when reloaded -> recreate it
             */
            if (_isLayoutNatTable) {

               // save column ordering
               saveState();

               recreateViewer_NatTable();

            } else {

               reloadViewer();
            }

            enableActions();

         } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

            _columnFactory.updateToolTipState();

         } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _columnFactory.updateColors();

            _tourViewer_Tree.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _tourViewer_Tree.refresh();

            /*
             * The tree must be redrawn because the styled text does not show with the new color
             */
            _tourViewer_Tree.getTree().redraw();
         }
      };

      /*
       * Common preferences
       */
      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

            recreateViewer_NatTable();
            _tourViewer_Tree = (TreeViewer) recreateViewer_Tree();

         } else if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            _columnManager_NatTable.saveState(_state_NatTable,
                  _natTable_Body_DataLayer,
                  _natTable_Body_ColumnReorderLayer,
                  _natTable_Body_ColumnHideShowLayer);
            _columnManager_NatTable.clearColumns();

            _columnManager_Tree.saveState(_state_Tree);
            _columnManager_Tree.clearColumns();

            _columnFactory.defineAllColumns();

            recreateViewer_NatTable();
            _tourViewer_Tree = (TreeViewer) recreateViewer_Tree();
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = (workbenchPart, selection) -> {

         // prevent to listen to a selection which is originated by this year chart
         if (workbenchPart == TourBookView.this) {
            return;
         }

         onSelectionChanged(selection, workbenchPart);
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   @SuppressWarnings("unchecked")
   private void addTourEventListener() {

      _tourPropertyListener = (part, tourEventId, eventData) -> {

         if (part == TourBookView.this) {
            return;
         }

         if (tourEventId == TourEventId.TOUR_CHANGED || tourEventId == TourEventId.UPDATE_UI) {

            /*
             * it is possible when a tour type was modified, the tour can be hidden or visible in
             * the viewer because of the tour type filter
             */
            reloadViewer();

         } else if (tourEventId == TourEventId.TOUR_SELECTION && eventData instanceof final ISelection selection) {

            onSelectionChanged(selection, part);

         } else if (tourEventId == TourEventId.TAG_STRUCTURE_CHANGED
               || tourEventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            reloadViewer();

         } else if (tourEventId == TourEventId.FULLTEXT_SEARCH_TOURS) {

            if (eventData instanceof final ArrayList allTourIDs) {

               setCollectionFilterFromFulltextSearch(allTourIDs);
            }

         } else if (tourEventId == TourEventId.TOUR_LOCATION_SELECTION) {

            if (eventData instanceof final List allTourLocations) {

               if (allTourLocations.size() > 0) {

                  // select tours from the tour locations

                  // check if enabled
                  if (_actionLinkWithOtherViews.getSelection() == false) {

                     // linking is disabled

                     return;
                  }

                  final List<Long> allTourIds = TourLocationManager.getToursWithLocations(allTourLocations);

                  _selectedTourIds.clear();
                  _selectedTourIds.addAll(allTourIds);

                  _postSelectionProvider.clearSelection();

                  if (_isLayoutNatTable) {

                     reselectTourViewer_NatTable(false);

                  } else {

                     /**
                      * !!! This do not yet support the tree viewer, it's too complex !!!
                      */
//                   reselectTourViewer_Tree();
                  }
               }
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
   private void closeOpenedDialogs(final IOpeningDialog openingDialog) {

      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionAdjustTourValues          = new SubMenu_AdjustTourValues(this, this);
      _actionCollapseAll               = new ActionCollapseAll_WithoutSelection(this);
      _actionCreateTourMarkers         = new ActionCreateTourMarkers();
      _actionCollapseOthers            = new ActionCollapseOthers(this);
      _actionDeleteTourMenu            = new ActionDeleteTourMenu(this);
      _actionDeleteTourValues          = new ActionDeleteTourValues(this);
      _actionDuplicateTour             = new ActionDuplicateTour(this);
      _actionEditQuick                 = new ActionEditQuick(this);
      _actionEditTour                  = new ActionEditTour(this);
      _actionExpandSelection           = new ActionExpandSelection(this);
      _actionExportTour                = new ActionExport(this);
      _actionExportViewCSV             = new ActionExportViewCSV(this);
      _actionGotoToday                 = new ActionGotoLastTour();
      _actionJoinTours                 = new ActionJoinTours(this);
      _actionMergeTour                 = new ActionMergeTour(this);
      _actionLinkWithOtherViews        = new ActionLinkWithOtherViews();
      _actionOpenAdjustAltitudeDialog  = new ActionOpenAdjustAltitudeDialog(this);
      _actionOpenMarkerDialog          = new ActionOpenMarkerDialog(this, true);
      _actionOpenTour                  = new ActionOpenTour(this);
      _actionPrintTour                 = new ActionPrint(this);
      _actionReimport_Tours            = new ActionReimportTours(this);
      _actionRefreshView               = new ActionRefreshView(this);
      _actionSelectAllTours            = new ActionSelectAllTours(this);
      _actionSetOtherPerson            = new ActionSetPerson(this);
      _actionSetStartEndLocation       = new ActionSetStartEndLocation(this, _parent);
      _actionToggleViewLayout          = new ActionToggleViewLayout(this);
      _actionTourBookOptions           = new ActionTourBookOptions();
      _actionTourCollectionFilter      = new ActionTourCollectionFilter();
      _actionUploadTour                = new ActionUpload(this);

      _actionContext_OnMouseSelect_ExpandCollapse  = new ActionOnMouseSelect_ExpandCollapse();
      _actionContext_SingleExpand_CollapseOthers   = new ActionSingleExpand_CollapseOthers();

      _allTourActions_Adjust  = new HashMap<>();
      _allTourActions_Edit    = new HashMap<>();
      _allTourActions_Export  = new HashMap<>();

      _allTourActions_Edit.put(_actionEditQuick                   .getClass().getName(),  _actionEditQuick);
      _allTourActions_Edit.put(_actionEditTour                    .getClass().getName(),  _actionEditTour);
      _allTourActions_Edit.put(_actionOpenMarkerDialog            .getClass().getName(),  _actionOpenMarkerDialog);
      _allTourActions_Edit.put(_actionOpenAdjustAltitudeDialog    .getClass().getName(),  _actionOpenAdjustAltitudeDialog);
      _allTourActions_Edit.put(_actionSetStartEndLocation         .getClass().getName(),  _actionSetStartEndLocation);
      _allTourActions_Edit.put(_actionOpenTour                    .getClass().getName(),  _actionOpenTour);
      _allTourActions_Edit.put(_actionDuplicateTour               .getClass().getName(),  _actionDuplicateTour);
      _allTourActions_Edit.put(_actionCreateTourMarkers           .getClass().getName(),  _actionCreateTourMarkers);
      _allTourActions_Edit.put(_actionMergeTour                   .getClass().getName(),  _actionMergeTour);
      _allTourActions_Edit.put(_actionJoinTours                   .getClass().getName(),  _actionJoinTours);

      _allTourActions_Export.put(_actionUploadTour                .getClass().getName(),  _actionUploadTour);
      _allTourActions_Export.put(_actionExportTour                .getClass().getName(),  _actionExportTour);
      _allTourActions_Export.put(_actionExportViewCSV             .getClass().getName(),  _actionExportViewCSV);
      _allTourActions_Export.put(_actionPrintTour                 .getClass().getName(),  _actionPrintTour);

      _allTourActions_Adjust.put(_actionAdjustTourValues          .getClass().getName(),  _actionAdjustTourValues);
      _allTourActions_Adjust.put(_actionDeleteTourValues          .getClass().getName(),  _actionDeleteTourValues);
      _allTourActions_Adjust.put(_actionReimport_Tours            .getClass().getName(),  _actionReimport_Tours);
      _allTourActions_Adjust.put(_actionSetOtherPerson            .getClass().getName(),  _actionSetOtherPerson);
      _allTourActions_Adjust.put(_actionDeleteTourMenu            .getClass().getName(),  _actionDeleteTourMenu);

// SET_FORMATTING_ON

      TourActionManager.setAllViewActions(ID,
            _allTourActions_Edit.keySet(),
            _allTourActions_Export.keySet(),
            _allTourActions_Adjust.keySet(),
            _tagMenuManager.getAllTagActions().keySet(),
            _tourTypeMenuManager.getAllTourTypeActions().keySet());

      fillActionBars();
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);
      _tourTypeMenuManager = new TourTypeMenuManager(this);

      _viewerMenuManager_NatTable = new MenuManager();
      _viewerMenuManager_NatTable.setRemoveAllWhenShown(true);
      _viewerMenuManager_NatTable.addMenuListener(menuManager -> {

         _tourInfoToolTip_NatTable.hideToolTip();

         natTable_ContextMenu_OnShow(menuManager);
      });

      _viewerMenuManager_Tree = new MenuManager();
      _viewerMenuManager_Tree.setRemoveAllWhenShown(true);
      _viewerMenuManager_Tree.addMenuListener(menuManager -> {

         _tourInfoToolTip_Tree.hideToolTip();

         fillContextMenu(menuManager, true);
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createMenuManager();

      // define all columns for the viewer
      _columnManager_NatTable = new ColumnManager(this, _state_NatTable);
      _columnManager_NatTable.setIsCategoryAvailable(true);

      _columnManager_Tree = new ColumnManager(this, _state_Tree);
      _columnManager_Tree.setIsCategoryAvailable(true);

      _columnFactory = new TourBook_ColumnFactory(_columnManager_NatTable, _columnManager_Tree, _pc);
      _columnFactory.defineAllColumns();

      createUI(parent);
      createActions();

      addSelectionListener();
      addPartListener();
      addPrefListener();
      addTourEventListener();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      restoreState();

      // update the viewer

      // delay loading, that the app filters are initialized
      _parent.getDisplay().asyncExec(() -> {

         if (_tourViewer_Tree.getTree().isDisposed()) {
            return;
         }

         _isInStartup = true;

         _columnFactory.updateColors();

         setupTourViewerContent();

         reselectTourViewer();

         restoreState_AfterUI();

         enableActions();
      });
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _viewerContainer_NatTable = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_NatTable);
      {
         createUI_20_NatTable_TourViewer(_viewerContainer_NatTable);
      }

      _viewerContainer_Tree = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer_Tree);
      {
         createUI_30_Tree_TourViewer(_viewerContainer_Tree);
      }
   }

   private void createUI_20_NatTable_TourViewer(final Composite parent) {

      // this MUST be done after the nattable is created
      _columnManager_NatTable.setupNatTable(this);

      // create a new ConfigRegistry which will be needed for GlazedLists handling
      final ConfigRegistry configRegistry = new ConfigRegistry();

      // data provider
      _natTable_DataLoader = new NatTable_DataLoader(this, _columnManager_NatTable);

      // body layer
      _natTable_DataProvider = new TourRowDataProvider(_natTable_DataLoader);
      _natTable_Body_DataLayer = new DataLayer(_natTable_DataProvider);

      // hover layer
      _natTable_Body_HoverLayer = new HoverLayer(_natTable_Body_DataLayer);

      // column drag&drop layer
      _natTable_Body_ColumnReorderLayer = new ColumnReorderLayer(_natTable_Body_HoverLayer);

      // show/hide columns
      _natTable_Body_ColumnHideShowLayer = new ColumnHideShowLayer(_natTable_Body_ColumnReorderLayer);

      /*
       * Selection layer
       */
      _natTable_Body_SelectionLayer = new SelectionLayer(_natTable_Body_ColumnHideShowLayer, false);

      // register the DefaultRowSelectionLayerConfiguration that contains the
      // default styling and functionality bindings (search, tick update)
      // and different configurations for a move command handler that always
      // moves by a row and row only selection bindings
      _natTable_Body_SelectionLayer.addConfiguration(new DefaultRowSelectionLayerConfiguration());

      // use a RowSelectionModel that will perform row selections and is able to identify a row via unique ID
      final IRowIdAccessor<TVITourBookTour> rowIdAccessor = rowObject -> rowObject.tourId;
      _natTable_Body_SelectionLayer.setSelectionModel(new RowSelectionModel<>(
            _natTable_Body_SelectionLayer,
            _natTable_DataProvider,
            rowIdAccessor));

      /*
       * Body viewport
       */
      _natTable_Body_ViewportLayer = new ViewportLayer(_natTable_Body_SelectionLayer);
      _natTable_Body_ViewportLayer.addConfiguration(new NatTable_ConfigField_TourType(_natTable_DataProvider));
      _natTable_Body_ViewportLayer.addConfiguration(new NatTable_ConfigField_Weather(_natTable_DataProvider));
      _natTable_Body_ViewportLayer.addLayerListener(new NatTable_ReorderListener());

      /*
       * Freeze columns
       */
      final FreezeLayer freezeLayer = new FreezeLayer(_natTable_Body_SelectionLayer);
      final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(
            freezeLayer,
            _natTable_Body_ViewportLayer,
            _natTable_Body_SelectionLayer);

      /*
       * Column header layer
       */
      final IDataProvider columnHeader_DataProvider = new DataProvider_ColumnHeader(_natTable_DataLoader, _columnManager_NatTable);
      _natTable_ColumnHeader_DataLayer = new DataLayer(columnHeader_DataProvider);
      _natTable_ColumnHeader_Layer = new ColumnHeaderLayer(
            _natTable_ColumnHeader_DataLayer,
            compositeFreezeLayer,
            _natTable_Body_SelectionLayer);

      // header sorting
      _natTable_SortModel = new NatTable_SortModel(_columnManager_NatTable, _natTable_DataLoader);

      final SortHeaderLayer<TVITourBookTour> sortHeaderLayer = new SortHeaderLayer<>(_natTable_ColumnHeader_Layer, _natTable_SortModel);

      // add single click handler to sort the column without pressing additional the ALT key
      sortHeaderLayer.addConfiguration(new SingleClickSortConfiguration_MT(_columnManager_NatTable));
      sortHeaderLayer.addLayerListener(layerEvent -> natTable_OnColumnSort(layerEvent));

      /*
       * Row header layer
       */
      final DefaultRowHeaderDataProvider rowHeader_DataProvider = new DefaultRowHeaderDataProvider(_natTable_DataProvider);
      final DefaultRowHeaderDataLayer rowHeader_DataLayer = new DefaultRowHeaderDataLayer(rowHeader_DataProvider);
      final ILayer rowHeader_Layer = new RowHeaderLayer(rowHeader_DataLayer, compositeFreezeLayer, _natTable_Body_SelectionLayer);

      /*
       * Corner layer
       */
      final DefaultCornerDataProvider corner_DataProvider = new DefaultCornerDataProvider(columnHeader_DataProvider, rowHeader_DataProvider);
      final DataLayer corner_DataLayer = new DataLayer(corner_DataProvider);
      final ILayer corner_Layer = new CornerLayer(corner_DataLayer, rowHeader_Layer, sortHeaderLayer);

      /*
       * Create: Grid layer composed with the prior created layer stacks
       */
      final GridLayer gridLayer = new GridLayer(compositeFreezeLayer, sortHeaderLayer, rowHeader_Layer, corner_Layer);

      /*
       * Setup other data
       */
      final List<ColumnDefinition> allSortedColumns = _natTable_DataLoader.allSortedColumns;

      natTable_SetColumnWidths(allSortedColumns, _natTable_Body_DataLayer);
      natTable_RegisterColumnLabels(allSortedColumns, _natTable_Body_DataLayer, _natTable_ColumnHeader_DataLayer);

      /*
       * Create table
       */
      // turn the auto configuration off as we want to add custom configurations
      _tourViewer_NatTable = new NatTable(parent, gridLayer, false);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_tourViewer_NatTable);

      _tourViewer_NatTable.setConfigRegistry(configRegistry);

      _columnManager_NatTable.setupNatTable_PostCreate();
      natTable_SetupColumnSorting();
      restoreState_SortColumns();

      final UiBindingRegistry uiBindingRegistry = _tourViewer_NatTable.getUiBindingRegistry();

      // add mouse double click listener
      final IMouseAction mouseDoubleClickAction = (natTable, mouseEvent) -> {

         _tourDoubleClickState.tourLocationFocus = getTourLocation_HoverState();

         TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);
      };

      uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(SWT.NONE), mouseDoubleClickAction);

      // setup selection listener for the nattable
      final ISelectionProvider selectionProvider = new RowSelectionProvider<>(
            _natTable_Body_SelectionLayer,
            _natTable_DataProvider,
            false, // Provides rows where any cell in the row is selected
            true // handleSameRowSelection, this is activated that different tour locations of the same tour can fire a selection
      );

      selectionProvider.addSelectionChangedListener(selectionChangedEvent -> onSelect_NatTableItem(selectionChangedEvent));

      // prevent selecting all cells when column header is clicked on a column which cannot be sorted
      uiBindingRegistry.registerSingleClickBinding(MouseEventMatcher.columnHeaderLeftClick(SWT.NONE), null);

      // prevent sorting columns with Alt key which sorting is disabled
      uiBindingRegistry.registerSingleClickBinding(MouseEventMatcher.columnHeaderLeftClick(SWT.MOD3), null);

      // delete a tour with the delete/backspace key
      final NatTable_KeyAction_DeleteTours natTable_KeyAction_DeleteTours = new NatTable_KeyAction_DeleteTours();

      uiBindingRegistry.registerFirstKeyBinding(new KeyEventMatcher(

            // <Ctrl><Shift>
            SWT.MOD1 | SWT.MOD2,

            SWT.DEL),

            natTable_KeyAction_DeleteTours);

      uiBindingRegistry.registerFirstKeyBinding(new KeyEventMatcher(

            // <Ctrl><Shift>
            SWT.MOD1 | SWT.MOD2,

            // Mac has no DEL key, support also BACKSPACE
            SWT.BS),

            natTable_KeyAction_DeleteTours);

      /*
       * Setup NatTable configuration
       */

      // as the autoconfiguration of the NatTable is turned off, we have to add the DefaultNatTableStyleConfiguration manually
      _tourViewer_NatTable.addConfiguration(new DefaultNatTableStyleConfiguration());

      _tourViewer_NatTable.addConfiguration(new NatTable_Configuration_CellStyle(_natTable_DataLoader.allSortedColumns));

      // add the style configuration for hover
      _tourViewer_NatTable.addConfiguration(new NatTable_Configuration_Hover());

//    // add debug menu, this will hide MT context menu
//    _tourViewer_NatTable.addConfiguration(new DebugMenuConfiguration(_tourViewer_NatTable));

      _tourViewer_NatTable.configure();

      // set header tooltip
      _natTable_Tooltip = new NatTable_Header_Tooltip(_tourViewer_NatTable, this);
      _natTable_Tooltip.setPopupDelay(0);

      createUI_22_NatTable_ContextMenu();

      // set tour info tooltip provider
      _tourInfoToolTip_NatTable = new NatTableViewer_TourInfo_ToolTip(this, ToolTip.NO_RECREATE);

      // set start/end location info tooltip provider
      _tourLocationTooltip_NatTable = new TourLocationToolTip(this, true);

      // ensure that tooltips are hidden
      _tourViewer_NatTable.addListener(SWT.MouseExit, event -> hideTooltip());

      _natTable_DummyColumnViewer = new NatTable_DummyColumnViewer(this);

      // this must be run async otherwise the dark theme is not yet initialized !!!
      _parent.getDisplay().asyncExec(() -> {

         // overwrite theme with MT's own theme, which is based on the modern or dark theme
         final ThemeConfiguration themeConfiguration = UI.IS_DARK_THEME
               ? new NatTable_Configuration_Theme_Dark()
               : new NatTable_Configuration_Theme_Light();

         _tourViewer_NatTable.setTheme(themeConfiguration);
      });
   }

   /**
    * Setup context menu for the nattable
    */
   private void createUI_22_NatTable_ContextMenu() {

      _contextMenu_NatTable = createUI_24_NatTable_ViewerContextMenu();

      _columnManager_NatTable.createHeaderContextMenu(
            _tourViewer_NatTable,
            _viewerContextMenuProvider_NatTable,
            _natTable_ColumnHeader_Layer);
   }

   /**
    * Creates context menu for the viewer
    *
    * @return Returns the {@link Menu} widget
    */
   private Menu createUI_24_NatTable_ViewerContextMenu() {

      final Menu contextMenu = _viewerMenuManager_NatTable.createContextMenu(_tourViewer_NatTable);

      _tourViewer_NatTable.addListener(SWT.MenuDetect, event -> natTable_ContextMenu_OnMenuDetect(event));

      contextMenu.addMenuListener(new MenuAdapter() {

         @Override
         public void menuHidden(final MenuEvent e) {

            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {

            // hide tour location tooltip
            _tourLocationTooltip_NatTable.hide();
            _tourLocationTooltip_Tree.hide();

            // set true/false/null
            _actionSetStartEndLocation.setIsStartLocation(getTourLocation_HoverState());

            _tagMenuManager.onShowMenu(menuEvent,
                  _tourViewer_NatTable,
                  Display.getCurrent().getCursorLocation(),
                  _tourInfoToolTip_NatTable);
         }

      });

      return contextMenu;
   }

   private void createUI_30_Tree_TourViewer(final Composite parent) {

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

      _tourViewer_Tree.addSelectionChangedListener(selectionChangedEvent -> onSelect_TreeItem(selectionChangedEvent));

      _tourViewer_Tree.addDoubleClickListener(doubleClickEvent -> {

         final Object selection = ((IStructuredSelection) _tourViewer_Tree.getSelection()).getFirstElement();

         if (selection instanceof TVITourBookTour) {

            _tourDoubleClickState.tourLocationFocus = getTourLocation_HoverState();

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
      });

      tree.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         _isSelectedWithKeyboard = true;

         if (UI.isCtrlKey(keyEvent) && UI.isShiftKey(keyEvent)

               && (keyEvent.keyCode == SWT.DEL

                     // Mac has no DEL key, support also BACKSPACE
                     || keyEvent.keyCode == SWT.BS)

         ) {

            // call action which is deleting selected tours
            _actionDeleteTour.run();
         }
      }));

      /*
       * Center images horizontally
       */
      createUI_40_Tree_ColumnImages(tree);

      /*
       * The context menu must be created after the viewer is created which is also done after the
       * measurement system has changed
       */
      createUI_60_Tree_ContextMenu();

      // set tour info tooltip provider
      _tourInfoToolTip_Tree = new TreeViewerTourInfoToolTip(_tourViewer_Tree);

      // set start/end location info tooltip provider
      _tourLocationTooltip_Tree = new TourLocationToolTip(this, false);

      // ensure that tooltips are hidden
      tree.addListener(SWT.MouseExit, event -> hideTooltip());
   }

   private void createUI_40_Tree_ColumnImages(final Tree tree) {

      _colDef_TourTypeImage_Tree = _columnFactory.getColDef_TourTypeImage_Tree();
      _colDef_WeatherClouds_Tree = _columnFactory.getColDef_WeatherClouds_Tree();

      final ControlListener controlResizedAdapter = ControlListener.controlResizedAdapter(controlEvent -> onResize_SetWidthForImageColumn());

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _columnManager_Tree.getActiveProfile();
      _columnIndex_TourTypeImage = activeProfile.getColumnIndex(_colDef_TourTypeImage_Tree.getColumnId());
      _columnIndex_WeatherClouds = activeProfile.getColumnIndex(_colDef_WeatherClouds_Tree.getColumnId());

      boolean isColumnVisible = false;
      final int numColumns = tree.getColumns().length;

      // add column resize listener
      if (_columnIndex_TourTypeImage >= 0 && _columnIndex_TourTypeImage < numColumns) {

         isColumnVisible = true;
         tree.getColumn(_columnIndex_TourTypeImage).addControlListener(controlResizedAdapter);
      }

      if (_columnIndex_WeatherClouds >= 0 && _columnIndex_WeatherClouds < numColumns) {

         isColumnVisible = true;
         tree.getColumn(_columnIndex_WeatherClouds).addControlListener(controlResizedAdapter);
      }

      // add tree resize listener
      if (isColumnVisible) {

         /*
          * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
          * critical for performance that these methods be as efficient as possible.
          */
         final Listener treePaintListener = event -> {

            if (event.type == SWT.PaintItem) {

               onPaint_TreeViewer(event);
            }
         };

         tree.addControlListener(controlResizedAdapter);
         tree.addListener(SWT.PaintItem, treePaintListener);
      }
   }

   /**
    * Setup context menu for the viewer
    */
   private void createUI_60_Tree_ContextMenu() {

      _contextMenu_Tree = createUI_62_Tree_ViewerContextMenu();

      final Tree tree = (Tree) _tourViewer_Tree.getControl();

      _columnManager_Tree.createHeaderContextMenu(tree, _viewerContextMenuProvider_Tree);
   }

   /**
    * Creates context menu for the viewer
    *
    * @return Returns the {@link Menu} widget
    */
   private Menu createUI_62_Tree_ViewerContextMenu() {

      final Tree tree = (Tree) _tourViewer_Tree.getControl();

      final Menu contextMenu = _viewerMenuManager_Tree.createContextMenu(tree);

      contextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip_Tree);
         }
      });

      return contextMenu;
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);
      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      if (_natTable_DataLoader != null) {
         _natTable_DataLoader.resetTourItems(true);
         _natTable_DataLoader = null;
      }
      if (_rootItem_Tree != null) {
         _rootItem_Tree.clearChildren();
         _rootItem_Tree = null;
      }

      super.dispose();
   }

   private void enableActions() {

      int numTourItems = 0;
      int numSelectedItems = 0;

      boolean firstElementHasChildren = false;

      TVITourBookItem firstTreeElement = null;
      TVITourBookTour firstTourItem = null;

      if (_isLayoutNatTable) {

         final RowSelectionModel<TVITourBookTour> rowSelectionModel = getNatTable_SelectionModel();

         switch (_natTable_ContextMenuActivator) {

         case SWT.MENU_KEYBOARD:

            numTourItems = numSelectedItems = rowSelectionModel.getSelectedRowCount();

            if (numTourItems > 0) {

               final List<TVITourBookTour> allSelectedRows = rowSelectionModel.getSelectedRowObjects();
               firstTourItem = allSelectedRows.get(0);
            }

            break;

         case SWT.MENU_MOUSE:

            boolean isSelectionHovered = false;
            int hoveredRow = -1;

            final Point hoveredPosition = _natTable_Body_HoverLayer.getCurrentHoveredCellPosition();
            if (hoveredPosition != null) {

               hoveredRow = hoveredPosition.y;

               for (final Range range : rowSelectionModel.getSelectedRowPositions()) {

                  if (range.contains(hoveredRow)) {

                     isSelectionHovered = true;
                     break;
                  }
               }
            }

            if (isSelectionHovered) {

               // mouse is hovering the selected tours

               numTourItems = numSelectedItems = rowSelectionModel.getSelectedRowCount();

               final List<TVITourBookTour> selection = rowSelectionModel.getSelectedRowObjects();

               if (selection.isEmpty() == false) {
                  firstTourItem = selection.get(0);
               }

            } else {

               // mouse is not hovering a tour selection

               final TVITourBookTour fetchedTour = _natTable_DataLoader.getFetchedTour(hoveredRow);
               if (fetchedTour != null) {

                  numTourItems = numSelectedItems = 1;
                  firstTourItem = fetchedTour;
               }
            }
            break;
         }

      } else {

         final ITreeSelection selection = (ITreeSelection) _tourViewer_Tree.getSelection();

         /*
          * count number of selected items
          */

         for (final Object treeItem : selection) {

            if (treeItem instanceof final TVITourBookTour tviTourBookTour) {
               if (numTourItems == 0) {
                  firstTourItem = tviTourBookTour;
               }
               numTourItems++;
            }
         }

         firstTreeElement = (TVITourBookItem) selection.getFirstElement();
         firstElementHasChildren = firstTreeElement == null ? false : firstTreeElement.hasChildren();
         numSelectedItems = selection.size();
      }

      final boolean isTourSelected = numTourItems > 0;
      final boolean isOneTour = numTourItems == 1;
      final boolean isAllToursSelected = _actionSelectAllTours.isChecked();

      final int numAvailableItems = _isLayoutNatTable
            ? _natTable_DataLoader.getNumberOfToursWithoutCollectionFilter()
            : _rootItem_Tree.getChildren().size();

      // set initial state to false until data are loaded, actions are enabled only for a single tour -> multiple tour is always false
      _actionDuplicateTour.setEnabled(false);
      _actionMergeTour.setEnabled(false);
      _actionOpenAdjustAltitudeDialog.setEnabled(false);
      _actionOpenMarkerDialog.setEnabled(false);

      if (isOneTour) {

         // loading the first tour is very expensive (with a delay in the UI) -> run it async

         // make var to a effectively final var, otherwise an exception occurs
         final TVITourBookTour finalFirstTour = firstTourItem;

         CompletableFuture.supplyAsync(() -> TourManager.getInstance().getTourData(finalFirstTour.getTourId()))
               .thenAccept(savedTour -> {

                  if (savedTour != null) {

                     final boolean isManualTour = savedTour.isManualTour();
                     final boolean isDeviceTour = isManualTour == false;
                     final boolean canMergeTours = isOneTour && isDeviceTour && savedTour.getMergeSourceTourId() != null;

                     _actionDuplicateTour.setEnabled(isOneTour);
                     _actionMergeTour.setEnabled(canMergeTours);
                     _actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);
                     _actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
                  }
               });
      }

      final boolean isWeatherRetrievalActivated = TourManager.isWeatherRetrievalActivated();

      final boolean isTableLayout = _isLayoutNatTable;
      final boolean isTreeLayout = !isTableLayout;

      // set double click infos
      _tourDoubleClickState.canEditTour = isOneTour;
      _tourDoubleClickState.canOpenTour = isOneTour;
      _tourDoubleClickState.canQuickEditTour = isOneTour;
      _tourDoubleClickState.canEditMarker = isOneTour;
      _tourDoubleClickState.canAdjustAltitude = isOneTour;

      /*
       * enable actions
       */
      _actionAdjustTourValues.setEnabled(isTourSelected || isAllToursSelected);
      _actionAdjustTourValues.getActionRetrieveWeatherData().setEnabled(isWeatherRetrievalActivated);
      _actionAdjustTourValues.enableSubMenu_Pauses();
      _actionAdjustTourValues.enableSubMenu_Cadence();

// SET_FORMATTING_OFF

      // re-import and tour values deletion can be run on all/selected/between dates tours
      _actionReimport_Tours         .setEnabled(true);
      _actionDeleteTourMenu         .setEnabled(true);
      _actionDeleteTourValues       .setEnabled(true);

      _actionCreateTourMarkers      .setEnabled(isTourSelected);
      _actionEditQuick              .setEnabled(isOneTour);
      _actionEditTour               .setEnabled(isOneTour);
      _actionExportTour             .setEnabled(isTourSelected);
      _actionExportViewCSV          .setEnabled(numSelectedItems > 0);
      _actionGotoToday              .setEnabled(numAvailableItems > 0);
      _actionJoinTours              .setEnabled(numTourItems > 1);
      _actionOpenTour               .setEnabled(isOneTour);
      _actionPrintTour              .setEnabled(isTourSelected);
      _actionSetOtherPerson         .setEnabled(isTourSelected);
      _actionSetStartEndLocation    .setEnabled(isTourSelected);

// SET_FORMATTING_ON

      _actionCollapseAll.setEnabled(isTreeLayout);
      _actionCollapseOthers.setEnabled(isTreeLayout && (numSelectedItems == 1 && firstElementHasChildren));

      _actionExpandSelection.setEnabled(isTreeLayout

            && (firstTreeElement == null
                  ? false
                  : numSelectedItems == 1
                        ? firstElementHasChildren
                        : true));

      _actionSelectAllTours.setEnabled(isTreeLayout);
      _actionToggleViewLayout.setEnabled(true);
      _actionUploadTour.setEnabled(isTourSelected);

      _actionTourCollectionFilter.setEnabled(isTableLayout);
      _actionTourCollectionFilter.setTooltip(isTableLayout

            // slideout is displayed, hide tooltip
            ? UI.EMPTY_STRING

            : Messages.Slideout_TourCollectionFilter_Action_Tooltip);

      _tagMenuManager.enableTagActions(
            isTourSelected,
            isOneTour,
            firstTourItem == null
                  ? null
                  : firstTourItem.getTagIds());

      final long tourTypeID = isOneTour
            ? firstTourItem.getTourTypeId()
            : TourDatabase.ENTITY_IS_NOT_SAVED;

      _tourTypeMenuManager.enableTourTypeActions(isTourSelected, tourTypeID);
   }

   private void expandCollapseItem(final TreeViewerItem treeItem) {

      if (_tourViewer_Tree.getExpandedState(treeItem)) {

         _tourViewer_Tree.collapseToLevel(treeItem, 1);

      } else {

         _tourViewer_Tree.expandToLevel(treeItem, 1);
      }
   }

   private void fillActionBars() {

      /*
       * Fill view menu
       */
      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

      menuMgr.add(_actionRefreshView);

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionGotoToday);
      tbm.add(_actionToggleViewLayout);
      tbm.add(_actionTourCollectionFilter);
      tbm.add(_actionSelectAllTours);
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll);
      tbm.add(_actionLinkWithOtherViews);
      tbm.add(_actionTourBookOptions);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   /**
    * @param menuMgr
    * @param isTree
    *           When <code>true</code> then actions which can be applied to a tree, e.g.
    *           expand/collapse are also displayed.
    */
   private void fillContextMenu(final IMenuManager menuMgr, final boolean isTree) {

      // edit actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EDIT, _allTourActions_Edit, this);

      // tag actions
      _tagMenuManager.fillTagMenu_WithActiveActions(menuMgr, this);

      // tour type actions
      _tourTypeMenuManager.fillContextMenu_WithActiveActions(menuMgr, this);

      // tree only actions
      if (isTree) {

         menuMgr.add(new Separator());
         menuMgr.add(_actionCollapseOthers);
         menuMgr.add(_actionExpandSelection);
         menuMgr.add(_actionCollapseAll);
         menuMgr.add(_actionContext_OnMouseSelect_ExpandCollapse);
         menuMgr.add(_actionContext_SingleExpand_CollapseOthers);
      }

      // export actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EXPORT, _allTourActions_Export, this);

      // adjust actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.ADJUST, _allTourActions_Adjust, this);

      // customize this context menu
      TourActionManager.fillContextMenu_CustomizeAction(menuMgr)

            // set pref page custom data that actions from this view can be identified
            .setPrefData(new ViewContext(ID, ViewNames.VIEW_NAME_TOUR_BOOK));

      ActionEditQuick.setTourLocationFocus(getTourLocation_HoverState());

      enableActions();
   }

   /**
    * Set's the tour selection {@link #_selectedTourIds} and fires an
    * {@link TourEventId#TOUR_SELECTION} event.
    *
    * @param tourIds
    */
   private void fireTourSelection(final LinkedHashSet<Long> tourIds) {

      ISelection selection;
      if (tourIds.isEmpty()) {

         // fire selection that nothing is selected

         selection = new SelectionTourIds(new ArrayList<>());

      } else {

         // keep selected tour id's
         _selectedTourIds.clear();
         _selectedTourIds.addAll(tourIds);

         final Boolean tourLocationHoverState = getTourLocation_HoverState();

         final GeoPosition hoveredTourLocationPosition = tourLocationHoverState == null
               ? null
               : getTourLocation_Position(tourLocationHoverState, _selectedTourIds.get(0));

         if (tourIds.size() == 1) {

            final SelectionTourId selectionTourId = new SelectionTourId(_selectedTourIds.get(0));

            selectionTourId.setHoveredTourLocation(hoveredTourLocationPosition);

            selection = selectionTourId;

         } else {

            final SelectionTourIds selectionTourIds = new SelectionTourIds(_selectedTourIds);

            selectionTourIds.setHoveredTourLocation(hoveredTourLocationPosition);

            selection = selectionTourIds;
         }
      }

      _isInFireSelection = true;
      {
         // _postSelectionProvider should be removed when all parts are listening to the TourManager event
         if (_isInStartup) {

            _isInStartup = false;

            // this view can be inactive -> selection is not fired with the SelectionProvider interface

            TourManager.fireEventWithCustomData(TourEventId.TOUR_SELECTION, selection, this);

         } else {

            // fire selection and keep it in the provider that when this part is activated, it will fire the selection again
            _postSelectionProvider.setSelection(selection, false);
         }
      }
      _isInFireSelection = false;

      enableActions();
   }

   public ActionTourCollectionFilter getActionTourCollectionFilter() {

      return _actionTourCollectionFilter;
   }

   /**
    * Returns the {@link ColumnManager} of the currently selected nattable/table/tree
    */
   @Override
   public ColumnManager getColumnManager() {

      if (_isLayoutNatTable) {

         return _columnManager_NatTable;

      } else {

         return _columnManager_Tree;
      }
   }

   @Override
   public NatTable getNatTable() {
      return _tourViewer_NatTable;
   }

   public ColumnManager getNatTable_ColumnManager() {
      return _columnManager_NatTable;
   }

   public NatTable_DataLoader getNatTable_DataLoader() {
      return _natTable_DataLoader;
   }

   public TourRowDataProvider getNatTable_DataProvider() {
      return _natTable_DataProvider;
   }

   /**
    * @param event
    *
    * @return Returns the {@link ColumnDefinition} of the currently selected row or
    *         <code>null</code> when nothing is selected.
    */
   public ColumnDefinition getNatTable_SelectedColumnDefinition(final Event event) {

      final NatTable natTable = _tourViewer_NatTable;

      final int colPos = natTable.getColumnPositionByX(event.x);
      final int rowPos = natTable.getRowPositionByY(event.y);

      final ILayerCell cell = natTable.getCellByPosition(colPos, rowPos);
      if (cell != null) {

         final int colIndexByPos = natTable.getColumnIndexByPosition(colPos);
         if (colIndexByPos == -1) {

            // a column is not hit
            return null;
         }

         return _columnManager_NatTable.getActiveProfile().getVisibleColumnDefinitions().get(colIndexByPos);
      }

      return null;
   }

   @SuppressWarnings("unchecked")
   private RowSelectionModel<TVITourBookTour> getNatTable_SelectionModel() {

      return (RowSelectionModel<TVITourBookTour>) _natTable_Body_SelectionLayer.getSelectionModel();
   }

   @Override
   public ColumnHideShowLayer getNatTableLayer_ColumnHideShow() {
      return _natTable_Body_ColumnHideShowLayer;
   }

   @Override
   public ColumnReorderLayer getNatTableLayer_ColumnReorder() {
      return _natTable_Body_ColumnReorderLayer;
   }

   @Override
   public DataLayer getNatTableLayer_Data() {
      return _natTable_Body_DataLayer;
   }

   /**
    * @return the _natTable_Body_HoverLayer
    */
   public HoverLayer getNatTableLayer_Hover() {
      return _natTable_Body_HoverLayer;
   }

   public NatTable_SortModel getNatTableLayer_SortModel() {
      return _natTable_SortModel;
   }

   @Override
   public ViewportLayer getNatTableLayer_Viewport() {
      return _natTable_Body_ViewportLayer;
   }

   @Override
   public PostSelectionProvider getPostSelectionProvider() {
      return _postSelectionProvider;
   }

   @Override
   public Set<Long> getSelectedTourIDs() {

      final LinkedHashSet<Long> allTourIds = new LinkedHashSet<>();

      IStructuredSelection selectedTours;

      /*
       * Get selected items of any type from the view
       */
      if (_isLayoutNatTable) {

         // flat view

         final RowSelectionModel<TVITourBookTour> rowSelectionModel = getNatTable_SelectionModel();

         final List<TVITourBookTour> selectedTVITours = rowSelectionModel.getSelectedRowObjects();

         for (final TVITourBookTour tviTourBookTour : selectedTVITours) {
            allTourIds.add(tviTourBookTour.tourId);
         }

//         if (tourIds.isEmpty() && _hoveredTourId != -1) {
//
//            // when nothing is selected but mouse is hovering a tour, return this tour id
//
//            tourIds.add(_hoveredTourId);
//         }

      } else {

         // tree view

         selectedTours = _tourViewer_Tree.getStructuredSelection();

         /*
          * Convert selected items into selected tour id's
          */
         final boolean isSelectAllInHierarchy = _actionSelectAllTours.isChecked();

         for (final Object viewItem : selectedTours) {

            if (viewItem instanceof final TVITourBookYear tviTourBookYear) {

               // one year is selected

               if (isSelectAllInHierarchy) {

                  // loop: all months
                  for (final TreeViewerItem viewerItem : tviTourBookYear.getFetchedChildren()) {
                     if (viewerItem instanceof final TVITourBookYearCategorized tviTourBookYearCategorized) {
                        getYearSubTourIDs(tviTourBookYearCategorized, allTourIds);
                     }
                  }
               }

            } else if (viewItem instanceof final TVITourBookYearCategorized tviTourBookYearCategorized) {

               // one month/week is selected

               if (isSelectAllInHierarchy) {
                  getYearSubTourIDs(tviTourBookYearCategorized, allTourIds);
               }

            } else if (viewItem instanceof TVITourBookTour) {

               // one tour is selected

               allTourIds.add(((TVITourBookTour) viewItem).getTourId());
            }
         }
      }

      return allTourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      if (_pageBook.isDisposed()) {
         return null;
      }

      // get selected tour id's
      final Set<Long> tourIds = getSelectedTourIDs();

      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      BusyIndicator.showWhile(_pageBook.getDisplay(), () -> TourManager.loadTourData(new ArrayList<>(tourIds), selectedTourData, false));

      return selectedTourData;
   }

   String getSlideoutData_NumberOfAllTours() {

      final int numAllTours = _natTable_DataLoader.getNumberOfToursWithoutCollectionFilter();

      return Integer.toString(numAllTours);
   }

   String getSlideoutData_NumberOfSelectedTours() {

      return Integer.toString(_selectedTourIds.size());
   }

   TourCollectionFilter getSlideoutData_TourCollectionFilter() {
      return _tourCollectionFilter;
   }

   IDialogSettings getState() {
      return _state;
   }

   /**
    * @return Returns <code>true</code> when start location is hovered, <code>false</code> when end
    *         location is hovered, <code>null</code> when a location is not hovered
    */
   private Boolean getTourLocation_HoverState() {

      final NatTable natTable = getTourViewer_NatTable();

      final Point controlCursorLocation = natTable.toControl(Display.getCurrent().getCursorLocation());

      final int devXMouse = controlCursorLocation.x;
      final int devYMouse = controlCursorLocation.y;

      final int colPosByX = natTable.getColumnPositionByX(devXMouse);
      final int rowPosByY = natTable.getRowPositionByY(devYMouse);

      if (colPosByX <= 0 || rowPosByY <= 0) {

         // first column or first row (this is the row number or table header) or an empty nattable (rowPosByY == -1)

      } else {

         // get hovered label provider from the column, this is needed to show the tour tooltip only for specific columns
         final int hoveredColumnIndex = natTable.getColumnIndexByPosition(colPosByX);
         if (hoveredColumnIndex > -1) {

            final ArrayList<ColumnDefinition> visibleAndSortedColumns = _columnManager_NatTable.getVisibleAndSortedColumns();
            final ColumnDefinition colDef = visibleAndSortedColumns.get(hoveredColumnIndex);

            final NatTable_LabelProvider labelProvider = colDef.getNatTable_LabelProvider();
            if (labelProvider instanceof final NatTable_LabelProvider_WithLocationTooltip locationTooltipLabelProvider) {

               // return true or false
               return locationTooltipLabelProvider.isStartLocation;
            }
         }
      }

      return null;
   }

   private GeoPosition getTourLocation_Position(final boolean hoveredTourLocation, final Long tourID) {

      final TourData tourData = TourManager.getInstance().getTourData(tourID);

      if (tourData == null) {
         return null;
      }

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      if (latitudeSerie == null || latitudeSerie.length == 0) {
         return null;
      }

      if (hoveredTourLocation) {

         // start location is hovered

         return new GeoPosition(latitudeSerie[0], longitudeSerie[0]);

      } else {

         // end location is hovered

         final int lastIndex = latitudeSerie.length - 1;

         return new GeoPosition(latitudeSerie[lastIndex], longitudeSerie[lastIndex]);
      }
   }

   /**
    * @return the {@link #_tourViewer_NatTable}
    */
   public NatTable getTourViewer_NatTable() {
      return _tourViewer_NatTable;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourViewer_Tree;
   }

   @Override
   public ColumnViewer getViewer() {

      if (_isLayoutNatTable) {

         return _natTable_DummyColumnViewer;

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
    * @param allTourIds
    *
    * @return Return all tours for one yearSubItem
    */
   private void getYearSubTourIDs(final TVITourBookYearCategorized yearSubItem, final LinkedHashSet<Long> allTourIds) {

      // get all tours for the month item
      for (final TreeViewerItem viewerItem : yearSubItem.getFetchedChildren()) {
         if (viewerItem instanceof final TVITourBookTour tourItem) {

            allTourIds.add(tourItem.getTourId());
         }
      }
   }

   /**
    * Hide the tooltip when mouse not hovering it and the mouse have exited the view
    */
   private void hideTooltip() {

      _parent.getDisplay().timerExec(100, () -> {

         if (_tourLocationTooltip_Tree.isMouseHovered() == false) {
            _tourLocationTooltip_Tree.hide();
         }

         if (_tourLocationTooltip_NatTable.isMouseHovered() == false) {
            _tourLocationTooltip_NatTable.hide();
         }
      });
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   /**
    * @return Returns <code>true</code> when tourbook view displays the flat table, otherwise the
    *         tree.
    */
   public boolean isLayoutNatTable() {
      return _isLayoutNatTable;
   }

   boolean isShowSummaryRow() {

      return Util.getStateBoolean(_state, TourBookView.STATE_IS_SHOW_SUMMARY_ROW, TourBookView.STATE_IS_SHOW_SUMMARY_ROW_DEFAULT);
   }

   /**
    * Set the context menu position when it's opened with the keyboard.
    *
    * @param event
    */
   private void natTable_ContextMenu_OnMenuDetect(final Event event) {

      _natTable_ContextMenuActivator = event.detail;

      if (event.detail == SWT.MENU_MOUSE) {

         // ignore mouse event

         return;
      }

      // position context menu to the selected tour

      final RowSelectionModel<TVITourBookTour> rowSelectionModel = getNatTable_SelectionModel();
      final Set<Range> allSelectedRowPositions = rowSelectionModel.getSelectedRowPositions();

      final int numSelectedRows = allSelectedRowPositions.size();

      if (numSelectedRows == 0) {
         return;
      }

      final NatTable natTable = _tourViewer_NatTable;

      final Range[] allRanges = allSelectedRowPositions.toArray(new Range[numSelectedRows]);
      final Range lastRange = allRanges[numSelectedRows - 1];

      final int lastRow = lastRange.end;

      final int lastRowIndex = _natTable_Body_ViewportLayer.getRowPositionByIndex(lastRow);
      final int devY_LastRowIndex = _natTable_Body_ViewportLayer.getStartYOfRowPosition(lastRowIndex);

      final PositionCoordinate selectionAnchor = _natTable_Body_SelectionLayer.getSelectionAnchor();
      final int anchorColumnPosition = selectionAnchor.columnPosition;

      final int devX_Anchor = _natTable_Body_ViewportLayer.getStartXOfColumnPosition(anchorColumnPosition);
      final int anchorColumnWidth = _natTable_Body_ViewportLayer.getColumnWidthByPosition(anchorColumnPosition);

// this do not have the expected position
//      final int devY_Anchor = _natTable_Body_ViewportLayer.getStartYOfRowPosition(selectionAnchor.rowPosition);
//      final Point displayPosition = natTable.toDisplay(devX_Anchor, devY_Anchor);

      final Point displayPosition = natTable.toDisplay(devX_Anchor, devY_LastRowIndex);

      /*
       * TODO Have no idea why horizontally it must be adjusted by 40 to be to the right of the
       * anchor cell ?-(
       */
      final int xOffset = 40;

      // micro adjust position to show exactly on the header lines otherwise it looks ugly
      event.x = displayPosition.x + anchorColumnWidth + xOffset;
      event.y = displayPosition.y - 0;
   }

   private void natTable_ContextMenu_OnShow(final IMenuManager manager) {

      final Set<Range> allSelectedRowPositions = getNatTable_SelectionModel().getSelectedRowPositions();

      final int numSelectedRows = allSelectedRowPositions.size();

      switch (_natTable_ContextMenuActivator) {

      case SWT.MENU_KEYBOARD:

         if (numSelectedRows == 0) {
            return;
         }

         fillContextMenu(manager, false);

         break;

      case SWT.MENU_MOUSE:

         int hoveredRow = -1;

         final Point hoveredPosition = _natTable_Body_HoverLayer.getCurrentHoveredCellPosition();
         if (hoveredPosition != null) {

            hoveredRow = hoveredPosition.y;

            for (final Range range : allSelectedRowPositions) {

               if (range.contains(hoveredRow)) {

                  // tour selection is hovered

                  fillContextMenu(manager, false);
                  return;
               }
            }
         }

         if (hoveredRow == -1) {

            // nothing is hovered, this should not occur because when a tour is selected it's row is set to be also hovered

            return;
         }

         // mouse is not hovering a tour selection -> select tour

         selectTours_NatTable(new int[] { hoveredRow }, true, false, false);

         // show context menu again
         _pageBook.getDisplay().timerExec(10, () -> {

//            final Set<Long> allSelectedRowPositions2 = getSelectedTourIDs();
//
//            final int numSelectedRows2 = allSelectedRowPositions2.size();

            UI.openContextMenu(_tourViewer_NatTable);
         });

         fillContextMenu(manager, false);

         break;
      }
   }

   /**
    * Column header is clicked to sort table by this column
    *
    * @param listener
    */
   private void natTable_OnColumnSort(final ILayerEvent listener) {

      if (listener instanceof SortColumnEvent) {

         // move selected tour into view

         _pageBook.getDisplay().timerExec(1, () -> natTable_ScrollSelectedToursIntoView());
      }
   }

   /**
    * Register column labels for the body and header -> this is necessary to apply styling, images,
    * ...
    *
    * @param allSortedColumns
    * @param body_DataLayer
    * @param columnHeader_DataLayer
    */
   private void natTable_RegisterColumnLabels(final List<ColumnDefinition> allSortedColumns,
                                              final DataLayer body_DataLayer,
                                              final DataLayer columnHeader_DataLayer) {

      final ColumnOverrideLabelAccumulator body_ColumnLabelAccumulator = new ColumnOverrideLabelAccumulator(body_DataLayer);
      final ColumnOverrideLabelAccumulator columnHeader_ColumnLabelAccumulator = new ColumnOverrideLabelAccumulator(columnHeader_DataLayer);

      body_DataLayer.setConfigLabelAccumulator(body_ColumnLabelAccumulator);
      columnHeader_DataLayer.setConfigLabelAccumulator(columnHeader_ColumnLabelAccumulator);

      for (int colIndex = 0; colIndex < allSortedColumns.size(); colIndex++) {

         final ColumnDefinition colDef = allSortedColumns.get(colIndex);
         final String columnId = colDef.getColumnId();

         columnHeader_ColumnLabelAccumulator.registerColumnOverrides(colIndex, columnId + HEADER_COLUMN_ID_POSTFIX);
         body_ColumnLabelAccumulator.registerColumnOverrides(colIndex, columnId);
      }
   }

   private void natTable_ScrollSelectedToursIntoView() {

      _natTable_DataLoader.getRowIndexFromTourId(_selectedTourIds).thenAccept(allRowPositions -> {

         if (allRowPositions.length < 1) {

            // fixed ArrayIndexOutOfBoundsException

            return;
         }

         final int firstRowPosition = allRowPositions[0];
         final int numVisibleRows = _natTable_Body_ViewportLayer.getRowCount();
         final int scrollableRowCenterPosition = numVisibleRows / 2;

//         final ArrayList<SortDirectionEnum> sortDirection = _natTable_DataLoader.getSortDirections();
//         final String[] sortColumnId = _natTable_DataLoader.getSortColumnIds();

//         System.out.println((System.currentTimeMillis() + " " + sortColumnId));
//         // TODO remove SYSTEM.OUT.PRINTLN

         /*
          * TODO Have no idea why this is necessary: needs an offset to make row visible, otherwise
          * it is hidden, depending on the sort direction and column :-?
          */
         final int rowOffset = 0;
//         switch (sortColumnId) {
//
//         case TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP_ID:
//         case TableColumnFactory.TIME_DATE_ID:
//         case TableColumnFactory.MOTION_DISTANCE_ID:
//         case TableColumnFactory.MOTION_MAX_SPEED_ID:
//
//            rowOffset = sortDirection.equals(SortDirectionEnum.DESC) ? -numVisibleRows : 0;
//            break;
//
//         case TableColumnFactory.TOUR_TITLE_ID:
//         case TableColumnFactory.DEVICE_NAME_ID:
//
//            rowOffset = sortDirection.equals(SortDirectionEnum.ASC) ? -numVisibleRows : 0;
//            break;
//         }

         final int rowVerticalCenterPosition = firstRowPosition + scrollableRowCenterPosition + rowOffset;

         _natTable_Body_ViewportLayer.moveRowPositionIntoViewport(rowVerticalCenterPosition);
      });
   }

   /**
    * @param allSortedColumns
    * @param body_DataLayer
    */
   private void natTable_SetColumnWidths(final List<ColumnDefinition> allSortedColumns, final DataLayer body_DataLayer) {

      // set column widths
      for (int colIndex = 0; colIndex < allSortedColumns.size(); colIndex++) {

         final ColumnDefinition colDef = allSortedColumns.get(colIndex);

         int columnWidth = colDef.getColumnWidth();

         if (columnWidth < 0) {

            // this case happened in https://sourceforge.net/p/mytourbook/discussion/622811/thread/31aa349fc5/?limit=25

            // java.lang.IllegalArgumentException: size < 0
            // at org.eclipse.nebula.widgets.nattable.layer.SizeConfig.setSize(SizeConfig.java:292)
            // at org.eclipse.nebula.widgets.nattable.layer.DataLayer.setColumnWidthByPosition(DataLayer.java:272)
            // at net.tourbook.ui.views.tourBook.TourBookView.natTable_SetColumnWidths(TourBookView.java:2331)
            // at net.tourbook.ui.views.tourBook.TourBookView.createUI_20_NatTable_TourViewer(TourBookView.java:1344)
            // at net.tourbook.ui.views.tourBook.TourBookView.createUI(TourBookView.java:1223)
            // at net.tourbook.ui.views.tourBook.TourBookView.createPartControl(TourBookView.java:1182)

            // set default column width
            columnWidth = 50;
         }

         body_DataLayer.setColumnWidthByPosition(colIndex, columnWidth, false);
      }
   }

   /**
    * Set flag in all {@link ColumnDefinition}s if the column can be sorted or not.
    */
   private void natTable_SetupColumnSorting() {

      for (final ColumnDefinition colDef : _columnManager_NatTable.getRearrangedColumns()) {

         final String sqlField = _natTable_DataLoader.getSqlField_OrderBy(colDef.getColumnId());

         final boolean canSortColumn = NatTable_DataLoader.FIELD_WITHOUT_SORTING.equals(sqlField) == false;
         colDef.setCanSortColumn(canSortColumn);
      }
   }

   private void onPaint_TreeViewer(final Event event) {

      // paint images at the correct column

      final int columnIndex = event.index;

      if (columnIndex == _columnIndex_TourTypeImage) {

         onPaint_TreeViewer_TourTypeImage(event);

      } else if (columnIndex == _columnIndex_WeatherClouds) {

         onPaint_TreeViewer_WeatherClouds(event);
      }
   }

   private void onPaint_TreeViewer_TourTypeImage(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof TVITourBookTour) {

         final TVITourBookTour tviTourBookTour = (TVITourBookTour) itemData;
         final long tourTypeId = tviTourBookTour.getTourTypeId();

         final Image image = TourTypeImage.getTourTypeImage(tourTypeId);
         if (image != null) {

            UI.paintImage(event,
                  image,
                  _columnWidth_TourTypeImage,
                  _colDef_TourTypeImage_Tree.getColumnStyle(), // horizontal alignment
                  TourTypeImage.getHorizontalOffset());
         }
      }
   }

   private void onPaint_TreeViewer_WeatherClouds(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof TVITourBookTour) {

         final TVITourBookTour tviTourBookTour = (TVITourBookTour) itemData;

         final String weatherClouds = tviTourBookTour.colClouds;

         if (weatherClouds == null) {

            // paint nothing

         } else {

            final Image image = UI.IMAGE_REGISTRY.get(weatherClouds);

            if (image == null) {

               // paint text left aligned

               event.gc.drawText(weatherClouds, event.x, event.y, false);

            } else {

               final int alignment = _colDef_WeatherClouds_Tree.getColumnStyle();

               UI.paintImage(event, image, _columnWidth_WeatherClouds, alignment, 0);
            }
         }
      }
   }

   private void onResize_SetWidthForImageColumn() {

      if (_colDef_TourTypeImage_Tree != null) {

         final TreeColumn treeColumn = _colDef_TourTypeImage_Tree.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_TourTypeImage = treeColumn.getWidth();
         }
      }

      if (_colDef_WeatherClouds_Tree != null) {

         final TreeColumn treeColumn = _colDef_WeatherClouds_Tree.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_WeatherClouds = treeColumn.getWidth();
         }
      }
   }

   private void onSelect_CategoryItem(final TreeSelection treeSelection) {

      if (_isInExpandingSelection) {

         // prevent endless loops

         return;
      }

      /*
       * Ensure that the selection is valid
       */
      final TreePath[] selectedTreePaths = treeSelection.getPaths();
      if (selectedTreePaths.length == 0) {
         return;
      }

      final TreePath selectedTreePath = selectedTreePaths[0];
      if (selectedTreePath == null) {
         return;
      }

      onSelect_CategoryItem_10_AutoExpandCollapse(treeSelection);
   }

   /**
    * This is not yet working thoroughly because the expanded position moves up or down and all
    * expanded children are not visible (but they could) like when the triangle (+/-) icon in the
    * tree is clicked.
    *
    * @param treeSelection
    * @param selectedTreePath
    */
   private void onSelect_CategoryItem_10_AutoExpandCollapse(final ITreeSelection treeSelection) {

      if (_isInCollapseAll) {

         // prevent auto expand

         return;
      }

      if (_isBehaviour_SingleExpand_CollapseOthers) {

         /*
          * Run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         _parent.getDisplay().asyncExec(new Runnable() {

            private long           __expandRunnableCounter = ++_expandRunnableCounter;

            private ITreeSelection __treeSelection         = treeSelection;

            @Override
            public void run() {

               // check if a newer expand event occurred
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               if (_tourViewer_Tree.getTree().isDisposed()) {
                  return;
               }

               /*
                * With Linux the selection event is fired twice when a subcategory, e.g. month is
                * selected which causes an endless loop !!!
                */
               final long now = System.currentTimeMillis();
               final long timeDiff = now - _lastExpandSelectionTime;
               if (timeDiff < 200) {
                  return;
               }

               onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(__treeSelection);
            }
         });

      } else {

         if (_isBehaviour_OnSelect_ExpandCollapse) {

            // expand folder with one mouse click but not with the keyboard

            final TreePath selectedTreePath = treeSelection.getPaths()[0];

            expandCollapseItem((TreeViewerItem) selectedTreePath.getLastSegment());
         }
      }
   }

   /**
    * This behavior is complex and still have possible problems.
    *
    * @param treeSelection
    * @param selectedTreePath
    */
   private void onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(final ITreeSelection treeSelection) {

      /*
       * Create expanded elements from the tree selection
       */
      final TreePath selectedTreePath = treeSelection.getPaths()[0];
      final int numSegments = selectedTreePath.getSegmentCount();

      final Object[] expandedElements = new Object[numSegments];

      for (int segmentIndex = 0; segmentIndex < numSegments; segmentIndex++) {
         expandedElements[segmentIndex] = selectedTreePath.getSegment(segmentIndex);
      }

      _isInExpandingSelection = true;
      {
         final Tree tree = _tourViewer_Tree.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _tourViewer_Tree.getExpandedState(selectedTreePath);

            /*
             * Collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _tourViewer_Tree.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _tourViewer_Tree.setExpandedState(treePath, false);
            }

            /*
             * Expand and select selected folder
             */
            _tourViewer_Tree.setExpandedElements(expandedElements);
            _tourViewer_Tree.setSelection(treeSelection, true);

            if (_isBehaviour_OnSelect_ExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _tourViewer_Tree.setExpandedState(selectedTreePath, false);
            }

            /**
             * Set top item to the previous top item, otherwise the expanded/collapse item is
             * positioned at the bottom and the UI is jumping all the time
             * <p>
             * Win behaviour: When an item is set to top which was collapsed before, it will be
             * expanded
             */
            if (topItem.isDisposed() == false) {
               tree.setTopItem(topItem);
            }
         }
         tree.setRedraw(true);
      }
      _isInExpandingSelection = false;
      _lastExpandSelectionTime = System.currentTimeMillis();
   }

   private void onSelect_NatTableItem(final SelectionChangedEvent event) {

      if (_isInSelection) {
         return;
      }

      final LinkedHashSet<Long> tourIds = new LinkedHashSet<>();
      final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

      for (final Object selectedItem : selection) {
         tourIds.add(((TVITourBookTour) selectedItem).tourId);
      }

      /*
       * Set hovered tour to a selected tour otherwise the context menu is not opened as it requires
       * a hovered tour.
       * This sounds strange but after many try and error tests, this was the only "simple"
       * solution which I've found.
       * It seems that the context menu for the NatTable can not be implemented as easy as for a SWT
       * table or tree.
       */

      final Set<Range> allSelectedRowPositions = getNatTable_SelectionModel().getSelectedRowPositions();
      final IntArrayList allSelectedRowPos = new IntArrayList();

      // convert all ranges into a list
      for (final Range rowRange : allSelectedRowPositions) {
         for (final Integer rowPos : rowRange.getMembers()) {
            allSelectedRowPos.add(rowPos);
         }
      }

      if (allSelectedRowPos.size() > 0) {

         final int[] allRowPositions = allSelectedRowPos.toArray();
         boolean isSelectedTourHovered = false;

         final Point hoveredPosition = _natTable_Body_HoverLayer.getCurrentHoveredCellPosition();
         if (hoveredPosition != null) {

            final int hoveredRow = hoveredPosition.y;

            for (final int rowPosition : allRowPositions) {

               if (rowPosition == hoveredRow) {

                  // tour selection is also hovered

                  isSelectedTourHovered = true;

                  break;
               }
            }
         }

         if (isSelectedTourHovered == false) {

            // a selected tour is NOT hovered -> set one tour also to be hovered

            final PositionCoordinate selectionAnchor = _natTable_Body_SelectionLayer.getSelectionAnchor();

            try {

               _natTable_Body_HoverLayer.setCurrentHoveredCellPosition(
                     selectionAnchor.columnPosition,
                     allRowPositions[0]);

            } catch (final Exception e) {

               // ignore, sometimes this happens

               /**
                * <code>
                *
                * java.lang.NullPointerException: Cannot read field "x" because "this.currentHoveredCellPosition" is null
                * 	at org.eclipse.nebula.widgets.nattable.hover.HoverLayer.setCurrentHoveredCellPosition(HoverLayer.java:191)
                * 	at org.eclipse.nebula.widgets.nattable.hover.HoverLayer.setCurrentHoveredCellPosition(HoverLayer.java:146)
                * 	at net.tourbook.ui.views.tourBook.TourBookView.onSelect_NatTableItem(TourBookView.java:3507)
                * 	at net.tourbook.ui.views.tourBook.TourBookView.lambda$10(TourBookView.java:1859)
                *
                * </code>
                */
            }
         }
      }

      fireTourSelection(tourIds);
   }

   private void onSelect_TreeItem(final SelectionChangedEvent event) {

      if (_isInSelection) {
         return;
      }

      final boolean isSelectAllChildren = _actionSelectAllTours.isChecked();

      final LinkedHashSet<Long> allTourIds = new LinkedHashSet<>();

      boolean isFirstYear = true;
      boolean isFirstYearSub = true;
      boolean isFirstTour = true;

      TVITourBookItem selectedCategoryItem = null;

      final TreeSelection treeSelection = (TreeSelection) event.getSelection();

      // loop: all selected items
      for (final Object treeItem : treeSelection) {

         if (isSelectAllChildren) {

            // get ALL tours from all selected tree items (year/month/tour)

            if (treeItem instanceof final TVITourBookYear yearItem) {

               // year is selected

               if (isFirstYear) {

                  isFirstYear = false;

                  // keep selected year
                  _selectedYear = yearItem.tourYear;

                  selectedCategoryItem = yearItem;
               }

               // get all tours for the selected year
               for (final TreeViewerItem viewerItem : yearItem.getFetchedChildren()) {

                  if (viewerItem instanceof final TVITourBookYearCategorized yearSubItem) {

                     getYearSubTourIDs(yearSubItem, allTourIds);
                  }
               }

            } else if (treeItem instanceof final TVITourBookYearCategorized yearSubItem) {

               // month/week is selected

               if (isFirstYearSub) {

                  isFirstYearSub = false;

                  // keep selected year/month/week/day
                  _selectedYear = yearSubItem.tourYear;
                  _selectedYearSub = yearSubItem.tourYearSub;

                  selectedCategoryItem = yearSubItem;
               }

               // get all tours for the selected month
               getYearSubTourIDs(yearSubItem, allTourIds);

            } else if (treeItem instanceof final TVITourBookTour tourItem) {

               // tour is selected

               if (isFirstTour) {

                  isFirstTour = false;

                  // keep selected tour
                  _selectedYear = tourItem.tourYear;
                  _selectedYearSub = tourItem.tourYearSub;
               }

               allTourIds.add(tourItem.getTourId());
            }

         } else {

            // get only selected tours
            if (treeItem instanceof final TVITourBookYear yearItem) {

               // year is selected

               if (isFirstYear) {

                  isFirstYear = false;

                  selectedCategoryItem = yearItem;
               }

            } else if (treeItem instanceof final TVITourBookYearCategorized yearSubItem) {

               // month/week is selected

               if (isFirstYearSub) {

                  isFirstYearSub = false;

                  selectedCategoryItem = yearSubItem;
               }

            } else if (treeItem instanceof final TVITourBookTour tourItem) {

               if (isFirstTour) {

                  isFirstTour = false;

                  // keep selected tour
                  _selectedYear = tourItem.tourYear;
                  _selectedYearSub = tourItem.tourYearSub;
               }

               allTourIds.add(tourItem.getTourId());
            }
         }
      }

      fireTourSelection(allTourIds);

      if (selectedCategoryItem != null

            // a category is selected, expand/collapse category items

            // do not expand/collapse when keyboard is used -> unusable
            && _isSelectedWithKeyboard == false) {

         onSelect_CategoryItem(treeSelection);
      }

      // reset state
      _isSelectedWithKeyboard = false;
   }

   private void onSelectionChanged(final ISelection selection, final IWorkbenchPart part) {

      if (_isInFireSelection) {
         return;
      }

      // show and select the selected tour
      if (selection instanceof final SelectionTourId selectionTourId) {

         final long tourId = selectionTourId.getTourId();

         boolean isSelectTours = true;

         if (_isLayoutNatTable) {

            if (_actionLinkWithOtherViews.getSelection()) {

               // linking is enabled

               if (part instanceof final SearchView searchView) {

                  if (searchView.isPushSearchResult()) {

                     /*
                      * Disable tour selection otherwise the pushed search result is overwritten
                      */
                     isSelectTours = false;

                     /*
                      * Disable link with other views, this do not work when push is enabled
                      */
                     _actionLinkWithOtherViews.setSelection(false);
                  }
               }

               if (isSelectTours) {

                  _selectedTourIds.clear();
                  _selectedTourIds.add(tourId);

                  reselectTourViewer(false);
               }
            }

         } else {

            selectTour(tourId);
         }

      } else if (selection instanceof final SelectionTourIds selectionTourIds) {

         _selectedTourIds.clear();
         _selectedTourIds.addAll(selectionTourIds.getTourIds());

         reselectTourViewer(false);

      } else if (selection instanceof final StructuredSelection structuredSelection) {

         final Object firstElement = structuredSelection.getFirstElement();

         if (firstElement instanceof final GeoComparedTour comparerItem) {

            // show selected compared tour

            selectTour(comparerItem.tourId);

         } else if (firstElement instanceof final TVIRefTour_ComparedTour comparedTour) {

            selectTour(comparedTour.getTourId());
         }

      } else if (selection instanceof SelectionDeletedTours) {

         reloadViewer();
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      if (_isLayoutNatTable) {

         return recreateViewer_NatTable();

      } else {

         return recreateViewer_Tree();
      }
   }

   private ColumnViewer recreateViewer_NatTable() {

      final RowSelectionModel<TVITourBookTour> selectionModel = getNatTable_SelectionModel();
      final int[] allRowPositions = selectionModel.getFullySelectedRowPositions(0);

      // maybe prevent memory leaks
      _natTable_DataLoader.resetTourItems(true);

      _viewerContainer_NatTable.setRedraw(false);
      {
         _tourViewer_NatTable.dispose();

         createUI_20_NatTable_TourViewer(_viewerContainer_NatTable);

         _viewerContainer_NatTable.layout(true, true);

         setupTourViewerContent();

      }
      _viewerContainer_NatTable.setRedraw(true);

      selectTours_NatTable(allRowPositions, true, true, false);

      return null;
   }

   private ColumnViewer recreateViewer_Tree() {

      _viewerContainer_Tree.setRedraw(false);
      {
         final Object[] expandedElements = _tourViewer_Tree.getExpandedElements();
         final ISelection selection = _tourViewer_Tree.getSelection();

         _tourViewer_Tree.getTree().dispose();

         createUI_30_Tree_TourViewer(_viewerContainer_Tree);
         _viewerContainer_Tree.layout();

         setupTourViewerContent();

         _tourViewer_Tree.setExpandedElements(expandedElements);
         _tourViewer_Tree.setSelection(selection);
      }
      _viewerContainer_Tree.setRedraw(true);

      return _tourViewer_Tree;
   }

   @Override
   public void reloadViewer() {

      if (_isInSelection) {
         return;
      }

      _natTable_DataLoader.resetTourItems(true);

      if (_isLayoutNatTable) {

         _tourViewer_NatTable.setRedraw(false);
         _isInSelection = true;
         {
            setupTourViewerContent();
         }
         _isInSelection = false;
         _tourViewer_NatTable.setRedraw(true);

         /*
          * !!! Setting the focus is disabled because it would activate this view which is annoying
          * when another view has the focus !!!
          */
         reselectTourViewer(false);

      } else {

         final Tree tree = _tourViewer_Tree.getTree();
         tree.setRedraw(false);
         _isInSelection = true;
         {
            final Object[] expandedElements = _tourViewer_Tree.getExpandedElements();
            final ISelection selection = _tourViewer_Tree.getSelection();

            setupTourViewerContent();

            _tourViewer_Tree.setExpandedElements(expandedElements);
            _tourViewer_Tree.setSelection(selection, true);
         }
         _isInSelection = false;
         tree.setRedraw(true);
      }
   }

   private void reopenFirstSelectedTour() {

      if (_isLayoutNatTable) {

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

   /**
    * Reselect tours from {@link #_selectedTourIds}
    */
   private void reselectTourViewer() {

      reselectTourViewer(true);
   }

   /**
    * Reselect tours from {@link #_selectedTourIds}
    *
    * @param isFireSelection
    * @param isSetFocus
    */
   private void reselectTourViewer(final boolean isFireSelection) {

      _postSelectionProvider.clearSelection();

      if (_isLayoutNatTable) {

         reselectTourViewer_NatTable(isFireSelection);

      } else {

         reselectTourViewer_Tree();
      }
   }

   /**
    * Reselect tours from {@link #_selectedTourIds}
    *
    * @param isFireSelection
    * @param isSetFocus
    */
   private void reselectTourViewer_NatTable(final boolean isFireSelection) {

      final boolean isFilterActive = _actionTourCollectionFilter.getSelection();

      if (isFilterActive) {

         // filter tour data

         updateUI_NatTable(_tourCollectionFilter);

      } else {

         _natTable_DataLoader.getRowIndexFromTourId(_selectedTourIds).thenAccept(allRowPositions -> {

            selectTours_NatTable(allRowPositions,

                  true, // isClearSelection
                  true, // isScrollIntoView
                  isFireSelection);
         });
      }
   }

   private void reselectTourViewer_Tree() {

      // find the old selected year/[month/week] in the new tour items
      TreeViewerItem reselectYearItem = null;
      TreeViewerItem reselectYearSubItem = null;
      final ArrayList<TreeViewerItem> reselectTourItems = new ArrayList<>();

      /*
       * get the year/month/tour item in the data model
       */
      final ArrayList<TreeViewerItem> rootItems = _rootItem_Tree.getChildren();

      for (final TreeViewerItem rootItem : rootItems) {

         if (rootItem instanceof final TVITourBookYear tourBookYear) {

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

      } else if (rootItems.size() > 0) {

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

      // on mouse select -> expand/collapse
      _isBehaviour_OnSelect_ExpandCollapse = Util.getStateBoolean(_state, STATE_IS_ON_SELECT_EXPAND_COLLAPSE, true);
      _actionContext_OnMouseSelect_ExpandCollapse.setChecked(_isBehaviour_OnSelect_ExpandCollapse);

      // single expand -> collapse others
      _isBehaviour_SingleExpand_CollapseOthers = Util.getStateBoolean(_state, STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, true);
      _actionContext_SingleExpand_CollapseOthers.setChecked(_isBehaviour_SingleExpand_CollapseOthers);

      /*
       * Tour collection filter
       */
      _tourCollectionFilter = (TourCollectionFilter) Util.getStateEnum(_state,
            STATE_TOUR_COLLECTION_FILTER,
            TourCollectionFilter.ALL_TOURS);

      /*
       * View layout
       */
      _viewLayout = (TourBookViewLayout) Util.getStateEnum(_state, STATE_VIEW_LAYOUT, TourBookViewLayout.CATEGORY_MONTH);

      String viewLayoutImage = null;

      switch (_viewLayout) {
      case NAT_TABLE:
         viewLayoutImage = Images.TourBook_NatTable;
         _isLayoutNatTable = true;
         break;

      case CATEGORY_MONTH:
         viewLayoutImage = Images.TourBook_Month;
         _isLayoutNatTable = false;
         break;

      case CATEGORY_WEEK:
         viewLayoutImage = Images.TourBook_Week;
         _isLayoutNatTable = false;
         break;
      }

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(viewLayoutImage));

      /*
       * View options
       */
      _columnFactory.setIsShowSummaryRow(isShowSummaryRow());
      _columnFactory.updateToolTipState();

      /*
       * NatTable
       */
      // restore frozen columns
      final String frozenColumnId = _columnManager_NatTable.getActiveProfile().getFrozenColumnId();
      if (frozenColumnId != null) {

         for (final ColumnDefinition colDef : _columnManager_NatTable.getVisibleAndSortedColumns()) {

            if (frozenColumnId.equals(colDef.getColumnId())) {

               _columnManager_NatTable.action_FreezeColumn(colDef);
               break;
            }
         }
      }
   }

   private void restoreState_AfterUI() {

      /*
       * This must be selected lately otherwise the selection state is set but is not visible
       * (button is not pressed). Could not figure out why this occurs after debugging this issue
       */
      _actionLinkWithOtherViews.setSelection(_state.getBoolean(STATE_IS_LINK_WITH_OTHER_VIEWS));

      _actionTourCollectionFilter.setSelection(_state.getBoolean(STATE_IS_SELECTED_TOUR_COLLECTION_FILTER));
      updateUI_TourCollectionFilterIcons(_tourCollectionFilter);
   }

   private void restoreState_SortColumns() {

      final String[] columnIdDefaultValues = new String[] { TableColumnFactory.TIME_DATE_ID };
      final ArrayList<SortDirectionEnum> directionDefaultValues = new ArrayList<>();
      directionDefaultValues.add(SortDirectionEnum.DESC);

      String[] allSortColumnIds = Util.getStateStringArray(_state, STATE_SORT_COLUMN_ID, columnIdDefaultValues);
      final ArrayList<SortDirectionEnum> allSortDirections = Util.getStateEnumList(_state, STATE_SORT_COLUMN_DIRECTION, directionDefaultValues);

      // validate values, must have the same number of items
      if (allSortColumnIds.length != allSortDirections.size()) {

         // data are invalid -> cleanup

         allSortColumnIds = new String[0];
         allSortDirections.clear();
      }

      // setup model
      _natTable_SortModel.setupSortColumns(allSortColumnIds, allSortDirections);

      // setup data loader
      _natTable_DataLoader.setupSortColumns(allSortColumnIds, allSortDirections);
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

      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionContext_SingleExpand_CollapseOthers.isChecked());
      _state.put(STATE_IS_ON_SELECT_EXPAND_COLLAPSE, _actionContext_OnMouseSelect_ExpandCollapse.isChecked());

      _state.put(STATE_VIEW_LAYOUT, _viewLayout.name());

      // sort columns
      _state.put(STATE_SORT_COLUMN_ID, _natTable_DataLoader.getSortColumnIds());
      Util.setStateEnum(_state, STATE_SORT_COLUMN_DIRECTION, _natTable_DataLoader.getSortDirections());

      // tour collection filter
      _state.put(STATE_IS_SELECTED_TOUR_COLLECTION_FILTER, _actionTourCollectionFilter.getSelection());
      Util.setStateEnum(_state, STATE_TOUR_COLLECTION_FILTER, _tourCollectionFilter);

      _columnManager_Tree.saveState(_state_Tree);

      _columnManager_NatTable.saveState(
            _state_NatTable,
            _natTable_Body_DataLayer,
            _natTable_Body_ColumnReorderLayer,
            _natTable_Body_ColumnHideShowLayer);
   }

   private void selectTour(final long tourId) {

      if (_isLayoutNatTable) {

         // for performance reasons a tour cannot be selected by it's ID only by table index
         // TODO: get table index for a tour from db

         return;
      }

      // check if enabled
      if (_actionLinkWithOtherViews.getSelection() == false) {

         // linking is disabled

         return;
      }

      // check with old id
      final long oldTourId = _selectedTourIds.size() == 1
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

      // run async otherwise an internal NPE occurs
      _parent.getDisplay().asyncExec(() -> {

         final Tree tree = _tourViewer_Tree.getTree();

         if (tree.isDisposed()) {
            return;
         }

         tree.setRedraw(false);
         _isInSelection = true;
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

                  // this occurs sometimes but it seems that it's an eclipse internal problem
                  StatusUtil.log("This is a known issue when a treeviewer do a collapseAll()", e); //$NON-NLS-1$
               }
            }

            reselectTourViewer();
         }
         _isInSelection = false;
         tree.setRedraw(true);
      });
   }

   /**
    * Select tours (rows) in the NatTable by it's row positions, the selection is delayed that tours
    * are loaded ahead.
    *
    * @param allRowPositions
    * @param isClearSelection
    *           When <code>true</code> then only the provided rows will be selected, otherwise the
    *           provided tours will be added to the existing selection.
    * @param isScrollIntoView
    * @param isFireSelection
    * @param isSetFocus
    *           When <code>true</code> then the focus is set to the NatTable
    */
   void selectTours_NatTable(final int[] allRowPositions,
                             final boolean isClearSelection,
                             final boolean isScrollIntoView,
                             final boolean isFireSelection) {

      // ensure there is something to be selected
      if (allRowPositions == null || allRowPositions.length == 0 || allRowPositions[0] == -1) {
         return;
      }

      _parent.getDisplay().asyncExec(() -> {

         if (_parent.isDisposed()) {
            return;
         }

         /*
          * Prevent that _tourViewer_NatTable.setFocus() is firing a part selection which would
          * case the 2D map crumb to show the last part selection
          */
         _postSelectionProvider.clearSelection();

         // sort rows ascending
         Arrays.sort(allRowPositions);

         final int firstRowPosition = allRowPositions[0];

         /*
          * It took me hours to solve this issue, first deselect the old selection otherwise it
          * was PRESERVED :-(((
          */
         if (isClearSelection) {
            _natTable_Body_SelectionLayer.clear(false);
         }

         final SelectRowsCommand command = new SelectRowsCommand(
               _natTable_Body_SelectionLayer,
               0,
               allRowPositions,
               false,
               true,
               firstRowPosition);

         final boolean isPreventSelection = isFireSelection == false;

         if (isPreventSelection) {
            _isInSelection = true;
         }
         {
            _natTable_Body_SelectionLayer.doCommand(command);
         }
         if (isPreventSelection) {
            _isInSelection = false;
         }

         if (isScrollIntoView) {

            // show first selected row in the vertical middle, TODO# sometimes it is the top row

            final int numVisibleRows = _natTable_Body_ViewportLayer.getRowCount();
            final int scrollableRowCenterPosition = numVisibleRows / 2;
            final int rowVerticalCenterPosition = firstRowPosition + scrollableRowCenterPosition;

            _natTable_Body_ViewportLayer.moveRowPositionIntoViewport(rowVerticalCenterPosition);
         }
      });
   }

   void setActionDeleteTour(final ActionDeleteTour actionDeleteTour) {

      _actionDeleteTour = actionDeleteTour;
   }

   public void setActiveYear(final int activeYear) {
      _selectedYear = activeYear;
   }

   private void setCollectionFilterFromFulltextSearch(final ArrayList<Long> allTourIDs) {

      _tourCollectionFilter = TourCollectionFilter.COLLECTED_TOURS;

      _selectedTourIds.clear();
      _selectedTourIds.addAll(allTourIDs);

      // run in UI thread
      _parent.getDisplay().asyncExec(() -> {

         // disable sync action
         _actionLinkWithOtherViews.setSelection(false);

         final boolean isFilterActive = _actionTourCollectionFilter.getSelection();

         if (isFilterActive == false) {

            // filter is not active -> activate it

            _actionTourCollectionFilter.setSelection(true);
         }

         // update UI
         updateUI_TourCollectionFilterIcons(_tourCollectionFilter);
         updateUI_NatTable(_tourCollectionFilter);
      });
   }

   @Override
   public void setFocus() {

      if (_isLayoutNatTable) {

         if (_tourViewer_NatTable.isDisposed()) {
            return;
         }

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

      if (_isLayoutNatTable) {

         _tourViewer_NatTable.refresh();

         _pageBook.showPage(_viewerContainer_NatTable);

      } else {

         if (_rootItem_Tree != null) {
            _rootItem_Tree.clearChildren();
         }

         _rootItem_Tree = new TVITourBookRoot(this);

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

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourBook_Month));

      _isLayoutNatTable = false;
   }

   private void toggleLayout_Category_Week() {

      _viewLayout = TourBookViewLayout.CATEGORY_WEEK;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourBook_Week));

      _isLayoutNatTable = false;
   }

   private void toggleLayout_NatTable() {

      _viewLayout = TourBookViewLayout.NAT_TABLE;

      _actionToggleViewLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourBook_NatTable));

      _isLayoutNatTable = true;
   }

   @Override
   public void toursAreModified(final ArrayList<TourData> modifiedTours) {

      // do a reselection of the selected tours to fire the multi tour data selection

      actionSelectYearMonthTours();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   void updateTourBookOptions() {

      _columnFactory.setIsShowSummaryRow(isShowSummaryRow());

      reloadViewer();
   }

   void updateTourSelectionFilter(final TourCollectionFilter selectionFilter, final boolean isFromSlideout) {

      if (isFromSlideout) {

         _tourCollectionFilter = selectionFilter;
      }

//      final boolean isFilterActive = true;//_actionTourSelectionFilter.getSelection();
      final boolean isFilterActive = _actionTourCollectionFilter.getSelection();

      TourCollectionFilter tourCollectionFilter_InDataLoader;

      if (isFilterActive) {

         tourCollectionFilter_InDataLoader = _tourCollectionFilter;

         updateUI_TourCollectionFilterIcons(tourCollectionFilter_InDataLoader);

      } else {

         // filter is not active -> show all

         tourCollectionFilter_InDataLoader = TourCollectionFilter.ALL_TOURS;

         _actionTourCollectionFilter.showOtherEnabledImage(0);
      }

      // run async that the slideout UI is updated immediately
      _parent.getDisplay().asyncExec(() -> {

         updateUI_NatTable(tourCollectionFilter_InDataLoader);
      });
   }

   private void updateUI_NatTable(final TourCollectionFilter tourSelectionFilterInDataLoader) {

      _natTable_DataLoader.resetTourItems(true);

      _natTable_DataLoader.setTourCollectionFilter(tourSelectionFilterInDataLoader, _selectedTourIds);

      /*
       * Found no simple way to update the table otherwise an exceptions occurs somewhere in the
       * deep nattable layers
       */
      _tourViewer_NatTable.setRedraw(false);
      _isInSelection = true;
      {
         _tourViewer_NatTable.refresh();
      }
      _isInSelection = false;
      _tourViewer_NatTable.setRedraw(true);

      // update number of tours
      _actionTourCollectionFilter.slideoutTourSelectionFilter.updateUI();
   }

   private void updateUI_TourCollectionFilterIcons(final TourCollectionFilter tourCollectionFilter) {

      if (tourCollectionFilter == TourCollectionFilter.COLLECTED_TOURS) {

         _actionTourCollectionFilter.showOtherEnabledImage(1);

      } else if (tourCollectionFilter == TourCollectionFilter.NOT_COLLECTED_TOURS) {

         _actionTourCollectionFilter.showOtherEnabledImage(2);

      } else {

         // TourCollectionFilter.ALL_TOURS

         _actionTourCollectionFilter.showOtherEnabledImage(0);
      }
   }

   private void updateUI_TourViewerColumns_Tree() {

      // set tooltip text

      final String timeZone = _prefStore_Common.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
      final String timeZoneTooltip = NLS.bind(OtherMessages.COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP, timeZone);

      _columnFactory.getColDef_TimeZoneOffset_Tree().setColumnHeaderToolTipText(timeZoneTooltip);
   }

}
