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

package de.byteholder.geoclipse.mapprovider;

import de.byteholder.geoclipse.mapprovider.DialogMPCustom.PART_TYPE;

public class UrlPart implements Cloneable {

	private PART_TYPE	_partType;

	/**
	 * position of the part within the whole url
	 */
	private int			_position;

	private String		_html;

	private int			_randomIntegerStart;
	private int			_randomIntegerEnd;

	// alphanumeric random a...z
	private String		_randomAlphaStart;
	private String		_randomAlphaEnd;

	private int			fOffsetX;

 	public UrlPart() {}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		final UrlPart urlPart = (UrlPart) super.clone();

		urlPart._html = _html == null ? null : new String(_html);

		urlPart._randomAlphaStart = _randomAlphaStart == null ? null : new String(_randomAlphaStart);
		urlPart._randomAlphaEnd = _randomAlphaEnd == null ? null : new String(_randomAlphaEnd);

		return urlPart;
	}

	public String getHtml() {
		return _html;
	}

	public int getOffsetX() {
		return fOffsetX;
	}

	public PART_TYPE getPartType() {
		return _partType;
	}

	public int getPosition() {
		return _position;
	}

	public String getRandomAlphaEnd() {
		return _randomAlphaEnd;
	}

	public String getRandomAlphaStart() {
		return _randomAlphaStart;
	}

	public int getRandomIntegerEnd() {
		return _randomIntegerEnd;
	}

	public int getRandomIntegerStart() {
		return _randomIntegerStart;
	}

	public void setHtml(final String html) {
		_html = html;
	}

	public void setOffsetX(final int offsetX) {
		fOffsetX=offsetX;
	}

	public void setPartType(final PART_TYPE partType) {
		_partType = partType;
	}

	public void setPosition(final int position) {
		_position = position;
	}

	public void setRandomAlphaEnd(final String randomEnd) {
		_randomAlphaEnd = randomEnd;
	}

	public void setRandomAlphaStart(final String randomStart) {
		_randomAlphaStart = randomStart;
	}

	public void setRandomIntegerEnd(final int randomEnd) {
		_randomIntegerEnd = randomEnd;
	}

	public void setRandomIntegerStart(final int randomStart) {
		_randomIntegerStart = randomStart;
	}

	@Override
	public String toString() {
		return _partType + " pos:" + _position + " html:" + _html; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
