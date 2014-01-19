/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common.action;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public final class ActionOpenPrefDialog extends Action {

	private String	_prefPageId;

	private Object	_data;

	/**
	 * @param text
	 * @param prefPageId
	 */
	public ActionOpenPrefDialog(final String text, final String prefPageId) {

		setText(text);
		setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__options));

		_prefPageId = prefPageId;
	}

	/**
	 * @param text
	 * @param prefPageId
	 * @param data
	 */
	public ActionOpenPrefDialog(final String text, final String prefPageId, final Object data) {

		this(text, prefPageId);

		_data = data;
	}

	@Override
	public void run() {

		PreferencesUtil.createPreferenceDialogOn(//
				Display.getCurrent().getActiveShell(),
				_prefPageId,
				null,
				_data).open();
	}

	public void setData(final Object data) {

		_data = data;
	}
}
