/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package tasktop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * @author Shawn Minto
 */
public class TextSearchControl extends Composite {

	private static final String				FIND_TEXT_MEMENTO_ELEMENT	= "text";										//$NON-NLS-1$

	private static final String				FIND_MEMENTO_ELEMENT		= "historyItem";								//$NON-NLS-1$

	private static final String				FIND_MEMENTO_TYPE			= "filteredTreeFindHistory";					//$NON-NLS-1$

	private static final int				MAX_HISTORY_ITEMS			= 50;

	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String				CLEAR_ICON					= "org.eclipse.ui.internal.dialogs.CLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String				FIND_ICON					= "org.eclipse.ui.internal.dialogs.FIND_ICON";	//$NON-NLS-1$

	/* SWT STYLE BIT AVAILABLE ON 3.5 AND HIGHER */
	public static final int					ICON_SEARCH					= 1 << 9;

	/* SWT STYLE BIT AVAILABLE ON 3.5 AND HIGHER */
	public static final int					ICON_CANCEL					= 1 << 8;

	/**
	 * Get image descriptors for the clear button.
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				PlatformUI.PLUGIN_ID,
				"$nl$/icons/full/etool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CLEAR_ICON, descriptor);
		}

		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(DiscoveryUi.ID_PLUGIN, "$nl$/icons/etool16/find.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(FIND_ICON, descriptor);
		}
	}

	private final Text						textControl;

	private Control							clearControl;

	private Control							findControl;

	private final boolean					automaticFind;

	private final Set<SelectionListener>	selectionListeners			= new HashSet<SelectionListener>();

	private Collection<String>				searchHistory				= new LinkedHashSet<String>();

	private boolean							hasHistorySupport;

	private static Boolean					useNativeSearchField;

	@SuppressWarnings("restriction")
	public TextSearchControl(final Composite parent, final boolean automaticFind) {
		super(parent, getCompositeStyle(automaticFind, parent));
		this.automaticFind = automaticFind;

		int textStyle = SWT.SINGLE;
		int numColumns = 1;
		if (useNativeSearchField(automaticFind, parent)) {
			if (automaticFind) {
				textStyle |= SWT.SEARCH | ICON_CANCEL;
			} else {
				textStyle |= SWT.SEARCH | ICON_SEARCH | ICON_CANCEL;
			}
		} else {
			super.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			super.setFont(parent.getFont());
			numColumns = 2;
			if (!automaticFind) {
				numColumns += 1;
			}
		}
		GridLayoutFactory
				.swtDefaults()
				.margins(0, 0)
				.extendedMargins(0, 0, 0, 0)
				.spacing(0, 1)
				.numColumns(numColumns)
				.applyTo(this);

		textControl = new Text(this, textStyle);

		final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textControl.setLayoutData(gridData);

		if (useNativeSearchField == null || !useNativeSearchField) {
			findControl = createLabelButtonControl(
					this,
					textControl,
					JFaceResources.getImageRegistry().getDescriptor(FIND_ICON),
					"Find",
					"Find",
					ICON_SEARCH);
			clearControl = createLabelButtonControl(
					this,
					textControl,
					JFaceResources.getImageRegistry().getDescriptor(CLEAR_ICON),
					WorkbenchMessages.FilteredTree_ClearToolTip,//FilteredTree_AccessibleListenerClearButton,
					WorkbenchMessages.FilteredTree_ClearToolTip,
					ICON_CANCEL);
			addModifyListener(new ModifyListener() {

				public void modifyText(final ModifyEvent e) {
					updateButtonVisibilityAndEnablement();

				}
			});
			updateButtonVisibilityAndEnablement();
		}

		registerListeners();
	}

	private static int getCompositeStyle(final boolean automaticFind, final Composite parent) {
		if (useNativeSearchField(automaticFind, parent)) {
			return SWT.NONE;
		}
		return SWT.BORDER;
	}

	private static boolean useNativeSearchField(final boolean automaticFind, final Composite parent) {
		if (parent != null) {
			if (useNativeSearchField == null) {
				useNativeSearchField = Boolean.FALSE;
				Text testText = null;
				try {
					int style = SWT.SEARCH | ICON_CANCEL;
					if (automaticFind) {
						style |= ICON_SEARCH;
					}
					testText = new Text(parent, style);
					useNativeSearchField = new Boolean((testText.getStyle() & ICON_CANCEL) != 0
							&& (!automaticFind || (testText.getStyle() & ICON_SEARCH) != 0));
				} finally {
					if (testText != null) {
						testText.dispose();
					}
				}

			}
		} else {
			useNativeSearchField = Boolean.FALSE;
		}
		return useNativeSearchField.booleanValue();
	}

	public void addModifyListener(final ModifyListener listener) {
		textControl.addModifyListener(listener);
	}

	public void addSelectionListener(final SelectionListener listener) {
		textControl.addSelectionListener(listener);
		selectionListeners.add(listener);
	}

	void addToSearchHistory(final String searchString) {
		if (searchString != null && searchString.trim().length() > 0) {
			searchHistory.remove(searchString);
			searchHistory.add(searchString);
		}
	}

	private Control createLabelButtonControl(	final Composite parent,
												final Text textControl,
												final ImageDescriptor activeImageDescriptor,
												final String accessibilityText,
												final String toolTipText,
												final int detail) {

		final Image nativeImage = parent.getDisplay().getSystemImage(detail);

		final Image activeImage = nativeImage != null ? nativeImage : activeImageDescriptor.createImage();
		final Image inactiveImage = new Image(parent.getDisplay(), activeImage, SWT.IMAGE_GRAY);
		final Image pressedImage = inactiveImage;

		final Label labelButton = new Label(parent, SWT.NONE);
		labelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		labelButton.setImage(inactiveImage);
		labelButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		labelButton.setToolTipText(toolTipText);
		labelButton.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				if (nativeImage == null && activeImage != null && !activeImage.isDisposed()) {
					activeImage.dispose();
				}
				if (inactiveImage != null && !inactiveImage.isDisposed()) {
					inactiveImage.dispose();
				}
				if (pressedImage != null && !pressedImage.isDisposed()) {
					pressedImage.dispose();
				}
			}
		});
		labelButton.addMouseListener(new MouseAdapter() {
			private MouseMoveListener	fMoveListener;

			private boolean isMouseInButton(final MouseEvent e) {
				final Point buttonSize = labelButton.getSize();
				return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y && e.y < buttonSize.y;
			}

			@Override
			public void mouseDown(final MouseEvent e) {
				labelButton.setImage(pressedImage);
				fMoveListener = new MouseMoveListener() {
					private boolean	fMouseInButton	= true;

					public void mouseMove(final MouseEvent e) {
						final boolean mouseInButton = isMouseInButton(e);
						if (mouseInButton != fMouseInButton) {
							fMouseInButton = mouseInButton;
							labelButton.setImage(mouseInButton ? pressedImage : inactiveImage);
						}
					}
				};
				labelButton.addMouseMoveListener(fMoveListener);
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				if (fMoveListener != null) {
					labelButton.removeMouseMoveListener(fMoveListener);
					fMoveListener = null;
					final boolean mouseInButton = isMouseInButton(e);
					labelButton.setImage(mouseInButton ? activeImage : inactiveImage);
					if (mouseInButton) {
						if (textControl.isEnabled() && textControl.getText().length() > 0) {
							notifySelectionChanged(detail);
						}
					}
				}
			}
		});

		labelButton.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(final MouseEvent e) {
				if (labelButton.getImage() != activeImage) {
					labelButton.setImage(activeImage);
				}
			}

			public void mouseExit(final MouseEvent e) {
				if (labelButton.getImage() != inactiveImage) {
					labelButton.setImage(inactiveImage);
				}
			}

			public void mouseHover(final MouseEvent e) {}
		});

		labelButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(final AccessibleEvent e) {
				e.result = accessibilityText;
			}
		});
		labelButton.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getRole(final AccessibleControlEvent e) {
				e.detail = ACC.ROLE_PUSHBUTTON;
			}
		});
		return labelButton;
	}

	public Collection<String> getSearchHistory() {
		return Collections.unmodifiableCollection(searchHistory);
	}

	public Text getTextControl() {
		return textControl;
	}

	public boolean hasHistorySupport() {
		return hasHistorySupport;
	}

	private void notifySelectionChanged(final int detail) {
		if (useNativeSearchField != null && useNativeSearchField) {
			// notification should happen via the widgets selection listener
			return;
		}
		final Event event = new Event();
		event.detail = detail;
		event.widget = textControl;
		event.display = textControl.getDisplay();

		final SelectionEvent e = new SelectionEvent(event);
		for (final Object element : selectionListeners) {
			((SelectionListener) element).widgetDefaultSelected(e);
		}
	}

	private void registerListeners() {
		addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				if (textControl != null && !textControl.isDisposed()) {
					addToSearchHistory(textControl.getText());
				}

			}
		});
		textControl.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(final FocusEvent e) {
				if (textControl != null && !textControl.isDisposed()) {
					addToSearchHistory(textControl.getText());
				}
			}
		});
	}

	public void removeModifyListener(final ModifyListener listener) {
		textControl.removeModifyListener(listener);
	}

	public void removeSelectionListener(final SelectionListener listener) {
		textControl.removeSelectionListener(listener);
		selectionListeners.remove(listener);
	}

	public void restoreState(final IMemento memento) {
		if (memento == null) {
			return;
		}
		final List<String> history = new ArrayList<String>();

		final IMemento rootMemento = memento.getChild(FIND_MEMENTO_TYPE);
		if (rootMemento != null) {
			final IMemento[] children = rootMemento.getChildren(FIND_MEMENTO_ELEMENT);
			if (children != null && children.length > 0) {
				for (final IMemento child : children) {
					final String historyItem = child.getString(FIND_TEXT_MEMENTO_ELEMENT);
					// add to the end of the list
					history.add(history.size(), historyItem);
				}
			}
		}
		if (history != null) {
			searchHistory = history;
		}
	}

	public void saveState(final IMemento memento) {
		if (memento == null) {
			return;
		}

		final IMemento rootMemento = memento.createChild(FIND_MEMENTO_TYPE);
		int i = 0;
		for (final String text : searchHistory) {
			final IMemento child = rootMemento.createChild(FIND_MEMENTO_ELEMENT);
			child.putString(FIND_TEXT_MEMENTO_ELEMENT, text);
			i++;
			if (i > MAX_HISTORY_ITEMS) {
				break;
			}
		}
	}

	@Override
	public void setBackground(final Color color) {
		if (useNativeSearchField != null && useNativeSearchField) {
			super.setBackground(color);
		}
	}

	private void setClearButtonVisibility(final boolean visible) {
		if (clearControl != null && !clearControl.isDisposed()) {
			clearControl.setVisible(visible);
			if (clearControl.getLayoutData() instanceof GridData) {
				((GridData) clearControl.getLayoutData()).exclude = !visible;
				clearControl.getParent().layout();
			}
		}
	}

	private void setFindButtonVisibility(final boolean visible) {
		if (findControl != null && !findControl.isDisposed()) {
			findControl.setVisible(visible);
			if (findControl.getLayoutData() instanceof GridData) {
				((GridData) findControl.getLayoutData()).exclude = !visible;
				findControl.getParent().layout();
			}
		}
	}

	private void updateButtonVisibilityAndEnablement() {
		if (textControl != null && !textControl.isDisposed()) {
			final boolean hasText = textControl.getText().length() > 0;

			setFindButtonVisibility(!(hasText && automaticFind));
			setClearButtonVisibility(hasText);
		}
	}
}
