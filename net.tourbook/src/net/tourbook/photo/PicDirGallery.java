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
package net.tourbook.photo;

import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * Wrapper for the {@link Gallery} to get access to protected fields.
 */
public class PicDirGallery extends Gallery {

	private Rectangle	_clientArea;
	private boolean		_isVertical;

	public PicDirGallery(final Composite parent, final int style) {
		super(parent, style);

		_isVertical = (style & SWT.V_SCROLL) > 0;

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_clientArea = getClientArea();
			}
		});
	}

	public Rectangle getClientAreaGallery() {
		return _clientArea;
	}

	public int getTranslate() {
		return translate;
	}

	public boolean isVerticalGallery() {
		return _isVertical;
	}

}
