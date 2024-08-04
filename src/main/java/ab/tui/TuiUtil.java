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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TuiUtil {

  public static final int BLACK_RED = 0x10;
  public static final int WHITE_BLACK = 0x07;
  public static final int BLACK_WHITE = 0x70;
  public static final int BRIGHT_CYAN_BLUE = 0x4E;
  public static final int BLACK_CYAN = 0x60;
  public static final int CYAN_BLACK = 0x06;

  static class Splash implements Runnable, Consumer<String> {
    private final Tui tui;
    private boolean stopStatus;
    private final List<String> debug = new ArrayList<>();

    public Splash(Tui tui) {
      this.tui = tui;
    }

    private void drawBox(int x, int y, int w, int h, String s, int attr) {
      final int h1 = h - 1;
      final int w1 = w - 1;
      for (int iy = 0; iy <= h1; iy++) {
        int is = iy == 0 ? 0 : iy == h1 ? 6 : 3;
        for (int ix = 0; ix <= w1; ix++) {
          tui.print(x + ix, y + iy, s.substring(is + (ix == 0 ? 0 : ix == w1 ? 2 : 1)).substring(0, 1), attr);
        }
      }
    }

    private void fill(int x, int y, int w, int h, String s, int attr) {
      final StringBuffer sb = new StringBuffer();
      for (int iy = 0; iy < h; iy++) {
        while (sb.length() < w) sb.append(s);
        tui.print(x, y + iy, sb.substring(0, w), attr);
        sb.delete(0, 1);
      }
    }

    @Override
    public void accept(String s) {
      if ("Close".equals(s)) {
        stopStatus = true;
        return;
      }
      if ("Clock".equals(s)) return; // midi clock
      int[] rainbow = {4, 6, 2, 3, 1, 5}; // bcgyrm
      debug.add(s);
      for (int i = debug.size() - 1, y = 21; i >= 0 && y > 1; i--, y--) {
        int bg = rainbow[i % rainbow.length] << 4;
        drawBox(4, y, 72, 1, "   ", bg);
        tui.print(8, y, debug.get(i), bg + 15);
      }
      if ("Enter".equals(s)) stopStatus = true;
      if ("Ctrl+e".equals(s)) throw new ArithmeticException();
    }

    @Override
    public void run() {
      drawBox(0, 0, 80, 24, "┌─┐│ │└─┘", BRIGHT_CYAN_BLUE);
      fill(4, 2, 72, 20, "the quick brown fox jumps over the lazy dog ", BRIGHT_CYAN_BLUE);
      fill(17, 9, 50, 8, "jumps over the lazy dog the quick brown fox ", CYAN_BLACK);
      drawBox(15, 8, 50, 8, "         ", BLACK_WHITE);
      drawBox(17, 9, 46, 6, "╔═╗║ ║╚═╝", BLACK_WHITE);
      tui.print(27, 9, " Splash screen dialog box ", BLACK_WHITE);
      tui.print(19, 11, "Press enter to continue, Ctrl+E to crash.", BLACK_WHITE);
      tui.print(37, 13, "  OK  ", BLACK_CYAN);
      final long stopTime = System.nanoTime() + 30_000_000_000L;
      tui.setKeyListener(this);
      while (System.nanoTime() < stopTime && !stopStatus) {
        try {
          tui.print(68, 0, " " + Instant.now().toString().substring(11, 19) + " ", BRIGHT_CYAN_BLUE);
          tui.update();
          Thread.sleep(10);
        } catch (InterruptedException ignore) {
        }
      }
      tui.setKeyListener(null);
    }
  }

  static class Paint implements Runnable {
    private final Tui tui;
    private boolean exit;
    private int x = 44;
    private int y = 13;
    private boolean ink;

    public Paint(Tui tui) {
      this.tui = tui;
    }

    private void move(int x, int y) {
      tui.print(this.x, this.y, "  ", ink ? WHITE_BLACK : BLACK_WHITE);
      this.x += x * 2;
      this.y += y;
      this.x = Math.min(Math.max(0, this.x), 78);
      this.y = Math.min(Math.max(0, this.y), 23);
      refresh();
    }

    private void refresh() {
      tui.print(this.x, this.y, "[]", ink ? WHITE_BLACK : BLACK_WHITE);
      tui.update();
    }

    @Override
    public void run() {
      refresh();
      Consumer<String> keyListener = new EnumListener<>(this, Action.class, Action.KEY_BINDINGS);
      tui.setKeyListener(keyListener);
      while (!exit) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignore) {
        }
      }
      tui.setKeyListener(null);
    }

    public static enum Action implements EnumListener.Enum<Paint> {
      LEFT((o, s) -> { o.move(-1, 0); }),
      DOWN((o, s) -> { o.move(0, 1); }),
      UP((o, s) -> { o.move(0, -1); }),
      RIGHT((o, s) -> { o.move(1, 0); }),
      INK((o, s) -> { o.ink = true; o.refresh(); }),
      PAPER((o, s) -> { o.ink = false; o.refresh(); }),
      TOGGLE((o, s) -> { o.ink = !o.ink; o.refresh(); }),
      CLOSE((o, s) -> { o.exit = true; }),
      ENTER((o, s) -> { o.exit = true; });

      public static final String KEY_BINDINGS =
          "F1, Alt+i: INK\n" +
          "F2, t: TOGGLE\n" +
          "F3, Alt+p: PAPER\n" +
          "Ctrl+e: haltCatchFire";

      private final BiConsumer<Paint, String> consumer;

      Action(BiConsumer<Paint, String> consumer) {
        this.consumer = consumer;
      }

      @Override
      public void accept(Paint o, String s) {
        consumer.accept(o, s);
      }

    }
  }

  public static void testTui(Tui tui) {
    new Splash(tui).run();
    new Paint(tui).run();
  }

  public static void logError(Tui tui, String message, Exception exception) {
    java.util.logging.Logger.getAnonymousLogger().log(Level.SEVERE, message, exception);
    StringWriter stringWriter = new StringWriter();
    final StackTraceElement stackTraceElement = exception.getStackTrace()[0];
    stringWriter.append(Instant.now().toString()).append(' ').append(Tui.class.getName())
        .append(" logError\nSEVERE: ").append(message).append('\n');
    exception.printStackTrace(new PrintWriter(stringWriter));
    int y = 0;
    for (String s : stringWriter.toString().replace("\t", "    ").split("\r?\n")) {
      tui.print(0, y++, s, BLACK_RED);
    }
    tui.update();
  }



  public static class EnumListener<T> implements Consumer<String> {

    public interface Enum<T> extends BiConsumer<T, String> {
    }

    private final T listeningObject;
    private final Map<String, Enum<T>> enumMap;
    private final Map<String, String> bindings;

    public EnumListener(T object, Class<? extends Enum<T>> enumClass, String keyBindings) {
      this.listeningObject = object;

      enumMap = new HashMap<>();
      for (Enum<T> enumConstant : enumClass.getEnumConstants()) {
        String name = ((java.lang.Enum<?>) enumConstant).name();
        enumMap.put(name.toUpperCase(), enumConstant);
        enumMap.put(name, enumConstant);
      }

      bindings = new HashMap<>();
      for (String binding : keyBindings.split("\n")) {
        String[] s = binding.split(":", 2);
        if (s.length < 2) continue;
        String v = s[1].trim();
        s = s[0].split(",");
        for (String k : s) {
          bindings.put(k.trim(), v);
        }
      }
    }

    @Override
    public void accept(String s) {
      String binding = bindings.get(s);
      if (binding != null) {
        Optional.ofNullable(enumMap.get(binding))
            .orElseThrow(() -> new IllegalStateException("method not found: " + binding))
            .accept(listeningObject, s);
      } else {
        Optional.ofNullable(enumMap.get(s.replace('+', '_').toUpperCase()))
            .or(() -> Optional.ofNullable(enumMap.get("DEFAULT")))
            .ifPresent(tuiEnum -> tuiEnum.accept(listeningObject, s));
      }
    }

  }
}
