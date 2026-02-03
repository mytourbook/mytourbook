/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment.tour.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.dialog.MessageDialog_OnTop;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.StateSegment;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.equipment.EquipmentManager;
import net.tourbook.equipment.TVIEquipmentView_Equipment;
import net.tourbook.equipment.TVIEquipmentView_Item;
import net.tourbook.equipment.TVIEquipmentView_Part;
import net.tourbook.equipment.TVIEquipmentView_Root;
import net.tourbook.equipment.TVIEquipmentView_Tour;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Slideout for the tour equipment filter
 */
public class SlideoutTourEquipmentFilter extends AdvancedSlideout implements ITreeViewer {

   private static final String                         STATE_IS_LIVE_UPDATE           = "STATE_IS_LIVE_UPDATE";                  //$NON-NLS-1$
   private static final String                         STATE_SASH_WIDTH_CONTAINER     = "STATE_SASH_WIDTH_CONTAINER";            //$NON-NLS-1$
   private static final String                         STATE_SASH_WIDTH_TAG_CONTAINER = "STATE_SASH_WIDTH_TAG_CONTAINER";        //$NON-NLS-1$

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
   private static final String                         STATE_EXPANDED_ITEMS           = "STATE_EXPANDED_ITEMS";                  //$NON-NLS-1$

   /**
    * Using large numbers to easier debug and find issues
    */
   private static final int                            STATE_ITEM_TYPE_SEPARATOR      = -1;
   private static final int                            STATE_ITEM_TYPE_EQUIPMENT      = 1111;
   private static final int                            STATE_ITEM_TYPE_PART           = 2111;

   private static final Object[]                       EMPTY_LIST                     = new Object[] {};
   private static final long[]                         NO_EQUIPMENT                   = new long[] {};

   private static IDialogSettings                      _state;

   private final List<TourEquipmentFilterProfile>      _profiles                      = TourEquipmentFilterManager.getProfiles();

   private TableViewer                                 _profileViewer;
   private TourEquipmentFilterProfile                  _selectedProfile;

   private CheckboxTreeViewer                          _equipmentViewer;
   private TVIEquipmentView_Root                       _equipmentViewerRootItem;

   private CheckboxTableViewer                         _selectedEquipmentViewer;
   private List<SelectedEquipment>                     _allSelectedEquipmentItems     = new ArrayList<>();

   private ToolItem                                    _tourEquipmentFilterItem;

   private ModifyListener                              _defaultModifyListener;
   private SelectionListener                           _defaultSelectionListener;
   private ITourEventListener                          _tourEventListener;

   private boolean                                     _equipmentViewerItem_IsChecked;
   private boolean                                     _equipmentViewerItem_IsKeyPressed;
   private Object                                      _equipmentViewerItem_Data;

   private boolean                                     _selectedEquipmentViewerItem_IsChecked;
   private boolean                                     _selectedEquipmentViewerItem_IsKeyPressed;
   private Object                                      _selectedEquipmentViewerItem_Data;

   private boolean                                     _isInUpdateUI;
   private boolean                                     _isInUpdateUIAfterDelete;
   private boolean                                     _isLiveUpdate;

   private PixelConverter                              _pc;

   private ActionCollapseAllWithoutSelection           _actionCollapseAll;
   private ActionExpandAll                             _actionExpandAll;
   private ActionSelectedEquipment_CheckAllEquipment   _actionSelectedEquipment_CheckAll;
   private ActionSelectedEquipment_UncheckAllEquipment _actionSelectedEquipment_UncheckAll;

   /*
    * UI resources
    */
   private Image _imgEquipment_All;
   private Image _imgEquipment_Collated;
   private Image _imgEquipment_Part;
   private Image _imgEquipment_Part_Collate;
   private Image _imgEquipment_Service;
   private Image _imgEquipment_Service_Collate;

   /*
    * UI controls
    */
   private Button  _btnApply;
   private Button  _btnCopyProfile;
   private Button  _btnDeleteProfile;
   private Button  _btnNewProfile;
   private Button  _chkLiveUpdate;
   private Button  _rdoEquipmentOperator_OR;
   private Button  _rdoEquipmentOperator_AND;

   private Label   _lblAllEquipment;
   private Label   _lblProfileName;
   private Label   _lblSelectEquipment;
   private Label   _lblEquipmentOperator;

   private Text    _txtProfileName;

   private ToolBar _toolBarAllEquipment;
   private ToolBar _toolBarSelectedEquipment;

   private class ActionCollapseAllWithoutSelection extends ActionCollapseAll {

      public ActionCollapseAllWithoutSelection(final ITreeViewer treeViewerProvider) {
         super(treeViewerProvider);
      }

      @Override
      public void run() {

//         _isInCollapseAll = true;
         {
            super.run();
         }
//         _isInCollapseAll = false;
      }

   }

   private class ActionSelectedEquipment_CheckAllEquipment extends Action {

      public ActionSelectedEquipment_CheckAllEquipment() {

         super();

         setToolTipText(Messages.Slideout_TourTagFilter_Action_CheckAllTags_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Checked));
      }

