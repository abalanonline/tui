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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PoorTerminal {

  public Dimension size;
  private final PrintStream out;

  public PoorTerminal() {
    out = System.out;
    Map<String, String> map = Arrays.stream(systemExec("stty -a").split(";\\s+"))
        .collect(Collectors.toMap(a -> a.split("\\s", 2)[0], a -> a.split("\\s", 2)[1]));
    size = new Dimension(Integer.parseInt(map.get("columns")), Integer.parseInt(map.get("rows")));
    systemExec("stty raw -echo");
  }

  public String systemExec(String command) {
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
      if (exitStatus != 0 || !error.isEmpty()) throw new IOException(error.trim());
      return new String(process.getInputStream().readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void print(String s) {
    synchronized (out) {
      out.print(s);
    }
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

  public void enterPrivateMode() {
    csi("?1049h");
    flush();
  }

  public void exitPrivateMode() {
    resetColorAndSGR();
    setCursorVisible(true);
    csi("?1049l");
    flush();
  }

  public void clearScreen() {
    csi("2J");
  }

  public void setCursorPosition(int x, int y) {
    csi((y + 1) + ";" + (x + 1) + "H");
  }

  public void setCursorVisible(boolean visible) {
    csi(visible ? "?25h" : "?25l");
  }

  public void putString(String string) {
    print(string);
  }

  public void resetColorAndSGR() {
    sgr("0");
  }

  public void setColor(int color) {
    sgr(String.format("%d;3%d;%d%d", color >> 3 & 1, color & 7, (color & 0x80) == 0 ? 4 : 10, color >> 4 & 7));
  }

  public Dimension getTerminalSize() {
    setCursorPosition(900, 900);
    csi("6n");
    return new Dimension(size);
  }

  public void flush() {
    synchronized (out) {
      out.flush();
    }
  }

  public void close() {
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

  /**
   * It is expected that pollInput is called from one thread.
   */
  public String pollInput() throws IOException {
    int available = System.in.available();
    if (available == 0) return null;
    char c = (char) System.in.read();
    switch (c) {
      case '\r':
      case '\n':
        return "Enter";
      case '\u0005': return "Ctrl+e";
      case '\u001B':
        String csi = readCsi();
        Pattern CPR = Pattern.compile("\\[(\\d+);(\\d+)R");
        Matcher cpr = CPR.matcher(csi);
        if (cpr.matches()) {
          size = new Dimension(Integer.parseInt(cpr.group(2)), Integer.parseInt(cpr.group(1)));
          return pollInput();
        }
        switch (csi) {
          case "[A": return "Up";
          case "[B": return "Down";
          case "[C": return "Right";
          case "[D": return "Left";
          case "[15~": return "F5";
          case "[17~": return "F6";
          case "[18~": return "F7";
          case "[19~": return "F8";
          case "[20~": return "F9";
          case "[21~": return "F10";
          default:
            return "^[" + csi;
        }
      default:
        return String.format("\\u%04X", (int) c);
    }
  }
}
