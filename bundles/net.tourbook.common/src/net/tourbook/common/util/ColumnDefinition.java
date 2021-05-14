/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.text.NumberFormat;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

public class ColumnDefinition implements Cloneable {

   private final static NumberFormat _nf0;
   private final static NumberFormat _nf1;
   private final static NumberFormat _nf2;

   static {

      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1 = NumberFormat.getNumberInstance();
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf2 = NumberFormat.getNumberInstance();
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   /**
    * Visible name in the modify dialog.
    */
   private String                 _label;

   /**
    * every column in a table must have a unique id
    */
   private String                 _columnId;

   /**
    * Visibility status used in the modify dialog, this is used if the dialog is canceled to not
    * touch the visible status
    */
   private boolean                _isColumnChecked;

   /**
    * when <code>true</code> the visibility for this column can be changed
    */
   private boolean                _canModifyVisibility = true;

   protected int                  _style;

   private CellLabelProvider      _cellLabelProvider;
   private NatTable_LabelProvider _natTable_LabelProvider;

   private String                 _columnCategory;
   private String                 _columnHeaderText;
   private String                 _columnToolTipText;

   private String                 _columnUnit;
   private int                    _columnWidth;

   private boolean                _isColumnResizable   = true;
   private boolean                _isColumnMoveable    = true;
   private boolean                _isColumnFreezed;

   private ControlListener        _columnControlListener;
   private SelectionListener      _columnSelectionListener;

   private int                    _createIndex;

   /**
    * when <code>true</code> this column will be checked in the modify dialog when the default
    * button is selected
    */
   private boolean                _isDefaultColumn;

   private int                    _defaultColumnWidth;

   /**
    * The column will have the width of 0 to be hidden, this is necessary that the first visible
    * column can be right aligned
    */
   private boolean                _isColumnHidden;

   private EditingSupport         _editingSupport;

   private ColumnLayoutData       _columnLayoutData;

   /**
    * When <code>true</code> then this column can be sorted in the {@link NatTable}
    */
   private boolean                _canSortColumn;

   /*
    * Value formatter
    */

   /** Available value formats */
   private ValueFormat[]   _availableFormats;

   /** Default value format */
   private ValueFormat     _defaultValueFormat_Category;

   /** Default detail value format */
   private ValueFormat     _defaultValueFormat_Detail;

   /** Current value format */
   private ValueFormat     _valueFormat_Category;

   /** Current value formatter */
   private IValueFormatter _valueFormatter_Category;

   /** Current value format */
   private ValueFormat     _valueFormat_Detail;

   /** Current value formatter */
   private IValueFormatter _valueFormatter_Detail;

   ColumnDefinition(final String columnId, final int style) {

      _columnId = columnId;
      _style = style;
   }

   /**
    * @return Returns <code>true</code> when the visibility of this column can be modified
    */
   public boolean canModifyVisibility() {
      return _canModifyVisibility;
   }

   /**
    * @return <code>true</code> when this column can be sorted in the NatTable
    */
   public boolean canSortColumn() {
      return _canSortColumn;
   }

   @Override
   public Object clone() throws CloneNotSupportedException {

      final ColumnDefinition clone = (ColumnDefinition) super.clone();

// this seems to be not necesary
//
//      clone._label = _label;
//      clone._columnId = _columnId;
//      clone._isColumnDisplayed = _isColumnDisplayed;
//      clone._canModifyVisibility = _canModifyVisibility;
//
//      clone._style = _style;
//
//      clone._cellLabelProvider = _cellLabelProvider;
//      clone._columnCategory = _columnCategory;
//      clone._columnHeaderText = _columnHeaderText;
//      clone._columnToolTipText = _columnToolTipText;
//      clone._columnWidth = _columnWidth;
//      clone._defaultColumnWidth = _defaultColumnWidth;
//      clone._isColumnResizable = _isColumnResizable;
//
//      clone._isColumnMoveable = _isColumnMoveable;
//      clone._columnSelectionListener = _columnSelectionListener;
//
//      clone._defaultValueFormat_Category = _defaultValueFormat_Category;
//      clone._availableFormats = _availableFormats;
//      clone._valueFormat_Category = _valueFormat_Category;
//      clone._valueFormatter_Category = _valueFormatter_Category;
//
//      clone._createIndex = _createIndex;

      return clone;
   }

   /**
    * Do not use the value formatter, this prevents that the column header context menu is displayed
    * with the column formatter.
    * <p>
    * The formatter is set in the column factory but it do not always make sense to have it, e.g.
    * tour data editor time slice columns.
    */
   public void disableValueFormatter() {

      _availableFormats = null;

      _defaultValueFormat_Category = null;
      _defaultValueFormat_Detail = null;

      _valueFormat_Category = null;
      _valueFormat_Detail = null;

      _valueFormatter_Category = null;
      _valueFormatter_Detail = null;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof ColumnDefinition)) {
         return false;
      }
      final ColumnDefinition other = (ColumnDefinition) obj;
      if (_columnId == null) {
         if (other._columnId != null) {
            return false;
         }
      } else if (!_columnId.equals(other._columnId)) {
         return false;
      }
      return true;
   }

   ValueFormat[] getAvailableFormatter() {
      return _availableFormats;
   }

   public CellLabelProvider getCellLabelProvider() {
      return _cellLabelProvider;
   }

   public String getColumnCategory() {
      return _columnCategory;
   }

   public ControlListener getColumnControlListener() {
      return _columnControlListener;
   }

   /**
    * @return Returns the text which is displayed in the column header.
    */
   public String getColumnHeaderText(final ColumnManager columnManager) {

      final StringBuilder sb = new StringBuilder();
      sb.append(_columnHeaderText);

      // add annotations to this text
      if (columnManager.isShowColumnAnnotation_Formatting()
            && (_defaultValueFormat_Category != null || _defaultValueFormat_Detail != null)) {

         if (_columnHeaderText == null) {
            sb.setLength(0);
            sb.append(UI.EMPTY_STRING);
         }

         if (_defaultValueFormat_Category != null || _defaultValueFormat_Detail != null) {
            sb.append(UI.SPACE1 + Messages.Column_Annotation_Formatting);
         }
      }

      if (columnManager.isNatTableColumnManager() && columnManager.isShowColumnAnnotation_Sorting() && _canSortColumn) {
         sb.append(UI.SPACE1 + Messages.Column_Annotation_Sorting);
      }

      /*
       * In the dark theme the right aligned text is just left of the column separator which cannot
       * be set hidden, it looks just awful
       */
      if (columnManager.getColumnViewer() instanceof TreeViewer
            && _style == SWT.TRAIL
            && UI.isDarkTheme()) {

         sb.append(UI.SPACE2);
      }

      return sb.toString();
   }

   public String getColumnHeaderToolTipText() {
      return _columnToolTipText;
   }

   public String getColumnId() {
      return _columnId;
   }

   /**
    * @return Returns the text which is displayed in the column modification dialog.
    */
   public String getColumnLabel() {
      return _label;
   }

   public SelectionListener getColumnSelectionListener() {
      return _columnSelectionListener;
   }

   public int getColumnStyle() {
      return _style;
   }

   public String getColumnUnit() {
      return _columnUnit;
   }

   public ColumnLayoutData getColumnWeightData() {
      return _columnLayoutData;
   }

   public int getColumnWidth() {
      return _columnWidth;
   }

   public int getCreateIndex() {
      return _createIndex;
   }

   public int getDefaultColumnWidth() {
      return _defaultColumnWidth;
   }

   public ValueFormat getDefaultValueFormat_Category() {
      return _defaultValueFormat_Category;
   }

   public ValueFormat getDefaultValueFormat_Detail() {
      return _defaultValueFormat_Detail;
   }

   public EditingSupport getEditingSupport() {
      return _editingSupport;
   }

   /**
    * @return the _natTable_LabelProvider
    */
   public NatTable_LabelProvider getNatTable_LabelProvider() {
      return _natTable_LabelProvider;
   }

   public ValueFormat getValueFormat_Category() {
      return _valueFormat_Category;
   }

   public ValueFormat getValueFormat_Detail() {
      return _valueFormat_Detail;
   }

   public IValueFormatter getValueFormatter() {

      if (_valueFormatter_Category == null) {
         return ColumnManager.getDefaultDefaultValueFormatter();
      }

      return _valueFormatter_Category;
   }

   public IValueFormatter getValueFormatter_Detail() {

      if (_valueFormatter_Detail == null) {
         return ColumnManager.getDefaultDefaultValueFormatter();
      }

      return _valueFormatter_Detail;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_columnId == null) ? 0 : _columnId.hashCode());
      return result;
   }

   /**
    * @return Returns the visibility status used in the modify dialog, this is used if the dialog is
    *         canceled to not touch the visible status.
    */
   public boolean isColumnCheckedInContextMenu() {
      return _isColumnChecked;
   }

   /**
    * @return the _isColumnFreezed
    */
   public boolean isColumnFreezed() {
      return _isColumnFreezed;
   }

   /**
    * @return Returns <code>true</code> when it is displayed but the width is 0, this is necessary
    *         that the first visiblecolumn can be right aligned in a {@link Table} or {@link Tree}.
    */
   public boolean isColumnHidden() {
      return _isColumnHidden;
   }

   /**
    * @return Returns <code>true</code> when the column is movable, default is <code>true</code>.
    */
   public boolean isColumnMoveable() {
      return _isColumnMoveable;
   }

   public boolean isColumnResizable() {
      return _isColumnResizable;
   }

   /**
    * @return Returns <code>true</code> when this is a default column
    */
   public boolean isDefaultColumn() {
      return _isDefaultColumn;
   }

   /**
    * Print double value with the detail value formatter.
    *
    * @param cell
    * @param value
    */
   public void printDetailValue(final ViewerCell cell, final double value) {

      if (value == 0) {

         cell.setText(UI.EMPTY_STRING);

      } else {

         cell.setText(getValueFormatter_Detail().printDouble(value));
      }
   }

   /**
    * Print long value with the detail value formatter.
    *
    * @param cell
    * @param value
    * @param isDetail
    */
   public void printDetailValue(final ViewerCell cell, final long value) {

      if (value == 0) {

         cell.setText(UI.EMPTY_STRING);

      } else {

         cell.setText(getValueFormatter_Detail().printLong(value));
      }
   }

   /**
    * Print double value with a value formatter.
    *
    * @param cell
    * @param value
    * @param isDetail
    * @return
    */
   public String printDoubleValue(final double value) {

      if (value == 0) {

         return UI.EMPTY_STRING;

      } else {

         return getValueFormatter_Detail().printDouble(value);
      }
   }

   /**
    * Print double value with a value formatter.
    *
    * @param cell
    * @param value
    * @param isDetail
    */
   public void printDoubleValue(final ViewerCell cell, final double value, final boolean isDetail) {

      if (value == 0) {

         cell.setText(UI.EMPTY_STRING);

      } else if (isDetail) {

         cell.setText(getValueFormatter_Detail().printDouble(value));

      } else {

         cell.setText(getValueFormatter().printDouble(value));
      }
   }

   /**
    * Print long value with a value formatter.
    *
    * @param cell
    * @param value
    * @param isDetail
    */
   public String printLongValue(final long value) {

      if (value == 0) {

         return UI.EMPTY_STRING;

      } else {

         return getValueFormatter_Detail().printLong(value);
      }
   }

   /**
    * Print long value with a value formatter.
    *
    * @param cell
    * @param value
    * @param isDetail
    */
   public void printLongValue(final ViewerCell cell, final long value, final boolean isDetail) {

      if (value == 0) {

         cell.setText(UI.EMPTY_STRING);

      } else if (isDetail) {

         cell.setText(getValueFormatter_Detail().printLong(value));

      } else {

         cell.setText(getValueFormatter().printLong(value));
      }
   }

   /**
    * Print double value without fraction digits.
    *
    * @param cell
    * @param value
    */
   public String printValue_0(final double value) {

      if (value == 0) {
         return UI.EMPTY_STRING;
      } else {
         return _nf0.format(value);
      }
   }

   /**
    * Print double value without fraction digits.
    *
    * @param cell
    * @param value
    */
   public void printValue_0(final ViewerCell cell, final double value) {

      if (value == 0) {
         cell.setText(UI.EMPTY_STRING);
      } else {
         cell.setText(_nf0.format(value));
      }
   }

   /**
    * Set status, if the visibility can be changed, when set to <code>false</code> the column is
    * always visible and can't be hidden, default is <code>true</code>
    *
    * @param canModifyVisibility
    */
   public void setCanModifyVisibility(final boolean canModifyVisibility) {
      _canModifyVisibility = canModifyVisibility;
   }

   /**
    * @param canSortColumn
    *           the _canSortColumn to set
    */
   public void setCanSortColumn(final boolean canSortColumn) {
      _canSortColumn = canSortColumn;
   }

   public void setColumnCategory(final String category) {
      _columnCategory = category;
   }

   /**
    * Set the text which is displayed in the column header
    *
    * @param text
    */
   public void setColumnHeaderText(final String text) {
      _columnHeaderText = text;
   }

   /**
    * @param toolTipText
    *           Text is displayed as tooltip in the column header
    */
   public void setColumnHeaderToolTipText(final String toolTipText) {
      _columnToolTipText = toolTipText;
   }

   /**
    * Overwrite default column ID which is set in the constructor.
    *
    * @param columnId
    */
   public void setColumnId(final String columnId) {
      _columnId = columnId;
   }

   /**
    * @param label
    *           This text is displayed in the column modification dialog.
    */
   public void setColumnLabel(final String label) {
      _label = label;
   }

   /**
    * Set text which is displayed in the column header and in the column modification dialog.
    * <p>
    * This will replace calling {@link #setColumnLabel(String)} and
    * {@link #setColumnHeaderText(String)} when the same text is displayed.
    *
    * @param text
    */
   public void setColumnName(final String text) {

      _label = text;
      _columnHeaderText = text;
   }

   /**
    * By default a column is resizeable
    *
    * @param isResizable
    */
   public void setColumnResizable(final boolean isResizable) {
      _isColumnResizable = isResizable;
   }

   /**
    * Add listener which is called when the column header is selected, this is mainly used to sort
    * columns.
    *
    * @param selectionListener
    */
   public void setColumnSelectionListener(final SelectionListener selectionListener) {
      _columnSelectionListener = selectionListener;
   }

   /**
    * @param columnUnit
    *           This text is displayed the in the column modification dialog
    */
   public void setColumnUnit(final String columnUnit) {
      _columnUnit = columnUnit;
   }

   public void setColumnWeightData(final ColumnLayoutData layoutData) {

      _columnLayoutData = layoutData;

      if (layoutData instanceof ColumnPixelData) {
         // keep the default width
         _defaultColumnWidth = ((ColumnPixelData) layoutData).width;
      }
   }

   public void setColumnWidth(final int columnWidth) {

      _columnWidth = Math.min(columnWidth, ColumnManager.COLUMN_WIDTH_MAXIMUM);
   }

   public void setControlListener(final ControlListener controlListener) {
      _columnControlListener = controlListener;
   }

   /**
    * Set the number in which the column was created
    *
    * @param createIndex
    */
   public void setCreateIndex(final int createIndex) {
      _createIndex = createIndex;
   }

   public void setDefaultColumnWidth(final int defaultColumnWidth) {
      // set the default width
      _defaultColumnWidth = defaultColumnWidth;
   }

   public void setEditingSupport(final EditingSupport editingSupport) {
      _editingSupport = editingSupport;
   }

   /**
    * Hidden columns will be displayed with the width 0
    */
   public void setHideColumn() {
      _isColumnHidden = true;
   }

   public void setIsColumnChecked(final boolean isChecked) {
      _isColumnChecked = isChecked;
   }

   /**
    * @param _isColumnFreezed
    *           the _isColumnFreezed to set
    */
   public void setIsColumnFreezed(final boolean _isColumnFreezed) {
      this._isColumnFreezed = _isColumnFreezed;
   }

   public void setIsColumnMoveable(final boolean isColumnMovablee) {
      _isColumnMoveable = isColumnMovablee;
   }

   /**
    * Sets the column as default column, a column will be checked in the modify dialog when the
    * default button is selected, default is <code>false</code>
    */
   public void setIsDefaultColumn() {
      _isDefaultColumn = true;
   }

   public void setLabelProvider(final CellLabelProvider cellLabelProvider) {
      _cellLabelProvider = cellLabelProvider;
   }

   public void setLabelProvider_NatTable(final NatTable_LabelProvider natTable_LabelProvider) {
      _natTable_LabelProvider = natTable_LabelProvider;
   }

   /**
    * Set formats which are used to render the column.
    * <p>
    * <b>This must be called after {@link #setColumnHeaderText(String)} that annotations can be
    * added to the column header text. </b>
    *
    * @param availableFormats
    * @param defaultDetailFormat
    *           When <code>null</code> this format cannot be selected.
    * @param columnManager
    */
   public void setValueFormats(final ValueFormat[] availableFormats,
                               final ValueFormat defaultDetailFormat,
                               final ColumnManager columnManager) {

      setValueFormats(availableFormats, null, defaultDetailFormat, columnManager);
   }

   /**
    * Set formats which are used to render the column.
    * <p>
    * <b>This must be called after {@link #setColumnHeaderText(String)} that annotations can be
    * added to the column header text. </b>
    *
    * @param availableFormats
    * @param defaultCategoryFormat
    *           When <code>null</code> this format cannot be selected.
    * @param defaultDetailFormat
    *           When <code>null</code> this format cannot be selected.
    * @param columnManager
    */
   public void setValueFormats(final ValueFormat[] availableFormats,
                               final ValueFormat defaultCategoryFormat,
                               final ValueFormat defaultDetailFormat,
                               final ColumnManager columnManager) {

      _availableFormats = availableFormats;

      _defaultValueFormat_Category = defaultCategoryFormat;
      _defaultValueFormat_Detail = defaultDetailFormat;
   }

   void setValueFormatter_Category(final ValueFormat valueFormat, final IValueFormatter valueFormatter) {

      _valueFormat_Category = valueFormat;
      _valueFormatter_Category = valueFormatter;
   }

   void setValueFormatter_Detail(final ValueFormat valueFormat, final IValueFormatter valueFormatter) {

      _valueFormat_Detail = valueFormat;
      _valueFormatter_Detail = valueFormatter;
   }

   @Override
   public String toString() {

// SET_FORMATTING_OFF
      return "ColumnDefinition [" //$NON-NLS-1$

//				+ "_label="                + _label                + ", "   //$NON-NLS-1$ //$NON-NLS-2$
            + "_isDefaultColumn="      + String.format("%-5s", Boolean.toString(_isDefaultColumn))    + ", "   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "_isColumnChecked="      + String.format("%-5s", Boolean.toString(_isColumnChecked))    + ", "   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "_columnId="             + String.format("%-40s", _columnId)                            + ", "   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//          + "_valueFormat="          + _valueFormat_Category + ", "   //$NON-NLS-1$ //$NON-NLS-2$
//          + "_valueFormat_Detail="   + _valueFormat_Detail   + ", "   //$NON-NLS-1$ //$NON-NLS-2$
				+ "_columnWidth="          + _columnWidth          + ", "   //$NON-NLS-1$ //$NON-NLS-2$
//				+ "_defaultColumnWidth="   + _defaultColumnWidth            //$NON-NLS-1$

            + "]\n"; //$NON-NLS-1$

// SET_FORMATTING_ON
   }

}
