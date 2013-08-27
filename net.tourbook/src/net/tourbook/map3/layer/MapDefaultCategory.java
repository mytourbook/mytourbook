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
package net.tourbook.map3.layer;

import java.util.HashMap;

import net.tourbook.map3.Messages;

/**
 * Contains default layers.
 */
public class MapDefaultCategory {

	/*
	 * default category id's
	 */
	public static final String						ID_INFO				= "Info";									//$NON-NLS-1$
	public static final String						ID_MAP				= "Map";									//$NON-NLS-1$
	public static final String						ID_TOOL				= "Tool";									//$NON-NLS-1$
	public static final String						ID_TOUR				= "Tour";									//$NON-NLS-1$

	/**
	 * Contains all default categories, key is category id.
	 */
	private static HashMap<String, DefaultCategory>	_defaultCategory	= new HashMap<String, DefaultCategory>();

	static {

		_defaultCategory.put(ID_INFO, new DefaultCategory(ID_INFO, Messages.Layer_Category_Info));
		_defaultCategory.put(ID_MAP, new DefaultCategory(ID_MAP, Messages.Layer_Category_Map));
		_defaultCategory.put(ID_TOOL, new DefaultCategory(ID_TOOL, Messages.Layer_Category_Tools));
		_defaultCategory.put(ID_TOUR, new DefaultCategory(ID_TOUR, Messages.Layer_Category_Tour));
	}

	/**
	 * @param defaultCategoryId
	 * @return Returns <code>null</code> when category is not found.
	 */
	public static DefaultCategory getLayer(final String defaultCategoryId) {
		return _defaultCategory.get(defaultCategoryId);
	}

}
