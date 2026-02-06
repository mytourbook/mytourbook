/****************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.ui.SelectionCellLabelProvider;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StateSegment;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.ViewContext;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.ViewNames;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class EquipmentView extends ViewPart implements ITourProvider, ITourViewer, ITreeViewer {

   public static final String            ID                                     = "net.tourbook.equipment.EquipmentView.ID"; //$NON-NLS-1$

   private static final String           STATE_EQUIPMENT_FILTER                 = "STATE_EQUIPMENT_FILTER";                  //$NON-NLS-1$

   /**
    * The expanded equipment items have these structure:
    * <p>
    * 1. Type<br>
    * 2. id/year/month<br>
    * <br>
    * 3. Type<br>
    * 4. id/year/month<br>
    * ...
    */
   private static final String           STATE_EXPANDED_ITEMS                   = "STATE_EXPANDED_ITEMS";                    //$NON-NLS-1$
   private static final String           STATE_IS_ON_SELECT_EXPAND_COLLAPSE     = "STATE_IS_ON_SELECT_EXPAND_COLLAPSE";      //$NON-NLS-1$
   private static final String           STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS = "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS";  //$NON-NLS-1$

   /**
    * Using large numbers to easier debug and find issues
    */
   private static final int              STATE_ITEM_TYPE_SEPARATOR              = -1;

   private static final int              STATE_ITEM_TYPE_EQUIPMENT              = 1111;
   private static final int              STATE_ITEM_TYPE_EQUIPMENT_YEAR         = 1222;
   private static final int              STATE_ITEM_TYPE_EQUIPMENT_MONTH        = 1333;

   private static final int              STATE_ITEM_TYPE_PART                   = 2111;
   private static final int              STATE_ITEM_TYPE_PART_YEAR              = 2222;
   private static final int              STATE_ITEM_TYPE_PART_MONTH             = 2333;

   private static final IPreferenceStore _prefStore                             = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common                      = CommonActivator.getPrefStore();
   private static final IDialogSettings  _state                                 = TourbookPlugin.getState(ID);

   private static final PeriodType       _tourPeriodTemplate                    = PeriodType.yearMonthDay();

   private NumberFormat                  _nf0                                   = NumberFormat.getNumberInstance();
   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   private IPropertyChangeListener            _prefChangeListener;
   private IPropertyChangeListener            _prefChangeListener_Common;
   private ISelectionListener                 _postSelectionListener;
   private ITourEventListener                 _tourEventListener;

   private PostSelectionProvider              _postSelectionProvider;

   private TreeViewer                         _equipmentViewer;
   private ColumnManager                      _columnManager;
   private TVIEquipmentView_Root              _rootItem;
   private TourDoubleClickState               _tourDoubleClickState                    = new TourDoubleClickState();

   private MenuManager                        _viewerMenuManager;
   private Menu                               _treeContextMenu;
   private IContextMenuProvider               _viewerContextMenuProvider               = new TreeContextMenuProvider();

   private TreeViewerTourInfoToolTip          _tourInfoToolTip;

   private TreeColumnDefinition               _colDef_EquipmentImage;
   private int                                _columnIndex_EquipmentImage;
   private int                                _columnWidth_EquipmentImage;
   private int                                _defaultTreeItemHeight;
   private int                                _selectedTreeItemHeight;

   private OpenDialogManager                  _openDlgMgr                              = new OpenDialogManager();

   private EquipmentFilterType                _equipmentFilterType                     = EquipmentFilterType.ALL_IS_DISPLAYED;

   private EquipmentMenuManager               _equipmentMenuManager;
   private TagMenuManager                     _tagMenuManager;
   private TourTypeMenuManager                _tourTypeMenuManager;

   private HashMap<String, Object>            _allTourActions_Edit;
   private HashMap<String, Object>            _allTourActions_Export;

   private ActionEquipmentFilter              _actionToggleEquipmentFilter;
   private ActionEquipmentOptions             _actionEquipmentOptions;

   private ActionDeleteEquipment              _actionDeleteEquipment;
   private ActionDeletePart                   _actionDeletePart;
   private ActionDeleteService                _actionDeleteService;
   private ActionDuplicateEquipment           _actionDuplicateEquipment;
   private ActionDuplicatePart                _actionDuplicatePart;
   private ActionDuplicateService             _actionDuplicateService;
   private ActionEditEquipment                _actionEditEquipment;
   private ActionEditPart                     _actionEditPart;
   private ActionEditService                  _actionEditService;
   private ActionNewEquipment                 _actionNewEquipment;
   private ActionNewPart                      _actionNewPart;
   private ActionNewService                   _actionNewService;
   private ActionRefreshView                  _actionRefreshView;
   private ActionSetTourStructure             _actionSetTourStructure;
   private ActionSetTourStructure_All         _actionSetTourStructure_All;
   private ActionToggleCollatedTours          _actionToggleCollatedTours;

   private ActionCollapseAll_WithoutSelection _actionCollapseAll_WithoutSelection;
   private ActionCollapseOthers               _actionCollapseOthers;
   private ActionExpandSelection              _actionExpandSelection;
   private ActionOnMouseSelect_ExpandCollapse _actionOnMouseSelect_ExpandCollapse;
   private ActionSingleExpand_CollapseOthers  _actionSingleExpand_CollapseOthers;

   private ActionEditQuick                    _actionEditQuick;
   private ActionEditTour                     _actionEditTour;
   private ActionExport                       _actionExportTour;
   private ActionOpenTour                     _actionOpenTour;

   private int                                _numSelectedTours;

   private boolean                            _isBehaviour_SingleExpand_CollapseOthers = true;
   private boolean                            _isBehaviour_OnSelect_ExpandCollapse     = true;
   private boolean                            _isInCollapseAll;
   private boolean                            _isInExpandingSelection;
   private int                                _expandRunnableCounter;
   private long                               _lastExpandSelectionTime;

   private boolean                            _isMouseContextMenu;
   private boolean                            _isSelectedWithKeyboard;

   private boolean                            _isShowToolTipInEquipment;
   private boolean                            _isShowToolTipInTitle;
   private boolean                            _isShowToolTipInTags;

   private PixelConverter                     _pc;

   private Color                              _colorTour;
   private Color                              _colorContentCategory;
   private Color                              _colorContentSubCategory;
   private Color                              _colorDateCategory;
   private Color                              _colorDateSubCategory;

   /*
    * UI resources
    */
   private final Image _imgEquipment                 = TourbookPlugin.getImage(Images.Equipment);
   private final Image _imgEquipment_All             = TourbookPlugin.getImage(Images.Equipment_Only);
   private final Image _imgEquipment_Collated        = TourbookPlugin.getImage(Images.Equipment_Collated);
   private final Image _imgEquipment_Part            = TourbookPlugin.getImage(Images.Equipment_Part);
   private final Image _imgEquipment_Part_Collate    = TourbookPlugin.getImage(Images.Equipment_Part_Collated);
   private final Image _imgEquipment_Part_New        = TourbookPlugin.getImage(Images.Equipment_Part_New);
   private final Image _imgEquipment_Service         = TourbookPlugin.getImage(Images.Equipment_Service);
   private final Image _imgEquipment_Service_Collate = TourbookPlugin.getImage(Images.Equipment_Service_Collated);
   private final Image _imgEquipment_Service_New     = TourbookPlugin.getImage(Images.Equipment_Service_New);
   private final Image _imgTours_All                 = TourbookPlugin.getImage(Images.TourInView);

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerContainer;

