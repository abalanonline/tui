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

import com.googlecode.lanterna.TextColor;

import java.awt.Color;

public class PoorTextColor implements TextColor {
  private final String fg;
  private final String bg;

  public PoorTextColor(String fg, String bg) {
    this.fg = fg;
    this.bg = bg;
  }

  @Override
  public byte[] getForegroundSGRSequence() {
    return fg.getBytes();
  }

  @Override
  public byte[] getBackgroundSGRSequence() {
    return bg.getBytes();
  }

  @Override
  public int getRed() {
    throw new IllegalStateException();
  }

  @Override
  public int getGreen() {
    throw new IllegalStateException();
  }

  @Override
  public int getBlue() {
    throw new IllegalStateException();
  }

  @Override
  public Color toColor() {
    throw new IllegalStateException();
  }
}
