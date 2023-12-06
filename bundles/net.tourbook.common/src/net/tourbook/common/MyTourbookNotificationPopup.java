/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package net.tourbook.common;

import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MyTourbookNotificationPopup extends AbstractNotificationPopup {

   private ImageDescriptor IMAGEDESCRIPTOR;

   private String          _title;
   private String          _text;

   MyTourbookNotificationPopup(final Display display,
                               final ImageDescriptor imageDescriptor,
                               final String title,
                               final String text) {
      super(display);
      IMAGEDESCRIPTOR = imageDescriptor;
      _title = title;
      _text = text;
   }

   @Override
   protected void createContentArea(final Composite parent) {

      final Composite notificationComposite = new Composite(parent, SWT.NO_FOCUS);
      GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(notificationComposite);
      notificationComposite.setLayout(new GridLayout(2, false));
      notificationComposite.setBackground(parent.getBackground());

      final Label notificationLabelIcon = new Label(notificationComposite, SWT.NO_FOCUS);
      notificationLabelIcon.setBackground(parent.getBackground());

      if (StringUtils.hasContent(_text)) {

         final Text descriptionLabel = new Text(notificationComposite, SWT.NO_FOCUS | SWT.WRAP | SWT.MULTI);
         descriptionLabel.setText(_text);
         descriptionLabel.setBackground(parent.getBackground());
         GridDataFactory.fillDefaults()
               .span(2, SWT.DEFAULT)
               .grab(true, true)
               .align(SWT.FILL, SWT.FILL)
               .applyTo(descriptionLabel);
      }
   }

   @Override
   protected Image getPopupShellImage(final int maximumHeight) {
      return IMAGEDESCRIPTOR.createImage();
   }

   @Override
   protected String getPopupShellTitle() {
      return _title;
   }

}
