/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class MapInfoContribution extends WorkbenchWindowControlContribution {

	private MapInfoManager	_mapInfoManager;
	private MapInfoControl	_infoWidget;

	@Override
	protected Control createControl(final Composite parent) {

		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=471313
		parent.getParent().setRedraw(true);

		if (_mapInfoManager == null) {
			_mapInfoManager = MapInfoManager.getInstance();
		}

		_infoWidget = new MapInfoControl(parent, getOrientation());

		updateUI();

		return _infoWidget;
	}

	@Override
	public boolean isDynamic() {

		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=471313
		return true;
	}

	private void updateUI() {
		_mapInfoManager.setInfoWidget(_infoWidget);
	}

}

