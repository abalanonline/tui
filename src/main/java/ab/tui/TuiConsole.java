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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TuiConsole implements Tui {

  private Consumer<String> keyListener = null;
  private boolean stop;
  private final Thread thread;

  private static final int UPDATED_BIT = 0x100;
  private final Object colorLock = new Object();
  private int[][] color = new int[0][];
  private char[][] c = new char[0][];
  private Dimension size;
  private boolean sizeUpdated;
  private final PrintStream out;

  public TuiConsole() {
    out = System.out;
    Map<String, String> map = Arrays.stream(systemExec("stty -a").split(";\\s+"))
        .collect(Collectors.toMap(a -> a.split("\\s", 2)[0], a -> a.split("\\s", 2)[1]));
    size = new Dimension(Integer.parseInt(map.get("columns")), Integer.parseInt(map.get("rows")));
    systemExec("stty raw -echo");

    csi("?1049h"); // private mode
    flush();
    csi("2J"); // clear screen
    csi("?25l"); // cursor visible off

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
    sgrResetColor();
    csi("?25h"); // cursor visible on
    csi("?1049l"); // private mode
    flush();
    systemExec("stty -raw echo");
  }

  @Override
  public Dimension getSize() {
    return new Dimension(size);
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
      csiCursorPosition(900, 900);
      csiReportCursorPosition(); // update the size on every update
      sgrResetColor();
      StringBuilder stringBuilder = new StringBuilder();
      Dimension size = getSize();
      int height = Math.min(size.height, color.length);
      for (int y = 0; y < height; y++) {
        char[] cy = this.c[y];
        int[] cly = this.color[y];
        int co = -1;
        int width = Math.min(size.width, cly.length);
        for (int x = 0; x < width; x++) {
          boolean u = cly[x] < UPDATED_BIT || sizeUpdated;
          int cn = u ? cly[x] : -1;
          if (cn != co) {
            if (stringBuilder.length() > 0) {
              print(stringBuilder.toString());
              stringBuilder.setLength(0);
            }
            if (co < 0) csiCursorPosition(x, y);
            if (cn >= 0) sgrColor(cly[x]);
            co = cn;
          }
          if (cn >= 0) stringBuilder.append(this.c[y][x]);
          cly[x] |= UPDATED_BIT;
        }
        if (stringBuilder.length() > 0) {
          print(stringBuilder.toString());
          stringBuilder.setLength(0);
        }
      }
      sizeUpdated = false;
      csiCursorPosition(0, 0);
      flush();
    }
  }

  private void run() {
    try {
      while (!stop) {
        String key = pollInput();
        if ("F8".equals(key)) {
          csi("2J"); // FIXME: 2025-01-09 remove this test clear screen
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

  public void flush() {
    synchronized (out) {
      out.flush();
    }
  }

  public void sgrColor(int color) {
    sgr(String.format("%d;3%d;%d%d", color >> 3 & 1, color & 7, (color & 0x80) == 0 ? 4 : 10, color >> 4 & 7));
  }

  public void sgrResetColor() {
    sgr("0");
  }

  public void csiCursorPosition(int x, int y) {
    csi(String.format("%d;%dH", y + 1, x + 1));
  }

  public void csiReportCursorPosition() {
    csi("6n");
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
          Dimension size = new Dimension(Integer.parseInt(cpr.group(2)), Integer.parseInt(cpr.group(1)));
          if (!size.equals(this.size)) sizeUpdated = true;
          this.size = size;
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
