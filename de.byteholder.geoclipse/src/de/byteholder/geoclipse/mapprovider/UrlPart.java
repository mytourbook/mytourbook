/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

	private PART_TYPE	fPartType;

	/**
	 * position of the part within the whole url
	 */
	private int			fPosition;

	private String		fHtml;

	private int			fRandomIntegerStart;
	private int			fRandomIntegerEnd;

	// alphanumeric random a...z
	private String		fRandomAlphaStart;
	private String		fRandomAlphaEnd;

	private int			fOffsetX;

 	public UrlPart() {}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		final UrlPart urlPart = (UrlPart) super.clone();

		urlPart.fHtml = fHtml == null ? null : new String(fHtml);

		urlPart.fRandomAlphaStart = fRandomAlphaStart == null ? null : new String(fRandomAlphaStart);
		urlPart.fRandomAlphaEnd = fRandomAlphaEnd == null ? null : new String(fRandomAlphaEnd);

		return urlPart;
	}

	public String getHtml() {
		return fHtml;
	}

	public PART_TYPE getPartType() {
		return fPartType;
	}

	public int getPosition() {
		return fPosition;
	}

	public String getRandomAlphaEnd() {
		return fRandomAlphaEnd;
	}

	public String getRandomAlphaStart() {
		return fRandomAlphaStart;
	}

	public int getRandomIntegerEnd() {
		return fRandomIntegerEnd;
	}

	public int getRandomIntegerStart() {
		return fRandomIntegerStart;
	}

	public void setHtml(final String html) {
		fHtml = html;
	}

	public void setOffsetX(final int offsetX) {
		fOffsetX=offsetX;
	}

	public void setPartType(final PART_TYPE partType) {
		fPartType = partType;
	}

	public void setPosition(final int position) {
		fPosition = position;
	}

	public void setRandomAlphaEnd(final String randomEnd) {
		fRandomAlphaEnd = randomEnd;
	}

	public void setRandomAlphaStart(final String randomStart) {
		fRandomAlphaStart = randomStart;
	}

	public void setRandomIntegerEnd(final int randomEnd) {
		fRandomIntegerEnd = randomEnd;
	}

	public void setRandomIntegerStart(final int randomStart) {
		fRandomIntegerStart = randomStart;
	}

	@Override
	public String toString() {
		return fPartType + " pos:" + fPosition + " html:" + fHtml; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public int getOffsetX() {
		return fOffsetX;
	}

}
