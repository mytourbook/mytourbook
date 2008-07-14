/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TreeViewer;

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
	 * @return Returns the {@link TreeViewer} or <code>null</code> when a tree viewer is not
	 *         available
	 */
	ColumnViewer getViewer();

	/**
	 * Recreates the viewer after the columns or the measurement system has changed
	 */
	void recreateViewer();

	/**
	 * Reloads the viewer completely
	 */
	void reloadViewer();

}
