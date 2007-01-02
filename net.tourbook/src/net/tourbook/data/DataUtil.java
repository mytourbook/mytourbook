/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
/*
 * Author:	Wolfgang Schramm
 * Created: 03.06.2005
 *
 * 
 */

package net.tourbook.data;

import java.io.IOException;
import java.io.RandomAccessFile;


/**
 */
public class DataUtil {

	public static final int		CICLO_TOUR_DATA_START_POS	= 765;
	public static final int		CICLO_RECORD_SIZE			= 40;
	public static final int		CICLO_TOUR_LAST_RECORD_POS	= 81920;
	public static final int		CICLO_CHECKSUM_POS			= 81925;
	public static final short	CICLO_TOUR_HAC4_TIMESLICE	= 20;
	public static final int		CICLO_HAC4_DATA_SIZE		= 81930;


	/**
	 * read an file offset value from the file stream
	 * 
	 * @param fileRawData
	 * @param buffer
	 * @return offset value adjusted to the file position
	 * @throws IOException
	 */
	public static int readFileOffset(RandomAccessFile fileRawData, byte[] buffer) throws IOException {

		fileRawData.read(buffer);

		return (int) ((Integer.parseInt(new String(buffer, 0, 4), 16) * 2.5) + 5);
	}

}
