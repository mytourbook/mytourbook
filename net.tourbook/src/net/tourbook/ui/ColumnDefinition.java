/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.events.SelectionAdapter;

public class ColumnDefinition implements Cloneable {

	/**
	 * visible name in the modify dialog
	 */
	private String				fLabel;

	/**
	 * every column in a table must have a unique id
	 */
	protected String			fColumnId;

	/**
	 * visibility status used in the modify dialog, this is used if the dialog is canceled to not
	 * touch the visible status
	 */
	private boolean				fIsCheckedInDialog;

	/**
	 * when <code>true</code> the visibility for this column can be changed
	 */
	private boolean				fCanModifyVisibility	= true;

	protected int				fStyle;

	private CellLabelProvider	fCellLabelProvider;
	private String				fColumnText;
	private String				fColumnToolTipText;
	private int					fColumnWidth;
	private boolean				fIsColumnResizable		= true;

	private boolean				fIsColumnMoveable		= true;
	private SelectionAdapter	fColumnSelectionListener;

	private int					fCreateIndex;

	/**
	 * when <code>true</code> this column will be checked in the modify dialog when the default
	 * button is selected
	 */
	private boolean				fIsDefaultColumn		= false;

	private int					fDefaultColumnWidth;

	/**
	 * column will have the width 0 to be hidden, this is necessary that the first visible column
	 * can be right aligned
	 */
	private boolean				fIsColumnHidden			= false;

	public void addSelectionListener(final SelectionAdapter selectionAdapter) {
		fColumnSelectionListener = selectionAdapter;
	}

	/**
	 * @return Returns <code>true</code> when the visibility of this column can be modified
	 */
	public boolean canModifyVisibility() {
		return fCanModifyVisibility;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final ColumnDefinition clone = (ColumnDefinition) super.clone();

		clone.fLabel = fLabel;
		clone.fColumnId = fColumnId;
		clone.fIsCheckedInDialog = fIsCheckedInDialog;
		clone.fCanModifyVisibility = fCanModifyVisibility;

		clone.fStyle = fStyle;

		clone.fCellLabelProvider = fCellLabelProvider;
		clone.fColumnText = fColumnText;
		clone.fColumnToolTipText = fColumnToolTipText;
		clone.fColumnWidth = fColumnWidth;
		clone.fDefaultColumnWidth = fDefaultColumnWidth;
		clone.fIsColumnResizable = fIsColumnResizable;

		clone.fIsColumnMoveable = fIsColumnMoveable;
		clone.fColumnSelectionListener = fColumnSelectionListener;

		clone.fCreateIndex = fCreateIndex;

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
		if (fColumnId == null) {
			if (other.fColumnId != null) {
				return false;
			}
		} else if (!fColumnId.equals(other.fColumnId)) {
			return false;
		}
		return true;
	}

	public CellLabelProvider getCellLabelProvider() {
		return fCellLabelProvider;
	}

	public String getColumnId() {
		return fColumnId;
	}

	public String getColumnLabel() {
		return fLabel;
	}

	public SelectionAdapter getColumnSelectionListener() {
		return fColumnSelectionListener;
	}

	public int getColumnStyle() {
		return fStyle;
	}

	public String getColumnText() {
		return fColumnText;
	}

	public String getColumnToolTipText() {
		return fColumnToolTipText;
	}

	public int getColumnWidth() {
		return fColumnWidth;
	}

	public int getCreateIndex() {
		return fCreateIndex;
	}

	public int getDefaultColumnWidth() {
		return fDefaultColumnWidth;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fColumnId == null) ? 0 : fColumnId.hashCode());
		return result;
	}

	public boolean isCheckedInDialog() {
		return fIsCheckedInDialog;
	}

	public boolean isColumnHidden() {
		return fIsColumnHidden;
	}

	public boolean isColumnMoveable() {
		return fIsColumnMoveable;
	}

	public boolean isColumnResizable() {
		return fIsColumnResizable;
	}

	/**
	 * @return Returns <code>true</code> when this is a default column
	 */
	public boolean isDefaultColumn() {
		return fIsDefaultColumn;
	}

	/**
	 * Set status, if the visibility can be changed, when set to <code>false</code> the column is
	 * always visible and can't be hidden, default is <code>true</code>
	 * 
	 * @param canModifyVisibility
	 */
	public void setCanModifyVisibility(final boolean canModifyVisibility) {
		fCanModifyVisibility = canModifyVisibility;
	}

	/**
	 * @param label
	 *            contains the label which is displayed in the column modify dialog
	 */
	public void setColumnLabel(final String label) {
		fLabel = label;
	}

	/**
	 * By default a column is resizeable
	 * 
	 * @param isResizable
	 */
	public void setColumnResizable(final boolean isResizable) {
		fIsColumnResizable = isResizable;
	}

	/**
	 * Set the text which is displayed in the column header
	 * 
	 * @param text
	 */
	public void setColumnText(final String text) {
		fColumnText = text;
	}

	/**
	 * Set the text which is displayed as tooltip in the column header
	 * 
	 * @param toolTipText
	 */
	public void setColumnToolTipText(final String toolTipText) {
		fColumnToolTipText = toolTipText;
	}

	public void setColumnWidth(final int columnWidth) {
		fColumnWidth = columnWidth;
	}

	/**
	 * Set the number in which the column was created
	 * 
	 * @param createIndex
	 */
	public void setCreateIndex(final int createIndex) {
		fCreateIndex = createIndex;
	}

	public void setDefaultColumnWidth(final int defaultColumnWidth) {
		fDefaultColumnWidth = defaultColumnWidth;
	}

	/**
	 * Hidden columns will be displayed with the width 0
	 */
	public void setHideColumn() {
		fIsColumnHidden = true;
	}

	public void setIsCheckedInDialog(final boolean isCheckedInDialog) {
		fIsCheckedInDialog = isCheckedInDialog;
	}

	public void setIsColumnMoveable(final boolean isColumnMovablee) {
		fIsColumnMoveable = isColumnMovablee;
	}

	public void setIsDefaultColumn() {
		this.fIsDefaultColumn = true;
	}

	public void setLabelProvider(final CellLabelProvider cellLabelProvider) {
		fCellLabelProvider = cellLabelProvider;
	}

	@Override
	public String toString() {
		return fColumnId + ":" + fColumnWidth + " (" + fDefaultColumnWidth + ")";
	}

}
