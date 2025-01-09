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
import java.util.function.Consumer;

public class TuiConsole implements Tui {

  private final PoorTerminal terminal;
  private final PoorScreen screen;
  private Consumer<String> keyListener = null;
  private boolean stop;
  private final Thread thread;

  public TuiConsole() {
    terminal = new PoorTerminal();
    screen = new PoorScreen(terminal);
    screen.startScreen();

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
    screen.stopScreen();
    terminal.close();
  }

  @Override
  public Dimension getSize() {
    Dimension size = terminal.getTerminalSize();
    Dimension size1 = screen.getTerminalSize();
    return size;
  }

  @Override
  public void print(int x, int y, String s, int attr) {
    if (stop) throw new IllegalStateException("closed");
    for (char c : s.toCharArray()) screen.setCharacter(x++, y, c, attr);
  }

  @Override
  public void update() {
    screen.refresh();
  }

  private void run() {
    try {
      while (!stop) {
        String key = terminal.pollInput();
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
