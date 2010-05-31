/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class PersonManager {

	private static PersonManager			_instance;

	private static ArrayList<TourPerson>	_people	= null;

	private static final Object				LOCK	= new Object();

	public static PersonManager getInstance() {

		if (_instance != null) {
			return _instance;
		}

		synchronized (LOCK) {
			// check again
			if (_instance == null) {
				_instance = new PersonManager();
			}
		}

		return _instance;
	}

	@SuppressWarnings("unchecked")
	private static void getPeopleFromDb() {

		if (_people != null) {
			_people.clear();
		}

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		if (em != null) {

			final Query emQuery = em.createQuery(//
					//
					"SELECT TourPerson" //$NON-NLS-1$
							//
							+ (" FROM TourPerson AS TourPerson") //$NON-NLS-1$
							+ (" ORDER BY TourPerson.lastName, TourPerson.firstName")); //$NON-NLS-1$

			_people = (ArrayList<TourPerson>) emQuery.getResultList();

			em.close();
		}
	}

	/**
	 * @param personId
	 * @return Returns the person name (first + last) for the person id or an empty string when
	 *         person is not available
	 */
	public static String getPersonName(final long personId) {

		ArrayList<TourPerson> people = _people;

		if (people != null) {
			people = getTourPeople();
		}

		for (final TourPerson tourPerson : people) {
			if (tourPerson.getPersonId() == personId) {
				return tourPerson.getName();
			}
		}

		return UI.EMPTY_STRING;
	}

	/**
	 * @return Returns all tour people in the db sorted by last/first name
	 */
	public static ArrayList<TourPerson> getTourPeople() {

		if (_people != null) {
			return _people;
		}

		synchronized (LOCK) {
			// check again
			if (_people != null) {
				return _people;
			}
			getPeopleFromDb();
		}

		return _people;
	}

	/**
	 * Checks if a person is available in the application.
	 * 
	 * @return Returns <code>true</code> when a person is available in the applications. When a
	 *         person is not available, an error message is displayed and <code>false</code> is
	 *         returned.
	 */
	public static boolean isPersonAvailable() {

		final boolean isPersonAvailable = getTourPeople().size() > 0;

		if (isPersonAvailable == false) {

			MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
					Messages.Dialog_PersonManager_PersonIsNotAvailable_Title,
					Messages.Dialog_PersonManager_PersonIsNotAvailable_Message);

			return false;
		}

		return true;
	}

	public static void refreshPeople() {
		getPeopleFromDb();
	}
}
