/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.ui.UI;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSTrackpoint;
import org.dinopolis.gpstool.gpsinput.GPSWaypoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.gpstool.gpsinput.garmin.GarminWaypoint;
import org.dinopolis.util.text.OneArgumentMessageFormat;
import org.osgi.framework.Version;

/**
 * Exports tours using a Velocity template. 
 */
public class VelocityTourExporter {

	private static final String						ZERO				= "0";												//$NON-NLS-1$

	private static final DecimalFormat				_intFormatter		= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final DecimalFormat				_double2Formatter	= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final DecimalFormat				_double6Formatter	= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final OneArgumentMessageFormat	_stringFormatter	= new OneArgumentMessageFormat("{0}", Locale.US);	//$NON-NLS-1$
	private static final SimpleDateFormat			_dateFormat			= new SimpleDateFormat();

	static {
		_intFormatter.applyPattern("000000"); //$NON-NLS-1$
		_double2Formatter.applyPattern("0.00"); //$NON-NLS-1$
		_double6Formatter.applyPattern("0.0000000"); //$NON-NLS-1$
		_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private final String							_formatTemplate;

	/**
	 * Creates an exporter for the given template. 
	 * 
	 * @param formatTemplate resource path to the Velocity template that defines the target file format
	 */
	public VelocityTourExporter(final String formatTemplate) {
		this._formatTemplate = formatTemplate;
		
		// initialize velocity
		VelocityService.init();
	}

	/**
	 * Exports the tours given as Garmin* objects, which can be converted with {@link TourData2GarminTrack}.
	 */
	public void doExportTour(	final GarminLap lap,
								final ArrayList<GarminTrack> tList,
								final ArrayList<GarminWaypoint> wayPointList,
								final String exportFileName) throws IOException {

		final VelocityContext context = new VelocityContext();

		// set properties in the context
		context.put("tracks", tList); //$NON-NLS-1$

		context.put("printtracks", Boolean.valueOf(true)); //$NON-NLS-1$

		context.put("waypoints", wayPointList); //$NON-NLS-1$
		context.put("printwaypoints", Boolean.valueOf(wayPointList.size() > 0)); //$NON-NLS-1$

		context.put("printroutes", Boolean.valueOf(false)); //$NON-NLS-1$

		// tcx

		final Reader templateReader = new InputStreamReader(this.getClass().getResourceAsStream(_formatTemplate));

		final File exportFile = new File(exportFileName);

		final Writer exportWriter = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(exportFile),
				UI.UTF_8));

		addValuesToContext(context, lap);

		try {
			Velocity.evaluate(context, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$
		} finally {
			exportWriter.close();
		}
	}

