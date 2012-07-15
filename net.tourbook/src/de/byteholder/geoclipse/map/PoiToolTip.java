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
package de.byteholder.geoclipse.map;

import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.PoiToolTipShell;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

 
public class PoiToolTip extends PoiToolTipShell {

	public GeoPosition	geoPosition;

	private Text		_lblPoiText;

	public PoiToolTip(final Composite parent) {
		super(parent);
	}

	@Override
	protected Composite setContent(final Shell shell) {

		final Display display = shell.getDisplay();
		final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);

		final Composite container = new Composite(shell, SWT.NONE);
		container.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		container.setBackground(bgColor);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			_lblPoiText = new Text(container, SWT.WRAP | SWT.READ_ONLY);
			_lblPoiText.setBackground(bgColor);
		}

		return container;
	}

	public void setText(final String poiText) {
		_lblPoiText.setText(poiText);
	}
}