//   private class ActionAppFilter extends Action {
//
//      ActionAppFilter() {
//
//         super(UI.EMPTY_STRING, AS_CHECK_BOX);
//
//         setToolTipText("Toggle app filter");
//
//         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
//      }
//
//      @Override
//      public void runWithEvent(final Event event) {
//         onAction_ToggleAppFilter(event);
//      }
//   }

   private class ActionCollapseAll_WithoutSelection extends ActionCollapseAll {

      public ActionCollapseAll_WithoutSelection() {
         super(EquipmentView.this);
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

   private class ActionDeleteEquipment extends Action {

      ActionDeleteEquipment() {

         super(Messages.Action_Equipment_Delete, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {
         onAction_DeleteEquipment();
      }
   }

   private class ActionDeletePart extends Action {

      ActionDeletePart() {

         super("&Delete Part...", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {
         onAction_DeletePart();
      }
   }

   private class ActionDeleteService extends Action {

      ActionDeleteService() {

         super("&Delete Service...", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {
         onAction_DeletePart();
      }
   }

   private class ActionDuplicateEquipment extends Action {

      public ActionDuplicateEquipment() {

         super("D&uplicate Equipment", AS_PUSH_BUTTON);

         setToolTipText("Duplicate equipment and adjust date to today");

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Duplicate));
      }

      @Override
      public void run() {
         onAction_DuplicateItem();
      }
   }

   private class ActionDuplicatePart extends Action {

      public ActionDuplicatePart() {

         super("D&uplicate Part", AS_PUSH_BUTTON);

         setToolTipText("Duplicate part and adjust date to today");

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Part_Duplicate));
      }

      @Override
      public void run() {
         onAction_DuplicateItem();
      }
   }

   private class ActionDuplicateService extends Action {

      public ActionDuplicateService() {

         super("D&uplicate Service", AS_PUSH_BUTTON);

         setToolTipText("Duplicate service and adjust date to today");

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Service_Duplicate));
      }

      @Override
      public void run() {
         onAction_DuplicateItem();
      }
   }

   private class ActionEditEquipment extends Action {

      public ActionEditEquipment() {

         super(Messages.Action_Equipment_Edit, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      }

      @Override
      public void run() {
         onAction_EditItem();
      }
   }

   private class ActionEditPart extends Action {

      public ActionEditPart() {

         super("&Edit Part", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      }

      @Override
      public void run() {
         onAction_EditItem();
      }
   }

   private class ActionEditService extends Action {

      public ActionEditService() {

         super("&Edit Service", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      }

      @Override
      public void run() {
         onAction_EditItem();
      }
   }

   private class ActionEquipmentFilter extends Action {

      ActionEquipmentFilter() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         final String tooltip = UI.EMPTY_STRING

               + "Toggle equipment filter between\n\n"
               + "• Show all equipment\n"
               + "• Show only equipment which contain tours\n"
               + "• Show only equipment which do not contain tours\n";

         setToolTipText(tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
      }

      @Override
      public void runWithEvent(final Event event) {
         onAction_ToggleEquipmentFilter(event);
      }
   }

   private class ActionEquipmentOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutEquipmentOptions(_parent, toolbar, EquipmentView.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionNewEquipment extends Action {

      public ActionNewEquipment() {

         super(Messages.Action_Equipment_New, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_New));
      }

      @Override
      public void run() {
         onAction_NewEquipment();
      }
   }

   private class ActionNewPart extends Action {

      public ActionNewPart() {

         super("New &Part", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Part_New));
      }

      @Override
      public void run() {
         onAction_NewPart();
      }
   }

   private class ActionNewService extends Action {

      public ActionNewService() {

         super("New &Service", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Service_New));
      }

      @Override
      public void run() {
         onAction_NewService();
      }
   }

   private class ActionOnMouseSelect_ExpandCollapse extends Action {

      public ActionOnMouseSelect_ExpandCollapse() {
         super(Messages.Tour_Tags_Action_OnMouseSelect_ExpandCollapse, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_OnMouseSelect_ExpandCollapse();
      }
   }

   private class ActionSingleExpand_CollapseOthers extends Action {

      public ActionSingleExpand_CollapseOthers() {
         super(Messages.Tour_Tags_Action_SingleExpand_CollapseOthers, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_SingleExpandCollapseOthers();
      }
   }

   private class ActionToggleCollatedTours extends Action {

      public ActionToggleCollatedTours() {

         super("Toggle Co&llated Tours", AS_PUSH_BUTTON);

         setToolTipText("Set or unset if tours are collated for this item");

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Asset_Collated));
      }

      @Override
      public void run() {
         onAction_ToggleCollatedTours();
      }
   }

   /**
    * Comparator is sorting the tree items
    */
   private final class EquipmentComparator extends ViewerComparator {
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         if (obj1 instanceof final TVIEquipmentView_Equipment item1
               && obj2 instanceof final TVIEquipmentView_Equipment item2) {

            // sort equipment by name

            final Equipment equipment1 = item1.getEquipment();
            final Equipment equipment2 = item2.getEquipment();

            final boolean isCollate1 = equipment1.isCollate();
            final boolean isCollate2 = equipment2.isCollate();

            if (isCollate1 && isCollate2) {

               // collated equipment

               // 1st compare by type
               int compareDiff = equipment1.getType().compareTo(equipment2.getType());

               // 2nd compare by date
               if (compareDiff == 0) {

                  final long date1 = equipment1.getDateFrom();
                  final long date2 = equipment2.getDateFrom();

                  final long dateDiff = date1 - date2;

                  // diff value can be larger than Integer.MAX_VALUE
                  if (dateDiff > 0) {
                     compareDiff = 1;
                  } else if (dateDiff < 0) {
                     compareDiff = -1;
                  }
               }

               return compareDiff;

            } else if (isCollate1) {

               // sort collated before not collated

               return -1;

            } else if (isCollate2) {

               // sort collated before not collated

               return 1;

            } else {

               // not collated equipment -> sort by name

               return equipment1.getName().compareTo(equipment2.getName());
            }

         } else if (obj1 instanceof final TVIEquipmentView_Part item1
               && obj2 instanceof final TVIEquipmentView_Part item2) {

            // sort part by type/date

            final EquipmentPart part1 = item1.getPart();
            final EquipmentPart part2 = item2.getPart();

            // 1st compare by type
            int compareDiff = part1.getType().compareTo(part2.getType());

            // 2nd compare by date
            if (compareDiff == 0) {

               final long date1 = part1.getDateFrom();
               final long date2 = part2.getDateFrom();

               final long dateDiff = date1 - date2;

               // diff value can be larger than Integer.MAX_VALUE
               if (dateDiff > 0) {
                  compareDiff = 1;
               } else if (dateDiff < 0) {
                  compareDiff = -1;
               }
            }

            return compareDiff;
         }

         return 0;
      }
   }

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <p>
    * <b>
    * A comparer is necessary to set and restore the expanded elements AND to reselect elements
    * </b>
    * <p>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    */
   private class EquipmentComparer implements IElementComparer {

      @Override
      public boolean equals(final Object o1, final Object o2) {

// SET_FORMATTING_OFF

         if (o1 == o2) {

            return true;

         } else if (o1 instanceof final TVIEquipmentView_Tour item1
                 && o2 instanceof final TVIEquipmentView_Tour item2) {

            return item1.tourId == item2.tourId;

         } else if (o1 instanceof final TVIEquipmentView_Equipment item1
                 && o2 instanceof final TVIEquipmentView_Equipment item2) {

            return item1.getEquipmentID() == item2.getEquipmentID();

         } else if (o1 instanceof final TVIEquipmentView_Part item1
                 && o2 instanceof final TVIEquipmentView_Part item2) {

            return item1.getPartID() == item2.getPartID();

         } else if (o1 instanceof final TVIEquipmentView_Equipment_Year item1
                 && o2 instanceof final TVIEquipmentView_Equipment_Year item2) {

            return item1.getEquipmentId() == item2.getEquipmentId()
                && item1.getYear()        == item2.getYear();

         } else if (o1 instanceof final TVIEquipmentView_Part_Year item1
                 && o2 instanceof final TVIEquipmentView_Part_Year item2) {

            return item1.getPartId() == item2.getPartId()
                && item1.getYear()   == item2.getYear();

         } else if (o1 instanceof final TVIEquipmentView_Equipment_Month monthItem1
                 && o2 instanceof final TVIEquipmentView_Equipment_Month monthItem2) {

            final TVIEquipmentView_Equipment_Year yearItem1 = monthItem1.getYearItem();
            final TVIEquipmentView_Equipment_Year yearItem2 = monthItem2.getYearItem();

            return yearItem1.getEquipmentId() == yearItem2.getEquipmentId()
                && yearItem1.getYear()        == yearItem2.getYear()
                && monthItem1.getMonth()      == monthItem2.getMonth();

         } else if (o1 instanceof final TVIEquipmentView_Part_Month monthItem1
                 && o2 instanceof final TVIEquipmentView_Part_Month monthItem2) {

            final TVIEquipmentView_Part_Year yearItem1 = monthItem1.getYearItem();
            final TVIEquipmentView_Part_Year yearItem2 = monthItem2.getYearItem();

            return yearItem1.getPartId() == yearItem2.getPartId()
                && yearItem1.getYear()   == yearItem2.getYear()
                && monthItem1.getMonth() == monthItem2.getMonth();
         }

// SET_FORMATTING_ON

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }

   }

   private final class EquipmentContentProvider implements ITreeContentProvider {

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _rootItem.getFetchedChildrenAsArray();
      }

      @Override
      public Object getParent(final Object element) {
         return ((TreeViewerItem) element).getParentItem();
      }

      @Override
      public boolean hasChildren(final Object element) {
         return ((TreeViewerItem) element).hasChildren();
      }
   }

   private class EquipmentFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         return isInEquipmentFilter(element);
      }
   }

   private enum EquipmentFilterType {

      ALL_IS_DISPLAYED,

      /**
       * Only equipment with tours are displayed
       */
      EQUIPMENT_WITH_TOURS,

      /**
       * Only equipment without tours are displayed
       */
      EQUIPMENT_WITHOUT_TOURS
   }

   private class TreeContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_treeContextMenu != null) {
            _treeContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _treeContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _treeContextMenu = createUI_22_CreateViewerContextMenu();

         return _treeContextMenu;
      }
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            updateColors();

            _equipmentViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _equipmentViewer.refresh();

            /*
             * The tree must be redrawn because the styled text does not show with the new color
             */
            _equipmentViewer.getTree().redraw();

         } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

            updateToolTipState();

         } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

            reloadViewer();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

//         final String property = propertyChangeEvent.getProperty();
//
//         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {
//
//            // measurement system has changed
//
//            _columnManager.saveState(_state);
//            _columnManager.clearColumns();
//
//            defineAllColumns();
//
//            _equipViewer = (TableViewer) recreateViewer(_equipViewer);
//         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * Listen for events when a tour is selected or deleted
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == EquipmentView.this) {
               return;
            }

            onSelectionChanged(selection);
         }
      };

      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == EquipmentView.this) {
            return;
         }

         if (tourEventId == TourEventId.TOUR_CHANGED
               || tourEventId == TourEventId.EQUIPMENT_STRUCTURE_CHANGED
               || tourEventId == TourEventId.EQUIPMENT_CONTENT_CHANGED // equipment image size is modified
         ) {

            reloadViewer();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * Close all opened dialogs except the opening dialog
    *
    * @param openingDialog
    */
   private void closeOpenedDialogs(final IOpeningDialog openingDialog) {

      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionToggleEquipmentFilter           = new ActionEquipmentFilter();
      _actionEquipmentOptions                = new ActionEquipmentOptions();

      // equipment actions
      _actionDeleteEquipment                 = new ActionDeleteEquipment();
      _actionDeletePart                      = new ActionDeletePart();
      _actionDeleteService                   = new ActionDeleteService();
      _actionDuplicateEquipment              = new ActionDuplicateEquipment();
      _actionDuplicatePart                   = new ActionDuplicatePart();
      _actionDuplicateService                = new ActionDuplicateService();
      _actionEditEquipment                   = new ActionEditEquipment();
      _actionEditPart                        = new ActionEditPart();
      _actionEditService                     = new ActionEditService();
      _actionNewEquipment                    = new ActionNewEquipment();
      _actionNewPart                         = new ActionNewPart();
      _actionNewService                      = new ActionNewService();
      _actionRefreshView                     = new ActionRefreshView(this);
      _actionSetTourStructure                = new ActionSetTourStructure(this);
      _actionSetTourStructure_All            = new ActionSetTourStructure_All();
      _actionToggleCollatedTours             = new ActionToggleCollatedTours();

      // collapse/expand actions
      _actionCollapseAll_WithoutSelection    = new ActionCollapseAll_WithoutSelection();
      _actionCollapseOthers                  = new ActionCollapseOthers(this);
      _actionExpandSelection                 = new ActionExpandSelection(this, true);
      _actionOnMouseSelect_ExpandCollapse    = new ActionOnMouseSelect_ExpandCollapse();
      _actionSingleExpand_CollapseOthers     = new ActionSingleExpand_CollapseOthers();

      // tour actions
      _actionEditQuick                       = new ActionEditQuick(this);
      _actionEditTour                        = new ActionEditTour(this);
      _actionExportTour                      = new ActionExport(this);
      _actionOpenTour                        = new ActionOpenTour(this);

      _allTourActions_Edit    = new HashMap<>();
      _allTourActions_Export  = new HashMap<>();

      _allTourActions_Edit.put(_actionEditQuick          .getClass().getName(),  _actionEditQuick);
      _allTourActions_Edit.put(_actionEditTour           .getClass().getName(),  _actionEditTour);
      _allTourActions_Edit.put(_actionOpenTour           .getClass().getName(),  _actionOpenTour);

      _allTourActions_Export.put(_actionExportTour       .getClass().getName(),  _actionExportTour);

      TourActionManager.setAllViewActions(ID,

            _allTourActions_Edit    .keySet(),
            _allTourActions_Export  .keySet(),

            _tourTypeMenuManager    .getAllTourTypeActions()   .keySet(),
            _tagMenuManager         .getAllTagActions()        .keySet(),
            _equipmentMenuManager   .getAllEquipmentActions()  .keySet()
      );

// SET_FORMATTING_ON
   }

   private void createMenuManager() {

// SET_FORMATTING_OFF

      _equipmentMenuManager   = new EquipmentMenuManager(this, true, true);
      _tagMenuManager         = new TagMenuManager(this, true);
      _tourTypeMenuManager    = new TourTypeMenuManager(this);

// SET_FORMATTING_ON

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> {

         _tourInfoToolTip.hideToolTip();

         fillContextMenu(menuManager);
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createMenuManager();

      // define all columns
      _columnManager = new ColumnManager(this, _state);
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createActions();
      fillActionBars();

      createUI(parent);

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      addPrefListener();
      addTourEventListener();
      addSelectionListener();

      restoreState();

      updateColors();
      reloadViewer();

      restoreState_Viewer();

      enableActions();
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_EqipmentViewer(_viewerContainer);
      }
   }

   private void createUI_10_EqipmentViewer(final Composite parent) {

      /*
       * Create tree
       */
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _defaultTreeItemHeight = tree.getItemHeight();

      /*
       * Create viewer
       */
      _equipmentViewer = new TreeViewer(tree);
      _columnManager.createColumns(_equipmentViewer);

      _equipmentViewer.setContentProvider(new EquipmentContentProvider());
      _equipmentViewer.setComparator(new EquipmentComparator());
      _equipmentViewer.setComparer(new EquipmentComparer());
      _equipmentViewer.setFilters(new EquipmentFilter());

      _equipmentViewer.setUseHashlookup(true);

      _equipmentViewer.addSelectionChangedListener(selectionChangedEvent -> onEquipmentViewer_Selection(selectionChangedEvent));
      _equipmentViewer.addDoubleClickListener(doubleClickEvent -> onEquipmentViewer_DoubleClick());

      tree.addListener(SWT.MouseDoubleClick, event -> onAction_EditItem());
      tree.addListener(SWT.MouseDown, event -> onEquipmentTree_MouseDown(event));

      tree.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         _isSelectedWithKeyboard = true;

         enableActions();

         switch (keyEvent.keyCode) {

         case SWT.DEL:

            // delete equipment only when the delete button is enabled
            if (_actionDeleteEquipment.isEnabled()) {

               onAction_DeleteEquipment();

            } else if (_actionDeletePart.isEnabled()
                  || _actionDeleteService.isEnabled()) {

               onAction_DeletePart();
            }

            break;

         case SWT.CR:
         case SWT.F2:
            onAction_EditItem();
            break;
         }
      }));

      /*
       * The context menu must be created AFTER the viewer is created which is also done after the
       * measurement system has changed, if not, the context menu is not displayed because it
       * belongs to the old viewer
       */
      createUI_20_ContextMenu();

      createUI_30_SetupColumnImages(tree);

