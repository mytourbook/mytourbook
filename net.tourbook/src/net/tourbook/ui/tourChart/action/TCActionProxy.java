/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import org.eclipse.jface.action.Action;

/**
 * A tour chart can contain an internal action bar or can use the global tour actions. This proxy
 * keeps the state for a tour action.
 */
public class TCActionProxy {

	/**
	 * action for this command, when <code>null</code> an action handler is used
	 */
	private Action	_action;

	private String	_commandId;

	private boolean	_isEnabled	= true;
	private boolean	_isChecked;

	public TCActionProxy(final String commandId, final Action action) {

		_commandId = commandId;
		_action = action;
	}

	public Action getAction() {
		return _action;
	}

	public String getCommandId() {
		return _commandId;
	}

	public boolean isChecked() {
		return _isChecked;
	}

	public boolean isEnabled() {
		return _isEnabled;
	}

	/**
	 * Set check state in the proxy and action/handler,this does not update the UÎ when the handler
	 * is used. To update the UI, the method {@link ICommandService#refreshElements(*)} must be
	 * called.
	 * 
	 * @param isChecked
	 */
	public void setChecked(final boolean isChecked) {

		// keep check state for this action
		_isChecked = isChecked;

		if (_action != null) {
			_action.setChecked(isChecked);
		}

		final TCActionHandler actionHandler = TCActionHandlerManager.getInstance().getActionHandler(_commandId);

		if (actionHandler != null) {
			actionHandler.setChecked(isChecked);
		}
	}

	/**
	 * Set the enablement state in the proxy and action/handler, this does not update the UÎ when
	 * the handler is used. To update the UI, {@link TCActionHandler#fireHandlerChanged()} method
	 * must be called
	 * 
	 * @param isChecked
	 */
	public void setEnabled(final boolean isEnabled) {

		// keep enabled state for this action
		_isEnabled = isEnabled;

		if (_action != null) {
			_action.setEnabled(isEnabled);
		}

		final TCActionHandler actionHandler = TCActionHandlerManager.getInstance().getActionHandler(_commandId);

		if (actionHandler != null) {
			actionHandler.setEnabled(isEnabled);
		}
	}

}
