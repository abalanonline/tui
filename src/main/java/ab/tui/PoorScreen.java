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

public class PoorScreen {
  private final PoorTerminal terminal;
  char[][] c = new char[0][];
  int[][] color = new int[0][];
  boolean[][] update = new boolean[0][];
  Dimension lastSize;
  boolean sizeChanged;

  public PoorScreen(PoorTerminal terminal) {
    this.terminal = terminal;
  }

  public void startScreen() {
    terminal.enterPrivateMode();
    terminal.clearScreen();
    terminal.setCursorVisible(false);
  }

  public void stopScreen() {
    terminal.exitPrivateMode();
  }

  public Dimension getTerminalSize() {
    Dimension size = terminal.getTerminalSize();
    if (!size.equals(lastSize)) {
      lastSize = size;
      sizeChanged = true;
    }
    return size;
  }

  public void setCharacter(int column, int row, char screenCharacterChar, int screenCharacterColor) {
    if (c.length <= row) c = Arrays.copyOf(c, row + 1);
    char[] rc = c[row];
    if (rc == null) rc = c[row] = new char[column + 1];
    if (rc.length <= column) rc = c[row] = Arrays.copyOf(rc, column + 1);
    rc[column] = screenCharacterChar;

    if (color.length <= row) color = Arrays.copyOf(color, row + 1);
    int[] rcolor = color[row];
    if (rcolor == null) rcolor = color[row] = new int[column + 1];
    if (rcolor.length <= column) rcolor = color[row] = Arrays.copyOf(rcolor, column + 1);
    rcolor[column] = screenCharacterColor;

    if (update.length <= row) update = Arrays.copyOf(update, row + 1);
    boolean[] rupdate = update[row];
    if (rupdate == null) rupdate = update[row] = new boolean[column + 1];
    if (rupdate.length <= column) rupdate = update[row] = Arrays.copyOf(rupdate, column + 1);
    rupdate[column] = true;
  }

  public synchronized void refresh() {
    getTerminalSize();
    boolean sizeChanged = this.sizeChanged;
    this.sizeChanged = false;
    terminal.resetColorAndSGR();
    StringBuilder stringBuilder = new StringBuilder();
    int height = Math.min(lastSize.height, c.length);
    for (int y = 0; y < height; y++) {
      int co = -1;
      char[] cy = this.c[y];
      int width = Math.min(lastSize.width, cy == null ? 0 : cy.length);
      for (int x = 0; x < width; x++) {
        boolean u = (this.update[y][x] || sizeChanged) && this.c[y][x] != 0;
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
        this.update[y][x] = false;
      }
      if (stringBuilder.length() > 0) {
        terminal.putString(stringBuilder.toString());
        stringBuilder.setLength(0);
      }
    }
    terminal.flush();
  }
}
