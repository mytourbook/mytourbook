/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
	private Action	fAction;

	private String	fCommandId;

	private boolean	fIsEnabled	= true;
	private boolean	fIsChecked;

	public TCActionProxy(final String commandId, final Action action) {

		fCommandId = commandId;
		fAction = action;
	}

	public Action getAction() {
		return fAction;
	}

	public String getCommandId() {
		return fCommandId;
	}

	public boolean isChecked() {
		return fIsChecked;
	}

	public boolean isEnabled() {
		return fIsEnabled;
	}

	/**
	 * Set check state in the proxy and action/handler,this does not update the UÎ when the handler
	 * is used. To update the UI {@link ICommandService#refreshElements(*)} method must be called
	 * 
	 * @param isChecked
	 */
	public void setChecked(final boolean isChecked) {

		// keep check state for this action 
		fIsChecked = isChecked;

		if (fAction != null) {
			fAction.setChecked(isChecked);
		}

		final TCActionHandler actionHandler = TCActionHandlerManager.getInstance().getActionHandler(fCommandId);

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
		fIsEnabled = isEnabled;

		if (fAction != null) {
			fAction.setEnabled(isEnabled);
		}

		final TCActionHandler actionHandler = TCActionHandlerManager.getInstance().getActionHandler(fCommandId);

		if (actionHandler != null) {
			actionHandler.setEnabled(isEnabled);
		}
	}

}
