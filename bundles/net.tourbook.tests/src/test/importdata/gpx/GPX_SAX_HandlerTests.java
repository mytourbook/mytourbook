/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package importdata.gpx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.device.garmin.GarminTCX_DeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.tour.TourManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

/**
 * GPX device plugin test.
 * <p>
 * Run as "JUnit Plug-in Test", as application "[No Application] - Headless
 * Mode" (Main Tab)
 *
 * @author Norbert Renner
 */
class GPX_SAX_HandlerTests {

	private static SAXParser parser;
	private static DeviceData deviceData;
	private static HashMap<Long, TourData> newlyImportedTours;
	private static HashMap<Long, TourData> alreadyImportedTours;
	private static GarminTCX_DeviceDataReader deviceDataReader;

	/**
	 * Resource path to GPX file, generally available from net.tourbook Plugin
	 * in test/net.tourbook
	 */
	public static final String IMPORT_FILE_PATH = FilesUtils.rootPath + "importdata/gpx/files/test.gpx"; //$NON-NLS-1$

	@BeforeAll
	static void initAll() {
		parser = Initializer.initializeParser();
		deviceData = new DeviceData();
		newlyImportedTours = new HashMap<>();
		alreadyImportedTours = new HashMap<>();
		deviceDataReader = new GarminTCX_DeviceDataReader();
	}

	/**
	 * Regression test. Imports GPX into TourData and checks all values of tour1
	 * and waypoint1.
	 *
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	void testParse() throws SAXException, IOException {

		final String testFilePath = Paths.get(IMPORT_FILE_PATH).toAbsolutePath().toString();
		final File file = new File(testFilePath);
		final InputStream in = new FileInputStream(file);

		final GPX_SAX_Handler handler = new GPX_SAX_Handler(

				IMPORT_FILE_PATH, deviceData, alreadyImportedTours, newlyImportedTours,

				new ImportState_File(), new ImportState_Process(),

				deviceDataReader);

		parser.parse(in, handler);

		final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

		final Set<TourWayPoint> tourWayPoints = tour.getTourWayPoints();

		assertEquals(1, tourWayPoints.size());
		final Iterator<TourWayPoint> iter = tourWayPoints.iterator();

		TourWayPoint waypoint1 = iter.next();
		final String wayPoint = "waypoint1"; //$NON-NLS-1$
		if (!wayPoint.equals(waypoint1.getName())) {
			waypoint1 = iter.next();
		}
		assertEquals(47.5674099, waypoint1.getLatitude());
		assertEquals(9.47741068, waypoint1.getLongitude());
		assertEquals(394f, waypoint1.getAltitude());
		assertEquals(1286706600000L, waypoint1.getTime());
		assertEquals(wayPoint, waypoint1.getName());
		assertEquals(wayPoint, waypoint1.getComment());
		assertEquals(wayPoint, waypoint1.getDescription());

		assertEquals("track1", tour.getTourTitle());//$NON-NLS-1$

		final float[] altitudeSerie = tour.altitudeSerie;
		assertArrayEquals(new float[] { 395, 394, 393, 394 }, altitudeSerie);
		assertEquals("Sunday, October 10, 2010 - 12:00 PM", TourManager.getTourDateTimeFull(tour)); //$NON-NLS-1$
		final Integer timeSerieLength = tour.timeSerie.length;
		assertEquals(4, timeSerieLength);
		final int[] timeSerie = tour.timeSerie;
		assertArrayEquals(new int[] { 0, 600, 1200, 1800 }, timeSerie);

		final double[] latitudeSerie = tour.latitudeSerie;
		assertArrayEquals(new double[] { 47.567517620, 47.585505568, 47.585397817, 47.567409937 }, latitudeSerie);
		final double[] longitudeSerie = tour.longitudeSerie;
		assertArrayEquals(new double[] { 9.450832423, 9.450987056, 9.477574415, 9.477410677 }, longitudeSerie);
		final float[] temperatureSerie = tour.temperatureSerie;
		assertArrayEquals(new float[] { 20, 21, 22, 21 }, temperatureSerie);
		final float[] pulseSerie = tour.pulseSerie;
		assertArrayEquals(new float[] { 110, 120, 130, 120 }, pulseSerie);
		final float[] cadenceSerie = tour.getCadenceSerie();
		assertArrayEquals(new float[] { 40, 50, 60, 50 }, cadenceSerie);
	}
}
