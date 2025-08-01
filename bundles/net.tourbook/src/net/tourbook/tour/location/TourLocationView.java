/*******************************************************************************
 * Copyright (C) 2023, 2025 Wolfgang Schramm and Contributors
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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnDefinitionFor1stVisibleAlignmentColumn;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TableColumnFactory;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
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
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.ViewPart;

public class TourLocationView extends ViewPart implements ITourViewer {

   public static final String                ID                              = "net.tourbook.tour.location.TourLocationView"; //$NON-NLS-1$
   //
   private static final char                 NL                              = UI.NEW_LINE;
   //
   private static final String               STATE_IS_LINK_WITH_OTHER_VIEWS  = "STATE_IS_LINK_WITH_OTHER_VIEWS";              //$NON-NLS-1$
   private static final String               STATE_IS_LOCATION_FILTER_ACTIVE = "STATE_IS_LOCATION_FILTER_ACTIVE";             //$NON-NLS-1$
   private static final String               STATE_IS_SHOW_INFO_TOOLTIP      = "STATE_IS_SHOW_INFO_TOOLTIP";                  //$NON-NLS-1$
   private static final String               STATE_LOCATION_FILTER_COUNTRY   = "STATE_LOCATION_FILTER_COUNTRY";               //$NON-NLS-1$
   private static final String               STATE_SELECTED_SENSOR_INDICES   = "STATE_SELECTED_SENSOR_INDICES";               //$NON-NLS-1$
   private static final String               STATE_SORT_COLUMN_DIRECTION     = "STATE_SORT_COLUMN_DIRECTION";                 //$NON-NLS-1$
   private static final String               STATE_SORT_COLUMN_ID            = "STATE_SORT_COLUMN_ID";                        //$NON-NLS-1$
   //
   private static final String               PROFILE_NAME                    = "%s - %d";                                     //$NON-NLS-1$
   //
   /**
    * This set is used to prevent duplicated action names
    */
   private final Set<String>                 _usedDisplayNames               = new HashSet<>();
   //
   private final IPreferenceStore            _prefStore                      = TourbookPlugin.getPrefStore();
   private final IPreferenceStore            _prefStore_Common               = CommonActivator.getPrefStore();
   private final IDialogSettings             _state                          = TourbookPlugin.getState(ID);
   //
   private ISelectionListener                _postSelectionListener;
   private IPropertyChangeListener           _prefChangeListener;
   private IPropertyChangeListener           _prefChangeListener_Common;
   private ITourEventListener                _tourPropertyListener;
   //
   private TableViewer                       _locationViewer;
   private LocationComparator                _locationComparator             = new LocationComparator();
   private List<LocationItem>                _allLocationItems               = new ArrayList<>();
   //
   private List<String>                      _allLocationCountries;
   private boolean                           _isLocationFilterActive;
   //
   /**
    * When <code>null</code> then nothing is filtered
    */
   private String                            _locationFilter_Country;
   //
   private ColumnManager                     _columnManager;
   private SelectionAdapter                  _columnSortListener;
   //
   /**
    * Contains all {@link LocationItem}s which are displayed in the viewer, the key is
    * {@link TourLocation#locationID}
    */
   private Map<Long, LocationItem>           _locationItemsMap               = new HashMap<>();
   private MenuManager                       _viewerMenuManager;
   //
   private IContextMenuProvider              _tableViewerContextMenuProvider = new TableContextMenuProvider();
   //
   private boolean                           _isInUIUpdate;
   //
   private ActionBoundingBox_Reset           _actionBoundingBox_Reset;
   private ActionBoundingBox_Resize          _actionBoundingBox_Resize;
   private ActionCombineBoundingBox          _actionCombineBoundingBoxes;
   private ActionDeleteAndRetrieveAgain      _actionDeleteAndRetrieveAgain;
   private ActionDeleteLocation              _actionDeleteLocation;
   private ActionIncludeGeoPosition          _actionIncludeGeoPosition;
   private ActionLinkWithOtherViews          _actionLinkWithOtherViews;
   private ActionLocationFilter              _actionLocationFilter;
   private ActionOne                         _actionOne;
   private ActionReapplyLocationIntoTour     _actionReapplyLocationIntoTour;
   private ActionRelocateBoundingBox         _actionRelocateBoundingBox;
   private ActionSetLocationIntoTour_Default _actionSetLocationIntoTour_Default;
   private ActionSetLocationIntoTour_Part    _actionSetLocationIntoTour_Part;
   private ActionSetLocationIntoTour_Profile _actionSetLocationIntoTour_Profile;
   private ActionShowLocationInfo            _actionShowLocationInfo;
   //
   private final NumberFormat                _nf1                            = NumberFormat.getNumberInstance();
   private final NumberFormat                _nf3                            = NumberFormat.getNumberInstance();
   private final NumberFormat                _nf6                            = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
      _nf6.setMinimumFractionDigits(6);
      _nf6.setMaximumFractionDigits(6);
   }
   //
   private TourLocationToolTip _locationTooltip;
   //
   private PixelConverter      _pc;
   //
   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerContainer;

   private Menu      _tableContextMenu;

   private class ActionBoundingBox_Reset extends Action {

      public ActionBoundingBox_Reset() {

         setText(Messages.Tour_Location_Action_ResetBoundingBox);
         setToolTipText(Messages.Tour_Location_Action_ResetBoundingBox_Tooltip);
      }

      @Override
      public void run() {

         onAction_BoundingBox_Reset();
      }
   }

   private class ActionBoundingBox_Resize extends Action {

      public ActionBoundingBox_Resize() {

         super(Messages.Tour_Location_Action_ResizeBoundingBox);
      }

      @Override
      public void run() {

         onAction_BoundingBox_Resize();
      }
   }

   private class ActionCombineBoundingBox extends Action {

      public ActionCombineBoundingBox() {

         super(Messages.Tour_Location_Action_CombineBoundingBox);

         setToolTipText(Messages.Tour_Location_Action_CombineBoundingBox_Tooltip);
      }

      @Override
      public void run() {

         onAction_CombineBoundingBox();
      }
   }

   private class ActionDeleteAndRetrieveAgain extends Action {

      public ActionDeleteAndRetrieveAgain() {

         setText(Messages.Tour_Location_Action_DeleteAndReapply);
         setToolTipText(Messages.Tour_Location_Action_DeleteAndReapply_Tooltip);
      }

      @Override
      public void run() {

         onAction_DeleteAndReapply(false);
      }
   }

   private class ActionDeleteLocation extends Action {

      public ActionDeleteLocation() {

         setText(Messages.Tour_Location_Action_DeleteLocation);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {

         onAction_DeleteLocation();
      }
   }

   private class ActionIncludeGeoPosition extends Action {

      public ActionIncludeGeoPosition() {

         super(Messages.Tour_Location_Action_IncludeGeoPosition);

         setToolTipText(Messages.Tour_Location_Action_IncludeGeoPosition_Tooltip);
      }

      @Override
      public void run() {

         onAction_IncludeGeoPosition();
      }
   }

   private class ActionLinkWithOtherViews extends Action {

      public ActionLinkWithOtherViews() {

         super(Messages.Calendar_View_Action_LinkWithOtherViews, AS_CHECK_BOX);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.SyncViews));
      }

      @Override
      public void run() {}
   }

   private class ActionLocationFilter extends ActionToolbarSlideout {

      private SlideoutTourLocationFilter slideoutTourLocationFilter;

      public ActionLocationFilter() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.TourFilter_Collected_All));

         isToggleAction = true;
         isShowSlideoutAlways = true;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         slideoutTourLocationFilter = new SlideoutTourLocationFilter(_parent, toolbar, TourLocationView.this);

         return slideoutTourLocationFilter;
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         _isLocationFilterActive = getSelection();

         updateTourLocationFilter(_locationFilter_Country);
      }
   }

   private class ActionOne extends Action {

      public ActionOne() {

         setToolTipText(Messages.Tour_Location_Action_One_Tooltip);
      }

      @Override
      public void run() {

         onAction_One();
      }
   }

   private class ActionReapplyLocationIntoTour extends Action {

      private TourLocation _tourLocation;
      private String       _locationLabel;

      /**
       * @param locationLabel
       */
      public ActionReapplyLocationIntoTour() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Tour_Location_Action_ReapplyLocationIntoTour_Tooltip);
      }

      @Override
      public void run() {

         onAction_SetLocationIntoTour(_locationLabel, _tourLocation);
      }

      public boolean setupAction() {

         // currently only one location is supported

         final List<TourLocation> allLocations = getSelectedLocations();

         if (allLocations.size() != 1) {
            return false;
         }

         final TourLocation tourLocation = allLocations.get(0);
         final String appliedName = tourLocation.appliedName;

         if (StringUtils.hasContent(appliedName) == false) {
            return false;
         }

         _tourLocation = tourLocation;
         _locationLabel = appliedName;

         setText(Messages.Tour_Location_Action_ReapplyLocationIntoTour.formatted(_locationLabel));

         return true;
      }
   }

   private class ActionRelocateBoundingBox extends Action {

      public ActionRelocateBoundingBox() {

         setText(Messages.Tour_Location_Action_RelocateBoundingBox);
         setToolTipText(Messages.Tour_Location_Action_RelocateBoundingBox_Tooltip);
      }

      @Override
      public void run() {

         onAction_RelocateBoundingBox();
      }
   }

   private class ActionSetLocationIntoTour extends Action {

      private TourLocation _tourLocation;
      private String       _locationLabel;

      /**
       * @param locationLabel
       * @param actionTooltip
       *           Can be <code>null</code>
       * @param tourLocation
       */
      public ActionSetLocationIntoTour(final String locationLabel,
                                       final String actionTooltip,
                                       final TourLocation tourLocation) {

         super(locationLabel, AS_PUSH_BUTTON);

         _locationLabel = locationLabel;
         _tourLocation = tourLocation;

         if (actionTooltip != null) {
            setToolTipText(actionTooltip);
         }
      }

      @Override
      public void run() {

         onAction_SetLocationIntoTour(_locationLabel, _tourLocation);
      }
   }

   private class ActionSetLocationIntoTour_Default extends SubMenu {

      public ActionSetLocationIntoTour_Default() {

         super(Messages.Tour_Location_Action_SetLocationIntoTour_Default, AS_DROP_DOWN_MENU);

         setToolTipText(Messages.Tour_Location_Action_SetLocationIntoTour_Default_Tooltip);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         fillMenu_SetLoctionIntoTour_Default(menu);
      }
   }

   private class ActionSetLocationIntoTour_Part extends SubMenu {

      public ActionSetLocationIntoTour_Part() {

         super(Messages.Tour_Location_Action_SetLocationIntoTour_Part, AS_DROP_DOWN_MENU);

         setToolTipText(Messages.Tour_Location_Action_SetLocationIntoTour_Tooltip);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         fillMenu_SetLoctionIntoTour_Part(menu);
      }
   }

   private class ActionSetLocationIntoTour_Profile extends SubMenu {

      public ActionSetLocationIntoTour_Profile() {

         super(Messages.Tour_Location_Action_SetLocationIntoTour_Profile, AS_DROP_DOWN_MENU);

         setToolTipText(Messages.Tour_Location_Action_SetLocationIntoTour_Tooltip);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         fillMenu_SetLoctionIntoTour_Profile(menu);
      }

   }

   private class ActionShowLocationInfo extends Action {

      public ActionShowLocationInfo() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_Location_Action_ShowLocationInfo_Tooltip);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.TourInfo));
      }

      @Override
      public void run() {}
   }

   private class LocationComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = TableColumnFactory.SENSOR_NAME_ID;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final LocationItem item1 = (LocationItem) e1;
         final LocationItem item2 = (LocationItem) e2;

         double rc = 0;

         final TourLocation location1 = item1.tourLocation;
         final TourLocation location2 = item2.tourLocation;

         // determine which column and do the appropriate sort
         switch (__sortColumnId) {

// SET_FORMATTING_OFF

         case TableColumnFactory.LOCATION_PART_Name_ID:              rc = compareText(location1.name, location2.name);                    break;
         case TableColumnFactory.LOCATION_DATA_APPLIED_NAME_ID:      rc = compareText(location1.appliedName, location2.appliedName);      break;
         case TableColumnFactory.LOCATION_DATA_LAST_MODIFIED_ID:     rc = location1.lastModified - location2.lastModified;                break;

         case TableColumnFactory.LOCATION_TOUR_USAGE_ID:

            rc = item1.numTourAllLocations - item2.numTourAllLocations;

            if (rc == 0) {
               rc = item1.numTourStartLocations - item2.numTourStartLocations;
            }

            if (rc == 0) {
               rc = item1.numTourEndLocations - item2.numTourEndLocations;
            }

            break;

         case TableColumnFactory.LOCATION_TOUR_USAGE_START_LOCATIONS_ID:

            rc = item1.numTourStartLocations - item2.numTourStartLocations;

            if (rc == 0) {
               rc = item1.numTourEndLocations - item2.numTourEndLocations;
            }

            break;

         case TableColumnFactory.LOCATION_TOUR_USAGE_END_LOCATIONS_ID:

            rc = item1.numTourEndLocations - item2.numTourEndLocations;

            if (rc == 0) {
               rc = item1.numTourStartLocations - item2.numTourStartLocations;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_ZOOMLEVEL_ID:

            rc = location1.zoomlevel - location2.zoomlevel;

            break;

         case TableColumnFactory.LOCATION_PART_Continent_ID:         rc = compareText(location1.continent,           location2.continent);         break;
         case TableColumnFactory.LOCATION_PART_Country_ID:           rc = compareText(location1.country,             location2.country);           break;
         case TableColumnFactory.LOCATION_PART_CountryCode_ID:       rc = compareText(location1.country_code,        location2.country_code);      break;

         case TableColumnFactory.LOCATION_PART_Region_ID:            rc = compareText(location1.region,              location2.region);            break;
         case TableColumnFactory.LOCATION_PART_State_ID:             rc = compareText(location1.state,               location2.state);             break;
         case TableColumnFactory.LOCATION_PART_StateDistrict_ID:     rc = compareText(location1.state_district,      location2.state_district);    break;
         case TableColumnFactory.LOCATION_PART_County_ID:            rc = compareText(location1.county,              location2.county);            break;

         case TableColumnFactory.LOCATION_PART_Municipality_ID:      rc = compareText(location1.municipality,        location2.municipality);      break;
         case TableColumnFactory.LOCATION_PART_City_ID:              rc = compareText(location1.city,                location2.city);              break;
         case TableColumnFactory.LOCATION_PART_Town_ID:              rc = compareText(location1.town,                location2.town);              break;
         case TableColumnFactory.LOCATION_PART_Village_ID:           rc = compareText(location1.village,             location2.village);           break;

         case TableColumnFactory.LOCATION_PART_CityDistrict_ID:      rc = compareText(location1.city_district,       location2.city_district);     break;
         case TableColumnFactory.LOCATION_PART_District_ID:          rc = compareText(location1.district,            location2.district);          break;
         case TableColumnFactory.LOCATION_PART_Borough_ID:           rc = compareText(location1.borough,             location2.borough);           break;
         case TableColumnFactory.LOCATION_PART_Suburb_ID:            rc = compareText(location1.suburb,              location2.suburb);            break;
         case TableColumnFactory.LOCATION_PART_Subdivision_ID:       rc = compareText(location1.subdivision,         location2.subdivision);       break;

         case TableColumnFactory.LOCATION_PART_Hamlet_ID:            rc = compareText(location1.hamlet,              location2.hamlet);            break;
         case TableColumnFactory.LOCATION_PART_Croft_ID:             rc = compareText(location1.croft,               location2.croft);             break;
         case TableColumnFactory.LOCATION_PART_IsolatedDwelling_ID:  rc = compareText(location1.isolated_dwelling,   location2.isolated_dwelling); break;

         case TableColumnFactory.LOCATION_PART_Neighbourhood_ID:     rc = compareText(location1.neighbourhood,       location2.neighbourhood);     break;
         case TableColumnFactory.LOCATION_PART_Allotments_ID:        rc = compareText(location1.allotments,          location2.allotments);        break;
         case TableColumnFactory.LOCATION_PART_Quarter_ID:           rc = compareText(location1.quarter,             location2.quarter);           break;

         case TableColumnFactory.LOCATION_PART_CityBlock_ID:         rc = compareText(location1.city_block,          location2.city_block);        break;
         case TableColumnFactory.LOCATION_PART_Residential_ID:       rc = compareText(location1.residential,         location2.residential);       break;
         case TableColumnFactory.LOCATION_PART_Farm_ID:              rc = compareText(location1.farm,                location2.farm);              break;
         case TableColumnFactory.LOCATION_PART_Farmyard_ID:          rc = compareText(location1.farmyard,            location2.farmyard);          break;
         case TableColumnFactory.LOCATION_PART_Industrial_ID:        rc = compareText(location1.industrial,          location2.industrial);        break;
         case TableColumnFactory.LOCATION_PART_Commercial_ID:        rc = compareText(location1.commercial,          location2.commercial);        break;
         case TableColumnFactory.LOCATION_PART_Retail_ID:            rc = compareText(location1.retail,              location2.retail);            break;

         case TableColumnFactory.LOCATION_PART_Road_ID:              rc = compareText(location1.road,                location2.road);              break;

         case TableColumnFactory.LOCATION_PART_HouseName_ID:         rc = compareText(location1.house_name,          location2.house_name);        break;
         case TableColumnFactory.LOCATION_PART_HouseNumber_ID:       rc = compareHouseNumber(location1,              location2);                   break;

         case TableColumnFactory.LOCATION_PART_Aerialway_ID:         rc = compareText(location1.aerialway,           location2.aerialway);         break;
         case TableColumnFactory.LOCATION_PART_Aeroway_ID:           rc = compareText(location1.aeroway,             location2.aeroway);           break;
         case TableColumnFactory.LOCATION_PART_Amenity_ID:           rc = compareText(location1.amenity,             location2.amenity);           break;
         case TableColumnFactory.LOCATION_PART_Boundary_ID:          rc = compareText(location1.boundary,            location2.boundary);          break;
         case TableColumnFactory.LOCATION_PART_Bridge_ID:            rc = compareText(location1.bridge,              location2.bridge);            break;
         case TableColumnFactory.LOCATION_PART_Club_ID:              rc = compareText(location1.club,                location2.club);              break;
         case TableColumnFactory.LOCATION_PART_Craft_ID:             rc = compareText(location1.craft,               location2.craft);             break;
         case TableColumnFactory.LOCATION_PART_Emergency_ID:         rc = compareText(location1.emergency,           location2.emergency);         break;
         case TableColumnFactory.LOCATION_PART_Historic_ID:          rc = compareText(location1.historic,            location2.historic);          break;
         case TableColumnFactory.LOCATION_PART_Landuse_ID:           rc = compareText(location1.landuse,             location2.landuse);           break;
         case TableColumnFactory.LOCATION_PART_Leisure_ID:           rc = compareText(location1.leisure,             location2.leisure);           break;
         case TableColumnFactory.LOCATION_PART_ManMade_ID:           rc = compareText(location1.man_made,            location2.man_made);          break;
         case TableColumnFactory.LOCATION_PART_Military_ID:          rc = compareText(location1.military,            location2.military);          break;
         case TableColumnFactory.LOCATION_PART_MountainPass_ID:      rc = compareText(location1.mountain_pass,       location2.mountain_pass);     break;
         case TableColumnFactory.LOCATION_PART_Natural_ID:           rc = compareText(location1.natural2,            location2.natural2);          break;
         case TableColumnFactory.LOCATION_PART_Office_ID:            rc = compareText(location1.office,              location2.office);            break;
         case TableColumnFactory.LOCATION_PART_Place_ID:             rc = compareText(location1.place,               location2.place);             break;
         case TableColumnFactory.LOCATION_PART_Railway_ID:           rc = compareText(location1.railway,             location2.railway);           break;
         case TableColumnFactory.LOCATION_PART_Shop_ID:              rc = compareText(location1.shop,                location2.shop);              break;
         case TableColumnFactory.LOCATION_PART_Tourism_ID:           rc = compareText(location1.tourism,             location2.tourism);           break;
         case TableColumnFactory.LOCATION_PART_Tunnel_ID:            rc = compareText(location1.tunnel,              location2.tunnel);            break;
         case TableColumnFactory.LOCATION_PART_Waterway_ID:          rc = compareText(location1.waterway,            location2.waterway);          break;

         case TableColumnFactory.LOCATION_PART_Postcode_ID:          rc = comparePostcode(location1,                 location2);                   break;

         case TableColumnFactory.LOCATION_PART_SettlementSmall_ID:   rc = compareText(location1.settlementSmall,     location2.settlementSmall);   break;
         case TableColumnFactory.LOCATION_PART_SettlementLarge_ID:   rc = compareText(location1.settlementLarge,     location2.settlementLarge);   break;

         case TableColumnFactory.LOCATION_GEO_LATITUDE_ID:

            rc = location1.latitudeE6_Normalized - location2.latitudeE6_Normalized;

            if (rc==0) {
               rc = location1.longitudeE6_Normalized - location2.longitudeE6_Normalized;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_LONGITUDE_ID:

            rc = location1.longitudeE6_Normalized - location2.longitudeE6_Normalized;

            if (rc==0) {
               rc = location1.latitudeE6_Normalized - location2.latitudeE6_Normalized;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_LATITUDE_DIFF_ID:

            rc = item1.latitudeDiff_Value - item2.latitudeDiff_Value;

            if (rc == 0) {
               rc = item1.longitudeDiff_Value - item2.longitudeDiff_Value;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_LONGITUDE_DIFF_ID:

            rc = item1.longitudeDiff_Value - item2.longitudeDiff_Value;

            if (rc == 0) {
               rc = item1.latitudeDiff_Value - item2.latitudeDiff_Value;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_IS_RESIZED_BOUNDING_BOX_ID:

            final boolean isResizedBoundingBox1 = item1.isResizedBoundingBox;
            final boolean isResizedBoundingBox2 = item2.isResizedBoundingBox;

            rc = isResizedBoundingBox1 == isResizedBoundingBox2

                  ? 1
                  : isResizedBoundingBox1

                     ? 1
                     : -1;

            break;

         case TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_HEIGHT_ID:

            rc = item1.boundingBoxHeight_Value- item2.boundingBoxHeight_Value;

            if (rc == 0) {
               rc = item1.boundingBoxWidth_Value - item2.boundingBoxWidth_Value;
            }

            break;

         case TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_WIDTH_ID:

            rc = item1.boundingBoxWidth_Value - item2.boundingBoxWidth_Value;

            if (rc == 0) {
               rc = item1.boundingBoxHeight_Value - item2.boundingBoxHeight_Value;
            }

            break;

         case TableColumnFactory.LOCATION_DATA_ID_ID:

            rc = location1.getLocationId() - location2.getLocationId();

            if (rc == 0) {
               rc = item1.boundingBoxHeight_Value - item2.boundingBoxHeight_Value;
            }

            break;

         case TableColumnFactory.LOCATION_PART_DisplayName_ID:
         default:
            rc = location1.display_name.compareTo(location2.display_name);
         }

// SET_FORMATTING_ON

         // nth sort by country
         if (rc == 0) {
            rc = compareText(location1.country, location2.country);
         }

         // nth sort by city
         if (rc == 0) {
            rc = compareText(location1.city, location2.city);
         }

         // nth sort by town
         if (rc == 0) {
            rc = compareText(location1.town, location2.town);
         }

         // nth sort by village
         if (rc == 0) {
            rc = compareText(location1.village, location2.village);
         }

         // nth sort by largest settlement
         if (rc == 0) {
            rc = compareText(location1.settlementLarge, location2.settlementLarge);
         }

         // nth sort by smallest settlement
         if (rc == 0) {
            rc = compareText(location1.settlementSmall, location2.settlementSmall);
         }

         // nth sort by road
         if (rc == 0) {
            rc = compareText(location1.road, location2.road);
         }

         // nth sort by house number
         if (rc == 0) {
            rc = compareHouseNumber(location1, location2);
         }

         // nth sort by lat/lon diff
         if (rc == 0) {
            rc = item1.latitudeDiff_Value - item2.latitudeDiff_Value;
         }
         if (rc == 0) {
            rc = item1.longitudeDiff_Value - item2.longitudeDiff_Value;
         }

         // If descending order, flip the direction
         if (__sortDirection == DESCENDING) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly
          */
         return rc > 0
               ? 1
               : rc < 0
                     ? -1
                     : 0;
      }

      private double compareHouseNumber(final TourLocation location1, final TourLocation location2) {

         if (location1.houseNumberValue != Integer.MIN_VALUE && location2.houseNumberValue != Integer.MIN_VALUE) {

            return location1.houseNumberValue - location2.houseNumberValue;

         } else {

            return compareText(location1.house_number, location2.house_number);
         }
      }

      private double comparePostcode(final TourLocation location1, final TourLocation location2) {

         if (location1.postcodeValue != Integer.MIN_VALUE && location2.postcodeValue != Integer.MIN_VALUE) {

            return location1.postcodeValue - location2.postcodeValue;

         } else {

            return compareText(location1.postcode, location2.postcode);
         }
      }

      private double compareText(final String text1, final String text2) {

         if (text1 != null && text2 != null) {

            return text1.compareTo(text2);

         } else if (text1 != null) {

            return 1;

         } else if (text2 != null) {

            return -1;
         }

         return 0;
      }

      public void setSortColumn(final Widget widget) {

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

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }
   }

   private class LocationContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allLocationItems.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   class LocationItem {

      TourLocation   tourLocation;

      public String  latitude_Text;
      public String  longitude_Text;

      public String  latitudeDiff_Text;
      public String  longitudeDiff_Text;
      public int     latitudeDiff_Value;
      public int     longitudeDiff_Value;

      public String  boundingBoxHeight_Text;
      public String  boundingBoxWidth_Text;
      public int     boundingBoxHeight_Value;
      public int     boundingBoxWidth_Value;

      public long    numTourAllLocations;
      public long    numTourStartLocations;
      public long    numTourEndLocations;

      public boolean isResizedBoundingBox;

      @Override
      public boolean equals(final Object obj) {

         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }

         final LocationItem other = (LocationItem) obj;
         if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
            return false;
         }

         return tourLocation.getLocationId() == other.tourLocation.getLocationId();
      }

      private TourLocationView getEnclosingInstance() {
         return TourLocationView.this;
      }

      @Override
      public int hashCode() {

         final int prime = 31;
         int result = 1;
         result = prime * result + getEnclosingInstance().hashCode();
         result = prime * result + Objects.hash(tourLocation.getLocationId());

         return result;
      }
   }

   public class TableContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tableContextMenu != null) {
            _tableContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _tableContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tableContextMenu = createUI_22_CreateViewerContextMenu();

         return _tableContextMenu;
      }
   }

   /**
    * This class is used to show a tooltip only when this cell is hovered
    */
   public abstract class TooltipLabelProvider extends CellLabelProvider {}

   public class TourLocationFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (_isLocationFilterActive == false) {

            // filter is NOT active

            return true;
         }

         // filter is active

         if (_locationFilter_Country == null) {

            // nothing is filtered

            return true;
         }

         // locations are filtered

         if (element instanceof final LocationItem locationItem) {

            if (locationItem.tourLocation.country.equals(_locationFilter_Country)) {

               return true;
            }
         }

         return false;
      }
   }

   /**
    * Add an action to the provided menu
    *
    * @param menu
    * @param action
    */
   private void addActionToMenu(final Menu menu, final Action action) {

      new ActionContributionItem(action).fill(menu, -1);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _locationViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _locationViewer.refresh();

            } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               reloadViewer();
            }
         }
      };

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               loadAllLocations();

               _columnManager.saveState(_state);
               _columnManager.clearColumns();

               defineAllColumns();

               _locationViewer = (TableViewer) recreateViewer(_locationViewer);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = (workbenchPart, selection) -> {

         // prevent to listen to a selection which is originated by this view
         if (workbenchPart == TourLocationView.this) {
            return;
         }

         onSelectionChanged(selection);
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourPropertyListener = (part, eventId, eventData) -> {

         if (part == TourLocationView.this) {
            return;
         }

         if (eventId == TourEventId.UPDATE_UI
               || eventId == TourEventId.ALL_TOURS_ARE_MODIFIED
               || eventId == TourEventId.TOUR_CHANGED

         ) {

            // new locations could be added

            reloadViewer();

         } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof final ISelection selection) {

            onSelectionChanged(selection);
         }
      };

      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionBoundingBox_Reset            = new ActionBoundingBox_Reset();
      _actionBoundingBox_Resize           = new ActionBoundingBox_Resize();
      _actionCombineBoundingBoxes         = new ActionCombineBoundingBox();
      _actionDeleteAndRetrieveAgain       = new ActionDeleteAndRetrieveAgain();
      _actionDeleteLocation               = new ActionDeleteLocation();
      _actionIncludeGeoPosition           = new ActionIncludeGeoPosition();
      _actionLinkWithOtherViews           = new ActionLinkWithOtherViews();
      _actionLocationFilter               = new ActionLocationFilter();
      _actionOne                          = new ActionOne();
      _actionReapplyLocationIntoTour      = new ActionReapplyLocationIntoTour();
      _actionRelocateBoundingBox          = new ActionRelocateBoundingBox();
      _actionSetLocationIntoTour_Default  = new ActionSetLocationIntoTour_Default();
      _actionSetLocationIntoTour_Part     = new ActionSetLocationIntoTour_Part();
      _actionSetLocationIntoTour_Profile  = new ActionSetLocationIntoTour_Profile();
      _actionShowLocationInfo             = new ActionShowLocationInfo();

