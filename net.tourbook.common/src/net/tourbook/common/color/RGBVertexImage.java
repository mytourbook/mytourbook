/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.common.color;

import java.util.ArrayList;

import net.tourbook.common.UI;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

public class RGBVertexImage {

	private static final int		IMAGE_MIN_WIDTH		= 10;
	private static final int		IMAGE_MIN_HEIGHT	= 10;

	private int						MAX_VERTICES_VALUE	= 8850;

	private Image					_profileImage;

	private int						_imageWidth;
	private int						_imageHeight;
	private boolean					_isHorizontal		= true;

	/**
	 * Contains all vertices.
	 */
	private ArrayList<RGBVertex>	_rgbVertices		= new ArrayList<RGBVertex>();

	private RGBVertex[]				_vertexArray;

	public void addVertex(final RGBVertex rgbVertex) {

		_rgbVertices.add(rgbVertex);

		// invalidate array
		_vertexArray = null;
	}

	private void checkVerticesArray() {
		
		// ensure array is valid
		if (_vertexArray == null) {
			_vertexArray = _rgbVertices.toArray(new RGBVertex[_rgbVertices.size()]);
		}
	}

	public void cloneVertices(final RGBVertexImage rgbVertexImage) {

		_rgbVertices.clear();

		for (final RGBVertex rgbVertex : rgbVertexImage.getRgbVerticies()) {
			_rgbVertices.add(new RGBVertex(rgbVertex));
		}

		// invalidate array
		_vertexArray = null;
	}

	public Image createVertexImage(int width, int height, final boolean isHorizontal) {

		// ensure min image size
		width = width < IMAGE_MIN_WIDTH ? IMAGE_MIN_WIDTH : width;
		height = height < IMAGE_MIN_HEIGHT ? IMAGE_MIN_HEIGHT : height;

		final Device display = Display.getCurrent();
		final Image profileImage = new Image(display, width, height);

		/*
		 * draw colors
		 */
		final GC gc = new GC(profileImage);
		final long maxValue = _rgbVertices.size() == 0 //
				? MAX_VERTICES_VALUE
				: _rgbVertices.get(_rgbVertices.size() - 1).getElevation();

		final int horizontal = isHorizontal ? width : height + 1;
		final int vertical = isHorizontal ? height : width;

		for (int x = 0; x < horizontal; x++) {

			final long elev = maxValue * x / horizontal;

			final int rgb = getRGB(elev);
			final byte blue = (byte) ((rgb & 0xFF0000) >> 16);
			final byte green = (byte) ((rgb & 0xFF00) >> 8);
			final byte red = (byte) ((rgb & 0xFF) >> 0);

			final Color color = new Color(display, red & 0xFF, green & 0xFF, blue & 0xFF);
			{
				gc.setForeground(color);

				if (isHorizontal) {

					final int x1 = horizontal - x - 1;
					final int x2 = horizontal - x - 1;

					final int y1 = 0;
					final int y2 = vertical;

					gc.drawLine(x1, y1, x2, y2);

				} else {

					final int x1 = 0;
					final int x2 = vertical;

					final int y1 = horizontal - x - 1;
					final int y2 = horizontal - x - 1;

					gc.drawLine(x1, y1, x2, y2);
				}
			}
			color.dispose();
		}

		/*
		 * draw text
		 */
		final Transform transform = new Transform(display);
		for (int ix = 0; ix < _rgbVertices.size(); ix++) {

			final long elev = _rgbVertices.get(ix).getElevation();

			if (elev < 0) {
				continue;
			}

			final int rgb = getRGB(elev);
			final byte blue = (byte) ((rgb & 0xFF0000) >> 16);
			final byte green = (byte) ((rgb & 0xFF00) >> 8);
			final byte red = (byte) ((rgb & 0xFF) >> 0);


			int x = maxValue == 0 ? 0 : (int) (elev * horizontal / maxValue);
			x = Math.max(x, 13);

			// Rotate by -90 degrees

			if (isHorizontal) {
				final int dx = horizontal - x - 1;
				final int dy = vertical - 3;
				transform.setElements(0, -1, 1, 0, dx, dy);
			} else {
				final int dx = 3;
				final int dy = horizontal - x - 1;
				transform.setElements(1, 0, 0, 1, dx, dy);
			}

			final Color fgColor = ColorUtil.getContrastColor(display, red & 0xFF, green & 0xFF, blue & 0xFF);
			{
				gc.setTransform(transform);
				gc.setForeground(fgColor);
				gc.drawText(net.tourbook.common.UI.EMPTY_STRING + elev, 0, 0, true);
			}
			fgColor.dispose();
		}
		transform.dispose();

		gc.dispose();

		return profileImage;
	}

