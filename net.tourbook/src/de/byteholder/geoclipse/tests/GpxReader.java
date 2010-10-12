package de.byteholder.geoclipse.tests;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.byteholder.gpx.Document;
import de.byteholder.gpx.GeoPosition;
import de.byteholder.gpx.Track;
import de.byteholder.gpx.TrackSegment;
import de.byteholder.gpx.Waypoint;

/**
 * @author Michael Kanis
 */
public class GpxReader extends DefaultHandler {

	private final Document	gpxDocument	= new Document();

	private Track			currentTrack;

	private TrackSegment	currentTrackSegment;

	// ---- main ----
	public static void main(final String[] argv) {
		if (argv.length != 1) {
			System.err.println("Usage: java " + GpxReader.class.getSimpleName() + " <file>"); //$NON-NLS-1$ //$NON-NLS-2$
			System.exit(1);
		}

		final long startMillis = System.currentTimeMillis();

		try {
			final File f = new File(argv[0]);
			System.out.println("File size: " + f.length() + " bytes"); //$NON-NLS-1$ //$NON-NLS-2$

			// Use an instance of ourselves as the SAX event handler
			final DefaultHandler handler = new GpxReader();

			// Parse the input with the default (non-validating) parser
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(f, handler);
		} catch (final Exception t) {
			t.printStackTrace();
			System.exit(2);
		}

		final long millis = System.currentTimeMillis() - startMillis;
		System.out.println("Time consumed: " + millis + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// ---- SAX DefaultHandler methods ----
	@Override
	public void startDocument() throws SAXException {}

	@Override
	public void endDocument() throws SAXException {
		System.out.println("Number of tracks: " + gpxDocument.getTracks().size()); //$NON-NLS-1$

//		double totalDistance = 0d;
//		
//		GeoPosition previousPoint = null;
//		for (GeoPosition point : points) {
//			if (previousPoint != null) {
//				totalDistance += GeoUtils.distance(previousPoint, point);
//			}
//			previousPoint = point;
//		}
//		
//		System.out.println("Total distance: " + totalDistance + " km");
	}

	@SuppressWarnings("unused")
	private Waypoint	currentWaypoint;

	@Override
	public void startElement(	final String namespaceURI,
								final String localName,
								final String qName,
								final Attributes attrs) throws SAXException {

		final String eName = ("".equals(localName)) ? qName : localName; //$NON-NLS-1$

		if ("trk".equals(eName)) { //$NON-NLS-1$
			final Track track = new Track();
			gpxDocument.addTrack(track);
			currentTrack = track;
		}

		if ("trkseg".equals(eName)) { //$NON-NLS-1$
			final TrackSegment segment = new TrackSegment();
			currentTrack.addSegment(segment);
			currentTrackSegment = segment;
		}

		if ("trkpt".equals(eName)) { //$NON-NLS-1$
			final String sLat = attrs.getValue("lat"); //$NON-NLS-1$
			final String sLon = attrs.getValue("lon"); //$NON-NLS-1$

			final double lat = Double.parseDouble(sLat);
			final double lon = Double.parseDouble(sLon);

			final GeoPosition position = new GeoPosition(lat, lon);
			final Waypoint waypoint = new Waypoint(position);
			currentTrackSegment.addTrackPoint(waypoint);
			currentWaypoint = waypoint;
		}

		if ("ele".equals(eName)) { //$NON-NLS-1$
//			String sEle = 
//			double ele = Double.parseDouble(sEle);
//
//			currentWaypoint.setElevation(elevation)
		}
	}

	@Override
	public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {

		final String eName = ("".equals(localName)) ? qName : localName; //$NON-NLS-1$

		if ("trk".equals(eName)) { //$NON-NLS-1$
			currentTrack = null;
		}

		if ("trkseg".equals(eName)) { //$NON-NLS-1$
			currentTrackSegment = null;
		}

		if ("trkpt".equals(eName)) { //$NON-NLS-1$
			currentWaypoint = null;
		}
	}
}
