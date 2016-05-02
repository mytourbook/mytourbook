/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;

public class ColumnDefinition implements Cloneable {

	/**
	 * Visible name in the modify dialog.
	 */
	private String						_label;

	/**
	 * every column in a table must have a unique id
	 */
	private String						_columnId;

	/**
	 * visibility status used in the modify dialog, this is used if the dialog is canceled to not
	 * touch the visible status
	 */
	private boolean						_isColumnDisplayed;

	/**
	 * when <code>true</code> the visibility for this column can be changed
	 */
	private boolean						_canModifyVisibility	= true;

	protected int						_style;

	private CellLabelProvider			_cellLabelProvider;
	private ControlListener				_columnControlListener;

	private String						_columnCategory;
	private String						_columnText;
	private String						_columnToolTipText;
	private int							_columnWidth;
	private String						_columnUnit;

	private boolean						_isColumnResizable		= true;
	private boolean						_isColumnMoveable		= true;

	private SelectionAdapter			_columnSelectionListener;
	private int							_createIndex;

	/**
	 * when <code>true</code> this column will be checked in the modify dialog when the default
	 * button is selected
	 */
	private boolean						_isDefaultColumn		= false;

	private int							_defaultColumnWidth;

	/**
	 * column will have the width 0 to be hidden, this is necessary that the first visible column
	 * can be right aligned
	 */
	private boolean						_isColumnHidden			= false;

	private EditingSupport				_editingSupport;

	private ColumnLayoutData			_columnLayoutData;

	/*
	 * Value formatter
	 */

	/** Available value formats */
	private ValueFormat[]				_availableFormats;

	/** Default value format */
	private ValueFormat					_defaultValueFormat_Category;

	/** Default detail value format */
	private ValueFormat					_defaultValueFormat_Detail;

	/** Current value format */
	private ValueFormat					_valueFormat_Category;

	/** Current value formatter */
	private IValueFormatter				_valueFormatter_Category;

	/** Current value format */
	private ValueFormat					_valueFormat_Detail;

	/** Current value formatter */
	private IValueFormatter				_valueFormatter_Detail;

	private final static NumberFormat	_nf0;
	private final static NumberFormat	_nf1;
	private final static NumberFormat	_nf2;

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

	@Override
	public Object clone() throws CloneNotSupportedException {

		final ColumnDefinition clone = (ColumnDefinition) super.clone();

		clone._label = _label;
		clone._columnId = _columnId;
		clone._isColumnDisplayed = _isColumnDisplayed;
		clone._canModifyVisibility = _canModifyVisibility;

		clone._style = _style;

		clone._cellLabelProvider = _cellLabelProvider;
		clone._columnCategory = _columnCategory;
		clone._columnText = _columnText;
		clone._columnToolTipText = _columnToolTipText;
		clone._columnWidth = _columnWidth;
		clone._defaultColumnWidth = _defaultColumnWidth;
		clone._isColumnResizable = _isColumnResizable;

		clone._isColumnMoveable = _isColumnMoveable;
		clone._columnSelectionListener = _columnSelectionListener;

		clone._defaultValueFormat_Category = _defaultValueFormat_Category;
		clone._availableFormats = _availableFormats;
		clone._valueFormat_Category = _valueFormat_Category;
		clone._valueFormatter_Category = _valueFormatter_Category;

		clone._createIndex = _createIndex;

		return clone;
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
	public String getColumnHeaderText() {
		return _columnText;
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

	public SelectionAdapter getColumnSelectionListener() {
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

	public ValueFormat getDefaultValueFormat() {
		return _defaultValueFormat_Category;
	}

	public ValueFormat getDefaultValueFormat_Detail() {
		return _defaultValueFormat_Detail;
	}

	public EditingSupport getEditingSupport() {
		return _editingSupport;
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
	 * @return Returns <code>true</code> when the column is displayed in the UI, otherwise
	 *         <code>false</code>.
	 */
	public boolean isColumnDisplayed() {
		return _isColumnDisplayed;
	}

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
	 */
	public void printValue(final ViewerCell cell, final double value, final boolean isDetail) {

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
	public void printValue(final ViewerCell cell, final long value, final boolean isDetail) {

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

	public void setColumnCategory(final String category) {
		_columnCategory = category;
	}

	/**
	 * Set the text which is displayed in the column header
	 * 
	 * @param text
	 */
	public void setColumnHeaderText(final String text) {
		_columnText = text;
	}

	/**
	 * @param toolTipText
	 *            Text is displayed as tooltip in the column header
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
	 *            This text is displayed in the column modification dialog.
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
		_columnText = text;
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
	 * @param selectionAdapter
	 */
	public void setColumnSelectionListener(final SelectionAdapter selectionAdapter) {
		_columnSelectionListener = selectionAdapter;
	}

	/**
	 * @param columnUnit
	 *            This text is displayed the in the column modification dialog
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
		_columnWidth = columnWidth;
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

	public void setIsColumnDisplayed(final boolean isDisplayed) {
		_isColumnDisplayed = isDisplayed;
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

	/**
	 * Set formats which are used to render the column.
	 * <p>
	 * <b>This must be called after {@link #setColumnHeaderText(String)} that annotations can be
	 * added to the column header text. </b>
	 * 
	 * @param availableFormats
	 * @param defaultCategoryFormat
	 *            When <code>null</code> this format cannot be selected.
	 * @param defaultDetailFormat
	 *            When <code>null</code> this format cannot be selected.
	 * @param columnManager
	 */
	public void setValueFormats(final ValueFormat[] availableFormats,
								final ValueFormat defaultCategoryFormat,
								final ValueFormat defaultDetailFormat,
								final ColumnManager columnManager) {

		_availableFormats = availableFormats;

		_defaultValueFormat_Category = defaultCategoryFormat;
		_defaultValueFormat_Detail = defaultDetailFormat;

		/*
		 * Add annotations to the column header text
		 */
		if (columnManager.isShowColumnAnnotations()) {

			if (_columnText == null) {
				_columnText = UI.EMPTY_STRING;
			}

			if (defaultCategoryFormat != null || defaultDetailFormat != null) {
				_columnText += UI.SPACE1 + Messages.App_Annotation_1;
			}
		}
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
		return "ColumnDefinition [" //$NON-NLS-1$
//				+ ("_label=" + _label + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("_isCheckedInDialog=" + _isCheckedInDialog + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("_columnId=" + _columnId + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("_valueFormat=" + _valueFormat_Category + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("_valueFormat_Detail=" + _valueFormat_Detail + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("_columnWidth=" + _columnWidth + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("_defaultColumnWidth=" + _defaultColumnWidth) //$NON-NLS-1$
				+ "]\n"; //$NON-NLS-1$
	}

}
