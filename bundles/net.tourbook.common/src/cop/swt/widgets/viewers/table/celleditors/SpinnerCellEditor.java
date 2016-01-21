/**
 * <b>License</b>: <a href="http://www.gnu.org/licenses/lgpl.html">GNU Leser General Public License</a>
 * <b>Copyright</b>: <a href="mailto:abba-best@mail.ru">Cherednik, Oleg</a>
 * 
 * $Id$
 * $HeadURL$
 */
package cop.swt.widgets.viewers.table.celleditors;

import static cop.swt.widgets.viewers.table.celleditors.TraverseListenerSet.allowEscape;
import static cop.swt.widgets.viewers.table.celleditors.TraverseListenerSet.allowReturn;
import static cop.swt.widgets.viewers.table.celleditors.TraverseListenerSet.allowTabKey;

import java.text.NumberFormat;

import net.tourbook.common.UI;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

/*
 * Source: 2014-02-28 http://cop-swt-controls.googlecode.com/svn-history/r408/trunk/cop.swt.tableviewer/src/cop/swt/widgets/viewers/table/celleditors/SpinnerCellEditor.java
 * Contains some modifications.
 */

/**
 * @author <a href="mailto:abba-best@mail.ru">Cherednik, Oleg</a>
 * @since 20.12.2010
 */
public class SpinnerCellEditor extends CellEditor {

	private final int			_multiplier;

	private MouseWheelListener	_defaultMouseWheelListener;
	{
		/*
		 * Modify values with the mouse wheel, a addListener() must be set to get the notifications
		 * in editorValueChanged()
		 */
		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				fireEditorValueChanged(false, true);
			}
		};
	}

	public SpinnerCellEditor(final Composite parent, final NumberFormat nf, final int style) {
		this(parent, nf, null, style);
	}

	public SpinnerCellEditor(final Composite parent, final NumberFormat nf, final RangeContent range, final int style) {

		super(parent, style);

		postConstruct(nf, range);

		_multiplier = (int) Math.pow(10, getControl().getDigits());
	}

	@Override
	protected Spinner createControl(final Composite parent) {

		final Spinner spinner = new Spinner(parent, SWT.NONE);

		spinner.addTraverseListener(allowTabKey);
		spinner.addTraverseListener(allowEscape);
		spinner.addTraverseListener(allowReturn);

		return spinner;
	}

	@Override
	protected Object doGetValue() {

		final int intValue = getControl().getSelection();
		final double doubleValue = intValue;

		return doubleValue / _multiplier;
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

		final double doubleValue = ((Number) value).doubleValue() * _multiplier;
		final int intValue = (int) Math.round(doubleValue);

		getControl().setSelection(intValue);
	}

	@Override
	public Spinner getControl() {
		return (Spinner) super.getControl();
	}

	private void postConstruct(final NumberFormat nf, final RangeContent range) {

		final int increment = range.getIncrement();

		final Spinner spinner = getControl();

		spinner.setDigits(nf.getMaximumFractionDigits());
		spinner.setMinimum(range.getMinimum());
		spinner.setMaximum(range.getMaximum());
		spinner.setIncrement(increment);
		spinner.setPageIncrement(increment * 10);

		spinner.addMouseWheelListener(_defaultMouseWheelListener);
	}
}
