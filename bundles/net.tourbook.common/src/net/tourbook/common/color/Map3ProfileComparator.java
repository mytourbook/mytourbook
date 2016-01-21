/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class Map3ProfileComparator extends ViewerComparator {

	@Override
	public int compare(final Viewer viewer, final Object c1, final Object c2) {

		if (c1 instanceof Map3GradientColorProvider && c2 instanceof Map3GradientColorProvider) {

			// compare color profiles by name

			final Map3GradientColorProvider cp1 = (Map3GradientColorProvider) c1;
			final Map3GradientColorProvider cp2 = (Map3GradientColorProvider) c2;

			return cp1.getMap3ColorProfile().getProfileName().compareTo(cp2.getMap3ColorProfile().getProfileName());
		}

		return 0;
	}
}