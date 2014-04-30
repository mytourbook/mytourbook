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
package net.tourbook.sign;

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;

/**
 * Contains all signs and sign categories for a sign category.
 */
public class SignCollection {

	public ArrayList<TourSignCategory>	tourSignCategories;
	public ArrayList<TourSign>			tourSigns;

	public SignCollection() {}

	public SignCollection(final ArrayList<TourSign> sortedSigns) {
		tourSigns = sortedSigns;
	}

	public SignCollection(final Set<TourSign> tourSignsInOneTour) {
		tourSigns = new ArrayList<TourSign>(tourSignsInOneTour);
	}

}
