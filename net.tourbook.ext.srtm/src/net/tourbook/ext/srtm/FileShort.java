package net.tourbook.ext.srtm;

/*
 * 
 * shorts werden hier von Java big-endian interpretiert
 * => big-endian Variante der Datenfiles verwenden
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

public class FileShort {

	private FileChannel	fileChannel;
	private ShortBuffer	shortBuffer;
	
	/**
	 * is <code>true</code> when the file exists and can be used, otherwise <code>false</code>
	 */
	private boolean		fileExists	= false;
	
	String				zipName;

	public FileShort(final String fileName) throws Exception {
		init(fileName, false);
	}

	public FileShort(final String fileName, final boolean zipFtp) throws Exception {
		init(fileName, zipFtp);
	}

//	private void close() throws Exception {
//		fileChannel.close();
//	}

	public short get(final int index) {
		if (!fileExists) {
//			return (-32767);
			return Short.MIN_VALUE;
		}
		return shortBuffer.get(index);
	}

	private String getZipName(final String fileName) {
		String zipName;
//      Pattern praefixPattern = Pattern.compile("^(.*)\\.+[A-Za-z]{3,4}$");// Praefix + Punkt + Endung
//      Matcher praefixMatcher = praefixPattern.matcher(fileName);
//      if (praefixMatcher.matches())
//         zipName = praefixMatcher.group(1)+".zip";
//      else
		zipName = fileName + ".zip";
		return zipName;
	}

	private void handleError(final String fileName, final Exception e) { // throws Exception{
		fileExists = false;
	}

	private void init(final String fileName, final boolean zipFtp) throws Exception {

		try {
			open(fileName);

		} catch (final FileNotFoundException e1) {

			if (!zipFtp) {
				handleError(fileName, e1);
				return;
			}
			try {
				// zip-File <fileName>.zip per FTP downloaden und entzippen
				zipName = getZipName(fileName);

				transfer(zipName);
				unzip(zipName);
				open(fileName);

			} catch (final Exception e2) {
				handleError(fileName, e2);
			}
		} catch (final Exception e1) { // sonstige Fehler
			handleError(fileName, e1);
		}
	}

	private void open(final String fileName) throws Exception {

		try {

			fileChannel = new FileInputStream(new File(fileName)).getChannel();

			shortBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).asShortBuffer();

		} catch (final Exception e) {

			throw (e);

		}
		
		fileExists = true;
	}

	private void transfer(final String localName) throws Exception {
		final String remoteName = localName.substring(localName.lastIndexOf(File.separator) + 1);
		FileSRTM3FTP.get(remoteName, localName);
	}

	private void unzip(final String zipName) throws Exception {
		FileZip.unzip(zipName);
	}
}
