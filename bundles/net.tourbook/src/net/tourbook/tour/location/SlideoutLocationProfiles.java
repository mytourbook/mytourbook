/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.dialog.MessageDialog_OnTop;
import net.tourbook.common.form.SashBottomFixedForm;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.location.TourLocationManager.Zoomlevel;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DLItem;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DualList;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * Slideout for the start/end location
 */
public class SlideoutLocationProfiles extends AdvancedSlideout {

   private static final String             ID                = "net.tourbook.tour.location.SlideoutLocationProfiles"; //$NON-NLS-1$

   private static final String             ZOOM_LEVEL_ITEM   = "%3d  %s";                                             //$NON-NLS-1$

   private static final String             STATE_SASH_HEIGHT = "STATE_SASH_HEIGHT";                                   //$NON-NLS-1$

   private static final IDialogSettings    _state            = TourbookPlugin.getState(ID);

   private PixelConverter                  _pc;

   private boolean                         _isStartLocation;

   private TableViewer                     _profileViewer;

   private ModifyListener                  _defaultModifyListener;
   private FocusListener                   _keepOpenListener;

   private boolean                         _isInUpdateUI;

   private final List<TourLocationProfile> _allProfiles      = TourLocationManager.getProfiles();
   private TourLocationProfile             _selectedProfile;

   private List<MT_DLItem>                 _allDualListItems = new ArrayList<>();

   /**
    * Can be <code>null</code>
    */
   private ITourLocationConsumer           _tourLocationConsumer;
   private TourLocation                    _tourLocation;

   private Rectangle                       _ownerBounds;

   /*
    * UI controls
    */
   private Composite           _parent;
   private MT_DualList         _listLocationParts;

   private SashBottomFixedForm _sashForm;
   private Sash                _sashSlider;
   private Composite           _sashTop_Flex;
   private Composite           _sashBottom_Fixed;

   private Button              _btnApplyAndClose;
   private Button              _btnCopyProfile;
   private Button              _btnDefaultProfile;
   private Button              _btnDeleteProfile;

   private Combo               _comboZoomlevel;

   private Label               _lblLocationParts;
   private Label               _lblProfileName;
   private Label               _lblProfiles;
   private Label               _lblSelectedLocationParts;
   private Label               _lblZoomlevel;

   private Text                _txtProfileName;
   private Text                _txtSelectedLocationParts;

   private class LocationProfileComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final TourLocationProfile profile1 = (TourLocationProfile) e1;
         final TourLocationProfile profile2 = (TourLocationProfile) e2;

