/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.srtm;

public class ElevationLayer {

	private static ElevationBase[]	fLayer	= new ElevationBase[4];

	private static ElevationEtopo	fEtopo	= new ElevationEtopo();
	private static ElevationGlobe	fGlobe	= new ElevationGlobe();
	private static ElevationSRTM3	fSrtm3	= new ElevationSRTM3();
	private static ElevationSRTM1	fSrtm1	= new ElevationSRTM1();

	private static int				zoom;
	private static int				fileTypIndexStart;

	public ElevationLayer() {

		fLayer[0] = fEtopo;
		fLayer[1] = fGlobe;
		fLayer[2] = fSrtm3;
		fLayer[3] = fSrtm1;

		zoom = 0;
	}

	public short getElevation(final GeoLat lat, final GeoLon lon) {

		int layerIndex = fileTypIndexStart;

		while (layerIndex >= 0) {
			try {
				final short hoehe = fLayer[layerIndex].getElevation(lat, lon);

				if (fLayer[layerIndex].isValid(hoehe)) {
					return hoehe;
				} else {
					layerIndex--;
				}
			} catch (final Exception e) {
				layerIndex--;
			}
		}
 
		layerIndex = 0;

		return -500;
	}

	private int getElevationType() {

		if (zoom <= 4) {
			return Constants.ELEVATION_TYPE_ETOPO;
		}

		if (zoom <= 8) {
			return Constants.ELEVATION_TYPE_GLOBE;
		}

//		if (zoom <= 14) {
			return Constants.ELEVATION_TYPE_SRTM3;
//		}
//
//		return Constants.ELEVATION_TYPE_SRTM1;
	}

	public String getName() {
		// ETOPO, GLOBE, SRTM3, SRTM1
		return fLayer[getElevationType()].getName();
	}

	public short getSekDiff() {
		// Anzahl Degreesseconds zwischen zwei Datenpunkten
		return fLayer[getElevationType()].getSecDiff();
	}

	private void setFileTypIndexStart() {
		fileTypIndexStart = getElevationType();
	}

	public void setZoom(final int z) {
		zoom = z;
		setFileTypIndexStart();
	}

}
