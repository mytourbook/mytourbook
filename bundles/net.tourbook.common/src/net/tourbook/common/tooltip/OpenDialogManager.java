/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.common.tooltip;

import java.util.HashMap;

public class OpenDialogManager {

	private HashMap<String, IOpeningDialog>	_openedDialogs	= new HashMap<>();

	/**
	 * Close all opened dialogs except the opening dialog.
	 * 
	 * @param openingDialog
	 */
	public void closeOpenedDialogs(final IOpeningDialog openingDialog) {

		final String openingDialogId = openingDialog.getDialogId();

		// keep reference of all opened dialogs
		_openedDialogs.put(openingDialogId, openingDialog);

		for (final String dialogId : _openedDialogs.keySet()) {

			if (dialogId.equals(openingDialogId)) {
				continue;
			}

			_openedDialogs.get(dialogId).hideDialog();
		}
	}

}
