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

public class TVIMapProvider extends TVIMapProviderItem {

	private MPWrapper	_mpWrapper;

	public TVIMapProvider(final TreeViewer treeViewer, final MPWrapper mpWrapper) {

		super(treeViewer);

		_mpWrapper = mpWrapper;
	}

	@Override
	protected void fetchChildren() {
 
		// only WMS has children
		if ((_mpWrapper.getMP() instanceof MPWms) == false) {
			return;
		}

		// check if wms is loaded
		final MPWms mpWms = (MPWms) _mpWrapper.getMP();
 
		if (MapProviderManager.checkWms(mpWms, null) == null) {
			return;
		}

		// create children, wms layer
		for (final MtLayer mtLayer : mpWms.getMtLayers()) {
			addChild(new TVIWmsLayer(getTreeViewer(), mtLayer));
		}
	}

	public MPWrapper getMapProviderWrapper() {
		return _mpWrapper;
	}

	@Override
	public boolean hasChildren() {

		if (_mpWrapper.getMP() instanceof MPWms) {
			// wms has children
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void remove() {}

}
