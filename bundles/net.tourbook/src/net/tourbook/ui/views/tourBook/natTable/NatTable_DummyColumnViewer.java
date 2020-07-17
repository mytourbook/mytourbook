/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook.natTable;

import java.util.List;

import net.tourbook.ui.INatTable_TourProvider;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

/**
 * This is a {@link ColumnViewer} implementation for a {@link NatTable} that it can be used where a
 * {@link ColumnViewer} is required. It contains only a small subset of {@link ColumnViewer}
 * methods.
 */
public class NatTable_DummyColumnViewer extends ColumnViewer {

   private INatTable_TourProvider _natTable_TourProvider;

   public NatTable_DummyColumnViewer(final INatTable_TourProvider tourProvider) {
      _natTable_TourProvider = tourProvider;
   }

   @Override
   protected ColumnViewerEditor createViewerEditor() {
      return null;
   }

   @Override
   protected Widget doFindInputItem(final Object element) {
      return null;
   }

   @Override
   protected Widget doFindItem(final Object element) {
      return null;
   }

   @Override
   protected int doGetColumnCount() {
      return 0;
   }

   @Override
   protected void doUpdateItem(final Widget item, final Object element, final boolean fullMap) {}

   @Override
   protected Widget getColumnViewerOwner(final int columnIndex) {
      return null;
   }

   @Override
   public Control getControl() {
      return null;
   }

   @Override
   protected Item getItemAt(final Point point) {
      return null;
   }

   @Override
   protected List<?> getSelectionFromWidget() {
      return null;
   }

   @Override
   public IStructuredSelection getStructuredSelection() throws ClassCastException {

      return new StructuredSelection(_natTable_TourProvider.getSelectedTours());
   }

   @Override
   protected ViewerRow getViewerRowFromItem(final Widget item) {
      return null;
   }

   @Override
   protected void internalRefresh(final Object element) {}

   @Override
   public void reveal(final Object element) {}

   @Override
   protected void setSelectionToWidget(@SuppressWarnings("rawtypes") final List l, final boolean reveal) {}

}