//    fillToolBar();

      // set tour info tooltip provider
      _tourInfoToolTip = new TreeViewerTourInfoToolTip(_equipmentViewer);

// this tooltip provider shows equipment info
//    _tourInfoToolTip.setTooltipUICustomProvider(new TaggingView_TooltipUIProvider(this));
   }

   /**
    * Setup the viewer context menu
    */
   private void createUI_20_ContextMenu() {

      _treeContextMenu = createUI_22_CreateViewerContextMenu();

      final Tree tree = (Tree) _equipmentViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

   /**
    * Create the viewer context menu
    *
    * @return
    */
   private Menu createUI_22_CreateViewerContextMenu() {

      final Tree tree = (Tree) _equipmentViewer.getControl();

      // add the context menu to the tree viewer

      final Menu treeContextMenu = _viewerMenuManager.createContextMenu(tree);

//      treeContextMenu.addMenuListener(new MenuAdapter() {
//         @Override
//         public void menuHidden(final MenuEvent e) {
//            _tagMenuManager.onHideMenu();
//         }
//
//         @Override
//         public void menuShown(final MenuEvent menuEvent) {
//            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
//         }
//      });

      return treeContextMenu;
   }

   private void createUI_30_SetupColumnImages(final Tree tree) {

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _columnManager.getActiveProfile();
      _columnIndex_EquipmentImage = activeProfile.getColumnIndex(_colDef_EquipmentImage.getColumnId());

      final int numColumns = tree.getColumns().length;

      // add listeners
      if (_columnIndex_EquipmentImage >= 0 && _columnIndex_EquipmentImage < numColumns) {

         // column is visible

         final TreeColumn imageColumn = tree.getColumn(_columnIndex_EquipmentImage);
         final ControlListener controlResizedAdapter = ControlListener.controlResizedAdapter(
               controlEvent -> onColumnImage_Resize_SetWidthForImageColumn());

         imageColumn.addControlListener(controlResizedAdapter);
         tree.addControlListener(controlResizedAdapter);

         /*
          * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
          * critical for performance that these methods be as efficient as possible.
          */
         final Listener paintListener = event -> onColumnImage_OnPaintViewer(event);
         tree.addListener(SWT.MeasureItem, paintListener);
         tree.addListener(SWT.PaintItem, paintListener);

      }
   }

   /**
    * Defines all columns for the table viewer in the column manager
    */
   private void defineAllColumns() {

      defineColumn_1stColumn();

      defineColumn_Equipment_Brand();
      defineColumn_Equipment_Model();
      defineColumn_Equipment_Type();
      defineColumn_Equipment_Collate();

      defineColumn_Tour_Title();

      defineColumn_Tour_Marker();
      defineColumn_Tour_Tags();
      defineColumn_Tour_Equipment();

      defineColumn_Equipment_Image();
      defineColumn_Equipment_ImageFilePath();

      defineColumn_Equipment_Date();
      defineColumn_Equipment_Date_Until();
      defineColumn_Equipment_Date_UsageDuration();
      defineColumn_Equipment_Date_Built();
      defineColumn_Equipment_Date_Retired();

      defineColumn_Equipment_Price();
      defineColumn_Equipment_PriceUnit();
      defineColumn_Equipment_Size();
      defineColumn_Equipment_Weight();
      defineColumn_Equipment_InitialDistance();

      defineColumn_Time_ElapsedTime();
      defineColumn_Time_MovingTime();
      defineColumn_Time_PausedTime();

      defineColumn_Motion_Distance();
      defineColumn_Motion_MaxSpeed();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();

      defineColumn_Altitude_Up();
      defineColumn_Altitude_Down();
      defineColumn_Altitude_Max();

      defineColumn_Body_MaxPulse();
      defineColumn_Body_AvgPulse();

      defineColumn_Weather_Temperature_Avg_Device();

      defineColumn_Powertrain_AvgCadence();

      defineColumn_Equipment_ID();
      defineColumn_Equipment_ExpandType();
   }

   /**
    * Column: Equipment & Category
    */
   private void defineColumn_1stColumn() {

      final TreeColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_AND_CATEGORY.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Object getData(final ViewerCell cell) {

//            if (_isShowToolTipInEquipment == false) {
//               return null;
//            }
//
//            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) cell.getElement();
//
//            if (viewItem instanceof TVIEquipmentView_Equipment) {
//
//               // return equipment to show it's notes fields in the tooltip
//
//               return viewItem;
//            }

            return null;
         }

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipInEquipment == false) {
               return null;
            }

            final Object element = cell.getElement();
            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) element;

            if (viewItem instanceof final TVIEquipmentView_Tour tourItem) {
               return tourItem.tourId;
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) element;

            final long numTours = viewItem.numTours_IsCollated;

            final StyledString styledString = new StyledString();

            if (viewItem instanceof final TVIEquipmentView_Tour tourItem) {

               /*
                * Tour
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.TOUR_STYLER);

               cell.setImage(TourTypeImage.getTourTypeImage(tourItem.tourTypeId));

               setCellColor(cell, element);

            } else if (viewItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

               /*
                * Equipment
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.CONTENT_CATEGORY_STYLER);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

               final long numTours_All = viewItem.numTours_All;

               if (numTours_All > 0) {

                  styledString.append(UI.SPACE3 + numTours_All, net.tourbook.ui.UI.TOUR_STYLER);
               }

               if (equipmentItem.getEquipment().isCollate()) {
                  cell.setImage(_imgEquipment_Collated);
               } else {
                  cell.setImage(_imgEquipment_All);
               }

            } else if (viewItem instanceof final TVIEquipmentView_Part partItem) {

               /*
                * Part
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.CONTENT_SUB_CATEGORY_STYLER);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

               final EquipmentPart part = partItem.getPart();

               if (part.isItemType_Part()) {

                  if (part.isCollate()) {
                     cell.setImage(_imgEquipment_Part_Collate);
                  } else {
                     cell.setImage(_imgEquipment_Part);
                  }

               } else if (part.isItemType_Service()) {

                  if (part.isCollate()) {
                     cell.setImage(_imgEquipment_Service_Collate);
                  } else {
                     cell.setImage(_imgEquipment_Service);
                  }
               }

            } else if (viewItem instanceof TVIEquipmentView_Part_Year
                  || viewItem instanceof TVIEquipmentView_Equipment_Year) {

               /*
                * Year
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.DATE_CATEGORY_STYLER);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

            } else if (viewItem instanceof TVIEquipmentView_Part_Month
                  || viewItem instanceof TVIEquipmentView_Equipment_Month) {

               /*
                * Month
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.DATE_SUB_CATEGORY_STYLER);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

            } else {

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.TOUR_STYLER);
            }

            cell.setText(styledString.getString());
            cell.setStyleRanges(styledString.getStyleRanges());
         }
      });
   }

   /**
    * Column: Elevation loss (m)
    */
   private void defineColumn_Altitude_Down() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            double value = ((TVIEquipmentView_Item) element).colAltitudeDown;

            if (value != 0) {

               value = -value / UI.UNIT_VALUE_ELEVATION;

               colDef.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Max elevation
    */
   private void defineColumn_Altitude_Max() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_MAX.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            double value = ((TVIEquipmentView_Item) element).colMaxAltitude;

            if (value != 0) {

               value = value / UI.UNIT_VALUE_ELEVATION;

               colDef.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Elevation gain (m)
    */
   private void defineColumn_Altitude_Up() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            double value = ((TVIEquipmentView_Item) element).colAltitudeUp;

            if (value != 0) {

               value = value / UI.UNIT_VALUE_ELEVATION;

               colDef.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: avg pulse
    */
   private void defineColumn_Body_AvgPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = ((TVIEquipmentView_Item) element).colAvgPulse;

            if (value != 0) {

               colDef.printDoubleValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: max pulse
    */
   private void defineColumn_Body_MaxPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long value = ((TVIEquipmentView_Item) element).colMaxPulse;

            if (value != 0) {

               colDef.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Brand
    */
   private void defineColumn_Equipment_Brand() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_BRAND.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               cell.setText(tviEquipment.getEquipment().getBrand());
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Collate
    */
   private void defineColumn_Equipment_Collate() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_COLLATE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            boolean isCollate = false;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               isCollate = equipmentItem.getEquipment().isCollate();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               isCollate = partItem.getPart().isCollate();
            }

            if (isCollate) {

               cell.setText(UI.SYMBOL_BOX);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: First use date
    */
   private void defineColumn_Equipment_Date() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            LocalDateTime date = null;

            if (element instanceof final TVIEquipmentView_Equipment viewItem) {

               date = viewItem.getEquipment().getDateFrom_Local();

            } else if (element instanceof final TVIEquipmentView_Part viewItem) {

               date = viewItem.getPart().getDateFrom_Local();
            }

            if (date != null) {

               final ValueFormat valueFormatter = colDef.getValueFormat_Detail();

               String dateFormatted;

               if (valueFormatter.equals(ValueFormat.DATE_TIME)) {
                  dateFormatted = date.format(TimeTools.Formatter_Date_S);
               } else {
                  dateFormatted = date.format(TimeTools.Formatter_DateTime_SM);
               }

               cell.setText(dateFormatted);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Build date
    */
   private void defineColumn_Equipment_Date_Built() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_BUILT.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            LocalDateTime date = null;

            if (element instanceof final TVIEquipmentView_Equipment viewItem) {

               date = viewItem.getEquipment().getDateBuilt_Local();

            } else if (element instanceof final TVIEquipmentView_Part viewItem) {

               date = viewItem.getPart().getDateBuilt_Local();
            }

            if (date != null) {

               cell.setText(TimeTools.Formatter_Date_S.format(date));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Retired date
    */
   private void defineColumn_Equipment_Date_Retired() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_RETIRED.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            LocalDateTime date = null;

            if (element instanceof final TVIEquipmentView_Equipment viewItem) {

               date = viewItem.getEquipment().getDateRetired_Local();

            } else if (element instanceof final TVIEquipmentView_Part viewItem) {

               date = viewItem.getPart().getDateRetired_Local();
            }

            if (date != null) {

               cell.setText(TimeTools.Formatter_Date_S.format(date));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Until date
    */
   private void defineColumn_Equipment_Date_Until() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_UNTIL.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            boolean isCollate = false;
            LocalDateTime date = null;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               final Equipment equipment = equipmentItem.getEquipment();

               isCollate = equipment.isCollate();
               date = equipment.getDateUntil_Local();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               final EquipmentPart part = partItem.getPart();

               isCollate = part.isCollate();
               date = part.getDateUntil_Local();
            }

            if (isCollate && date != null) {

               final ValueFormat valueFormatter = colDef.getValueFormat_Detail();

               String dateFormatted;

               if (valueFormatter.equals(ValueFormat.DATE_TIME)) {
                  dateFormatted = date.format(TimeTools.Formatter_Date_S);
               } else {
                  dateFormatted = date.format(TimeTools.Formatter_DateTime_SM);
               }

               cell.setText(dateFormatted);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Usage duration
    */
   private void defineColumn_Equipment_Date_UsageDuration() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_USAGE_DURATION.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Part partItem) {

               final boolean isCollate = partItem.getPart().isCollate();

               if (isCollate) {

                  final long durationMS = partItem.usageDuration;
                  final String durationLast = partItem.usageDurationLast;

                  final Period durationPeriod = new Period(0, durationMS, _tourPeriodTemplate);

                  final String formattedDuration = durationPeriod.toString(UI.DURATION_FORMATTER_YEAR_MONTH_DAY);

                  cell.setText(durationLast + formattedDuration);
                  setCellColor(cell, element);
               }
            }
         }
      });
   }

   /**
    * Column: Expand type
    */
   private void defineColumn_Equipment_ExpandType() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_EXPAND_TYPE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            int expandType;
            String label = null;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               if (equipmentItem.getEquipment().isCollate()) {

                  expandType = equipmentItem.getExpandType();
                  label = EquipmentManager.EXPAND_TYPE_LABEL[expandType];
               }

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               expandType = partItem.getExpandType();
               label = EquipmentManager.EXPAND_TYPE_LABEL[expandType];
            }

            if (label != null) {

               cell.setText(label);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: ID
    */
   private void defineColumn_Equipment_ID() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_ID.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            long id = -1;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               id = equipmentItem.getEquipmentID();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               id = partItem.getPartID();

            } else if (element instanceof final TVIEquipmentView_Tour tourItem) {

               id = tourItem.tourId;
            }

            if (id != -1) {

               cell.setText(Long.toString(id));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Image
    */
   private void defineColumn_Equipment_Image() {

      _colDef_EquipmentImage = TreeColumnFactory.EQUIPMENT_IMAGE.createColumn(_columnManager, _pc);

      _colDef_EquipmentImage.setLabelProvider(new CellLabelProvider() {

         // !!! set dummy label provider, otherwise an error occurs !!!
         // the image is painted in onColumnImage_OnPaintViewer()

         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Image file path
    */
   private void defineColumn_Equipment_ImageFilePath() {

      final TreeColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_IMAGE_FILE_PATH.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            String imageFilePath = null;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               imageFilePath = equipmentItem.getEquipment().getImageFilePath();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               imageFilePath = partItem.getPart().getImageFilePath();
            }

            if (imageFilePath != null) {
               cell.setText(imageFilePath);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Initial distance
    */
   private void defineColumn_Equipment_InitialDistance() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_INITIAL_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            float distance = 0;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               distance = equipmentItem.getEquipment().getDistanceFirstUse();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               distance = partItem.getPart().getDistanceFirstUse();
            }

            if (distance != 0) {

               cell.setText(_nf0.format(distance));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Model
    */
   private void defineColumn_Equipment_Model() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_MODEL.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               cell.setText(tviEquipment.getEquipment().getModel());
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Price
    */
   private void defineColumn_Equipment_Price() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_PRICE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Item viewItem) {

               final float price = viewItem.price;

               colDef.printDoubleValue(cell, price, true);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Price unit
    */
   private void defineColumn_Equipment_PriceUnit() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_PRICE_UNIT.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Item viewItem) {

               cell.setText(viewItem.priceUnit);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Size
    */
   private void defineColumn_Equipment_Size() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_SIZE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            String size = null;
            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               size = equipmentItem.getEquipment().getSize();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               size = partItem.getPart().getSize();
            }

            if (size != null) {

               cell.setText(size);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Type
    */
   private void defineColumn_Equipment_Type() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_TYPE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Item viewItem) {

               final String type = viewItem.type;

               if (type != null) {

                  cell.setText(type);
                  setCellColor(cell, element);
               }
            }
         }
      });
   }

   /**
    * Column: Weight
    */
   private void defineColumn_Equipment_Weight() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_WEIGHT.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            float weight = 0;

            if (element instanceof final TVIEquipmentView_Equipment equipmentItem) {

               weight = equipmentItem.getEquipment().getWeight();

            } else if (element instanceof final TVIEquipmentView_Part partItem) {

               weight = partItem.getPart().getWeight();
            }

            if (weight != 0) {

               colDef.printDoubleValue(cell, weight, true);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: avg pace min/km - min/mi
    */
   private void defineColumn_Motion_AvgPace() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float value = ((TVIEquipmentView_Item) element).colAvgPace;

            if (value != 0) {

               final float pace = value * UI.UNIT_VALUE_DISTANCE;

               if (pace == 0.0) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(UI.format_mm_ss((long) pace));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: avg speed km/h - mph
    */
   private void defineColumn_Motion_AvgSpeed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            float value = ((TVIEquipmentView_Item) element).colAvgSpeed;

            if (value != 0) {

               value = value / UI.UNIT_VALUE_DISTANCE;

               colDef.printDoubleValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Distance (km/miles)
    */
   private void defineColumn_Motion_Distance() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            float value = ((TVIEquipmentView_Item) element).colDistance;

            if (value != 0) {

               value = value
                     / 1000.0f
                     / UI.UNIT_VALUE_DISTANCE;

               colDef.printDoubleValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: max speed
    */
   private void defineColumn_Motion_MaxSpeed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            float value = ((TVIEquipmentView_Item) element).colMaxSpeed;

            if (value != 0) {

               value = value / UI.UNIT_VALUE_DISTANCE;

               colDef.printDoubleValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: avg cadence
    */
   private void defineColumn_Powertrain_AvgCadence() {

      final TreeColumnDefinition colDef = TreeColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float value = ((TVIEquipmentView_Item) element).colAvgCadence;

            if (value != 0) {

               colDef.printDoubleValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: elapsed time (h)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long value = ((TVIEquipmentView_Item) element).colElapsedTime;

            if (value != 0) {

               colDef.printLongValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Time - Moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long value = ((TVIEquipmentView_Item) element).colMovingTime;

            if (value != 0) {

               colDef.printLongValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: paused time (h)
    */
   private void defineColumn_Time_PausedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_PAUSED_TIME.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long value = ((TVIEquipmentView_Item) element).colPausedTime;

            if (value != 0) {

               colDef.printLongValue(cell, value, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour - Equipment
    */
   private void defineColumn_Tour_Equipment() {

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_EQUIPMENT.createColumn(_columnManager, _pc);

      colDef_Tree.setLabelProvider(new SelectionCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof final TVIEquipmentView_Tour tourItem) {

               cell.setText(EquipmentManager.getEquipmentNames(tourItem.getEquipmentIds()));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour - Markers
    */
   private void defineColumn_Tour_Marker() {

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_NUM_MARKERS.createColumn(_columnManager, _pc);

      colDef_Tree.setIsDefaultColumn();

      colDef_Tree.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Tour tourItem) {

               final List<Long> allMarkerIds = tourItem.getMarkerIds();
               if (allMarkerIds != null) {

                  cell.setText(Integer.toString(allMarkerIds.size()));

                  setCellColor(cell, element);
               }
            }
         }
      });
   }

   /**
    * Column: Tour - Tags
    */
   private void defineColumn_Tour_Tags() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipInTags == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            List<Long> tagIds = null;
            if (element instanceof final TVIEquipmentView_Tour tourItem) {

               tagIds = tourItem.getTagIds();

               if (tagIds != null) {

                  cell.setText(TourDatabase.getTagNames(tagIds));
                  setCellColor(cell, element);
               }
            }
         }
      });
   }

   /**
    * Column: Title
    */
   private void defineColumn_Tour_Title() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipInTitle == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Tour tourItem) {

               cell.setText(tourItem.tourTitle);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Weather - Average temperature (measured from the device)
    */
   private void defineColumn_Weather_Temperature_Avg_Device() {

      final TreeColumnDefinition colDef = TreeColumnFactory.WEATHER_TEMPERATURE_AVG_DEVICE.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float value = ((TVIEquipmentView_Item) element).colAvgTemperature_Device;

            if (value != 0) {

               final double temperature = UI.convertTemperatureFromMetric(value);

               colDef.printDoubleValue(cell, temperature, element instanceof TVIEquipmentView_Tour);

               setCellColor(cell, element);
            }
         }
      });
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

// SET_FORMATTING_OFF

      _imgEquipment                 .dispose();
      _imgEquipment_All             .dispose();
      _imgEquipment_Collated        .dispose();
      _imgEquipment_Part            .dispose();
      _imgEquipment_Part_Collate    .dispose();
      _imgEquipment_Part_New        .dispose();
      _imgEquipment_Service         .dispose();
      _imgEquipment_Service_Collate .dispose();
      _imgEquipment_Service_New     .dispose();
      _imgTours_All                 .dispose();

// SET_FORMATTING_ON

      super.dispose();
   }

   private void enableActions() {

      final ITreeSelection allSelectedItems = _equipmentViewer.getStructuredSelection();

      /*
       * Count number of selected items
       */
      int numItems = 0;
      int numEquipment = 0;
      int numParts = 0;
      int numServices = 0;
      int numTours = 0;

      TVIEquipmentView_Part selectedPartItem = null;
      TVIEquipmentView_Tour selectedTourItem = null;
      final List<TreeViewerItem> allSelectedTreeItems = new ArrayList<>();

      boolean isEquipmentCollate = false;

      for (final Object selectedItem : allSelectedItems) {

         numItems++;

         TVIEquipmentView_Equipment equipmentItemContext = null;

         if (selectedItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

            equipmentItemContext = equipmentItem;

            numEquipment++;

         } else if (selectedItem instanceof final TVIEquipmentView_Part partItem) {

            final EquipmentPart part = partItem.getPart();

            if (part.isItemType_Part()) {

               numParts++;

            } else if (part.isItemType_Service()) {

               numServices++;
            }

            selectedPartItem = partItem;

         } else if (selectedItem instanceof final TVIEquipmentView_Equipment_Year yearItem) {

            equipmentItemContext = yearItem.getEquipmentItem();

         } else if (selectedItem instanceof final TVIEquipmentView_Equipment_Month monthItem) {

            equipmentItemContext = monthItem.getEquipmentItem();

         } else if (selectedItem instanceof final TVIEquipmentView_Part_Year yearItem) {

            selectedPartItem = yearItem.getPartItem();

         } else if (selectedItem instanceof final TVIEquipmentView_Part_Month monthItem) {

            selectedPartItem = monthItem.getPartItem();

         } else if (selectedItem instanceof final TVIEquipmentView_Tour tourItem) {

            numTours++;
            selectedTourItem = tourItem;

            selectedPartItem = tourItem.getPartItem();
            equipmentItemContext = tourItem.getEquipmentItem();

            allSelectedTreeItems.add(tourItem);
         }

         if (equipmentItemContext != null) {

            final Equipment equipment = equipmentItemContext.getEquipment();

            isEquipmentCollate = equipment.isCollate();
         }
      }

      final List<Long> allSelectedTourIds = getSelectedTourIDs();
      _numSelectedTours = allSelectedTourIds.size();
      final boolean isSelectedTours = _numSelectedTours > 0;

// SET_FORMATTING_OFF

      final boolean isOneItemSelected           = numItems == 1;

      final boolean isEquipmentSelected         = numEquipment > 0;
      final boolean isPartSelected              = numParts > 0;
      final boolean isServiceSelected           = numServices > 0;
      final boolean isTourSelected              = numTours > 0;
      final boolean isOneTour                   = numTours == 1;
      final boolean isOneServiceSelected        = isServiceSelected   && isOneItemSelected;
      final boolean isOnePartSelected           = isPartSelected      && isOneItemSelected;

      final boolean canCreatePartOrService      = isOneItemSelected
                                                   && (isEquipmentSelected || isPartSelected || isServiceSelected)
                                                   && isEquipmentCollate == false                                                   ;

      final boolean canSetTourStructure         = isOneItemSelected
                                                   && (selectedPartItem != null || isEquipmentCollate);


      /*
       * Multiple part/services can be much more complex when they have different anchestors (equipment)
       */
      final boolean isEnableDeletePart    = numParts == 1;
      final boolean isEnableDeleteService = numServices == 1;

      final long tourTypeID = isOneTour
            ? selectedTourItem.tourId
            : TourDatabase.ENTITY_IS_NOT_SAVED;

      final List<Long> oneTourTagIds = isOneTour
            ? selectedTourItem.getTagIds()
            : null;

      _actionDeleteEquipment     .setEnabled(isEquipmentSelected);
      _actionDeletePart          .setEnabled(isEnableDeletePart);
      _actionDeleteService       .setEnabled(isEnableDeleteService);
      _actionDuplicateEquipment  .setEnabled(isEquipmentSelected && isEquipmentCollate);
      _actionDuplicatePart       .setEnabled(isOnePartSelected);
      _actionDuplicateService    .setEnabled(isOneServiceSelected);
      _actionEditEquipment       .setEnabled(isEquipmentSelected && isOneItemSelected);
      _actionEditPart            .setEnabled(isOnePartSelected);
      _actionEditService         .setEnabled(isOneServiceSelected);
      _actionNewPart             .setEnabled(canCreatePartOrService);
      _actionNewService          .setEnabled(canCreatePartOrService);
      _actionSetTourStructure    .setEnabled(canSetTourStructure);
      _actionToggleCollatedTours .setEnabled(isOneItemSelected && (isEquipmentSelected || isPartSelected));

      _actionExportTour          .setEnabled(isSelectedTours);

      _actionEditTour            .setEnabled(isOneTour);
      _actionOpenTour            .setEnabled(isOneTour);
      _actionEditQuick           .setEnabled(isOneTour);

      _tagMenuManager            .enableTagActions(isTourSelected, isOneTour, oneTourTagIds);
      _tourTypeMenuManager       .enableTourTypeActions(isTourSelected, tourTypeID);
      _equipmentMenuManager      .enableActions(allSelectedTreeItems);

      _tourDoubleClickState.canEditTour         = isOneTour;
      _tourDoubleClickState.canOpenTour         = isOneTour;
      _tourDoubleClickState.canQuickEditTour    = isOneTour;
      _tourDoubleClickState.canEditMarker       = isOneTour;
      _tourDoubleClickState.canAdjustAltitude   = isOneTour;

// SET_FORMATTING_ON

   }

   private void expandCollapseItem(final TreeViewerItem treeItem) {

      if (_equipmentViewer.getExpandedState(treeItem)) {

         _equipmentViewer.collapseToLevel(treeItem, 1);

      } else {

         _equipmentViewer.expandToLevel(treeItem, 1);
      }
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionToggleEquipmentFilter);
      tbm.add(_actionNewEquipment);
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll_WithoutSelection);
      tbm.add(_actionRefreshView);
      tbm.add(_actionEquipmentOptions);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      /*
       * Count number of selected items
       */
      final ITreeSelection allSelectedItems = _equipmentViewer.getStructuredSelection();

      int numEquipment = 0;
      int numParts = 0;
      int numServices = 0;

      for (final Object selectedItem : allSelectedItems) {

         if (selectedItem instanceof TVIEquipmentView_Equipment) {

            numEquipment++;

         } else if (selectedItem instanceof final TVIEquipmentView_Part partItem) {

            final EquipmentPart part = partItem.getPart();

            if (part.isItemType_Part()) {

               numParts++;

            } else if (part.isItemType_Service()) {

               numServices++;
            }
         }
      }

      // edit tour actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EDIT, _allTourActions_Edit, this);

      // tour type actions
      _tourTypeMenuManager.fillContextMenu_WithActiveActions(menuMgr, this);

      // add/remove ... tags in the tours
      _tagMenuManager.fillTagMenu_WithActiveActions(menuMgr, this);

      // equipment actions
      _equipmentMenuManager.fillEquipmentMenu_WithActiveActions(menuMgr, this);

      // export actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EXPORT, _allTourActions_Export, this);

      // customize equipment actions
      menuMgr.add(_actionNewEquipment);
      menuMgr.add(_actionNewPart);
      menuMgr.add(_actionNewService);

      // display only ONE action
      if (numEquipment == 1) {

         menuMgr.add(_actionDuplicateEquipment);
         menuMgr.add(_actionEditEquipment);

      } else if (numParts == 1) {

         menuMgr.add(_actionDuplicatePart);
         menuMgr.add(_actionEditPart);

      } else if (numServices == 1) {

         menuMgr.add(_actionDuplicateService);
         menuMgr.add(_actionEditService);
      }

      menuMgr.add(_actionToggleCollatedTours);
      menuMgr.add(_actionSetTourStructure);
      menuMgr.add(_actionSetTourStructure_All);

      // expand/collapse actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionCollapseOthers);
      menuMgr.add(_actionExpandSelection);
      menuMgr.add(_actionCollapseAll_WithoutSelection);
      menuMgr.add(_actionOnMouseSelect_ExpandCollapse);
      menuMgr.add(_actionSingleExpand_CollapseOthers);

      // delete actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionDeleteEquipment);
      menuMgr.add(_actionDeletePart);
      menuMgr.add(_actionDeleteService);

      // customize context menu
      TourActionManager.fillContextMenu_CustomizeAction(menuMgr)

            // set pref page custom data that actions from this view can be identified
            .setPrefData(new ViewContext(ID, ViewNames.VIEW_NAME_EQUIPMENT));

      enableActions();

      // set AFTER the actions are enabled this retrieves the number of tours
      _actionExportTour.setNumberOfTours(_numSelectedTours);
   }

   private Long getCellTourId(final ViewerCell cell) {

      final Object element = cell.getElement();

      if (element instanceof final TVIEquipmentView_Tour tourItem) {

         return tourItem.tourId;
      }

      return null;
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   public int getDefaultItemHeight() {
      return _defaultTreeItemHeight;
   }

   private Equipment getEquipmentFromSelection() {

      final ITreeSelection allSelectedItems = _equipmentViewer.getStructuredSelection();
      final Object firstSelectedItem = allSelectedItems.getFirstElement();

      if (firstSelectedItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

         return equipmentItem.getEquipment();

      } else if (firstSelectedItem instanceof final TVIEquipmentView_Part partItem) {

         return partItem.getEquipment();

      } else {

         return null;
      }
   }

   private List<Long> getSelectedTourIDs() {

      final List<Long> allTourIds = new ArrayList<>();
      final Set<Long> checkedTourIds = new HashSet<>();

      final ITreeSelection allSelectedItems = _equipmentViewer.getStructuredSelection();

      for (final Object selectedItem : allSelectedItems) {

         if (selectedItem instanceof final TVIEquipmentView_Tour tourItem) {

            final long tourId = tourItem.tourId;

            if (checkedTourIds.add(tourId)) {
               allTourIds.add(tourId);
            }
         }
      }

      return allTourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tour id's
      final List<Long> tourIds = getSelectedTourIDs();

      /*
       * Show busyindicator when multiple tours needs to be retrieved from the database
       */
      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      if (tourIds.size() > 1) {

         BusyIndicator.showWhile(Display.getCurrent(), () -> TourManager.getInstance().getTourData(selectedTourData, tourIds));

      } else {

         TourManager.getInstance().getTourData(selectedTourData, tourIds);
      }

      return selectedTourData;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _equipmentViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _equipmentViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

   }

   /**
    * @param item
    *
    * @return Returns <code>true</code> when the item is visible in the current equipment filter
    */
   private boolean isInEquipmentFilter(final Object item) {

      if (_equipmentFilterType == EquipmentFilterType.ALL_IS_DISPLAYED) {

         // nothing is filtered

         return true;
      }

      // equipment is filtered

      if (item instanceof final TVIEquipmentView_Equipment equipmentItem) {

         final boolean hasTour = equipmentItem.numTours_All > 0;

         if (_equipmentFilterType == EquipmentFilterType.EQUIPMENT_WITH_TOURS && hasTour) {

            // show equipment WITH tours

            return true;

         } else if (_equipmentFilterType == EquipmentFilterType.EQUIPMENT_WITHOUT_TOURS && hasTour == false) {

            // show equipment WITHOUT tours

            return true;

         } else {

            return false;
         }
      }

      // all other items are not filtered

      return true;
   }

   /**
    * Load all tree items that expandable items do show the number of items
    */
   private void loadAllTreeItems() {

      // get all equipment viewer items

      final List<TreeViewerItem> allRootItems = _rootItem.getFetchedChildren();

      for (final TreeViewerItem rootItem : allRootItems) {

         // is recursive !!!
         loadAllTreeItems_One(rootItem, _rootItem);
      }
   }

   /**
    * !!! RECURSIVE !!!
    * <p>
    * Traverses all equipment viewer items
    *
    * @param parentItem
    */
   private void loadAllTreeItems_One(final TreeViewerItem parentItem, final TVIEquipmentView_Root rootItem) {

      if (parentItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

         if (equipmentItem.getEquipment().isCollate()) {

            // do not digg deeper, children are fetched when the parent item is expanded
            return;
         }

      } else if (parentItem instanceof TVIEquipmentView_Part) {

         // do not digg deeper, children are fetched when the parent item is expanded
         return;
      }

      final ArrayList<TreeViewerItem> allFetchedChildren = parentItem.getFetchedChildren();

      for (final TreeViewerItem childItem : allFetchedChildren) {

         // skip tour items, they do not have further children
         if (childItem instanceof TVIEquipmentView_Tour) {
            continue;
         }

         loadAllTreeItems_One(childItem, rootItem);
      }
   }

   private void onAction_DeleteEquipment() {

      final ITreeSelection structuredSelection = _equipmentViewer.getStructuredSelection();
      final List<?> allSelection = structuredSelection.toList();

      final Map<Long, Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_ByID();

      final List<Equipment> allSelectedEquipment = new ArrayList<>();

      for (final Object object : allSelection) {

         if (object instanceof final TVIEquipmentView_Equipment tviEquipment) {

            allSelectedEquipment.add(allAvailableEquipment.get(tviEquipment.getEquipment().getEquipmentId()));
         }
      }

      if (allSelectedEquipment.size() > 0) {

         // delete equipments

         EquipmentManager.equipment_DeleteEquipment(allSelectedEquipment);
      }
   }

   private void onAction_DeletePart() {

      final ITreeSelection structuredSelection = _equipmentViewer.getStructuredSelection();
      final List<?> allSelection = structuredSelection.toList();

      final List<EquipmentPart> allSelectedParts = new ArrayList<>();

      for (final Object object : allSelection) {

         if (object instanceof final TVIEquipmentView_Part partItem) {

            final EquipmentPart part = partItem.getPart();
            allSelectedParts.add(part);
         }
      }

      if (allSelectedParts.size() > 0) {

         // delete parts

         EquipmentManager.equipment_DeleteParts(allSelectedParts);
      }
   }

   private void onAction_DuplicateItem() {

      final ITreeSelection structuredSelection = _equipmentViewer.getStructuredSelection();
      final Object firstElement = structuredSelection.getFirstElement();

      if (firstElement instanceof final TVIEquipmentView_Equipment equipmentItem) {

         final Equipment selectedEquipment = equipmentItem.getEquipment();

         final DialogEquipment dialogEquipment = new DialogEquipment(

               _parent.getShell(),
               selectedEquipment,
               true);

         if (dialogEquipment.open() != Window.OK) {
            return;
         }

         final Equipment equipmentFromDialog = dialogEquipment.getEquipment();

         final String typeOld = selectedEquipment.getType();
         final String typeNew = equipmentFromDialog.getType();

         final Set<String> allModifiedTypes = new HashSet<>(Arrays.asList(typeOld, typeNew));

         TourDatabase.saveEntity(equipmentFromDialog, equipmentFromDialog.getEquipmentId(), Equipment.class);

         EquipmentManager.updateUntilDate_Equipment(allModifiedTypes);

         updateUI_ReloadViewer();

      } else if (firstElement instanceof final TVIEquipmentView_Part partItem) {

         final Equipment equipment = partItem.getEquipment();
         final EquipmentPart selectedPart = partItem.getPart();

         EquipmentPart partFromDialog = null;

         if (selectedPart.isItemType_Part()) {

            final DialogEquipmentPart dialog = new DialogEquipmentPart(

                  _parent.getShell(),
                  equipment,
                  selectedPart,
                  true);

            if (dialog.open() != Window.OK) {
               return;
            }

            partFromDialog = dialog.getPart();

         } else if (selectedPart.isItemType_Service()) {

            final DialogEquipmentService dialog = new DialogEquipmentService(

                  _parent.getShell(),
                  equipment,
                  selectedPart,
                  true);

            if (dialog.open() != Window.OK) {
               return;
            }

            partFromDialog = dialog.getService();
         }

         if (partFromDialog == null) {
            return;
         }

         final String typeOld = selectedPart.getType();
         final String typeNew = partFromDialog.getType();

         final Set<String> allModifiedTypes = new HashSet<>(Arrays.asList(typeOld, typeNew));

         // update model
         final EquipmentPart savedPart = TourDatabase.saveEntity(

               partFromDialog,
               partFromDialog.getPartId(),
               EquipmentPart.class);

         equipment.getParts().add(savedPart);

         EquipmentManager.updateUntilDate_Parts(equipment, allModifiedTypes);

         updateUI_ReloadViewer();
      }
   }

   private void onAction_EditItem() {

      final ITreeSelection structuredSelection = _equipmentViewer.getStructuredSelection();
      final Object firstElement = structuredSelection.getFirstElement();

      if (firstElement instanceof final TVIEquipmentView_Equipment equipmentItem) {

         final Equipment selectedEquipment = equipmentItem.getEquipment();

         final DialogEquipment dialogEquipment = new DialogEquipment(_parent.getShell(), selectedEquipment, false);

         if (dialogEquipment.open() != Window.OK) {
            return;
         }

         final Equipment equipmentFromDialog = dialogEquipment.getEquipment();

         final boolean areCollatedFieldsModified = selectedEquipment.isCollatedFieldsModified(equipmentFromDialog);

         final String typeOld = selectedEquipment.getType();
         final String typeNew = equipmentFromDialog.getType();

         final Set<String> allModifiedTypes = new HashSet<>(Arrays.asList(typeOld, typeNew));

         // update model
         selectedEquipment.updateFromOther(equipmentFromDialog);

         TourDatabase.saveEntity(selectedEquipment, selectedEquipment.getEquipmentId(), Equipment.class);

         if (areCollatedFieldsModified) {

            // date and/or type is modified -> update "until date"

            EquipmentManager.updateUntilDate_Equipment(allModifiedTypes);
         }

         updateUI_ReloadViewer();

      } else if (firstElement instanceof final TVIEquipmentView_Part partItem) {

         final Equipment equipment = partItem.getEquipment();
         final EquipmentPart selectedPart = partItem.getPart();

         EquipmentPart partFromDialog = null;

         if (selectedPart.isItemType_Part()) {

            final DialogEquipmentPart dialog = new DialogEquipmentPart(

                  _parent.getShell(),
                  equipment,
                  selectedPart,
                  false);

            if (dialog.open() != Window.OK) {
               return;
            }

            partFromDialog = dialog.getPart();

         } else if (selectedPart.isItemType_Service()) {

            final DialogEquipmentService dialog = new DialogEquipmentService(

                  _parent.getShell(),
                  equipment,
                  selectedPart,
                  false);

            if (dialog.open() != Window.OK) {
               return;
            }

            partFromDialog = dialog.getService();
         }

         if (partFromDialog == null) {
            return;
         }

         final boolean areCollatedFieldsModified = selectedPart.isCollatedFieldsModified(partFromDialog);

         final String typeOld = selectedPart.getType();
         final String typeNew = partFromDialog.getType();

         final Set<String> allModifiedTypes = new HashSet<>(Arrays.asList(typeOld, typeNew));

         // update model
         selectedPart.updateFromOther(partFromDialog);

         TourDatabase.saveEntity(selectedPart, selectedPart.getPartId(), EquipmentPart.class);

         if (areCollatedFieldsModified) {

            // date and/or type is modified -> update "until date"

            EquipmentManager.updateUntilDate_Parts(equipment, allModifiedTypes);
         }

         updateUI_ReloadViewer();
      }
   }

   private void onAction_NewEquipment() {

      final DialogEquipment dialogEquipment = new DialogEquipment(_parent.getShell(), null, false);

      if (dialogEquipment.open() != Window.OK) {
         return;
      }

      final Equipment equipmentFromDialog = dialogEquipment.getEquipment();

      updateAfterModified_Equipment(equipmentFromDialog);
   }

   private void onAction_NewPart() {

      final Equipment equipment = getEquipmentFromSelection();

      if (equipment == null) {
         return;
      }

      final DialogEquipmentPart partDialog = new DialogEquipmentPart(

            _parent.getShell(),
            equipment,
            null, //       EquipmentPart part
            false); //     isDuplicate)

      if (partDialog.open() != Window.OK) {
         return;
      }

      final EquipmentPart partFromDialog = partDialog.getPart();

      updateAfterModified_Part(equipment, partFromDialog);
   }

   private void onAction_NewService() {

      final Equipment equipment = getEquipmentFromSelection();

      if (equipment == null) {
         return;
      }

      final DialogEquipmentService serviceDialog = new DialogEquipmentService(

            _parent.getShell(),
            equipment,
            null,
            false);

      if (serviceDialog.open() != Window.OK) {
         return;
      }

      final EquipmentPart serviceFromDialog = serviceDialog.getService();

      // update model
      final EquipmentPart savedService = TourDatabase.saveEntity(

            serviceFromDialog,
            serviceFromDialog.getPartId(),
            EquipmentPart.class);

      equipment.getParts().add(savedService);

      final Set<String> allTypes = new HashSet<>(Arrays.asList(serviceFromDialog.getType()));

      EquipmentManager.updateUntilDate_Parts(equipment, allTypes);

      updateUI_ReloadViewer();
   }

   private void onAction_OnMouseSelect_ExpandCollapse() {

      _isBehaviour_OnSelect_ExpandCollapse = _actionOnMouseSelect_ExpandCollapse.isChecked();
   }

   private void onAction_SingleExpandCollapseOthers() {

      _isBehaviour_SingleExpand_CollapseOthers = _actionSingleExpand_CollapseOthers.isChecked();
   }

   private void onAction_ToggleCollatedTours() {

      final Object selection = _equipmentViewer.getStructuredSelection().getFirstElement();

      if (selection instanceof final TVIEquipmentView_Equipment equipmentItem) {

         final Equipment equipment = equipmentItem.getEquipment();

         final int numParts = equipment.getParts().size();

         if (numParts > 0) {

            MessageDialog.openInformation(_parent.getShell(),
                  "Toggle Collated Tours",
                  "An equipment which contains parts or services cannot collate tours itself, only the parts or services of this equipment can collate tours");

            return;
         }

         equipment.setIsCollate(!equipment.isCollate());

         updateAfterModified_Equipment(equipment);

      } else if (selection instanceof final TVIEquipmentView_Part partItem) {

         final EquipmentPart part = partItem.getPart();
         final Equipment equipment = partItem.getEquipment();

         part.setIsCollate(!part.isCollate());

         updateAfterModified_Part(equipment, part);
      }
   }

   private void onAction_ToggleEquipmentFilter(final Event event) {

      final boolean isForwards = UI.isCtrlKey(event) == false;

      if (_equipmentFilterType == EquipmentFilterType.ALL_IS_DISPLAYED) {

         if (isForwards) {
            setEquipmentFilter_WithTours();
         } else {
            setEquipmentFilter_NoTours();
         }

      } else if (_equipmentFilterType == EquipmentFilterType.EQUIPMENT_WITH_TOURS) {

         if (isForwards) {
            setEquipmentFilter_NoTours();
         } else {
            setEquipmentFilter_All();
         }

      } else {

         if (isForwards) {
            setEquipmentFilter_All();
         } else {
            setEquipmentFilter_WithTours();
         }
      }

      final Tree tree = _equipmentViewer.getTree();
      tree.setRedraw(false);
      {
         _equipmentViewer.refresh();
      }
      tree.setRedraw(true);

   }

   private void onColumnImage_OnPaintViewer(final Event event) {

      // skip other columns
      if (event.index != _columnIndex_EquipmentImage) {
         return;
      }

      switch (event.type) {
      case SWT.MeasureItem:

         // set row height to the selected image height
         event.height = _selectedTreeItemHeight;

         break;

      case SWT.PaintItem:

         /*
          * Paint equipment image
          */
         final TreeItem item = (TreeItem) event.item;
         final Object itemData = item.getData();

         Image equipmentImage = null;

         // skip other tree items
         try {

            if (itemData instanceof final TVIEquipmentView_Equipment equipmentItem) {

               final Equipment equipment = equipmentItem.getEquipment();
               equipmentImage = EquipmentManager.getEquipmentImage(equipment.getImageFilePath(), ImageSize.VIEW);

            } else if (itemData instanceof final TVIEquipmentView_Part partItem) {

               final EquipmentPart part = partItem.getPart();
               equipmentImage = EquipmentManager.getEquipmentImage(part.getImageFilePath(), ImageSize.VIEW);
            }

         } catch (final IOException e) {
            // ignore
         }

         if (equipmentImage != null && equipmentImage.isDisposed() == false) {

            UI.paintImage(

                  event,
                  equipmentImage,
                  _columnWidth_EquipmentImage,

                  _colDef_EquipmentImage.getColumnStyle(), //  horizontal alignment
                  SWT.CENTER, //                               vertical alignment

                  0 //                                         horizontal offset
            );

            break;
         }
      }
   }

   private void onColumnImage_Resize_SetWidthForImageColumn() {

      if (_colDef_EquipmentImage != null) {

         final TreeColumn treeColumn = _colDef_EquipmentImage.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_EquipmentImage = treeColumn.getWidth();
         }
      }
   }

   private void onEquipmentTree_MouseDown(final Event event) {

      _isMouseContextMenu = event.button == 3;
   }

   private void onEquipmentViewer_DoubleClick() {

      final Object selection = _equipmentViewer.getStructuredSelection().getFirstElement();

      if (selection instanceof TVIEquipmentView_Tour) {

         TourManager.getInstance().tourDoubleClickAction(EquipmentView.this, _tourDoubleClickState);

      } else if (selection != null) {

         // expand/collapse current item

         final TreeViewerItem treeItem = (TreeViewerItem) selection;

         expandCollapseItem(treeItem);
      }
   }

   private void onEquipmentViewer_Selection(final SelectionChangedEvent event) {

      if (_isMouseContextMenu) {
         return;
      }

      final TreeSelection allSelectedItems = (TreeSelection) (event.getSelection());
      final Object firstSelectedItem = allSelectedItems.getFirstElement();
      final int numSelectedItems = allSelectedItems.size();

      if (firstSelectedItem instanceof final TVIEquipmentView_Tour tourItem && numSelectedItems == 1) {

         // one tour is selected

         _postSelectionProvider.setSelection(new SelectionTourId(tourItem.tourId));

      } else if (firstSelectedItem instanceof TVIEquipmentView_Equipment
            || firstSelectedItem instanceof TVIEquipmentView_Equipment_Year
            || firstSelectedItem instanceof TVIEquipmentView_Equipment_Month
            || firstSelectedItem instanceof TVIEquipmentView_Part
            || firstSelectedItem instanceof TVIEquipmentView_Part_Year
            || firstSelectedItem instanceof TVIEquipmentView_Part_Month) {

         // a category item is selected, expand/collapse category items

         if (_isSelectedWithKeyboard == false) {

            // do not expand/collapse when keyboard is used -> unusable

            onSelect_CategoryItem((TreeSelection) event.getSelection());
         }

      } else {

         // multiple tours are selected

         final ArrayList<Long> tourIds = new ArrayList<>();

         for (final Object viewItem : allSelectedItems) {

            if (viewItem instanceof final TVIEquipmentView_Tour tourItem) {
               tourIds.add(tourItem.tourId);
            }
         }

         if (tourIds.size() > 0) {
            _postSelectionProvider.setSelection(new SelectionTourIds(tourIds));
         }
      }

      // reset state
      _isSelectedWithKeyboard = false;

      enableActions();
   }

   private void onSelect_CategoryItem(final TreeSelection treeSelection) {

      if (_isInExpandingSelection) {

         // prevent endless loops
         return;
      }

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
    */
   private void onSelect_CategoryItem_10_AutoExpandCollapse(final ITreeSelection treeSelection) {

      if (_isInCollapseAll) {

         // prevent auto expand
         return;
      }

      if (_isBehaviour_SingleExpand_CollapseOthers) {

         /*
          * run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         Display.getCurrent().asyncExec(new Runnable() {

            private long           __expandRunnableCounter = ++_expandRunnableCounter;

            private ITreeSelection __treeSelection         = treeSelection;

            @Override
            public void run() {

               // check if a newer expand event occurred
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               if (_equipmentViewer.getTree().isDisposed()) {
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
         final Tree tree = _equipmentViewer.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _equipmentViewer.getExpandedState(selectedTreePath);

            /*
             * collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _equipmentViewer.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _equipmentViewer.setExpandedState(treePath, false);
            }

            /*
             * expand and select selected folder
             */
            _equipmentViewer.setExpandedElements(expandedElements);
            _equipmentViewer.setSelection(treeSelection, true);

            if (_isBehaviour_OnSelect_ExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _equipmentViewer.setExpandedState(selectedTreePath, false);
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

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionDeletedTours) {

         reloadViewer();
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _equipmentViewer.getExpandedElements();
         final ITreeSelection selection = _equipmentViewer.getStructuredSelection();

         _equipmentViewer.getTree().dispose();

         createUI_10_EqipmentViewer(_viewerContainer);
         _viewerContainer.layout();

         reloadViewer_SetContent();

         _equipmentViewer.setExpandedElements(expandedElements);
         _equipmentViewer.setSelection(selection);
      }
      _viewerContainer.setRedraw(true);

      return _equipmentViewer;
   }

   @Override
   public void reloadViewer() {

      final Tree tree = _equipmentViewer.getTree();

      tree.setRedraw(false);
      {
         final Object[] expandedElements = _equipmentViewer.getExpandedElements();

         reloadViewer_SetContent();

         _equipmentViewer.setExpandedElements(expandedElements);
      }
      tree.setRedraw(true);
   }

   private void reloadViewer_SetContent() {

      _rootItem = new TVIEquipmentView_Root(_equipmentViewer, EquipmentViewerType.IS_EQUIPMENT_VIEWER);

      // first: load all tree items
      loadAllTreeItems();

      // second: update viewer
      _equipmentViewer.setInput(_rootItem);
   }

   private void restoreState() {

      // on mouse select -> expand/collapse
      _isBehaviour_OnSelect_ExpandCollapse = Util.getStateBoolean(_state, STATE_IS_ON_SELECT_EXPAND_COLLAPSE, true);
      _actionOnMouseSelect_ExpandCollapse.setChecked(_isBehaviour_OnSelect_ExpandCollapse);

      // single expand -> collapse others
      _isBehaviour_SingleExpand_CollapseOthers = Util.getStateBoolean(_state, STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, true);
      _actionSingleExpand_CollapseOthers.setChecked(_isBehaviour_SingleExpand_CollapseOthers);

      _equipmentFilterType = (EquipmentFilterType) Util.getStateEnum(_state, STATE_EQUIPMENT_FILTER, EquipmentFilterType.ALL_IS_DISPLAYED);

      restoreState_TreeItemHeight();

      EquipmentManager.setEquipmentImageSize_View(_selectedTreeItemHeight);

      updateUI_EquipmentFilter();
      updateToolTipState();
   }

   private void restoreState_TreeItemHeight() {

      final boolean isUseDefaultHeight = Util.getStateBoolean(_state,
            TourDataEditorView.STATE_EQUIPMENT_IS_USE_VIEWER_DEFAULT_HEIGHT,
            true);

      if (isUseDefaultHeight) {

         _selectedTreeItemHeight = _defaultTreeItemHeight;

      } else {

         _selectedTreeItemHeight = Util.getStateInt(_state,
               TourDataEditorView.STATE_EQUIPMENT_VIEWER_IMAGE_HEIGHT,
               _defaultTreeItemHeight,
               TourDataEditorView.STATE_EQUIPMENT_IMAGE_SIZE_MIN,
               TourDataEditorView.STATE_EQUIPMENT_IMAGE_SIZE_MAX);
      }
   }

   /**
    * Restore viewer state after the viewer is loaded
    */
   private void restoreState_Viewer() {

      /*
       * Expanded equipment categories
       */
      final long[] allStateItems = Util.getStateLongArray(_state, STATE_EXPANDED_ITEMS, null);
      if (allStateItems != null) {

         final List<TreePath> allViewerTreePaths = new ArrayList<>();

         final List<StateSegment[]> allStateSegmentsAllPaths = restoreState_Viewer_GetSegments(allStateItems);

         for (final StateSegment[] allStateSegmentsOnePath : allStateSegmentsAllPaths) {

            final List<Object> allPathSegments = new ArrayList<>();

            // start tree items with the root and go deeper with every segment
            List<TreeViewerItem> allTreeItems = _rootItem.getFetchedChildren();

            for (final StateSegment stateSegment : allStateSegmentsOnePath) {

               /*
                * This is somehow recursive as it goes deeper into the child tree items until there
                * are no children
                */
               allTreeItems = restoreState_Viewer_ExpandItem(allPathSegments, allTreeItems, stateSegment);
            }

            if (allPathSegments.size() > 0) {
               allViewerTreePaths.add(new TreePath(allPathSegments.toArray()));
            }
         }

         if (allViewerTreePaths.size() > 0) {

            final TreePath[] allPaths = allViewerTreePaths.toArray(new TreePath[allViewerTreePaths.size()]);

            _equipmentViewer.setExpandedTreePaths(allPaths);
         }
      }
   }

   /**
    * @param allPathSegments
    * @param allTreeItems
    * @param stateSegment
    *
    * @return Returns children when it could be expanded otherwise <code>null</code>
    */
   private List<TreeViewerItem> restoreState_Viewer_ExpandItem(final List<Object> allPathSegments,
                                                               final List<TreeViewerItem> allTreeItems,
                                                               final StateSegment stateSegment) {

      if (allTreeItems == null) {
         return null;
      }

      final long stateValue = stateSegment.itemData;

      if (stateSegment.itemType == STATE_ITEM_TYPE_EQUIPMENT) {

         for (final TreeViewerItem treeItem : allTreeItems) {

            if (treeItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

               final long itemValue = equipmentItem.getEquipment().getEquipmentId();

               if (itemValue == stateValue) {

                  allPathSegments.add(treeItem);

                  return equipmentItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.itemType == STATE_ITEM_TYPE_EQUIPMENT_YEAR) {

         for (final TreeViewerItem treeItem : allTreeItems) {

            if (treeItem instanceof final TVIEquipmentView_Equipment_Year yearItem) {

               final long itemValue = yearItem.getYear();

               if (itemValue == stateValue) {

                  allPathSegments.add(treeItem);

                  return yearItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.itemType == STATE_ITEM_TYPE_EQUIPMENT_MONTH) {

         for (final TreeViewerItem treeItem : allTreeItems) {

            if (treeItem instanceof final TVIEquipmentView_Equipment_Month monthItem) {

               final long itemValue = monthItem.getMonth();

               if (itemValue == stateValue) {

                  allPathSegments.add(treeItem);

                  return monthItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.itemType == STATE_ITEM_TYPE_PART) {

         for (final TreeViewerItem treeItem : allTreeItems) {

            if (treeItem instanceof final TVIEquipmentView_Part partItem) {

               final long itemValue = partItem.getPartID();

               if (itemValue == stateValue) {

                  allPathSegments.add(treeItem);

                  return partItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.itemType == STATE_ITEM_TYPE_PART_YEAR) {

         for (final TreeViewerItem treeItem : allTreeItems) {

            if (treeItem instanceof final TVIEquipmentView_Part_Year yearItem) {

               final long itemValue = yearItem.getYear();

               if (itemValue == stateValue) {

                  allPathSegments.add(treeItem);

                  return yearItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.itemType == STATE_ITEM_TYPE_PART_MONTH) {

         for (final TreeViewerItem treeItem : allTreeItems) {

            if (treeItem instanceof final TVIEquipmentView_Part_Month monthItem) {

               final long itemValue = monthItem.getMonth();

               if (itemValue == stateValue) {

                  allPathSegments.add(treeItem);

                  return monthItem.getFetchedChildren();
               }
            }
         }
      }

      return null;
   }

   /**
    * Convert state structure into a 'segment' structure.
    */
   private List<StateSegment[]> restoreState_Viewer_GetSegments(final long[] expandedItems) {

      final List<StateSegment[]> allTreePathSegments = new ArrayList<>();
      final List<StateSegment> currentSegments = new ArrayList<>();

      for (int itemIndex = 0; itemIndex < expandedItems.length;) {

         // ensure array bounds
         if (itemIndex + 1 >= expandedItems.length) {
            // this should not happen when data are not corrupted
            break;
         }

         final long itemType = expandedItems[itemIndex++];
         final long itemData = expandedItems[itemIndex++];

         if (itemType == STATE_ITEM_TYPE_SEPARATOR) {

            // a new tree path starts

            if (currentSegments.size() > 0) {

               // keep current tree path segments

               allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));

               // start a new path
               currentSegments.clear();
            }

         } else {

            // a new segment is available

            if (false
                  || itemType == STATE_ITEM_TYPE_EQUIPMENT
                  || itemType == STATE_ITEM_TYPE_EQUIPMENT_YEAR
                  || itemType == STATE_ITEM_TYPE_EQUIPMENT_MONTH
                  || itemType == STATE_ITEM_TYPE_PART
                  || itemType == STATE_ITEM_TYPE_PART_YEAR
                  || itemType == STATE_ITEM_TYPE_PART_MONTH) {

               currentSegments.add(new StateSegment(itemType, itemData));
            }
         }
      }

      if (currentSegments.size() > 0) {
         allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
      }

      return allTreePathSegments;
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionSingleExpand_CollapseOthers.isChecked());
      _state.put(STATE_IS_ON_SELECT_EXPAND_COLLAPSE, _actionOnMouseSelect_ExpandCollapse.isChecked());

      Util.setStateEnum(_state, STATE_EQUIPMENT_FILTER, _equipmentFilterType);

      saveState_ExpandedItems();
   }

   /**
    * Save state for expanded tree items
    */
   private void saveState_ExpandedItems() {

      final Object[] allVisibleAndExpandedItems = _equipmentViewer.getVisibleExpandedElements();

      if (allVisibleAndExpandedItems.length == 0) {

         Util.setState(_state, STATE_EXPANDED_ITEMS, new long[0]);

         return;
      }

      final LongArrayList allExpandedItemIDs = new LongArrayList();

      final TreePath[] allExpandedAndOpenedTreePaths = UI.getExpandedAndOpenedItems(
            allVisibleAndExpandedItems,
            _equipmentViewer.getExpandedTreePaths());

      for (final TreePath expandedPath : allExpandedAndOpenedTreePaths) {

         // start a new path, always set it twice to have an even structure
         allExpandedItemIDs.add(STATE_ITEM_TYPE_SEPARATOR);
         allExpandedItemIDs.add(STATE_ITEM_TYPE_SEPARATOR);

         final int numSegments = expandedPath.getSegmentCount();

         for (int segmentIndex = 0; segmentIndex < numSegments; segmentIndex++) {

            final Object segment = expandedPath.getSegment(segmentIndex);

            if (segment instanceof final TVIEquipmentView_Equipment treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_EQUIPMENT);
               allExpandedItemIDs.add(treeItem.getEquipment().getEquipmentId());

            } else if (segment instanceof final TVIEquipmentView_Equipment_Year treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_EQUIPMENT_YEAR);
               allExpandedItemIDs.add(treeItem.getYear());

            } else if (segment instanceof final TVIEquipmentView_Equipment_Month treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_EQUIPMENT_MONTH);
               allExpandedItemIDs.add(treeItem.getMonth());

            } else if (segment instanceof final TVIEquipmentView_Part treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_PART);
               allExpandedItemIDs.add(treeItem.getPartID());

            } else if (segment instanceof final TVIEquipmentView_Part_Year treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_PART_YEAR);
               allExpandedItemIDs.add(treeItem.getYear());

            } else if (segment instanceof final TVIEquipmentView_Part_Month treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_PART_MONTH);
               allExpandedItemIDs.add(treeItem.getMonth());
            }
         }
      }

      Util.setState(_state, STATE_EXPANDED_ITEMS, allExpandedItemIDs.toArray());
   }

   private void setCellColor(final ViewerCell cell, final Object element) {

      // set color

      if (element instanceof TVIEquipmentView_Equipment) {

         cell.setForeground(_colorContentCategory);

      } else if (element instanceof TVIEquipmentView_Part) {

         cell.setForeground(_colorContentSubCategory);

      } else if (element instanceof TVIEquipmentView_Part_Year) {

         cell.setForeground(_colorDateCategory);

      } else if (element instanceof TVIEquipmentView_Part_Month) {

         cell.setForeground(_colorDateSubCategory);

      } else {

         cell.setForeground(_colorTour);
      }
   }

   private void setEquipmentFilter_All() {

      _equipmentFilterType = EquipmentFilterType.ALL_IS_DISPLAYED;

      _actionToggleEquipmentFilter.setChecked(false);
      _actionToggleEquipmentFilter.setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
   }

   private void setEquipmentFilter_NoTours() {

      _equipmentFilterType = EquipmentFilterType.EQUIPMENT_WITHOUT_TOURS;

      _actionToggleEquipmentFilter.setChecked(true);
      _actionToggleEquipmentFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Filter_NoTours));
   }

   private void setEquipmentFilter_WithTours() {

      _equipmentFilterType = EquipmentFilterType.EQUIPMENT_WITH_TOURS;

      _actionToggleEquipmentFilter.setChecked(true);
      _actionToggleEquipmentFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Filter));
   }

   @Override
   public void setFocus() {

      _equipmentViewer.getTree().setFocus();
   }

   private void updateAfterModified_Equipment(final Equipment equipment) {

      final Set<String> allTypes = new HashSet<>(Arrays.asList(equipment.getType()));

      TourDatabase.saveEntity(equipment, equipment.getEquipmentId(), Equipment.class);

      EquipmentManager.updateUntilDate_Equipment(allTypes);

      updateUI_ReloadViewer();
   }

   private void updateAfterModified_Part(final Equipment equipment, final EquipmentPart part) {

      final EquipmentPart savedPart = TourDatabase.saveEntity(part, part.getPartId(), EquipmentPart.class);

      equipment.getParts().add(savedPart);

      final HashSet<String> allTypes = new HashSet<>(Arrays.asList(part.getType()));

      EquipmentManager.updateUntilDate_Parts(equipment, allTypes);

      updateUI_ReloadViewer();
   }

   private void updateColors() {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

// SET_FORMATTING_OFF

      _colorContentCategory      = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_CATEGORY);
      _colorContentSubCategory   = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_SUB_CATEGORY);
      _colorDateCategory         = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_DATE_CATEGORY);
      _colorDateSubCategory      = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_DATE_SUB_CATEGORY);
      _colorTour                 = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_TOUR);

// SET_FORMATTING_ON
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   private void updateToolTipState() {

// SET_FORMATTING_OFF

      _isShowToolTipInEquipment  = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_EQUIPMENT_EQUIPMENT);
      _isShowToolTipInTitle      = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_EQUIPMENT_TITLE);
      _isShowToolTipInTags       = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_EQUIPMENT_TAGS);

// SET_FORMATTING_ON
   }

   private void updateUI_EquipmentFilter() {

      if (_equipmentFilterType == EquipmentFilterType.ALL_IS_DISPLAYED) {

         setEquipmentFilter_All();

      } else if (_equipmentFilterType == EquipmentFilterType.EQUIPMENT_WITH_TOURS) {

         setEquipmentFilter_WithTours();

      } else {

         setEquipmentFilter_NoTours();
      }
   }

   private void updateUI_ReloadViewer() {

      // remove old equipment from cached tours
      EquipmentManager.clearCachedValues();

      // this MUST be called after clearCachedValues()
      EquipmentMenuManager.updateRecentEquipment();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
      TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED, this);

      // update UI
      reloadViewer();
   }

   /**
    * Update viewer after properties are modified in the slideout
    */
   public void updateUI_Viewer() {

      restoreState_TreeItemHeight();

      EquipmentManager.setEquipmentImageSize_View(_selectedTreeItemHeight);

      // ensure to keep column width otherwise the columns are resized to the default width
      _columnManager.saveState(_state);

      // prevent a selection which could expand/collapse a category item
      _isInExpandingSelection = true;
      {
         // the viewer must be recreated because a smaller height is not recognized with a refresh() method !!!
         recreateViewer(_equipmentViewer);
      }
      _isInExpandingSelection = false;
   }
}
