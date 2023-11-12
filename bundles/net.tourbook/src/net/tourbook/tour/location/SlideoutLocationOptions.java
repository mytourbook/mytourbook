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
import net.tourbook.common.UI;
import net.tourbook.common.dialog.MessageDialog_OnTop;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
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
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for the start/end location
 */
public class SlideoutLocationOptions extends AdvancedSlideout {

   private static final Font               _boldFont         = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private ToolItem                        _toolItem;

   private PixelConverter                  _pc;

   private TourData                        _tourData;
   private boolean                         _isStartLocation;

   private TableViewer                     _profileViewer;

   private ModifyListener                  _defaultModifyListener;

   private final List<TourLocationProfile> _allProfiles      = TourLocationManager.getProfiles();
   private TourLocationProfile             _selectedProfile;

   private List<MT_DLItem>                 _allDualListItems = new ArrayList<>();

   /*
    * UI controls
    */
   private Composite             _parent;
   private MT_DualList           _listLocationParts;

   private Button                _btnApply;
   private Button                _btnCopyProfile;
   private Button                _btnDeleteProfile;

   private Label                 _lblDefaultName;
   private Label                 _lblLocationParts;
   private Label                 _lblProfileName;
   private Label                 _lblProfiles;
   private Label                 _lblSelectedLocationParts;

   private Text                  _txtDefaultName;
   private Text                  _txtProfileName;
   private Text                  _txtSelectedLocationParts;

   private ITourLocationConsumer _tourLocationConsumer;

   private class LocationProfileComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final TourLocationProfile profile1 = (TourLocationProfile) e1;
         final TourLocationProfile profile2 = (TourLocationProfile) e2;

         return profile1.name.compareTo(profile2.name);
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

   public SlideoutLocationOptions(final ToolItem toolItem,
                                  final IDialogSettings state,
                                  final ITourLocationConsumer tourLocationConsumer,
                                  final boolean isStartLocation,
                                  final TourData tourData) {

      super(toolItem.getParent(), state, new int[] { 800, 800 });

      _toolItem = toolItem;

      _tourLocationConsumer = tourLocationConsumer;
      _isStartLocation = isStartLocation;
      _tourData = tourData;

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      final String title = _isStartLocation
            ? Messages.Slideout_TourLocation_Label_StartLocation_Title
            : Messages.Slideout_TourLocation_Label_EndLocation_Title;

      setTitleText(title);
   }

