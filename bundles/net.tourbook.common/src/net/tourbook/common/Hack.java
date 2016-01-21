/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

public class Hack {

	/**
	 * Use Java Reflection to call protected Table#setItemHeight(int).
	 * 
	 * @param table
	 *            Tree on which to adjust item height.
	 * @param itemHeight
	 *            height in pixels to allow for each item.
	 */
	public static void setTableItemHeight(final Table table, final int itemHeight) {

		try {

			// Search class hierarchy for protected setter method
			final Method setter = Table.class.getDeclaredMethod("setItemHeight", int.class);//$NON-NLS-1$

			setter.setAccessible(true);
			setter.invoke(table, Integer.valueOf(itemHeight));

		} catch (final NoSuchMethodException e) {
			// ignore
		} catch (final IllegalArgumentException e) {
			// ignore
		} catch (final IllegalAccessException e) {
			// ignore
		} catch (final InvocationTargetException e) {
			// ignore
		}
	}

	/**
	 * Use Java Reflection to call protected Tree#setItemHeight(int).
	 * 
	 * @param tree
	 *            Tree on which to adjust item height.
	 * @param itemHeight
	 *            height in pixels to allow for each item.
	 */
	public static void setTreeItemHeight(final Tree tree, final int itemHeight) {

		try {

			// Search class hierarchy for protected setter method
			final Method setter = Tree.class.getDeclaredMethod("setItemHeight", int.class);//$NON-NLS-1$

			setter.setAccessible(true);
			setter.invoke(tree, Integer.valueOf(itemHeight));

		} catch (final NoSuchMethodException e) {
			// ignore
		} catch (final IllegalArgumentException e) {
			// ignore
		} catch (final IllegalAccessException e) {
			// ignore
		} catch (final InvocationTargetException e) {
			// ignore
		}
	}

}
