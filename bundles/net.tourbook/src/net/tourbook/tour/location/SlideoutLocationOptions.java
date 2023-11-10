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
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.tour.DialogQuickEdit;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DLItem;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DualList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for the start/end location
 */
public class SlideoutLocationOptions extends AdvancedSlideout {

   private static final String OSM_FIELD_NAME = "name";         //$NON-NLS-1$

   private static final String LOCATION_KEY   = "LOCATION_KEY"; //$NON-NLS-1$

   private ToolItem            _toolItem;

   private PixelConverter      _pc;

   private TourData            _tourData;
   private boolean             _isStartLocation;

   private DialogQuickEdit     _dialogQuickEdit;

   /*
    * UI controls
    */

   private MT_DualList _listLocationParts;

   private Text        _txtDefaultName;
   private Text        _txtSelectedLocationNames;

   public SlideoutLocationOptions(final ToolItem toolItem,
                                  final IDialogSettings state,
                                  final DialogQuickEdit dialogQuickEdit,
                                  final boolean isStartLocation,
                                  final TourData tourData) {

      super(toolItem.getParent(), state, new int[] { 800, 800 });

      _toolItem = toolItem;

      _dialogQuickEdit = dialogQuickEdit;
      _isStartLocation = isStartLocation;
      _tourData = tourData;

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      final String title = _isStartLocation
            ? Messages.Slideout_LocationOptions_Label_StartLocation_Title
            : Messages.Slideout_LocationOptions_Label_EndLocation_Title;

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
                        LOCATION_KEY,
                        LocationPart.valueOf(fieldName));

                  allItems.add(dlItem);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void addCustomPart(final LocationPart locationPart,
                              final String partValue,
                              final List<MT_DLItem> allParts) {

      if (partValue != null) {

         final String partName = "* " + locationPart.name();

         allParts.add(new MT_DLItem(

               partValue,
               partName,
               LOCATION_KEY,
               locationPart));
      }
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      restoreState();

      updateUI_Initial();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_RED);
      {
         {
            // default location name

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_LocationOptions_Label_DefaultLocationName);

            _txtDefaultName = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_txtDefaultName);
         }
         {
            // selected location parts

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_LocationOptions_Label_SelectedLocationParts);

            _txtSelectedLocationNames = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_txtSelectedLocationNames);
         }
         {
            // dual list with location parts

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_LocationOptions_Label_LocationParts);

            _listLocationParts = new MT_DualList(container, SWT.NONE);
            _listLocationParts.addSelectionChangeListener(selectionChangeListener -> onChangeLocationPart());
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .applyTo(_listLocationParts);
         }
      }
   }

   private void enableActions() {

   }

   private String getFormattedPartName(final LocationPart locationPart) {

      return "* " + locationPart.name();
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

      _pc = new PixelConverter(parent);
   }

   private void onChangeLocationPart() {

      // get selected part IDs
      final List<MT_DLItem> allSelectedItems = _listLocationParts.getSelectionAsList();

      final String locationDisplayName = TourLocationManager.createLocationDisplayName(allSelectedItems);

      _txtSelectedLocationNames.setText(locationDisplayName);
   }

   @Override
   protected void onDispose() {

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   private void restoreState() {

      enableActions();
   }

   @Override
   protected void saveState() {

      // save slideout position/size
      super.saveState();
   }

   private void updateUI_Initial() {

      final OSMLocation osmLocation = getOsmLocation();
      final OSMAddress address = osmLocation.address;

      // show "display_name" as default name
      _txtDefaultName.setText(osmLocation.display_name);

      /*
       * Fill address part widget
       */
      final List<MT_DLItem> allParts = new ArrayList<>();

// SET_FORMATTING_OFF

      // add customized parts
      final String smallestCity           = TourLocationManager.getCustom_City_Smallest(address);
      final String smallestCityWithZip    = TourLocationManager.getCustom_CityWithZip_Smallest(address);
      final String largestCity            = TourLocationManager.getCustom_City_Largest(address);
      final String largestCityWithZip     = TourLocationManager.getCustom_CityWithZip_Largest(address);
      final String streetWithHouseNumber  = TourLocationManager.getCustom_Street(address);

// SET_FORMATTING_ON

      boolean isShowSmallestCity = false;
      if (largestCity != null && largestCity.equals(smallestCity) == false) {
         isShowSmallestCity = true;
      }

      addCustomPart(LocationPart.CUSTOM_CITY_LARGEST, largestCity, allParts);
      if (isShowSmallestCity) {
         addCustomPart(LocationPart.CUSTOM_CITY_SMALLEST, smallestCity, allParts);
      }

      addCustomPart(LocationPart.CUSTOM_CITY_WITH_ZIP_LARGEST, largestCityWithZip, allParts);
      if (isShowSmallestCity) {
         addCustomPart(LocationPart.CUSTOM_CITY_WITH_ZIP_SMALLEST, smallestCityWithZip, allParts);
      }

      addCustomPart(LocationPart.CUSTOM_STREET_WITH_HOUSE_NUMBER, streetWithHouseNumber, allParts);

      // add "name" when available
      final String locationName = osmLocation.name;
      if (StringUtils.hasContent(locationName)) {

         allParts.add(new MT_DLItem(

               locationName,
               getFormattedPartName(LocationPart.OSM_NAME),
               LOCATION_KEY,
               LocationPart.OSM_NAME));
      }

      // add address parts
      addAllAddressParts(address, allParts);

      _listLocationParts.setItems(allParts);
   }

}
