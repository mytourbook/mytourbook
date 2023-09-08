/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import net.tourbook.common.UI;
import net.tourbook.database.FIELD_VALIDATION;
import net.tourbook.database.TourDatabase;

@Entity
public class TourTagCategory implements Cloneable, Comparable<Object> {

   private static final char          NL              = UI.NEW_LINE;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       tagCategoryId   = TourDatabase.ENTITY_IS_NOT_SAVED;

   /**
    * Derby does not support BOOLEAN (when this was implemented)
    * <p>
    * <code>1 = true</code><br>
    * <code>0 = false</code>
    * <p>
    */
   private int                        isRoot          = 0;

   @Basic(optional = false)
   private String                     name;

   /**
    * Notes for this tag category
    */
   private String                     notes;                                             // db-version 38

   @ManyToMany(fetch = FetchType.LAZY)
   @JoinTable(joinColumns = @JoinColumn(name = "TOURTAGCATEGORY_TagCategoryID", referencedColumnName = "TagCategoryId"), //
         inverseJoinColumns = @JoinColumn(name = "TOURTAG_TagID", referencedColumnName = "TagId") //
   )
   private final Set<TourTag>         tourTags        = new HashSet<>();

   @ManyToMany(fetch = FetchType.LAZY)
   @JoinTable(joinColumns = @JoinColumn(name = "TOURTAGCATEGORY_TagCategoryID1", referencedColumnName = "TagCategoryId"), //
         inverseJoinColumns = @JoinColumn(name = "TOURTAGCATEGORY_TagCategoryID2", referencedColumnName = "TagCategoryId")//
   )
   private final Set<TourTagCategory> tourTagCategory = new HashSet<>();

   /**
    * Contains the number of categories or <code>-1</code> when the categories are not loaded
    */
   @Transient
   private int                        _numCategories  = -1;

   /**
    * Contains the number of tags or <code>-1</code> when the tags are not loaded
    */
   @Transient
   private int                        _numTags        = -1;

   /**
    * Default constructor used in ejb
    */
   public TourTagCategory() {}

   public TourTagCategory(final String categoryName) {
      name = categoryName;
   }

   @Override
   public TourTagCategory clone() {

      TourTagCategory newTagCategory = null;

      try {
         newTagCategory = (TourTagCategory) super.clone();
      } catch (final CloneNotSupportedException e) {
         e.printStackTrace();
      }

      return newTagCategory;
   }

   @Override
   public int compareTo(final Object obj) {

      if (obj instanceof TourTagCategory) {
         final TourTagCategory otherCategory = (TourTagCategory) obj;
         return name.compareTo(otherCategory.name);
      }

      return 0;
   }

   public long getCategoryId() {
      return tagCategoryId;
   }

   public String getCategoryName() {
      return name;
   }

   /**
    * @return Returns notes or an empty string when not available
    */
   public String getNotes() {

      if (notes == null) {
         return UI.EMPTY_STRING;
      }

      return notes;
   }

   public int getNumberOfCategories() {
      return _numCategories;
   }

   public int getNumberOfTags() {
      return _numTags;
   }

   public Set<TourTagCategory> getTagCategories() {
      return tourTagCategory;
   }

   public long getTagCategoryId() {
      return tagCategoryId;
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

      FIELD_VALIDATION fieldValidation;

      /*
       * Check: name
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            name,
            TourTag.DB_LENGTH_NAME,
            Messages.Db_Field_TourTag_Name);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         name = name.substring(0, TourTag.DB_LENGTH_NAME);
      }

      /*
       * Check: notes
       */
      fieldValidation = TourDatabase.isFieldValidForSave(
            notes,
            TourTag.DB_LENGTH_NOTES,
            Messages.Db_Field_TourTag_Notes);

      if (fieldValidation == FIELD_VALIDATION.IS_INVALID) {
         return false;
      } else if (fieldValidation == FIELD_VALIDATION.TRUNCATE) {
         notes = notes.substring(0, TourTag.DB_LENGTH_NOTES);
      }

      return true;
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

   public void setNumberOfCategories(final int numCategories) {

      _numCategories = numCategories;
   }

   public void setNumberOfTags(final int numTags) {

      _numTags = numTags;
   }

   /**
    * Set root flag if this tag is a root item or not
    * <p>
    * 1 = <code>true</code><br>
    * 0 = <code>false</code>
    */
   public void setRoot(final boolean isRoot) {

      this.isRoot = isRoot ? 1 : 0;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourTagCategory" + NL //                        //$NON-NLS-1$

            + "  name          = " + name + NL //              //$NON-NLS-1$
//          + "  tagCategoryId = " + tagCategoryId + NL //     //$NON-NLS-1$
            + "  isRoot        = " + isRoot + NL //            //$NON-NLS-1$
      ;
   }

   /**
    * Updates values from a modified {@link TourTagCategory}
    *
    * @param modifiedTagCategory
    */
   public void updateFromModified(final TourTagCategory modifiedTagCategory) {

      name = modifiedTagCategory.name;
      notes = modifiedTagCategory.notes;
   }

}
