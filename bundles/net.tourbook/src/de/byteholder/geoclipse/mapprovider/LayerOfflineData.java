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
package de.byteholder.geoclipse.mapprovider;

public class LayerOfflineData implements Cloneable {

	public String	name;
	public String	title;

	public boolean	isDisplayedInMap;
	public int		position;

	@Override
	protected Object clone() throws CloneNotSupportedException {

		final LayerOfflineData offlineData = (LayerOfflineData) super.clone();

		offlineData.name = name == null ? null : new String(name);
		offlineData.title = title == null ? null : new String(title);

		return offlineData;
	}

	@Override
	public String toString() {
 		return "p:" + position + " v:" + isDisplayedInMap + " " + title + "(" + name + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

}
