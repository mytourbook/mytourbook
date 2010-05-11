package net.tourbook.printing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import net.tourbook.data.IXmlSerializable;
import net.tourbook.data.TourData;
import net.tourbook.ui.Messages;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PrintTourPDF extends PrintTourExtension {

	private static final String		TOURDATA_2_FO_XSL	= "/printing-templates/tourdata2fo.xsl";

	private final FopFactory		_fopFactory			= FopFactory.newInstance();
	private final String			_printOutputPath	= (Platform.getInstanceLocation().getURL().getPath() + "print-output");
	private final DateTimeFormatter	_dateFormatter		= DateTimeFormat.fullDate();
	private final DateTimeFormatter	_timeFormatter		= DateTimeFormat.shortTime();

	/**
	 * plugin extension constructor
	 */
	public PrintTourPDF() {}

	/**
	 * performs the actual PDF generation
	 * info and examples at:
	 * http://www.ibm.com/developerworks/xml/library/x-xstrmfo/index.html
	 * http://www.ibm.com/developerworks/xml/library/x-xslfo
	 * 
	 * @param object
	 * @param xslFile
	 * @param pdfFile
	 * @throws FileNotFoundException
	 * @throws FOPException
	 * @throws TransformerException
	 */
	public void generatePDF(final IXmlSerializable object, final File xslFile, final File pdfFile)
			throws FileNotFoundException, FOPException, TransformerException {

		FileOutputStream pdfContentStream = null;
		BufferedOutputStream pdfContent = null;
		try {
			// setup pdf outpoutStream
			pdfContentStream = new FileOutputStream(pdfFile);
			pdfContent = new BufferedOutputStream(pdfContentStream);

			// setup xml input source
			final String xml = object.toXml();
			final StreamSource xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes()));

			// setup xsl stylesheet source
			final FileInputStream xslFileStream = new FileInputStream(xslFile);
			final StreamSource xslSource = new StreamSource(xslFileStream);

			// get transformer
			final TransformerFactory tfactory = TransformerFactory.newInstance();
			final Transformer transformer = tfactory.newTransformer(xslSource);

			// setup FOP
			final FOUserAgent foUserAgent = _fopFactory.newFOUserAgent();
			foUserAgent.setProducer(this.getClass().getName());
			final Fop fop = _fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdfContent);

			// setup transformation parameters
			transformer.setParameter("startDate", formatStartDate((TourData) object));

			// perform transformation
			final Result res = new SAXResult(fop.getDefaultHandler());
			transformer.transform(xmlSource, res);

		} finally {
			if (pdfContent != null) {
				try {
					pdfContent.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	@Override
	public void printTours(final ArrayList<TourData> tourDataList, final int tourStartIndex, final int tourEndIndex) {

		new DialogPrintTour(Display.getCurrent().getActiveShell(), this, tourDataList, tourStartIndex, tourEndIndex)
				.open();

		final URL url = this.getClass().getResource(TOURDATA_2_FO_XSL);

		File xslFile = null;
		try {
			URL fileUrl = null;
			try {
				fileUrl = FileLocator.toFileURL(url);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			xslFile = new File(fileUrl.toURI());

		} catch (final URISyntaxException e1) {
			e1.printStackTrace();
		}

		// hardcoded pdf output path for development
		final File pdfFile = new File(_printOutputPath, "tourdata_" + System.currentTimeMillis() + ".pdf");

		for (final TourData tourData : tourDataList) {
			System.out.println("### printing: " + tourData.getTourTitle());
			try {
				generatePDF(tourData, xslFile, pdfFile);
			} catch (final FOPException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final TransformerException e) {
				e.printStackTrace();
			}
		}

		// new PrintDialog(Display.getCurrent().getActiveShell()).open();

	}

	/**
	 * formats tour startDate and startTime according to the preferences
	 * 
	 * @param _tourData
	 * @return
	 */
	private String formatStartDate(final TourData _tourData) {
		final DateTime dtTour = new DateTime(//
				_tourData.getStartYear(),
				_tourData.getStartMonth(),
				_tourData.getStartDay(),
				_tourData.getStartHour(),
				_tourData.getStartMinute(),
				_tourData.getStartSecond(),
				0);

		final int recordingTime = _tourData.getTourRecordingTime();
		final int movingTime = _tourData.getTourDrivingTime();
		final int breakTime = recordingTime - movingTime;

		return new Formatter().format(
				Messages.Tour_Tooltip_Format_DateWeekTime,
				_dateFormatter.print(dtTour.getMillis()),
				_timeFormatter.print(dtTour.getMillis()),
				dtTour.getWeekOfWeekyear()).toString();
	}
}