   private void addAllAddressParts(final OSMAddress address, final List<MT_DLItem> allItems) {

      try {

         final Field[] allAddressFields = address.getClass().getFields();

         for (final Field field : allAddressFields) {

            final String fieldName = field.getName();

            // skip field names which are not needed
            if ("ISO3166_2_lvl4".equals(fieldName)) { //$NON-NLS-1$
               continue;
            }

            final Object fieldValue = field.get(address);

            if (fieldValue instanceof final String stringValue) {

               // log only fields with value
               if (stringValue.length() > 0) {

                  final MT_DLItem dlItem = new MT_DLItem(
                        stringValue,
                        fieldName,
                        TourLocationManager.KEY_LOCATION_PART_ID,
                        LocationPartID.valueOf(fieldName));

                  allItems.add(dlItem);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void addCustomPart(final LocationPartID locationPart,
                              final String partValue,
                              final List<MT_DLItem> allParts) {

      if (StringUtils.hasContent(partValue)) {

         final String partName = createPartName_Combined(locationPart);

         allParts.add(new MT_DLItem(

               partValue,
               partName,
               TourLocationManager.KEY_LOCATION_PART_ID,
               locationPart));
      }
   }

   private String createPartName_Combined(final LocationPartID locationPart) {

      return UI.SYMBOL_STAR + UI.SPACE + locationPart.name();
   }

   private String createPartName_NotAvailable(final LocationPartID remainingPart) {

      return UI.SYMBOL_STAR + UI.SYMBOL_STAR + UI.SPACE + remainingPart;
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      setupUI();

      // load profile viewer
      _profileViewer.setInput(new Object());

      restoreState();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         createUI_20_Profiles(container);
         createUI_30_ProfileName(container);
         createUI_50_LocationParts(container);
         createUI_90_ProfileActions(container);
      }
   }

   private Composite createUI_20_Profiles(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(10))
            .applyTo(container);
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

      // !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
//      table.setHeaderVisible(false);
      table.setHeaderVisible(true);

      _profileViewer = new TableViewer(table);

      /*
       * Create columns
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

               final TourLocationProfile profile = (TourLocationProfile) cell.getElement();

               cell.setText(profile.name);
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(1, true));
      }
      {
         // Column: Location Parts

         tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
         tc = tvc.getColumn();
         tc.setText(Messages.Slideout_TourLocation_Column_LocationParts);
         tvc.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final TourLocationProfile profile = (TourLocationProfile) cell.getElement();

               cell.setText(profile.allParts.toString());
            }
         });
         tableLayout.setColumnData(tc, new ColumnWeightData(2, true));
      }

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
      GridDataFactory.fillDefaults()
//            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Button: New
             */
            final Button button = new Button(container, SWT.PUSH);
            button.setText(Messages.Slideout_TourFilter_Action_AddProfile);
            button.setToolTipText(Messages.Slideout_TourFilter_Action_AddProfile_Tooltip);
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

   private void createUI_30_ProfileName(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            // Label: Profile name

            _lblProfileName = new Label(container, SWT.NONE);
            _lblProfileName.setText(Messages.Slideout_TourFilter_Label_ProfileName);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblProfileName);
         }
         {
            // Text: Profile name

            _txtProfileName = new Text(container, SWT.BORDER);
            _txtProfileName.addModifyListener(_defaultModifyListener);
            GridDataFactory.fillDefaults()
                  .indent(20, 0)
                  .hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT)

                  .applyTo(_txtProfileName);
         }
      }
   }

   private void createUI_50_LocationParts(final Composite parent) {

      {
         // default location name

         _lblDefaultName = new Label(parent, SWT.NONE);
         _lblDefaultName.setText(Messages.Slideout_TourLocation_Label_DefaultLocationName);
         _lblDefaultName.setFont(_boldFont);
         GridDataFactory.fillDefaults().applyTo(_lblDefaultName);

         _txtDefaultName = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .grab(true, false)
               .applyTo(_txtDefaultName);
      }
      {
         // selected location parts

         _lblSelectedLocationParts = new Label(parent, SWT.NONE);
         _lblSelectedLocationParts.setText(Messages.Slideout_TourLocation_Label_SelectedLocationParts);
         _lblSelectedLocationParts.setFont(_boldFont);
         GridDataFactory.fillDefaults().applyTo(_lblSelectedLocationParts);

         _txtSelectedLocationParts = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .grab(true, false)
               .applyTo(_txtSelectedLocationParts);

      }
      {
         // dual list with location parts

         _lblLocationParts = new Label(parent, SWT.NONE);
         _lblLocationParts.setText(Messages.Slideout_TourLocation_Label_LocationParts);
         GridDataFactory.fillDefaults().applyTo(_lblLocationParts);

         _listLocationParts = new MT_DualList(parent, SWT.NONE);
         _listLocationParts.addSelectionChangeListener(selectionChangeListener -> updateModelAndUI_FromSelectedParts());
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(_listLocationParts);
      }
   }

