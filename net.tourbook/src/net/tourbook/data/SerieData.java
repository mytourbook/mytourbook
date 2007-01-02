/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
 * 
 */
package net.tourbook.data;

import java.io.Serializable;

/**
 * @author Administrator
 * 
 */
public class SerieData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int timeSerie[];
	public int distanceSerie[];

	public int altitudeSerie[];
	public int cadenceSerie[];
	public int pulseSerie[];
	public int temperatureSerie[];

	public int speedSerie[];
	public int powerSerie[];

	public int deviceMarker[];

	public SerieData(int length) {
	
		timeSerie = new int[length];
		distanceSerie = new int[length];
		altitudeSerie = new int[length];
		cadenceSerie = new int[length];
		pulseSerie = new int[length];
		temperatureSerie = new int[length];

		speedSerie = new int[length];
		powerSerie = new int[length];
	}
}
