/*
 * Copyright (C) 2024 Aleksei Balan
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

import java.awt.*;
import java.util.function.Consumer;

public interface Tui extends AutoCloseable {

  Dimension getSize();

  /**
   * @param attr in ANSI order - KRGYBMCW - flash, bb, bg, br, bright, fb, fg, fr.
   */
  void print(int x, int y, String s, int attr);

  // also paint, repaint, refresh
  void update();

  /**
   * To make key events consistent among different software, this guideline should be used.
   * Modifier keys have first letter capitalized and concatenated with "+" in this order "Ctrl", "Alt", "Shift".
   * If shift alternates the character, it must be omitted from the notation. Use "Alt+?" instead of "Alt+Shift+/".
   * Examples: "Up", "Down", "Tab", "Esc", "Ctrl+Alt+Shift+F1", "Ctrl+Alt+a", "@"
   * @param keyListener null to unset
   */
  void setKeyListener(Consumer<String> keyListener);

  @Override
  void close();
}
