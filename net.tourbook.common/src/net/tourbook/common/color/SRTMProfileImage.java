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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

/**
 * Contain the profile image and all rgb vertices.
 */
public class SRTMProfileImage extends ProfileImage implements Cloneable {

	private static int	MAX_VERTICES_VALUE	= 8850;

	@Override
	public Image createImage(int width, int height, final boolean isHorizontal) {

		// ensure min image size
		width = width < IMAGE_MIN_WIDTH ? IMAGE_MIN_WIDTH : width;
		height = height < IMAGE_MIN_HEIGHT ? IMAGE_MIN_HEIGHT : height;

		final Device display = Display.getCurrent();
		final Image profileImage = new Image(display, width, height);

		/*
		 * draw colors
		 */
		final GC gc = new GC(profileImage);

		final ArrayList<RGBVertex> rgbVertices = getRgbVertices();

		final long maxValue = rgbVertices.size() == 0 //
				? MAX_VERTICES_VALUE
				: rgbVertices.get(rgbVertices.size() - 1).getValue();

		final int horizontal = isHorizontal ? width : height + 1;
		final int vertical = isHorizontal ? height : width;

		for (int x = 0; x < horizontal; x++) {

			final long value = maxValue * x / horizontal;

			final int rgb = getRGB(value);

			final byte blue = (byte) ((rgb & 0xFF0000) >> 16);
			final byte green = (byte) ((rgb & 0xFF00) >> 8);
			final byte red = (byte) ((rgb & 0xFF) >> 0);

			final Color color = new Color(display, red & 0xFF, green & 0xFF, blue & 0xFF);
			{
				gc.setForeground(color);

				if (isHorizontal) {

					// draw horizontal

					final int x1 = horizontal - x - 1;
					final int x2 = horizontal - x - 1;

					final int y1 = 0;
					final int y2 = vertical;

					gc.drawLine(x1, y1, x2, y2);

				} else {

					// draw vertical

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
		for (int ix = 0; ix < rgbVertices.size(); ix++) {

			final long elev = rgbVertices.get(ix).getValue();

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

	@Override
	public int getRGB(final long value) {

		final RGBVertex[] vertexArray = getRgbVerticesArray();

		final int vertexSize = vertexArray.length;

		if (vertexSize == 0) {
			return 0xFFFFFF;
		}

		if (vertexSize == 1) {

			final RGB rgb = vertexArray[0].getRGB();

			return (//
					(rgb.blue & 0xFF) << 16)
					+ ((rgb.green & 0xFF) << 8)
					+ (rgb.red & 0xFF);
		}

		for (int ix = vertexSize - 2; ix >= 0; ix--) {

			final RGBVertex vertex = vertexArray[ix];

			if (value > vertex.getValue()) {

				final RGBVertex vertex2 = vertexArray[ix + 1];

				final RGB rgb1 = vertex.getRGB();
				final RGB rgb2 = vertex2.getRGB();

				final long elev1 = vertex.getValue();
				final long elev2 = vertex2.getValue();

				final long dElevG = elev2 - elev1;
				final long dElev1 = value - elev1;
				final long dElev2 = elev2 - value;

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

				return (//
						(blue & 0xFF) << 16)
						+ ((green & 0xFF) << 8)
						+ (red & 0xFF);
			}
		}

		return 0xFF005F;
	}

}