	/**
	 * Adds some important values to the velocity context (e.g. date, ...).
	 * 
	 * @param context
	 *            the velocity context holding all the data
	 */
	private void addValuesToContext(final VelocityContext context, final GarminLap lap) {

		context.put("dateformatter", _dateFormat); //$NON-NLS-1$
		context.put("intformatter", _intFormatter); //$NON-NLS-1$
		context.put("double6formatter", _double6Formatter); //$NON-NLS-1$
		context.put("double2formatter", _double2Formatter); //$NON-NLS-1$
		context.put("stringformatter", _stringFormatter); //$NON-NLS-1$

		/*
		 * GPX & TCX fields
		 */

		// current time, date
		final Calendar now = Calendar.getInstance();
		final Date creationDate = now.getTime();
		context.put("creation_date", creationDate); //$NON-NLS-1$

		// lap data
		context.put("lap", lap); //$NON-NLS-1$

		// creator
		final Version version = Activator.getDefault().getVersion();
		context.put("creator", new StringBuilder().append("MyTourbook")//$NON-NLS-1$ //$NON-NLS-2$
				.append(" ")//$NON-NLS-1$
				.append(version.getMajor())
				.append(".") //$NON-NLS-1$
				.append(version.getMinor())
				.append(".") //$NON-NLS-1$
				.append(version.getMicro())
				.append(".") //$NON-NLS-1$
				.append(version.getQualifier())
				.append(" - http://mytourbook.sourceforge.net")//$NON-NLS-1$
				.toString());

		// extent of waypoint, routes and tracks:
		double min_latitude = 90.0;
		double min_longitude = 180.0;
		double max_latitude = -90.0;
		double max_longitude = -180.0;

		final List<?> routes = (List<?>) context.get("routes"); //$NON-NLS-1$
		if (routes != null) {
			final Iterator<?> route_iterator = routes.iterator();
			while (route_iterator.hasNext()) {
				final GPSRoute route = (GPSRoute) route_iterator.next();
				min_longitude = route.getMinLongitude();
				max_longitude = route.getMaxLongitude();
				min_latitude = route.getMinLatitude();
				max_latitude = route.getMaxLatitude();
			}
		}

		final List<?> waypoints = (List<?>) context.get("waypoints"); //$NON-NLS-1$
		if (waypoints != null) {
			final Iterator<?> waypoint_iterator = waypoints.iterator();
			while (waypoint_iterator.hasNext()) {
				final GPSWaypoint waypoint = (GPSWaypoint) waypoint_iterator.next();
				min_longitude = Math.min(min_longitude, waypoint.getLongitude());
				max_longitude = Math.max(max_longitude, waypoint.getLongitude());
				min_latitude = Math.min(min_latitude, waypoint.getLatitude());
				max_latitude = Math.max(max_latitude, waypoint.getLatitude());
			}
		}

		final List<?> tracks = (List<?>) context.get("tracks"); //$NON-NLS-1$
		if (tracks != null) {
			final Iterator<?> track_iterator = tracks.iterator();
			while (track_iterator.hasNext()) {
				final GPSTrack track = (GPSTrack) track_iterator.next();
				min_longitude = Math.min(min_longitude, track.getMinLongitude());
				max_longitude = Math.max(max_longitude, track.getMaxLongitude());
				min_latitude = Math.min(min_latitude, track.getMinLatitude());
				max_latitude = Math.max(max_latitude, track.getMaxLatitude());
			}
		}
		context.put("min_latitude", new Double(min_latitude)); //$NON-NLS-1$
		context.put("min_longitude", new Double(min_longitude)); //$NON-NLS-1$
		context.put("max_latitude", new Double(max_latitude)); //$NON-NLS-1$
		context.put("max_longitude", new Double(max_longitude)); //$NON-NLS-1$

		/*
		 * additional TCX fields
		 */

		// Version
		String pluginMajorVersion = ZERO;
		String pluginMinorVersion = ZERO;
		String pluginMicroVersion = ZERO;
		String pluginQualifierVersion = ZERO;
		if (version != null) {
			pluginMajorVersion = Integer.toString(version.getMajor());
			pluginMinorVersion = Integer.toString(version.getMinor());
			pluginMicroVersion = Integer.toString(version.getMicro());
			pluginQualifierVersion = version.getQualifier();
		}
		context.put("pluginMajorVersion", pluginMajorVersion); //$NON-NLS-1$
		context.put("pluginMinorVersion", pluginMinorVersion); //$NON-NLS-1$
		context.put("pluginMicroVersion", pluginMicroVersion); //$NON-NLS-1$
		context.put("pluginQualifierVersion", pluginQualifierVersion); //$NON-NLS-1$

//		// device infos
//		final String productName = productInfo.getProductName();
//		context.put("devicename", productName.substring(0, productName.indexOf(' '))); //$NON-NLS-1$
//		context.put("productid", UI.EMPTY_STRING + productInfo.getProductId()); //$NON-NLS-1$ //$NON-NLS-2$
//		context.put("devicemajorversion", UI.EMPTY_STRING + (productInfo.getProductSoftware() / 100)); //$NON-NLS-1$ //$NON-NLS-2$
//		context.put("deviceminorversion", UI.EMPTY_STRING + (productInfo.getProductSoftware() % 100)); //$NON-NLS-1$ //$NON-NLS-2$

		// time, heart, cadence, min/max
		Date starttime = null;
		Date endtime = null;
		int heartNum = 0;
		long heartSum = 0;
		int cadNum = 0;
		long cadSum = 0;
		short maximumheartrate = 0;
		double totaldistance = 0;

		for (final Object name : tracks) {
			final GPSTrack track = (GPSTrack) name;
			for (final Iterator<?> wpIter = track.getWaypoints().iterator(); wpIter.hasNext();) {

				final GPSTrackpoint wp = (GPSTrackpoint) wpIter.next();

				// starttime, totaltime
				if (wp.getDate() != null) {
					if (starttime == null) {
						starttime = wp.getDate();
					}
					endtime = wp.getDate();
				}

				if (wp instanceof GarminTrackpointAdapter) {

					final GarminTrackpointAdapter gta = (GarminTrackpointAdapter) wp;

					// averageheartrate, maximumheartrate
					if (gta.hasValidHeartrate()) {
						heartSum += gta.getHeartrate();
						heartNum++;
						if (gta.getHeartrate() > maximumheartrate) {
							maximumheartrate = gta.getHeartrate();
						}
					}

					// averagecadence
					if (gta.hasValidCadence()) {
						cadSum += gta.getCadence();
						cadNum++;
					}

					// totaldistance
					if (gta.hasValidDistance()) {
						totaldistance = gta.getDistance();
					}
				}
			}
		}

		if (starttime != null) {
			context.put("starttime", starttime); //$NON-NLS-1$
		} else {
			context.put("starttime", creationDate); //$NON-NLS-1$
		}

		if ((starttime != null) && (endtime != null)) {
			context.put("totaltime", ((double) endtime.getTime() - starttime.getTime()) / 1000); //$NON-NLS-1$
		} else {
			context.put("totaltime", (double) 0); //$NON-NLS-1$
		}

		context.put("totaldistance", totaldistance); //$NON-NLS-1$

		if (maximumheartrate != 0) {
			context.put("maximumheartrate", maximumheartrate); //$NON-NLS-1$
		}
		if (heartNum != 0) {
			context.put("averageheartrate", heartSum / heartNum); //$NON-NLS-1$
		}

		if (cadNum != 0) {
			context.put("averagecadence", cadSum / cadNum); //$NON-NLS-1$
		}
	}
}
