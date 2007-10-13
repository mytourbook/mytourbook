/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class TourEditorInputFactory implements IElementFactory {

	private static final String	ID_FACTORY	= "net.tourbook.tour.TourEditorInputFactory";	//$NON-NLS-1$

	private static final String	MEMENTO_TOUR_ID	= "tourId"; //$NON-NLS-1$

	public static String getFactoryId() {
		return ID_FACTORY;
	}

	public static void saveState(IMemento memento, TourEditorInput input) {
		memento.putString(MEMENTO_TOUR_ID, Long.toString(input.getTourId()));
	}

	public IAdaptable createElement(IMemento memento) {
		return new TourEditorInput(Long.parseLong(memento.getString(MEMENTO_TOUR_ID)));
	}

}
