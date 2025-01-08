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

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TabBehaviour;
import com.googlecode.lanterna.terminal.Terminal;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Objects;

public class PoorScreen implements Screen {
  public static final int MAXH = 250;
  public static final int MAXW = 800;
  private final Terminal terminal;
  char[][] c = new char[MAXH][MAXW];
  TextCharacter[][] color = new TextCharacter[MAXH][MAXW];
  boolean[][] update = new boolean[MAXH][MAXW];
  Dimension lastSize;
  boolean sizeChanged;

  public PoorScreen(Terminal terminal) {
    this.terminal = terminal;
  }

  @Override
  public void startScreen() throws IOException {
    terminal.enterPrivateMode();
    terminal.clearScreen();
    terminal.setCursorVisible(false);
  }

  @Override
  public void close() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stopScreen() throws IOException {
    terminal.exitPrivateMode();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TerminalPosition getCursorPosition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCursorPosition(TerminalPosition position) {
  }

  @Override
  public TabBehaviour getTabBehaviour() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTabBehaviour(TabBehaviour tabBehaviour) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TerminalSize getTerminalSize() {
    try {
      TerminalSize size = terminal.getTerminalSize();
      Dimension dimension = new Dimension(size.getColumns(), size.getRows());
      if (!dimension.equals(lastSize)) {
        lastSize = dimension;
        sizeChanged = true;
      }
      return size;
    } catch (IOException e) {
      return new TerminalSize(80, 24);
    }
  }

  public String getColor(TextCharacter textCharacter) {
    if (textCharacter == null) return null;
    return new String(textCharacter.getForegroundColor().getForegroundSGRSequence()) + ";" +
        new String(textCharacter.getBackgroundColor().getBackgroundSGRSequence());
  }

  @Override
  public void setCharacter(int column, int row, TextCharacter screenCharacter) {
    c[row][column] = screenCharacter.getCharacterString().charAt(0);
    color[row][column] = screenCharacter;
    update[row][column] = true;
  }

  @Override
  public void setCharacter(TerminalPosition position, TextCharacter screenCharacter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextGraphics newTextGraphics() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextCharacter getFrontCharacter(int column, int row) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextCharacter getFrontCharacter(TerminalPosition position) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextCharacter getBackCharacter(int column, int row) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextCharacter getBackCharacter(TerminalPosition position) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void refresh() throws IOException {
    getTerminalSize();
    boolean sizeChanged = this.sizeChanged;
    this.sizeChanged = false;
    terminal.resetColorAndSGR();
    StringBuilder stringBuilder = new StringBuilder(2 * MAXW);
    for (int y = 0; y < lastSize.height; y++) {
      String co = null;
      for (int x = 0; x < lastSize.width; x++) {
        boolean u = (this.update[y][x] || sizeChanged) && this.c[y][x] != 0;
        String cn = u ? getColor(this.color[y][x]) : null;
        if (!Objects.equals(cn, co)) {
          if (stringBuilder.length() > 0) {
            terminal.putString(stringBuilder.toString());
            stringBuilder.setLength(0);
          }
          if (co == null) terminal.setCursorPosition(x, y);
          if (cn != null) {
            terminal.setForegroundColor(this.color[y][x].getForegroundColor());
            terminal.setBackgroundColor(this.color[y][x].getBackgroundColor());
          }
          co = cn;
        }
        if (cn != null) stringBuilder.append(this.c[y][x]);
        this.update[y][x] = false;
      }
      if (stringBuilder.length() > 0) {
        terminal.putString(stringBuilder.toString());
        stringBuilder.setLength(0);
      }
    }
    terminal.flush();
  }

  @Override
  public void refresh(RefreshType refreshType) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public TerminalSize doResizeIfNecessary() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void scrollLines(int firstLine, int lastLine, int distance) {
    throw new UnsupportedOperationException();
  }

  @Override
  public KeyStroke pollInput() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public KeyStroke readInput() throws IOException {
    throw new UnsupportedOperationException();
  }
}
