package net.tourbook.device.suunto;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;

public class Suunto9DeviceDataReaderTests {

	/**
	 * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
	 */
	public static final String		IMPORT_FILE_PATH	= "/test/net/tourbook/device/suunto/testFiles/";

	private Map<String, String>	testFiles			= new HashMap<>();										// Java 7

	/**
	 */
	@Test
	public void testEquals() {

		// City of Rocks, ID
		String filePath = Paths.get(Paths.get(".").toAbsolutePath().toString(),
				IMPORT_FILE_PATH,
				"1537365846902_183010004848_post_timeline-1.json.gz").toAbsolutePath().normalize().toString();
		testFiles.put(
				Paths.get(IMPORT_FILE_PATH, "1537365846902_183010004848_post_timeline-1.xml").toString(),
				Paths.get(filePath).toString());

		// Single file tests
		SuuntoJsonProcessor suuntoJsonProcessor = new SuuntoJsonProcessor();
		for (Map.Entry<String, String> entry : testFiles.entrySet()) {
			TourData tour = suuntoJsonProcessor.ImportActivity(entry.getValue(), null, null);

			CompareAgainstControl(entry.getKey(), tour.toXml());
		}
	}

	private static void CompareAgainstControl(String controlDocumentFilePath,
															String xmlTestDocument) {
		final String control = Util.readContentFromFile(xmlTestDocument);

		Diff myDiff = DiffBuilder.compare(Input.fromString(control))
				.withTest(Input.fromString(xmlTestDocument))
				.build();

		Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
	}

}
