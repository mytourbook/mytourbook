/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.viewers.TreeViewer;

public class TVIWmsLayer extends TVIMapProviderItem {

	private MtLayer	_mtLayer;

	public TVIWmsLayer(final TreeViewer treeViewer, final MtLayer mtLayer) {

		super(treeViewer);

		_mtLayer = mtLayer;
	}

	@Override
	protected void fetchChildren() {}

	public MtLayer getMtLayer() {
		return _mtLayer;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected void remove() {}

}
