/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.IActionProvider;

import org.eclipse.jface.action.IMenuManager;

/**
 * This is just a placeholder for recent tag actions which are provided from an
 * {@link IActionProvider}
 */
public class ActionAddRecentTags implements IActionProvider {

   private IActionProvider _actionProvider;

   public ActionAddRecentTags(final IActionProvider actionProvider) {

      _actionProvider = actionProvider;
   }

   @Override
   public void fillActions(final IMenuManager menuMgr,
                           final ITourProvider tourProvider) {

      _actionProvider.fillActions(menuMgr, tourProvider);
   }
}
