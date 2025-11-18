/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.equipment.EquipmentGroup;
import net.tourbook.equipment.EquipmentGroupManager;
import net.tourbook.equipment.EquipmentManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageEquipmentGroups extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String  ID                                             = "net.tourbook.preferences.PrefPageEquipmentGroups"; //$NON-NLS-1$

   private static final String STATE_IS_SHOW_ONLY_EQUIPMENT_WHICH_ARE_CHECKED = "STATE_IS_SHOW_ONLY_EQUIPMENT_WHICH_ARE_CHECKED";   //$NON-NLS-1$
   private static final String STATE_SELECTED_EQUIPMENT_GROUP                 = "STATE_SELECTED_EQUIPMENT_GROUP";                   //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final IDialogSettings      _state      = TourbookPlugin.getState(ID);
   private static final IPreferenceStore     _prefStore  = TourbookPlugin.getDefault().getPreferenceStore();

// SET_FORMATTING_ON

   private IPropertyChangeListener _prefChangeListener;
   private ITourEventListener      _tourEventListener;

   private TableViewer             _equipmentViewer_Groups;
   private CheckboxTableViewer     _equipmentViewer_Items;

   private List<Equipment>         _allEquipment;
   private EquipmentFilter         _equipmentFilter = new EquipmentFilter();

   /**
    * Complicated, it is necessary to check the checkbox, otherwise another item could be checked
    */
   private boolean                 _equipmentViewerItem_IsChecked;
   private boolean                 _equipmentViewerItem_IsKeyPressed;
   private Object                  _equipmentViewerItem_Data;

   private boolean                 _isModified;
   private boolean                 _isShowOnlyCheckedEquipment;

   private EquipmentGroup          _selectedEquipmentGroup;

   private ActionEquipment_Filter  _actionEquipment_Filter;

   /*
    * UI controls
    */
   private Composite _containerEquipmentGroupHeader;

   private ToolBar   _toolBarAllEquipment;

   private Button    _btnCheckAll;
   private Button    _btnDelete;
   private Button    _btnNew;
   private Button    _btnRename;
   private Button    _btnUncheckAll;

   private Label     _lblActionSpacer;

   private Composite _parent;

   private class ActionEquipment_Filter extends Action {

      public ActionEquipment_Filter() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Pref_Equipment_Action_FilterEquipment_ShowOnlyChecked_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Checked));
      }

      @Override
      public void run() {
         onEquipment_Filter();
      }
   }

   private class EquipmentFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (_selectedEquipmentGroup == null) {

            return false;
         }

         if (element instanceof final Equipment equipment) {

            if (_selectedEquipmentGroup.allEquipment.contains(equipment)) {

               return true;
            }
         }

         return false;
      }
   }

   private class EquipmentGroupViewer_Comparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final EquipmentGroup group1 = (EquipmentGroup) e1;
         final EquipmentGroup group2 = (EquipmentGroup) e2;

         return group1.name.compareTo(group2.name);
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }
   }

   private final class EquipmentGroupViewer_ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {

         return EquipmentGroupManager.getEquipmentGroups().toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer,
                               final Object oldInput,
                               final Object newInput) {}
   }

   private final class EquipmentViewer_ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allEquipment.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public PrefPageEquipmentGroups() {}

   public PrefPageEquipmentGroups(final String title) {
      super(title);
   }

   public PrefPageEquipmentGroups(final String title, final ImageDescriptor image) {
      super(title, image);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         if (propertyChangeEvent.getProperty().equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            updateViewers_All();

            enableControls();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

      _actionEquipment_Filter = new ActionEquipment_Filter();
   }

   @Override
   protected Control createContents(final Composite parent) {

      _parent = parent;

      initUI();

      restoreStateBeforeUI();

      final Composite ui = createUI(parent);

      createActions();
      fillToolbar();

      // this must be called AFTER the toolbar is created and filled !!!
      updateUI_TopGridRowHeight();

      addPrefListener();
      addTourEventListener();

      updateViewers_All();

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         createUI_10_Equipment_Groups(container);
         createUI_20_Equipment_Items(container);
         createUI_30_Equipment_Actions(container);
      }

      return container;
   }

   private void createUI_10_Equipment_Groups(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

         _containerEquipmentGroupHeader = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerEquipmentGroupHeader);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_containerEquipmentGroupHeader);
         {
            final Label label = UI.createLabel(_containerEquipmentGroupHeader, Messages.Pref_Equipment_Label_EquipmentGroups);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .align(SWT.FILL, SWT.END)
                  .applyTo(label);
         }

         createUI_12_Equipment_Groups_Viewer(container);
      }
   }

   private void createUI_12_Equipment_Groups_Viewer(final Composite parent) {

      final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(70, 100)
            .applyTo(tableLayouter);

      final Table table = new Table(tableLayouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      _equipmentViewer_Groups = new TableViewer(table);

      _equipmentViewer_Groups.setUseHashlookup(true);
      _equipmentViewer_Groups.setContentProvider(new EquipmentGroupViewer_ContentProvider());
      _equipmentViewer_Groups.setComparator(new EquipmentGroupViewer_Comparator());

      _equipmentViewer_Groups.addSelectionChangedListener(selectionChangedEvent -> onEquipmentGroup_Select());
      _equipmentViewer_Groups.addDoubleClickListener(doubleClickEvent -> onEquipmentGroup_Rename());

      /*
       * Column: Group name
       */
      final TableViewerColumn tvc = new TableViewerColumn(_equipmentViewer_Groups, SWT.NONE);
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final EquipmentGroup equipmentGroup = ((EquipmentGroup) cell.getElement());

            final StyledString styledString = new StyledString();

            styledString.append(equipmentGroup.name, net.tourbook.ui.UI.CONTENT_CATEGORY_STYLER);
            styledString.append(UI.SPACE3 + equipmentGroup.allEquipment.size(), net.tourbook.ui.UI.TOTAL_STYLER);

            String text = styledString.getString();

            if (UI.IS_SCRAMBLE_DATA) {
               text = UI.scrambleText(text);
            }

            cell.setText(text);
            cell.setStyleRanges(styledString.getStyleRanges());

         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(1));
   }

   private void createUI_20_Equipment_Items(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         createUI_22_Equipment_Items_Header(container);
         createUI_24_Equipment_Items_Viewer(container);
      }
   }

   private void createUI_22_Equipment_Items_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            // Label: All equipment
            final Label label = UI.createLabel(container, Messages.Pref_Equipment_Label_AllEquipment);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.END)
                  .grab(true, true)
                  .applyTo(label);
         }
         {
            // toolbar
            _toolBarAllEquipment = new ToolBar(container, SWT.FLAT);
         }
      }
   }

   private void createUI_24_Equipment_Items_Viewer(final Composite parent) {

      final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(50, 100)
            .applyTo(tableLayouter);

      final Table table = new Table(tableLayouter, SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION);

      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      table.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> _equipmentViewerItem_IsKeyPressed = true));
      table.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentItem_TableSelect(selectionEvent)));

      _equipmentViewer_Items = new CheckboxTableViewer(table);

      _equipmentViewer_Items.setContentProvider(new EquipmentViewer_ContentProvider());

      // this is needed that the keyboard works !!!
      _equipmentViewer_Items.addCheckStateListener(checkStateChangedEvent -> updateUI_EquipmentGroup_FromCheckedEquipment());
      _equipmentViewer_Items.addSelectionChangedListener(event -> onEquipmentItem_ViewerSelect(event));

      final TableViewerColumn tvc = new TableViewerColumn(_equipmentViewer_Items, SWT.NONE);

      tvc.setLabelProvider(new StyledCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Equipment equipment = ((Equipment) cell.getElement());

            cell.setText(equipment.getName());
         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(20));
   }

   private void createUI_30_Equipment_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
      {
         _lblActionSpacer = UI.createSpacer_Horizontal(container);
         GridDataFactory.fillDefaults().applyTo(_lblActionSpacer);

         // button: new
         _btnNew = new Button(container, SWT.NONE);
         _btnNew.setText(Messages.App_Action_New);
         _btnNew.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentGroup_New()));
         setButtonLayoutData(_btnNew);

         // button: rename
         _btnRename = new Button(container, SWT.NONE);
         _btnRename.setText(Messages.App_Action_Rename);
         _btnRename.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentGroup_Rename()));
         setButtonLayoutData(_btnRename);

         // button: delete
         _btnDelete = new Button(container, SWT.NONE);
         _btnDelete.setText(Messages.App_Action_Delete_WithConfirm);
         _btnDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentGroup_Delete()));
         setButtonLayoutData(_btnDelete);
         {
            /*
             * Button: Check all
             */
            _btnCheckAll = new Button(container, SWT.PUSH);
            _btnCheckAll.setText(Messages.App_Action_CheckAll);
            _btnCheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(true)));
            final GridData gd = setButtonLayoutData(_btnCheckAll);

            // set vertical space to the upper action
            gd.verticalIndent = 20;
         }
         {
            /*
             * Button: Uncheck all
             */
            _btnUncheckAll = new Button(container, SWT.PUSH);
            _btnUncheckAll.setText(Messages.App_Action_UncheckAll);
            _btnUncheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(false)));
            setButtonLayoutData(_btnUncheckAll);
         }
      }
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      super.dispose();
   }

   private void enableControls() {

      final EquipmentGroup equipmentGroup = (EquipmentGroup) _equipmentViewer_Groups.getStructuredSelection().getFirstElement();

      final boolean areGroupsAvailable = EquipmentGroupManager.getEquipmentGroups().size() > 0;
      final boolean isGroupSelected = equipmentGroup != null;

// SET_FORMATTING_OFF

      _actionEquipment_Filter .setEnabled(areGroupsAvailable);

      _btnRename              .setEnabled(isGroupSelected);
      _btnDelete              .setEnabled(isGroupSelected);
      _btnCheckAll            .setEnabled(isGroupSelected);
      _btnUncheckAll          .setEnabled(isGroupSelected);

// SET_FORMATTING_ON

      _equipmentViewer_Items.getTable().setEnabled(isGroupSelected);
   }

   /**
    * Set the toolbar action after the {@link #_equipmentViewer_Items} is created
    */
   private void fillToolbar() {

      /*
       * Toolbar: All equipment
       */
      final ToolBarManager tbmAllEquipment = new ToolBarManager(_toolBarAllEquipment);

      tbmAllEquipment.add(_actionEquipment_Filter);

      tbmAllEquipment.update(true);
   }

   private long[] getAllCheckedEqipmentIDs() {

      final LongHashSet allCheckedEquipmentIDs = new LongHashSet();

      final Object[] checkedElements = _equipmentViewer_Items.getCheckedElements();

      for (final Object object : checkedElements) {

         if (object instanceof final Equipment equipment) {

            allCheckedEquipmentIDs.add(equipment.getEquipmentId());
         }
      }

      return allCheckedEquipmentIDs.toArray();
   }

   /**
    * Get the input validator for the receiver.
    *
    * @return IInputValidator
    */
   private IInputValidator getNameValidator() {

      return newText -> {

         if (newText.trim().length() > 0) {
            return null;
         }

         return Messages.Pref_Equipment_Error_NameIsEmpty;
      };
   }

   @Override
   public void init(final IWorkbench workbench) {

      noDefaultAndApplyButton();
   }

   private void initUI() {

      _parent.addDisposeListener(disposeEvent -> onDispose());
   }

   @Override
   public boolean okToLeave() {

      saveChanges();

      return true;
   }

   private void onCheckAll(final boolean isChecked) {

      // update UI
      _equipmentViewer_Items.setAllChecked(isChecked);

      // this is needed that the styler is applied !!!
      _equipmentViewer_Items.refresh();

      updateUI_EquipmentGroup_FromCheckedEquipment();
   }

   private void onDispose() {

   }

   private void onEquipment_Filter() {

      // toggle equipment filter
      _isShowOnlyCheckedEquipment = !_isShowOnlyCheckedEquipment;

      updateUI_EquipmentAfterFiltering();
   }

   private void onEquipmentGroup_Delete() {

      final EquipmentGroup equipmentGroup = (EquipmentGroup) _equipmentViewer_Groups.getStructuredSelection().getFirstElement();

      if (equipmentGroup == null) {
         return;
      }

      if (new MessageDialog(

            _parent.getShell(),

            Messages.Pref_Equipment_Dialog_DeleteEquipment_Title,
            null, // no title image

            Messages.Pref_Equipment_Dialog_DeleteEquipment_Message.formatted(equipmentGroup.name),
            MessageDialog.CONFIRM,

            0, // default index

            Messages.App_Action_Delete,
            Messages.App_Action_Cancel

      ).open() != IDialogConstants.OK_ID) {

         return;
      }

      _isModified = true;

      // update model
      EquipmentGroupManager.removeEquipmentGroup(equipmentGroup);

      // update UI
      final Table groupTable = _equipmentViewer_Groups.getTable();
      final int selectionIndex = groupTable.getSelectionIndex();

      _equipmentViewer_Groups.remove(equipmentGroup);

      // select next group
      final int nextIndex = Math.min(groupTable.getItemCount() - 1, selectionIndex);

      if (nextIndex >= 0) {

         _equipmentViewer_Groups.setSelection(new StructuredSelection(_equipmentViewer_Groups.getElementAt(nextIndex)));

      } else {

         // all groups are deleted

         _selectedEquipmentGroup = null;

         // uncheck all equipment
         _equipmentViewer_Items.setCheckedElements(new Object[0]);

         // deselect equipment filter
         _isShowOnlyCheckedEquipment = false;

         updateUI_EquipmentAfterFiltering();

         enableControls();
      }
   }

   private void onEquipmentGroup_New() {

      final InputDialog inputDialog = new InputDialog(getShell(),
            Messages.Pref_Equipment_Dialog_NewEquipment_Title,
            Messages.Pref_Equipment_Dialog_NewEquipment_Message,
            UI.EMPTY_STRING,
            getNameValidator());

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      _isModified = true;

      // create new equipment group
      final EquipmentGroup equipmentGroup = new EquipmentGroup();
      equipmentGroup.name = inputDialog.getValue().trim();

      // update model
      EquipmentGroupManager.addEquipmentGroup(equipmentGroup);

      // update UI
      _equipmentViewer_Groups.setInput(new Object());

      // select new group
      _equipmentViewer_Groups.setSelection(new StructuredSelection(equipmentGroup), true);

      _equipmentViewer_Items.getTable().setFocus();
   }

   private void onEquipmentGroup_Rename() {

      final EquipmentGroup equipmentGroup = (EquipmentGroup) (_equipmentViewer_Groups.getStructuredSelection()).getFirstElement();

      final InputDialog inputDialog = new InputDialog(getShell(),
            Messages.Pref_Equipment_Dialog_RenameEquipment_Title,
            Messages.Pref_Equipment_Dialog_RenameEquipment_Message,
            equipmentGroup.name,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      _isModified = true;

      // update model
      equipmentGroup.name = inputDialog.getValue().trim();

      // update UI
      _equipmentViewer_Groups.refresh();

      _equipmentViewer_Groups.setSelection(new StructuredSelection(_selectedEquipmentGroup), true);
      _equipmentViewer_Groups.getTable().setFocus();

      // reselect group equipment
      onEquipmentGroup_Select();
   }

   private void onEquipmentGroup_Select() {

      final EquipmentGroup equipmentGroup = (EquipmentGroup) (_equipmentViewer_Groups.getStructuredSelection()).getFirstElement();

      if (equipmentGroup == null) {
         return;
      }

      _selectedEquipmentGroup = equipmentGroup;

      _isModified = true;

      updateUI_EquipmentViewer(equipmentGroup.allEquipment);

      enableControls();
   }

   private void onEquipmentItem_TableSelect(final SelectionEvent selectionEvent) {

      _equipmentViewerItem_IsChecked = selectionEvent.detail == SWT.CHECK;

      if (_equipmentViewerItem_IsChecked) {

         /*
          * Item can be null when <ctrl>+A is pressed !!!
          */
         final Widget item = selectionEvent.item;

         _equipmentViewerItem_Data = item.getData();
      }
   }

   private void onEquipmentItem_ViewerSelect(final SelectionChangedEvent event) {

      if (_equipmentViewerItem_IsKeyPressed) {

         // ignore when selected with keyboard

         // reset state
         _equipmentViewerItem_IsKeyPressed = false;

         return;
      }

      Object selection;

      if (_equipmentViewerItem_IsChecked) {

         // the checkbox is selected

         selection = _equipmentViewerItem_Data;

      } else {

         // the row is selected

         selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
      }

      if (selection instanceof final Equipment equipment) {

         // equipment is selected

         // toggle equipment
         if (_equipmentViewerItem_IsChecked == false) {

            // equipment is selected and NOT the checkbox !!!

            final boolean isChecked = _equipmentViewer_Items.getChecked(equipment);

            _equipmentViewer_Items.setChecked(equipment, !isChecked);
         }

         updateUI_EquipmentGroup_FromCheckedEquipment();
      }
   }

   @Override
   public boolean performCancel() {

      saveState();

      return true;
   }

   @Override
   protected void performDefaults() {

      _isModified = true;

      super.performDefaults();

      // this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();
   }

   @Override
   public boolean performOk() {

      saveChanges();

      return true;
   }

   private void restoreState() {

      /*
       * Equipment filter
       */
      _actionEquipment_Filter.setChecked(_isShowOnlyCheckedEquipment);

      /*
       * Equipment group
       */
      final String selectedGroupName = Util.getStateString(_state, STATE_SELECTED_EQUIPMENT_GROUP, null);

      if (selectedGroupName != null) {

         for (final EquipmentGroup equipmentGroup : EquipmentGroupManager.getEquipmentGroups()) {

            if (selectedGroupName.equals(equipmentGroup.name)) {

               _selectedEquipmentGroup = equipmentGroup;

               break;
            }
         }

         if (_selectedEquipmentGroup != null) {

            _equipmentViewer_Groups.setSelection(new StructuredSelection(_selectedEquipmentGroup), true);
         }
      }

      updateUI_EquipmentFilter();
   }

   private void restoreStateBeforeUI() {

      _isShowOnlyCheckedEquipment = Util.getStateBoolean(_state, STATE_IS_SHOW_ONLY_EQUIPMENT_WHICH_ARE_CHECKED, false);
   }

   private void saveChanges() {

      if (_isModified) {

         _isModified = false;

         EquipmentGroupManager.saveState();

         // fire modify event
         TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED);
      }

      saveState();
   }

   private void saveState() {

      _state.put(STATE_IS_SHOW_ONLY_EQUIPMENT_WHICH_ARE_CHECKED, _isShowOnlyCheckedEquipment);

      if (_selectedEquipmentGroup != null) {

         _state.put(STATE_SELECTED_EQUIPMENT_GROUP, _selectedEquipmentGroup.name);
      }
   }

   private void updateUI_EquipmentAfterFiltering() {

      _parent.setRedraw(false);
      {
         updateUI_EquipmentFilter();
      }
      _parent.setRedraw(true);
   }

   /**
    * Set equipment viewer filter which will refilter it
    */
   private void updateUI_EquipmentFilter() {

      if (_isShowOnlyCheckedEquipment) {

         _actionEquipment_Filter.setToolTipText(Messages.Pref_Equipment_Action_FilterEquipment_ShowAll_Tooltip);

         _equipmentViewer_Items.setFilters(_equipmentFilter);

      } else {

         _actionEquipment_Filter.setToolTipText(Messages.Pref_Equipment_Action_FilterEquipment_ShowOnlyChecked_Tooltip);

         _equipmentViewer_Items.setFilters();
      }
   }

   private void updateUI_EquipmentGroup_FromCheckedEquipment() {

      if (_selectedEquipmentGroup == null) {
         return;
      }

      final long[] allCheckedIDs = getAllCheckedEqipmentIDs();
      final Map<Long, Equipment> allEquipmentByID = EquipmentManager.getAllEquipment_ByID();

      final List<Equipment> allCheckedEquipment = new ArrayList<>();

      for (final long equipmentId : allCheckedIDs) {

         final Equipment equipment = allEquipmentByID.get(equipmentId);

         allCheckedEquipment.add(equipment);
      }

      // update model
      _selectedEquipmentGroup.allEquipment.clear();
      _selectedEquipmentGroup.allEquipment.addAll(allCheckedEquipment);

      // update UI
      _equipmentViewer_Groups.update(_selectedEquipmentGroup, null);

      _isModified = true;

      enableControls();
   }

   private void updateUI_EquipmentViewer(final Set<Equipment> allEquipment) {

      final List<Equipment> allCheckedEquipment = new ArrayList<>(allEquipment);

      _parent.setRedraw(false);
      {

         if (_isShowOnlyCheckedEquipment) {

            // show all equipment -> disable filter

            _equipmentViewer_Items.setFilters();
         }

         /**
          * !!! VERY IMPORTANT !!!
          * <p>
          * Uncheck all, otherwise a second selection hides the grayed state
          */
         _equipmentViewer_Items.setCheckedElements(new Object[0]);

         // update UI
         _equipmentViewer_Items.setCheckedElements(allCheckedEquipment.toArray());

         if (_isShowOnlyCheckedEquipment) {

            // enable filter

            _equipmentViewer_Items.setFilters(_equipmentFilter);
         }
      }
      _parent.setRedraw(true);
   }

   private void updateUI_TopGridRowHeight() {

      final Point toolbarSize = _toolBarAllEquipment.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      final int toolbarHeight = toolbarSize.y;

      GridData gd = (GridData) _lblActionSpacer.getLayoutData();
      gd.heightHint = toolbarHeight;

      gd = (GridData) _containerEquipmentGroupHeader.getLayoutData();
      gd.heightHint = toolbarHeight;
   }

   private void updateViewers_All() {

      _equipmentViewer_Groups.setInput(new Object());

      _allEquipment = EquipmentManager.getAllEquipment_Name();
      _equipmentViewer_Items.setInput(this);
   }

}
