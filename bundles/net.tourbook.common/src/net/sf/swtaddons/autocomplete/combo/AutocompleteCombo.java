package net.sf.swtaddons.autocomplete.combo;

import net.sf.swtaddons.autocomplete.AutocompleteWidget;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class AutocompleteCombo extends AutocompleteWidget {

   private static final int NUMBER_OF_DEFAULT_ROWS = 20;

   protected Combo          combo                  = null;

   private int              _popupHeight;

   private final class ComboFocusListener implements FocusListener {

      @Override
      public void focusGained(final FocusEvent e) {

         provider.setProposals(combo.getItems());
      }

      @Override
      public void focusLost(final FocusEvent e) {
         // do nothing
      }
   }

   private final class ComboResizeListener implements Listener {

      @Override
      public void handleEvent(final Event event) {

         if (combo != null) {

            final Point size = combo.getSize();

            if (_popupHeight == 0) {

               size.y *= NUMBER_OF_DEFAULT_ROWS;

            } else {

               size.y = _popupHeight;
            }

            adapter.setPopupSize(size);
         }
      }
   }

   public AutocompleteCombo(final Combo providedCombo) {

      combo = providedCombo;

      if (combo != null) {

         combo.addFocusListener(new ComboFocusListener());
         combo.addListener(SWT.Resize, new ComboResizeListener());

         provider = getContentProposalProvider(combo.getItems());

         adapter = new ContentProposalAdapter(combo,
               new ComboContentAdapter(),
               provider,
               getActivationKeystroke(),
               getAutoactivationChars());

         adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
      }
   }

   public void restoreState(final IDialogSettings state, final String stateKey) {

      final Point popupDefaultSize = combo.getSize();

      final int minimumHeight = popupDefaultSize.y * 3;
      final int defaultHeight = popupDefaultSize.y * NUMBER_OF_DEFAULT_ROWS;

      _popupHeight = Math.max(

            // ensure minimum height
            minimumHeight,

            Util.getStateInt(state, stateKey, defaultHeight));
   }

   public void saveState(final IDialogSettings state, final String stateKey) {

      final Point popupSize = adapter.getPopupSize();

      if (popupSize != null) {

         // save popup height
         state.put(stateKey, popupSize.y);
      }
   }
}
