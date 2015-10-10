/*******************************************************************************
 * Copyright (c) 2008 Stefan Mücke and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Mücke - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.nls.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.babel.core.message.Message;
import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.resource.IMessagesResource;
import org.eclipse.babel.core.message.resource.PropertiesIFileResource;
import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.preferences.MsgEditorPreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundle;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundleKey;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditResourceBundleEntriesDialog extends Dialog {

	private class LocaleField {
		ResourceBundle bundle;
		Label label;
		Text text;
		Locale locale;
		String oldValue;
		boolean isReadOnly;
		Button button;
	}

	private ResourceBundleKey resourceBundleKey;
	protected ArrayList<LocaleField> fields = new ArrayList<LocaleField>();
	private final Locale[] locales;
	private Color errorColor;

	/**
	 * @param locales the locales to edit 
	 */
	public EditResourceBundleEntriesDialog(Shell parentShell, Locale[] locales) {
		super(parentShell);
		this.locales = locales;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	public void setResourceBundleKey(ResourceBundleKey resourceBundleKey) {
		this.resourceBundleKey = resourceBundleKey;
	}

	/*
	 * @see org.eclipse.jface.window.Window#open()
	 */
	@Override
	public int open() {
		if (resourceBundleKey == null)
			throw new RuntimeException("Resource bundle key not set.");
		return super.open();
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Resource Bundle Entries");
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) composite.getLayout();
		gridLayout.numColumns = 3;

		Label keyLabel = new Label(composite, SWT.NONE);
		keyLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		keyLabel.setText("&Key:");

		int style = SWT.SINGLE | SWT.LEAD | SWT.BORDER | SWT.READ_ONLY;
		Text keyText = new Text(composite, style);
		keyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		keyText.setText(resourceBundleKey.getName());

		new Label(composite, SWT.NONE); // spacer

		for (Locale locale : locales) {
			if (locale.getLanguage().equals("")) { //$NON-NLS-1$
				fields.add(createLocaleField(composite, locale, "&Default Bundle:"));
			} else {
				fields.add(createLocaleField(composite, locale, "&" + locale.getDisplayName() + ":"));
			}
		}

		// Set focus on first editable field
		if (fields.size() > 0) {
			for (int i = 0; i < fields.size(); i++) {
				if (fields.get(i).text.getEditable()) {
					fields.get(i).text.setFocus();
					break;
				}
			}
		}

		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
		label.setText("Note: The following escape sequences are allowed: \\r, \\n, \\t, \\\\");

		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		for (LocaleField field : fields) {
			field.text.addModifyListener(modifyListener);
		}
		errorColor = new Color(Display.getCurrent(), 0xff, 0x7f, 0x7f);
		
		return composite;
	}

	private LocaleField createLocaleField(Composite parent, Locale locale, String localeLabel) {
		ResourceBundle bundle = resourceBundleKey.getFamily().getBundle(locale);

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setText(localeLabel);

		boolean readOnly = bundle == null || bundle.isReadOnly();
		int style = SWT.SINGLE | SWT.LEAD | SWT.BORDER | (readOnly ? SWT.READ_ONLY : 0);
		Text text = new Text(parent, style);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		String value = null;
		if (bundle != null) {
			try {
				value = bundle.getString(resourceBundleKey.getName());
			} catch (CoreException e) {
				MessagesEditorPlugin.log(e);
			}
			if (value == null) {
				if (readOnly) {
					value = "(Key does not exist)";
				} else {
					value = ""; // TODO Indicate that the entry is missing: perhaps red background
				}
			}
			text.setText(escape(value));
		} else {
			text.setText("(Resource bundle not found)");
		}

		Button button = new Button(parent, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.setText("..."); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (LocaleField field : fields) {
					if (e.widget == field.button) {
						EditMultiLineEntryDialog dialog = new EditMultiLineEntryDialog(
							getShell(),
							unescape(field.text.getText()),
							field.isReadOnly);
						if (dialog.open() == Window.OK) {
							field.text.setText(escape(dialog.getValue()));
						}
					}
				}
			}
		});

		LocaleField field = new LocaleField();
		field.bundle = bundle;
		field.label = label;
		field.text = text;
		field.locale = locale;
		field.oldValue = value;
		field.isReadOnly = readOnly;
		field.button = button;
		return field;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		for (LocaleField field : fields) {
			if (field.isReadOnly)
				continue;
			String key = resourceBundleKey.getName();
			String value = unescape(field.text.getText());
			boolean hasChanged = (field.oldValue == null && !value.equals("")) //$NON-NLS-1$
					|| (field.oldValue != null && !field.oldValue.equals(value));
			if (hasChanged) {
				try {
					Object resource = field.bundle.getUnderlyingResource();
					if (resource instanceof IFile) {
			            MsgEditorPreferences prefs = MsgEditorPreferences.getInstance();

		                IMessagesResource messagesResource = new PropertiesIFileResource(
		                        field.locale,
		                        new PropertiesSerializer(prefs),
		                        new PropertiesDeserializer(prefs),
		                        (IFile) resource, MessagesEditorPlugin.getDefault());
		                MessagesBundle bundle = new MessagesBundle(messagesResource);

		                Message message = new Message(key, field.locale);
		                message.setText(value);
						bundle.addMessage(message);

						// This commented out code is how the update was done before this code was merged
						// into the Babel Message Editor.  This code should be removed.
						
//						InputStream inputStream;
//						IFile file = (IFile) resource;
//						
//						inputStream = file.getContents();
//						RawBundle rawBundle;
//						try {
//							rawBundle = RawBundle.createFrom(inputStream);
//							rawBundle.put(key, value);
//						} catch (Exception e) {
//							openError("Value could not be saved: " + value, e);
//							return;
//						}
//						StringWriter stringWriter = new StringWriter();
//						rawBundle.writeTo(stringWriter);
//						byte[] bytes = stringWriter.toString().getBytes("ISO-8859-1"); //$NON-NLS-1$
//						ByteArrayInputStream newContents = new ByteArrayInputStream(bytes);
//						file.setContents(newContents, false, false, new NullProgressMonitor());
					} else {
						// Unexpected type of resource
						throw new RuntimeException("Not yet implemented."); //$NON-NLS-1$
					}
					field.bundle.put(key, value);
				} catch (Exception e) {
					openError("Value could not be saved: " + value, e);
					return;
				}
			}
		}
		super.okPressed();
		errorColor.dispose();
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsStrategy()
	 */
	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTLOCATION | DIALOG_PERSISTSIZE;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = MessagesEditorPlugin.getDefault().getDialogSettings();
		String sectionName = "EditResourceBundleEntriesDialog"; //$NON-NLS-1$
		IDialogSettings section = settings.getSection(sectionName);
		if (section == null)
			section = settings.addNewSection(sectionName);
		return section;
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
	 */
	@Override
	protected Point getInitialSize() {
		Point initialSize = super.getInitialSize();
		// Make sure that all locales are visible
		Point size = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		if (initialSize.y < size.y)
			initialSize.y = size.y;
		return initialSize;
	}

	protected void validate() {
		boolean isValid = true;
		for (LocaleField field : fields) {
			try {
				unescape(field.text.getText());
				field.text.setBackground(null);
			} catch (IllegalArgumentException e) {
				field.text.setBackground(errorColor);
				isValid = false;
			}
		}
		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(isValid);
	}
	
	private void openError(String message, Exception e) {
		IStatus status;
		if (e instanceof CoreException) {
			CoreException coreException = (CoreException) e;
			status = coreException.getStatus();
		} else {
			status = new Status(IStatus.ERROR, "<dummy>", e.getMessage(), e); //$NON-NLS-1$
		}
		e.printStackTrace();
		ErrorDialog.openError(getParentShell(), "Error", message, status);
	}

	/**
	 * Escapes line separators, tabulators and double backslashes. 
	 * 
	 * @param str
	 * @return the escaped string
	 */
	public static String escape(String str) {
		StringBuilder builder = new StringBuilder(str.length() + 10);
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
				case '\r' :
					builder.append("\\r"); //$NON-NLS-1$
					break;
				case '\n' :
					builder.append("\\n"); //$NON-NLS-1$
					break;
				case '\t' :
					builder.append("\\t"); //$NON-NLS-1$
					break;
				case '\\' :
					builder.append("\\\\"); //$NON-NLS-1$
					break;
				default :
					builder.append(c);
					break;
			}
		}
		return builder.toString();
	}

	/**
	 * Unescapes line separators, tabulators and double backslashes. 
	 * 
	 * @param str
	 * @return the unescaped string
	 * @throws IllegalArgumentException when an invalid or unexpected escape is encountered
	 */
	public static String unescape(String str) {
		StringBuilder builder = new StringBuilder(str.length() + 10);
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '\\') {
				switch (str.charAt(i + 1)) {
					case 'r' :
						builder.append('\r');
						break;
					case 'n' :
						builder.append('\n');
						break;
					case 't' :
						builder.append('\t');
						break;
					case '\\' :
						builder.append('\\');
						break;
					default :
						throw new IllegalArgumentException("Invalid escape sequence.");
				}
				i++;
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}
