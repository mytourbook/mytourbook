/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package org.dinopolis.gpstool.gpsinput.garmin;

import java.io.IOException;

public class FixedGPSGarminDataProcessor extends GPSGarminDataProcessor {

	class ReaderThread extends GPSGarminDataProcessor.ReaderThread {

		@Override
		public void run() {
			while (!isInterrupted()) {
				try {

					if (logger_packet_.isDebugEnabled()) {
						logger_packet_.debug("waiting for packet..."); //$NON-NLS-1$
					}
					if (in_stream_.available() > 0) {
						final GarminPacket garmin_packet = getPacket();
						if (garmin_packet == null) {
							if (logger_packet_.isDebugEnabled()) {
								logger_packet_.debug("invalid packet received"); //$NON-NLS-1$
							}
						} else {
							if (logger_packet_.isDebugEnabled()) {
								logger_packet_.debug("packet received: " + garmin_packet.getPacketId()); //$NON-NLS-1$
							}
							if (logger_packet_detail_.isDebugEnabled()) {
								logger_packet_detail_.debug("packet details: " + garmin_packet.toString()); //$NON-NLS-1$
							}
							firePacketReceived(garmin_packet);
						}
					} else {
						sleep(200);
					}
				} catch (final NullPointerException npe) {
					// NullPointerException may be thrown/caught when close() is called
					if (!isInterrupted()) {
						throw npe;
					}
				} catch (final IOException ex) {
					ex.printStackTrace();
				} catch (final InterruptedException ex) {
					interrupt();
				}
			}
		}

		/**
		 * Interrupts the reader thread.
		 */
		@Override
		public void stopThread() {
			interrupt();
		}
	}

	public FixedGPSGarminDataProcessor() {
		read_thread_ = new ReaderThread();
		read_thread_.setDaemon(true);
	}

}
