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
package de.byteholder.geoclipse.tileinfo;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import de.byteholder.geoclipse.Messages;

public class TileInfoControl extends CLabel {

	private static final String	INFO_PATTERN	= "Q:000  T:000.000.000  P:000.000.000  S:00.00 xxxxx"; //$NON-NLS-1$

	public TileInfoControl(final Composite parent, final int style) {

		super(parent, style);

		setToolTipText(Messages.tileInfo_control_tooltip_title
				+ Messages.tileInfo_control_tooltip_line1
				+ Messages.tileInfo_control_tooltip_line2
				+ Messages.tileInfo_control_tooltip_line3
				+ Messages.tileInfo_control_tooltip_line4//
				+ Messages.tileInfo_control_tooltip_line5//
		);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {

		final GC gc = new GC(this);
		final Point p = gc.textExtent(INFO_PATTERN);
		gc.dispose();

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

		setText(NLS.bind(Messages.tileInfo_control_statisticsAll, new Object[] {
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
