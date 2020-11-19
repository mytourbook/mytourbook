package net.tourbook.trainingstress;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class GovssTest {

   private static TourPerson  tourPerson;
   private static SAXParser   parser;

   /**
    * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
    */
   public static final String IMPORT_FILE_PATH = "/net/tourbook/trainingStress/files/Move_2017_09_30_05_36_06_Trail+running-MtWhitney.gpx"; //$NON-NLS-1$

   @BeforeAll
   static void setUp() throws ParserConfigurationException, SAXException {
      TimeTools.setDefaultTimeZone("UTC");

      tourPerson = new TourPerson();
      tourPerson.setGovssThresholdPower(367);
      tourPerson.setHeight(1.84f);
      tourPerson.setWeight(70);
      tourPerson.setGovssTimeTrialDuration(3600);

      //Xml Parser
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      parser = factory.newSAXParser();
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
   }

   @Test
   void testComputeGovss() throws SAXException, IOException {

      final InputStream gpx = GovssTest.class.getResourceAsStream(IMPORT_FILE_PATH);

      final DeviceData deviceData = new DeviceData();
      final GPXDeviceDataReader deviceDataReader = new GPXDeviceDataReader();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();

      final GPX_SAX_Handler gpxSaxHandler = new GPX_SAX_Handler(
            deviceDataReader,
            IMPORT_FILE_PATH,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours);

      parser.parse(gpx, gpxSaxHandler);

      final TourData tour = newlyImportedTours.get(Long.valueOf(2017930123618648L));

      final Integer govss = new Govss(tourPerson, tour).Compute();
      assert govss.equals(114);
   }
}
