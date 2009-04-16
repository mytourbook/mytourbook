/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FileZip {

	public final static String gunzip(final String gzipName) throws Exception {

		String outFileName = null;
		String gzipEntryName = null;
		try {
			// Open the GZIP file
			final GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(gzipName));

			gzipEntryName = gzipName;
			if (gzipEntryName.indexOf(File.separator) != -1) // delimiter in name
				gzipEntryName = gzipEntryName.substring(gzipEntryName.lastIndexOf(File.separator) + 1);
			if (gzipEntryName.indexOf('.') != -1)
				gzipEntryName = gzipEntryName.substring(0, gzipEntryName.lastIndexOf('.'));

			outFileName = gzipName.substring(0, gzipName.lastIndexOf(File.separator)) + File.separator + gzipEntryName;

			System.out.println("outFileName " + outFileName); //$NON-NLS-1$

			final OutputStream fileOutputStream = new FileOutputStream(outFileName);

			// Transfer bytes from the GZIP file to the output file
			final byte[] buf = new byte[1024];
			int len;
			while ((len = gzipInputStream.read(buf)) > 0) {
				fileOutputStream.write(buf, 0, len);
			}

			fileOutputStream.close();
			gzipInputStream.close();

			return gzipEntryName;

		} catch (final IOException e) {
			System.out.println("gunzip: Error: " + e.getMessage()); //$NON-NLS-1$
			throw (e); // return exception
		}
	}

	public static void main(final String[] args) throws Exception {}

	public final static String unzip(final String zipName) throws Exception {

		String outFileName = null;
		String zipEntryName = null;

		try {
			// Open the ZIP file
			final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipName));
			
			// Get the first entry
			final ZipEntry zipEntry = zipInputStream.getNextEntry();
			if (zipEntry == null) {
				throw new Exception("zipEntry is null");
			}
			
			zipEntryName = zipEntry.getName();
			System.out.println("zipEntryName " + zipEntryName); //$NON-NLS-1$

			if (zipEntryName.indexOf(File.separator) != -1)
				// delimiter in name (e.g. in self created kmz files)
				zipEntryName = zipEntryName.substring(zipEntryName.lastIndexOf(File.separator) + 1);

			outFileName = zipName.substring(0, zipName.lastIndexOf(File.separator)) + File.separator + zipEntryName;

			System.out.println("outFileName " + outFileName); //$NON-NLS-1$

			final OutputStream fileOutputStream = new FileOutputStream(outFileName);

			// Transfer bytes from the ZIP file to the output file
			final byte[] buf = new byte[1024];
			int len;
			while ((len = zipInputStream.read(buf)) > 0) {
				fileOutputStream.write(buf, 0, len);
			}

			fileOutputStream.close();
			zipInputStream.close();

			return zipEntryName;

		} catch (final IOException e) {
			System.out.println("unzip: Error: " + e.getMessage()); //$NON-NLS-1$
			throw(e); // return exception 
		}
	}

	public final static void zip(String fileName, final String zipName) throws Exception {
		try {
			// Compress the file         
			final File file = new File(fileName);
			final FileInputStream fileInputStream = new FileInputStream(file);

			// Create the ZIP file
			final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipName));

			// Add ZIP entry to output stream (Filename only)
			if (fileName.indexOf(File.separator) != -1)
				fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
			zipOutputStream.putNextEntry(new ZipEntry(fileName));

			// Create a buffer for reading the files
			final byte[] buf = new byte[1024];

			// Transfer bytes from the file to the ZIP file
			int len;
			while ((len = fileInputStream.read(buf)) > 0) {
				zipOutputStream.write(buf, 0, len);
			}

			// Complete the entry
			zipOutputStream.closeEntry();
			fileInputStream.close();
			// Complete the ZIP file
			zipOutputStream.close();

		} catch (final IOException e) {
			System.out.println("zip: Error: " + e.getMessage()); //$NON-NLS-1$
			throw (e); // return exception
		}
	}
}