	public void disposeImage() {
		UI.disposeResource(_profileImage);
	}

	public int getRGB(final long elev) {

		checkVerticesArray();

		final int vertexSize = _vertexArray.length;

		if (vertexSize == 0) {
//			return new RGB(255, 255, 255);
			return 0xFFFFFF;
		}

		if (vertexSize == 1) {

			final RGB rgb = _vertexArray[0].getRGB();

			return (//
					(rgb.blue & 0xFF) << 16)
					+ ((rgb.green & 0xFF) << 8)
					+ (rgb.red & 0xFF);
		}

		for (int ix = vertexSize - 2; ix >= 0; ix--) {

			final RGBVertex vertex = _vertexArray[ix];
			if (elev > vertex.getElevation()) {

				final RGBVertex vertex2 = _vertexArray[ix + 1];

				final RGB rgb1 = vertex.getRGB();
				final RGB rgb2 = vertex2.getRGB();

				final long elev1 = vertex.getElevation();
				final long elev2 = vertex2.getElevation();

				final long dElevG = elev2 - elev1;
				final long dElev1 = elev - elev1;
				final long dElev2 = elev2 - elev;

				int red = (int) ((double) (rgb2.red * dElev1 + rgb1.red * dElev2) / dElevG);
				int green = (int) ((double) (rgb2.green * dElev1 + rgb1.green * dElev2) / dElevG);
				int blue = (int) ((double) (rgb2.blue * dElev1 + rgb1.blue * dElev2) / dElevG);

				if (red > 0xFF) {
					red = 0xFF;
				}
				if (green > 0xFF) {
					green = 0xFF;
				}
				if (blue > 0xFF) {
					blue = 0xFF;
				}

				if (red < 0) {
					red = 0;
				}
				if (green < 0) {
					green = 0;
				}
				if (blue < 0) {
					blue = 0;
				}

//				return new RGB(red, green, blue);

				return (//
						(blue & 0xFF) << 16)
						+ ((green & 0xFF) << 8)
						+ (red & 0xFF);
			}
		}
//		return new RGB(255, 255, 255);
		return 0xFFFFFF;
	}

	/**
	 * @return Return rgb verticies, this list should not be modified, use
	 *         {@link #setVerticies(ArrayList)} to modify this list.
	 */
	public ArrayList<RGBVertex> getRgbVerticies() {
		return _rgbVertices;
	}

	/**
	 * @param width
	 * @param height
	 * @param isHorizontal
	 * @return Returns cached rgb vertex image, the image will be created when size or orientation
	 *         is different.
	 */
	public Image getValidatedImage(final int width, final int height, final boolean isHorizontal) {

		/*
		 * create image when the requested image size/orientation is different than the previous
		 * created image
		 */
		if (_profileImage == null
				|| _profileImage.isDisposed()
				|| _isHorizontal != isHorizontal
				|| _imageWidth != width
				|| _imageHeight != height) {

			_isHorizontal = isHorizontal;
			_imageWidth = width;
			_imageHeight = height;

			// dispose previous image
			disposeImage();

			_profileImage = createVertexImage(width, height, isHorizontal);
		}

		return _profileImage;
	}

	public String getVertexKey() {

		final StringBuilder sb = new StringBuilder();
		for (final RGBVertex vertex : _rgbVertices) {
			sb.append(vertex.toString());
		}
		return sb.toString();
	}

	/**
	 * Removes vertices by index.
	 * 
	 * @param vertexRemoveIndex
	 */
	public void removeVertices(final ArrayList<Integer> vertexRemoveIndex) {

		final ArrayList<RGBVertex> removedVertices = new ArrayList<RGBVertex>();

		for (final Integer integer : vertexRemoveIndex) {
			removedVertices.add(_rgbVertices.get(integer));
		}

		_rgbVertices.removeAll(removedVertices);

		// invalidate array
		_vertexArray = null;
	}

	public void setVerticies(final ArrayList<RGBVertex> vertexList) {

		_rgbVertices.clear();

		for (final RGBVertex vertex : vertexList) {
			_rgbVertices.add(vertex);
		}

		// invalidate array
		_vertexArray = null;
	}

}
