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

import org.eclipse.babel.runtime.TranslatableMenuItem;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A content provider that provides IContributionItem objects that
 * match the workbench window's menu.
 */
class MenuContentProvider implements IStructuredContentProvider, 
ITreeContentProvider {

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		return ((TranslatableMenuItem)parent).getChildren().toArray();
	}

	public Object getParent(Object child) {
		// This does not appear to be required to be implemented
		return null;
	}

	public Object [] getChildren(Object parent) {
		if (parent instanceof TranslatableMenuItem) {
			return ((TranslatableMenuItem)parent).getChildren().toArray();
		}		

		// It may be TranslatableText, not wrapped in menu stuff.
		// It will never have child objects, because menu labels do not contain
		// formatted text.
		// (Returning null causes exception)
		return new Object[0];
	}

	/**
	 * @param children
	 */
/*	
	private void addShowViewChildren(ArrayList<Object> children) {
		// If no page disable all.
		IWorkbenchPage page = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage();
		if (page == null) {
			return;
		}

		// If no active perspective disable all
		if (page.getPerspective() == null) {
			return;
		}

		// Get visible actions.
		List viewIds = Arrays.asList(page.getShowViewShortcuts());

		// add all open views
		viewIds = addOpenedViews(page, viewIds);

		IViewRegistry reg = WorkbenchPlugin.getDefault().getViewRegistry();

		List<IViewDescriptor> actions = new ArrayList<IViewDescriptor>(viewIds.size());
		for (Iterator i = viewIds.iterator(); i.hasNext();) {
			String id = (String) i.next();
			if (id.equals(IIntroConstants.INTRO_VIEW_ID)) {
				continue;
			}

			// TODO: check activity

			IViewDescriptor desc = reg.find(id);
			if (desc != null) {
				IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI
				.getWorkbench().getActivitySupport();

				String pluginId =
					desc instanceof IPluginContribution 
					? ((IPluginContribution) desc).getPluginId()
							: null;

					String localId = desc.getId();
					String unifiedId =  (pluginId != null) 
					? pluginId + '/' + localId 
							: localId;

					IIdentifier identifier = workbenchActivitySupport
					.getActivityManager().getIdentifier(
							unifiedId);
					if (!identifier.isEnabled()) {
						continue;
					}

					actions.add(desc);
			}

		}

		Collections.sort(actions, actionComparator);

		for (Iterator i = actions.iterator(); i.hasNext();) {
			IViewDescriptor action = (IViewDescriptor) i.next();

			ITranslatableText localizableText = extractFromPluginXml("org.eclipse.ui.views", "view", action.getId(), "name", false);
			children.add(localizableText);
		}

//						// Add Other ..

//						// Build resource bundle for WorkbenchMessages.
//						TranslatableResourceBundle resourceBundle = (TranslatableResourceBundle)ResourceBundle.getBundle(
//						"org.eclipse.ui.internal.messages", // WorkbenchMessages.BUNDLE_NAME //$NON-NLS-1$
//						Locale.getDefault(),
//						org.eclipse.ui.internal.WorkbenchPlugin.class.getClassLoader(),
//						new UpdatableResourceControl(org.eclipse.ui.internal.WorkbenchPlugin.getDefault().getStateLocation()));
//						TranslatableResourceBundle.register(resourceBundle, org.eclipse.ui.internal.WorkbenchPlugin.getDefault().getBundle());


//						ITranslatableText localizableText = new TranslatableText(resourceBundle, "ShowView_title");

//						TranslatableTextInput textInput = new TranslatableTextInput(localizableText) {
//						@Override
//						public void updateControl(String text) {
//						WorkbenchMessages.ShowView_title = text;
//						// The menu title should update????
//						}
//						};
	}

	private ITranslatableText extractFromPluginXml(String extensionPointId, String elementName, String subElementName, String id, String labelAttributeName, boolean localMatchOnly) {
		for (IConfigurationElement element: Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			if (element.getName().equals(elementName)) {
				for (IConfigurationElement subElement: element.getChildren(subElementName)) {
					String thisId = subElement.getAttribute("id");
					boolean idMatches = (localMatchOnly ? (thisId.endsWith("." + id)) : thisId.equals(id));
					if (idMatches) {
						String contributorName = element.getDeclaringExtension().getContributor().getName();
						Bundle bundle = InternalPlatform.getDefault().getBundle(contributorName);
						String fullId = subElement.getAttribute("id");

						try {
							LocalizableContribution contribution = PluginXmlRegistry.getInstance().getLocalizableContribution(bundle);
							return contribution.getLocalizableText(extensionPointId, elementName, subElementName, fullId, labelAttributeName);
						} catch (Exception e1) {
							/*
							 * If there were any problems reading the plugin.xml, return the text from
							 * the extension registry.  This is the localized text, without any information
							 * as to the source property file and key, so it must be returned as non-localizable
							 * text.
							 * /
							e1.printStackTrace();
							return new NonTranslatableText(element.getAttribute(labelAttributeName));
						}
					}
				}
			}
		}
		return null;
	}

	private ITranslatableText extractFromPluginXml(String extensionPointId, String elementName, String id, String labelAttributeName, boolean localMatchOnly) {

		for (IConfigurationElement element: Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			if ((element.getName().equals(elementName))) {
				String thisId = element.getAttribute("id");
				boolean idMatches = (localMatchOnly ? (thisId.endsWith("." + id)) : thisId.equals(id));
				if (idMatches) {
					String contributorName = element.getDeclaringExtension().getContributor().getName();
					Bundle bundle = InternalPlatform.getDefault().getBundle(contributorName);
					String fullId = element.getAttribute("id");

					try {
						LocalizableContribution contribution = PluginXmlRegistry.getInstance().getLocalizableContribution(bundle);
						return contribution.getLocalizableText(extensionPointId, elementName, fullId, labelAttributeName);
					} catch (Exception e1) {
						/*
						 * If there were any problems reading the plugin.xml, return the text from
						 * the extension registry.  This is the localized text, without any information
						 * as to the source property file and key, so it must be returned as non-localizable
						 * text.
						 * /
						e1.printStackTrace();
						return new NonTranslatableText(element.getAttribute(labelAttributeName));
					}
				}
			}
		}

		return null;
	}
*/
	public boolean hasChildren(Object parent) {
		return getChildren(parent).length != 0;
	}

}