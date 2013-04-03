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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class ToolTip3Tool {

	private Shell			_ttShell;

	/**
	 * When <code>null</code> this is a default tool otherwise it is a flexible tool.
	 */
	private FlexTool		_flexTool;

	private Object			_toolTipArea;

	/**
	 * State if a tool is visible or not. When a tool is hidden, the default tooltip is displayed
	 * and not the tool specific tooltip.
	 */
	private boolean			_isVisible;

	private IToolProvider	_toolProvider;

	/**
	 * Default location when tooltip area is hovered.
	 */
	private Point			_defaultLocation;

	/**
	 * Initial location where the tooltip should be displayed.
	 */
	private Point			_initialLocation;

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

	Point getDefaultLocation() {
		return _defaultLocation;
	}

	Point getInitialLocation() {

		// get location
		final Point location = _initialLocation;

		// reset location that location is returned only once
		_initialLocation = null;

		_toolProvider.resetInitialLocation();

		if (location != null) {

			if (_flexTool != null) {

				/*
				 * set moved because the _initialLocation is used to display the tooltip which is
				 * only available when the tooltip was moved previously
				 */
				_flexTool.setMoved();
			}
		}

		return location;
	}

	Shell getShell() {
		return _ttShell;
	}

	IToolProvider getToolProvider() {
		return _toolProvider;
	}

	Object getToolTipArea() {
		return _toolTipArea;
	}

	/**
	 * @return Returns <code>true</code> when this is a flexible (movable) tool.
	 */
	boolean isFlexTool() {
		return _flexTool != null;
	}

	/**
	 * @return Returns <code>true</code> when this tool is moved.
	 */
	public boolean isMoved() {

		if (_flexTool == null) {
			return false;
		}

		return _flexTool.isMoved();
	}

	boolean isVisible() {
		return _isVisible;
	}

	void setDefaultLocation(final Point defaultLocation) {
		_defaultLocation = defaultLocation;
	}

	void setFlexable(final FlexTool flexTool) {

		_flexTool = flexTool;

		// flex tools are displayed when created
		_isVisible = true;
	}

	void setInitialLocation(final Point initialLocation) {
		_initialLocation = initialLocation;
	}

	void setToolProvider(final IToolProvider toolProvider) {
		_toolProvider = toolProvider;
	}

	/**
	 * Set state if this tool is visible or not.
	 * 
	 * @param isVisible
	 */
	void setToolVisibility(final boolean isVisible) {

//		System.out.println(UI.timeStampNano() + " _isVisible=" + _isVisible + "\t" + isVisible);
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		if (isVisible == false) {
//			int a = 0;
//			a++;
//		}

		_isVisible = isVisible;
	}

	@Override
	public String toString() {
		return String
				.format(
						"\nToolTip3Tool\n   _toolTipArea=%s, \n   _toolProvider=%s, \n   isFlexTool()=%s, \n   isMoved()=%s, \n   isVisible()=%s\n",
						_toolTipArea,
						_toolProvider,
						isFlexTool(),
						isMoved(),
						isVisible());
	}

}
