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
package net.tourbook.map25.renderer;

import static org.oscim.backend.GLAdapter.gl;

import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.oscim.backend.GL;
import org.oscim.backend.GLAdapter;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copied/adjusted from {@link org.oscim.renderer.GLShader}
 */
public abstract class GLShaderMT {

   private static final char NL                   = UI.NEW_LINE;

   static final Logger       log                  = LoggerFactory.getLogger(GLShaderMT.class);

   private static boolean    IS_LOG_SHADER_SOURCE = false;

   private int               _programID;

   protected int getAttrib(final String name) {

      final int loc = gl.getAttribLocation(_programID, name);

      if (loc < 0) {
         log.debug("Missing attribute: {}", name); //$NON-NLS-1$
      }

      return loc;
   }

   protected int getUniform(final String name) {

      final int loc = gl.getUniformLocation(_programID, name);
      if (loc < 0) {
         log.debug("Missing uniform: {}", name); //$NON-NLS-1$
      }

      return loc;
   }

   protected boolean loadShader(final String fileName, final String directives) {

      _programID = loadShader_10(fileName, directives);

      return _programID != 0;
   }

   /**
    * Load shader file from the plugin bundle filepath
    *
    * @param file
    * @param directives
    * @return
    */
   private int loadShader_10(final String file, final String directives) {

      final String path = "shaders/" + file + ".glsl"; //$NON-NLS-1$ //$NON-NLS-2$

      final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(path);
      if (bundleUrl == null) {
         throw new IllegalArgumentException("Shader file is not in bundle: " + path); //$NON-NLS-1$
      }

      String fileURL = null;
      try {
         fileURL = NIO.getAbsolutePathFromBundleUrl(bundleUrl);
      } catch (final IOException e) {
         e.printStackTrace();
      }

      String shaderFileContent = Util.readContentFromFile(fileURL);
      if (shaderFileContent == null) {
         throw new IllegalArgumentException("Shader file not found: " + path); //$NON-NLS-1$
      }

      final int fragmentShaderStart = shaderFileContent.indexOf('$');
      if (fragmentShaderStart < 0 || shaderFileContent.charAt(fragmentShaderStart + 1) != '$') {
         throw new IllegalArgumentException("Not a shader file " + path); //$NON-NLS-1$
      }

      final String fragmentShaderCode = shaderFileContent.substring(fragmentShaderStart + 2);
      shaderFileContent = shaderFileContent.substring(0, fragmentShaderStart);

      final int shaderID = loadShader_20_AttachShader(shaderFileContent, fragmentShaderCode, directives, fileURL);
      if (shaderID == 0) {
         System.out.println(shaderFileContent + " \n\n" + fragmentShaderCode); //$NON-NLS-1$
      }

      return shaderID;
   }

   /**
    * @param vertexSource
    * @param fragmentSource
    * @param directives
    * @param fileURL
    * @return Returns OpenGL program ID
    */
   private int loadShader_20_AttachShader(final String vertexSource,
                                          final String fragmentSource,
                                          final String directives,
                                          final String fileURL) {

      String sourceDefinitions = ""; //$NON-NLS-1$
      if (directives != null) {
         sourceDefinitions += directives + "\n"; //$NON-NLS-1$
      } else {
         sourceDefinitions += "#version 120 \n"; //$NON-NLS-1$
      }

      if (GLAdapter.GDX_DESKTOP_QUIRKS) {
         sourceDefinitions += "#define DESKTOP_QUIRKS 1\n"; //$NON-NLS-1$
      } else {
         sourceDefinitions += "#define GLES 1\n"; //$NON-NLS-1$
      }

      final int vertexShaderID = loadShader_30_CompileShader(GL.VERTEX_SHADER, sourceDefinitions + vertexSource, fileURL);
      if (vertexShaderID == 0) {
         return 0;
      }

      final int fragmentShaderID = loadShader_30_CompileShader(GL.FRAGMENT_SHADER, sourceDefinitions + fragmentSource, fileURL);
      if (fragmentShaderID == 0) {
         return 0;
      }

      int programID = gl.createProgram();
      GLUtils.checkGlError(GLShaderMT.class.getName() + ": glCreateProgram"); //$NON-NLS-1$

      if (programID != 0) {

         gl.attachShader(programID, vertexShaderID);
         GLUtils.checkGlError(GLShaderMT.class.getName() + ": glAttachShader vertex"); //$NON-NLS-1$

         gl.attachShader(programID, fragmentShaderID);
         GLUtils.checkGlError(GLShaderMT.class.getName() + ": glAttachShader fragment"); //$NON-NLS-1$

         gl.linkProgram(programID);

         final IntBuffer linkStatus = MapRenderer.getIntBuffer(1);
         gl.getProgramiv(programID, GL.LINK_STATUS, linkStatus);
         linkStatus.position(0);

         if (linkStatus.get() != GL.TRUE) {

            log.error("Could not link program: "); //$NON-NLS-1$
            log.error(gl.getProgramInfoLog(programID));

            gl.deleteProgram(programID);
            programID = 0;
         }
      }

      return programID;
   }

   /**
    * @param shaderType
    * @param source
    * @param fileURL
    * @return Returns OpenGL shader ID
    */
   private int loadShader_30_CompileShader(final int shaderType, final String source, final String fileURL) {

      int shaderID = gl.createShader(shaderType);

      if (shaderID != 0) {

         gl.shaderSource(shaderID, source);
         gl.compileShader(shaderID);
         final IntBuffer compiled = MapRenderer.getIntBuffer(1);

         gl.getShaderiv(shaderID, GL.COMPILE_STATUS, compiled);
         compiled.position(0);

         final String shaderLog = gl.getShaderInfoLog(shaderID);

         if (compiled.get() == 0) {

            // log errors

            System.out.println(fileURL + NL);
            log.error("Could not compile shader ID " + shaderType); //$NON-NLS-1$
            log.error(NL + shaderLog);
            System.out.println(Util.addLineNumbers(source, 0));

            gl.deleteShader(shaderID);
            shaderID = 0;

         } else if (shaderLog != null && shaderLog.length() > 0) {

            // log warnings

            log.error(NL + shaderLog);
            System.out.println(fileURL + NL);
            System.out.println(Util.addLineNumbers(source, 0));

         } else if (IS_LOG_SHADER_SOURCE) {

            System.out.println(fileURL + NL);
            System.out.println(Util.addLineNumbers(source, 0));
         }
      }

      return shaderID;
   }

   public boolean useProgram() {

      return GLState.useProgram(_programID);
   }
}
