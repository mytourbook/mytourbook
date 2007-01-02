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

package net.tourbook.dataImport;

import net.tourbook.ext.serialport.IDataListener;
import net.tourbook.ext.serialport.PortListener;
import net.tourbook.ext.serialport.SerialConnectionException;
import net.tourbook.ext.serialport.SerialParameters;

public class PortThread implements Runnable, IDataListener {

	private WizardImportData	fImportWizard;
	private String				portName;

	public PortThread(WizardImportData importWizard, String portName) {
		this.fImportWizard = importWizard;
		this.portName = portName;
	}

	public void run() {

		PortListener portListener = null;

		// open connection
		try {
			SerialParameters portParameters = new SerialParameters();
			portParameters.setPortName(portName);

			portListener = new PortListener(portParameters, this);
			portListener.openConnection();

		} catch (SerialConnectionException e) {
			e.printStackTrace();
		}

		// sleep until this thread gets interrupted
		try {
			while (true) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e2) {
			if (portListener != null) {
				// cleanup resources
				portListener.closeConnection();
			}
		}
	}

	public void dataArrived(StringBuilder newData) {
		fImportWizard.appendReceivedData(newData.toString().getBytes());
	}
}
