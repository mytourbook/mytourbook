/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.poi;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class PostSelectionProvider implements IPostSelectionProvider {

   ListenerList<ISelectionChangedListener> postSelectionListeners = new ListenerList<>();
   private ISelection                      currentSelection;

   @Override
   public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
      postSelectionListeners.add(listener);
   }

   @Override
   public void addSelectionChangedListener(final ISelectionChangedListener listener) {}

   @Override
   public ISelection getSelection() {
      return currentSelection;
   }

   @Override
   public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
      postSelectionListeners.remove(listener);
   }

   @Override
   public void removeSelectionChangedListener(final ISelectionChangedListener listener) {}

   @Override
   public void setSelection(final ISelection selection) {

      if (selection == null) {
         return;
      }

      currentSelection = selection;

      final SelectionChangedEvent event = new SelectionChangedEvent(this, currentSelection);

      final Object[] listeners = postSelectionListeners.getListeners();
      for (final Object listener : listeners) {
         final ISelectionChangedListener l = (ISelectionChangedListener) listener;
         SafeRunnable.run(new SafeRunnable() {
            @Override
            public void run() {
               l.selectionChanged(event);
            }
         });
      }
   }
}
