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

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

public class PoorTerminal implements Terminal {

  public PoorTerminal() {
    systemExec("stty raw -echo");
  }

  public void systemExec(String command) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.redirectInput(new File("/dev/tty"));
      processBuilder.command(command.split("\\s+"));
      Process process = processBuilder.start();
      int exitStatus;
      try {
        exitStatus = process.waitFor();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
      String error = new String(process.getErrorStream().readAllBytes());
      if (exitStatus != 0 || !error.isEmpty()) throw new IOException(error);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void print(String s) {
    System.out.print(s);
  }

  public void csi(String s) {
    print("\u001B[" + s);
  }

  public void sgr(String s) {
    csi(s + "m");
  }

  public void sgr(byte[] bytes) {
    sgr(new String(bytes));
  }

  @Override
  public void enterPrivateMode() throws IOException {
    csi("?1049h");
    flush();
  }

  @Override
  public void exitPrivateMode() throws IOException {
    resetColorAndSGR();
    setCursorVisible(true);
    csi("?1049l");
    flush();
  }

  @Override
  public void clearScreen() throws IOException {
    csi("2J");
  }

  @Override
  public void setCursorPosition(int x, int y) throws IOException {
    csi((y + 1) + ";" + (x + 1) + "H");
  }

  @Override
  public void setCursorPosition(TerminalPosition position) throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public TerminalPosition getCursorPosition() throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void setCursorVisible(boolean visible) throws IOException {
    csi(visible ? "?25h" : "?25l");
  }

  @Override
  public void putCharacter(char c) throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void putString(String string) throws IOException {
    print(string);
  }

  @Override
  public TextGraphics newTextGraphics() throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void enableSGR(SGR sgr) throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void disableSGR(SGR sgr) throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void resetColorAndSGR() throws IOException {
    sgr("0");
  }

  @Override
  public void setForegroundColor(TextColor color) throws IOException {
    sgr(color.getForegroundSGRSequence());
  }

  @Override
  public void setBackgroundColor(TextColor color) throws IOException {
    sgr(color.getBackgroundSGRSequence());
  }

  @Override
  public void addResizeListener(TerminalResizeListener listener) {
    //throw new IllegalStateException();
  }

  @Override
  public void removeResizeListener(TerminalResizeListener listener) {
    throw new IllegalStateException();
  }

  @Override
  public TerminalSize getTerminalSize() throws IOException {
    return new TerminalSize(80, 24);
  }

  @Override
  public byte[] enquireTerminal(int timeout, TimeUnit timeoutUnit) throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void bell() throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void flush() throws IOException {
    System.out.flush();
  }

  @Override
  public void close() throws IOException {
    exitPrivateMode();
    systemExec("stty -raw echo");
  }

  public String readCsi() throws IOException {
    StringBuilder s = new StringBuilder();
    while (System.in.available() > 0) {
      char c = (char) System.in.read();
      s.append(c);
      if (c >= 'A' && c <= 'Z' || c == '~') break;
    }
    return s.toString();
  }

  @Override
  public KeyStroke pollInput() throws IOException {
    int available = System.in.available();
    if (available == 0) return null;
    char c = (char) System.in.read();
    switch (c) {
      case '\r':
      case '\n':
        return new KeyStroke(KeyType.Enter);
      case '\u0005': return new KeyStroke('e', true, false);
      case '\u001B':
        String csi = readCsi();
        switch (csi) {
          case "[A": return new KeyStroke(KeyType.ArrowUp);
          case "[B": return new KeyStroke(KeyType.ArrowDown);
          case "[C": return new KeyStroke(KeyType.ArrowRight);
          case "[D": return new KeyStroke(KeyType.ArrowLeft);
          default:
            return null;
        }
      default:
        return null;
    }
  }

  @Override
  public KeyStroke readInput() throws IOException {
    throw new IllegalStateException();
  }
}
