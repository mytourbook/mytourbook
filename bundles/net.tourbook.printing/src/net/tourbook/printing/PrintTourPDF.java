/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.printing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.IXmlSerializable;
import net.tourbook.data.TourData;
import net.tourbook.tour.printing.PrintTourExtension;
import net.tourbook.ui.FileCollisionBehavior;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.SAXException;

/**
 * @author Jo Klaps
 */
public class PrintTourPDF extends PrintTourExtension {

   private static final String TOURDATA_2_FO_XSL = "/printing-templates/tourdata2fo.xsl"; //$NON-NLS-1$

   /**
    * Plugin extension constructor
    */
   public PrintTourPDF() {

   }

   /**
    * formats tour startDate and startTime according to the preferences
    *
    * @param tourData
    * @return
    */
   private String formatStartDate(final TourData tourData) {

      final ZonedDateTime dtTourStart = tourData.getTourStartTime();
      final ZonedDateTime dtTourEnd = dtTourStart.plusSeconds(tourData.getTourDeviceTime_Elapsed());

      return String.format(
            net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime,
            dtTourStart.format(TimeTools.Formatter_Date_F),
            dtTourStart.format(TimeTools.Formatter_Time_M),
            dtTourEnd.format(TimeTools.Formatter_Time_M),
            dtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear()));
   }

   /**
    * performs the actual PDF generation info and examples at:
    * http://www.ibm.com/developerworks/xml/library/x-xstrmfo/index.html
    * http://www.ibm.com/developerworks/xml/library/x-xslfo
    *
    * @param object
    * @param printSettings
    * @throws TransformerException
    */
   void printPDF(final IXmlSerializable object, final PrintSettings printSettings)
         throws TransformerException {

      boolean canWriteFile = true;

      // setup PDF outputStream
      final File pdfFile = new File(printSettings.getCompleteFilePath());

      if (pdfFile.exists() && !printSettings.isOverwriteFiles()) {

         // overwrite is not enabled in the UI
         final FileCollisionBehavior fileCollisionBehaviour = new FileCollisionBehavior();
         canWriteFile = net.tourbook.ui.UI.confirmOverwrite(fileCollisionBehaviour, pdfFile);

         if (fileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
            return;
         }
      }

      if (!canWriteFile) {
         return;
      }

      try (final FileOutputStream pdfContentStream = new FileOutputStream(pdfFile);
            BufferedOutputStream pdfContent = new BufferedOutputStream(pdfContentStream)) {

         // setup XML input source
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

         // prepare XSL file for transformation
         final ClassLoader classLoader = getClass().getClassLoader();
         final InputStream xslFile = classLoader.getResourceAsStream(TOURDATA_2_FO_XSL);

         // setup XSL stylesheet source
         final StreamSource xslSource = new StreamSource(xslFile);

         final TransformerFactory transformerFactory = TransformerFactory.newInstance();
         transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, UI.EMPTY_STRING);
         transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, UI.EMPTY_STRING);
         final Transformer transformer = transformerFactory.newTransformer(xslSource);

         // setup FOP
         final FopFactory fopFactory = FopFactory.newInstance(new File(UI.SYMBOL_DOT).toURI());
         final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
         foUserAgent.setProducer(this.getClass().getName());
         final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdfContent);

         setTranslationParameters(transformer);
         setTransformationParameters((TourData) object, transformer, printSettings);

         // perform transformation
         final Result res = new SAXResult(fop.getDefaultHandler());
         final StreamSource xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
         transformer.transform(xmlSource, res);

         if (printSettings.isOpenFile()) {
            // launch the PDF file (will only work if the user has a registered PDF viewer installed)
            Program.launch(printSettings.getCompleteFilePath());
         }

         Util.close(xslFile);
      } catch (final SAXException | IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void printTours(final ArrayList<TourData> tourDataList, final int tourStartIndex, final int tourEndIndex) {

      final DialogPrintTour dpt = new DialogPrintTour(
            Display.getCurrent().getActiveShell(),
            this,
            tourDataList,
            tourStartIndex,
            tourEndIndex);
      dpt.open();

      // hardcoded PDF output path for development
      // final File pdfFile = new File(_printOutputPath, "tourdata_" + System.currentTimeMillis() + ".pdf");
   }

   /**
    * configures parameters used in the XSL transformation
    *
    * @param tourData
    * @param transformer
    * @param printSettings
    */
   private void setTransformationParameters(final TourData tourData,
                                            final Transformer transformer,
                                            final PrintSettings printSettings) {

      transformer.setParameter("isPrintMarkers", printSettings.isPrintMarkers()); //$NON-NLS-1$
      transformer.setParameter("isPrintDescription", printSettings.isPrintDescription()); //$NON-NLS-1$

      transformer.setParameter("paperSize", printSettings.getPaperSize().toString()); //$NON-NLS-1$
      transformer.setParameter("paperOrientation", printSettings.getPaperOrientation().toString()); //$NON-NLS-1$

      transformer.setParameter("startDate", formatStartDate(tourData)); //$NON-NLS-1$

      transformer.setParameter("unitAltitude", Double.valueOf(UI.UNIT_VALUE_ELEVATION)); //$NON-NLS-1$
      transformer.setParameter("unitDistance", Double.valueOf(UI.UNIT_VALUE_DISTANCE)); //$NON-NLS-1$
      transformer.setParameter("unitTemperature", UI.UNIT_VALUE_TEMPERATURE); //$NON-NLS-1$
      transformer.setParameter("unitLabelDistance", UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$
      transformer.setParameter("unitLabelSpeed", UI.UNIT_LABEL_SPEED); //$NON-NLS-1$
      transformer.setParameter("unitLabelAltitude", UI.UNIT_LABEL_ELEVATION); //$NON-NLS-1$
      transformer.setParameter("unitLabelTemperature", UI.UNIT_LABEL_TEMPERATURE); //$NON-NLS-1$
      transformer.setParameter("unitLabelHeartBeat", net.tourbook.ui.Messages.Value_Unit_Pulse); //$NON-NLS-1$
      transformer.setParameter("unitLabelCadence", net.tourbook.ui.Messages.Value_Unit_Cadence); //$NON-NLS-1$
      transformer.setParameter("unitLabelCalories", net.tourbook.ui.Messages.Value_Unit_Calories); //$NON-NLS-1$
   }

   private void setTranslationParameters(final Transformer transformer) {

      transformer.setParameter("lang.Tour_Print_PageTitle", Messages.Tour_Print_PageTitle); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Tour", Messages.Tour_Print_Tour); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Start", Messages.Tour_Print_Start); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Start_Location", Messages.Tour_Print_Start_Location); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_End_Location", Messages.Tour_Print_End_Location); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Time_Distance_Speed", Messages.Tour_Print_Time_Distance_Speed); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Tour_Time", Messages.Tour_Print_Tour_Time); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Tour_Pausing_Time", Messages.Tour_Print_Tour_Pausing_Time); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Tour_Moving_Time", Messages.Tour_Print_Tour_Moving_Time); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Distance", Messages.Tour_Print_Distance); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Maximum_Speed", Messages.Tour_Print_Maximum_Speed); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Personal", Messages.Tour_Print_Personal); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Rest_Pulse", Messages.Tour_Print_Rest_Pulse); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Maximum_Pulse", Messages.Tour_Print_Maximum_Pulse); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Average_Pulse", Messages.Tour_Print_Average_Pulse); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Calories", Messages.Tour_Print_Calories); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Average_Cadence", Messages.Tour_Print_Average_Cadence); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Altitude", Messages.Tour_Print_Altitude); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Highest_Altitude", Messages.Tour_Print_Highest_Altitude); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Meters_Up", Messages.Tour_Print_Meters_Up); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Meters_Down", Messages.Tour_Print_Meters_Down); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_Tour_Markers", Messages.Tour_Print_Tour_Markers); //$NON-NLS-1$
      transformer.setParameter("lang.Tour_Print_No_Markers_Found", Messages.Tour_Print_No_Markers_Found); //$NON-NLS-1$
   }
}
