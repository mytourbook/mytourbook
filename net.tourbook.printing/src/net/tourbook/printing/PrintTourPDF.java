package net.tourbook.printing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import org.apache.xmlgraphics.util.MimeConstants;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.program.Program;
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

	private DialogPrintTour			dpt;

	/**
	 * plugin extension constructor
	 */
	public PrintTourPDF() {

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
	public void printPDF(final IXmlSerializable object, final PrintSettings printSettings)
			throws FileNotFoundException, FOPException, TransformerException {
		boolean canWriteFile = true;
		FileOutputStream pdfContentStream = null;
		BufferedOutputStream pdfContent = null;
		try {
			// setup pdf outpoutStream
			final File pdfFile = new File(printSettings.getCompleteFilePath());

			if (pdfFile.exists()) {
				if (printSettings.isOverwriteFiles()) {
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

				/*
				 * debug logging
				 * System.err.println("--------------------------------------------------------");
				 * System.err.println(object.toXml());
				 * System.err.println("--------------------------------------------------------");
				 * XStream xStream = new XStream();
				 * try {
				 * FileUtils.writeStringToFile(new File("/home/jkl/tourdata_xs.xml"),
				 * xStream.toXML(object));
				 * } catch (IOException e) {
				 * e.printStackTrace();
				 * }
				 */

				// prepare xsl file for transformation
				final ClassLoader classLoader = getClass().getClassLoader();
				final InputStream xslFile = classLoader.getResourceAsStream(TOURDATA_2_FO_XSL);		

				
				StreamSource xmlSource;
				try {
					xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
				} catch (final UnsupportedEncodingException e) {
					//if UTF-8 fails, try default encoding
					xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
					e.printStackTrace();
				}

				// setup xsl stylesheet source
				final StreamSource xslSource = new StreamSource(xslFile);

				// get transformer
				final TransformerFactory tfactory = TransformerFactory.newInstance();
				final Transformer transformer = tfactory.newTransformer(xslSource);

				// setup FOP
				final FOUserAgent foUserAgent = _fopFactory.newFOUserAgent();
				foUserAgent.setProducer(this.getClass().getName());
				final Fop fop = _fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdfContent);

				setTranslationParameters(transformer);
				setTransformationParameters((TourData) object, transformer, printSettings);

				// perform transformation
				final Result res = new SAXResult(fop.getDefaultHandler());
				transformer.transform(xmlSource, res);

				// launch the pdf file (will only work if the user has a registered pdf viewer installed)
				Program.launch(printSettings.getCompleteFilePath());
				
				try {
					xslFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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

	@Override
	public void printTours(final ArrayList<TourData> tourDataList, final int tourStartIndex, final int tourEndIndex) {

		dpt = new DialogPrintTour(
				Display.getCurrent().getActiveShell(),
				this,
				tourDataList,
				tourStartIndex,
				tourEndIndex);
		dpt.open();

		// hardcoded pdf output path for development
		// final File pdfFile = new File(_printOutputPath, "tourdata_" + System.currentTimeMillis() + ".pdf");
	}

	/**
	 * configures parameters used in the xsl transformation
	 * 
	 * @param _tourData
	 * @param _transformer
	 */
	private void setTransformationParameters(	final TourData _tourData,
												final Transformer _transformer,
												final PrintSettings _printSettings) {
		_transformer.setParameter("isPrintMarkers", _printSettings.isPrintMarkers());
		_transformer.setParameter("isPrintDescription", _printSettings.isPrintDescription());

		_transformer.setParameter("paperSize", _printSettings.getPaperSize().toString());
		_transformer.setParameter("paperOrientation", _printSettings.getPaperOrientation().toString());

		_transformer.setParameter("startDate", formatStartDate(_tourData));

		_transformer.setParameter("unitAltitude", new Double(UI.UNIT_VALUE_ALTITUDE));
		_transformer.setParameter("unitDistance", new Double(UI.UNIT_VALUE_DISTANCE));
		_transformer.setParameter("unitTemperature", UI.UNIT_VALUE_TEMPERATURE);
		_transformer.setParameter("unitLabelDistance", UI.UNIT_LABEL_DISTANCE);
		_transformer.setParameter("unitLabelSpeed", UI.UNIT_LABEL_SPEED);
		_transformer.setParameter("unitLabelAltitude", UI.UNIT_LABEL_ALTITUDE);
		_transformer.setParameter("unitLabelTemperature", UI.UNIT_LABEL_TEMPERATURE);
		_transformer.setParameter("unitLabelHeartBeat", Messages.Value_Unit_Pulse);
		_transformer.setParameter("unitLabelCadence", Messages.Value_Unit_Cadence);
		_transformer.setParameter("unitLabelCalories", Messages.Value_Unit_Calories);
	}

	private void setTranslationParameters(final Transformer _transformer) {

		_transformer.setParameter("lang.Tour_Print_Tour", net.tourbook.printing.Messages.Tour_Print_Tour);
		_transformer.setParameter("lang.Tour_Print_Start", net.tourbook.printing.Messages.Tour_Print_Start);
		_transformer.setParameter(
				"lang.Tour_Print_Start_Location",
				net.tourbook.printing.Messages.Tour_Print_Start_Location);
		_transformer.setParameter(
				"lang.Tour_Print_End_Location",
				net.tourbook.printing.Messages.Tour_Print_End_Location);
		_transformer.setParameter(
				"lang.Tour_Print_Time_Distance_Speed",
				net.tourbook.printing.Messages.Tour_Print_Time_Distance_Speed);
		_transformer.setParameter("lang.Tour_Print_Tour_Time", net.tourbook.printing.Messages.Tour_Print_Tour_Time);
		_transformer.setParameter(
				"lang.Tour_Print_Tour_Pausing_Time",
				net.tourbook.printing.Messages.Tour_Print_Tour_Pausing_Time);
		_transformer.setParameter(
				"lang.Tour_Print_Tour_Moving_Time",
				net.tourbook.printing.Messages.Tour_Print_Tour_Moving_Time);
		_transformer.setParameter("lang.Tour_Print_Distance", net.tourbook.printing.Messages.Tour_Print_Distance);
		_transformer.setParameter(
				"lang.Tour_Print_Maximum_Speed",
				net.tourbook.printing.Messages.Tour_Print_Maximum_Speed);
		_transformer.setParameter("lang.Tour_Print_Personal", net.tourbook.printing.Messages.Tour_Print_Personal);
		_transformer.setParameter("lang.Tour_Print_Rest_Pulse", net.tourbook.printing.Messages.Tour_Print_Rest_Pulse);
		_transformer.setParameter(
				"lang.Tour_Print_Maximum_Pulse",
				net.tourbook.printing.Messages.Tour_Print_Maximum_Pulse);
		_transformer.setParameter(
				"lang.Tour_Print_Average_Pulse",
				net.tourbook.printing.Messages.Tour_Print_Average_Pulse);
		_transformer.setParameter("lang.Tour_Print_Calories", net.tourbook.printing.Messages.Tour_Print_Calories);
		_transformer.setParameter(
				"lang.Tour_Print_Average_Cadence",
				net.tourbook.printing.Messages.Tour_Print_Average_Cadence);
		_transformer.setParameter("lang.Tour_Print_Altitude", net.tourbook.printing.Messages.Tour_Print_Altitude);
		_transformer.setParameter(
				"lang.Tour_Print_Highest_Altitude",
				net.tourbook.printing.Messages.Tour_Print_Highest_Altitude);
		_transformer.setParameter("lang.Tour_Print_Meters_Up", net.tourbook.printing.Messages.Tour_Print_Meters_Up);
		_transformer.setParameter("lang.Tour_Print_Meters_Down", net.tourbook.printing.Messages.Tour_Print_Meters_Down);
		_transformer.setParameter(
				"lang.Tour_Print_Tour_Markers",
				net.tourbook.printing.Messages.Tour_Print_Tour_Markers);
		_transformer.setParameter(
				"lang.Tour_Print_No_Markers_Found",
				net.tourbook.printing.Messages.Tour_Print_No_Markers_Found);
	}
}
