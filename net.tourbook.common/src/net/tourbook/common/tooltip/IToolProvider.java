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

import org.eclipse.swt.widgets.Composite;

/**
 * Provides the UI for a {@link MovableTool}.
 */
public interface IToolProvider {

	/**
	 * Creates the UI for this tool.
	 * 
	 * @param parent
	 */
	void createToolUI(final Composite parent);

	/**
	 * @return Returns a title which is displayed as tooltip when the tooltip header in a movable
	 *         tooltip is hovered.
	 */
	String getToolTitle();

	/**
	 * @return Returns <code>true</code> when the tooltip can be moved.
	 */
	boolean isToolMovable();

}
