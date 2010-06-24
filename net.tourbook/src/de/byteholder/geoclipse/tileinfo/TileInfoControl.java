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
package de.byteholder.geoclipse.tileinfo;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import de.byteholder.geoclipse.Messages;
 
public class TileInfoControl extends CLabel {

	private static final int	CLABEL_INDENT	= 3;

	public TileInfoControl(final Composite parent, final int style) {

		super(parent, style);

		setToolTipText(Messages.TileInfo_Control_Tooltip_Title
				+ Messages.TileInfo_Control_Tooltip_Line1
				+ Messages.TileInfo_Control_Tooltip_Line2
				+ Messages.TileInfo_Control_Tooltip_Line3
				+ Messages.TileInfo_Control_Tooltip_Line4//
				+ Messages.TileInfo_Control_Tooltip_Line5//
		);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {

		final GC gc = new GC(this);
		final Point p = gc.textExtent(Messages.TileInfo_Control_Pattern);
		gc.dispose();

		if (wHint == SWT.DEFAULT) {
			p.x += 2 * CLABEL_INDENT;
		} else {
			p.x = wHint;
		}
		if (hHint == SWT.DEFAULT) {
			p.y += 2 * CLABEL_INDENT;
		} else {
			p.y = hHint;
		}
		return p;

	}

	public void updateInfo(final String text) {

		if (isDisposed()) {
			return;
		}

		setText(text);

		/*
		 * force redraw NOW this is necessary when the UI thread will be blocked later, like when
		 * downloading srtm data
		 */
		update();
	}

	public void updateInfo(	final String queue,
							final String errorLoading,
							final String endLoading,
							final String startLoading,
							final String errorPainting,
							final String endPainting,
							final String startPainting,
							final String endSRTMData,
							final String startSRTMData,
							final String errorSRTM) {

		if (isDisposed()) {
			return;
		}

		setText(NLS.bind(Messages.TileInfo_Control_Tooltip_StatisticsAll, new Object[] {
				queue,
				endLoading,
				startLoading,
				errorLoading,
				endPainting,
				startPainting,
				errorPainting,
				endSRTMData,
				errorSRTM }));
	}
}
