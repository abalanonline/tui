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

import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorPalette;

import java.awt.*;

public class LanternaColor {

  private static Color m(int i, int c) {
    double luma = (c >> 2 & 1) * 0.299 + (c >> 1 & 1) * 0.587 + (c & 1) * 0.114;
    c = (int) (Math.round(luma * 0xAA)) + i * 0x55;
    return new Color(c, c, c);
  }
  // fg, bright, bg, black, red, green, yellow, blue, magenta, cyan, white

  public static TerminalEmulatorColorConfiguration fromColor(int... color) {
    TerminalEmulatorPalette palette = new TerminalEmulatorPalette(
        new Color(color[17]), new Color(color[18]), new Color(color[16]),
        new Color(color[0]), new Color(color[8]), new Color(color[4]), new Color(color[8 + 4]),
        new Color(color[2]), new Color(color[8 + 2]), new Color(color[6]), new Color(color[8 + 6]),
        new Color(color[1]), new Color(color[8 + 1]), new Color(color[5]), new Color(color[8 + 5]),
        new Color(color[3]), new Color(color[8 + 3]), new Color(color[7]), new Color(color[8 + 7]));
    return TerminalEmulatorColorConfiguration.newInstance(palette);
  }

  public static final TerminalEmulatorColorConfiguration VGA_MONO = TerminalEmulatorColorConfiguration.newInstance(
      new TerminalEmulatorPalette(m(0, 7), m(1, 7), m(0, 0),
      m(0, 0), m(1, 0), m(0, 4), m(1, 4), m(0, 2), m(1, 2), m(0, 6), m(1, 6),
      m(0, 1), m(1, 1), m(0, 5), m(1, 5), m(0, 3), m(1, 3), m(0, 7), m(1, 7)));

}
