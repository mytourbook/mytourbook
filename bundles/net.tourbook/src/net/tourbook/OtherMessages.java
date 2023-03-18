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
package net.tourbook;

/**
 * Contains links to Messages which are not in {@link Messages}.
 * <p>
 * This is needed because the externalize string tool do not support different Messages
 * classes in one Java file
 * <p>
 * After many years I found this solution which has the advantage that the 2nd Messages in one Java
 * file must not set into comments when using the externalized string tool
 */
public class OtherMessages {

   public static final String COLUMN_FACTORY_CATEGORY_MARKER              = net.tourbook.ui.Messages.ColumnFactory_Category_Marker;
   public static final String COLUMN_FACTORY_GEAR_REAR_SHIFT_COUNT_LABEL  = net.tourbook.ui.Messages.ColumnFactory_GearRearShiftCount_Label;
   public static final String COLUMN_FACTORY_GEAR_FRONT_SHIFT_COUNT_LABEL = net.tourbook.ui.Messages.ColumnFactory_GearFrontShiftCount_Label;
   public static final String COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP       = net.tourbook.ui.Messages.ColumnFactory_TimeZoneDifference_Tooltip;

   public static final String VALUE_UNIT_CADENCE                          = net.tourbook.ui.Messages.Value_Unit_Cadence;
   public static final String VALUE_UNIT_CADENCE_SPM                      = net.tourbook.ui.Messages.Value_Unit_Cadence_Spm;
   public static final String VALUE_UNIT_K_CALORIES                       = net.tourbook.ui.Messages.Value_Unit_KCalories;
   public static final String VALUE_UNIT_PULSE                            = net.tourbook.ui.Messages.Value_Unit_Pulse;

}
