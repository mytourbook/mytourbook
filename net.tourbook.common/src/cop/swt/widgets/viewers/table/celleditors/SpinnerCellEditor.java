/**
 * <b>License</b>: <a href="http://www.gnu.org/licenses/lgpl.html">GNU Leser General Public License</a>
 * <b>Copyright</b>: <a href="mailto:abba-best@mail.ru">Cherednik, Oleg</a>
 * 
 * $Id$
 * $HeadURL$
 */
/*
 * Source: 2014-02-28 http://cop-swt-controls.googlecode.com/svn-history/r408/trunk/cop.swt.tableviewer/src/cop/swt/widgets/viewers/table/celleditors/SpinnerCellEditor.java
 */
package cop.swt.widgets.viewers.table.celleditors;

import static cop.swt.widgets.viewers.table.celleditors.TraverseListenerSet.allowEscape;
import static cop.swt.widgets.viewers.table.celleditors.TraverseListenerSet.allowReturn;

import java.text.NumberFormat;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

/**
 * @author <a href="mailto:abba-best@mail.ru">Cherednik, Oleg</a>
 * @since 20.12.2010
 */
public class SpinnerCellEditor extends CellEditor {

	private final int	multiplier;

	public SpinnerCellEditor(final Composite parent, final NumberFormat nf, final int style) {
		this(parent, nf, null, style);
	}

	public SpinnerCellEditor(final Composite parent, final NumberFormat nf, final RangeContent range, final int style) {

		super(parent, style);

		postConstruct(nf, range);

		this.multiplier = (int) Math.pow(10, getControl().getDigits());
	}

	@Override
	protected Spinner createControl(final Composite parent) {

		final Spinner spinner = new Spinner(parent, SWT.NONE);

		spinner.addTraverseListener(allowEscape);
		spinner.addTraverseListener(allowReturn);

		return spinner;
	}

	/*
	 * CellEditor
	 */

	@Override
	protected Object doGetValue() {

		final int intValue = getControl().getSelection();
		final double doubleValue = intValue;

		return doubleValue / multiplier;
	}

	@Override
	protected void doSetFocus() {
		getControl().setFocus();
	}

	@Override
	protected void doSetValue(final Object value) {

		if (value == null) {
			return;
		}

		final double doubleValue = ((Number) value).doubleValue() * multiplier;
		final int intValue = (int) Math.round(doubleValue);

		getControl().setSelection(intValue);
	}

	@Override
	public Spinner getControl() {
		return (Spinner) super.getControl();
	}

	private void postConstruct(final NumberFormat nf, final RangeContent range) {

		final Spinner spinner = getControl();

		spinner.setDigits(nf.getMaximumFractionDigits());
		spinner.setMinimum(range.getMinimum());
		spinner.setMaximum(range.getMaximum());
		spinner.setIncrement(range.getIncrement());
	}
}
