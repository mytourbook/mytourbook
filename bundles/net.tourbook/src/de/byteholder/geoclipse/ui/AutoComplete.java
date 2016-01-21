package de.byteholder.geoclipse.ui;

import java.util.Date;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Combo;

import de.byteholder.geoclipse.map.UI;

/**
 * Adds the autocomplete feature to a comboviewer <br>
 * <br>
 * http://sourcezone.wordpress.com/2008/02/14/java-jface-autocomplete-comboviewer/
 * 
 * @author Felipe Lang
 */
public class AutoComplete {

	/**
	 * Intervalo en milisegundos que deben pasar entre que el usuario presiona
	 * una tecla para que se resetee la variable strTyped
	 */
	private static final int	INTERVAL_KEY_PRESSED	= 3000;

	/**
	 * guarda la hora en milisegundos de cuando fu presionada por ultima vez una
	 * tecla
	 */
	private long				_lastKeyTime			= new Date().getTime();

	/**
	 * guarda los caracteres que el usuario va presionando
	 */
	private String				_typed;

	private ComboViewer			_comboViewer;

	public AutoComplete(final ComboViewer comboViewer) {

		_comboViewer = comboViewer;

		final Combo combo = comboViewer.getCombo();

		combo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.keyCode == SWT.DEL)
					setSelection(null);
				autoCompleteKeyUp(e);
			}

		});

		combo.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent arg0) {
				autoCompleteLeave();
			}
		});

	}

	private void autoCompleteKeyUp(final KeyEvent e) {

		// hora actual en milisegundos
		final long now = new Date().getTime();
		if (((now - _lastKeyTime) > INTERVAL_KEY_PRESSED) || (_typed == null)) {
			_typed = UI.EMPTY_STRING;
		}
		_lastKeyTime = now;

		// Ignore basic selection keys
		switch (e.keyCode) {
		case SWT.ARROW_RIGHT:
			return;
		case SWT.ARROW_LEFT:
			return;
		case SWT.ARROW_UP:
			_typed = UI.EMPTY_STRING;
			return;
		case SWT.ARROW_DOWN:
			_typed = UI.EMPTY_STRING;
			return;
		case SWT.CAPS_LOCK:
			return;
		case SWT.BS:
			_typed = UI.EMPTY_STRING;
		case SWT.DEL:
			_typed = UI.EMPTY_STRING;
			return;
		}

		// guardo lo que el usuario va tipeando
		_typed += String.valueOf(e.character).toLowerCase();
		final Combo combo = _comboViewer.getCombo();

		int index = combo.getSelectionIndex();
		final String[] items = combo.getItems();

		// busco alguna coicidencia en la lista
		for (int i = 0; i < items.length; i++) {
			final String s = items[i].toLowerCase();
			if (s.indexOf(_typed) == 0) {
				index = i;
				break;
			}
		}
		setSelection(_comboViewer.getElementAt(index));
	}

	private void autoCompleteLeave() {

		final Combo combo = _comboViewer.getCombo();

		// correct casing when leaving combo
		final int intFoundIndex = combo.indexOf(combo.getText());
		combo.select(-1);
		combo.select(intFoundIndex);
	}

	private void setSelection(final Object object) {
		if (object == null) {
			/*
			 * disabled otherwise new characters cannot be entered
			 */
			// _comboViewer.setSelection(null);
		} else {
			_comboViewer.setSelection(new StructuredSelection(object));
		}
	}

}
