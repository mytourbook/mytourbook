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

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

public class RGBVertex implements Comparable<Object> {

	private RGB		_rgb;
	private long	_elevation;

	public RGBVertex() {
		_elevation = 0;
		_rgb = new RGB(255, 255, 255); // WHITE
	}

	public RGBVertex(final int red, final int green, final int blue, final long elevation) {

		if ((red > 255) || (red < 0) || (green > 255) || (green < 0) || (blue > 255) || (blue < 0)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}

		_elevation = elevation;
		_rgb = new RGB(red, green, blue);
	}

	public RGBVertex(final RGB rgb) {
		_elevation = 0;
		_rgb = rgb;
	}

	/**
	 * Make a clone of another {@link RGBVertex}.
	 * 
	 * @param vertexSource
	 */
	public RGBVertex(final RGBVertex vertexSource) {

		final RGB sourceRGB = vertexSource.getRGB();

		_rgb = new RGB(sourceRGB.red, sourceRGB.green, sourceRGB.blue);
		_elevation = vertexSource._elevation;
	}

	public int compareTo(final Object anotherRGBVertex) throws ClassCastException {

		if (!(anotherRGBVertex instanceof RGBVertex)) {
			throw new ClassCastException(Messages.rgv_vertex_class_cast_exception);
		}

		final long anotherElev = ((RGBVertex) anotherRGBVertex).getElevation();

		if (_elevation < anotherElev) {
			return (-1);
		}

		if (_elevation > anotherElev) {
			return 1;
		}

		return 0;
	}

	public long getElevation() {
		return _elevation;
	}

	public RGB getRGB() {
		return _rgb;
	}

	public void setElevation(final long l) {
		_elevation = l;
	}

	public void setRGB(final RGB rgb) {
		_rgb = rgb;
	}

	@Override
	public String toString() {
		return UI.EMPTY_STRING + _elevation + "," + _rgb.red + "," + _rgb.green + "," + _rgb.blue + ";"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
