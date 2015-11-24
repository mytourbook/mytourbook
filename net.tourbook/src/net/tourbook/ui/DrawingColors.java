/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import org.eclipse.swt.graphics.Color;

public class DrawingColors {

	/**
	 * when <code>true</code> the colors must be disposed, when <code>false</code> the colors must
	 * not be disposed
	 * <p>
	 * default is <code>true</code>
	 */
	public boolean	mustBeDisposed	= true;

	public Color	colorBright;
	public Color	colorDark;
	public Color	colorLine;

	public void dispose() {

		if (mustBeDisposed) {
			colorBright.dispose();
			colorDark.dispose();
			colorLine.dispose();
		}
	}

	@Override
	public String toString() {
		return "DrawingColors [colorBright="
				+ colorBright
				+ ", colorDark="
				+ colorDark
				+ ", colorLine="
				+ colorLine
				+ "]";
	}

}
