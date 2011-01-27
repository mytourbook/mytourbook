/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime.actions;

import java.util.Locale;
import java.util.Set;

import org.eclipse.babel.runtime.Activator;
import org.eclipse.babel.runtime.Messages;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.babel.runtime.external.TranslatableResourceBundle;
import org.eclipse.babel.runtime.external.TranslatableSet;
import org.eclipse.babel.runtime.external.TranslatableText;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

public class TranslatableTreeComposite extends Composite {

	/**
	 * There appears to be no listener on FocusCellManager for a change in the
	 * focus cell. Therefore we override the focusCellChanged method in
	 * FocusCellHighlighter as the only means of getting focus cell change
	 * notifications.
	 */
	private class MyFocusCellHighlighter extends FocusCellHighlighter {

		public MyFocusCellHighlighter(TreeViewer viewer) {
			super(viewer);
		}

		@Override
		protected void focusCellChanged(ViewerCell newCell, ViewerCell oldCell) {
			super.focusCellChanged(newCell, oldCell);
			updateButtonEnablement(newCell);
		}
	}

	private Tree treeControl;
	
	private Button revertButton;

	private TreeViewerFocusCellManager focusCellManager;

	public TranslatableTreeComposite(Composite parent, ITreeContentProvider contentProvider, Object input, TranslatableSet languageSet, Set<TranslatableResourceBundle> updatedBundles) {
		super(parent, SWT.NONE);

		setLayout(new GridLayout(1, false));
		
		final TreeViewer viewer = new TreeViewer(this, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		treeControl = viewer.getTree(); 
		viewer.getTree().setHeaderVisible(true);
		viewer.getTree().setLinesVisible(true);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		viewer.setContentProvider(contentProvider);

		createTreeColumns(viewer, languageSet, updatedBundles);

		viewer.setInput(input);

		ColumnViewerToolTipSupport.enableFor(viewer);

		createButtonsSection(this, viewer, languageSet, updatedBundles).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public void updateButtonEnablement(ViewerCell newCell) {
		if (newCell == null) {
			revertButton.setEnabled(false);
		} else {
			ITranslatableText translatableText = getTranslatableText(newCell.getElement());
			Locale locale = (Locale)treeControl.getColumn(newCell.getColumnIndex()).getData();
			if (translatableText instanceof TranslatableText) {
				revertButton.setEnabled(((TranslatableText)translatableText).isDirty(locale));
			} else {
				revertButton.setEnabled(false);
			}
		}
	}

	private void createTreeColumns(final TreeViewer viewer, final TranslatableSet languageSet, final Set<TranslatableResourceBundle> updatedBundles) {
		focusCellManager = new TreeViewerFocusCellManager(viewer, new MyFocusCellHighlighter(viewer));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
			@Override
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
				|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
				|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
				|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};

		TreeViewerEditor.create(viewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
				| ColumnViewerEditor.TABBING_VERTICAL 
				| ColumnViewerEditor.KEYBOARD_ACTIVATION);


		// Add the columns

		final TreeViewerColumn columnKey = new TreeViewerColumn(viewer, SWT.LEFT);
		columnKey.getColumn().setWidth(80);
		languageSet.associate(columnKey.getColumn(), Activator.getLocalizableText("LocalizeDialog.keyColumnHeader")); 
		columnKey.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}

			@Override
			public Image getImage(Object element) {
				ITranslatableText text = getTranslatableText(element);
				if (text instanceof TranslatableText) {
					return Activator.getImage("icons/localizable.gif"); //$NON-NLS-1$
				} else {
					return Activator.getImage("icons/nonLocalizable.gif"); //$NON-NLS-1$
				}
			}

			@Override
			public String getToolTipText(Object element) {
				ITranslatableText text = getTranslatableText(element);
				if (text instanceof TranslatableText) {
					TranslatableText translatableText = (TranslatableText)text;
					ITranslatableText tooltipLocalizableText = translatableText.getTooltip();
					languageSet.associate2(columnKey, tooltipLocalizableText);
					return tooltipLocalizableText.getLocalizedText();
				} else {
					return null;
				}
			}
		});

		Locale rootLocale = new Locale("", "", ""); 
		createLocaleColumn(viewer, updatedBundles, rootLocale, null);
		String languageCode = Locale.getDefault().getLanguage();
		if (languageCode.length() != 0) {
			Locale languageLocale = new Locale(languageCode, "", ""); 
			createLocaleColumn(viewer, updatedBundles, languageLocale, rootLocale);

			String countryCode = Locale.getDefault().getCountry();
			if (countryCode.length() != 0) {
				Locale countryLocale = new Locale(languageCode, countryCode, ""); 
				createLocaleColumn(viewer, updatedBundles, countryLocale, languageLocale);

				String variantCode = Locale.getDefault().getVariant();
				if (variantCode.length() != 0) {
					Locale variantLocale = new Locale(languageCode, countryCode, variantCode); 
					createLocaleColumn(viewer, updatedBundles, variantLocale, countryLocale);
				}
			}
		}
	}

	private void createLocaleColumn(final TreeViewer viewer,
			final Set<TranslatableResourceBundle> updatedBundles,
			final Locale locale, final Locale previousLocale) {
		TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setWidth(150);
		column.getColumn().setText(locale.getDisplayName());
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ITranslatableText text = getTranslatableText(element);
				String message = text.getLocalizedText(locale);
				if (previousLocale == null) {
					return message;
				} else {
					String fallbackMessage = text.getLocalizedText(previousLocale);
					return (message.equals(fallbackMessage)) ? "" : message; //$NON-NLS-1$
				}
			}
			
			@Override
			public Color getForeground(Object element) {
				ITranslatableText text = getTranslatableText(element);
				return (text instanceof TranslatableText
				 	&& ((TranslatableText)text).isDirty(locale)) ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA) : null;
			}
			
		});

		column.setEditingSupport(new EditingSupport(viewer) {
			@Override
			protected boolean canEdit(Object element) {
				ITranslatableText text = getTranslatableText(element);
				return text instanceof TranslatableText;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getTree());
			}

			@Override
			protected Object getValue(Object element) {
				// The text cell editor requires that null is never returned
				// by this method.
				ITranslatableText text = getTranslatableText(element);
				String message = text.getLocalizedText(locale);
				if (previousLocale == null) {
					return message;
				} else {
					String fallbackMessage = text.getLocalizedText(previousLocale);
					return (message.equals(fallbackMessage)) ? "" : message; //$NON-NLS-1$
				}
			}

			@Override
			protected void setValue(Object element, Object value) {
				ITranslatableText translatableText = getTranslatableText(element);

				String text = (String)value;

				/*
				 * If the text is all white space then we assume that the user
				 * is clearing out the locale override.  The text would then be
				 * obtained by looking to the parent locale.  For example, if the
				 * user was using Canadian English and saw "colour", but the user then blanked
				 * that out, then "color" would be used for Canadian locales.
				 * 
				 * Note this means that an entry must be placed in the delta properties
				 * file for Canadian English.  "Colour" would be in the original (immutable)
				 * properties file, and so we need an entry in the delta file to say that
				 * we should ignore that and use whatever might be used for US-English.
				 * We should not simply put the current US-English in the file because then 
				 * we would not pick up future changes to the US-English.    
				 * 
				 * We never allow the user to set text to be blank.  If the original
				 * developer displayed a message, then a message must be displayed,
				 * regardless of the language.  It is conceivable that this could be
				 * a problem in very specific circumstances.  Suppose a developer uses
				 * a message to be the suffix that is appended to a word to make it plural.
				 * In English the text for most words would be "s".  In another language
				 * the word may be the same in the singular and plural so would be the empty
				 * string.  This is, however, not a good example, because that would be bad
				 * localization.  So this is probably not a problem.
				 * 
				 * This is a restriction caused by the UI design of this dialog.  The resource
				 * bundle implementation would allow an empty string to be passed
				 * and that would result in the user seeing a blank string.  Null, on the
				 * other hand, results in the value from the parent locale being used.
				 */

				text = text.trim();
				if (text.length() == 0) {
					// Setting null means use value from parent locale.
					text = null;
				}

				// If the text can be edited, the text must be of type TranslatableText
				// because that is the only type that the user can edit.
				((TranslatableText)translatableText).setLocalizedText(locale, text, updatedBundles);
				viewer.update(element, null);
				
				revertButton.setEnabled(((TranslatableText)translatableText).isDirty(locale));
			}
		});
		
		column.getColumn().setData(locale);
	}

	
	private Control createButtonsSection(Composite parent, final TreeViewer viewer, TranslatableSet languageSet, final Set<TranslatableResourceBundle> updatedBundles) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		revertButton = new Button(container, SWT.PUSH);
		languageSet.associate(revertButton, Messages.LocalizeDialog_CommandLabel_Revert);
		languageSet.associateToolTip(revertButton, Messages.LocalizeDialog_CommandTooltip_Revert);
		revertButton.setEnabled(false);
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		revertButton.setLayoutData(gd);

		revertButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int columnIndex = focusCellManager.getFocusCell().getColumnIndex();
				Object element = focusCellManager.getFocusCell().getElement();
				ITranslatableText translatableText = getTranslatableText(element);
				Locale locale = (Locale)viewer.getTree().getColumn(columnIndex).getData();
				
				// If this button is enabled, the text must be of type TranslatableText
				// because that is the only type that the user can edit.
				((TranslatableText)translatableText).revertLocalizedText(locale, updatedBundles);
				viewer.update(element, null);
			}
		});
		
		return container;
	}

	private ITranslatableText getTranslatableText(Object element) {
		ITranslatableText text = null;
		if (element instanceof ITranslatableText) {
			text = (ITranslatableText)element;
		} else if (element instanceof IAdaptable) {
			text = (ITranslatableText)((IAdaptable)element).getAdapter(ITranslatableText.class);
		}
		return text;
	}
}
