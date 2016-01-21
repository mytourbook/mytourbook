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
package net.tourbook.ui.views.tourCatalog;

public class TVIWizardCompareTour extends TVIWizardCompareItem {

	long	tourId;

	int		tourYear;
	int		tourMonth;
	int		tourDay;

	long	colDistance;
	long	colRecordingTime;
	long	colAltitudeUp;
	long	tourTypeId;

	public TVIWizardCompareTour(final TVIWizardCompareItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {}

	/**
	 * tour items do not have children
	 */
	@Override
	public boolean hasChildren() {
		return false;
	}
}
