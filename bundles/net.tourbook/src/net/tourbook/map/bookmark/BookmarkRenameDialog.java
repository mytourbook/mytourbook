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
package net.tourbook.map.bookmark;

import net.tourbook.Messages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class BookmarkRenameDialog extends InputDialog {

	private boolean _isOpenedWithMouse;

	public BookmarkRenameDialog(final Shell parentShell,
								final String dialogTitle,
								final String dialogMessage,
								final String initialValue,
								final boolean isOpenedWithMouse,
								final IInputValidator validator) {

		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);

		_isOpenedWithMouse = isOpenedWithMouse;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.Map_Bookmark_Button_Rename);
	}

	@Override
	protected Point getInitialLocation(final Point initialSize) {

		try {

			Point cursorLocation = Display.getCurrent().getCursorLocation();

			// center below the cursor location
			cursorLocation.x -= initialSize.x / 2;
			cursorLocation.y += 50;

			if (_isOpenedWithMouse) {

				// opened with mouse, use default

			} else {

				// opened with keyboard

				final Control focusControl = getShell().getDisplay().getFocusControl();

				if (focusControl != null) {
					cursorLocation = focusControl.toDisplay(0, 0);
				}
			}

			return cursorLocation;

		} catch (final NumberFormatException ex) {

			return super.getInitialLocation(initialSize);
		}
	}

}
