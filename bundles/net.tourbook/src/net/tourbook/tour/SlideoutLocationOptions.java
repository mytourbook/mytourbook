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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DLItem;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DualList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor options.
 */
public class SlideoutLocationOptions extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final IDialogSettings _state = TourbookPlugin.getState(TourDataEditorView.ID);

   private TourData                     _tourData;
   private boolean                      _isStartLocation;

   private DialogQuickEdit              _dialogQuickEdit;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private MT_DualList _addressItemList;

   public SlideoutLocationOptions(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final DialogQuickEdit dialogQuickEdit,
                                  final boolean isStartLocation,
                                  final TourData tourData) {

      super(ownerControl, toolBar);

      _dialogQuickEdit = dialogQuickEdit;
      _isStartLocation = isStartLocation;
      _tourData = tourData;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      fillUI();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {

            _addressItemList = new MT_DualList(container, SWT.NONE);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(800, 400)
                  .applyTo(_addressItemList);

         }
      }

      return _shellContainer;
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

      _addressItemList.setItems(allItems);
   }

   @Override
   public void resetToDefaults() {

   }

   private void restoreState() {

   }

}
