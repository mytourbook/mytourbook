/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;

public abstract class TVIWizardCompareItem extends TreeViewerItem {

   static ZonedDateTime monthDateTime = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

   String               treeColumn;

   boolean              isUseAppFilter;
}
