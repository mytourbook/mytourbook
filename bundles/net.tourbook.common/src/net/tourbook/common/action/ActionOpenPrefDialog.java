/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.common.tooltip.AnimatedToolTipShell;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ActionOpenPrefDialog extends Action {

	private String					_prefPageId;
	private Object					_prefDialogData;

	private Shell					_shell	= Display.getCurrent().getActiveShell();

	private AnimatedToolTipShell	_openedTooltip;

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

		_prefDialogData = data;
	}

	@Override
	public void run() {

		// hide other opened dialog
		if (_openedTooltip != null) {
			_openedTooltip.hideNow();
		}

		PreferencesUtil.createPreferenceDialogOn(//
				_shell,
				_prefPageId,
				null,
				_prefDialogData).open();
	}

	/**
	 * This tooltip will be closed when the pref dialog is opened.
	 * 
	 * @param openedTooltip
	 */
	public void setCloseThisTooltip(final AnimatedToolTipShell openedTooltip) {

		_openedTooltip = openedTooltip;
	}

	public void setPrefData(final Object data) {

		_prefDialogData = data;
	}

	/**
	 * Set shell to the parent otherwise the pref dialog is closed when the slideout is closed. Is a
	 * bit tricky :-)
	 */
	public void setShell(final Shell shell) {

		_shell = shell;
	}
}
