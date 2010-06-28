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
package net.tourbook.mapping;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourWayPoint;
import net.tourbook.ui.IInfoToolTipProvider;
import net.tourbook.ui.TourToolTip;
import net.tourbook.util.ITourToolTipProvider;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import de.byteholder.geoclipse.map.HoveredAreaContext;
import de.byteholder.geoclipse.map.IHoveredArea;

public class TourInfoToolTipProvider implements ITourToolTipProvider, IInfoToolTipProvider, IHoveredArea {

	private static final int	HOVER_AREA	= 2;

//	private TourToolTip			_tourToolTip;

	private Color				_bgColor;
	private Color				_fgColor;
	private Font				_boldFont;

	private TourWayPoint		_hoveredWayPoint;

	private static Image		_tourInfoImage;
	private static Image		_tourInfoImageHovered;
	private static Rectangle	_tourInfoImageSize;

	public TourInfoToolTipProvider() {

//		_tourToolTip = tourToolTip;

		createInfoIcon();
	}

	private void createInfoIcon() {

		if (_tourInfoImage != null) {
			return;
		}

		final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();

		imageRegistry.put(
				Messages.Image_ToolTip_TourInfo,
				TourbookPlugin.getImageDescriptor(Messages.Image_ToolTip_TourInfo));

		imageRegistry.put(
				Messages.Image_ToolTip_TourInfo_Hovered,
				TourbookPlugin.getImageDescriptor(Messages.Image_ToolTip_TourInfo_Hovered));

		_tourInfoImage = imageRegistry.get(Messages.Image_ToolTip_TourInfo);
		_tourInfoImageHovered = imageRegistry.get(Messages.Image_ToolTip_TourInfo_Hovered);

		_tourInfoImageSize = _tourInfoImage.getBounds();
	}

	public Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_hoveredWayPoint == null) {
			// this case should not happen
			return null;
		}

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		return createUI(parent);
	}

	private Composite createUI(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		final Label label;

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
				label = new Label(container, SWT.NONE);
				label.setText("tooltip"); //$NON-NLS-1$
			}
		}

		return shellContainer;
	}

	@Override
	public HoveredAreaContext getHoveredContext(final int devMouseX, final int devMouseY) {

		if (devMouseX >= HOVER_AREA
				&& devMouseX <= HOVER_AREA + _tourInfoImageSize.width
				&& devMouseY >= HOVER_AREA
				&& devMouseY <= HOVER_AREA + _tourInfoImageSize.height) {

			return new HoveredAreaContext(
					this,
					this,
					HOVER_AREA,
					HOVER_AREA,
					_tourInfoImageSize.width,
					_tourInfoImageSize.height);
		}

		return null;
	}

	@Override
	public Image getHoveredImage() {
		return _tourInfoImageHovered;
	}

	private void onDispose() {

	}

	@Override
	public void paint(final GC gc, final Rectangle clientArea) {
		// paint static image
		gc.drawImage(_tourInfoImage, HOVER_AREA, HOVER_AREA);
	}

	@Override
	public void show(final Point point) {
		// TODO Auto-generated method stub

	}

}
