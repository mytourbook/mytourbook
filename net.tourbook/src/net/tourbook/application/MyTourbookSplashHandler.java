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
package net.tourbook.application;

import net.tourbook.Messages;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
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

	@Override
	public void init(final Shell splash) {

		super.init(splash);

		// keep the splash handler to be used outside of this splash handlers
		TourbookPlugin.setSplashHandler(this);

		String progressRectString = null;
		String messageRectString = null;
//		String foregroundColorString = null;

		final IProduct product = Platform.getProduct();
		if (product != null) {
			progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
			messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
//			foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
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
//		try {
//			// debug color
//			foregroundColorInteger = Integer.parseInt(foregroundColorString, 16);
//		} catch (final Exception ex) {
//			// production color, debug is not using this color
//			foregroundColorInteger = 0x2d84f6;
//		}
//		foregroundColorInteger = 0x2d84f6;
//		foregroundColorInteger = 0x2a7ce7;
		foregroundColorInteger = 0xffffff;

		setForeground(new RGB(
				(foregroundColorInteger & 0xFF0000) >> 16,
				(foregroundColorInteger & 0xFF00) >> 8,
				foregroundColorInteger & 0xFF));

//		final String buildId = "Version " + System.getProperty("eclipse.buildId", "Unknown Version"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		getContent().addPaintListener(new PaintListener() {

			@Override
			public void paintControl(final PaintEvent e) {
				onPaint(e);
			}
		});
	}

	private void onPaint(final PaintEvent e) {

		final GC gc = e.gc;
		gc.setForeground(getForeground());

		final int borderRight = 385;
		final int borderBottom = 101;

		final String copyRight = NLS.bind(Messages.App_Splash_Copyright, ApplicationVersion.SPLASH_COPYRIGHT_YEAR);
		final int textHeight = gc.textExtent(copyRight).y;

		final String version = "Version " + ApplicationVersion.getVersionSimple(); //$NON-NLS-1$
		final Point versionExtent = gc.textExtent(version);

		final String qualifier = ApplicationVersion.getVersionQualifier();
		final Point qualifierExtent = gc.textExtent(qualifier);

		gc.drawText(
				version,
				borderRight - versionExtent.x,
				borderBottom - versionExtent.y - 2 - qualifierExtent.y,
				true);
		gc.drawText(qualifier, borderRight - qualifierExtent.x, borderBottom - versionExtent.y, true);

		gc.drawText(copyRight, 5, 162 - textHeight, true);
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
