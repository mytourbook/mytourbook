package net.tourbook.map25.animation;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/**
 * https://stackoverflow.com/questions/33577427/lwjgl-how-to-create-two-different-opengl-contexts
 */
public class MultiThreadTest {

   long mainWindow, otherWindow;

   private class Runner implements Runnable {
      public int vbo;

      @Override
      public void run() {

         GLFW.glfwMakeContextCurrent(otherWindow);
         GLContext.createFromCurrent();

         final float[] vertices = new float[] { -1, -1, 0, 1, 1, -1 };
         final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(6);
         for (final float f : vertices) {
            vertexBuffer.put(f);
         }
         vertexBuffer.flip();

         vbo = GL15.glGenBuffers();
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
         GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
      }
   }

   public static void main(final String[] args) {
      new MultiThreadTest().run();
   }

   public void run() {
      if (GLFW.glfwInit() != GL11.GL_TRUE) {
         throw new IllegalStateException("Unable to initialize GLFW");
      }

      mainWindow = GLFW.glfwCreateWindow(1366, 768, "threading", MemoryUtil.NULL, MemoryUtil.NULL);
      if (mainWindow == MemoryUtil.NULL) {
         throw new RuntimeException("Failed to create the GLFW window");
      }

      GLFW.glfwSetWindowPos(mainWindow, 1080 / 2 - 1366 / 4, 30);
      GLFW.glfwShowWindow(mainWindow);

      GLFW.glfwMakeContextCurrent(mainWindow);
      GLContext.createFromCurrent();

      GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11.GL_FALSE);
      otherWindow = GLFW.glfwCreateWindow(1, 1, "", MemoryUtil.NULL, mainWindow);
      if (otherWindow == MemoryUtil.NULL) {
         throw new RuntimeException("Failed to create the GLFW window");
      }

      final Runner runner = new Runner();
      final Thread other = new Thread(runner);
      other.start();
      try {
         other.join();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }

      final Program program = new Program("shaders/2d/simple.vs", "shaders/2d/simple.fs");

      final int vao = GL30.glGenVertexArrays();

      GL30.glBindVertexArray(vao);
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, runner.vbo);
      GL20.glEnableVertexAttribArray(0);
      GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
      GL30.glBindVertexArray(0);
      GL20.glDisableVertexAttribArray(0);

      GL11.glClearColor(0.5f, 0.5f, 1f, 1);

      while (GLFW.glfwWindowShouldClose(mainWindow) != GL11.GL_TRUE) {
         GLFW.glfwPollEvents();
         GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

         program.use();
         {
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            GL30.glBindVertexArray(0);
         }
         program.unuse();

         GLFW.glfwSwapBuffers(mainWindow);
      }
   }
}
