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
 
public final class DownloadGLOBE extends DownloadResource {

	final static String	addressPraefix	= "http://www.ngdc.noaa.gov/mgg/topo/DATATILES/elev/";	//$NON-NLS-1$

	public static void get(final String remoteName, final String localName) throws Exception {
		get(addressPraefix, remoteName, localName);
	}

//	public final static void get(final String remoteName, final String localName) throws Exception {
//	    
//	      try {
//	         final String address = addressPraefix + remoteName;
//	         System.out.println("load " + address); //$NON-NLS-1$
//	         OutputStream outputStream = null;
//	         InputStream inputStream = null;
//	         try {
//	             final URL url = new URL(address);
//	             outputStream = new BufferedOutputStream(
//	                                new FileOutputStream(localName));
//	             final URLConnection urlConnection = url.openConnection();
//	             inputStream = urlConnection.getInputStream();
//	             final byte[] buffer = new byte[1024];
//	             int numRead;
//	             long numWritten = 0;
//	             while ((numRead = inputStream.read(buffer)) != -1) {
//	             outputStream.write(buffer, 0, numRead);
//	             numWritten += numRead;
//	             }
//	             System.out.println("# Bytes localName = " + numWritten); //$NON-NLS-1$
//	         } catch (final Exception e) {
//	             e.printStackTrace();
//	         } finally {
//	             try {
//	             if (inputStream != null) {
//	                 inputStream.close();
//	             }
//	             if (outputStream != null) {
//	                 outputStream.close();
//	             }
//	             } catch (final IOException ioe) {
//	             ioe.printStackTrace();
//	             }
//	         }
//
//	         System.out.println("get " + remoteName + " -> " + localName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//
//	      } catch (final Exception e) {
//	         System.out.println(e.getMessage());
//	         throw(e);
//	      }
//	   }
}
