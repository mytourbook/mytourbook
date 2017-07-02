/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.preferences.PrefPageMap25Provider;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ActionManageMap25Providers extends Action {

	public ActionManageMap25Providers() {

		super(Messages.Map_Action_ManageMapProviders, AS_PUSH_BUTTON);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__options));
	}

	@Override
	public void run() {

		PreferencesUtil
				.createPreferenceDialogOn(
						Display.getCurrent().getActiveShell(),
						PrefPageMap25Provider.ID,
						null,
						null)
				.open();
	}

}
