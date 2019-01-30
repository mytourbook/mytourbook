/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolItem;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.tour.filter.TourFilterProfile;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourGeoFilter extends AdvancedSlideout {

   private final IPreferenceStore  _prefStore = TourbookPlugin.getPrefStore();
   private final IDialogSettings   _state;

   private ModifyListener          _defaultModifyListener;
   private FocusListener           _keepOpenListener;
   private IPropertyChangeListener _prefChangeListener;

   {
      _defaultModifyListener = new ModifyListener() {
         @Override
         public void modifyText(final ModifyEvent e) {
//            onProfile_Modify();
         }
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsKeepOpenInternally(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsKeepOpenInternally(false);
         }
      };
   }

   private PixelConverter _pc;

   private TableViewer    _profileViewer;

//   private final ArrayList<TourFilterProfile> _filterProfiles = TourFilterManager.getProfiles();
   private TourFilterProfile _selectedProfile;

   private ToolItem          _tourFilterItem;

   public SlideoutTourGeoFilter(final ToolItem toolItem,
                                final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 900, 200, 900, 200 });

      _state = state;
      _tourFilterItem = toolItem;

      setShellFadeOutDelaySteps(30);
      setTitleText(Messages.Slideout_TourFilter_Label_Title);
   }

   @Override
   protected boolean canCloseShell(final Shell[] openedShells) {

      /*
       * Linux creates a shell in DateTime widget which prevents to close the slideout, accept this
       * "shell".
       */

//      for (final Shell shell : openedShells) {
//
////			Util.dumpChildren(shell, 1);
//
//         for (final Control child : shell.getChildren()) {
//
//            final String controlText = child.toString();
//
////				System.out.println(this.getClass().getName() + "\tcontrolText:" + controlText);
//
//         }
//      }

      return true;
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      /*
       * Reset to a valid state when the slideout is opened again
       */
      _selectedProfile = null;

      initUI(parent);

      createUI(parent);

      // load viewer
//      _profileViewer.setInput(new Object());

      restoreState();
      enableControls();
   }

   private void createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(container);
         GridLayoutFactory.swtDefaults().applyTo(container);
         {}
      }
   }

   private void doLiveUpdate() {

      enableControls();

      fireModifyEvent();
   }

   private void enableControls() {

      final boolean isProfileSelected = _selectedProfile != null;
   }

   private void fireModifyEvent() {

//      TourFilterManager.fireTourFilterModifyEvent();
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _tourFilterItem.getBounds();
      final Point itemDisplayPosition = _tourFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   private boolean isFilterDisposed() {

//      if (_filterOuterContainer != null && _filterOuterContainer.isDisposed()) {
//
//         /*
//          * This can happen when a sub dialog was closed and the mouse is outside of the slideout ->
//          * this is closing the slideout
//          */
//         return true;
//      }

      return false;
   }

   private void onDisposeSlideout() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);
   }

   @Override
   protected void onFocus() {

//      if (_selectedProfile != null
//            && _selectedProfile.name != null
//            && _selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {
//
//         // default profile is selected, make it easy to rename it
//
//         _txtProfileName.selectAll();
//         _txtProfileName.setFocus();
//
//      } else {
//
//         _profileViewer.getTable().setFocus();
//      }
   }

   private void restoreState() {

      /*
       * Get previous selected profile
       */
//      TourFilterProfile selectedProfile = TourFilterManager.getSelectedProfile();
//
//      if (selectedProfile == null) {
//
//         // select first profile
//
//         selectedProfile = (TourFilterProfile) _profileViewer.getElementAt(0);
//      }
//
//      if (selectedProfile != null) {
//         selectProfile(selectedProfile);
//      }

   }

   private void selectProfile(final TourFilterProfile selectedProfile) {

      _profileViewer.setSelection(new StructuredSelection(selectedProfile));

      final Table table = _profileViewer.getTable();
      table.setSelection(table.getSelectionIndices());
   }

}
