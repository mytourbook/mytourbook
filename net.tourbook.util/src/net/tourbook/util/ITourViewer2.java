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
package net.tourbook.util;

import org.eclipse.jface.viewers.ColumnViewer;

/**
 * @since version 11.8.2
 */
public interface ITourViewer2 extends ITourViewer {

	/**
	 * @param columnViewer
	 * @return Returns <code>true</code> when the first column should be visible, otherwise the
	 *         first column width is set to 0.
	 */
	public boolean isColumn0Visible(ColumnViewer columnViewer);

}
