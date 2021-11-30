package net.tourbook.ui;

import net.tourbook.tour.CadenceMultiplier;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ComboViewerCadence extends ComboViewer {

   public ComboViewerCadence(final Composite container) {

      super(container, SWT.READ_ONLY | SWT.DROP_DOWN);

      init();
   }

   public ComboViewerCadence(final Composite container, final int i) {

      super(container, i);

      init();
   }

   @Override
   public void add(final Object element) {
      throw new NotImplementedException("This method cannot be called"); //$NON-NLS-1$
   }

   public CadenceMultiplier getSelectedCadence() {

      return (CadenceMultiplier) getStructuredSelection().getFirstElement();
   }

   private void init() {

      setContentProvider(ArrayContentProvider.getInstance());

      setLabelProvider(new LabelProvider() {
         @Override
         public String getText(final Object element) {
            if (!(element instanceof CadenceMultiplier)) {
               throw new IllegalStateException("Invalid object type found in ComboViewerCadence: " + element.getClass()); //$NON-NLS-1$
            }
            return ((CadenceMultiplier) element).getNlsLabel();
         }
      });

      final CadenceMultiplier[] cadences = new CadenceMultiplier[] {
            CadenceMultiplier.NONE,
            CadenceMultiplier.RPM,
            CadenceMultiplier.SPM };

      setInput(cadences);
   }

   public void setSelection(final CadenceMultiplier selection) {
      super.setSelection(new StructuredSelection(selection));
   }
}
