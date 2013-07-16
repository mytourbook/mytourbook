/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import net.tourbook.map2.Messages;

import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public class MapInfoControl extends CLabel {

	public MapInfoControl(final Composite parent, final int style) {
		super(parent, style);
		setToolTipText(Messages.statusLine_mapInfo_tooltip);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
 
		final GC gc = new GC(this);
		final Point p = gc.textExtent(Messages.statusLine_mapInfo_pattern);
		gc.dispose();

		return p;
	}
}
