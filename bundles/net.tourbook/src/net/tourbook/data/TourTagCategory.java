/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

@Entity
public class TourTagCategory implements Comparable<Object> {

   public static final int            DB_LENGTH_NAME   = 255;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       tagCategoryId    = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * derby does not support BOOLEAN, 1 = <code>true</code>, 0 = <code>false</code>
    */
   private int                        isRoot           = 0;

   @Basic(optional = false)
   private String                     name;

   /**
    * Notes for this tag category
    */
   private String                     notes;                                              // db-version 38

   @ManyToMany(fetch = FetchType.LAZY)
   @JoinTable(joinColumns = @JoinColumn(name = "TOURTAGCATEGORY_TagCategoryID", referencedColumnName = "TagCategoryId"), //
         inverseJoinColumns = @JoinColumn(name = "TOURTAG_TagID", referencedColumnName = "TagId") //
   )
   private final Set<TourTag>         tourTags         = new HashSet<>();

   @ManyToMany(fetch = FetchType.LAZY)
   @JoinTable(//
         joinColumns = @JoinColumn(name = "TOURTAGCATEGORY_TagCategoryID1", referencedColumnName = "TagCategoryId"), //
         inverseJoinColumns = @JoinColumn(name = "TOURTAGCATEGORY_TagCategoryID2", referencedColumnName = "TagCategoryId")//
   )
   private final Set<TourTagCategory> tourTagCategory  = new HashSet<>();

   /**
    * contains the number of categories or <code>-1</code> when the categories are not loaded
    */
   @Transient
   private int                        _categoryCounter = -1;

   /**
    * contains the number of tags or <code>-1</code> when the tags are not loaded
    */
   @Transient
   private int                        _tagCounter      = -1;

   /**
    * default constructor used in ejb
    */
   public TourTagCategory() {}

   public TourTagCategory(final String categoryName) {
      name = categoryName;
   }

   @Override
   public int compareTo(final Object obj) {

      if (obj instanceof TourTagCategory) {
         final TourTagCategory otherCategory = (TourTagCategory) obj;
         return name.compareTo(otherCategory.name);
      }

      return 0;
   }

   public int getCategoryCounter() {
      return _categoryCounter;
   }
   public long getCategoryId() {
      return tagCategoryId;
   }

   public String getCategoryName() {
      return name;
   }

   public String getNotes() {
      return notes;
   }

   public Set<TourTagCategory> getTagCategories() {
      return tourTagCategory;
   }

   public int getTagCounter() {
      return _tagCounter;
   }

   /**
    * @return Returns the tags which belong to this category, the tags will be fetched with the
    *         fetch type {@link FetchType#LAZY}
    */
   public Set<TourTag> getTourTags() {
      return tourTags;
   }

   public boolean isRoot() {
      return isRoot == 1;
   }

   /**
    * Checks if VARCHAR fields have the correct length
    *
    * @return Returns <code>true</code> when the data are valid and can be saved
    */
   public boolean isValidForSave() {

      /*
       * Check: notes
       */
      final FIELD_VALIDATION fieldValidation = TourDatabase.isFieldValidForSave(
            notes,
            TourTag.DB_LENGTH_NOTES,
            Messages.Db_Field_TourTagCategory_Notes);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         notes = notes.substring(0, TourTag.DB_LENGTH_NOTES);
      }

      return true;
   }

   public void setCategoryCounter(final int fCategoryCounter) {
      this._categoryCounter = fCategoryCounter;
   }

   /**
    * Set the name for the tag category
    *
    * @param name
    */
   public void setName(final String name) {
      this.name = name;
   }

   public void setNotes(final String notes) {
      this.notes = notes;
   }

   /**
    * set root flag if this tag is a root item or not, 1 = <code>true</code>, 0 =
    * <code>false</code>
    */
   public void setRoot(final boolean isRoot) {
      this.isRoot = isRoot ? 1 : 0;
   }

   public void setTagCounter(final int fTagCounter) {
      this._tagCounter = fTagCounter;
   }

   @Override
   public String toString() {

      final String category = "TourTagCategory" //$NON-NLS-1$

            + "\t" + name //$NON-NLS-1$
            + "\tID:" + tagCategoryId //$NON-NLS-1$
            + "\tisRoot:" + isRoot //$NON-NLS-1$
      ;

      return category;
   }

}
