/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import org.eclipse.swt.widgets.Composite;

public interface ISmoothingAlgorithm {

	public static final String	SMOOTHING_ALGORITHM_JAMET	= "jamet";		//$NON-NLS-1$
	public static final String	SMOOTHING_ALGORITHM_INITIAL	= "initial";	//$NON-NLS-1$

	/**
	 * @param smoothingUI
	 * @param parent
	 * @param isShowDescription
	 * @return
	 */
	Composite createUI(SmoothingUI smoothingUI, Composite parent, boolean isShowDescription);

	void dispose();

	void performDefaults(boolean isFireModifications);

	void updateUIFromPrefStore();

}
