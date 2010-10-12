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

package de.byteholder.geoclipse.util;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import de.byteholder.gpx.GeoPosition;

public class YahooGeoCoder implements GeoCoder {

	/**
	 * Convert a street address into a position. Uses the Yahoo GeoCoder. You must
	 * supply your own yahoo id.
	 * 
	 * @param street Street
	 * @param city City
	 * @param state State (must be a US state)
	 * @throws java.io.IOException if the request fails.
	 * @return the position of this street address
	 */
	public GeoPosition getPositionForAddress(String street, String city, String state) throws IOException {
	    try {
	        URL load = new URL("http://api.local.yahoo.com/MapsService/V1/geocode?"+ //$NON-NLS-1$
	                "appid=joshy688"+ //$NON-NLS-1$
	                "&street="+street.replace(' ','+')+ //$NON-NLS-1$
	                "&city="+city.replace(' ','+')+ //$NON-NLS-1$
	                "&state="+state.replace(' ','+')); //$NON-NLS-1$
	        //System.out.println("using address: " + load);
	        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	        Document doc = builder.parse(load.openConnection().getInputStream());
	        XPath xpath = XPathFactory.newInstance().newXPath();
	        //NodeList str = (NodeList)xpath.evaluate("//Result",doc,XPathConstants.NODESET);
	        Double lat = (Double)xpath.evaluate("//Result/Latitude/text()",doc,XPathConstants.NUMBER); //$NON-NLS-1$
	        Double lon = (Double)xpath.evaluate("//Result/Longitude/text()",doc,XPathConstants.NUMBER); //$NON-NLS-1$
	        //System.out.println("got address at: " + lat + " " + lon);
	        return new GeoPosition(lat,lon);
	    } catch (IOException e) {
	        throw e;
	    } catch (Exception e) {
	        throw new IOException("Failed to retrieve location information from the internet: " + e.toString()); //$NON-NLS-1$
	    }
	}

}