// SET_FORMATTING_ON

   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> fillContextMenu(menuManager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);
      createMenuManager();

      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      addPrefListener();
      addSelectionListener();
      addTourEventListener();

      createActions();
      fillToolbar();

      /*
       * Ensure that UI is created otherwise the restore do not work -> toolbar was not initialized
       * and the state could not be set
       */
      parent.getDisplay().asyncExec(() -> {

         BusyIndicator.showWhile(parent.getDisplay(), () -> {

            loadAllLocations();

            updateUI_SetViewerInput();

            restoreState_WithUI();
         });
      });
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_LocationViewer(_viewerContainer);
      }
   }

   private void createUI_10_LocationViewer(final Composite parent) {

      /*
       * Create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      table.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL) {
            onAction_DeleteLocation();
         }
      }));

      /*
       * Create table viewer
       */
      _locationViewer = new TableViewer(table);

      _columnManager.createColumns(_locationViewer);
      _columnManager.setIsCategoryAvailable(true);

      _locationViewer.setUseHashlookup(true);
      _locationViewer.setContentProvider(new LocationContentProvider());
      _locationViewer.setComparator(_locationComparator);
      _locationViewer.setFilters(new TourLocationFilter());

      _locationViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectLocation());
      _locationViewer.addDoubleClickListener(doubleClickEvent -> onAction_BoundingBox_Resize());

      updateUI_SetSortDirection(
            _locationComparator.__sortColumnId,
            _locationComparator.__sortDirection);

      // set info tooltip provider
      _locationTooltip = new TourLocationToolTip(this);

      // ensure that tooltips are hidden
      table.addListener(SWT.MouseExit, (event) -> hideTooltip());

      createUI_20_ContextMenu();
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _tableContextMenu = createUI_22_CreateViewerContextMenu();

      _columnManager.createHeaderContextMenu(

            (Table) _locationViewer.getControl(),
            _tableViewerContextMenuProvider);
   }

   private Menu createUI_22_CreateViewerContextMenu() {

      final Table table = (Table) _locationViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns() {

      // Name
      defineColumn_Part_10_DisplayName();
      defineColumn_Part_12_Name();
      defineColumn_Data_20_AppliedName();

      // Number of tour locations
      defineColumn_Tour_10_Usage();
      defineColumn_Tour_20_UsageStartLocations();
      defineColumn_Tour_30_UsageEndLocations();

      defineColumn_Geo_50_Zoomlevel();
      defineColumn_Geo_30_IsResizedBoundingBox();

      // Country
      defineColumn_Part_30_Country();
      defineColumn_Part_30_CountryCode();
      defineColumn_Part_30_Continent();

      // State
      defineColumn_Part_35_Region();
      defineColumn_Part_35_State();
      defineColumn_Part_35_StateDistrict();
      defineColumn_Part_35_County();

      // City
      defineColumn_Part_40_Municipality();
      defineColumn_Part_40_City();
      defineColumn_Part_40_Town();
      defineColumn_Part_40_Village();
      defineColumn_Part_43_SettlementLarge();
      defineColumn_Part_42_SettlementSmall();
      defineColumn_Part_40_Postcode();

      // Road
      defineColumn_Part_45_Road();
      defineColumn_Part_45_HouseNumber();
      defineColumn_Part_45_HouseName();

      // Area I
      defineColumn_Part_50_CityDistrict();
      defineColumn_Part_50_District();
      defineColumn_Part_50_Borough();
      defineColumn_Part_50_Suburb();
      defineColumn_Part_50_Subdivision();

      // Area II
      defineColumn_Part_55_Hamlet();
      defineColumn_Part_55_Croft();
      defineColumn_Part_55_IsolatedDwelling();

      // Area III
      defineColumn_Part_60_Neighbourhood();
      defineColumn_Part_60_Allotments();
      defineColumn_Part_60_Quarter();

      // Area IV
      defineColumn_Part_65_CityBlock();
      defineColumn_Part_65_Residential();
      defineColumn_Part_65_Farm();
      defineColumn_Part_65_Farmyard();
      defineColumn_Part_65_Commercial();
      defineColumn_Part_65_Industrial();
      defineColumn_Part_65_Retail();

      // Other
      defineColumn_Part_80_Aerialway();
      defineColumn_Part_80_Aeroway();
      defineColumn_Part_80_Amenity();
      defineColumn_Part_80_Boundary();
      defineColumn_Part_80_Bridge();
      defineColumn_Part_80_Club();
      defineColumn_Part_80_Craft();
      defineColumn_Part_80_Emergency();
      defineColumn_Part_80_Historic();
      defineColumn_Part_80_Landuse();
      defineColumn_Part_80_Leisure();
      defineColumn_Part_80_ManMade();
      defineColumn_Part_80_Military();
      defineColumn_Part_80_MountainPass();
      defineColumn_Part_80_Natural();
      defineColumn_Part_80_Office();
      defineColumn_Part_80_Place();
      defineColumn_Part_80_Railway();
      defineColumn_Part_80_Shop();
      defineColumn_Part_80_Tourism();
      defineColumn_Part_80_Tunnel();
      defineColumn_Part_80_Waterway();

      defineColumn_Geo_10_BoundingBox_Width();
      defineColumn_Geo_12_BoundingBox_Height();
      defineColumn_Geo_20_Latitude();
      defineColumn_Geo_22_Longitude();
      defineColumn_Geo_30_LatitudeDiff();
      defineColumn_Geo_32_LongitudeDiff();

      defineColumn_Data_10_ID();
      defineColumn_Data_30_LastModified();

      new ColumnDefinitionFor1stVisibleAlignmentColumn(_columnManager);
   }

   private void defineColumn_Data_10_ID() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_DATA_ID.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            cell.setText(Long.toString(locationItem.tourLocation.getLocationId()));
         }
      });
   }

   private void defineColumn_Data_20_AppliedName() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_DATA_APPLIED_NAME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            cell.setText(locationItem.tourLocation.appliedName);
         }
      });
   }

   private void defineColumn_Data_30_LastModified() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_DATA_LAST_MODIFIED.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            final long lastModified = locationItem.tourLocation.lastModified;

            if (lastModified != 0) {

               final ZonedDateTime dt = TimeTools.getZonedDateTime(lastModified);

               cell.setText(dt.format(TimeTools.Formatter_DateTime_S));
            }
         }
      });
   }

   private void defineColumn_Geo_10_BoundingBox_Width() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_WIDTH.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).boundingBoxWidth_Text);
         }
      });
   }

   private void defineColumn_Geo_12_BoundingBox_Height() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_BOUNDING_BOX_HEIGHT.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            cell.setText(locationItem.boundingBoxHeight_Text);
         }
      });
   }

   private void defineColumn_Geo_20_Latitude() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_LATITUDE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            cell.setText(locationItem.latitude_Text);
         }
      });
   }

   private void defineColumn_Geo_22_Longitude() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_LONGITUDE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).longitude_Text);
         }
      });
   }

   private void defineColumn_Geo_30_IsResizedBoundingBox() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_IS_RESIZED_BOUNDING_BOX.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final boolean isResizedBoundingBox = ((LocationItem) cell.getElement()).isResizedBoundingBox;

            cell.setText(isResizedBoundingBox ? UI.SYMBOL_BOX : UI.EMPTY_STRING);

         }
      });
   }

   private void defineColumn_Geo_30_LatitudeDiff() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_LATITUDE_DIFF.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            cell.setText(locationItem.latitudeDiff_Text);
         }
      });
   }

   private void defineColumn_Geo_32_LongitudeDiff() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_LONGITUDE_DIFF.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).longitudeDiff_Text);
         }
      });
   }

   private void defineColumn_Geo_50_Zoomlevel() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_GEO_ZOOMLEVEL.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(Integer.toString(((LocationItem) cell.getElement()).tourLocation.zoomlevel));
         }
      });
   }

   private void defineColumn_Part_10_DisplayName() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_DisplayName.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new TooltipLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.display_name);
         }
      });
   }

   private void defineColumn_Part_12_Name() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Name.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new TooltipLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.name);
         }
      });
   }

   private void defineColumn_Part_30_Continent() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Continent.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.continent);
         }
      });
   }

   private void defineColumn_Part_30_Country() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Country.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.country);
         }
      });
   }

   private void defineColumn_Part_30_CountryCode() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_CountryCode.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.country_code);
         }
      });
   }

   private void defineColumn_Part_35_County() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_County.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.county);
         }
      });
   }

   private void defineColumn_Part_35_Region() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Region.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.region);
         }
      });
   }

   private void defineColumn_Part_35_State() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_State.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.state);
         }
      });
   }

   private void defineColumn_Part_35_StateDistrict() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_StateDistrict.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.state_district);
         }
      });
   }

   private void defineColumn_Part_40_City() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_City.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.city);
         }
      });
   }

   private void defineColumn_Part_40_Municipality() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Municipality.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.municipality);
         }
      });
   }

   private void defineColumn_Part_40_Postcode() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Postcode.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.postcode);
         }
      });
   }

   private void defineColumn_Part_40_Town() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Town.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.town);
         }
      });
   }

   private void defineColumn_Part_40_Village() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Village.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.village);
         }
      });
   }

   private void defineColumn_Part_42_SettlementSmall() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_SettlementSmall.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new TooltipLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.settlementSmall);
         }
      });
   }

   private void defineColumn_Part_43_SettlementLarge() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_SettlementLarge.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new TooltipLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.settlementLarge);
         }
      });
   }

   private void defineColumn_Part_45_HouseName() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_HouseName.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.house_name);
         }
      });
   }

   private void defineColumn_Part_45_HouseNumber() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_HouseNumber.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.house_number);
         }
      });
   }

   private void defineColumn_Part_45_Road() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Road.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.road);
         }
      });
   }

   private void defineColumn_Part_50_Borough() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Borough.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.borough);
         }
      });
   }

   private void defineColumn_Part_50_CityDistrict() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_CityDistrict.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.city_district);
         }
      });
   }

   private void defineColumn_Part_50_District() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_District.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.district);
         }
      });
   }

   private void defineColumn_Part_50_Subdivision() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Subdivision.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.subdivision);
         }
      });
   }

   private void defineColumn_Part_50_Suburb() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Suburb.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.suburb);
         }
      });
   }

   private void defineColumn_Part_55_Croft() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Croft.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.croft);
         }
      });
   }

   private void defineColumn_Part_55_Hamlet() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Hamlet.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.hamlet);
         }
      });
   }

   private void defineColumn_Part_55_IsolatedDwelling() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_IsolatedDwelling.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.isolated_dwelling);
         }
      });
   }

   private void defineColumn_Part_60_Allotments() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Allotments.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.allotments);
         }
      });
   }

   private void defineColumn_Part_60_Neighbourhood() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Neighbourhood.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.neighbourhood);
         }
      });
   }

   private void defineColumn_Part_60_Quarter() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Quarter.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.quarter);
         }
      });
   }

   private void defineColumn_Part_65_CityBlock() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_CityBlock.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.city_block);
         }
      });
   }

   private void defineColumn_Part_65_Commercial() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Commercial.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.commercial);
         }
      });
   }

   private void defineColumn_Part_65_Farm() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Farm.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.farm);
         }
      });
   }

   private void defineColumn_Part_65_Farmyard() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Farmyard.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.farmyard);
         }
      });
   }

   private void defineColumn_Part_65_Industrial() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Industrial.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.industrial);
         }
      });
   }

   private void defineColumn_Part_65_Residential() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Residential.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.residential);
         }
      });
   }

   private void defineColumn_Part_65_Retail() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Retail.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.retail);
         }
      });
   }

   private void defineColumn_Part_80_Aerialway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Aerialway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.aerialway);
         }
      });
   }

   private void defineColumn_Part_80_Aeroway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Aeroway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.aeroway);
         }
      });
   }

   private void defineColumn_Part_80_Amenity() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Amenity.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.amenity);
         }
      });
   }

   private void defineColumn_Part_80_Boundary() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Boundary.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.boundary);
         }
      });
   }

   private void defineColumn_Part_80_Bridge() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Bridge.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.bridge);
         }
      });
   }

   private void defineColumn_Part_80_Club() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Club.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.club);
         }
      });
   }

   private void defineColumn_Part_80_Craft() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Craft.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.craft);
         }
      });
   }

   private void defineColumn_Part_80_Emergency() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Emergency.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.emergency);
         }
      });
   }

   private void defineColumn_Part_80_Historic() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Historic.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.historic);
         }
      });
   }

   private void defineColumn_Part_80_Landuse() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Landuse.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.landuse);
         }
      });
   }

   private void defineColumn_Part_80_Leisure() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Leisure.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.leisure);
         }
      });
   }

   private void defineColumn_Part_80_ManMade() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_ManMade.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.man_made);
         }
      });
   }

   private void defineColumn_Part_80_Military() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Military.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.military);
         }
      });
   }

   private void defineColumn_Part_80_MountainPass() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_MountainPass.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.mountain_pass);
         }
      });
   }

   private void defineColumn_Part_80_Natural() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Natural.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.natural2);
         }
      });
   }

   private void defineColumn_Part_80_Office() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Office.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.office);
         }
      });
   }

   private void defineColumn_Part_80_Place() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Place.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.place);
         }
      });
   }

   private void defineColumn_Part_80_Railway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Railway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.railway);
         }
      });
   }

   private void defineColumn_Part_80_Shop() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Shop.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.shop);
         }
      });
   }

   private void defineColumn_Part_80_Tourism() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Tourism.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.tourism);
         }
      });
   }

   private void defineColumn_Part_80_Tunnel() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Tunnel.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.tunnel);
         }
      });
   }

   private void defineColumn_Part_80_Waterway() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_PART_Waterway.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((LocationItem) cell.getElement()).tourLocation.waterway);
         }
      });
   }

   private void defineColumn_Tour_10_Usage() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_TOUR_USAGE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            final long value = locationItem.numTourStartLocations + locationItem.numTourEndLocations;

            if (value == 0) {

               cell.setText(UI.EMPTY_STRING);
            } else {

               cell.setText(Long.toString(value));
            }
         }
      });
   }

   private void defineColumn_Tour_20_UsageStartLocations() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_TOUR_USAGE_START_LOCATIONS.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());

            final long value = locationItem.numTourStartLocations;

            if (value == 0) {

               cell.setText(UI.EMPTY_STRING);
            } else {

               cell.setText(Long.toString(value));
            }
         }
      });
   }

   private void defineColumn_Tour_30_UsageEndLocations() {

      final ColumnDefinition colDef = TableColumnFactory.LOCATION_TOUR_USAGE_END_LOCATIONS.createColumn(_columnManager, _pc);

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final LocationItem locationItem = ((LocationItem) cell.getElement());
            final long value = locationItem.numTourEndLocations;

            if (value == 0) {

               cell.setText(UI.EMPTY_STRING);
            } else {

               cell.setText(Long.toString(value));
            }
         }
      });
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      super.dispose();
   }

   private void enableActions() {

      final List<TourLocation> allLocations = getSelectedLocations();

      final int numLocations = allLocations.size();

      final boolean isLocationSelected = numLocations > 0;
      final boolean isOneLocation = numLocations == 1;
      final boolean isMultipleLocations = numLocations > 1;

      boolean hasAppliedName = false;

      if (isOneLocation) {

         final String appliedName = allLocations.get(0).appliedName;

         hasAppliedName = appliedName != null && appliedName.length() > 0;
      }

// SET_FORMATTING_OFF

      _actionBoundingBox_Reset            .setEnabled(isLocationSelected);
      _actionBoundingBox_Resize           .setEnabled(isOneLocation);
      _actionCombineBoundingBoxes         .setEnabled(isMultipleLocations);
      _actionDeleteLocation               .setEnabled(isLocationSelected);
      _actionDeleteAndRetrieveAgain       .setEnabled(isLocationSelected);
      _actionIncludeGeoPosition           .setEnabled(isLocationSelected);
      _actionOne                          .setEnabled(isMultipleLocations);
      _actionReapplyLocationIntoTour      .setEnabled(isOneLocation && hasAppliedName);
      _actionRelocateBoundingBox          .setEnabled(isLocationSelected);
      _actionSetLocationIntoTour_Default  .setEnabled(isOneLocation);
      _actionSetLocationIntoTour_Part     .setEnabled(isOneLocation);
      _actionSetLocationIntoTour_Profile  .setEnabled(isOneLocation);

// SET_FORMATTING_ON
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      final boolean isReapplyAvailable = _actionReapplyLocationIntoTour.setupAction();

      // show default profile name in the One action
      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      final String oneActionLabel = defaultProfile == null
            ? Messages.Tour_Location_Action_One
            : Messages.Tour_Location_Action_One + UI.DASH_WITH_DOUBLE_SPACE + defaultProfile.name;

      _actionOne.setText(oneActionLabel);

      /*
       * Fill menu
       */

      menuMgr.add(_actionSetLocationIntoTour_Profile);
      menuMgr.add(_actionSetLocationIntoTour_Part);
      menuMgr.add(_actionSetLocationIntoTour_Default);

      if (isReapplyAvailable) {
         menuMgr.add(_actionReapplyLocationIntoTour);
      }

      menuMgr.add(new Separator());
      menuMgr.add(_actionOne);

      menuMgr.add(new Separator());
      menuMgr.add(_actionBoundingBox_Resize);
      menuMgr.add(_actionIncludeGeoPosition);
      menuMgr.add(_actionCombineBoundingBoxes);
      menuMgr.add(_actionRelocateBoundingBox);
      menuMgr.add(_actionBoundingBox_Reset);

      menuMgr.add(new Separator());
      menuMgr.add(_actionDeleteAndRetrieveAgain);
      menuMgr.add(_actionDeleteLocation);

      enableActions();
   }

   private void fillMenu_SetLoctionIntoTour_Default(final Menu menu) {

      final TourLocation tourLocation = getSelectedLocations().get(0);

      final Map<LocationPartID, PartItem> allLocationParts = PartItem.getAllPartItems(tourLocation, true);

      if (allLocationParts == null) {
         return;
      }

      final PartItem osmDefaultPart = allLocationParts.get(LocationPartID.OSM_DEFAULT_NAME);
      if (osmDefaultPart == null) {
         return;
      }

      final String osmDefaultNames = osmDefaultPart.locationLabel_Start;

      final String[] allNames = osmDefaultNames.split(UI.SYMBOL_COMMA);

      // remove leading/trailing spaces
      final String[] allNamesTrimmed = Arrays.stream(allNames).map(String::trim).toArray(String[]::new);

      // sort names
      Arrays.sort(allNamesTrimmed);

      _usedDisplayNames.clear();

      for (final String locationLabel : allNamesTrimmed) {

         if (locationLabel.length() > 0

               // prevent to display duplicated labels
               && _usedDisplayNames.contains(locationLabel) == false) {

            _usedDisplayNames.add(locationLabel);

            addActionToMenu(menu,

                  new ActionSetLocationIntoTour(

                        locationLabel,
                        null,

                        tourLocation));
         }
      }
   }

   private void fillMenu_SetLoctionIntoTour_Part(final Menu menu) {

      final TourLocation tourLocation = getSelectedLocations().get(0);

      final Map<LocationPartID, PartItem> allLocationParts = PartItem.getAllPartItems(tourLocation, true);

      if (allLocationParts == null) {
         return;
      }

      _usedDisplayNames.clear();

      // sort parts by name
      final Collection<PartItem> allUnsortedParts = allLocationParts.values();
      final List<PartItem> allSortedParts = new ArrayList<>(allUnsortedParts);
      Collections.sort(allSortedParts);

      for (final PartItem partItem : allSortedParts) {

         final String locationLabel = partItem.locationLabel_Start;
         final String actionTooltip = partItem.partLabel_Start;

         if (locationLabel.length() > 0

               // prevent to display duplicated labels
               && _usedDisplayNames.contains(locationLabel) == false) {

            _usedDisplayNames.add(locationLabel);

            addActionToMenu(menu,

                  new ActionSetLocationIntoTour(

                        locationLabel,
                        actionTooltip,

                        tourLocation));
         }
      }
   }

   private void fillMenu_SetLoctionIntoTour_Profile(final Menu menu) {

      final TourLocation tourLocation = getSelectedLocations().get(0);

      final TourLocationProfile defaultProfile = TourLocationManager.getDefaultProfile();

      final List<TourLocationProfile> allProfiles = TourLocationManager.getProfiles();

      /*
       * Sort profiles by location name
       */
      final Map<String, TourLocationProfile> allUnsortedProfiles = new HashMap<>();

      _usedDisplayNames.clear();

      for (final TourLocationProfile locationProfile : allProfiles) {

         final String locationLabel = TourLocationManager.createLocationDisplayName(tourLocation, locationProfile);

         if (locationLabel.length() > 0

               // prevent duplicate names
               && _usedDisplayNames.contains(locationLabel) == false) {

            _usedDisplayNames.add(locationLabel);

            allUnsortedProfiles.put(locationLabel, locationProfile);
         }
      }

      final Map<String, TourLocationProfile> allSortedProfiles = new TreeMap<>(allUnsortedProfiles);

      /*
       * Create an action for each profile
       */
      for (final Entry<String, TourLocationProfile> entrySet : allSortedProfiles.entrySet()) {

         final String locationLabel = entrySet.getKey();
         final TourLocationProfile locationProfile = entrySet.getValue();

         final boolean isDefaultProfile = locationProfile.equals(defaultProfile);

         final String defaultMarker = isDefaultProfile

               // show a marker for the default profile
               ? UI.SYMBOL_STAR + UI.SPACE

               : UI.EMPTY_STRING;

         final String profileName = defaultMarker + PROFILE_NAME.formatted(locationProfile.getName(), locationProfile.getZoomlevel());

         final String partText = Messages.Tour_Location_Action_Profile_Tooltip.formatted(

               TourLocationManager.createJoinedPartNames(locationProfile, UI.NEW_LINE1),
               locationProfile.getZoomlevel());

         final String locationTooltip = profileName + UI.NEW_LINE2 + partText;

         addActionToMenu(menu,

               new ActionSetLocationIntoTour(

                     locationLabel,
                     locationTooltip,

                     tourLocation));
      }
   }

   private void fillToolbar() {

      final IActionBars actionBars = getViewSite().getActionBars();

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = actionBars.getToolBarManager();

      tbm.add(_actionLocationFilter);
      tbm.add(_actionShowLocationInfo);
      tbm.add(_actionLinkWithOtherViews);
   }

   private void fireLocationSelection() {

      final IStructuredSelection selection = _locationViewer.getStructuredSelection();

      final List<TourLocation> allTourLocations = new ArrayList<>();

      for (final Object object : selection.toArray()) {

         if (object instanceof final LocationItem locationItem) {
            allTourLocations.add(locationItem.tourLocation);
         }
      }

      // this view could be inactive -> selection is not fired with the SelectionProvider interface
      TourManager.fireEventWithCustomData(
            TourEventId.TOUR_LOCATION_SELECTION,
            allTourLocations,
            this);
   }

   private void fireUpdateUI() {

      // cached tours are not valid any more
      TourManager.getInstance().clearTourDataCache();

      // fire modify event
      TourManager.fireEvent(TourEventId.UPDATE_UI);

      // fire selection to update the map
      fireLocationSelection();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   List<String> getCountries() {

      return _allLocationCountries;
   }

   String getLocationFilter_Country() {

      return _locationFilter_Country;
   }

   TableViewer getLocationViewer() {
      return _locationViewer;
   }

   private List<TourLocation> getSelectedLocations() {

      final List<TourLocation> allTourLocations = new ArrayList<>();

      final IStructuredSelection structuredSelection = _locationViewer.getStructuredSelection();

      for (final Object selection : structuredSelection) {
         if (selection instanceof final LocationItem locationItem) {
            allTourLocations.add(locationItem.tourLocation);
         }
      }

      return allTourLocations;
   }

   /**
    * @param sortColumnId
    *
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _locationViewer.getTable().getColumns();

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
      return _locationViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _locationViewer.getSelection();
   }

   /**
    * Hide the tooltip when mouse is not hovering the tooltip and the mouse have exited the view
    */
   private void hideTooltip() {

      _viewerContainer.getDisplay().timerExec(100, () -> {

         if (_locationTooltip.isMouseHovered() == false) {

            _locationTooltip.hide();
         }
      });
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onColumn_Select(e);
         }
      };
   }

   boolean isLocationFilterActive() {

      return _isLocationFilterActive;
   }

   boolean isShowLocationTooltip() {

      return _actionShowLocationInfo.isChecked();
   }

   private void loadAllLocations() {

      _allLocationItems.clear();
      _locationItemsMap.clear();

      loadAllLocations_10_Locations();
      loadAllLocations_20_NumberOfTours();
   }

   private String loadAllLocations_05_AllFields() {

      final String sql = UI.EMPTY_STRING

            + "name," + NL //                      1  //$NON-NLS-1$
            + "display_name," + NL //              2  //$NON-NLS-1$

            + "continent," + NL //                 3  //$NON-NLS-1$
            + "country," + NL //                   4  //$NON-NLS-1$
            + "country_code," + NL //              5  //$NON-NLS-1$

            + "region," + NL //                    6  //$NON-NLS-1$
            + "state," + NL //                     7  //$NON-NLS-1$
            + "state_district," + NL //            8  //$NON-NLS-1$
            + "county," + NL //                    9  //$NON-NLS-1$

            + "municipality," + NL //              10 //$NON-NLS-1$
            + "city," + NL //                      11 //$NON-NLS-1$
            + "town," + NL //                      12 //$NON-NLS-1$
            + "village," + NL //                   13 //$NON-NLS-1$

            + "city_district," + NL //             14 //$NON-NLS-1$
            + "district," + NL //                  15 //$NON-NLS-1$
            + "borough," + NL //                   16 //$NON-NLS-1$
            + "suburb," + NL //                    17 //$NON-NLS-1$
            + "subdivision," + NL //               18 //$NON-NLS-1$

            + "hamlet," + NL //                    19 //$NON-NLS-1$
            + "croft," + NL //                     20 //$NON-NLS-1$
            + "isolated_dwelling," + NL //         21 //$NON-NLS-1$

            + "neighbourhood," + NL //             22 //$NON-NLS-1$
            + "allotments," + NL //                23 //$NON-NLS-1$
            + "quarter," + NL //                   24 //$NON-NLS-1$

            + "city_block," + NL //                25 //$NON-NLS-1$
            + "residential," + NL //               26 //$NON-NLS-1$
            + "farm," + NL //                      27 //$NON-NLS-1$
            + "farmyard," + NL //                  28 //$NON-NLS-1$
            + "industrial," + NL //                29 //$NON-NLS-1$
            + "commercial," + NL //                30 //$NON-NLS-1$
            + "retail," + NL //                    31 //$NON-NLS-1$

            + "road," + NL //                      32 //$NON-NLS-1$

            + "house_number," + NL //              33 //$NON-NLS-1$
            + "house_name," + NL //                34 //$NON-NLS-1$

            + "aerialway," + NL //                 35 //$NON-NLS-1$
            + "aeroway," + NL //                   36 //$NON-NLS-1$
            + "amenity," + NL //                   37 //$NON-NLS-1$
            + "boundary," + NL //                  38 //$NON-NLS-1$
            + "bridge," + NL //                    39 //$NON-NLS-1$
            + "club," + NL //                      40 //$NON-NLS-1$
            + "craft," + NL //                     41 //$NON-NLS-1$
            + "emergency," + NL //                 42 //$NON-NLS-1$
            + "historic," + NL //                  43 //$NON-NLS-1$
            + "landuse," + NL //                   44 //$NON-NLS-1$
            + "leisure," + NL //                   45 //$NON-NLS-1$
            + "man_made," + NL //                  46 //$NON-NLS-1$
            + "military," + NL //                  47 //$NON-NLS-1$
            + "mountain_pass," + NL //             48 //$NON-NLS-1$
            + "natural2," + NL //                  49 //$NON-NLS-1$
            + "office," + NL //                    50 //$NON-NLS-1$
            + "place," + NL //                     51 //$NON-NLS-1$
            + "railway," + NL //                   52 //$NON-NLS-1$
            + "shop," + NL //                      53 //$NON-NLS-1$
            + "tourism," + NL //                   54 //$NON-NLS-1$
            + "tunnel," + NL //                    55 //$NON-NLS-1$
            + "waterway," + NL //                  56 //$NON-NLS-1$

            + "postcode," + NL //                  57 //$NON-NLS-1$

            + "latitudeE6_Normalized," + NL //                    58 //$NON-NLS-1$
            + "longitudeE6_Normalized," + NL //                   59 //$NON-NLS-1$

            + "latitudeMinE6_Normalized," + NL //                 60 //$NON-NLS-1$
            + "latitudeMaxE6_Normalized," + NL //                 61 //$NON-NLS-1$
            + "longitudeMinE6_Normalized," + NL //                62 //$NON-NLS-1$
            + "longitudeMaxE6_Normalized," + NL //                63 //$NON-NLS-1$

            + "latitudeMinE6_Resized_Normalized," + NL //         64 //$NON-NLS-1$
            + "latitudeMaxE6_Resized_Normalized," + NL //         65 //$NON-NLS-1$
            + "longitudeMinE6_Resized_Normalized," + NL //        66 //$NON-NLS-1$
            + "longitudeMaxE6_Resized_Normalized," + NL //        67 //$NON-NLS-1$

            + "zoomlevel," + NL //                                68 //$NON-NLS-1$

            + "locationID," + NL //                               69 //$NON-NLS-1$

            + "appliedName," + NL //                              70 //$NON-NLS-1$
            + "lastModified" + NL //                              71 //$NON-NLS-1$
      ;

      return sql;
   }

   private void loadAllLocations_10_Locations() {

      final Set<String> allCountries = new HashSet<>();

      final String allLocationFields = loadAllLocations_05_AllFields();

      String sql;

      final TourPerson activePerson = TourbookPlugin.getActivePerson();

      if (activePerson == null) {

         // select all people

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                       //$NON-NLS-1$

               + allLocationFields

               + "FROM " + TourDatabase.TABLE_TOUR_LOCATION + NL //     //$NON-NLS-1$
         ;

      } else {

         // use person filter

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + allLocationFields + UI.SYMBOL_COMMA + NL
               + "TourData.tourPerson_personId" + NL //                                         //$NON-NLS-1$

               + "FROM TourLocation" + NL //                                                    //$NON-NLS-1$
               + "LEFT JOIN TourData AS TourData" + NL //                                       //$NON-NLS-1$
               + "ON TourLocation.LocationID = TourData.TourLocationStart_LocationID" + NL //   //$NON-NLS-1$
               + "WHERE TourData.tourPerson_personId = ?" + NL //                               //$NON-NLS-1$

               + "UNION" + NL //                                                                //$NON-NLS-1$

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + allLocationFields + UI.SYMBOL_COMMA + NL
               + "TourData.tourPerson_personId" + NL //                                         //$NON-NLS-1$

               + "FROM TourLocation" + NL //                                                    //$NON-NLS-1$
               + "LEFT JOIN TourData AS TourData" + NL //                                       //$NON-NLS-1$
               + "ON TourLocation.LocationID = TourData.TourLocationEnd_LocationID" + NL //     //$NON-NLS-1$
               + "WHERE TourData.tourPerson_personId = ?" + NL //                               //$NON-NLS-1$
         ;
      }

      PreparedStatement statement = null;
      ResultSet result = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         // set person filter parameter
         if (activePerson != null) {

            final long personId = activePerson.getPersonId();

            statement.setLong(1, personId);
            statement.setLong(2, personId);
         }

         result = statement.executeQuery();

         while (result.next()) {

            final LocationItem locationItem = new LocationItem();

            // create detached tour location
            final TourLocation tourLocation = locationItem.tourLocation = new TourLocation();

//SET_FORMATTING_OFF

            tourLocation.name                = result.getString(1);
            tourLocation.display_name        = result.getString(2);

            tourLocation.continent           = result.getString(3);
            tourLocation.country             = result.getString(4);
            tourLocation.country_code        = result.getString(5);

            tourLocation.region              = result.getString(6);
            tourLocation.state               = result.getString(7);
            tourLocation.state_district      = result.getString(8);
            tourLocation.county              = result.getString(9);

            tourLocation.municipality        = result.getString(10);
            tourLocation.city                = result.getString(11);
            tourLocation.town                = result.getString(12);
            tourLocation.village             = result.getString(13);

            tourLocation.city_district       = result.getString(14);
            tourLocation.district            = result.getString(15);
            tourLocation.borough             = result.getString(16);
            tourLocation.suburb              = result.getString(17);
            tourLocation.subdivision         = result.getString(18);

            tourLocation.hamlet              = result.getString(19);
            tourLocation.croft               = result.getString(20);
            tourLocation.isolated_dwelling   = result.getString(21);

            tourLocation.neighbourhood       = result.getString(22);
            tourLocation.allotments          = result.getString(23);
            tourLocation.quarter             = result.getString(24);

            tourLocation.city_block          = result.getString(25);
            tourLocation.residential         = result.getString(26);
            tourLocation.farm                = result.getString(27);
            tourLocation.farmyard            = result.getString(28);
            tourLocation.industrial          = result.getString(29);
            tourLocation.commercial          = result.getString(30);
            tourLocation.retail              = result.getString(31);

            tourLocation.road                = result.getString(32);

            tourLocation.house_number        = result.getString(33);
            tourLocation.house_name          = result.getString(34);

            tourLocation.aerialway           = result.getString(35);
            tourLocation.aeroway             = result.getString(36);
            tourLocation.amenity             = result.getString(37);
            tourLocation.boundary            = result.getString(38);
            tourLocation.bridge              = result.getString(39);
            tourLocation.club                = result.getString(40);
            tourLocation.craft               = result.getString(41);
            tourLocation.emergency           = result.getString(42);
            tourLocation.historic            = result.getString(43);
            tourLocation.landuse             = result.getString(44);
            tourLocation.leisure             = result.getString(45);
            tourLocation.man_made            = result.getString(46);
            tourLocation.military            = result.getString(47);
            tourLocation.mountain_pass       = result.getString(48);
            tourLocation.natural2            = result.getString(49);
            tourLocation.office              = result.getString(50);
            tourLocation.place               = result.getString(51);
            tourLocation.railway             = result.getString(52);
            tourLocation.shop                = result.getString(53);
            tourLocation.tourism             = result.getString(54);
            tourLocation.tunnel              = result.getString(55);
            tourLocation.waterway            = result.getString(56);

            tourLocation.postcode            = result.getString(57);

            final int latitudeE6_Normalized              = result.getInt(58);
            final int longitudeE6_Normalized             = result.getInt(59);

            final int latitudeMinE6_Normalized           = result.getInt(60);
            final int latitudeMaxE6_Normalized           = result.getInt(61);
            final int longitudeMinE6_Normalized          = result.getInt(62);
            final int longitudeMaxE6_Normalized          = result.getInt(63);

            final int latitudeMinE6_Resized_Normalized   = result.getInt(64);
            final int latitudeMaxE6_Resized_Normalized   = result.getInt(65);
            final int longitudeMinE6_Resized_Normalized  = result.getInt(66);
            final int longitudeMaxE6_Resized_Normalized  = result.getInt(67);

            tourLocation.zoomlevel                       = result.getInt(68);

            final long locationID                        = result.getLong(69);

            tourLocation.appliedName                     = result.getString(70);
            tourLocation.lastModified                    = result.getLong(71);

            /*
             * Set geo positions
             */
            tourLocation.latitudeE6_Normalized              = latitudeE6_Normalized;
            tourLocation.longitudeE6_Normalized             = longitudeE6_Normalized;

            tourLocation.latitudeMinE6_Normalized           = latitudeMinE6_Normalized;
            tourLocation.latitudeMaxE6_Normalized           = latitudeMaxE6_Normalized;
            tourLocation.longitudeMinE6_Normalized          = longitudeMinE6_Normalized;
            tourLocation.longitudeMaxE6_Normalized          = longitudeMaxE6_Normalized;

            tourLocation.latitudeMinE6_Resized_Normalized   = latitudeMinE6_Resized_Normalized;
            tourLocation.latitudeMaxE6_Resized_Normalized   = latitudeMaxE6_Resized_Normalized;
            tourLocation.longitudeMinE6_Resized_Normalized  = longitudeMinE6_Resized_Normalized;
            tourLocation.longitudeMaxE6_Resized_Normalized  = longitudeMaxE6_Resized_Normalized;

//SET_FORMATTING_ON

            tourLocation.setTransientValues();

            updateUI_LocationItem(locationItem, tourLocation);

            /*
             * Keep location
             */
            tourLocation.setLocationID(locationID);

            _allLocationItems.add(locationItem);
            _locationItemsMap.put(locationID, locationItem);

            allCountries.add(tourLocation.country);

            if (UI.IS_SCRAMBLE_DATA) {
               scrambleData(tourLocation);
            }
         }

         setupCountries(allCountries);

      } catch (final SQLException e) {
         SQL.showException(e);
      } finally {
         Util.closeSql(statement);
         Util.closeSql(result);
      }
   }

   private void loadAllLocations_20_NumberOfTours() {

      String sql;

      final TourPerson activePerson = TourbookPlugin.getActivePerson();

      if (activePerson == null) {

         // select all people

         sql = UI.EMPTY_STRING

               /*
                * Start locations
                */
               + "SELECT" + NL //                                                   //$NON-NLS-1$
               + "   TourLocation.LocationID," + NL //                              //$NON-NLS-1$
               + "   COUNT(TourDataStart.TourLocationSTART_LocationID)," + NL //    //$NON-NLS-1$
               + "   0" + NL //                                                     //$NON-NLS-1$
               + "FROM TourLocation" + NL //                                        //$NON-NLS-1$
               + "LEFT JOIN TourData AS TourDataStart ON TourLocation.LocationID = TourDataStart.TourLocationSTART_LocationID " + NL //$NON-NLS-1$
               + "GROUP BY" + NL //                                                 //$NON-NLS-1$
               + "   TourLocation.LocationID" + NL //                               //$NON-NLS-1$

               + "UNION" + NL //                                                    //$NON-NLS-1$

               /*
                * End locations
                */
               + "SELECT" + NL //                                                   //$NON-NLS-1$
               + "   TourLocation.LocationID," + NL //                              //$NON-NLS-1$
               + "   0," + NL //                                                    //$NON-NLS-1$
               + "   COUNT(TourDataEnd.TourLocationEND_LocationID)" + NL //         //$NON-NLS-1$
               + "FROM TourLocation" + NL //                                        //$NON-NLS-1$
               + "LEFT JOIN TourData AS TourDataEnd ON TourLocation.LocationID = TourDataEnd.TourLocationEND_LocationID " + NL //$NON-NLS-1$
               + "GROUP BY " + NL //                                                //$NON-NLS-1$
               + "   TourLocation.LocationID" + NL //                               //$NON-NLS-1$
         ;

      } else {

         // use person filter

         sql = UI.EMPTY_STRING

               /*
                * Start locations
                */
               + "SELECT                                                            " + NL //$NON-NLS-1$
               + "   TourLocation.LocationID,                                       " + NL //$NON-NLS-1$
               + "   COUNT(TourDataStart.TourLocationSTART_LocationID),             " + NL //$NON-NLS-1$
               + "   0                                                              " + NL //$NON-NLS-1$
               + "FROM TourLocation                                                 " + NL //$NON-NLS-1$
               + "LEFT JOIN                                                         " + NL //$NON-NLS-1$
               + "   (                                                              " + NL //$NON-NLS-1$
               + "      SELECT                                                      " + NL //$NON-NLS-1$
               + "         TourLocationSTART_LocationID,                            " + NL //$NON-NLS-1$
               + "         tourPerson_personId                                      " + NL //$NON-NLS-1$
               + "      FROM TourData                                               " + NL //$NON-NLS-1$
               + "      WHERE tourPerson_personId = ?                               " + NL //$NON-NLS-1$
               + "            AND TourLocationSTART_LocationID IS NOT NULL          " + NL //$NON-NLS-1$
               + "   )                                                              " + NL //$NON-NLS-1$
               + "   AS TourDataStart                                               " + NL //$NON-NLS-1$
               + "   ON TourLocation.LocationID = TourDataStart.TourLocationSTART_LocationID" + NL //$NON-NLS-1$
               + "GROUP BY                                                          " + NL //$NON-NLS-1$
               + "   TourLocation.LocationID                                        " + NL //$NON-NLS-1$

               + "UNION                                                             " + NL //$NON-NLS-1$

               /*
                * Endlocations
                */
               + "SELECT                                                            " + NL //$NON-NLS-1$
               + "   TourLocation.LocationID,                                       " + NL //$NON-NLS-1$
               + "   0,                                                             " + NL //$NON-NLS-1$
               + "   COUNT(TourDataEnd.TourLocationEND_LocationID)                  " + NL //$NON-NLS-1$
               + "FROM TourLocation                                                 " + NL //$NON-NLS-1$
               + "LEFT JOIN                                                         " + NL //$NON-NLS-1$
               + "   (                                                              " + NL //$NON-NLS-1$
               + "      SELECT                                                      " + NL //$NON-NLS-1$
               + "         TourLocationEND_LocationID,                              " + NL //$NON-NLS-1$
               + "         tourPerson_personId                                      " + NL //$NON-NLS-1$
               + "      FROM TourData                                               " + NL //$NON-NLS-1$
               + "      WHERE tourPerson_personId = ?                               " + NL //$NON-NLS-1$
               + "            AND TourLocationEND_LocationID IS NOT NULL            " + NL //$NON-NLS-1$
               + "   )                                                              " + NL //$NON-NLS-1$
               + "   AS TourDataEnd                                                 " + NL //$NON-NLS-1$
               + "   ON TourLocation.LocationID = TourDataEnd.TourLocationEND_LocationID" + NL //$NON-NLS-1$
               + "GROUP BY                                                          " + NL //$NON-NLS-1$
               + "   TourLocation.LocationID                                        " + NL //$NON-NLS-1$
         ;
      }

      PreparedStatement statement = null;
      ResultSet result = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         // set person filter parameter
         if (activePerson != null) {

            final long personId = activePerson.getPersonId();

            statement.setLong(1, personId);
            statement.setLong(2, personId);
         }

         result = statement.executeQuery();

         while (result.next()) {

            final long locationID = result.getLong(1);
            final int numStartLocation = result.getInt(2);
            final int numEndLocation = result.getInt(3);

            if (numStartLocation != 0) {

               final LocationItem locationItem = _locationItemsMap.get(locationID);

               if (locationItem != null) {

                  locationItem.numTourStartLocations = numStartLocation;
                  locationItem.numTourAllLocations += numStartLocation;
               }
            }

            if (numEndLocation != 0) {

               final LocationItem locationItem = _locationItemsMap.get(locationID);

               if (locationItem != null) {

                  locationItem.numTourEndLocations = numEndLocation;
                  locationItem.numTourAllLocations += numEndLocation;
               }
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e);
      } finally {
         Util.closeSql(statement);
         Util.closeSql(result);
      }
   }

   /**
    */
   private void onAction_BoundingBox_Reset() {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      for (final TourLocation tourLocation : allSelectedLocations) {

         TourLocationManager.setResizedBoundingBox(tourLocation.getLocationId(),

               tourLocation.latitudeMinE6_Normalized,
               tourLocation.latitudeMaxE6_Normalized,

               tourLocation.longitudeMinE6_Normalized,
               tourLocation.longitudeMaxE6_Normalized);
      }

      fireUpdateUI();
   }

   private void onAction_BoundingBox_Resize() {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TourLocation tourLocation = getSelectedLocations().get(0);

      new DialogResizeTourLocation(this, tourLocation).open();
   }

   private void onAction_CombineBoundingBox() {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      if (allSelectedLocations.size() < 2) {
         return;
      }

      boolean isFirst = true;

      int latitudeMinE6_Normalized = 0;
      int latitudeMaxE6_Normalized = 0;
      int longitudeMinE6_Normalized = 0;
      int longitudeMaxE6_Normalized = 0;

      for (final TourLocation tourLocation : allSelectedLocations) {

         if (isFirst) {

            isFirst = false;

            latitudeMinE6_Normalized = tourLocation.latitudeMinE6_Resized_Normalized;
            latitudeMaxE6_Normalized = tourLocation.latitudeMaxE6_Resized_Normalized;

            longitudeMinE6_Normalized = tourLocation.longitudeMinE6_Resized_Normalized;
            longitudeMaxE6_Normalized = tourLocation.longitudeMaxE6_Resized_Normalized;
         }

         latitudeMinE6_Normalized = Math.min(latitudeMinE6_Normalized, tourLocation.latitudeMinE6_Resized_Normalized);
         latitudeMaxE6_Normalized = Math.max(latitudeMaxE6_Normalized, tourLocation.latitudeMaxE6_Resized_Normalized);

         longitudeMinE6_Normalized = Math.min(longitudeMinE6_Normalized, tourLocation.longitudeMinE6_Resized_Normalized);
         longitudeMaxE6_Normalized = Math.max(longitudeMaxE6_Normalized, tourLocation.longitudeMaxE6_Resized_Normalized);
      }

      TourLocationManager.setResizedBoundingBox(allSelectedLocations.get(0).getLocationId(),

            latitudeMinE6_Normalized,
            latitudeMaxE6_Normalized,

            longitudeMinE6_Normalized,
            longitudeMaxE6_Normalized);

      fireUpdateUI();
   }

   private void onAction_DeleteAndReapply(final boolean isOneAction) {

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      if (TourLocationManager.deleteAndReapply(allSelectedLocations, isOneAction)) {

         fireUpdateUI();
      }
   }

   private void onAction_DeleteLocation() {

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      if (TourLocationManager.deleteTourLocations(allSelectedLocations, false) == false) {
         return;
      }

      /*
       * Deletion was performed -> update viewer
       */

      final Table table = _locationViewer.getTable();

      // get index for selected location
      final int lastLocationIndex = table.getSelectionIndex();

      // update model
      loadAllLocations();

      // reload viewer
      updateUI_SetViewerInput();

      // get next location
      LocationItem nextLocationItem = (LocationItem) _locationViewer.getElementAt(lastLocationIndex);

      if (nextLocationItem == null) {
         nextLocationItem = (LocationItem) _locationViewer.getElementAt(lastLocationIndex - 1);
      }

      // select next location
      if (nextLocationItem != null) {
         _locationViewer.setSelection(new StructuredSelection(nextLocationItem), true);
      }

      table.setFocus();

      fireUpdateUI();

      TourManager.fireEventWithCustomData(
            TourEventId.TOUR_LOCATION_SELECTION,
            null,
            this);
   }

   private void onAction_IncludeGeoPosition() {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      TourLocationManager.setResizedBoundingBox_IncludeGeoPosition(allSelectedLocations);

      fireUpdateUI();
   }

   private void onAction_One() {

      onAction_IncludeGeoPosition();
      onAction_CombineBoundingBox();

      onAction_DeleteAndReapply(true);
   }

   private void onAction_RelocateBoundingBox() {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<TourLocation> allSelectedLocations = getSelectedLocations();

      TourLocationManager.setResizedBoundingBox_Relocate(allSelectedLocations);

      fireUpdateUI();
   }

   private void onAction_SetLocationIntoTour(final String locationLabel, final TourLocation tourLocation) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return;
      }

      TourLocationManager.setLocationIntoTour(locationLabel, tourLocation);

      _locationViewer.refresh();

      fireUpdateUI();
   }

   private void onColumn_Select(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _locationComparator.setSortColumn(e.widget);

            _locationViewer.refresh();
         }
         selectSelectionInViewer(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_actionLinkWithOtherViews.isChecked() == false) {
         return;
      }

      // select tour locations for the selected tours
      if (selection instanceof final SelectionTourId selectionTourId) {

         final long tourId = selectionTourId.getTourId();
         final TourData tourData = TourManager.getTour(tourId);
         final List<TourData> tourDataList = List.of(tourData);

         selectTourLocations(tourDataList);

      } else if (selection instanceof final SelectionTourIds selectionTourIds) {

         final List<Long> allTourIds = selectionTourIds.getTourIds();
         final List<TourData> allTourData = new ArrayList<>();

         TourManager.loadTourData(allTourIds, allTourData, false);

         selectTourLocations(allTourData);
      }
   }

   private void onSelectLocation() {

      // hide tooltip which could display content from the previous tour location
      _locationTooltip.hide();

      if (_isInUIUpdate) {
         return;
      }

      fireLocationSelection();
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            _locationViewer.getTable().dispose();

            createUI_10_LocationViewer(_viewerContainer);

            // update UI
            _viewerContainer.layout();

            // update the viewer
            updateUI_SetViewerInput();
         }
         selectSelectionInViewer(selectionBackup);
      }
      _viewerContainer.setRedraw(true);

      _locationViewer.getTable().setFocus();

      return _locationViewer;
   }

   @Override
   public void reloadViewer() {

      loadAllLocations();

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            updateUI_SetViewerInput();
         }
         selectSelectionInViewer(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, TableColumnFactory.SENSOR_NAME_ID);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, LocationComparator.ASCENDING);

      // update comparator
      _locationComparator.__sortColumnId = sortColumnId;
      _locationComparator.__sortDirection = sortDirection;

      // location filter
      _isLocationFilterActive = Util.getStateBoolean(_state, STATE_IS_LOCATION_FILTER_ACTIVE, false);
      _locationFilter_Country = Util.getStateString(_state, STATE_LOCATION_FILTER_COUNTRY, null);
   }

   private void restoreState_WithUI() {

      /*
       * Restore selected location
       */
      final String[] allViewerIndices = _state.getArray(STATE_SELECTED_SENSOR_INDICES);

      if (allViewerIndices != null) {

         final ArrayList<Object> allLocations = new ArrayList<>();

         for (final String viewerIndex : allViewerIndices) {

            Object location = null;

            try {
               final int index = Integer.parseInt(viewerIndex);
               location = _locationViewer.getElementAt(index);
            } catch (final NumberFormatException e) {
               // just ignore
            }

            if (location != null) {
               allLocations.add(location);
            }
         }

         if (allLocations.size() > 0) {

            _viewerContainer.getDisplay().timerExec(

                  /*
                   * When this value is too small, then the chart axis could not be painted
                   * correctly with the dark theme during the app startup
                   */
                  1000,

                  () -> {

                     _locationViewer.setSelection(new StructuredSelection(allLocations.toArray()), true);

                     enableActions();
                  });
         }
      }

      _actionLinkWithOtherViews.setChecked(Util.getStateBoolean(_state, STATE_IS_LINK_WITH_OTHER_VIEWS, false));
      _actionShowLocationInfo.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_INFO_TOOLTIP, false));

      // location filter
      _actionLocationFilter.setSelection(_isLocationFilterActive);

      enableActions();
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_SORT_COLUMN_ID, _locationComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _locationComparator.__sortDirection);

      _state.put(STATE_IS_LINK_WITH_OTHER_VIEWS, _actionLinkWithOtherViews.isChecked());
      _state.put(STATE_IS_SHOW_INFO_TOOLTIP, _actionShowLocationInfo.isChecked());

      _state.put(STATE_IS_LOCATION_FILTER_ACTIVE, _isLocationFilterActive);
      _state.put(STATE_LOCATION_FILTER_COUNTRY, _locationFilter_Country);

      // keep selected tours
      Util.setState(_state, STATE_SELECTED_SENSOR_INDICES, _locationViewer.getTable().getSelectionIndices());
   }

   /**
    * Scramble fields in the tour location
    *
    * @param tourLocation
    */
   private void scrambleData(final TourLocation tourLocation) {

      try {

         for (final Field field : TourLocation.class.getDeclaredFields()) {

            final int modifiers = field.getModifiers();
            final boolean isStatic = Modifier.isStatic(modifiers);
            final boolean isPrivate = Modifier.isPrivate(modifiers);

            if (isStatic || isPrivate) {
               continue;
            }

            final String fieldName = field.getName();

            if (false

                  || "locationID".equals(fieldName) //$NON-NLS-1$
                  || "boundingBoxKey".equals(fieldName) //$NON-NLS-1$
                  || "country".equals(fieldName) //$NON-NLS-1$

                  || fieldName.startsWith("lang") //$NON-NLS-1$
                  || fieldName.startsWith("long") //$NON-NLS-1$

            ) {

               continue;
            }

            final Type fieldType = field.getGenericType();

            if (String.class.equals(fieldType)) {

               final String fieldValue = (String) field.get(tourLocation);
               final String scrambledText = UI.scrambleText(fieldValue);

               field.set(tourLocation, scrambledText);
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {

         StatusUtil.showStatus(e);
      }
   }

   /**
    * Select and reveal tour location item
    *
    * @param selection
    * @param checkedElements
    */
   private void selectSelectionInViewer(final ISelection selection) {

      _isInUIUpdate = true;
      {
         _locationViewer.setSelection(selection, true);

         final Table table = _locationViewer.getTable();
         table.showSelection();
      }
      _isInUIUpdate = false;
   }

   private void selectTourLocations(final List<TourData> allTourData) {

      final List<TourLocationExtended> allTourLocations = TourLocationManager.getTourLocations(allTourData);
      final List<LocationItem> allLocationItems = new ArrayList<>();

      /*
       * Get viewer items from the provided tour locations
       */
      for (final TourLocationExtended tourLocationExtended : allTourLocations) {

         final LocationItem locationItem = _locationItemsMap.get(tourLocationExtended.tourLocation.getLocationId());

         if (locationItem != null) {
            allLocationItems.add(locationItem);
         }
      }

      final StructuredSelection selection = new StructuredSelection(allLocationItems.toArray());

      selectSelectionInViewer(selection);
   }

   @Override
   public void setFocus() {

      _locationViewer.getTable().setFocus();
   }

   private void setupCountries(final Set<String> allCountries) {

      _allLocationCountries = new ArrayList<>(allCountries);

      // create sorted country list
      Collections.sort(_allLocationCountries);

      if (_locationFilter_Country != null) {

         // ensure that the filter country is available in all countries

         boolean countryIsAvailable = false;

         for (final String country : allCountries) {

            if (country.equals(_locationFilter_Country)) {

               countryIsAvailable = true;

               break;
            }
         }

         if (countryIsAvailable == false) {

            // filter is invalid -> reset to display all locations

            _locationFilter_Country = null;
         }
      }
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   void updateTourLocationFilter(final String country) {

      // update model
      _locationFilter_Country = country;

      // run async that the slideout UI is updated immediately
      _parent.getDisplay().asyncExec(() -> {

         _viewerContainer.setRedraw(false);
         {
            _locationViewer.refresh();
         }
         _viewerContainer.setRedraw(true);
      });
   }

   void updateUI(final TourLocation tourLocation) {

      final LocationItem locationItem = _locationItemsMap.get(tourLocation.getLocationId());

      if (locationItem != null) {

         updateUI_LocationItem(locationItem, tourLocation);

         _locationViewer.update(locationItem, null);
      }
   }

   private void updateUI_LocationItem(final LocationItem locationItem,
                                      final TourLocation tourLocation) {

// SET_FORMATTING_OFF

      final double latitude                        = tourLocation.latitude;
      final double longitude                       = tourLocation.longitude;

      final double latitudeMin                     = tourLocation.latitudeMin;
      final double latitudeMax                     = tourLocation.latitudeMax;
      final double longitudeMin                    = tourLocation.longitudeMin;
      final double longitudeMax                    = tourLocation.longitudeMax;

      final int latitudeE6_Normalized              = tourLocation.latitudeE6_Normalized;
      final int longitudeE6_Normalized             = tourLocation.longitudeE6_Normalized;

      final int latitudeMinE6_Normalized           = tourLocation.latitudeMinE6_Normalized;
      final int latitudeMaxE6_Normalized           = tourLocation.latitudeMaxE6_Normalized;
      final int longitudeMinE6_Normalized          = tourLocation.longitudeMinE6_Normalized;
      final int longitudeMaxE6_Normalized          = tourLocation.longitudeMaxE6_Normalized;

      final int latitudeMinE6_Resized_Normalized   = tourLocation.latitudeMinE6_Resized_Normalized;
      final int latitudeMaxE6_Resized_Normalized   = tourLocation.latitudeMaxE6_Resized_Normalized;
      final int longitudeMinE6_Resized_Normalized  = tourLocation.longitudeMinE6_Resized_Normalized;
      final int longitudeMaxE6_Resized_Normalized  = tourLocation.longitudeMaxE6_Resized_Normalized;

      final double boundingBoxWidth                = tourLocation.boundingBoxWidth / UI.UNIT_VALUE_DISTANCE_SMALL;
      final double boundingBoxHeight               = tourLocation.boundingBoxHeight / UI.UNIT_VALUE_DISTANCE_SMALL;

// SET_FORMATTING_ON

      final int latitudeDiff_Normalized =

            latitudeE6_Normalized < latitudeMinE6_Normalized

                  ? latitudeE6_Normalized - latitudeMinE6_Normalized
                  : latitudeE6_Normalized > latitudeMaxE6_Normalized

                        ? latitudeE6_Normalized - latitudeMaxE6_Normalized
                        : 0;

      final int longitudeDiff_Normalized =

            longitudeE6_Normalized < longitudeMinE6_Normalized

                  ? longitudeE6_Normalized - longitudeMinE6_Normalized
                  : longitudeE6_Normalized > longitudeMaxE6_Normalized

                        ? longitudeE6_Normalized - longitudeMaxE6_Normalized
                        : 0;

      final int latitudeHeight_Normalized = latitudeMaxE6_Resized_Normalized - latitudeMinE6_Resized_Normalized;
      final int longitudeWidth_Normalized = longitudeMaxE6_Resized_Normalized - longitudeMinE6_Resized_Normalized;

      final double latitudeDiff_Distance = MtMath.distanceVincenty(

            latitude,
            longitude,

            latitude + (latitudeDiff_Normalized / 10e5),
            longitude

      ) / UI.UNIT_VALUE_DISTANCE_SMALL;

      final double longitudeDiff_Distance = MtMath.distanceVincenty(

            latitude,
            longitude,

            latitude,
            longitude + (longitudeDiff_Normalized / 10e5)

      ) / UI.UNIT_VALUE_DISTANCE_SMALL;

      // create formatted text
      final String latDiffText = latitudeDiff_Normalized == 0

            ? UI.EMPTY_STRING
            : latitudeDiff_Normalized < 0

                  ? UI.DASH + Integer.toString((int) (latitudeDiff_Distance + 0.5))
                  : Integer.toString((int) (latitudeDiff_Distance + 0.5));

      final String lonDiffText = longitudeDiff_Normalized == 0

            ? UI.EMPTY_STRING
            : longitudeDiff_Normalized < 0

                  ? UI.DASH + Integer.toString((int) (longitudeDiff_Distance + 0.5))
                  : Integer.toString((int) (longitudeDiff_Distance + 0.5));

      final boolean isBBoxResized = false

            || latitudeMin != tourLocation.latitudeMin_Resized
            || latitudeMax != tourLocation.latitudeMax_Resized

            || longitudeMin != tourLocation.longitudeMin_Resized
            || longitudeMax != tourLocation.longitudeMax_Resized;

      /*
       * Update model
       */
      locationItem.latitude_Text = _nf6.format(latitude);
      locationItem.longitude_Text = _nf6.format(longitude);

      locationItem.latitudeDiff_Value = latitudeDiff_Normalized;
      locationItem.longitudeDiff_Value = longitudeDiff_Normalized;

      locationItem.latitudeDiff_Text = latDiffText;
      locationItem.longitudeDiff_Text = lonDiffText;

      locationItem.boundingBoxHeight_Value = latitudeHeight_Normalized;
      locationItem.boundingBoxWidth_Value = longitudeWidth_Normalized;

      locationItem.boundingBoxHeight_Text = FormatManager.formatNumber_0(boundingBoxHeight);
      locationItem.boundingBoxWidth_Text = FormatManager.formatNumber_0(boundingBoxWidth);

      locationItem.isResizedBoundingBox = isBBoxResized;
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final Table table = _locationViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == LocationComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_SetViewerInput() {

      _isInUIUpdate = true;
      {
         _locationViewer.setInput(new Object[0]);
      }
      _isInUIUpdate = false;
   }
}
