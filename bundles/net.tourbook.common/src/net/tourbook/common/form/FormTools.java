/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.common.form;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.widgets.Composite;


/**
 * The class provides tools for forms
 */
public class FormTools {

	/**
	 * Display text as a bulleted list
	 * 
	 * @param parent
	 * @param bulletText
	 * @param startLine
	 *            Line where bullets should be started, 0 is the first line
	 * @param spanHorizontal
	 * @param horizontalHint
	 * @param backgroundColor
	 *            background color or <code>null</code> when color should not be set
	 * @return Returns the bulleted list as styled text
	 */
	public static StyledText createBullets(	final Composite parent,
											final String bulletText,
											final int startLine,
											final int spanHorizontal,
											final int horizontalHint,
											final Color backgroundColor) {

		StyledText styledText = null;

		try {

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.span(spanHorizontal, 1)
					.hint(horizontalHint, SWT.DEFAULT)
					.applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(1)
					.margins(5, 5)
					.applyTo(container);

			container.setBackground(backgroundColor == null ? //
					container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
					: backgroundColor);
			{
				final StyleRange style = new StyleRange();
				style.metrics = new GlyphMetrics(0, 0, 10);

				final Bullet bullet = new Bullet(style);
				final int lineCount = Util.countCharacter(bulletText, '\n');

				styledText = new StyledText(container, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(styledText);
				styledText.setText(bulletText);

				styledText.setLineBullet(startLine, lineCount, bullet);
				styledText.setLineWrapIndent(startLine, lineCount, 10);

				styledText.setBackground(backgroundColor == null ? //
						container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
						: backgroundColor);
			}

		} catch (final Exception e) {
			// ignore exception when there are less lines as required
			StatusUtil.log(e);
		}

		return styledText;
	}
}
