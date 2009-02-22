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
package net.tourbook.ext.srtm;

public class ElevationLayer {

	static private ElevationBase[]	fLayer	= new ElevationBase[4];
	static private ElevationEtopo	fEtopo	= new ElevationEtopo();
	static private ElevationGlobe	fGlobe	= new ElevationGlobe();
	static private ElevationSRTM3	fSrtm3	= new ElevationSRTM3();
	static private ElevationSRTM1	fSrtm1	= new ElevationSRTM1();

	static int						zoom;
	static int						layerIndex;
	static int						fileTypIndexStart;

	public ElevationLayer() {

		fLayer[0] = fEtopo;
		fLayer[1] = fGlobe;
		fLayer[2] = fSrtm3;
		fLayer[3] = fSrtm1;
		zoom = 0;
	}

	private void setFileTypIndexStart() {
		fileTypIndexStart = getElevationType();
	}

	public short getElevation(GeoLat lat, GeoLon lon) {
		return getElevationPrivate(lat, lon, fileTypIndexStart);
	}

	private short getElevationPrivate(GeoLat lat, GeoLon lon, int layerIndexStart) {

		layerIndex = layerIndexStart;
		while (layerIndex >= 0) {
			try {
				short hoehe = fLayer[layerIndex].getElevation(lat, lon);

				if (fLayer[layerIndex].isValid(hoehe))
					return hoehe;
				else
					layerIndex--;
			} catch (Exception e) {
				layerIndex--;
			}
		}
		layerIndex = 0;
		return -500;
	}

	public double getElevationDouble(GeoLat lat, GeoLon lon) {

		layerIndex = Constants.ELEVATION_TYPE_SRTM1;
		while (layerIndex >= 0) {
			try {
				double hoehe = fLayer[layerIndex].getElevationDouble(lat, lon);

				if (fLayer[layerIndex].isValid(hoehe))
					return hoehe;
				else
					layerIndex--;
			} catch (Exception e) {
				layerIndex--;
			}
		}
		layerIndex = 0;
		return -500;
	}

	public short getSekDiff() {
		// Anzahl Degreesseconds zwischen zwei Datenpunkten
		return fLayer[getElevationType()].getSecDiff();
	}

	public String getName() {
		// ETOPO, GLOBE, SRTM3, SRTM1
		return fLayer[getElevationType()].getName();
	}

	private int getElevationType() {
		if (zoom <= 4)
			return Constants.ELEVATION_TYPE_ETOPO;
		if (zoom <= 8)
			return Constants.ELEVATION_TYPE_GLOBE;
		if (zoom <= 14)
			return Constants.ELEVATION_TYPE_SRTM3;
		return Constants.ELEVATION_TYPE_SRTM1;
	}

	public void setZoom(int z) {
		zoom = z;
		setFileTypIndexStart();
	}

}
