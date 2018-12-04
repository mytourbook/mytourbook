/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import org.eclipse.jface.action.Action;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

public class ActionExternalSearchUI_2 extends Action {

   private SearchView_2 _searchView_2;

   public ActionExternalSearchUI_2(final SearchView_2 searchView_2) {

		super(Messages.Search_View_Action_ExternalSearchUI, AS_CHECK_BOX);

		setToolTipText(Messages.Search_View_Action_ExternalSearchUI_Tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SearchExternal));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SearchExternal_Disabled));

      _searchView_2 = searchView_2;
	}

	@Override
	public void run() {
      _searchView_2.actionSearchUI();
	}
}
