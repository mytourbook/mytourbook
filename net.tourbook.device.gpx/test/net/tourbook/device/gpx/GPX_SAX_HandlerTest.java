/*******************************************************************************
 * Copyright (C) 2005, 2010 Wolfgang Schramm and Contributors
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
package net.tourbook.device.gpx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;
import net.tourbook.tour.TourManager;

import org.junit.Test;

/**
 * GPX device plugin test.
 * <p>
 * Run as "JUnit Plug-in Test", as application "[No Application] - Headless Mode" (Main Tab)
 * 
 * @author Norbert Renner
 */
public class GPX_SAX_HandlerTest {

	/**
	 * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
	 */
	public static final String	IMPORT_FILE_PATH	= "/net/tourbook/test.gpx"; //$NON-NLS-1$

	/**
	 * Regression test. Imports GPX into TourData and checks all values of tour1 and waypoint1.
	 */
	@Test
	public void testParse() throws Exception {

		final InputStream gpx = GPX_SAX_HandlerTest.class.getResourceAsStream(IMPORT_FILE_PATH);

		final HashMap<Long, TourData> tourDataMap = new HashMap<Long, TourData>();
		final DeviceData deviceData = new DeviceData();
		final GPXDeviceDataReader deviceDataReader = new GPXDeviceDataReader();

		final GPX_SAX_Handler handler = new GPX_SAX_Handler(deviceDataReader, IMPORT_FILE_PATH, deviceData, tourDataMap);
		SAXParserFactory.newInstance().newSAXParser().parse(gpx, handler);

		assertEquals(2, tourDataMap.size());

		final TourData tour1 = tourDataMap.get(Long.valueOf(201010101205990L));

		// waypoint1
		final Set<TourWayPoint> tourWayPoints = tour1.getTourWayPoints();
		assertEquals(2, tourWayPoints.size());
		final Iterator<TourWayPoint> iter = tourWayPoints.iterator();
		TourWayPoint waypoint1 = iter.next();
		if (!"waypoint1".equals(waypoint1.getName())) { //$NON-NLS-1$
			waypoint1 = iter.next();
		}
		assertEquals(47.5674099, waypoint1.getLatitude(), 0);
		assertEquals(9.47741068, waypoint1.getLongitude(), 0);
		assertEquals(394.0, waypoint1.getAltitude(), 0);
		assertEquals(1286706600000L, waypoint1.getTime());
		assertEquals("waypoint1", waypoint1.getName()); //$NON-NLS-1$
		assertEquals("waypoint1", waypoint1.getComment()); //$NON-NLS-1$
		assertEquals("waypoint1", waypoint1.getDescription()); //$NON-NLS-1$

		// tour1
		assertEquals("track1", tour1.getTourTitle()); //$NON-NLS-1$
		assertEquals(4, tour1.timeSerie.length);

		assertArrayEquals(new int[] { 395, 394, 393, 394 }, tour1.altitudeSerie);
		assertEquals("2010-10-10T12:00:00.000+02:00", TourManager.getTourDateTime(tour1).toString()); //$NON-NLS-1$
		assertArrayEquals(new int[] { 0, 600, 1200, 1800 }, tour1.timeSerie);

		final double[] expectedLat = new double[] { 47.567517620, 47.585505568, 47.585397817, 47.567409937 };
		assertArrayEquals(expectedLat, tour1.latitudeSerie, 0);
		final double[] expectedLon = new double[] { 9.450832423, 9.450987056, 9.477574415, 9.477410677 };
		assertArrayEquals(expectedLon, tour1.longitudeSerie, 0);

		assertArrayEquals(new int[] { 20, 21, 22, 21 }, tour1.temperatureSerie);
		assertArrayEquals(new int[] { 110, 120, 130, 120 }, tour1.pulseSerie);
		assertArrayEquals(new int[] { 40, 50, 60, 50 }, tour1.cadenceSerie);

		// tour2
		final TourData tour2 = tourDataMap.get(Long.valueOf(200011003991L));
		assertEquals(3, tour2.timeSerie.length);
		assertEquals("track2", tour2.getTourTitle()); //$NON-NLS-1$

		final Set<TourWayPoint> tour2WayPoints = tour2.getTourWayPoints();
		assertEquals(2, tour2WayPoints.size());
	}
}
