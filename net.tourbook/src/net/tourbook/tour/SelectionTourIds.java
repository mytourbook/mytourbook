/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelection;

/**
 * selection contains multiple tour ids
 */
public class SelectionTourIds implements ISelection {

	private ArrayList<Long>	_tourIds;

	public SelectionTourIds(final ArrayList<Long> tourIds) {
		_tourIds = tourIds;
	}

	public ArrayList<Long> getTourIds() {
		return _tourIds;
	}

	public boolean isEmpty() {
		return false;
	}

}
