package de.byteholder.geoclipse.tests;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.map.GeoPosition;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.byteholder.geoclipse.util.GeoUtils;

/**
 * @author Michael Kanis
 */
public class GpxTrackDistanceTest extends DefaultHandler {

	private final ArrayList<GeoPosition>	points	= new ArrayList<GeoPosition>();

	// ---- main ----
	public static void main(final String[] argv) {
		if (argv.length != 1) {
			System.err.println("Usage: java ExampleSaxEcho MyXmlFile.xml"); //$NON-NLS-1$
			System.exit(1);
		}

		final long startMillis = System.currentTimeMillis();

		try {
			final File f = new File(argv[0]);
			System.out.println("File size: " + f.length() + " bytes"); //$NON-NLS-1$ //$NON-NLS-2$

			// Use an instance of ourselves as the SAX event handler
			final DefaultHandler handler = new GpxTrackDistanceTest();

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
		System.out.println("Number of points: " + points.size()); //$NON-NLS-1$

		double totalDistance = 0d;

		GeoPosition previousPoint = null;
		for (final GeoPosition point : points) {
			if (previousPoint != null) {
				totalDistance += GeoUtils.distance(previousPoint, point);
			}
			previousPoint = point;
		}

		System.out.println("Total distance: " + totalDistance + " km"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void startElement(	final String namespaceURI,
								final String localName,
								final String qName,
								final Attributes attrs) throws SAXException {

		final String eName = ("".equals(localName)) ? qName : localName; //$NON-NLS-1$

		if ("trkpt".equals(eName)) { //$NON-NLS-1$
			if (attrs != null) {
				final String sLat = attrs.getValue("lat"); //$NON-NLS-1$
				final String sLon = attrs.getValue("lon"); //$NON-NLS-1$

				final double lat = Double.parseDouble(sLat);
				final double lon = Double.parseDouble(sLon);

				points.add(new GeoPosition(lat, lon));
			}
		}
	}

	@Override
	public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {}
}
