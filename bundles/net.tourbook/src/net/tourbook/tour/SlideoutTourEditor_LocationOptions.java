/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor options.
 */
public class SlideoutTourEditor_LocationOptions extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final IDialogSettings _state = TourbookPlugin.getState(TourDataEditorView.ID);

   private DialogQuickEdit              _dialogQuickEdit;

   private boolean                      _isStartLocation;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   public SlideoutTourEditor_LocationOptions(final Control ownerControl,
                                             final ToolBar toolBar,
                                             final DialogQuickEdit dialogQuickEdit,
                                             final boolean isStartLocation) {

      super(ownerControl, toolBar);

      _dialogQuickEdit = dialogQuickEdit;
      _isStartLocation = isStartLocation;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {}
      }

      return _shellContainer;
   }

   @Override
   protected boolean isAlignLeft() {

      return true;
   }

   @Override
   public void resetToDefaults() {

   }

   private void restoreState() {

   }

}
