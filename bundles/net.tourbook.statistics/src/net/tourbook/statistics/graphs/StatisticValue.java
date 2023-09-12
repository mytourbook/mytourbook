/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import net.tourbook.common.UI;

import org.apache.commons.lang3.StringUtils;

public class StatisticValue {

   private static final String COLUMN_SEPARATOR = "\t"; //$NON-NLS-1$

   private String              _head1_Raw;
   private String              _head2_Raw;

   private String              _unitLabel_Raw;

   private String              _valueFormatting_Raw;
   private int                 _valueLength_Raw;

   private String              _head1;
   private String              _head2;
   private String              _valueFormatting;
   private int                 _itemMaxLength;

   /**
    * When <code>true</code>, then the text is left aligned, default is right aligned
    */
   private boolean             _isLeftAlign;

   private boolean             _isSpaceBefore   = true;
   private boolean             _isSpaceAfter    = false;

   private boolean             _isUsePadding    = true;

   /**
    * @param head1
    * @param head2
    * @param unitLabel
    * @param valueFormatting
    * @param valueLength
    */
   public StatisticValue(final String head1, final String head2, final String unitLabel, final String valueFormatting, final int valueLength) {

      _head1_Raw = head1;
      _head2_Raw = head2;

      _unitLabel_Raw = unitLabel;

      _valueFormatting_Raw = valueFormatting;
      _valueLength_Raw = valueLength;

      updateItems();
   }

   public String getHead1() {
      return _head1;
   }

   public String getHead2() {
      return _head2;
   }

   private int getItemMaxLength() {

      int length = 0;

      if (_head1_Raw != null) {
         length = _head1_Raw.length();
      }

      if (_head2_Raw != null) {
         length = Math.max(length, _head2_Raw.length());
      }

      if (_unitLabel_Raw != null) {
         length = Math.max(length,
               _unitLabel_Raw.length()
                     // unit is displayed with parentheses
                     + 2);
      }

      length = Math.max(length, _valueLength_Raw);

      return length;
   }

   public String getPaddedText(final String text) {

      if (_isUsePadding) {

         return _isLeftAlign
               ? StringUtils.rightPad(text, _itemMaxLength, ' ')
               : StringUtils.leftPad(text, _itemMaxLength, ' ');

      } else {

         return text;
      }

   }

   private String getSpaceAfter() {

      if (_isSpaceAfter) {
         return UI.SPACE1;
      } else {
         return UI.EMPTY_STRING;
      }
   }

   private String getSpaceBefore() {

      if (_isSpaceBefore) {
         return UI.SPACE1;
      } else {
         return UI.EMPTY_STRING;
      }
   }

   public String getValueFormatting() {
      return _valueFormatting;
   }

   private void updateItems() {

      _itemMaxLength = getItemMaxLength();

      /*
       * Header line 1
       */
      if (_head1_Raw != null) {
         _head1 = getSpaceBefore() + getPaddedText(_head1_Raw) + getSpaceAfter() + COLUMN_SEPARATOR;
      } else {
         _head1 = getSpaceBefore() + getPaddedText(UI.EMPTY_STRING) + getSpaceAfter() + COLUMN_SEPARATOR;
      }

      /*
       * Header line 2
       */
      if (_head2_Raw != null) {

         if (_unitLabel_Raw != null) {

            _head2 = getSpaceBefore()
                  + getPaddedText(_head2_Raw + UI.SPACE + UI.SYMBOL_BRACKET_LEFT + _unitLabel_Raw + UI.SYMBOL_BRACKET_RIGHT)
                  + getSpaceAfter()
                  + COLUMN_SEPARATOR;

         } else {

            _head2 = getSpaceBefore()
                  + getPaddedText(_head2_Raw)
                  + getSpaceAfter()
                  + COLUMN_SEPARATOR;
         }

      } else {

         if (_unitLabel_Raw != null) {

            _head2 = getSpaceBefore()
                  + getPaddedText(UI.SYMBOL_BRACKET_LEFT + _unitLabel_Raw + UI.SYMBOL_BRACKET_RIGHT)
                  + getSpaceAfter()
                  + COLUMN_SEPARATOR;

         } else {

            _head2 = getSpaceBefore()
                  + getPaddedText(UI.EMPTY_STRING)
                  + getSpaceAfter()
                  + COLUMN_SEPARATOR;
         }
      }

      /*
       * Value line
       */
      final int valuePaddingMax = Math.max(_valueLength_Raw, _itemMaxLength);
      final int valuePaddingLength = valuePaddingMax - _valueLength_Raw;
      final String valuePadding = _isUsePadding
            ? StringUtils.repeat(UI.SPACE, valuePaddingLength)
            : UI.EMPTY_STRING;

      _valueFormatting = getSpaceBefore() + valuePadding + _valueFormatting_Raw + getSpaceAfter() + COLUMN_SEPARATOR;
   }

   /**
    * Set text left aligned, default is right aligned.
    */
   public StatisticValue withLeftAlign() {

      _isLeftAlign = true;

      updateItems();

      return this;
   }

   public StatisticValue withNoPadding() {

      _isUsePadding = false;

      updateItems();

      return this;
   }

   /**
    * Do not set a space before the item, default sets one space before the item.
    */
   public StatisticValue withNoSpaceBefore() {

      _isSpaceBefore = false;

      updateItems();

      return this;
   }

   /**
    * Set a space after the item, default do not set a space.
    */
   public StatisticValue withSpaceAfter() {

      _isSpaceAfter = true;

      updateItems();

      return this;
   }

   public StatisticValue withUnitLabel(final String unitLabel) {

      _unitLabel_Raw = unitLabel;

      updateItems();

      return this;
   }

}
