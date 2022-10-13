/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public abstract class UITest {

   protected SWTWorkbenchBot bot                                     = new SWTWorkbenchBot();

   protected int             tourBookView_Distance_Column_Index      = 10;
   protected int             tourBookView_ElevationGain_Column_Index = 11;
   protected int             tourBookView_Temperature_Column_Index   = 3;
}
