/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;

/**
 * This was a copy of EclipseSplashHandler Parses the well known product constants and constructs a
 * splash handler accordingly.
 */
public class MyTourbookSplashHandler extends BasicSplashHandler {

	public static final String	APP_BUILD_ID_VERSION	= "1.7.0";								//$NON-NLS-1$
	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2008-11";	//$NON-NLS-1$

//	public static final String	APP_BUILD_ID_VERSION	= "1.6.1";								//$NON-NLS-1$
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2008-09";	//$NON-NLS-1$

//	public static final String	APP_BUILD_ID_VERSION	= "1.6.0";								//$NON-NLS-1$
//	public static final String	APP_BUILD_ID			= APP_BUILD_ID_VERSION + ".v2008-08";	//$NON-NLS-1$
//	public static final String	APP_BUILD_ID	= "1.5.0.v20080509";	//$NON-NLS-1$
//	public static final String	APP_BUILD_ID	= "1.4.0.v20080312";	//$NON-NLS-1$
//	public static final String	APP_BUILD_ID	= "1.3.0.v20080125";	//$NON-NLS-1$
//	public static final String	APP_BUILD_ID	= "1.2.0.v20071231";	//$NON-NLS-1$
//	public static final String	APP_BUILD_ID	= "1.1.0.v20071107";	//$NON-NLS-1$

	@Override
	public void init(final Shell splash) {

		super.init(splash);

		// keep the splash handler to be used outside of this splash handlers
		TourbookPlugin.getDefault().setSplashHandler(this);

		String progressRectString = null;
		String messageRectString = null;
		String foregroundColorString = null;

		final IProduct product = Platform.getProduct();
		if (product != null) {
			progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
			messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
			foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
		}

		// set progressbar position
		Rectangle progressRect = parseRect(progressRectString);
		if (progressRect == null) {
			progressRect = new Rectangle(10, 0, 300, 15);
		}
		setProgressRect(progressRect);

		// set message position
		Rectangle messageRect = parseRect(messageRectString);
		if (messageRect == null) {
			messageRect = new Rectangle(10, 25, 300, 15);
		}
		setMessageRect(messageRect);

		// set message color
		int foregroundColorInteger;
		try {
			foregroundColorInteger = Integer.parseInt(foregroundColorString, 16);
		} catch (final Exception ex) {
			foregroundColorInteger = 0xD2D7FF; // off white
		}

		setForeground(new RGB((foregroundColorInteger & 0xFF0000) >> 16,
				(foregroundColorInteger & 0xFF00) >> 8,
				foregroundColorInteger & 0xFF));

//		final String buildId = "Version " + System.getProperty("eclipse.buildId", "Unknown Version"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		getContent().addPaintListener(new PaintListener() {

			public void paintControl(final PaintEvent e) {
				final GC gc = e.gc;
				final String version = "Version " + APP_BUILD_ID; //$NON-NLS-1$
				final Point extend = gc.textExtent(version);

				gc.setForeground(getForeground());
				gc.drawText(version, 383 - extend.x, 57, true);
			}
		});
	}

	private Rectangle parseRect(final String string) {
		if (string == null) {
			return null;
		}
		int x, y, w, h;
		int lastPos = 0;
		try {
			int i = string.indexOf(',', lastPos);
			x = Integer.parseInt(string.substring(lastPos, i));
			lastPos = i + 1;
			i = string.indexOf(',', lastPos);
			y = Integer.parseInt(string.substring(lastPos, i));
			lastPos = i + 1;
			i = string.indexOf(',', lastPos);
			w = Integer.parseInt(string.substring(lastPos, i));
			lastPos = i + 1;
			h = Integer.parseInt(string.substring(lastPos));
		} catch (final RuntimeException e) {
			// sloppy error handling
			return null;
		}
		return new Rectangle(x, y, w, h);
	}
}
