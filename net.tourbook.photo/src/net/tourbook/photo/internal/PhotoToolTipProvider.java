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
package net.tourbook.photo.internal;

import java.text.NumberFormat;

import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.photo.Photo;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

public class PhotoToolTipProvider implements ITourToolTipProvider {

//	private TourToolTip		_tourToolTip;

	private Color				_bgColor;
	private Color				_fgColor;
	private Font				_boldFont;

	private Photo				_photo;

	private final NumberFormat	_nf_1_1	= NumberFormat.getNumberInstance();
	{
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	public PhotoToolTipProvider() {}

	@Override
	public void afterHideToolTip() {
		_photo = null;
	}

	public Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_photo == null) {
			// this case should not happen
			return null;
		}

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		return createUI(parent);
	}

	private Composite createUI(final Composite parent) {

		Label label;

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{

			final Composite container = new Composite(shellContainer, SWT.NONE);
			container.setForeground(_fgColor);
			container.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults()//
					.margins(TourToolTip.SHELL_MARGIN, TourToolTip.SHELL_MARGIN)
					.numColumns(2)
					.spacing(5, 2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				/*
				 * name
				 */
				final String name = Double.toString(_photo.getLatitude());
				if (name != null) {

					label = createUILabel(container, name);
					GridDataFactory.fillDefaults().span(2, 1).indent(0, -5).applyTo(label);
					label.setFont(_boldFont);
				}
			}
		}

		return shellContainer;
	}

	private Label createUILabel(final Composite parent, final String labelText) {

		final Label label = new Label(parent, SWT.NONE);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (labelText != null) {
			label.setText(labelText);
		}

		return label;
	}


	@Override
	public void hide() {}

	@Override
	public void paint(final GC gc, final Rectangle rectangle) {
		// painting is done in TourPainter
	}

	@Override
	public boolean setHoveredLocation(final int x, final int y) {
		// this method is not used in this tool tip provider, the method getHoveredContext() is used instead
		return false;
	}

	@Override
	public void setTourToolTip(final TourToolTip tourToolTip) {

		// this feature is not yet used in this tool tip provider

//		_tourToolTip = tourToolTip;
	}

	@Override
	public void show(final Point point) {}

}
