/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public final class ActionOpenPrefDialog extends Action {

	private String	fPrefPageId;

	public ActionOpenPrefDialog(final String text, final String prefPageId) {

		setText(text);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__options));

		fPrefPageId = prefPageId;
	}

	@Override
	public void run() {
		PreferencesUtil.createPreferenceDialogOn(//
		Display.getCurrent().getActiveShell(),
				fPrefPageId,
				null,
				null).open();
	}
}
