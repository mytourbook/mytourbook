package net.tourbook.trainingstress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

class GovssTest {
   /**
    * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
    */
   public static final String IMPORT_FILE_PATH = "/net/tourbook/trainingStress/Move_2017_09_30_05_36_06_Trail+running-MtWhitney.gpx"; //$NON-NLS-1$
   @Test
   void testComputeGovss() {
      final InputStream gpx = GovssTest.class.getResourceAsStream(IMPORT_FILE_PATH);

//  final DeviceData deviceData = new DeviceData();
//      final deviceData.
//      final GPXDeviceDataReader deviceDataReader = new GPXDeviceDataReader();
////    final HashMap<Long, TourData> tourDataMap = new HashMap<Long, TourData>();
//      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
//      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
//
//      final GPX_SAX_Handler handler = new GPX_SAX_Handler(
//            deviceDataReader,
//            IMPORT_FILE_PATH,
//            deviceData,
//            alreadyImportedTours,
//            newlyImportedTours);
//
//      SAXParserFactory.newInstance().newSAXParser().parse(gpx, handler);

      assertEquals(2, 2);
   }

}