      @Override
      public void run() {
         onSelectedEquipment_Checkbox_CheckAll();
      }
   }

   private class ActionSelectedEquipment_UncheckAllEquipment extends Action {

      public ActionSelectedEquipment_UncheckAllEquipment() {

         super();

         setToolTipText(Messages.Slideout_TourTagFilter_Action_UncheckAllTags_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Checkbox_Uncheck));
      }

      @Override
      public void run() {
         onSelectedEquipment_Checkbox_UncheckAll();
      }
   }

   /**
    * Comparator is sorting the tree items
    */
   private class AllEquipment_Comparator extends ViewerComparator {
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
   private class AllEquipment_Comparer implements IElementComparer {

      @Override
      public boolean equals(final Object o1, final Object o2) {

// SET_FORMATTING_OFF

         if (o1 == o2) {

            return true;

         } else if (o1 instanceof final TVIEquipmentView_Equipment item1
                 && o2 instanceof final TVIEquipmentView_Equipment item2) {

            return item1.getEquipmentID() == item2.getEquipmentID();

         } else if (o1 instanceof final TVIEquipmentView_Part item1
                 && o2 instanceof final TVIEquipmentView_Part item2) {

            return item1.getPartID() == item2.getPartID();
         }

// SET_FORMATTING_ON

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }

   }

   private class AllEquipment_ContentProvider implements ITreeContentProvider {

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _equipmentViewerRootItem.getFetchedChildrenAsArray();
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

   private class ProfileComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final TourEquipmentFilterProfile profile1 = (TourEquipmentFilterProfile) e1;
         final TourEquipmentFilterProfile profile2 = (TourEquipmentFilterProfile) e2;

         return profile1.name.compareTo(profile2.name);
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }
   }

   private class ProfileProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _profiles.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private class SelectedEquipment {

      Equipment equipment;

      long      equipmentId;
      String    equipmentName;

      public SelectedEquipment(final Equipment equipment) {

         this.equipment = equipment;
         this.equipmentId = equipment.getEquipmentId();
         this.equipmentName = equipment.getName();
      }
   }

   /**
    * Comparator is sorting the tree items
    */
   private class SelectedEquipment_Comparator extends ViewerComparator {
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         if (obj1 instanceof final SelectedEquipment item1
               && obj2 instanceof final SelectedEquipment item2) {

            // sort equipment by name

            final Equipment equipment1 = item1.equipment;
            final Equipment equipment2 = item2.equipment;

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
         }

         return 0;
      }
   }

   private class SelectedEquipment_Provider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allSelectedEquipmentItems.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   /**
    * @param toolItem
    * @param state
    */
   public SlideoutTourEquipmentFilter(final ToolItem toolItem,
                                      final IDialogSettings state) {

      super(toolItem.getParent(),
            state,
            new int[] { 700, 400, 700, 400 });

      _tourEquipmentFilterItem = toolItem;
      _state = state;

      setShellFadeOutDelaySteps(30);

      setTitleText("Tour Equipment Filter");
      setTitleImage(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Filter));
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (tourEventId == TourEventId.TAG_STRUCTURE_CHANGED) {

            if (_profileViewer != null && _profileViewer.getTable().isDisposed()) {
               return;
            }

            updateEquipmentModel();

            // reselect profile
            onProfile_Select(false);
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

      _actionExpandAll = new ActionExpandAll(this);
      _actionCollapseAll = new ActionCollapseAllWithoutSelection(this);
      _actionSelectedEquipment_CheckAll = new ActionSelectedEquipment_CheckAllEquipment();
      _actionSelectedEquipment_UncheckAll = new ActionSelectedEquipment_UncheckAllEquipment();
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      // reset to a valid state when the slideout is opened again
      _selectedProfile = null;

      initUI(parent);

      createUI(parent);

      createActions();
      fillToolbar();

      addTourEventListener();

      // load profile viewer
      _profileViewer.setInput(new Object());

      // load equipment viewer
      updateEquipmentModel();

      restoreState();
      restoreState_Viewer();

      enableControls();
   }

   private void createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         final Composite sashContainer = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(sashContainer);
         GridLayoutFactory.swtDefaults().applyTo(sashContainer);
         {
            // left part
            final Composite containerProfiles = createUI_200_Profiles(sashContainer);

            // sash
            final Sash sash = new Sash(sashContainer, SWT.VERTICAL);

            // right part
            final Composite containerEquipment = createUI_300_Equipment(sashContainer);

            new SashLeftFixedForm(
                  sashContainer,
                  containerProfiles,
                  sash,
                  containerEquipment,
                  _state,
                  STATE_SASH_WIDTH_CONTAINER,
                  30);
         }

         createUI_800_Actions(shellContainer);
      }
   }

   private Composite createUI_200_Profiles(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .extendedMargins(0, 3, 0, 0)
            .applyTo(container);
      {
         {
            // label: Profiles

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_TourFilter_Label_Profiles);
         }

         createUI_210_ProfileViewer(container);
      }

      return container;
   }

   private void createUI_210_ProfileViewer(final Composite parent) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(_pc.convertWidthInCharsToPixels(15), _pc.convertHeightInCharsToPixels(8))
            .applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * create table
       */
      final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

      table.setLayout(new TableLayout());

      // !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
      table.setHeaderVisible(true);

      _profileViewer = new TableViewer(table);

      /*
       * create columns
       */
      TableViewerColumn tvc;
      TableColumn tc;

      {
         // Column: Profile name

         tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final TourEquipmentFilterProfile profile = (TourEquipmentFilterProfile) cell.getElement();

               cell.setText(profile.name);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
      }
      {
         // Column: Number of checked equipment

         tvc = new TableViewerColumn(_profileViewer, SWT.TRAIL);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_TourTagFilter_Column_Tags_Checked);
         tc.setToolTipText(Messages.Slideout_TourTagFilter_Column_Tags_Checked_Tooltip);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final TourEquipmentFilterProfile profile = (TourEquipmentFilterProfile) cell.getElement();
               final int numEquipment = profile.allEquipmentFilterIDs.size();

               cell.setText(numEquipment == 0
                     ? UI.EMPTY_STRING
                     : Integer.toString(numEquipment));
            }
         });
         tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 6));
      }
      {
         // Column: Number of unchecked equipment

         tvc = new TableViewerColumn(_profileViewer, SWT.TRAIL);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_TourTagFilter_Column_Tags_Unchecked);
         tc.setToolTipText(Messages.Slideout_TourTagFilter_Column_Tags_Unchecked_Tooltip);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final TourEquipmentFilterProfile profile = (TourEquipmentFilterProfile) cell.getElement();
               final int numUncheckedEquipment = profile.allEquipmentFilterIDs_Unchecked.size();

               cell.setText(numUncheckedEquipment == 0
                     ? UI.EMPTY_STRING
                     : Integer.toString(numUncheckedEquipment));
            }
         });
         tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 6));
      }
      {
         // Column: Combine equipment with OR or AND

         tvc = new TableViewerColumn(_profileViewer, SWT.TRAIL);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_TourTagFilter_Column_CombineTags);
         tc.setToolTipText(Messages.Slideout_TourTagFilter_Column_CombineTags_Tooltip);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final TourEquipmentFilterProfile profile = (TourEquipmentFilterProfile) cell.getElement();
               final int numEquipment = profile.allEquipmentFilterIDs.size();

               final String combineEquipment = profile.isOrOperator
                     ? Messages.Slideout_TourTagFilter_CombineTags_With_OR
                     : Messages.Slideout_TourTagFilter_CombineTags_With_AND;

               cell.setText(numEquipment > 1

                     // combine equipment requires at least 2 equipment
                     ? combineEquipment

                     : UI.EMPTY_STRING);
            }
         });
         tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 10));
      }

      /*
       * create table viewer
       */
      _profileViewer.setContentProvider(new ProfileProvider());
      _profileViewer.setComparator(new ProfileComparator());

      _profileViewer.addSelectionChangedListener(selectionChangedEvent -> onProfile_Select(true));

      _profileViewer.addDoubleClickListener(doubleClickEvent -> {

         // set focus to  profile name
         _txtProfileName.setFocus();
         _txtProfileName.selectAll();
      });

      _profileViewer.getTable().addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL) {
            onProfile_Delete();
         }
      }));
   }

   private Composite createUI_300_Equipment(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .extendedMargins(3, 0, 0, 0)
            .applyTo(container);
      {
         createUI_310_ProfileName(container);
         createUI_320_EquipmentContainer(container);
      }

//      /**
//       * Very Important !
//       * <p>
//       * Do a layout NOW, otherwise the initial profile container is using the whole width of the
//       * slideout :-(
//       */
//      container.layout(true, true);

      return container;
   }

   private void createUI_310_ProfileName(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            // Label: Profile name
            _lblProfileName = new Label(container, SWT.NONE);
            _lblProfileName.setText(Messages.Slideout_TourFilter_Label_ProfileName);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblProfileName);
         }
         {
            // Text: Profile name
            _txtProfileName = new Text(container, SWT.BORDER);
            _txtProfileName.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
                  .applyTo(_txtProfileName);
         }
      }
   }

   private void createUI_320_EquipmentContainer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .indent(0, 10)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         // left part
         final Composite containerEquipmentList = createUI_330_SelectedEquipment(container);

         // sash
         final Sash sash = new Sash(container, SWT.VERTICAL);

         // right part
         final Composite containerEquipmentViewer = createUI_340_AllEquipment(container);

         new SashLeftFixedForm(
               container,
               containerEquipmentList,
               sash,
               containerEquipmentViewer,
               _state,
               STATE_SASH_WIDTH_TAG_CONTAINER,
               40);
      }
   }

   private Composite createUI_330_SelectedEquipment(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .spacing(0, 2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_332_SelectedEquipment_Header(container);
         createUI_334_SelectedEquipment_Viewer(container);
         createUI_336_SelectedEquipment_Options(container);
      }

      return container;
   }

   private void createUI_332_SelectedEquipment_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            // Label: Selected equipment

            _lblSelectEquipment = new Label(container, SWT.NONE);
            _lblSelectEquipment.setText("Se&lected Equipment");
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_lblSelectEquipment);
         }
         {
            // toolbar
            _toolBarSelectedEquipment = new ToolBar(container, SWT.FLAT);
         }
      }
   }

   private void createUI_334_SelectedEquipment_Viewer(final Composite parent) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * Create table
       */
      final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.CHECK);

      table.setLayout(new TableLayout());

      table.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

         /*
          * The selected equipment viewer selection event can have another selection !!!
          */

         _selectedEquipmentViewerItem_IsChecked = selectionEvent.detail == SWT.CHECK;
         _selectedEquipmentViewerItem_Data = selectionEvent.item.getData();
      }));

      table.addKeyListener(new KeyAdapter() {

         @Override
         public void keyPressed(final KeyEvent e) {

            if (e.keyCode == SWT.DEL) {

               onSelectedEquipment_Delete();

            } else {

               _selectedEquipmentViewerItem_IsKeyPressed = true;
            }
         }
      });

      layoutContainer.addTraverseListener(traverseEvent -> onTraverse_SelectedEquipmentContainer(table, traverseEvent));

      _selectedEquipmentViewer = new CheckboxTableViewer(table);

      /*
       * Create columns
       */
      TableViewerColumn tvc;
      TableColumn tc;

      {
         // Column: Equipment name

         tvc = new TableViewerColumn(_selectedEquipmentViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final SelectedEquipment selectedEquipment = (SelectedEquipment) cell.getElement();

               cell.setText(selectedEquipment.equipmentName);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
      }

      /*
       * create table viewer
       */
      _selectedEquipmentViewer.setContentProvider(new SelectedEquipment_Provider());
      _selectedEquipmentViewer.setComparator(new SelectedEquipment_Comparator());

      _selectedEquipmentViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectedEquipment_Select(selectionChangedEvent));
   }

   private void createUI_336_SelectedEquipment_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            // Label: Equipment operator
            _lblEquipmentOperator = new Label(container, SWT.NONE);
            _lblEquipmentOperator.setText("&Combine equipment with");
         }

         final Composite containerOperator = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerOperator);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerOperator);
         {
            {
               /*
                * Radio: OR
                */
               _rdoEquipmentOperator_OR = new Button(containerOperator, SWT.RADIO);
               _rdoEquipmentOperator_OR.setText(Messages.Slideout_TourTagFilter_Radio_TagOperator_OR);
               _rdoEquipmentOperator_OR.setToolTipText("A tour is displayed when it contains at least ONE of the selected equipment");
               _rdoEquipmentOperator_OR.addSelectionListener(_defaultSelectionListener);
            }
            {
               /*
                * Radio: AND
                */
               _rdoEquipmentOperator_AND = new Button(containerOperator, SWT.RADIO);
               _rdoEquipmentOperator_AND.setText(Messages.Slideout_TourTagFilter_Radio_TagOperator_AND);
               _rdoEquipmentOperator_AND.setToolTipText("A tour is displayed when it contains ALL selected equipment");
               _rdoEquipmentOperator_AND.addSelectionListener(_defaultSelectionListener);
            }
         }
      }
   }

   private Composite createUI_340_AllEquipment(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory
            .fillDefaults()
            .spacing(0, 2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_342_AllEquipment_Header(container);
         createUI_344_AllEquipment_Viewer(container);
      }

      return container;
   }

   private void createUI_342_AllEquipment_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            // Label: All Equipment
            _lblAllEquipment = new Label(container, SWT.NONE);
            _lblAllEquipment.setText("&Available Equipment");
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_lblAllEquipment);
         }
         {
            // toolbar
            _toolBarAllEquipment = new ToolBar(container, SWT.FLAT);
         }
      }
   }

   private void createUI_344_AllEquipment_Viewer(final Composite parent) {

      /*
       * create tree layout
       */

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(200, 100)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * create viewer tree
       */
      final Tree tree = new Tree(
            layoutContainer,
            SWT.H_SCROLL | SWT.V_SCROLL
                  | SWT.MULTI
                  | SWT.CHECK
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(false);

      tree.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

         /*
          * The equipment treeviewer selection event can have another selection !!!
          */

         _equipmentViewerItem_IsChecked = selectionEvent.detail == SWT.CHECK;

         if (_equipmentViewerItem_IsChecked) {

            /*
             * Item can be null when <ctrl>+A is pressed !!!
             */
            final Widget item = selectionEvent.item;

            _equipmentViewerItem_Data = item.getData();
         }
      }));

      tree.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> _equipmentViewerItem_IsKeyPressed = true));

      layoutContainer.addTraverseListener(traverseEvent -> onTraverse_EquipmentContainer(tree, traverseEvent));

      /*
       * Create viewer
       */
      _equipmentViewer = new CheckboxTreeViewer(tree);

      _equipmentViewer.setUseHashlookup(true);

      _equipmentViewer.setContentProvider(new AllEquipment_ContentProvider());
      _equipmentViewer.setComparator(new AllEquipment_Comparator());
      _equipmentViewer.setComparer(new AllEquipment_Comparer());

      _equipmentViewer.addCheckStateListener(checkStateChangedEvent -> onEquipment_Checked(checkStateChangedEvent));
      _equipmentViewer.addSelectionChangedListener(selectionChangedEvent -> onEquipment_Select(selectionChangedEvent));

      /*
       * Create 1st column
       */
      final TreeViewerColumn tvc = new TreeViewerColumn(_equipmentViewer, SWT.LEAD);
      final TreeColumn tvcColumn = tvc.getColumn();

      treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));

      tvc.setLabelProvider(new StyledCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) element;

            final long numTours = viewItem.numTours_IsCollated;

            final StyledString styledString = new StyledString();

            if (viewItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

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

            } else {

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.TOUR_STYLER);
            }

            cell.setText(styledString.getString());
            cell.setStyleRanges(styledString.getStyleRanges());
         }
      });

   }

   private void createUI_800_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);

      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         createUI_810_ProfileActions(container);
         createUI_820_FilterActions(container);
      }

      /**
       * Sometimes (but not always) the action buttons are more than 200px wide, this layout request
       * may help to force the correct size.
       * <p>
       * It's difficult to debug because sometimes it occures and sometimes not
       */
      container.computeSize(SWT.DEFAULT, SWT.DEFAULT);

   }

   private void createUI_810_ProfileActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Button: New
             */
            _btnNewProfile = new Button(container, SWT.PUSH);
            _btnNewProfile.setText(Messages.Slideout_TourFilter_Action_AddProfile);
            _btnNewProfile.setToolTipText(Messages.Slideout_TourTagFilter_Action_AddProfile_Tooltip);
            _btnNewProfile.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Add()));

            // set button default width
            UI.setButtonLayoutData(_btnNewProfile);
         }
         {
            /*
             * Button: Copy
             */
            _btnCopyProfile = new Button(container, SWT.PUSH);
            _btnCopyProfile.setText(Messages.Slideout_TourFilter_Action_CopyProfile);
            _btnCopyProfile.setToolTipText(Messages.Slideout_TourFilter_Action_CopyProfile_Tooltip);
            _btnCopyProfile.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Copy()));

            // set button default width
            UI.setButtonLayoutData(_btnCopyProfile);
         }
         {
            /*
             * Button: Delete
             */
            _btnDeleteProfile = new Button(container, SWT.PUSH);
            _btnDeleteProfile.setText(Messages.Slideout_TourFilter_Action_DeleteProfile);
            _btnDeleteProfile.setToolTipText(Messages.Slideout_TourFilter_Action_DeleteProfile_Tooltip);
            _btnDeleteProfile.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Delete()));

            // set button default width
            UI.setButtonLayoutData(_btnDeleteProfile);
         }
      }
   }

   private void createUI_820_FilterActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Checkbox: live update
             */
            _chkLiveUpdate = new Button(container, SWT.CHECK);
            _chkLiveUpdate.setText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate);
            _chkLiveUpdate.setToolTipText(Messages.Slideout_TourTagFilter_Checkbox_IsLiveUpdate_Tooltip);
            _chkLiveUpdate.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> doLiveUpdate()));

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_chkLiveUpdate);
         }
         {
            /*
             * Button: Apply
             */
            _btnApply = new Button(container, SWT.PUSH);
            _btnApply.setText(Messages.Slideout_TourFilter_Action_Apply);
            _btnApply.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> TourEquipmentFilterManager
                  .fireFilterModifyEvent()));

            // set button default width
            UI.setButtonLayoutData(_btnApply);
         }
      }
   }

   private void doLiveUpdate() {

      _isLiveUpdate = _chkLiveUpdate.getSelection();

      enableControls();

      fireModifyEvent();
   }

   private void enableControls() {

      final int numCheckedSelectedEquipmentItems = _selectedEquipmentViewer.getCheckedElements().length;
      final int numSelectedEquipmentItems = _allSelectedEquipmentItems.size();

      final boolean isProfileSelected = _selectedProfile != null;
      final boolean canCheckEquipment = numSelectedEquipmentItems > 0 && numCheckedSelectedEquipmentItems < numSelectedEquipmentItems;
      final boolean canUncheckEquipment = numSelectedEquipmentItems > 0 && numCheckedSelectedEquipmentItems > 0;
      final boolean canSetEquipmentOperator = isProfileSelected && numCheckedSelectedEquipmentItems > 1;

      _btnApply.setEnabled(isProfileSelected && _isLiveUpdate == false);
      _btnCopyProfile.setEnabled(isProfileSelected);
      _btnDeleteProfile.setEnabled(isProfileSelected);

      _actionCollapseAll.setEnabled(isProfileSelected);
      _actionExpandAll.setEnabled(isProfileSelected);
      _actionSelectedEquipment_CheckAll.setEnabled(isProfileSelected && canCheckEquipment);
      _actionSelectedEquipment_UncheckAll.setEnabled(isProfileSelected && canUncheckEquipment);

      _chkLiveUpdate.setEnabled(isProfileSelected);

      _lblAllEquipment.setEnabled(isProfileSelected);
      _lblProfileName.setEnabled(isProfileSelected);
      _lblSelectEquipment.setEnabled(isProfileSelected);
      _lblEquipmentOperator.setEnabled(canSetEquipmentOperator);

      _rdoEquipmentOperator_AND.setEnabled(canSetEquipmentOperator);
      _rdoEquipmentOperator_OR.setEnabled(canSetEquipmentOperator);

      _selectedEquipmentViewer.getTable().setEnabled(isProfileSelected);
      _equipmentViewer.getTree().setEnabled(isProfileSelected);

      _txtProfileName.setEnabled(isProfileSelected);
   }

   /**
    * set the toolbar action after the {@link #_equipmentViewer} is created
    */
   private void fillToolbar() {

      /*
       * Toolbar: Selected equipment
       */
      final ToolBarManager tbmSelectedEquipment = new ToolBarManager(_toolBarSelectedEquipment);

      tbmSelectedEquipment.add(_actionSelectedEquipment_CheckAll);
      tbmSelectedEquipment.add(_actionSelectedEquipment_UncheckAll);

      tbmSelectedEquipment.update(true);

      /*
       * Toolbar: All equipment
       */
      final ToolBarManager tbmAllEquipment = new ToolBarManager(_toolBarAllEquipment);

      tbmAllEquipment.add(_actionExpandAll);
      tbmAllEquipment.add(_actionCollapseAll);

      tbmAllEquipment.update(true);
   }

   /**
    * Fire modify event only when live update is selected
    */
   private void fireModifyEvent() {

      if (_isLiveUpdate) {
         TourEquipmentFilterManager.fireFilterModifyEvent();
      }
   }

   private long[] getEquipmentIDs_FromEquipmentViewer() {

      final LongHashSet equipmentIDs = new LongHashSet();

      final Object[] checkedElements = _equipmentViewer.getCheckedElements();

      for (final Object object : checkedElements) {

         if (object instanceof final TVIEquipmentView_Equipment equipmentItem) {

            final long equipmentID = equipmentItem.getEquipmentID();

            equipmentIDs.add(equipmentID);
         }
      }

      return equipmentIDs.toArray();
   }

   private long[] getEquipmentIDs_FromSelectedEquipment_Checked() {

      final LongHashSet equipmentIDs = new LongHashSet();

      final Object[] checkedElements = _selectedEquipmentViewer.getCheckedElements();

      for (final Object object : checkedElements) {

         if (object instanceof final SelectedEquipment selectedEquipment) {
            equipmentIDs.add(selectedEquipment.equipmentId);
         }
      }

      return equipmentIDs.toArray();
   }

   private long[] getEquipmentIDs_FromSelectedEquipment_Unchecked() {

      final Object[] allCheckedEquipment = _selectedEquipmentViewer.getCheckedElements();
      final LongHashSet allUncheckedEquipmentIDs = new LongHashSet();

      for (final SelectedEquipment selectedEquipmentItem : _allSelectedEquipmentItems) {

         final long equipmentID = selectedEquipmentItem.equipmentId;
         boolean isChecked = false;

         for (final Object item : allCheckedEquipment) {

            if (item instanceof SelectedEquipment) {

               final SelectedEquipment selectedEquipmentChecked = (SelectedEquipment) item;

               if (equipmentID == selectedEquipmentChecked.equipmentId) {
                  isChecked = true;
                  break;
               }
            }
         }

         if (!isChecked) {
            allUncheckedEquipmentIDs.add(equipmentID);
         }
      }

      return allUncheckedEquipmentIDs.toArray();
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _tourEquipmentFilterItem.getBounds();
      final Point itemDisplayPosition = _tourEquipmentFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _equipmentViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

// SET_FORMATTING_OFF

      _imgEquipment_All             = TourbookPlugin.getImage(Images.Equipment_Only);
      _imgEquipment_Collated        = TourbookPlugin.getImage(Images.Equipment_Collated);
      _imgEquipment_Part            = TourbookPlugin.getImage(Images.Equipment_Part);
      _imgEquipment_Part_Collate    = TourbookPlugin.getImage(Images.Equipment_Part_Collated);
      _imgEquipment_Service         = TourbookPlugin.getImage(Images.Equipment_Service);
      _imgEquipment_Service_Collate = TourbookPlugin.getImage(Images.Equipment_Service_Collated);

// SET_FORMATTING_ON

      parent.addDisposeListener(disposeEvent -> onDisposeSlideout());

      _defaultModifyListener = modifyEvent -> onProfile_Modify();

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> {
         onProfile_Modify();
         fireModifyEvent();
      });
   }

   /**
    * Load all tree items that expandable items do show the number of items
    */
   private void loadAllTreeItems() {

      // get all equipment viewer items

      final List<TreeViewerItem> allRootItems = _equipmentViewerRootItem.getFetchedChildren();

      for (final TreeViewerItem rootItem : allRootItems) {

         // is recursive !!!
         loadAllTreeItems_One(rootItem, _equipmentViewerRootItem);
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

         final Equipment equipment = equipmentItem.getEquipment();

         if (equipment.isCollate()) {

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

   private void onDisposeSlideout() {

// SET_FORMATTING_OFF

      _imgEquipment_All             .dispose();
      _imgEquipment_Collated        .dispose();
      _imgEquipment_Part            .dispose();
      _imgEquipment_Part_Collate    .dispose();
      _imgEquipment_Service         .dispose();
      _imgEquipment_Service_Collate .dispose();

// SET_FORMATTING_ON

      saveState();
   }

   private void onEquipment_Checked(final CheckStateChangedEvent checkStateChangedEvent) {

      update_FromEquipmentViewer();
   }

   private void onEquipment_Select(final SelectionChangedEvent event) {

      if (_equipmentViewerItem_IsKeyPressed) {

         // ignore when selected with keyboard

         // reset state
         _equipmentViewerItem_IsKeyPressed = false;

         return;
      }

      Object selection;

      if (_equipmentViewerItem_IsChecked) {

         // a checkbox is checked

         selection = _equipmentViewerItem_Data;

      } else {

         selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
      }

      if (selection instanceof final TVIEquipmentView_Equipment equipmentItem) {

         // equipment is selected

         // toggle equipment
         if (_equipmentViewerItem_IsChecked == false) {

            // equipment is selected and NOT the checkbox !!!

            final boolean isChecked = _equipmentViewer.getChecked(equipmentItem);

            _equipmentViewer.setChecked(equipmentItem, !isChecked);
         }

         update_FromEquipmentViewer();
      }
   }

   @Override
   protected void onFocus() {

      if (_selectedProfile != null
            && _selectedProfile.name != null
            && _selectedProfile.name.startsWith(Messages.Tour_Filter_Default_ProfileName)) {

         // default profile is selected, make it easy to rename it

         _txtProfileName.selectAll();
         _txtProfileName.setFocus();

      } else if (_selectedProfile == null) {

         _btnNewProfile.setFocus();
      }
   }

   private void onProfile_Add() {

      final TourEquipmentFilterProfile filterProfile = new TourEquipmentFilterProfile();

      // update model
      _profiles.add(filterProfile);

      // update viewer
      _profileViewer.refresh();

      // select new profile
      selectProfile(filterProfile);

      _txtProfileName.setFocus();
   }

   private void onProfile_Copy() {

      if (_selectedProfile == null) {
         // ignore
         return;
      }

      final TourEquipmentFilterProfile filterProfile = _selectedProfile.clone();

      // update model
      _profiles.add(filterProfile);

      // update viewer
      _profileViewer.refresh();

      // select new profile
      selectProfile(filterProfile);

      _txtProfileName.setFocus();
   }

   private void onProfile_Delete() {

      if (_selectedProfile == null) {
         // ignore
         return;
      }

      /*
       * Confirm deletion
       */
      boolean isDeleteProfile = false;
      setIsKeepOpenInternally(true);
      {
         MessageDialog_OnTop dialog = new MessageDialog_OnTop(

               getToolTipShell(),

               Messages.Slideout_TourFilter_Confirm_DeleteProfile_Title,
               null, // no title image

               NLS.bind(Messages.Slideout_TourFilter_Confirm_DeleteProfile_Message, _selectedProfile.name),
               MessageDialog.CONFIRM,

               0, // default index

               Messages.App_Action_DeleteProfile,
               Messages.App_Action_Cancel);

         dialog = dialog.withStyleOnTop();

         if (dialog.open() == IDialogConstants.OK_ID) {
            isDeleteProfile = true;
         }
      }
      setIsKeepOpenInternally(false);

      if (isDeleteProfile == false) {
         return;
      }

      // keep currently selected position
      final int lastIndex = _profileViewer.getTable().getSelectionIndex();

      // update model
      _profiles.remove(_selectedProfile);
      TourEquipmentFilterManager.setSelectedProfile(null);

      // update UI
      _profileViewer.remove(_selectedProfile);

      /*
       * Select another filter at the same position
       */
      final int numFilters = _profiles.size();
      final int nextFilterIndex = Math.min(numFilters - 1, lastIndex);

      final Object nextSelectedProfile = _profileViewer.getElementAt(nextFilterIndex);
      if (nextSelectedProfile == null) {

         // all profiles are deleted

         _selectedProfile = null;

         updateEquipment_EquipmentViewer(NO_EQUIPMENT);
         updateEquipment_SelectedEquipment(NO_EQUIPMENT, NO_EQUIPMENT);

         enableControls();

         fireModifyEvent();

      } else {

         selectProfile((TourEquipmentFilterProfile) nextSelectedProfile);
      }

      // set focus back to the viewer
      _profileViewer.getTable().setFocus();
   }

   private void onProfile_Modify() {

      if (_isInUpdateUI) {
         return;
      }

      if (_selectedProfile == null) {
         return;
      }

      _selectedProfile.name = _txtProfileName.getText();
      _selectedProfile.isOrOperator = _rdoEquipmentOperator_OR.getSelection();

      _profileViewer.refresh();
   }

   /**
    * @param isCheckOldProfile
    *           When <code>true</code> then the old profile is checked if it is already selected
    */
   private void onProfile_Select(final boolean isCheckOldProfile) {

      TourEquipmentFilterProfile selectedProfile = null;

      // get selected profile from viewer
      final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
      final Object firstElement = selection.getFirstElement();
      if (firstElement != null) {
         selectedProfile = (TourEquipmentFilterProfile) firstElement;
      }

      if (isCheckOldProfile && _selectedProfile != null && _selectedProfile == selectedProfile) {
         // a new profile is not selected
         return;
      }

      _selectedProfile = selectedProfile;

      /*
       * Update model
       */
      TourEquipmentFilterManager.setSelectedProfile(_selectedProfile);

      /*
       * Update UI
       */
      _isInUpdateUI = true;
      {
         if (_selectedProfile == null) {

            // no profile

            _txtProfileName.setText(UI.EMPTY_STRING);

         } else {

            // a profile is selected

            final boolean isOrOperator = _selectedProfile.isOrOperator;

            _txtProfileName.setText(_selectedProfile.name);

            _rdoEquipmentOperator_OR.setSelection(isOrOperator);
            _rdoEquipmentOperator_AND.setSelection(!isOrOperator);

            if (_selectedProfile.name.startsWith(Messages.Tour_Filter_Default_ProfileName)) {

               // a default profile is selected, make is easy to rename it

               _txtProfileName.selectAll();
               _txtProfileName.setFocus();
            }

            update_FromProfile();
         }
      }
      _isInUpdateUI = false;

      fireModifyEvent();
   }

   private void onSelectedEquipment_Checkbox_CheckAll() {

      _selectedEquipmentViewer.setCheckedElements(_allSelectedEquipmentItems.toArray());

      update_FromSelectedEquipment();
   }

   private void onSelectedEquipment_Checkbox_UncheckAll() {

      _selectedEquipmentViewer.setCheckedElements(EMPTY_LIST);

      update_FromSelectedEquipment();
   }

   private void onSelectedEquipment_Delete() {

      final SelectedEquipment selectedSelectedEquipment = (SelectedEquipment) _selectedEquipmentViewer.getStructuredSelection().getFirstElement();

      if (selectedSelectedEquipment == null) {
         return;
      }

      /*
       * Update model
       */
      _allSelectedEquipmentItems.remove(selectedSelectedEquipment);

      /*
       * Update UI
       */
      final Table selectedEquipmentTable = _selectedEquipmentViewer.getTable();
      final int selectionIndex = selectedEquipmentTable.getSelectionIndex();

      _selectedEquipmentViewer.remove(selectedSelectedEquipment);

      // select next item
      final int nextIndex = Math.min(selectedEquipmentTable.getItemCount() - 1, selectionIndex);
      if (nextIndex >= 0) {

         // set new selection this will also fire the event

         _isInUpdateUIAfterDelete = true;
         {
            _selectedEquipmentViewer.setSelection(new StructuredSelection(_selectedEquipmentViewer.getElementAt(nextIndex)));
         }
         _isInUpdateUIAfterDelete = false;
      }

      update_FromSelectedEquipment();
   }

   private void onSelectedEquipment_Select(final SelectionChangedEvent event) {

      if (_isInUpdateUIAfterDelete) {
         return;
      }

      if (_selectedEquipmentViewerItem_IsKeyPressed && _selectedEquipmentViewerItem_IsChecked == false) {

         // ignore when only selected with keyboard and not checked

         // reset state
         _selectedEquipmentViewerItem_IsKeyPressed = false;

         return;
      }

      Object selection;

      if (_selectedEquipmentViewerItem_IsChecked) {

         // a checkbox is checked

         selection = _selectedEquipmentViewerItem_Data;

      } else {

         selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
      }

      if (selection instanceof SelectedEquipment) {

         // equipment is selected

         final SelectedEquipment selectedEquipment = (SelectedEquipment) selection;

         // toggle equipment
         if (_selectedEquipmentViewerItem_IsChecked == false) {

            // equipment is selected and NOT the checkbox !!!

            final boolean isChecked = _selectedEquipmentViewer.getChecked(selectedEquipment);

            _selectedEquipmentViewer.setChecked(selectedEquipment, !isChecked);
         }

         update_FromSelectedEquipment();
      }
   }

   /**
    * Terrible solution to traverse to a tree
    *
    * @param tree
    * @param event
    */
   private void onTraverse_EquipmentContainer(final Tree tree, final TraverseEvent event) {

      if (event.detail == SWT.TRAVERSE_TAB_NEXT) {

         tree.setFocus();

         final TreeItem[] selection = tree.getSelection();
         if (selection == null || selection.length == 0) {

            if (tree.getItemCount() > 0) {
               tree.setSelection(tree.getItem(0));
            }

         } else {

            tree.setSelection(selection);
         }
      }
   }

   /**
    * Terrible solution to traverse to a table
    *
    * @param table
    * @param event
    */
   private void onTraverse_SelectedEquipmentContainer(final Table table, final TraverseEvent event) {

      if (event.detail == SWT.TRAVERSE_TAB_NEXT) {

         table.setFocus();

         final TableItem[] selection = table.getSelection();
         if (selection == null || selection.length == 0) {

            if (table.getItemCount() > 0) {
               table.setSelection(table.getItem(0));
            }

         } else {

            table.setSelection(selection);
         }
      }
   }

   private void restoreState() {

      // live update
      _isLiveUpdate = Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false);
      _chkLiveUpdate.setSelection(_isLiveUpdate);

      /*
       * Get previous selected profile
       */
      TourEquipmentFilterProfile selectedProfile = TourEquipmentFilterManager.getSelectedProfile();
      if (selectedProfile == null) {

         // select first profile

         selectedProfile = (TourEquipmentFilterProfile) _profileViewer.getElementAt(0);
      }

      if (selectedProfile != null) {
         selectProfile(selectedProfile);
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
            List<TreeViewerItem> allTreeItems = _equipmentViewerRootItem.getFetchedChildren();

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
                  || itemType == STATE_ITEM_TYPE_PART) {

               currentSegments.add(new StateSegment(itemType, itemData));
            }
         }
      }

      if (currentSegments.size() > 0) {
         allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
      }

      return allTreePathSegments;
   }

   @Override
   protected void saveState_BeforeDisposed() {

      _state.put(STATE_IS_LIVE_UPDATE, _isLiveUpdate);

      saveState_ExpandedItems();

      super.saveState();
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

            } else if (segment instanceof final TVIEquipmentView_Part treeItem) {

               allExpandedItemIDs.add(STATE_ITEM_TYPE_PART);
               allExpandedItemIDs.add(treeItem.getPartID());
            }
         }
      }

      Util.setState(_state, STATE_EXPANDED_ITEMS, allExpandedItemIDs.toArray());
   }

   private void selectProfile(final TourEquipmentFilterProfile selectedProfile) {

      _profileViewer.setSelection(new StructuredSelection(selectedProfile));

      final Table table = _profileViewer.getTable();
      table.setSelection(table.getSelectionIndices());
   }

   private void update_FromEquipmentViewer() {

      if (_selectedProfile == null) {
         return;
      }

      final long[] equipmentIDs_Checked = getEquipmentIDs_FromEquipmentViewer();
      final long[] equipmentIDs_Unchecked = getEquipmentIDs_FromSelectedEquipment_Unchecked();

      updateEquipment_EquipmentProfile(_selectedProfile, equipmentIDs_Checked, equipmentIDs_Unchecked);
      updateEquipment_SelectedEquipment(equipmentIDs_Checked, equipmentIDs_Unchecked);

      enableControls();

      fireModifyEvent();
   }

   private void update_FromProfile() {

      final long[] equipmentIDs_Checked = _selectedProfile.allEquipmentFilterIDs.toArray();
      final long[] equipmentIDs_Unchecked = _selectedProfile.allEquipmentFilterIDs_Unchecked.toArray();

      updateEquipment_SelectedEquipment(equipmentIDs_Checked, equipmentIDs_Unchecked);
      updateEquipment_EquipmentViewer(equipmentIDs_Checked);

      enableControls();
   }

   private void update_FromSelectedEquipment() {

      if (_selectedProfile == null) {
         return;
      }

      final long[] equipmentIDs_Checked = getEquipmentIDs_FromSelectedEquipment_Checked();
      final long[] equipmentIDs_Unchecked = getEquipmentIDs_FromSelectedEquipment_Unchecked();

      updateEquipment_EquipmentProfile(_selectedProfile, equipmentIDs_Checked, equipmentIDs_Unchecked);
      updateEquipment_EquipmentViewer(equipmentIDs_Checked);

      enableControls();

      fireModifyEvent();
   }

   private void updateEquipment_EquipmentProfile(final TourEquipmentFilterProfile profile,
                                                 final long[] equipmentIDs_Checked,
                                                 final long[] equipmentIDs_Unchecked) {

      /*
       * Update model
       */
      final LongHashSet profileEquipmentFilterIDs = profile.allEquipmentFilterIDs;
      profileEquipmentFilterIDs.clear();
      profileEquipmentFilterIDs.addAll(equipmentIDs_Checked);

      final LongHashSet profileEquipmentFilterIDs_Unchecked = profile.allEquipmentFilterIDs_Unchecked;
      profileEquipmentFilterIDs_Unchecked.clear();
      profileEquipmentFilterIDs_Unchecked.addAll(equipmentIDs_Unchecked);

      /*
       * Update UI
       */
      _profileViewer.update(profile, null);
   }

   private void updateEquipment_EquipmentViewer(final long[] allEquipmentIDs) {

      final int numIDs = allEquipmentIDs.length;

      final List<TVIEquipmentView_Item> allEquipmentItems = new ArrayList<>(numIDs);

      if (numIDs > 0) {

         // get all equipment items which should be checked

         final ArrayList<TreeViewerItem> allRootItems = _equipmentViewerRootItem.getFetchedChildren();

         for (final TreeViewerItem rootItem : allRootItems) {

            if (rootItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

               final long itemID = equipmentItem.getEquipmentID();

               for (final long equipmentID : allEquipmentIDs) {

                  if (equipmentID == itemID) {

                     allEquipmentItems.add(equipmentItem);
                     break;
                  }
               }
            }
         }
      }

      // update UI
      _equipmentViewer.setCheckedElements(allEquipmentItems.toArray());
   }

   private void updateEquipment_SelectedEquipment(final long[] allEquipmentIDs_Checked,
                                                  final long[] allEquipmentIDs_Unchecked) {

      /*
       * Update model
       */
      _allSelectedEquipmentItems.clear();

      final List<SelectedEquipment> allCheckedEquipment = new ArrayList<>();
      final Map<Long, Equipment> allEquipment = EquipmentManager.getAllEquipment_ByID();

      // add all checked equipment
      for (final long equipmentID : allEquipmentIDs_Checked) {

         final Equipment equipment = allEquipment.get(equipmentID);

         if (equipment == null) {

            //fixed unknown NPE
            continue;
         }

         final SelectedEquipment selectedEquipment = new SelectedEquipment(equipment);

         _allSelectedEquipmentItems.add(selectedEquipment);
         allCheckedEquipment.add(selectedEquipment);
      }

      // add unchecked equipment
      for (final long equipmentID : allEquipmentIDs_Unchecked) {

         final Equipment equipment = allEquipment.get(equipmentID);

         if (equipment == null) {

            /*
             * It is possible that the equipment in the tour equipment filter is already
             * deleted and the tour equipment filter is not yet updated
             */

            continue;
         }

         final SelectedEquipment selectedEquipment = new SelectedEquipment(equipment);

         /*
          * It is possible that there are duplicates in unchecked equipment when a equipment is
          * selected in the available equipment and this equipment is unchecked in selected
          * equipment
          */
         boolean canAddSelectedEquipment = true;
         for (final SelectedEquipment alreadyAddedSelectedEquipment : _allSelectedEquipmentItems) {

            if (alreadyAddedSelectedEquipment.equipmentId == equipmentID) {
               canAddSelectedEquipment = false;
               break;
            }
         }

         if (canAddSelectedEquipment) {
            _allSelectedEquipmentItems.add(selectedEquipment);
         }
      }

      /*
       * Update UI
       */
      // reload viewer
      _selectedEquipmentViewer.setInput(EMPTY_LIST);

      // check selected equipment items
      _selectedEquipmentViewer.setCheckedElements(allCheckedEquipment.toArray());
   }

   private void updateEquipmentModel() {

      _equipmentViewerRootItem = new TVIEquipmentView_Root(_equipmentViewer, false);
      _equipmentViewer.setInput(this);

      loadAllTreeItems();
   }

}
