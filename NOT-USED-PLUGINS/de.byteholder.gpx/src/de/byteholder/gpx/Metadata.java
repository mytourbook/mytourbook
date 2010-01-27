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

/**
 * Information about the GPX file, author, and copyright restrictions goes in
 * the metadata section. Providing rich, meaningful information about your GPX
 * files allows others to search for and use your GPS data.
 * 
 * @author Michael Kanis
 */
public class Metadata {

	/**
	 * The name of the GPX file.
	 */
	private String name;
	
	/**
	 * A description of the contents of the GPX file.
	 */
	private String description;
	
	/**
	 * The person or organization who created the GPX file.
	 */
	private Person author;
	
	/**
	 * Copyright and license information governing use of the file.
	 */
	private Copyright copyright;
	
	/**
	 * URLs associated with the location described in the file.
	 */
	private Collection<URL> links;
	
	/**
	 * The creation date of the file.
	 */
	private Date time;
	
	/**
	 * Keywords associated with the file. Search engines or databases can use
	 * this information to classify the data.
	 */
	private String keywords;
	
	/**
	 * Minimum and maximum coordinates which describe the extent of the
	 * coordinates in the file.
	 */
	private Bounds bounds;

	public Person getAuthor() {
		return author;
	}

	public void setAuthor(Person author) {
		this.author = author;
	}

	public Bounds getBounds() {
		return bounds;
	}

	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}

	public Copyright getCopyright() {
		return copyright;
	}

	public void setCopyright(Copyright copyright) {
		this.copyright = copyright;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public Collection<URL> getLinks() {
		return links;
	}

	public void setLinks(Collection<URL> links) {
		this.links = links;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}
}
