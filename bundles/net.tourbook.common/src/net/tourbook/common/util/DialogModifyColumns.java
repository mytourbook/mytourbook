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
package net.tourbook.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;

public class DialogModifyColumns extends TrayDialog {

   private Action                      _actionShowHideCategory;

   private ColumnManager               _columnManager;

   /** Model for the column viewer */
   private ArrayList<ColumnDefinition> _columnViewerModel;

   private ArrayList<ColumnDefinition> _allDefinedColumns;
   //
   private PixelConverter              _pc;
   //
   private long                        _dndDragStartViewerLeft;
   private Object[]                    _dndCheckedElements;
   //
   private ColumnProfile               _selectedProfile;
   private ArrayList<ColumnProfile>    _columnMgr_Profiles;

   private ArrayList<ColumnProfile>    _dialog_Profiles;
   //
   private boolean                     _isInUpdate;
   private boolean                     _isShowColumnAnnotation_Formatting;
   private boolean                     _isShowColumnAnnotation_Sorting;
   //
   private boolean                     _isShowCategory;
   private boolean                     _isCategoryAvailable;
   private int                         _categoryColumnWidth;
   //
   private ViewerComparator            _profileComparator = new ProfileComparator();
   /*
    * UI controls
    */
   private Button                      _btnColumn_MoveUp;

   private Button                      _btnColumn_MoveDown;
   private Button                      _btnColumn_SelectAll;
   private Button                      _btnColumn_DeselectAll;
   private Button                      _btnColumn_Default;
   private Button                      _btnColumn_DefaultWidth;
   private Button                      _btnProfile_New;
   private Button                      _btnProfile_Remove;
   private Button                      _btnProfile_Rename;
   private Button                      _btnColumn_Sort;
   //
   private Button                      _chkShowColumnAnnotation_Formatting;
   private Button                      _chkShowColumnAnnotation_Sorting;
   //
   private CheckboxTableViewer         _columnViewer;
   private TableViewer                 _profileViewer;
   private Composite                   _uiContainer;

   private TableColumn                 _categoryColumn;

   public class ActionCategoryColumn extends Action {

