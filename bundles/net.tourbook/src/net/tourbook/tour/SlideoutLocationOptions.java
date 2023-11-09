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
package net.tourbook.tour;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;

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
 * Slideout for the easy import device state
 */
public class SlideoutLocationOptions extends AdvancedSlideout {

   private ToolItem        _toolItem;

   private PixelConverter  _pc;

   private TourData        _tourData;
   private boolean         _isStartLocation;

   private DialogQuickEdit _dialogQuickEdit;

   /*
    * UI controls
    */

   private MT_DualList _placeItemList;

   private Text        _txtCombinedLocation;

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

      final String title = isStartLocation
            ? Messages.Slideout_LocationOptions_Label_StartLocation_Title
            : Messages.Slideout_LocationOptions_Label_EndLocation_Title;

      setTitleText(title);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      restoreState();

      fillUI();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_RED);
      {
         {
            // Joined location parts

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_LocationOptions_Label_JoinedLocationParts);

            _txtCombinedLocation = new Text(container, SWT.READ_ONLY);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_txtCombinedLocation);
         }
         {
            // dual list with address parts

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_LocationOptions_Label_LocationParts);

            _placeItemList = new MT_DualList(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .applyTo(_placeItemList);
//            _placeItemList.setBackground(UI.SYS_COLOR_CYAN);
         }
      }
   }

   private void enableActions() {

   }

   private void fillUI() {
      // TODO Auto-generated method stub

      final List<MT_DLItem> allItems = new ArrayList<>();

      final TourLocationData locationData = _isStartLocation
            ? _tourData.osmLocation_Start
            : _tourData.osmLocation_End;

      final OSMLocation osmLocation = locationData.osmLocation;

      try {

         final OSMAddress address = osmLocation.address;

         final Field[] allAddressFields = address.getClass().getFields();

         for (final Field field : allAddressFields) {

            // skip names which are not needed
            if ("ISO3166_2_lvl4".equals(field.getName())) { //$NON-NLS-1$
               continue;
            }

            final Object fieldValue = field.get(address);

            if (fieldValue instanceof final String stringValue) {

               // log only fields with value
               if (stringValue.length() > 0) {

                  final MT_DLItem dlItem = new MT_DLItem(stringValue);

                  dlItem.setText2(field.getName());

                  allItems.add(dlItem);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.log(e);
      }

      _placeItemList.setItems(allItems);
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

}
