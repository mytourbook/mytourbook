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
import java.util.LinkedList;
import java.util.List;

/**
 * A Track is an ordered list of points describing a path.
 * 
 * @author Michael Kanis
 */
public class Track {

	/**
	 * GPS name of track.
	 */
	private String name;
	
	/**
	 * GPS comment for track.
	 */
	private String comment;
	
	/**
	 * User description of track.
	 */
	private String description;
	
	/**
	 * Source of data. Included to give user some idea of reliability and
	 * accuracy of data.
	 */
	private String source;
	
	/**
	 * Links to external information about track.
	 */
	private Collection<URL> link;
	
	/**
	 * GPS track number.
	 */
	private Long number;
	
	/**
	 * Type (classification) of track.
	 */
	private String type;

	/**
	 * A Track Segment holds a list of Track Points which are logically
	 * connected in order. To represent a single GPS track where GPS reception
	 * was lost, or the GPS receiver was turned off, start a new Track Segment
	 * for each continuous span of track data.
	 */
	private List<TrackSegment> segments;

	
	public Track() {
		segments = new LinkedList<TrackSegment>();
	}
	
	
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<URL> getLink() {
		return link;
	}

	public void setLink(Collection<URL> link) {
		this.link = link;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<TrackSegment> getSegments() {
		return segments;
	}
	
	public void addSegment(TrackSegment segment) {
		segments.add(segment);
	}
}