      public ActionCategoryColumn() {

         super(null, AS_CHECK_BOX);

         setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__ColumnCategory));
         setToolTipText(Messages.ColumnModifyDialog_Button_ShowCategoryColumn_Tooltip);
      }

      @Override
      public void run() {
         action_ShowHideCategory();
      }
   }

   public class ProfileComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final ColumnProfile profile1 = (ColumnProfile) e1;
         final ColumnProfile profile2 = (ColumnProfile) e2;

         return profile1.name.compareTo(profile2.name);
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {
         // force resorting when a name is renamed
         return true;
      }
   }

   public DialogModifyColumns(final Shell parentShell,
                              final ColumnManager columnManager,
                              final ArrayList<ColumnDefinition> allRearrangedColumns,
                              final ArrayList<ColumnDefinition> allDefinedColumns,
                              final ColumnProfile columnMgr_ActiveProfile,
                              final ArrayList<ColumnProfile> columnMgr_Profiles) {

      super(parentShell);

      _columnManager = columnManager;
      _columnViewerModel = allRearrangedColumns;
      _allDefinedColumns = allDefinedColumns;

      _columnMgr_Profiles = columnMgr_Profiles;

      // use cloned profiles in the dialog
      _dialog_Profiles = new ArrayList<>();
      for (final ColumnProfile columnMgr_Profile : columnMgr_Profiles) {

         final ColumnProfile clonedProfile = columnMgr_Profile.clone();

         _dialog_Profiles.add(clonedProfile);

         // set active profile
         if (columnMgr_Profile == columnMgr_ActiveProfile) {
            _selectedProfile = clonedProfile;
         }
      }
      if (_selectedProfile == null) {
         _selectedProfile = _dialog_Profiles.get(0);
      }

      sortDialogProfiles();

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   private void action_ShowHideCategory() {

      // toggle column
      _isShowCategory = !_isShowCategory;

      _categoryColumn.setWidth(_isShowCategory ? _categoryColumnWidth : 0);
   }

   private void actionOnColumn_Default_Sort() {

      // copy all defined columns into the dialog columns
      _columnViewerModel = cloneAllColumns(false);

      updateProfileModel_From_Model(_columnViewerModel);

      setupColumnsInColumnViewer();
   }

   private void actionOnColumn_Default_Width() {
      // set column width to the default width

      for (final ColumnDefinition colDef : _columnViewerModel) {
         colDef.setColumnWidth(colDef.getDefaultColumnWidth());
      }

      _columnViewer.refresh();
   }

   private void actionOnColumn_Select_AllColumns() {

      // update model
      for (final ColumnDefinition colDef : _columnViewerModel) {
         colDef.setIsColumnChecked(true);
      }

      // update UI
      _columnViewer.setAllChecked(true);

      updateProfileModel_From_ColumnViewer();

      updateUI_ProfileViewer();
   }

   private void actionOnColumn_Select_DefaultColumns() {

      /*
       * Copy all defined columns into the dialog columns
       */

      // update Model
      _columnViewerModel = cloneAllColumns(true);

      updateProfileModel_From_Model(_columnViewerModel);

      // update UI
      setupColumnsInColumnViewer();

      updateUI_ProfileViewer();
   }

   private void actionOnColumn_Select_NoColumns() {

      // list with all columns which must be checked
      final ArrayList<ColumnDefinition> checkedElements = new ArrayList<>();

      // update model
      for (final ColumnDefinition colDef : _columnViewerModel) {
         if (colDef.canModifyVisibility() == false) {
            checkedElements.add(colDef);
            colDef.setIsColumnChecked(true);
         } else {
            colDef.setIsColumnChecked(false);
         }
      }

      updateProfileModel_From_Model(_columnViewerModel);

      // update UI
      _columnViewer.setCheckedElements(checkedElements.toArray());

      updateUI_ProfileViewer();
   }

   private void actionOnProfile_Add() {

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.ColumnModifyDialog_Dialog_Profile_Title,
            Messages.ColumnModifyDialog_Dialog_ProfileNew_Message,
            UI.EMPTY_STRING,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      /*
       * Create new profile
       */
      final ColumnProfile newProfile = new ColumnProfile();

      // set profile name
      newProfile.name = inputDialog.getValue().trim();

      // update model
      _dialog_Profiles.add(newProfile);
      _selectedProfile = newProfile;

      // create columns for a new profile by copying current selected columns
      _columnViewerModel = cloneAllColumns(false);

      updateProfileModel_From_Model(_columnViewerModel);

      // update UI
      _profileViewer.add(newProfile);

      _profileViewer.setSelection(new StructuredSelection(newProfile), true);

      // force that horizontal scrollbar is NOT visible
      _uiContainer.layout(true, true);

      // show number of columns in the profile viewer
      updateUI_ProfileViewer();

      enableProfileActions();

      _profileViewer.getTable().setFocus();
   }

   private void actionOnProfile_Remove() {

      _isInUpdate = true;
      {
         final Table profileTable = _profileViewer.getTable();

         final int selectedIndex = profileTable.getSelectionIndex();

         final ColumnProfile selectedProfile = getSelectedProfile();

         // update UI
         _dialog_Profiles.remove(selectedProfile);

         // update model
         _profileViewer.remove(selectedProfile);

         /*
          * Select profile at the same position
          */
         final int profilesSize = _dialog_Profiles.size();
         int newIndex = selectedIndex;
         if (newIndex >= profilesSize) {
            newIndex = profilesSize - 1;
         }

         int nextIndex = 0;
         final TableItem nextItem = profileTable.getItem(newIndex);
         final ColumnProfile nextProfile = (ColumnProfile) nextItem.getData();

         for (int profileIndex = 0; profileIndex < _dialog_Profiles.size(); profileIndex++) {

            final ColumnProfile profile = _dialog_Profiles.get(profileIndex);

            if (profile.getID() == nextProfile.getID()) {
               nextIndex = profileIndex;
               break;
            }
         }
         _selectedProfile = _dialog_Profiles.get(nextIndex);

         // update UI
         _profileViewer.setSelection(new StructuredSelection(_selectedProfile), true);

         enableProfileActions();

         setupColumnProfile(_selectedProfile);

         profileTable.setFocus();
      }
      _isInUpdate = false;
   }

   private void actionOnProfile_Rename() {

      final ColumnProfile selectedProfile = getSelectedProfile();

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.ColumnModifyDialog_Dialog_Profile_Title,
            Messages.ColumnModifyDialog_Dialog_ProfileRename_Message,
            selectedProfile.name,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         // canceled
         return;
      }

      // get name
      final String modifiedProfileName = inputDialog.getValue().trim();

      // update model
      selectedProfile.name = modifiedProfileName;

      _profileViewer.update(selectedProfile, null);

      // focus can have changed when resorted, set focus to the selected item
      int selectedIndex = 0;
      final Table table = _profileViewer.getTable();
      final TableItem[] items = table.getItems();
      for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {

         final TableItem tableItem = items[itemIndex];

         if (tableItem.getData() == selectedProfile) {
            selectedIndex = itemIndex;
         }
      }
      table.setSelection(selectedIndex);
      table.showSelection();

      _profileViewer.getTable().setFocus();
   }

   /**
    * Clones all columns in default order.
    *
    * @param isSetDefaults
    *           When <code>true</code> then column properties are set from the default settings
    *           otherwise they are copied from {@link #_columnViewerModel}
    * @return Returns all {@link ColumnDefinition}s in default order/selection.
    */
   private ArrayList<ColumnDefinition> cloneAllColumns(final boolean isSetDefaults) {

      final ArrayList<ColumnDefinition> allDialogColumns = new ArrayList<>();

      for (final ColumnDefinition definedColDef : _allDefinedColumns) {

         try {

            // clone column
            final ColumnDefinition colDefClone = (ColumnDefinition) definedColDef.clone();

            if (isSetDefaults) {

               final ValueFormat valueFormat_Category = definedColDef.getDefaultValueFormat_Category();
               final ValueFormat valueFormat_Detail = definedColDef.getDefaultValueFormat_Detail();

               final IValueFormatter valueFormatter_Category = _columnManager.getValueFormatter(valueFormat_Category);
               final IValueFormatter valueFormatter_Detail = _columnManager.getValueFormatter(valueFormat_Detail);

               // visible columns in the viewer will be checked
               colDefClone.setIsColumnChecked(definedColDef.isDefaultColumn());

               colDefClone.setColumnWidth(definedColDef.getDefaultColumnWidth());
               colDefClone.setValueFormatter_Category(valueFormat_Category, valueFormatter_Category);
               colDefClone.setValueFormatter_Detail(valueFormat_Detail, valueFormatter_Detail);

            } else {

               // set properties from the current settings

               final String definedColumnId = definedColDef.getColumnId();

               for (final ColumnDefinition currentColDef : _columnViewerModel) {

                  if (currentColDef.getColumnId().equals(definedColumnId)) {

                     ValueFormat valueFormat = definedColDef.getValueFormat_Category();
                     ValueFormat valueFormat_Detail = definedColDef.getValueFormat_Detail();

                     if (valueFormat == null) {
                        valueFormat = definedColDef.getDefaultValueFormat_Category();
                     }

                     if (valueFormat_Detail == null) {
                        valueFormat_Detail = definedColDef.getDefaultValueFormat_Detail();
                     }

                     final IValueFormatter valueFormatter = _columnManager.getValueFormatter(valueFormat);
                     final IValueFormatter valueFormatter_Detail = _columnManager.getValueFormatter(valueFormat_Detail);

                     colDefClone.setIsColumnChecked(currentColDef.isColumnCheckedInContextMenu());

                     colDefClone.setColumnWidth(currentColDef.getColumnWidth());
                     colDefClone.setValueFormatter_Category(valueFormat, valueFormatter);
                     colDefClone.setValueFormatter_Detail(valueFormat_Detail, valueFormatter_Detail);

                     break;
                  }
               }
            }

            allDialogColumns.add(colDefClone);

         } catch (final CloneNotSupportedException e) {
            StatusUtil.log(e);
         }
      }

      return allDialogColumns;
   }

   /**
    * Create model for the column viewer from a {@link ColumnProfile}.
    *
    * @param columnProfile
    * @return Returns ALL columns, first the visible then the hidden columns.
    */
   private ArrayList<ColumnDefinition> cloneAllColumns(final ColumnProfile columnProfile) {

      // set column definitions in the ColumnProfile from the visible id's.
      _columnManager.setupVisibleColDefs(columnProfile);

      final ArrayList<ColumnDefinition> allClonedAndSortedColumns = new ArrayList<>();

      try {

         final ArrayList<ColumnDefinition> allClonedColDef = new ArrayList<>();

         /*
          * Clone original column definitions
          */
         for (final ColumnDefinition definedColDef : _allDefinedColumns) {
            allClonedColDef.add((ColumnDefinition) definedColDef.clone());
         }

         /*
          * Add visible columns
          */
         for (final ColumnDefinition colDef : columnProfile.visibleColumnDefinitions) {

            final ColumnDefinition modelColDef = (ColumnDefinition) colDef.clone();
            final String columnId = modelColDef.getColumnId();

            // get value format
            ValueFormat valueFormat = null;
            ValueFormat valueFormat_Detail = null;
            IValueFormatter valueFormatter = null;
            IValueFormatter valueFormatter_Detail = null;

            for (final ColumnProperties columnProperties : columnProfile.columnProperties) {
               if (columnId.equals(columnProperties.columnId)) {

                  valueFormat = columnProperties.valueFormat_Category;
                  valueFormatter = _columnManager.getValueFormatter(valueFormat);

                  valueFormat_Detail = columnProperties.valueFormat_Detail;
                  valueFormatter_Detail = _columnManager.getValueFormatter(valueFormat_Detail);

                  break;
               }
            }

            modelColDef.setIsColumnChecked(true);

            modelColDef.setColumnWidth(colDef.getColumnWidth());
            modelColDef.setValueFormatter_Category(valueFormat, valueFormatter);
            modelColDef.setValueFormatter_Detail(valueFormat_Detail, valueFormatter_Detail);

            allClonedAndSortedColumns.add(modelColDef);

            allClonedColDef.remove(colDef);
         }

         /*
          * Add not visible columns
          */
         for (final ColumnDefinition colDef : allClonedColDef) {

            final ValueFormat valueFormat = colDef.getDefaultValueFormat_Category();
            final ValueFormat valueFormat_Detail = colDef.getDefaultValueFormat_Detail();
            final IValueFormatter valueFormatter = _columnManager.getValueFormatter(valueFormat);
            final IValueFormatter valueFormatter_Detail = _columnManager.getValueFormatter(valueFormat_Detail);

            // set default values
            colDef.setIsColumnChecked(false);

            colDef.setColumnWidth(colDef.getDefaultColumnWidth());
            colDef.setValueFormatter_Category(valueFormat, valueFormatter);
            colDef.setValueFormatter_Detail(valueFormat_Detail, valueFormatter_Detail);

            allClonedAndSortedColumns.add(colDef);
         }

         /*
          * Set create index, otherwise save/restore do not work!!!
          */
         int createIndex = 0;
         for (final ColumnDefinition colDef : allClonedAndSortedColumns) {
            colDef.setCreateIndex(createIndex++);
         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return allClonedAndSortedColumns;
   }

   @Override
   protected void configureShell(final Shell newShell) {
      super.configureShell(newShell);
      newShell.setText(Messages.ColumnModifyDialog_Dialog_title);
   }

   private void createActions() {

      _actionShowHideCategory = new ActionCategoryColumn();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      // set default size
      final GridData gd = (GridData) dlgContainer.getLayoutData();
      gd.widthHint = 800;
      gd.heightHint = 900;

      restoreState_BeforeUI();

      createActions();
      createUI(dlgContainer);

      setupColumnsInColumnViewer();

      restoreState();

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _uiContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_uiContainer);
      GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(_uiContainer);
      {
         createUI_10_Profile(_uiContainer);
         createUI_12_ProfileViewer(_uiContainer);
         createUI_14_ProfileActions(_uiContainer);

         createUI_70_ColumnsHeader(_uiContainer);
         createUI_72_ColumnsViewer(_uiContainer);
         createUI_74_ColumnActions(_uiContainer);
         createUI_76_Hints(_uiContainer);

         createUI_80_ColumnAnnotations(_uiContainer);
      }
   }

   private void createUI_10_Profile(final Composite parent) {

      /*
       * Label: Profile
       */
      {
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.ColumnModifyDialog_Label_Profile);
         GridDataFactory.fillDefaults()//
               .align(SWT.FILL, SWT.CENTER)
               .span(2, 1)
               .applyTo(label);
      }

   }

   private void createUI_12_ProfileViewer(final Composite parent) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(7))
            .applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      final Table table = new Table(layoutContainer, //
            SWT.SINGLE
                  //						| SWT.H_SCROLL
                  //						| SWT.V_SCROLL
                  | SWT.BORDER
                  | SWT.FULL_SELECTION);

      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      _profileViewer = new CheckboxTableViewer(table);

      _profileViewer.setUseHashlookup(true);
      _profileViewer.setComparator(_profileComparator);

      _profileViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            return _dialog_Profiles.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onProfileViewer_Select(event);
         }
      });

      _profileViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
            actionOnProfile_Rename();
         }
      });

      /*
       * Create columns
       */
      TableViewerColumn tvc;
      {
         /*
          * Name
          */
         tvc = new TableViewerColumn(_profileViewer, SWT.NONE);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final ColumnProfile profile = ((ColumnProfile) cell.getElement());

               cell.setText(profile.name);
            }
         });
         tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(10));
      }
      {
         /*
          * Number of columns
          */
         tvc = new TableViewerColumn(_profileViewer, SWT.NONE);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final ColumnProfile profile = ((ColumnProfile) cell.getElement());

               final String[] visibleColumnIds = profile.getVisibleColumnIds();

               cell.setText(visibleColumnIds == null
                     ? UI.EMPTY_STRING
                     : Integer.toString(visibleColumnIds.length));
            }
         });
         tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(1));
      }
   }

   private void createUI_14_ProfileActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(5, 0, 0, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * Button: New
          */
         {
            _btnProfile_New = new Button(container, SWT.NONE);
            _btnProfile_New.setText(Messages.App_Action_New_WithConfirm);
            _btnProfile_New.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  actionOnProfile_Add();
               }
            });
            setButtonLayoutData(_btnProfile_New);

         }

         /*
          * Button: Rename
          */
         {
            _btnProfile_Rename = new Button(container, SWT.NONE);
            _btnProfile_Rename.setText(Messages.App_Action_Rename_WithConfirm);
            _btnProfile_Rename.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  actionOnProfile_Rename();
               }
            });
            setButtonLayoutData(_btnProfile_Rename);

         }

         // spacer
         new Label(container, SWT.NONE);

         /*
          * Button: Remove
          */
         {
            _btnProfile_Remove = new Button(container, SWT.NONE);
            _btnProfile_Remove.setText(Messages.App_Action_Remove_NoConfirm);
            _btnProfile_Remove.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  actionOnProfile_Remove();
               }
            });
            setButtonLayoutData(_btnProfile_Remove);
         }
      }
   }

   private void createUI_70_ColumnsHeader(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .indent(0, 20)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         /*
          * Action: Show/hide category
          */
         if (_isCategoryAvailable) {

            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionShowHideCategory);

            tbm.update(true);
         }

         /*
          * Label: Column
          */
         {
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.ColumnModifyDialog_Label_Column);
            GridDataFactory.fillDefaults()//
                  .align(SWT.FILL, SWT.END)
                  .applyTo(label);
         }
      }

      // 2nd column
      new Label(parent, SWT.NONE);
   }

   private void createUI_72_ColumnsViewer(final Composite parent) {

      final TableColumnLayout tableLayout = new TableColumnLayout();
      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      layoutContainer.setLayout(tableLayout);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

      /*
       * create table
       */
      final Table table = new Table(layoutContainer, SWT.CHECK | SWT.FULL_SELECTION | SWT.BORDER);

      table.setLayout(new TableLayout());
      table.setHeaderVisible(true);
      table.setLinesVisible(false);

      _columnViewer = new CheckboxTableViewer(table);

      defineAllColumns(tableLayout);
      reorderColumns(table);

      _columnViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            return _columnViewerModel.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _columnViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {

            final Object firstElement = ((IStructuredSelection) _columnViewer.getSelection()).getFirstElement();
            if (firstElement != null) {

               // check/uncheck current item

               _columnViewer.setChecked(firstElement, !_columnViewer.getChecked(firstElement));
            }
         }
      });

      _columnViewer.addCheckStateListener(new ICheckStateListener() {
         @Override
         public void checkStateChanged(final CheckStateChangedEvent event) {

            final ColumnDefinition colDef = (ColumnDefinition) event.getElement();

            if (colDef.canModifyVisibility()) {

               // keep the checked status
               colDef.setIsColumnChecked(event.getChecked());

               // select the checked item
               _columnViewer.setSelection(new StructuredSelection(colDef));

               updateUI_ProfileViewer();

            } else {

               // column can't be unchecked

               _columnViewer.setChecked(colDef, true);
            }

            // save columns in profile
            updateProfileModel_From_ColumnViewer();
         }
      });

      _columnViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            enableUpDownActions();
         }
      });

      /*
       * set drag adapter
       */
      _columnViewer.addDragSupport(
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
                  final ISelection selection = _columnViewer.getSelection();

                  _dndCheckedElements = _columnViewer.getCheckedElements();

                  transfer.setSelection(selection);
                  transfer.setSelectionSetTime(_dndDragStartViewerLeft = event.time & 0xFFFFFFFFL);

                  event.doit = !selection.isEmpty();
               }
            });

      /*
       * set drop adapter
       */
      final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_columnViewer) {

         private Widget _dragOverItem;

         @Override
         public void dragOver(final DropTargetEvent dropEvent) {

            // keep table item
            _dragOverItem = dropEvent.item;

            super.dragOver(dropEvent);
         }

         @Override
         public boolean performDrop(final Object data) {

            if (data instanceof StructuredSelection) {
               final StructuredSelection selection = (StructuredSelection) data;

               if (selection.getFirstElement() instanceof ColumnDefinition) {

                  final ColumnDefinition colDef = (ColumnDefinition) selection.getFirstElement();

                  final int location = getCurrentLocation();
                  final Table filterTable = _columnViewer.getTable();

                  /*
                   * check if drag was started from this item, remove the item before the new item
                   * is inserted
                   */
                  if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dndDragStartViewerLeft) {
                     _columnViewer.remove(colDef);
                  }

                  int filterIndex;

                  if (_dragOverItem == null) {

                     _columnViewer.add(colDef);
                     filterIndex = filterTable.getItemCount() - 1;

                  } else {

                     // get index of the target in the table
                     filterIndex = filterTable.indexOf((TableItem) _dragOverItem);
                     if (filterIndex == -1) {
                        return false;
                     }

                     if (location == LOCATION_BEFORE) {
                        _columnViewer.insert(colDef, filterIndex);
                     } else if (location == LOCATION_AFTER || location == LOCATION_ON) {
                        _columnViewer.insert(colDef, ++filterIndex);
                     }
                  }

                  // reselect filter item
                  _columnViewer.setSelection(new StructuredSelection(colDef));

                  // set focus to selection
                  filterTable.setSelection(filterIndex);
                  filterTable.setFocus();

                  // recheck items
                  _columnViewer.setCheckedElements(_dndCheckedElements);

                  enableUpDownActions();

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
               final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
               if (target == dragFilter) {
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

      _columnViewer.addDropSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            viewerDropAdapter);
   }

   private void createUI_74_ColumnActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).applyTo(container);
      {
         {
            /*
             * Button: Move Up
             */
            _btnColumn_MoveUp = new Button(container, SWT.NONE);
            _btnColumn_MoveUp.setText(Messages.ColumnModifyDialog_Button_move_up);
            _btnColumn_MoveUp.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  moveSelectionUp();
                  enableUpDownActions();
               }
            });
            setButtonLayoutData(_btnColumn_MoveUp);
         }

         {
            /*
             * Button: Move Down
             */
            _btnColumn_MoveDown = new Button(container, SWT.NONE);
            _btnColumn_MoveDown.setText(Messages.ColumnModifyDialog_Button_move_down);
            _btnColumn_MoveDown.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  moveSelectionDown();
                  enableUpDownActions();
               }
            });
            setButtonLayoutData(_btnColumn_MoveDown);
         }

         // spacer
         new Label(container, SWT.NONE);

         {
            /*
             * Button: Default columns
             */
            _btnColumn_Default = new Button(container, SWT.NONE);
            _btnColumn_Default.setText(Messages.ColumnModifyDialog_Button_default);
            _btnColumn_Default.setToolTipText(Messages.ColumnModifyDialog_Button_Default2_Tooltip);
            _btnColumn_Default.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent event) {
                  actionOnColumn_Select_DefaultColumns();
               }
            });
            setButtonLayoutData(_btnColumn_Default);
         }
         {
            /*
             * Button: Select all columns
             */
            _btnColumn_SelectAll = new Button(container, SWT.NONE);
            _btnColumn_SelectAll.setText(Messages.ColumnModifyDialog_Button_select_all);
            _btnColumn_SelectAll.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  actionOnColumn_Select_AllColumns();
               }
            });
            setButtonLayoutData(_btnColumn_SelectAll);
         }

         {
            /*
             * Button: Deselect all columns
             */
            _btnColumn_DeselectAll = new Button(container, SWT.NONE);
            _btnColumn_DeselectAll.setText(Messages.ColumnModifyDialog_Button_deselect_all);
            _btnColumn_DeselectAll.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  actionOnColumn_Select_NoColumns();
               }
            });
            setButtonLayoutData(_btnColumn_DeselectAll);
         }

         // spacer
         new Label(container, SWT.NONE);

         {
            /*
             * Button: Default width
             */
            _btnColumn_DefaultWidth = new Button(container, SWT.NONE);
            _btnColumn_DefaultWidth.setText(Messages.ColumnModifyDialog_Button_DefaultWidth);
            _btnColumn_DefaultWidth.setToolTipText(Messages.ColumnModifyDialog_Button_DefaultWidth_Tooltip);
            _btnColumn_DefaultWidth.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent event) {
                  actionOnColumn_Default_Width();
               }
            });
            setButtonLayoutData(_btnColumn_DefaultWidth);
         }

         {
            /*
             * Button: Sort
             */
            _btnColumn_Sort = new Button(container, SWT.NONE);
            _btnColumn_Sort.setText(Messages.ColumnModifyDialog_Button_Sort);
            _btnColumn_Sort.setToolTipText(Messages.ColumnModifyDialog_Button_Sort_Tooltip);
            _btnColumn_Sort.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent event) {
                  actionOnColumn_Default_Sort();
               }
            });
            setButtonLayoutData(_btnColumn_Sort);
         }
      }
   }

   private void createUI_76_Hints(final Composite parent) {

      // use a bulleted list to display this info
      final StyleRange style = new StyleRange();
      style.metrics = new GlyphMetrics(0, 0, 10);
      final Bullet bullet = new Bullet(style);

      final String infoText = Messages.ColumnModifyDialog_Label_Hints;
      final int lineCount = Util.countCharacter(infoText, '\n');

      final StyledText styledText = new StyledText(parent, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
      styledText.setText(infoText);
      styledText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      styledText.setLineBullet(1, lineCount, bullet);
      styledText.setLineWrapIndent(1, lineCount, 10);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(styledText);
   }

   private void createUI_80_ColumnAnnotations(final Composite parent) {

      {
         /*
          * Annotation: Column formatting
          */
         _chkShowColumnAnnotation_Formatting = new Button(parent, SWT.CHECK);
         _chkShowColumnAnnotation_Formatting.setText(Messages.ColumnModifyDialog_Checkbox_ShowFormatAnnotations);

         GridDataFactory.fillDefaults().span(2, 1).indent(0, 20).applyTo(_chkShowColumnAnnotation_Formatting);

         _chkShowColumnAnnotation_Formatting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               _isShowColumnAnnotation_Formatting = _chkShowColumnAnnotation_Formatting.getSelection();
            }
         });
      }
      {
         /*
          * Annotation: Column Sorting
          */
         _chkShowColumnAnnotation_Sorting = new Button(parent, SWT.CHECK);
         _chkShowColumnAnnotation_Sorting.setText(Messages.ColumnModifyDialog_Checkbox_ShowSortingAnnotations);

         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkShowColumnAnnotation_Sorting);

         _chkShowColumnAnnotation_Sorting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               _isShowColumnAnnotation_Sorting = _chkShowColumnAnnotation_Sorting.getSelection();
            }
         });
      }
   }

   private void defineAllColumns(final TableColumnLayout tableLayout) {

      defineColumn_10_ColumnName(tableLayout);
      defineColumn_20_ColumnHeaderText(tableLayout);
      defineColumn_30_Unit(tableLayout);
      defineColumn_40_Format_Category(tableLayout);
      defineColumn_50_Format_Tour(tableLayout);
      defineColumn_60_Width(tableLayout);

      /**
       * This column CANNOT be the first column because it would contain the checkbox, but with the
       * reorder feature this column is set as first column :-)
       */
      defineColumn_99_Category(tableLayout);
   }

   /**
    * Column: Label
    */
   private void defineColumn_10_ColumnName(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setMoveable(true);
      tc.setText(Messages.ColumnModifyDialog_column_column);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
            cell.setText(colDef.getColumnLabel());

            setColor(cell, colDef);
         }
      });
      tableLayout.setColumnData(tc, new ColumnWeightData(30, true));
   }

   /**
    * Column: Column header text
    */
   private void defineColumn_20_ColumnHeaderText(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setMoveable(true);
      tc.setText(Messages.ColumnModifyDialog_Column_HeaderText);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
            cell.setText(colDef.getColumnHeaderText(_columnManager));

            setColor(cell, colDef);
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(16), true));
   }

   /**
    * Column: Unit
    */
   private void defineColumn_30_Unit(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setText(Messages.ColumnModifyDialog_column_unit);
      tc.setMoveable(true);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
            cell.setText(colDef.getColumnUnit());

            setColor(cell, colDef);
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(12), true));
   }

   /**
    * Column: Format
    */
   private void defineColumn_40_Format_Category(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setMoveable(true);
      tc.setText(Messages.ColumnModifyDialog_Column_FormatCategory);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();

            ValueFormat valueFormat = colDef.getValueFormat_Category();

            if (valueFormat == null) {
               valueFormat = colDef.getDefaultValueFormat_Category();
            }

            if (valueFormat == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(FormatManager.getValueFormatterName(valueFormat));
            }

            setColor(cell, colDef);
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(13), true));
   }

   /**
    * Column: Detail format
    */
   private void defineColumn_50_Format_Tour(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setMoveable(true);
      tc.setText(Messages.ColumnModifyDialog_Column_FormatTour);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();

            ValueFormat valueFormat = colDef.getValueFormat_Detail();

            if (valueFormat == null) {
               valueFormat = colDef.getDefaultValueFormat_Detail();
            }

            if (valueFormat == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(FormatManager.getValueFormatterName(valueFormat));
            }

            setColor(cell, colDef);
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(13), true));
   }

   /**
    * Column: Width
    */
   private void defineColumn_60_Width(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.TRAIL);

      final TableColumn tc = tvc.getColumn();
      tc.setMoveable(true);
      tc.setText(Messages.ColumnModifyDialog_column_width);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
            cell.setText(Integer.toString(colDef.getColumnWidth()));

            setColor(cell, colDef);
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(10), true));
   }

   /**
    * Column: Category
    */
   private void defineColumn_99_Category(final TableColumnLayout tableLayout) {

      if (_isCategoryAvailable) {

         final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

         final TableColumn tc = tvc.getColumn();
         tc.setMoveable(true);
         tc.setText(Messages.ColumnModifyDialog_Column_Category);

         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
               cell.setText(colDef.getColumnCategory());

               setColor(cell, colDef);
            }
         });
         _categoryColumn = tc;

         _categoryColumnWidth = _pc.convertWidthInCharsToPixels(20);
         int categoryColumnWidth;
         if (_columnManager.isShowCategory()) {
            categoryColumnWidth = _categoryColumnWidth;
         } else {
            // hide column
            categoryColumnWidth = 0;
         }

         tableLayout.setColumnData(tc, new ColumnPixelData(categoryColumnWidth, true));
      }
   }

   private void enableActions() {

      _chkShowColumnAnnotation_Sorting.setEnabled(_columnManager.isNatTableColumnManager());
   }

   private void enableProfileActions() {

      final int numProfiles = _dialog_Profiles.size();

      _btnProfile_Remove.setEnabled(numProfiles > 1);
   }

   /**
    * Check if the up/down button are enabled
    */
   private void enableUpDownActions() {

      final Table table = _columnViewer.getTable();
      final TableItem[] items = table.getSelection();

      final boolean isSelected = items != null && items.length > 0;

      boolean isUpEnabled = isSelected;
      boolean isDownEnabled = isSelected;

      if (isSelected) {

         final int indices[] = table.getSelectionIndices();
         final int max = table.getItemCount();

         isUpEnabled = indices[0] != 0;
         isDownEnabled = indices[indices.length - 1] < max - 1;
      }

      // disable movable when a column is not allowed to be moved
      for (final TableItem tableItem : items) {
         final ColumnDefinition colDef = (ColumnDefinition) tableItem.getData();

         if (colDef.isColumnMoveable() == false) {
            isUpEnabled = false;
            isDownEnabled = false;

            break;
         }
      }

      _btnColumn_MoveUp.setEnabled(isUpEnabled);
      _btnColumn_MoveDown.setEnabled(isDownEnabled);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      final IDialogSettings state = CommonActivator.getState(getClass().getName() + "_DialogBounds");//$NON-NLS-1$
//		final IDialogSettings state = null;

      return state;
   }

   private ColumnProfile getSelectedProfile() {

      final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
      final ColumnProfile selectedProfile = (ColumnProfile) selection.getFirstElement();

      return selectedProfile;
   }

   /**
    * Moves an entry in the table to the given index.
    */
   private void move(final TableItem item, final int index) {

      final ColumnDefinition colDef = (ColumnDefinition) item.getData();

      // remove existing item
      item.dispose();

      // create new item
      _columnViewer.insert(colDef, index);
      _columnViewer.setChecked(colDef, colDef.isColumnCheckedInContextMenu());
   }

   /**
    * Move the current selection in the build list down.
    */
   private void moveSelectionDown() {

      final Table table = _columnViewer.getTable();

      final int indices[] = table.getSelectionIndices();
      if (indices.length < 1) {
         return;
      }

      final int newSelection[] = new int[indices.length];
      final int max = table.getItemCount() - 1;

      for (int i = indices.length - 1; i >= 0; i--) {
         final int index = indices[i];
         if (index < max) {
            move(table.getItem(index), index + 1);
            newSelection[i] = index + 1;
         }
      }

      table.setSelection(newSelection);
   }

   /**
    * Move the current selection in the build list up.
    */
   private void moveSelectionUp() {

      final Table table = _columnViewer.getTable();

      final int indices[] = table.getSelectionIndices();
      final int newSelection[] = new int[indices.length];

      for (int i = 0; i < indices.length; i++) {
         final int index = indices[i];
         if (index > 0) {
            move(table.getItem(index), index - 1);
            newSelection[i] = index - 1;
         }
      }

      table.setSelection(newSelection);
   }

   @Override
   protected void okPressed() {

      saveState();

      super.okPressed();
   }

   private void onProfileViewer_Select(final SelectionChangedEvent event) {

      if (_isInUpdate) {
         return;
      }

      final ColumnProfile selectedProfile = getSelectedProfile();

      if (selectedProfile == _selectedProfile) {
         // no new selection, this occurs when another profile is checked
         return;
      }

      // keep previous selected columns
      updateProfileModel_From_ColumnViewer();

      _selectedProfile = selectedProfile;

      setupColumnProfile(selectedProfile);
   }

   /**
    * Reorder columns, set category column to the first but the checkbox keeps with the column name
    * column.
    */
   private void reorderColumns(final Table table) {

      if (_isCategoryAvailable) {

         final int[] oldColumnOrder = table.getColumnOrder();
         final int numColumns = oldColumnOrder.length;
         final int[] newColumnOrder = new int[numColumns];

         // set last column to the first
         newColumnOrder[0] = oldColumnOrder[numColumns - 1];

         for (int columnIndex = 1; columnIndex < numColumns; columnIndex++) {
            newColumnOrder[columnIndex] = oldColumnOrder[columnIndex - 1];
         }

         table.setColumnOrder(newColumnOrder);
      }
   }

   private void restoreState() {

      // show/hide category
      _isShowCategory = _columnManager.isShowCategory();
      _actionShowHideCategory.setChecked(_isShowCategory);

      // show/hide column annotations
      _isShowColumnAnnotation_Formatting = _columnManager.isShowColumnAnnotation_Formatting();
      _chkShowColumnAnnotation_Formatting.setSelection(_isShowColumnAnnotation_Formatting);

      // show/hide column annotations
      _isShowColumnAnnotation_Sorting = _columnManager.isShowColumnAnnotation_Sorting();
      _chkShowColumnAnnotation_Sorting.setSelection(_isShowColumnAnnotation_Sorting);

      // load viewer
      _profileViewer.setInput(new Object());

      // select active profile
      _profileViewer.setSelection(new StructuredSelection(_selectedProfile), true);

      enableProfileActions();
      enableActions();
   }

   private void restoreState_BeforeUI() {

      _isCategoryAvailable = _columnManager.isCategoryAvailable();
   }

   private void saveState() {

      updateProfileModel_From_ColumnViewer();

      // replace column mgr profiles
      _columnMgr_Profiles.clear();
      _columnMgr_Profiles.addAll(_dialog_Profiles);

      _columnManager.setIsShowCategory(_isShowCategory);
      _columnManager.setIsShowColumnAnnotation_Formatting(_isShowColumnAnnotation_Formatting);
      _columnManager.setIsShowColumnAnnotation_Sorting(_isShowColumnAnnotation_Sorting);

      _columnManager.updateColumns(_selectedProfile, _columnViewer.getTable().getItems());
   }

   private void setColor(final ViewerCell cell, final ColumnDefinition colDef) {

      // paint columns in a different color which can't be hidden
      if (colDef.canModifyVisibility() == false) {
         cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
      }
   }

   /**
    * Update column viewer from the selected profile
    *
    * @param selectedProfile
    */
   private void setupColumnProfile(final ColumnProfile selectedProfile) {

      _columnViewerModel = cloneAllColumns(selectedProfile);

      setupColumnsInColumnViewer();

      enableProfileActions();
   }

   private void setupColumnsInColumnViewer() {

      // run async because displaying the column table it is soooo slow, with async it seems also to be faster
      _columnViewer.getTable().getDisplay().asyncExec(() -> {

         // load columns into the viewer
         _columnViewer.setInput(new Object[0]);

         // check columns
         final ArrayList<ColumnDefinition> checkedColumns = new ArrayList<>();

         for (final ColumnDefinition colDef : _columnViewerModel) {
            if (colDef.isColumnCheckedInContextMenu()) {
               checkedColumns.add(colDef);
            }
         }
         _columnViewer.setCheckedElements(checkedColumns.toArray());

         enableUpDownActions();

         // force that horizontal scrollbar is NOT visible
         _uiContainer.layout(true, true);
      });
   }

   private void sortDialogProfiles() {

      Collections.sort(_dialog_Profiles, new Comparator<ColumnProfile>() {
         @Override
         public int compare(final ColumnProfile colProfile1, final ColumnProfile colProfile2) {
            return colProfile1.name.compareTo(colProfile2.name);
         }
      });
   }

   /**
    * Set {@link ColumnProfile#visibleColumnIds} from the current column viewer into the current
    * profile.
    */
   private void updateProfileModel_From_ColumnViewer() {

      // update profile
      _columnManager.setVisibleColumnIds_FromModifyDialog(
            _selectedProfile,
            _columnViewer.getTable().getItems());

      /*
       * Update value formats from the model
       */
      for (final ColumnDefinition colDef : _columnViewerModel) {

         final String columnId = colDef.getColumnId();

         for (final ColumnProperties columnProperties : _selectedProfile.columnProperties) {

            if (columnId.equals(columnProperties.columnId)) {

               columnProperties.valueFormat_Category = colDef.getValueFormat_Category();
               columnProperties.valueFormat_Detail = colDef.getValueFormat_Detail();

               break;
            }
         }
      }
   }

   private void updateProfileModel_From_Model(final ArrayList<ColumnDefinition> columnViewerModel) {

      // update profile
      _columnManager.setVisibleColumnIds_FromModel(_selectedProfile, columnViewerModel);

      /*
       * Update value formats from the model
       */
      for (final ColumnDefinition colDef : _columnViewerModel) {

         final String columnId = colDef.getColumnId();

         for (final ColumnProperties columnProperties : _selectedProfile.columnProperties) {

            if (columnId.equals(columnProperties.columnId)) {

               columnProperties.valueFormat_Category = colDef.getValueFormat_Category();
               columnProperties.valueFormat_Detail = colDef.getValueFormat_Detail();

               break;
            }
         }
      }
   }

   /**
    * Update current profile in the profile viewer to show modified number of columns
    */
   private void updateUI_ProfileViewer() {

      // run async otherwise it is displayed by the next checkbox change
      _profileViewer.getTable().getDisplay().asyncExec(() -> {

         // update profile viewer
         _profileViewer.update(getSelectedProfile(), null);
      });
   }

}
