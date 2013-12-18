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
import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

public class RGBVertex implements Comparable<Object>, Cloneable {

	private long	_value;
	private RGB		_rgb;

	public RGBVertex() {
		_value = 0;
		_rgb = new RGB(255, 255, 255); // WHITE
	}

	public RGBVertex(final long value, final int red, final int green, final int blue) {

		if ((red > 255) || (red < 0) || (green > 255) || (green < 0) || (blue > 255) || (blue < 0)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}

		_value = value;
		_rgb = new RGB(red, green, blue);
	}

	public RGBVertex(final RGB rgb) {
		_value = 0;
		_rgb = rgb;
	}

	@Override
	public RGBVertex clone() {

		RGBVertex clonedObject = null;

		try {

			clonedObject = (RGBVertex) super.clone();

			clonedObject._rgb = new RGB(_rgb.red, _rgb.green, _rgb.blue);

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	public int compareTo(final Object anotherRGBVertex) throws ClassCastException {

		if (!(anotherRGBVertex instanceof RGBVertex)) {
			throw new ClassCastException(Messages.rgv_vertex_class_cast_exception);
		}

		final long anotherValue = ((RGBVertex) anotherRGBVertex).getValue();

		if (_value < anotherValue) {
			return (-1);
		}

		if (_value > anotherValue) {
			return 1;
		}

		return 0;
	}

	public RGB getRGB() {
		return _rgb;
	}

	public long getValue() {
		return _value;
	}

	public void setRGB(final RGB rgb) {
		_rgb = rgb;
	}

	public void setValue(final long l) {
		_value = l;
	}

	@Override
	public String toString() {
		return String.format("\n\tRGBVertex [_value=%s, _rgb=%s]", _value, _rgb);
	}
}
