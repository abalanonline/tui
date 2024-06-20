package ab.tui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TuiUtil {

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
      if ("Clock".equals(s)) return; // midi clock
      int[] rainbow = {1, 3, 2, 6, 4, 5};
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
      drawBox(0, 0, 80, 24, "┌─┐│ │└─┘", 0x1B);
      fill(4, 2, 72, 20, "the quick brown fox jumps over the lazy dog ", 0x1B);
      fill(17, 9, 50, 8, "jumps over the lazy dog the quick brown fox ", 0x03);
      drawBox(15, 8, 50, 8, "         ", 0x70);
      drawBox(17, 9, 46, 6, "╔═╗║ ║╚═╝", 0x70);
      tui.print(27, 9, " Splash screen dialog box ", 0x70);
      tui.print(19, 11, "Press enter to continue, Ctrl+E to crash.", 0x70);
      tui.print(37, 13, "  OK  ", 0x30);
      final long stopTime = System.nanoTime() + 30_000_000_000L;
      tui.setKeyListener(this);
      while (System.nanoTime() < stopTime && !stopStatus) {
        try {
          tui.print(68, 0, " " + Instant.now().toString().substring(11, 19) + " ", 0x1B);
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
      tui.print(this.x, this.y, "  ", ink ? 0x07 : 0x70);
      this.x += x * 2;
      this.y += y;
      this.x = Math.min(Math.max(0, this.x), 78);
      this.y = Math.min(Math.max(0, this.y), 23);
      refresh();
    }

    private void refresh() {
      tui.print(this.x, this.y, "[]", ink ? 0x07 : 0x70);
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
      tui.print(0, y++, s, 0x40);
    }
    tui.update();
  }



}
