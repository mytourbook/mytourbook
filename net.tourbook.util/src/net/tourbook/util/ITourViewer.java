/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
 * This interface provides acces to different parts in a viewer which displays tours
 * 
 * @author Wolfgang Schramm
 * @since version 1.2.0 / 2007-11-12
 */
public interface ITourViewer {

	/**
	 * @return Returns the {@link ColumnManager} for the tour viewer or <code>null</code> when a
	 *         column manager is not available
	 */
	ColumnManager getColumnManager();

	/**
	 * @return Returns the {@link ColumnViewer} or <code>null</code> when a viewer is not available
	 */
	ColumnViewer getViewer();

	/**
	 * Recreates the viewer after the columns or the measurement system has changed
	 * 
	 * @param columnViewer
	 *            Column viewer which is recreated
	 * @return Return the recreated column viewer
	 */
	ColumnViewer recreateViewer(ColumnViewer columnViewer);

	/**
	 * Reloads the viewer by setting the input, this handels structural changes
	 */
	void reloadViewer();

}