   private void createUI_90_ProfileActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            // Label: Profile name

            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Slideout_TourFilter_Label_Remarks);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.FILL, SWT.BEGINNING)
                  .applyTo(label);
         }
         {
            /*
             * Button: Apply & Close
             */
            _btnApply = new Button(container, SWT.PUSH);
            _btnApply.setText(OtherMessages.APP_ACTION_APPLY_AND_CLOSE);
            _btnApply.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> doApplyAndClose()));
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.END)
                  .applyTo(_btnApply);

            // set button default width
            UI.setButtonLayoutWidth(_btnApply);
         }
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

      final boolean isProfileSelected = _selectedProfile != null;
      final int numProfiles = _allProfiles.size();

      _btnApply.setEnabled(isProfileSelected);
      _btnCopyProfile.setEnabled(isProfileSelected);
      _btnDeleteProfile.setEnabled(isProfileSelected);

      _lblDefaultName.setEnabled(isProfileSelected);
      _lblLocationParts.setEnabled(isProfileSelected);
      _lblProfileName.setEnabled(isProfileSelected);
      _lblSelectedLocationParts.setEnabled(isProfileSelected);

      _txtDefaultName.setEnabled(isProfileSelected);
      _txtProfileName.setEnabled(isProfileSelected);

      _lblProfiles.setEnabled(numProfiles > 0);
      _profileViewer.getTable().setEnabled(numProfiles > 0);

      _listLocationParts.setEnabled(isProfileSelected);
   }

   private OSMLocation getOsmLocation() {

      return _isStartLocation
            ? _tourData.osmLocation_Start.osmLocation
            : _tourData.osmLocation_End.osmLocation;
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _defaultModifyListener = modifyEvent -> onProfile_Modify();

      // cleanup previous slideout openings
      _selectedProfile = null;
      _allDualListItems.clear();
   }

   @Override
   protected void onDispose() {

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   private void onProfile_Add() {

      final TourLocationProfile filterProfile = new TourLocationProfile();

      // update model
      _allProfiles.add(filterProfile);

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

      final TourLocationProfile filterProfile = _selectedProfile.clone();

      // update model
      _allProfiles.add(filterProfile);

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
      _allProfiles.remove(_selectedProfile);
      TourLocationManager.setSelectedProfile(null);

      // update UI
      _profileViewer.remove(_selectedProfile);

      /*
       * Select another filter at the same position
       */
      final int numFilters = _allProfiles.size();
      final int nextFilterIndex = Math.min(numFilters - 1, lastIndex);

      final Object nextSelectedProfile = _profileViewer.getElementAt(nextFilterIndex);
      if (nextSelectedProfile == null) {

         _selectedProfile = null;

         updateModelAndUI_FromSelectedParts();

      } else {

         selectProfile((TourLocationProfile) nextSelectedProfile);
      }

      enableControls();

      // set focus back to the viewer
      _profileViewer.getTable().setFocus();
   }

   private void onProfile_Modify() {

      if (_selectedProfile == null) {
         return;
      }

      final String profileName = _txtProfileName.getText();

      _selectedProfile.name = profileName;

      // a refresh() is needed to resort the viewer
      _profileViewer.refresh();
   }

   private void onProfile_Select() {

      TourLocationProfile selectedProfile = null;

      // get selected profile from viewer
      final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
      final Object firstElement = selection.getFirstElement();
      if (firstElement != null) {
         selectedProfile = (TourLocationProfile) firstElement;
      }

      if (_selectedProfile != null && _selectedProfile == selectedProfile) {

         // a new profile is not selected
         return;
      }

      _selectedProfile = selectedProfile;

      /*
       * Update model
       */
      TourLocationManager.setSelectedProfile(_selectedProfile);

      /*
       * Update UI
       */

      // set profile name
      if (_selectedProfile == null) {

         _txtProfileName.setText(UI.EMPTY_STRING);

      } else {

         _txtProfileName.setText(_selectedProfile.name);

         if (_selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {

            // a default profile is selected, make is easy to rename it

            _txtProfileName.selectAll();
            _txtProfileName.setFocus();
         }
      }

      // set selected/not selected parts

      final List<LocationPartID> allProfileParts = _selectedProfile.allParts;

      final Set<LocationPartID> allRemainingProfileParts = new HashSet<>();
      allRemainingProfileParts.addAll(allProfileParts);

      nextPart:

      // loop: all available parts
      for (final MT_DLItem dualListItem : _allDualListItems) {

         final LocationPartID dualListPart = (LocationPartID) dualListItem.getData(TourLocationManager.KEY_LOCATION_PART_ID);

         // loop: all profile parts
         for (final LocationPartID profilePart : allProfileParts) {

            if (dualListPart.equals(profilePart)) {

               // part is selected
               dualListItem.setLastAction(MT_DLItem.LAST_ACTION.SELECTION);

               // update remaining parts
               allRemainingProfileParts.remove(profilePart);

               // continue with the next part
               continue nextPart;
            }
         }

         // part is not selected
         dualListItem.setLastAction(MT_DLItem.LAST_ACTION.DESELECTION);
      }

      // add profile parts which are not available in the downloaded parts
      for (final LocationPartID remainingPart : allRemainingProfileParts) {

         final MT_DLItem dlItem = new MT_DLItem(
               UI.EMPTY_STRING,
               createPartName_NotAvailable(remainingPart),
               TourLocationManager.KEY_LOCATION_PART_ID,
               remainingPart);

         // set n/a flag
         dlItem.setData(TourLocationManager.KEY_IS_NOT_AVAILABLE, Boolean.TRUE);

         // show it in the selection list
         dlItem.setLastAction(MT_DLItem.LAST_ACTION.SELECTION);

         _allDualListItems.add(dlItem);
      }

      _listLocationParts.setItems(_allDualListItems);

      updateModelAndUI_FromSelectedParts();

      enableControls();
   }

   private void restoreState() {

      /*
       * Get previous selected profile
       */
      TourLocationProfile selectedProfile = TourLocationManager.getSelectedProfile();

      if (selectedProfile == null) {

         // select first profile

         selectedProfile = (TourLocationProfile) _profileViewer.getElementAt(0);
      }

      if (selectedProfile != null) {
         selectProfile(selectedProfile);
      }

      enableControls();
   }

   @Override
   protected void saveState() {

      // save slideout position/size
      super.saveState();
   }

   private void selectProfile(final TourLocationProfile selectedProfile) {

      _profileViewer.setSelection(new StructuredSelection(selectedProfile));

      final Table table = _profileViewer.getTable();
      table.setSelection(table.getSelectionIndices());
   }

   private void setupUI() {

      final OSMLocation osmLocation = getOsmLocation();
      final OSMAddress address = osmLocation.address;

      // show "display_name" as default name
      _txtDefaultName.setText(osmLocation.display_name);

      /*
       * Fill address part widget
       */

// SET_FORMATTING_OFF

      // add customized parts
      final String smallestCity           = TourLocationManager.getCombined_City_Smallest(address);
      final String smallestCityWithZip    = TourLocationManager.getCombined_CityWithZip_Smallest(address);
      final String largestCity            = TourLocationManager.getCombined_City_Largest(address);
      final String largestCityWithZip     = TourLocationManager.getCombined_CityWithZip_Largest(address);
      final String streetWithHouseNumber  = TourLocationManager.getCombined_StreetWithHouseNumber(address);

      boolean isShowSmallestCity = false;
      if (largestCity != null && largestCity.equals(smallestCity) == false) {
         isShowSmallestCity = true;
      }

      addCustomPart(LocationPartID    .OSM_DEFAULT_NAME,                  osmLocation.display_name, _allDualListItems);
      addCustomPart(LocationPartID    .OSM_NAME,                          osmLocation.name, _allDualListItems);

      addCustomPart(LocationPartID    .CUSTOM_CITY_LARGEST,               largestCity, _allDualListItems);
      if (isShowSmallestCity) {
         addCustomPart(LocationPartID .CUSTOM_CITY_SMALLEST,              smallestCity, _allDualListItems);
      }

      addCustomPart(LocationPartID    .CUSTOM_CITY_WITH_ZIP_LARGEST,      largestCityWithZip, _allDualListItems);
      if (isShowSmallestCity) {
         addCustomPart(LocationPartID .CUSTOM_CITY_WITH_ZIP_SMALLEST,     smallestCityWithZip, _allDualListItems);
      }

      addCustomPart(LocationPartID    .CUSTOM_STREET_WITH_HOUSE_NUMBER,   streetWithHouseNumber, _allDualListItems);


// SET_FORMATTING_ON

      // add address parts
      addAllAddressParts(address, _allDualListItems);

      _listLocationParts.setItems(_allDualListItems);
   }

   /**
    * Update model/UI from the selected parts
    */
   private void updateModelAndUI_FromSelectedParts() {

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

}
