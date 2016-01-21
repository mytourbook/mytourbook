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
import java.util.Collection;
import java.util.Date;

import net.tourbook.common.map.GeoPosition;


/**
 * A Waypoint represents a waypoint, point of interest, or named feature on a
 * map.
 * 
 * @author Michael Kanis
 */
public class Waypoint {
    
	/**
	 * The latitude and longitude of the point. Decimal degrees, WGS84 datum.
	 */
	private GeoPosition position;
    
	/**
	 * Elevation (in meters) of the point.
	 */
    private Float elevation;
    
    /**
     * Creation/modification timestamp for element. Date and time in are in
     * Univeral Coordinated Time (UTC), not local time! Conforms to ISO 8601
     * specification for date/time representation. Fractional seconds are
     * allowed for millisecond timing in tracklogs.
     */
    private Date time;
    
    /**
     * Magnetic variation (in degrees) at the point.
     */
    private Degrees magneticVariation;
    
    /**
     * Height (in meters) of geoid (mean sea level) above WGS84 earth ellipsoid.
     * As defined in NMEA GGA message.
     */
    private Double geoIdHeight;
    
    /**
     * The GPS name of the waypoint. This field will be transferred to and from
     * the GPS. GPX does not place restrictions on the length of this field or
     * the characters contained in it. It is up to the receiving application to
     * validate the field before sending it to the GPS.
     */
    private String name;
    
    /**
     * A text description of the element. Holds additional information about the
     * element intended for the user, not the GPS.
     */
    private String description;

    /**
     * GPS waypoint comment. Sent to GPS as comment.
     */
    private String comment;
    
    /**
     * Links to additional information about the waypoint.
     */
    private Collection<URL> link;
    
    /**
     * Type (classification) of the waypoint.
     */
    private String type;
    
    /**
     * Text of GPS symbol name. For interchange with other programs, use the
     * exact spelling of the symbol as displayed on the GPS. If the GPS
     * abbreviates words, spell them out.
     */
    private String symbolName;
    
    /**
     * Type of GPS fix.
     */
    private Fix fix;
    
    /**
     * Number of satellites used to calculate the GPX fix.
     */
    private Byte satellites;
    
    /**
     * Horizontal dilution of precision.
     */
    private Double hdop;
    
    /**
     * Vertical dilution of precision.
     */
    private Double vdop;
    
    /**
     * Position dilution of precision.
     */
    private Double pdop;
    
    /**
     * Number of seconds since last DGPS update.
     */
    private Double ageOfDgpsData;
    
    /**
     * DGPS station used in differential correction.
     */
    private DgpsStation dgpsId;
    
    
    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
     * Creates a new instance of Waypoint at lat/long 0,0
     */
    public Waypoint() {
        this(new GeoPosition(0, 0));
    }
    
    /**
     * Creates a new instance of Waypoint at the specified
     * latitude and longitude
     * @param latitude new latitude
     * @param longitude new longitude
     */
    public Waypoint(double latitude, double longitude) {
        this(new GeoPosition(latitude,longitude));
    }
    
    /**
     * Creates a new instance of Waypoint at the specified
     * GeoPosition
     * @param coord a GeoPosition to initialize the new Waypoint
     */
    public Waypoint(GeoPosition coord) {
        this.position = coord;
    }
    
    /**
     * Get the current GeoPosition of this Waypoint
     * @return the current position
     */
    public GeoPosition getPosition() {
        return position;
    }

    /**
     * Set a new GeoPosition for this Waypoint
     * @param coordinate a new position
     */
    public void setPosition(GeoPosition coordinate) {
        this.position = coordinate;
    }

	public Double getAgeOfDgpsData() {
		return ageOfDgpsData;
	}

	public void setAgeOfDgpsData(Double ageOfDgpsData) {
		this.ageOfDgpsData = ageOfDgpsData;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Float getElevation() {
		return elevation;
	}

	public void setElevation(Float elevation) {
		this.elevation = elevation;
	}

	public Fix getFix() {
		return fix;
	}

	public void setFix(Fix fix) {
		this.fix = fix;
	}

	public Double getGeoIdHeight() {
		return geoIdHeight;
	}

	public void setGeoIdHeight(Double geoIdHeight) {
		this.geoIdHeight = geoIdHeight;
	}

	public Double getHdop() {
		return hdop;
	}

	public void setHdop(Double hdop) {
		this.hdop = hdop;
	}

	public Collection<URL> getLink() {
		return link;
	}

	public void setLink(Collection<URL> link) {
		this.link = link;
	}

	public Degrees getMagneticVariation() {
		return magneticVariation;
	}

	public void setMagneticVariation(Degrees magneticVariation) {
		this.magneticVariation = magneticVariation;
	}

	public Double getPdop() {
		return pdop;
	}

	public void setPdop(Double pdop) {
		this.pdop = pdop;
	}

	public Byte getSatellites() {
		return satellites;
	}

	public void setSatellites(Byte satellites) {
		this.satellites = satellites;
	}

	public String getSymbolName() {
		return symbolName;
	}

	public void setSymbolName(String symbolName) {
		this.symbolName = symbolName;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Double getVdop() {
		return vdop;
	}

	public void setVdop(Double vdop) {
		this.vdop = vdop;
	}

	public DgpsStation getDgpsId() {
		return dgpsId;
	}

	public void setDgpsId(DgpsStation dgpsId) {
		this.dgpsId = dgpsId;
	}
    
    
}
