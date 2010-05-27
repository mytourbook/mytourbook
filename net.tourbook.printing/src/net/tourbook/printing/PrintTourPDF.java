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
import net.tourbook.ui.FileCollisionBehavior;
import net.tourbook.ui.Messages;
import net.tourbook.ui.UI;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.afp.tools.StringUtils;
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
	private File					_xslFile;

	/**
	 * plugin extension constructor
	 */
	public PrintTourPDF() {
		// prepare xsl file for transformation
		final URL url = this.getClass().getResource(TOURDATA_2_FO_XSL);

		try {
			URL fileUrl = null;
			try {
				fileUrl = FileLocator.toFileURL(url);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			_xslFile = new File(fileUrl.toURI());

		} catch (final URISyntaxException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * performs the actual PDF generation
	 * info and examples at:
	 * http://www.ibm.com/developerworks/xml/library/x-xstrmfo/index.html
	 * http://www.ibm.com/developerworks/xml/library/x-xslfo
	 * 
	 * @param object
	 * @param pdfFile
	 * @throws FileNotFoundException
	 * @throws FOPException
	 * @throws TransformerException
	 */
	public void printPDF(final IXmlSerializable object, final String pdfFilePath, final boolean isPrintMarkers, final boolean isOverwriteFiles)
			throws FileNotFoundException, FOPException, TransformerException {
		boolean canWriteFile = true;
		FileOutputStream pdfContentStream = null;
		BufferedOutputStream pdfContent = null;
		try {
			// setup pdf outpoutStream
			File pdfFile = new File(pdfFilePath);
			
			if (pdfFile.exists()) {
				if (isOverwriteFiles) {
					// overwrite is enabled in the UI
				} else {
					final FileCollisionBehavior fileCollisionBehaviour = new FileCollisionBehavior();
					canWriteFile = UI.confirmOverwrite(fileCollisionBehaviour, pdfFile);
					
					if (fileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
						return;
					}
				}
			}

			if (canWriteFile) {
				
				pdfContentStream = new FileOutputStream(pdfFile);
				pdfContent = new BufferedOutputStream(pdfContentStream);

				// setup xml input source
				final String xml = object.toXml();
							
				
				System.err.println("--------------------------------------------------------");
				System.err.println(object.toXml());
				System.err.println("--------------------------------------------------------");
				
				/* debug logging			
				XStream xStream = new XStream();
				try {
					FileUtils.writeStringToFile(new File("/home/jkl/tourdata_xs.xml"), xStream.toXML(object));
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
				
				final StreamSource xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes()));

				// setup xsl stylesheet source
				final FileInputStream xslFileStream = new FileInputStream(_xslFile);
				final StreamSource xslSource = new StreamSource(xslFileStream);

				// get transformer
				final TransformerFactory tfactory = TransformerFactory.newInstance();
				final Transformer transformer = tfactory.newTransformer(xslSource);

				// setup FOP
				final FOUserAgent foUserAgent = _fopFactory.newFOUserAgent();
				foUserAgent.setProducer(this.getClass().getName());
				final Fop fop = _fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdfContent);

				setTransformationParameters((TourData)object, transformer, isPrintMarkers);
				
				// perform transformation
				final Result res = new SAXResult(fop.getDefaultHandler());
				transformer.transform(xmlSource, res);

			}
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

	/**
	 * configures parameters used in the xsl transformation
	 * @param _tourData
	 * @param _transformer
	 */
	private void setTransformationParameters(final TourData _tourData, final Transformer _transformer, final boolean isPrintMarkers){
		_transformer.setParameter("isPrintMarkers", isPrintMarkers);
		
		_transformer.setParameter("startDate", formatStartDate(_tourData));
		
		_transformer.setParameter("tourTime", (_tourData.getTourRecordingTime() / 3600) + ":" 
				+ StringUtils.lpad(""+((_tourData.getTourRecordingTime() % 3600) / 60), '0', 2) + ":" 
				+ StringUtils.lpad(""+(_tourData.getTourRecordingTime() % 3600) % 60, '0', 2));
		
		_transformer.setParameter("tourDrivingTime", (_tourData.getTourDrivingTime() / 3600) + ":" 
				+ StringUtils.lpad(""+((_tourData.getTourDrivingTime() % 3600) / 60), '0', 2) + ":"
				+ StringUtils.lpad(""+(_tourData.getTourDrivingTime() % 3600) % 60, '0', 2));			
		
		final int tourBreakTime = _tourData.getTourRecordingTime() - _tourData.getTourDrivingTime();
		
		_transformer.setParameter("tourBreakTime", StringUtils.lpad(""+(tourBreakTime / 3600), '0', 1) + ":" 
				+ StringUtils.lpad(""+((tourBreakTime % 3600) / 60), '0', 2) + ":"
				+ StringUtils.lpad(""+(tourBreakTime % 3600) % 60, '0', 2));
		
		_transformer.setParameter("unitAltitude", UI.UNIT_VALUE_ALTITUDE);
		_transformer.setParameter("unitDistance", new Double(UI.UNIT_VALUE_DISTANCE));
		_transformer.setParameter("unitTemperature", UI.UNIT_VALUE_TEMPERATURE);
		_transformer.setParameter("unitLabelDistance", UI.UNIT_LABEL_DISTANCE);
		_transformer.setParameter("unitLabelSpeed", UI.UNIT_LABEL_SPEED);
		_transformer.setParameter("unitLabelAltitude", UI.UNIT_LABEL_ALTITUDE);
		_transformer.setParameter("unitLabelTemperature", UI.UNIT_LABEL_TEMPERATURE);
		_transformer.setParameter("unitLabelHeartBeat", net.tourbook.Messages.Graph_Label_Heartbeat_unit);
	}
	
	@Override
	public void printTours(final ArrayList<TourData> tourDataList, final int tourStartIndex, final int tourEndIndex) {

		new DialogPrintTour(Display.getCurrent().getActiveShell(), this, tourDataList, tourStartIndex, tourEndIndex)
				.open();

		// hardcoded pdf output path for development
		// final File pdfFile = new File(_printOutputPath, "tourdata_" + System.currentTimeMillis() + ".pdf");
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

		return new Formatter().format(
				Messages.Tour_Tooltip_Format_DateWeekTime,
				_dateFormatter.print(dtTour.getMillis()),
				_timeFormatter.print(dtTour.getMillis()),
				dtTour.getWeekOfWeekyear()).toString();
	}
}
