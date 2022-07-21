/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2019 Gustl22
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25.renderer;

import static org.oscim.backend.GLAdapter.gl;

import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.util.Util;

import org.oscim.backend.AssetAdapter;
import org.oscim.backend.GL;
import org.oscim.backend.GLAdapter;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GLShaderMT {

   static final Logger log = LoggerFactory.getLogger(GLShaderMT.class);

   public int          program;

   public static int createProgram(final String vertexSource, final String fragmentSource) {
      return createProgramDirective(vertexSource, fragmentSource, null);
   }

   public static int createProgramDirective(final String vertexSource, final String fragmentSource, final String directives) {

      String defs = "";
      if (directives != null) {
         defs += directives + "\n";
      }

      if (GLAdapter.GDX_DESKTOP_QUIRKS) {
         defs += "#define DESKTOP_QUIRKS 1\n";
      } else {
         defs += "#define GLES 1\n";
      }

      defs += "#define GLVERSION " + (GLAdapter.isGL30() ? "30" : "20") + "\n";

      final int vertexShader = loadShader(GL.VERTEX_SHADER, defs + vertexSource);
      if (vertexShader == 0) {
         return 0;
      }

      final int pixelShader = loadShader(GL.FRAGMENT_SHADER, defs + fragmentSource);
      if (pixelShader == 0) {
         return 0;
      }

      int program = gl.createProgram();
      if (program != 0) {

         GLUtils.checkGlError(GLShaderMT.class.getName() + ": glCreateProgram");
         gl.attachShader(program, vertexShader);

         GLUtils.checkGlError(GLShaderMT.class.getName() + ": glAttachShader");
         gl.attachShader(program, pixelShader);

         GLUtils.checkGlError(GLShaderMT.class.getName() + ": glAttachShader");
         gl.linkProgram(program);

         final IntBuffer linkStatus = MapRenderer.getIntBuffer(1);
         gl.getProgramiv(program, GL.LINK_STATUS, linkStatus);
         linkStatus.position(0);

         if (linkStatus.get() != GL.TRUE) {
            log.error("Could not link program: ");
            log.error(gl.getProgramInfoLog(program));
            gl.deleteProgram(program);
            program = 0;
         }
      }

      return program;
   }

   public static int loadShader(final int shaderType, final String source) {

      int shader = gl.createShader(shaderType);
      if (shader != 0) {

         gl.shaderSource(shader, source);
         gl.compileShader(shader);
         final IntBuffer compiled = MapRenderer.getIntBuffer(1);

         gl.getShaderiv(shader, GL.COMPILE_STATUS, compiled);
         compiled.position(0);

         if (compiled.get() == 0) {
            log.error("Could not compile shader " + shaderType + ":");
            log.error(gl.getShaderInfoLog(shader));
            gl.deleteShader(shader);
            shader = 0;
         }
      }

      return shader;
   }

   public static int loadShader(final String file) {
      return loadShaderDirective(file, null);
   }

   public static int loadShaderDirective(final String file, final String directives) {

      final String path = "shaders/" + file + ".glsl";
      String vs = AssetAdapter.readTextFile(path);

      if (vs == null) {
         throw new IllegalArgumentException("shader file not found: " + path);
      }

      // TODO ...
      final int fsStart = vs.indexOf('$');
      if (fsStart < 0 || vs.charAt(fsStart + 1) != '$') {
         throw new IllegalArgumentException("not a shader file " + path);
      }

      final String fs = vs.substring(fsStart + 2);
      vs = vs.substring(0, fsStart);

      final int shader = createProgramDirective(vs, fs, directives);
      if (shader == 0) {
         System.out.println(vs + " \n\n" + fs);
      }

      return shader;
   }

   public static int loadShaderDirectiveMT(final String file, final String directives) {

      final String path = "shaders/" + file + ".glsl";
//      final String vs = AssetAdapter.readTextFile(path);

      final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(path);
      String fileURL = null;
      try {
         fileURL = NIO.getAbsolutePathFromBundleUrl(bundleUrl);
      } catch (final IOException e1) {
         e1.printStackTrace();
      }
      String vs = Util.readContentFromFile(fileURL);

      if (vs == null) {
         throw new IllegalArgumentException("shader file not found: " + path);
      }

      // TODO ...
      final int fsStart = vs.indexOf('$');
      if (fsStart < 0 || vs.charAt(fsStart + 1) != '$') {
         throw new IllegalArgumentException("not a shader file " + path);
      }

      final String fs = vs.substring(fsStart + 2);
      vs = vs.substring(0, fsStart);

      final int shader = createProgramDirective(vs, fs, directives);
      if (shader == 0) {
         System.out.println(vs + " \n\n" + fs);
      }

      return shader;
   }

   protected boolean create(final String fileName) {
      return createDirective(fileName, null);
   }

   protected boolean create(final String vertexSource, final String fragmentSource) {
      return createVersioned(vertexSource, fragmentSource, null);
   }

   protected boolean createDirective(final String fileName, final String directives) {
      program = loadShaderDirective(fileName, directives);
      return program != 0;
   }

   protected boolean createDirective(final String vertexSource, final String fragmentSource, final String directives) {
      program = createProgramDirective(vertexSource, fragmentSource, directives);
      return program != 0;
   }

   protected boolean createDirectiveMT(final String fileName, final String directives) {
      program = loadShaderDirectiveMT(fileName, directives);
      return program != 0;
   }

   protected boolean createMT(final String fileName) {
      return createDirectiveMT(fileName, null);
   }

   protected boolean createVersioned(final String fileName, final String version) {
      program = loadShaderDirective(fileName, version == null ? null : ("#version " + version + "\n"));
      return program != 0;
   }

   protected boolean createVersioned(final String vertexSource, final String fragmentSource, final String version) {
      program = createProgramDirective(vertexSource, fragmentSource, version == null ? null : ("#version " + version + "\n"));
      return program != 0;
   }

   protected int getAttrib(final String name) {
      final int loc = gl.getAttribLocation(program, name);
      if (loc < 0) {
         log.debug("missing attribute: {}", name);
      }
      return loc;
   }

   protected int getUniform(final String name) {
      final int loc = gl.getUniformLocation(program, name);
      if (loc < 0) {
         log.debug("missing uniform: {}", name);
      }
      return loc;
   }

   public boolean useProgram() {
      return GLState.useProgram(program);
   }
}
