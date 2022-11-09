/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.map25.animation;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.system.awt.AwtPanel;

import java.util.ArrayList;

public class Map25jME_Context implements JmeContext {

   protected JmeContext          actualContext;
   protected AppSettings         settings          = new AppSettings(true);
   protected SystemListener      listener;

   protected ArrayList<AwtPanel> panels            = new ArrayList<>();
   protected AwtPanel            inputSource;
   protected AwtMouseInput       mouseInput        = new AwtMouseInput();
   protected AwtKeyInput         keyInput          = new AwtKeyInput();

   protected boolean             lastThrottleState = false;

   private class AwtPanelsListener implements SystemListener {

      @Override
      public void destroy() {
         sysListener_DestroyInThread();
      }

      @Override
      public void gainFocus() {
         // shouldn't happen
         throw new IllegalStateException();
      }

      @Override
      public void handleError(final String errorMsg, final Throwable t) {
         listener.handleError(errorMsg, t);
      }

      @Override
      public void initialize() {
         sysListener_InitInThread();
      }

      @Override
      public void loseFocus() {
         // shouldn't happen
         throw new IllegalStateException();
      }

      @Override
      public void requestClose(final boolean esc) {
         // shouldn't happen
         throw new IllegalStateException();
      }

      @Override
      public void reshape(final int width, final int height) {
         throw new IllegalStateException();
      }

      @Override
      public void update() {
         sysListener_UpdateInThread();
      }
   }

   @Override
   public void create(final boolean waitFor) {

      if (actualContext != null) {
         throw new IllegalStateException("Already created");
      }

      actualContext = JmeSystem.newContext(settings, Type.OffscreenSurface);
      actualContext.setSystemListener(new AwtPanelsListener());
      actualContext.create(waitFor);
   }

   @Override
   public void destroy(final boolean waitFor) {
      // TODO Auto-generated method stub

   }

   @Override
   public JoyInput getJoyInput() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public KeyInput getKeyInput() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public MouseInput getMouseInput() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Context getOpenCLContext() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Renderer getRenderer() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public AppSettings getSettings() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Timer getTimer() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public TouchInput getTouchInput() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Type getType() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isCreated() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean isRenderable() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void restart() {
      // TODO Auto-generated method stub

   }

   @Override
   public void setAutoFlushFrames(final boolean enabled) {
      // TODO Auto-generated method stub

   }

   @Override
   public void setSettings(final AppSettings settings) {
      // TODO Auto-generated method stub

   }

   @Override
   public void setSystemListener(final SystemListener listener) {
      // TODO Auto-generated method stub

   }

   @Override
   public void setTitle(final String title) {
      // TODO Auto-generated method stub

   }

   private void sysListener_DestroyInThread() {
      listener.destroy();
   }

   private void sysListener_InitInThread() {
      listener.initialize();
   }

   private void sysListener_UpdateInThread() {

      // Check if throttle required
      boolean needThrottle = true;

      for (final AwtPanel panel : panels) {
         if (panel.isActiveDrawing()) {
            needThrottle = false;
            break;
         }
      }

      if (lastThrottleState != needThrottle) {
         lastThrottleState = needThrottle;
         if (lastThrottleState) {
            System.out.println("OGL: Throttling update loop.");
         } else {
            System.out.println("OGL: Ceased throttling update loop.");
         }
      }

      if (needThrottle) {
         try {
            Thread.sleep(100);
         } catch (final InterruptedException ex) {}
      }

      listener.update();

      for (final AwtPanel panel : panels) {
         panel.onFrameEnd();
      }
   }

}
