/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.common.tooltip;

import net.tourbook.common.UI;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/**
 * Implementation for a default tool provider.
 */
public abstract class ToolProviderAdapter implements IToolProvider {

	@Override
	public abstract void createToolUI(final Composite parent);

	@Override
	public Point getInitialLocation() {
		return null;
	}

	@Override
	public Object getToolTipArea() {
		// ignore
		return null;
	}

	@Override
	public String getToolTitle() {
		return UI.EMPTY_STRING;
	}

	@Override
	public boolean isFlexTool() {
		return false;
	}

	@Override
	public void resetInitialLocation() {}

	@Override
	public void setToolTipArea(final Object toolTipArea) {};

	@Override
	public String toString() {
		return String.format(
				"\nToolProviderAdapter\n   isFlexTool()=%s\n   getToolTitle()=%s\n   getToolTipArea()=%s\n", //$NON-NLS-1$
				isFlexTool(),
				getToolTitle(),
				getToolTipArea());
	}

}