         return profile1.getName().compareTo(profile2.getName());
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }
   }

   private class LocationProfileProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allProfiles.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private final class ProfileEditingSupport extends EditingSupport {

      private final CheckboxCellEditor _cellEditor;

      private ProfileEditingSupport(final TableViewer tableViewer) {

         super(tableViewer);

         _cellEditor = new CheckboxCellEditor(tableViewer.getTable());
      }

      @Override
      protected boolean canEdit(final Object element) {

         if (element instanceof final TourLocationProfile locationProfile) {

            /*
             * Only another profile can be edited
             */
            final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

            final boolean isSameProfile = locationProfile.equals(defaultProfile);
            final boolean canEdit = isSameProfile == false;

            return canEdit;
         }

         return false;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {

         return _cellEditor;
      }

      @Override
      protected Object getValue(final Object element) {

         if (element instanceof final TourLocationProfile locationProfile) {

            final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

            final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

            return isDefaultProfile ? Boolean.TRUE : Boolean.FALSE;
         }

         return Boolean.FALSE;
      }

      @Override
      protected void setValue(final Object element, final Object value) {

         if (element instanceof final TourLocationProfile locationProfile) {

            TourLocationManager.setDefaultProfile(locationProfile);

            // update default state for all profiles
            _profileViewer.update(_allProfiles.toArray(), null);

            if (_tourLocationConsumer != null) {
               _tourLocationConsumer.defaultProfileIsUpdated();
            }
         }
      }
   }

   /**
    * @param tourLocationConsumer
    *           Can be <code>null</code>
    * @param tourData
    * @param ownerControl
    * @param ownerBounds
    * @param state
    * @param isStartLocation
    */
   public SlideoutLocationProfiles(final ITourLocationConsumer tourLocationConsumer,
                                   final TourLocation tourLocation,
                                   final Control ownerControl,
                                   final Rectangle ownerBounds,
                                   final IDialogSettings state,
                                   final boolean isStartLocation) {

      super(ownerControl, state, new int[] { 800, 800 });

      _ownerBounds = ownerBounds;

      _tourLocationConsumer = tourLocationConsumer;
      _tourLocation = tourLocation;
      _isStartLocation = isStartLocation;

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      final String title = isStartLocation
            ? Messages.Slideout_TourLocation_Label_StartLocation_Title
            : Messages.Slideout_TourLocation_Label_EndLocation_Title;

      setTitleText(title);
   }

   private void addAllAddressParts(final TourLocation tourLocation) {

      try {

         final Field[] allAddressFields = tourLocation.getClass().getFields();

         // loop: all fields in the retrieved address
         for (final Field field : allAddressFields) {

            final String fieldName = field.getName();

            // skip field names which are not address parts
            if (TourLocation.IGNORED_FIELDS.contains(fieldName)) {
               continue;
            }

            final Object fieldValue = field.get(tourLocation);

            if (fieldValue instanceof final String stringValue) {

               // use only fields with a value
               if (stringValue.length() > 0) {

                  final LocationPartID locationPart = LocationPartID.valueOf(fieldName);

                  final String label = TourLocationManager.ALL_LOCATION_PART_AND_LABEL.get(locationPart);

                  final MT_DLItem dlItem = new MT_DLItem(

                        label,
                        stringValue,

                        TourLocationManager.KEY_LOCATION_PART_ID,
                        locationPart);

                  _allDualListItems.add(dlItem);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void addCombinedPart(final LocationPartID locationPart,
                                final String partValue,
                                final List<MT_DLItem> allParts) {

      if (StringUtils.hasContent(partValue)) {

         final String partName = TourLocationManager.createPartName_Combined(locationPart);

         allParts.add(new MT_DLItem(

               partName,
               partValue,

               TourLocationManager.KEY_LOCATION_PART_ID,
               locationPart));
      }
   }

   private void createColumn_10_ProfileName(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
      final TableColumn tc = tvc.getColumn();
      tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocationProfile profile = (TourLocationProfile) cell.getElement();

            cell.setText(profile.getName());
         }
      });

      tableLayout.setColumnData(tc, new ColumnWeightData(1, true));
   }

   private void createColumn_12_Zoomlevel(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_profileViewer, SWT.CENTER);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Slideout_TourLocation_Column_Zoomlevel);

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourLocationProfile profile = (TourLocationProfile) cell.getElement();

            cell.setText(Integer.toString(profile.getZoomlevel()));
         }
      });

      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 10));
   }

   private void createColumn_20_LocationParts(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Slideout_TourLocation_Column_LocationParts);

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourLocationProfile profile = (TourLocationProfile) cell.getElement();

            cell.setText(TourLocationManager.createJoinedPartNames(profile, UI.SYMBOL_COMMA + UI.SPACE));
         }
      });

      tableLayout.setColumnData(tc, new ColumnWeightData(2, true));
   }

   private void createColumn_30_LocationText(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Slideout_TourLocation_Column_LocationText);

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourLocationProfile profile = (TourLocationProfile) cell.getElement();

            cell.setText(TourLocationManager.createLocationDisplayName(_tourLocation, profile));
         }
      });

      tableLayout.setColumnData(tc, new ColumnWeightData(2, true));
   }

   private void createColumn_40_IsDefaultProfile(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_profileViewer, SWT.CENTER);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Slideout_TourLocation_Column_Default);
      tc.setToolTipText(Messages.Slideout_TourLocation_Column_Default_Tooltip);

      tvc.setEditingSupport(new ProfileEditingSupport(_profileViewer));

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourLocationProfile locationProfile = (TourLocationProfile) cell.getElement();
            final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

            cell.setText(locationProfile.equals(defaultProfile)
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });

      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 10));
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      setupUI();

      // load profile viewer
      _profileViewer.setInput(new Object());

      fillUI();

      restoreState();
   }

   private void createUI(final Composite parent) {

      final Composite sashContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(sashContainer);
//    container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         // top part
         _sashTop_Flex = createUI_20_Profiles(sashContainer);

         // sash
         _sashSlider = new Sash(sashContainer, SWT.HORIZONTAL);
         UI.addSashColorHandler(_sashSlider);

         // bottom part
         _sashBottom_Fixed = createUI_40_Parts(sashContainer);

         _sashForm = new SashBottomFixedForm(
               sashContainer,
               _sashTop_Flex,
               _sashSlider,
               _sashBottom_Fixed);
      }
   }

   private Composite createUI_20_Profiles(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            // label: Profiles

            _lblProfiles = new Label(container, SWT.NONE);
            _lblProfiles.setText(Messages.Slideout_TourFilter_Label_Profiles);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_lblProfiles);
         }

         createUI_22_ProfileViewer(container);
         createUI_24_ProfileActions(container);
      }

      return container;
   }

   private void createUI_22_ProfileViewer(final Composite parent) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * Create table
       */
      final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

      table.setLayout(new TableLayout());

      table.setHeaderVisible(true);

      _profileViewer = new TableViewer(table);

      /*
       * Create columns
       */
      createColumn_10_ProfileName(tableLayout);
      createColumn_12_Zoomlevel(tableLayout);
      createColumn_20_LocationParts(tableLayout);
      createColumn_30_LocationText(tableLayout);
      createColumn_40_IsDefaultProfile(tableLayout);

      /*
       * Create table viewer
       */
      _profileViewer.setContentProvider(new LocationProfileProvider());
      _profileViewer.setComparator(new LocationProfileComparator());

      _profileViewer.addSelectionChangedListener(selectionChangedEvent -> onProfile_Select());

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

   private void createUI_24_ProfileActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Button: New
             */
            final Button button = new Button(container, SWT.PUSH);
            button.setText(Messages.Slideout_TourFilter_Action_AddProfile);
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Add()));

            // set button default width
            UI.setButtonLayoutData(button);
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
             * Button: Default
             */
            _btnDefaultProfile = new Button(container, SWT.PUSH);
            _btnDefaultProfile.setText(Messages.Slideout_TourLocation_Action_DefaultProfile);
            _btnDefaultProfile.setToolTipText(Messages.Slideout_TourLocation_Action_DefaultProfile_Tooltip);
            _btnDefaultProfile.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Default()));

            // set button default width
            UI.setButtonLayoutData(_btnDefaultProfile);
         }
         {
            /*
             * Button: Delete
             */
            _btnDeleteProfile = new Button(container, SWT.PUSH);
            _btnDeleteProfile.setText(Messages.Slideout_TourFilter_Action_DeleteProfile);
            _btnDeleteProfile.setToolTipText(Messages.Slideout_TourFilter_Action_DeleteProfile_Tooltip);
            _btnDeleteProfile.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Delete()));

            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .align(SWT.FILL, SWT.END)
                  .applyTo(_btnDeleteProfile);

            // set button default width
            UI.setButtonLayoutWidth(_btnDeleteProfile);
         }
      }
   }

   private Composite createUI_40_Parts(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         createUI_50_PartData(container);
         createUI_60_PartSelector(container);
      }

      return container;
   }

   private void createUI_50_PartData(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            // label: Profile name

            _lblProfileName = new Label(container, SWT.NONE);
            _lblProfileName.setText(Messages.Slideout_TourFilter_Label_ProfileName);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblProfileName);
         }
         {
            // text: Profile name

            _txtProfileName = new Text(container, SWT.BORDER);
            _txtProfileName.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_txtProfileName);
         }
         UI.createSpacer_Horizontal(container);
      }
      {
         /*
          * Zoomlevel
          */
         _lblZoomlevel = UI.createLabel(container,
               Messages.Slideout_TourLocation_Label_Zoomlevel,
               Messages.Slideout_TourLocation_Label_Zoomlevel_Tooltip);

         _comboZoomlevel = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboZoomlevel.setVisibleItemCount(20);
         _comboZoomlevel.setToolTipText(Messages.Slideout_TourLocation_Label_Zoomlevel_Tooltip);
         _comboZoomlevel.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onProfile_Modify()));
         _comboZoomlevel.addFocusListener(_keepOpenListener);

         UI.createSpacer_Horizontal(container);
      }
      {
         // label: selected location parts

         _lblSelectedLocationParts = new Label(container, SWT.NONE);
         _lblSelectedLocationParts.setText(Messages.Slideout_TourLocation_Label_SelectedLocationParts);
         GridDataFactory.fillDefaults().applyTo(_lblSelectedLocationParts);

         UI.createSpacer_Horizontal(container, 2);
      }
      {
         {
            // text: selected location parts

            _txtSelectedLocationParts = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.BEGINNING)
                  .grab(true, false)
                  .span(2, 1)

                  // align to the label position
                  .indent(-3, 0)
                  .applyTo(_txtSelectedLocationParts);
         }
         {
            // button: Apply & Close

            _btnApplyAndClose = new Button(container, SWT.PUSH);
            _btnApplyAndClose.setText(OtherMessages.APP_ACTION_APPLY_AND_CLOSE);
            _btnApplyAndClose.setToolTipText(Messages.Slideout_TourLocation_Action_ApplyAndClose_Tooltip);
            _btnApplyAndClose.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> doApplyAndClose()));
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.BEGINNING)
                  .applyTo(_btnApplyAndClose);

            // set button default width
            UI.setButtonLayoutWidth(_btnApplyAndClose);
         }
      }
   }

   private void createUI_60_PartSelector(final Composite parent) {

      {
         /*
          * Part dual list
          */

         // label
         _lblLocationParts = new Label(parent, SWT.NONE);
         _lblLocationParts.setText(Messages.Slideout_TourLocation_Label_LocationParts);

         // dual list
         _listLocationParts = new MT_DualList(parent, SWT.NONE);
         _listLocationParts.addSelectionChangeListener(selectionChangeListener -> onSelectParts());
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_listLocationParts);
      }
      {
         /*
          * Remarks
          */

         final Label label = new Label(parent, SWT.WRAP);
         label.setText(Messages.Slideout_TourLocation_Label_Remarks);
      }
   }

   private void doApplyAndClose() {

      if (_isStartLocation) {

         _tourLocationConsumer.setTourStartLocation(_txtSelectedLocationParts.getText());

      } else {

         _tourLocationConsumer.setTourEndLocation(_txtSelectedLocationParts.getText());
      }

      TourLocationManager.saveState();

      close();
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isLocationConsumer    = _tourLocationConsumer != null;
      final boolean isProfileSelected     = _selectedProfile != null;
      final boolean hasProfiles           = _allProfiles.size() > 0;

      _btnApplyAndClose          .setEnabled(isProfileSelected && isLocationConsumer);
      _btnCopyProfile            .setEnabled(isProfileSelected);
      _btnDefaultProfile         .setEnabled(isProfileSelected);
      _btnDeleteProfile          .setEnabled(isProfileSelected);

      _lblLocationParts          .setEnabled(isProfileSelected);
      _lblProfileName            .setEnabled(isProfileSelected);
      _lblSelectedLocationParts  .setEnabled(isProfileSelected);
      _lblZoomlevel              .setEnabled(isProfileSelected);

      _comboZoomlevel            .setEnabled(isProfileSelected);

      _txtProfileName            .setEnabled(isProfileSelected);

      _lblProfiles               .setEnabled(hasProfiles);
      _profileViewer.getTable()  .setEnabled(hasProfiles);

      _listLocationParts         .setEnabled(isProfileSelected);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      for (final Zoomlevel zoomlevel : TourLocationManager.ALL_ZOOM_LEVEL) {

         _comboZoomlevel.add(ZOOM_LEVEL_ITEM.formatted(zoomlevel.zoomlevel, zoomlevel.label));
      }
   }

   @Override
   protected Rectangle getParentBounds() {

      return _ownerBounds;
   }

   private int getSelectedZoomlevel() {

      int selectionIndex = _comboZoomlevel.getSelectionIndex();

      if (selectionIndex < 0) {
         selectionIndex = 0;
      }

      return TourLocationManager.ALL_ZOOM_LEVEL[selectionIndex].zoomlevel;
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _defaultModifyListener = modifyEvent -> onProfile_Modify();

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsKeepOpenInternally(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsKeepOpenInternally(false);
         }
      };

      // cleanup previous slideout openings
      _selectedProfile = null;
      _allDualListItems.clear();

      // allow this slideout to can have the max screen height
      setMaxHeightFactor(1);
   }

   @Override
   protected void onFocus() {

   }

   private void onProfile_Add() {

      final TourLocationProfile locationProfile = new TourLocationProfile();

      // update model
      _allProfiles.add(locationProfile);

      // update viewer
      _profileViewer.refresh();

      // select new profile
      selectProfile(locationProfile);

      _txtProfileName.setFocus();
   }

   private void onProfile_Copy() {

      if (_selectedProfile == null) {
         // ignore
         return;
      }

      final TourLocationProfile locationProfile = _selectedProfile.clone();

      // update model
      _allProfiles.add(locationProfile);

      // update viewer
      _profileViewer.refresh();

      // select new profile
      selectProfile(locationProfile);

      _txtProfileName.setFocus();
   }

   private void onProfile_Default() {

      if (_selectedProfile == null) {
         // ignore
         return;
      }

      TourLocationManager.setDefaultProfile(_selectedProfile);

      // update default state for all profiles
      _profileViewer.update(_allProfiles.toArray(), null);

      if (_tourLocationConsumer != null) {
         _tourLocationConsumer.defaultProfileIsUpdated();
      }
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

               NLS.bind(Messages.Slideout_TourFilter_Confirm_DeleteProfile_Message, _selectedProfile.getName()),
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
      _allProfiles.remove(_selectedProfile);
      TourLocationManager.setDefaultProfile(null);

      if (_tourLocationConsumer != null) {
         _tourLocationConsumer.defaultProfileIsUpdated();
      }

      // update UI
      _profileViewer.remove(_selectedProfile);

      /*
       * Select another profile at the same position
       */
      final int numProfiles = _allProfiles.size();
      final int nextLocationIndex = Math.min(numProfiles - 1, lastIndex);

      final Object nextSelectedProfile = _profileViewer.getElementAt(nextLocationIndex);
      if (nextSelectedProfile == null) {

         _selectedProfile = null;

         onSelectParts();

      } else {

         selectProfile((TourLocationProfile) nextSelectedProfile);
      }

      enableControls();

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
      _selectedProfile.zoomlevel = getSelectedZoomlevel();

      // a refresh() is needed to resort the viewer
      _profileViewer.refresh();
   }

   private void onProfile_Select() {

      _isInUpdateUI = true;

      TourLocationProfile selectedProfile = null;

      // get selected profile from viewer
      final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
      final Object firstElement = selection.getFirstElement();
      if (firstElement != null) {
         selectedProfile = (TourLocationProfile) firstElement;
      }

      if (_selectedProfile != null && _selectedProfile == selectedProfile) {

         // a new profile is not selected

//         _isInUpdateUI = false;
//
//         return;
      }

      _selectedProfile = selectedProfile;

      /*
       * Update UI
       */

      // set profile name
      if (_selectedProfile == null) {

         _txtProfileName.setText(UI.EMPTY_STRING);

         _isInUpdateUI = false;

         return;

      } else {

         _txtProfileName.setText(_selectedProfile.getName());

         if (_selectedProfile.getName().equals(Messages.Tour_Filter_Default_ProfileName)) {

            // a default profile is selected, make is easy to rename it

            _txtProfileName.selectAll();
            _txtProfileName.setFocus();
         }
      }

      // zoomlevel
      _comboZoomlevel.select(TourLocationManager.getZoomlevelIndex(_selectedProfile.getZoomlevel()));

      /*
       * Set selected/not selected parts
       */
      final List<LocationPartID> allProfileParts = _selectedProfile.allParts;

      final Set<LocationPartID> allRemainingProfileParts = new HashSet<>();
      allRemainingProfileParts.addAll(allProfileParts);

      final List<MT_DLItem> allNotSelectedItems = new ArrayList<>();
      final List<MT_DLItem> allSelectedItems = new ArrayList<>();
      final List<MT_DLItem> allSelectedAndSortedItems = new ArrayList<>();

      nextPart:

      // loop: all available parts
      for (final MT_DLItem dualListItem : _allDualListItems) {

         final LocationPartID dualListPart = (LocationPartID) dualListItem.getData(TourLocationManager.KEY_LOCATION_PART_ID);

         // loop: all profile parts
         for (final LocationPartID profilePart : allProfileParts) {

            if (dualListPart.equals(profilePart)) {

               // part is selected

               allSelectedItems.add(dualListItem);

               dualListItem.setLastAction(MT_DLItem.LAST_ACTION.SELECTION);

               // update remaining parts
               allRemainingProfileParts.remove(profilePart);

               // continue with the next part
               continue nextPart;
            }
         }

         // part is not selected

         allNotSelectedItems.add(dualListItem);

         dualListItem.setLastAction(MT_DLItem.LAST_ACTION.DESELECTION);
      }

      // resort selected items to be sorted like in the profile
      for (final LocationPartID profilePartID : allProfileParts) {

         for (final MT_DLItem dlItem : allSelectedItems) {

            final LocationPartID dlItemPartId = (LocationPartID) dlItem.getData(TourLocationManager.KEY_LOCATION_PART_ID);

            if (profilePartID.equals(dlItemPartId)) {

               allSelectedAndSortedItems.add(dlItem);

               continue;
            }
         }
      }

      // add profile parts which are not available in the downloaded parts
      for (final LocationPartID remainingPart : allRemainingProfileParts) {

         final MT_DLItem dlItem = new MT_DLItem(

               TourLocationManager.createPartName_NotAvailable(remainingPart),
               UI.EMPTY_STRING,

               TourLocationManager.KEY_LOCATION_PART_ID,
               remainingPart);

         // set n/a flag
         dlItem.setData(TourLocationManager.KEY_IS_NOT_AVAILABLE, Boolean.TRUE);

         // show it in the selection list
         dlItem.setLastAction(MT_DLItem.LAST_ACTION.SELECTION);

         allSelectedAndSortedItems.add(dlItem);

         _allDualListItems.add(dlItem);
      }

      // complicated, recreate item list that they are sorted correctly
      _allDualListItems.clear();
      _allDualListItems.addAll(allSelectedAndSortedItems);
      _allDualListItems.addAll(allNotSelectedItems);

      _listLocationParts.setItems(_allDualListItems);

      onSelectParts();

      enableControls();

      _isInUpdateUI = false;
   }

   /**
    * Update model/UI from the selected parts
    */
   private void onSelectParts() {

      if (_selectedProfile == null) {
         return;
      }

      // get selected parts
      final List<MT_DLItem> allSelectedItems = _listLocationParts.getSelectionAsList();

      final List<LocationPartID> allProfileParts = _selectedProfile.allParts;

      final Set<LocationPartID> allRemainingProfileParts = new HashSet<>();
      allRemainingProfileParts.addAll(allProfileParts);

      /*
       * Update model
       */
      allProfileParts.clear();

      for (final MT_DLItem partItem : allSelectedItems) {

         // !!! a part item can contain also deselected items when all items are removed from the right side !!!

         if (partItem.getLastAction().equals(MT_DLItem.LAST_ACTION.SELECTION)) {

            final LocationPartID locationPart = (LocationPartID) partItem.getData(TourLocationManager.KEY_LOCATION_PART_ID);

            allProfileParts.add(locationPart);

            allRemainingProfileParts.remove(locationPart);
         }
      }

      /*
       * Update UI
       */
      final String locationDisplayName = TourLocationManager.createLocationDisplayName(allSelectedItems);
      _txtSelectedLocationParts.setText(locationDisplayName);

      // update viewer
      _profileViewer.refresh(_selectedProfile, true, true);

      // color needs to be set very late otherwise the dark theme do not display it (overwrite it)
      _parent.getDisplay().asyncExec(() -> {

         if (_txtSelectedLocationParts.isDisposed()) {
            return;
         }

         _txtSelectedLocationParts.setForeground(UI.IS_DARK_THEME
               ? UI.SYS_COLOR_YELLOW
               : UI.SYS_COLOR_BLUE);
      });
   }

   private void restoreState() {

      /*
       * Get previous default profile
       */
      TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      if (defaultProfile == null) {

         // select first profile

         defaultProfile = (TourLocationProfile) _profileViewer.getElementAt(0);
      }

      if (defaultProfile != null) {
         selectProfile(defaultProfile);
      }

      final int bottomPartHeight = Util.getStateInt(_state, STATE_SASH_HEIGHT, _pc.convertWidthInCharsToPixels(10));
      _sashForm.setFixedHeight(bottomPartHeight);

      enableControls();
   }

   @Override
   protected void saveState() {

      // save slideout position/size
      super.saveState();

// create Java code for the default profiles
//
//      TourLocationManager.createDefaultProfiles_JavaCode();
   }

   @Override
   protected void saveState_BeforeDisposed() {

      final int sashFixedHeight = _sashBottom_Fixed.getSize().y;
      final int sashSliderHeight = _sashSlider.getSize().y;

      _state.put(STATE_SASH_HEIGHT, sashFixedHeight - sashSliderHeight);
   }

   private void selectProfile(final TourLocationProfile selectedProfile) {

      final int zoomlevel = selectedProfile.getZoomlevel();
      _comboZoomlevel.select(TourLocationManager.getZoomlevelIndex(zoomlevel));

      _profileViewer.setSelection(new StructuredSelection(selectedProfile));

      final Table table = _profileViewer.getTable();
      table.setSelection(table.getSelectionIndices());
   }

   private void setupUI() {

      /*
       * Fill address part widget
       */

// SET_FORMATTING_OFF

      // add customized parts
      final String streetWithHouseNumber  = TourLocationManager.getCombined_StreetWithHouseNumber( _tourLocation);

      addCombinedPart(LocationPartID    .OSM_DEFAULT_NAME,                 _tourLocation.display_name,    _allDualListItems);
      addCombinedPart(LocationPartID    .OSM_NAME,                         _tourLocation.name,            _allDualListItems);


      addCombinedPart(LocationPartID    .CUSTOM_STREET_WITH_HOUSE_NUMBER,  streetWithHouseNumber,        _allDualListItems);


// SET_FORMATTING_ON

      // add address parts
      addAllAddressParts(_tourLocation);

      _listLocationParts.setItems(_allDualListItems);
   }

}
