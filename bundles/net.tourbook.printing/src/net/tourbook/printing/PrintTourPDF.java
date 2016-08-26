package net.tourbook.printing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.data.IXmlSerializable;
import net.tourbook.data.TourData;
import net.tourbook.tour.printing.PrintTourExtension;
import net.tourbook.ui.FileCollisionBehavior;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

/**
 * @author Jo Klaps
 */
public class PrintTourPDF extends PrintTourExtension {

	private static final String	TOURDATA_2_FO_XSL	= "/printing-templates/tourdata2fo.xsl";	//$NON-NLS-1$

	private final FopFactory	_fopFactory			= FopFactory.newInstance();

	private DialogPrintTour		dpt;

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

		final ZonedDateTime dtTourStart = _tourData.getTourStartTime();
		final ZonedDateTime dtTourEnd = dtTourStart.plusSeconds(_tourData.getTourRecordingTime());

		return String.format(
				net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime,
				dtTourStart.format(TimeTools.Formatter_Full_Date),
				dtTourStart.format(TimeTools.Formatter_Medium_Time),
				dtTourEnd.format(TimeTools.Formatter_Medium_Time),
				dtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear()));
	}

	/**
	 * performs the actual PDF generation info and examples at:
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
					canWriteFile = net.tourbook.ui.UI.confirmOverwrite(fileCollisionBehaviour, pdfFile);

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

//				  // debug logging
//				  System.err.println("--------------------------------------------------------");
//				  System.err.println(object.toXml());
//				  System.err.println("--------------------------------------------------------");
//				  XStream xStream = new XStream();
//				  try {
//				  	FileUtils.writeStringToFile(new File("/home/jkl/tourdata_xs.xml"),
//				  	xStream.toXML(object));
//				  } catch (IOException e) {
//				  	e.printStackTrace();
//				  }

				// prepare xsl file for transformation
				final ClassLoader classLoader = getClass().getClassLoader();
				final InputStream xslFile = classLoader.getResourceAsStream(TOURDATA_2_FO_XSL);

				StreamSource xmlSource;
				try {
					xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))); //$NON-NLS-1$
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
				} catch (final IOException e) {
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

		_transformer.setParameter("isPrintMarkers", _printSettings.isPrintMarkers()); //$NON-NLS-1$
		_transformer.setParameter("isPrintDescription", _printSettings.isPrintDescription()); //$NON-NLS-1$

		_transformer.setParameter("paperSize", _printSettings.getPaperSize().toString()); //$NON-NLS-1$
		_transformer.setParameter("paperOrientation", _printSettings.getPaperOrientation().toString()); //$NON-NLS-1$

		_transformer.setParameter("startDate", formatStartDate(_tourData)); //$NON-NLS-1$

		_transformer.setParameter("unitAltitude", new Double(net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)); //$NON-NLS-1$
		_transformer.setParameter("unitDistance", new Double(net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)); //$NON-NLS-1$
		_transformer.setParameter("unitTemperature", net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE); //$NON-NLS-1$
		_transformer.setParameter("unitLabelDistance", UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$
		_transformer.setParameter("unitLabelSpeed", UI.UNIT_LABEL_SPEED); //$NON-NLS-1$
		_transformer.setParameter("unitLabelAltitude", UI.UNIT_LABEL_ALTITUDE); //$NON-NLS-1$
		_transformer.setParameter("unitLabelTemperature", UI.UNIT_LABEL_TEMPERATURE); //$NON-NLS-1$
		_transformer.setParameter("unitLabelHeartBeat", net.tourbook.ui.Messages.Value_Unit_Pulse); //$NON-NLS-1$
		_transformer.setParameter("unitLabelCadence", net.tourbook.ui.Messages.Value_Unit_Cadence); //$NON-NLS-1$
		_transformer.setParameter("unitLabelCalories", net.tourbook.ui.Messages.Value_Unit_Calories); //$NON-NLS-1$
	}

	private void setTranslationParameters(final Transformer _transformer) {

		_transformer.setParameter("lang.Tour_Print_PageTitle", Messages.Tour_Print_PageTitle); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Tour", Messages.Tour_Print_Tour); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Start", Messages.Tour_Print_Start); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Start_Location", Messages.Tour_Print_Start_Location); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_End_Location", Messages.Tour_Print_End_Location); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Time_Distance_Speed", Messages.Tour_Print_Time_Distance_Speed); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Tour_Time", Messages.Tour_Print_Tour_Time); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Tour_Pausing_Time", Messages.Tour_Print_Tour_Pausing_Time); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Tour_Moving_Time", Messages.Tour_Print_Tour_Moving_Time); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Distance", Messages.Tour_Print_Distance); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Maximum_Speed", Messages.Tour_Print_Maximum_Speed); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Personal", Messages.Tour_Print_Personal); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Rest_Pulse", Messages.Tour_Print_Rest_Pulse); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Maximum_Pulse", Messages.Tour_Print_Maximum_Pulse); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Average_Pulse", Messages.Tour_Print_Average_Pulse); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Calories", Messages.Tour_Print_Calories); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Average_Cadence", Messages.Tour_Print_Average_Cadence); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Altitude", Messages.Tour_Print_Altitude); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Highest_Altitude", Messages.Tour_Print_Highest_Altitude); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Meters_Up", Messages.Tour_Print_Meters_Up); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Meters_Down", Messages.Tour_Print_Meters_Down); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_Tour_Markers", Messages.Tour_Print_Tour_Markers); //$NON-NLS-1$
		_transformer.setParameter("lang.Tour_Print_No_Markers_Found", Messages.Tour_Print_No_Markers_Found); //$NON-NLS-1$
	}
}
