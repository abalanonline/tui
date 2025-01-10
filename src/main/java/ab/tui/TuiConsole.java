/*
 * Copyright (C) 2025 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.tui;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class TuiConsole implements Tui {

  private final PoorTerminal terminal;
  private Consumer<String> keyListener = null;
  private boolean stop;
  private final Thread thread;

  public static final int UPDATED_BIT = 0x100;
  private final Object colorLock = new Object();
  int[][] color = new int[0][];
  char[][] c = new char[0][];
  Dimension lastSize;

  public TuiConsole() {
    terminal = new PoorTerminal();
    terminal.enterPrivateMode();
    terminal.clearScreen();
    terminal.setCursorVisible(false);

    thread = new Thread(this::run);
    thread.start();
  }

  @Override
  public void close() {
    stop = true;
    try {
      if (!thread.equals(Thread.currentThread())) thread.join();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
    terminal.exitPrivateMode();
    terminal.close();
  }

  @Override
  public Dimension getSize() {
    return terminal.size;
  }

  @Override
  public void print(int x, int y, String s, int attr) {
    if (stop) throw new IllegalStateException("closed");
    int x1 = x + s.length();
    synchronized (colorLock) {
      int length = color.length;
      if (length <= y) {
        color = Arrays.copyOf(color, y + 1);
        c = Arrays.copyOf(c, y + 1);
        for (int i = length; i <= y; i++) {
          color[i] = new int[x1];
          Arrays.fill(color[i], -1);
          c[i] = new char[x1];
        }
      }
      int[] rcolor = color[y];
      char[] rc = c[y];
      length = rcolor.length;
      if (length < x1) {
        rcolor = color[y] = Arrays.copyOf(rcolor, x1);
        rc = c[y] = Arrays.copyOf(rc, x1);
        Arrays.fill(rcolor, length, x1, -1);
      }
      Arrays.fill(rcolor, x, x1, attr);
      for (int i = 0; x < x1;) rc[x++] = s.charAt(i++);
    }
  }

  @Override
  public void update() {
    synchronized (colorLock) {
      Dimension size = terminal.getTerminalSize();
      final boolean sizeChanged = !size.equals(lastSize);
      lastSize = size;
      terminal.resetColorAndSGR();
      StringBuilder stringBuilder = new StringBuilder();
      int height = Math.min(lastSize.height, color.length);
      for (int y = 0; y < height; y++) {
        int co = -1;
        char[] cy = this.c[y];
        int width = Math.min(lastSize.width, cy == null ? 0 : cy.length);
        for (int x = 0; x < width; x++) {
          boolean u = this.color[y][x] < UPDATED_BIT || sizeChanged;
          int cn = u ? this.color[y][x] : -1;
          if (!Objects.equals(cn, co)) {
            if (stringBuilder.length() > 0) {
              terminal.putString(stringBuilder.toString());
              stringBuilder.setLength(0);
            }
            if (co < 0) terminal.setCursorPosition(x, y);
            if (cn >= 0) {
              terminal.setColor(this.color[y][x]);
            }
            co = cn;
          }
          if (cn >= 0) stringBuilder.append(this.c[y][x]);
          this.color[y][x] |= UPDATED_BIT;
        }
        if (stringBuilder.length() > 0) {
          terminal.putString(stringBuilder.toString());
          stringBuilder.setLength(0);
        }
      }
      terminal.flush();
    }
  }

  private void run() {
    try {
      while (!stop) {
        String key = terminal.pollInput();
        if ("F8".equals(key)) {
          terminal.clearScreen();
          key = null;
        }
        if (key == null) {
          Thread.sleep(10);
          continue;
        }
        Consumer<String> keyListener = this.keyListener;
        if (keyListener != null) try {
          keyListener.accept(key);
        } catch (RuntimeException e) {
          TuiUtil.logError(this, "exception in keyListener", e);
        }
      }
    } catch (Exception e) {
      TuiUtil.logError(this, "Thread", e);
    }
  }

  @Override
  public void setKeyListener(Consumer<String> keyListener) {
    this.keyListener = keyListener;
  }

}
