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

package net.tourbook.ui.views.rawData;

import net.tourbook.Messages;
import net.tourbook.importdata.RawDataManager;

import org.eclipse.jface.action.Action;

public class ActionDisableChecksumValidation extends Action {

	public ActionDisableChecksumValidation(final RawDataView rawDataView) {
		super(Messages.import_data_action_disable_hac4_checksum_validation, AS_CHECK_BOX);
	}

	@Override
	public void run() {
		RawDataManager.getInstance().setIsChecksumValidation(isChecked() == false);
	}
}
