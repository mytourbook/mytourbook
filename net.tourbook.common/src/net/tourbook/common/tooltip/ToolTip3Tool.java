/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import org.eclipse.swt.widgets.Shell;

public class ToolTip3Tool {

	private Shell		_ttShell;

	/**
	 * When not <code>null</code> the tooltip is movable.
	 */
	private MovableTool	_movableTools;

	private Object		_toolTipArea;

	/**
	 * @param toolTipArea
	 * @param ttShell
	 *            Tooltip shell
	 */
	ToolTip3Tool(final Shell shell, final Object toolTipArea) {

		_ttShell = shell;
		_toolTipArea = toolTipArea;
	}

	/**
	 * @return Returns shell for this tool or <code>null</code> when shell is not set or disposed.
	 */
	Shell getCheckedShell() {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return null;
		} else {
			return _ttShell;
		}
	}

	Shell getShell() {
		return _ttShell;
	}

	Object getToolTipArea() {
		return _toolTipArea;
	}

	/**
	 * @return Returns <code>true</code> when this is a movable tool.
	 */
	boolean isMovable() {
		return _movableTools != null;
	}

	/**
	 * @return Returns <code>true</code> when this tool is moved.
	 */
	public boolean isMoved() {

		if (_movableTools == null) {
			return false;
		}

		return _movableTools.isMoved();
	}

	void setMovable(final MovableTool movableTool) {
		_movableTools = movableTool;
	}

}
