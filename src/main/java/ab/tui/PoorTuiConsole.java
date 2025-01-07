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

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.Terminal;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

public class PoorTuiConsole implements Tui {

  public static final TextColor[] TEXT_COLORS = {
      new PoorTextColor("0;30", "40"), new PoorTextColor("0;31", "41"),
      new PoorTextColor("0;32", "42"), new PoorTextColor("0;33", "43"),
      new PoorTextColor("0;34", "44"), new PoorTextColor("0;35", "45"),
      new PoorTextColor("0;36", "46"), new PoorTextColor("0;37", "47"),
      new PoorTextColor("1;30", "100"), new PoorTextColor("1;31", "101"),
      new PoorTextColor("1;32", "102"), new PoorTextColor("1;33", "103"),
      new PoorTextColor("1;34", "104"), new PoorTextColor("1;35", "105"),
      new PoorTextColor("1;36", "106"), new PoorTextColor("1;37", "107"),
  };
  private final Terminal terminal;
  private final Screen screen;
  private Consumer<String> keyListener = null;
  private boolean stop;
  private final Thread thread;

  public PoorTuiConsole() {
    try {
      terminal = new PoorTerminal();
      screen = new PoorScreen(terminal);
      screen.startScreen();
      screen.setCursorPosition(null);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    thread = new Thread(this::run);
    thread.start();
  }

  @Override
  public void close() {
    stop = true;
    try {
      if (!thread.equals(Thread.currentThread())) thread.join();
      screen.stopScreen();
      terminal.close();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Dimension getSize() {
    TerminalSize size;
    try {
      size = terminal.getTerminalSize();
    } catch (IOException e) {
      size = screen.getTerminalSize();
    }
    return new Dimension(size.getColumns(), size.getRows());
  }

  @Override
  public void print(int x, int y, String s, int attr) {
    if (stop) throw new IllegalStateException("closed");
    final TextCharacter[] cs = TextCharacter.fromString(s, TEXT_COLORS[attr & 15], TEXT_COLORS[attr >> 4 & 7]);
    for (TextCharacter c : cs) screen.setCharacter(x++, y, c);
  }

  @Override
  public void update() {
    try {
      screen.refresh();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String getCharacterNotation(KeyStroke key) {
    char c = key.getCharacter();
    switch (c) {
      case '\u001A': c = key.isShiftDown() ? 'Z' : 'z'; break;
      case '\u001C': c = key.isShiftDown() ? '|' : '\\'; break;
      case '\u001D': c = key.isShiftDown() ? '}' : ']'; break;
      default:
        return "?";
    }
    StringBuilder keyNotation = new StringBuilder();
    if (key.isCtrlDown()) keyNotation.append("Ctrl+");
    if (key.isAltDown()) keyNotation.append("Alt+");
    keyNotation.append(c);
    return keyNotation.toString();
  }

  private String getKeyNotation(KeyStroke key) {
    StringBuilder keyNotation = new StringBuilder();
    if (key.isCtrlDown()) keyNotation.append("Ctrl+");
    if (key.isAltDown()) keyNotation.append("Alt+");
    if (key.isShiftDown() && key.getKeyType() != KeyType.Character) keyNotation.append("Shift+");
    switch (key.getKeyType()) {
      case Character:
        char character = key.getCharacter();
        if (character < 0x20) return getCharacterNotation(key);
        keyNotation.append(character);
        break;
      case EOF:
        close();
        keyNotation.append("Close");
        break;
      case ArrowLeft:
      case ArrowDown:
      case ArrowUp:
      case ArrowRight:
        keyNotation.append(key.getKeyType().toString().substring(5));
        break;
      case ReverseTab:
        keyNotation.append("Shift+Tab");
        break;
      case Escape:
        keyNotation.append("Esc");
        break;
      default:
        keyNotation.append(key.getKeyType());
    }
    return keyNotation.toString();
  }

  private void run() {
    try {
      while (!stop) {
        KeyStroke key = terminal.pollInput();
        if (key == null) {
          Thread.sleep(10);
          continue;
        }
        Consumer<String> keyListener = this.keyListener;
        if (keyListener != null) try {
          keyListener.accept(getKeyNotation(key));
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
