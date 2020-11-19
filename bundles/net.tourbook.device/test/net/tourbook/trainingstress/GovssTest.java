package net.tourbook.trainingstress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourData;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class GovssTest {
   private static IPreferenceStore _prefStore;
   /**
    * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
    */
   public static final String IMPORT_FILE_PATH = "/net/tourbook/trainingStress/Move_2017_09_30_05_36_06_Trail+running-MtWhitney.gpx"; //$NON-NLS-1$
   @Test
   void testComputeGovss() throws SAXException, IOException, ParserConfigurationException {
      final InputStream gpx = GovssTest.class.getResourceAsStream(IMPORT_FILE_PATH);

  final DeviceData deviceData = new DeviceData();
      final GPXDeviceDataReader deviceDataReader = new GPXDeviceDataReader();
      final HashMap<Long, TourData> tourDataMap = new HashMap<>();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      if (_prefStore == null) {
         _prefStore = TourbookPlugin.getDefault().getPreferenceStore();
      }
      TimeTools.setDefaultTimeZone("UTC");
      final GPX_SAX_Handler handler = new GPX_SAX_Handler(
            deviceDataReader,
            IMPORT_FILE_PATH,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours);

      SAXParserFactory.newInstance().newSAXParser().parse(gpx, handler);

      final TourData tour1 = tourDataMap.get(Long.valueOf(201010101205990L));
      tour1.computeComputedValues();
      assertEquals(114, tour1.getGovss());
   }

}
