/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.ui.views.rawData;

import java.util.List;

import net.tourbook.importdata.RawDataManager.TourValueType;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;

public class DialogUtils {

   static final Color LOCK_COLOR_DARK  = new Color(0x0, 0xff, 0x0);
   static final Color LOCK_COLOR_LIGHT = new Color(0x0, 0xb0, 0x0);

   public static void addTourValueTypeFromCheckbox(final Button checkButton,
                                                   final TourValueType tourValueType,
                                                   final List<TourValueType> tourValueTypes) {

      if (checkButton.getSelection()) {
         tourValueTypes.add(tourValueType);
      }
   }
}
