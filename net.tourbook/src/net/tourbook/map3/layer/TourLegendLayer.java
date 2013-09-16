/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.ScreenImage;

import java.awt.Point;
import java.awt.image.BufferedImage;

import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.map.MapUtils;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.SWT_AWT_Images;

public class TourLegendLayer extends RenderableLayer {

	public static final String	MAP3_LAYER_ID	= "TourLegendLayer";	//$NON-NLS-1$

	private IMapColorProvider	_colorProvider;

	private ScreenImage			_legendImage;

	private int					_legendImageHeight;
	private Point				_legendImageLocation;

	private BufferedImage		_awtLegendImage;

	public TourLegendLayer(final IDialogSettings state) {

		restoreState(state);

		_legendImage = new ScreenImage();
		_legendImage.setOpacity(0.8);

		addRenderable(_legendImage);

		setPickEnabled(false);
	}

	public void resizeLegendImage() {

		IGradientColors colorProvider = null;
		if (_colorProvider instanceof IGradientColors) {
			colorProvider = (IGradientColors) _colorProvider;
		}

		if (colorProvider == null) {
			return;
		}

		/*
		 * check if the legend size must be adjusted
		 */
		final Object legendImage = _legendImage.getImageSource();
		if (legendImage == null) {
			// legend image is not yet created
			return;
		}

		// check if legend is displayed
//		if ((_isTourOrWayPoint == false) || (_isShowTour == false) || (_isShowLegend == false)) {
//			return;
//		}


		updateLegendImage();

	}

	private void restoreState(final IDialogSettings state) {

	}

	public void saveState(final IDialogSettings state) {

	}

	public void setColorProvider(final IMapColorProvider colorProvider) {
		_colorProvider = colorProvider;
	}

	public void setLegendVisible(final boolean isLegendVisible) {

		// show/hide image
		_legendImage.setScreenLocation(isLegendVisible ? _legendImageLocation : null);
	}

	/**
	 * Creates a new legend image.
	 * 
	 * @param isUpdateMinMax
	 * @param mapColorProvider
	 */
	public void updateLegendImage() {

		final int legendWidth = IMapColorProvider.DEFAULT_LEGEND_WIDTH;

		final int mapHeight = Map3Manager.getMap3View().getMapSize().height;

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tmapHeight=" + mapHeight);
//		// TODO remove SYSTEM.OUT.PRINTLN

		_legendImageHeight = Math.max(1, Math.min(//
				IMapColorProvider.DEFAULT_LEGEND_HEIGHT,
				mapHeight - IMapColorProvider.LEGEND_TOP_MARGIN));

		final int legendHeightNoMargin = _legendImageHeight - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

		MapUtils.updateMinMaxValues(//
				Map3Manager.getMap3View().getAllTours(),
				(IGradientColors) _colorProvider,
				legendHeightNoMargin);

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(//
				legendWidth,
				_legendImageHeight,
				24,
				new PaletteData(0xff, 0xff00, 0xff0000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();

		final Image legendImage = new Image(display, overlayImageData);
		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(legendImage);
		{
			final Rectangle legendImageBounds = legendImage.getBounds();

			gc.setBackground(transparentColor);
			gc.fillRectangle(legendImageBounds);

			TourMapPainter.drawMapLegend(gc, legendImageBounds, _colorProvider, true);

			final ImageData imageData = legendImage.getImageData();

			SWT_AWT_Images.convertTransparentPixelToTransparentData(imageData, rgbTransparent);

			_awtLegendImage = SWT_AWT_Images.convertToAWT(imageData);

			_legendImage.setImageSource(_awtLegendImage);

		}
		gc.dispose();
		transparentColor.dispose();
		legendImage.dispose();

		/*
		 * set legend positon at left/bottom
		 */
		final int devXCenter = (int) (5 + IMapColorProvider.DEFAULT_LEGEND_WIDTH / 2.0);
		final int devYCenter = (int) (mapHeight - 20 - _legendImageHeight / 2.0);

		_legendImageLocation = new Point(devXCenter, devYCenter);
		_legendImage.setScreenLocation(_legendImageLocation);
	}
}
