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

package org.eclipse.babel.runtime.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class TranslatableSet implements ITranslatableSet {

	/**
	 * All messages added to this object must come from bundles that have the same
	 * locale.  This is the locale of messages in this collection.
	 */
	private Locale locale;
	
	protected Map<Object, TranslatableTextInput> localizableTextCollection = new HashMap<Object, TranslatableTextInput>();	
	protected ArrayList<Object> controlOrder = new ArrayList<Object>();	

	public TranslatableSet() {
		this.locale = Locale.getDefault();
	}
	
	public TranslatableTextInput[] getLocalizedTexts() {
		/*
		 * We need to get the values from the map, but return them in the order
		 * in which the controls were originally added. This ensures a more
		 * sensible order.
		 */
		TranslatableTextInput[] result = new TranslatableTextInput[controlOrder.size()];
		int i = 0;
		for (Object controlKey: controlOrder) {
			result[i++] = localizableTextCollection.get(controlKey); 
		}
		return result;
	}

	/**
	 * This method is called whenever any text in this set has been changed.
	 * 
	 * The default implementation does nothing.  This method should
	 * be overwritten to re-layout and re-size as appropriate given
	 * the changes in the text lengths.
	 */
	public void layout() {
		// This default implementation does nothing.
	}

	/**
	 * This is the generic method that 'associates' text with a control.
	 * This method (or one of the more specific overloads of this method)
	 * should be called whenever text is to be set in some way (as the label,
	 * tool-tip, etc) into a control.  By calling this method, the control will
	 * automatically be updated whenever the text changes.
	 * 
	 * The <code>updateControl(String)</code> method of the <code>textInput</code> parameter will be called when this
	 * method is called and also whenever changes occur in the text.
	 */
	public void associate(Object controlKey, TranslatableTextInput textInput) {
		textInput.getLocalizedTextObject().validateLocale(locale);
		
		if (!controlOrder.contains(controlKey)) {
			controlOrder.add(controlKey);
		}
		localizableTextCollection.put(controlKey, textInput);
	}

	/**
	 * 'Associates' the given label with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the label control.  By using this method, the text shown in the label
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final Label label, ITranslatableText localizableText) {
		associate(label, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				label.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the given button with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the button control.  By using this method, the text shown in the button
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final Button button, ITranslatableText localizableText) {
		associate(button, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				button.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the tooltip of the given button with the given text.
	 * 
	 * This method should be called instead of calling <code>setToolTipText</code>
	 * on the button control.  By using this method, the tool-tip shown for the button
	 * will be updated immediately when the given text is changed.
	 */
	public void associateToolTip(final Button button, ITranslatableText localizableText) {
		associate(new ToolTipKey(button), new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				button.setToolTipText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the given button with the given item.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the item.  By using this method, the text shown for the item
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final Item item, ITranslatableText localizableText) {
		associate(item, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				item.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the title of the given shell with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the shell.  By using this method, the text shown as the title of the shell
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final Shell shell, ITranslatableText localizableText) {
		associate(shell, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				shell.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the text on the given tab with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the tab item.  By using this method, the text shown on the tab
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final TabItem tabItem, ITranslatableText localizableText) {
		associate(tabItem, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				tabItem.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the title of the given form with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the form.  By using this method, the text shown as the title of the form
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final ScrolledForm form, ITranslatableText localizableText) {
		associate(form, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				form.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the title of the given section with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the section.  By using this method, the text shown as the title of the section
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final Section section, ITranslatableText localizableText) {
		associate(section, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				section.setText(text);
			}
		}); 
	}

	/**
	 * 'Associates' the description of the given section with the given text.
	 * 
	 * This method should be called instead of calling <code>setDescription</code>
	 * on the section.  By using this method, the text shown as the description for the section
	 * will be updated immediately when the given text is changed.
	 */
	public void associateDescription(final Section section, ITranslatableText localizableText) {
		associate(new DescriptionKey(section), new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				section.setDescription(text);
			}
		}); 
	}

	/**
	 * 'Associates' the given hyper-link with the given text.
	 * 
	 * This method should be called instead of calling <code>setText</code>
	 * on the hyper-link.  By using this method, the text shown in the hyper-link
	 * will be updated immediately when the given text is changed.
	 */
	public void associate(final Hyperlink link, ITranslatableText localizableText) {
		associate(link, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				link.setText(text);
			}
		}); 
	}

	//	public void associate(
//			FormPage formPage, ITranslatableText localizableText) {
//		associate(formPage, new TranslatableTextInput(localizableText) {
//			@Override
//			public void updateControl(String text) {
//				formPage.setPartName(text);
//			}
//		}); 
//	}

	public Locale getLocale() {
		return locale;
	}

	/**
	 * This method should be used to register translatable text that does
	 * not appear directly in the part/dialog.  For example, text that appears
	 * in a message dialog.  Such text does not have to be updated in the controls
	 * so long as it is re-read every time it is about to be displayed.
	 *  
	 * @param localizableText
	 */
	public void associate(ITranslatableText localizableText) {
		associate(new Object(), new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				// do nothing
			}
		}); 
	}

	/**
	 * This method should be used to register translatable text that does
	 * not appear directly in the part/dialog.  For example, text that appears
	 * in a tool-tip.  This method should be used only if the tool-tip is built each
	 * time, such as a tool-tip in a table.
	 * 
	 * Although every cell in a column has a different tool-tip, they all have tool-tips
	 * built in the same way from the same templates.  Therefore the tool-tips for a column
	 * should appear just once.  However, we update the sample data so that the user
	 * always sees the last tool-tip shown.
	 *  
	 * @param localizableText
	 */
	public void associate2(Object control, ITranslatableText localizableText) {
		associate(control, new TranslatableTextInput(localizableText) {
			@Override
			public void updateControl(String text) {
				// do nothing
			}
		}); 
	}
	
	/**
	 * This class is used to form a key that identifies the tooltip
	 * for a control.  Normally the control itself is used as the key.
	 * However that would cause a conflict with the primary text for
	 * the control, hence the need to use this wrapper as a key for
	 * the tool-tip.
	 */
	class ToolTipKey {
		private Object control;
		
		ToolTipKey(Object control) {
			this.control = control;
		}
		
		@Override
		public boolean equals(Object other) {
			return other instanceof ToolTipKey
				&& ((ToolTipKey)other).control.equals(control);
		}
		
		@Override
		public int hashCode() {
			return control.hashCode();
		}
	}
	
	/**
	 * This class is used to form a key that identifies the description for a
	 * section. The section itself is used as the key for the title. Therefore
	 * we cannot use the section as a key for the description because that would
	 * cause a conflict. Hence the need to use this wrapper as a key for the
	 * description.
	 */
	class DescriptionKey {
		private Object control;
		
		DescriptionKey(Object control) {
			this.control = control;
		}
		
		@Override
		public boolean equals(Object other) {
			return other instanceof ToolTipKey
				&& ((ToolTipKey)other).control.equals(control);
		}
		
		@Override
		public int hashCode() {
			return control.hashCode();
		}
	}
}
