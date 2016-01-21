/* *****************************************************************************
 *  Copyright (C) 2008 Michael Kanis and others
 *  
 *  This file is part of Geoclipse.
 *
 *  Geoclipse is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Geoclipse is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Geoclipse.  If not, see <http://www.gnu.org/licenses/>. 
 *******************************************************************************/

package de.byteholder.gpx;

import java.net.URL;

/**
 * Information about the copyright holder and any license governing use of this
 * file. By linking to an appropriate license, you may place your data into the
 * public domain or grant additional usage rights.
 * 
 * @author Michael Kanis
 */
public class Copyright {

	/**
	 * Year of copyright.
	 */
	private Short year;
	
	/**
	 * Link to external file containing license text.
	 */
	private URL license;

	public URL getLicense() {
		return license;
	}

	public void setLicense(URL license) {
		this.license = license;
	}

	public Short getYear() {
		return year;
	}

	public void setYear(Short year) {
		this.year = year;
	}
}
