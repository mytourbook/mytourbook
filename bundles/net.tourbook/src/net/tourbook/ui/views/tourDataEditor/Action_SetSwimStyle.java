/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourDataEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import net.tourbook.common.UI;
import net.tourbook.common.swimming.StrokeStyle;
import net.tourbook.common.swimming.SwimStrokeManager;

class Action_SetSwimStyle extends Action {

	private TourDataEditorView	_tourEditor;
	private final StrokeStyle	_strokeStyle;

	private Image					_swimStyleImage;

	public Action_SetSwimStyle(final TourDataEditorView tourEditor, final StrokeStyle strokeStyle) {

		super(UI.SPACE3 + strokeStyle.swimStrokeLabel);

		_tourEditor = tourEditor;
		_strokeStyle = strokeStyle;

		_swimStyleImage = createSwimStyleImage(strokeStyle);
		setImageDescriptor(ImageDescriptor.createFromImage(_swimStyleImage));
	}

	private Image createSwimStyleImage(final StrokeStyle strokeStyle) {

		final RGB graphBgColor = SwimStrokeManager.getColor(strokeStyle.swimStroke);

		final int imageSize = 16;

		final Device display = Display.getDefault();
		final Image swimStyleImage = new Image(display, imageSize, imageSize);

		final GC gc = new GC(swimStyleImage);
		final Color bgColor = new Color(display, graphBgColor);
		{
			gc.setBackground(bgColor);
			gc.fillRectangle(0, 0, imageSize, imageSize);
		}
		bgColor.dispose();
		gc.dispose();

		return swimStyleImage;
	}

	public void dispose() {

		_swimStyleImage.dispose();
	}

	@Override
	public void run() {

		_tourEditor.setSwimStyle(_strokeStyle);
	}
}
