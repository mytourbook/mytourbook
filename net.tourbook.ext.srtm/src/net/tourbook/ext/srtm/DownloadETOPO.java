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
 
public final class DownloadETOPO extends DownloadResource {

	final static String	addressPraefix	= "http://www.ngdc.noaa.gov/mgg/global/relief/ETOPO5/TOPO/ETOPO5/"; //$NON-NLS-1$

	public static void get(final String remoteName, final String localName) throws Exception {
		get(addressPraefix, remoteName, localName);
	}

//   public final static void get(String remoteName, String localName) throws Exception {
//    
//      try {
//         String address = addressPraefix + remoteName;
//         System.out.println("load " + address); //$NON-NLS-1$
//         OutputStream outputStream = null;
//         InputStream inputStream = null;
//         try {
//             URL url = new URL(address);
//             outputStream = new BufferedOutputStream(
//                                new FileOutputStream(localName));
//             URLConnection urlConnection = url.openConnection();
//             inputStream = urlConnection.getInputStream();
//             byte[] buffer = new byte[1024];
//             int numRead;
//             long numWritten = 0;
//             while ((numRead = inputStream.read(buffer)) != -1) {
//             outputStream.write(buffer, 0, numRead);
//             numWritten += numRead;
//             }
//             System.out.println("# Bytes localName = " + numWritten); //$NON-NLS-1$
//         } catch (Exception e) {
//             e.printStackTrace();
//         } finally {
//             try {
//             if (inputStream != null) {
//                 inputStream.close();
//             }
//             if (outputStream != null) {
//                 outputStream.close();
//             }
//             } catch (IOException ioe) {
//             ioe.printStackTrace();
//             }
//         }
//
//         System.out.println("get " + remoteName + " -> " + localName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//
//      } catch (Exception e) {
//         System.out.println(e.getMessage());
//         throw(e);
//      }
//   }   
}
