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
package net.tourbook.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.events.SelectionAdapter;

public class ColumnDefinition {

	/**
	 * visible name in the modify dialog
	 */
	private String				fLabel;

	/**
	 * every column in a table must have a unique id
	 */
	protected String			fColumnId;

	/**
	 * when <code>true</code> this column is visible in the table
	 */
	private boolean				fIsVisible;

	/**
	 * visibility status in modify dialog
	 */
	private boolean				fIsVisibleInDialog;

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

	public void setLabelProvider(CellLabelProvider cellLabelProvider) {
		fCellLabelProvider = cellLabelProvider;
	}

	public void setText(String text) {
		fColumnText = text;
	}

	public void setToolTipText(String toolTipText) {
		fColumnToolTipText = toolTipText;
	}

	public void setWidth(int columnWidth) {
		fColumnWidth = columnWidth;
	}

	public void addSelectionListener(SelectionAdapter selectionAdapter) {
		fColumnSelectionListener = selectionAdapter;
	}

	public int getStyle() {
		return fStyle;
	}

	public CellLabelProvider getCellLabelProvider() {
		return fCellLabelProvider;
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

	public SelectionAdapter getColumnSelectionListener() {
		return fColumnSelectionListener;
	}

	/**
	 * @param label
	 *        contains the label which is displayed in the column modify dialog
	 */
	public void setLabel(String label) {
		fLabel = label;
	}

	public String getLabel() {
		return fLabel;
	}

	public boolean isVisible() {
		return fIsVisible;
	}

	public void setVisible(boolean isVisible) {
		fIsVisible = isVisible;
	}

	/**
	 * @return Returns <code>true</code> when the visibility of this column can be modified
	 */
	public boolean canModifyVisibility() {
		return fCanModifyVisibility;
	}

	/**
	 * Set status, if the visibility can be changed, when set to <code>false</code> the column is
	 * always visible and can't be hidden
	 * 
	 * @param canModifyVisibility
	 */
	public void setCanModifyVisibility(boolean canModifyVisibility) {
		fCanModifyVisibility = canModifyVisibility;
	}

	public String getColumnId() {
		return fColumnId;
	}

	public boolean isColumnResizable() {
		return fIsColumnResizable;
	}

	/**
	 * By default a column is resizeable
	 * 
	 * @param isResizable
	 */
	public void setColumnResizable(boolean isResizable) {
		fIsColumnResizable = isResizable;
	}

	public boolean isVisibleInDialog() {
		return fIsVisibleInDialog;
	}

	public void setIsVisibleInDialog(boolean isVisibleInDialog) {
		fIsVisibleInDialog = isVisibleInDialog;
	}

	/**
	 * Set the number in which the column was created
	 * 
	 * @param createIndex
	 */
	public void setCreateIndex(int createIndex) {
		fCreateIndex = createIndex;
	}

	public int getCreateIndex() {
		return fCreateIndex;
	}

	public boolean isColumnMoveable() {
		return fIsColumnMoveable;
	}

	public void setIsColumnMoveable(boolean isColumnMovablee) {
		fIsColumnMoveable = isColumnMovablee;
	}

}
