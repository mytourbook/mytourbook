/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.ContributionItem_Person;
import net.tourbook.common.UI;
import net.tourbook.data.TourPerson;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class PersonManager {

   private static final char              NL   = UI.NEW_LINE;

   private static PersonManager           _instance;

   private static ArrayList<TourPerson>   _people;

   private static final Object            LOCK = new Object();

   private static ContributionItem_Person _personSelector;

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

   private static void getPeopleFromDb() {

      if (_people != null) {
         _people.clear();
      }

      try (final EntityManager em = TourDatabase.getInstance().getEntityManager()) {

         final String sql = UI.EMPTY_STRING

               + "SELECT emTourPerson" + NL //                                               //$NON-NLS-1$
               + " FROM " + TourPerson.class.getSimpleName() + " AS emTourPerson" + NL //    //$NON-NLS-1$
               + " ORDER BY" + NL //                                                         //$NON-NLS-1$
               + "   emTourPerson.lastName," + NL //                                         //$NON-NLS-1$
               + "   emTourPerson.firstName" + NL //                                         //$NON-NLS-1$
         ;

         final TypedQuery<TourPerson> emQuery = em.createQuery(sql, TourPerson.class);

         _people = new ArrayList<>(emQuery.getResultList());
      }
   }

   /**
    * @param personId
    *
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

   public static ContributionItem_Person getPersonSelector() {

      return _personSelector;
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

      if (getTourPeople().isEmpty()) {

         MessageDialog.openInformation(Display.getDefault().getActiveShell(),
               Messages.Dialog_PersonManager_PersonIsNotAvailable_Title,
               Messages.Dialog_PersonManager_PersonIsNotAvailable_Message);

         return false;
      }

      return true;
   }

   public static void refreshPeople() {
      getPeopleFromDb();
   }

   public static void setPersonSelector(final ContributionItem_Person personSelector) {

      _personSelector = personSelector;
   }
}
