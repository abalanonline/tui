package ab.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.function.BiConsumer;

public class Main {

  static class Splash implements Runnable, Tui.KeyListener<Splash> {
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
          tui.setString(x + ix, y + iy, s.substring(is + (ix == 0 ? 0 : ix == w1 ? 2 : 1)).substring(0, 1), attr);
        }
      }
    }

    private void fill(int x, int y, int w, int h, String s, int attr) {
      final StringBuffer sb = new StringBuffer();
      for (int iy = 0; iy < h; iy++) {
        while (sb.length() < w) sb.append(s);
        tui.setString(x, y + iy, sb.substring(0, w), attr);
        sb.delete(0, 1);
      }
    }

    @Override
    public void keyTyped(Tui.KeyEvent<Splash> e) {
      int[] rainbow = {1, 3, 2, 6, 4, 5};
      debug.add(e.ev);
      for (int i = debug.size() - 1, y = 21; i >= 0 && y > 1; i--, y--) {
        int bg = rainbow[i % rainbow.length] << 4;
        drawBox(4, y, 72, 1, "   ", bg);
        tui.setString(8, y, debug.get(i), bg + 15);
      }
      if ("Enter".equals(e.ev)) stopStatus = true;
      if ("Ctrl+e".equals(e.ev)) throw new OutOfMemoryError();
    }

    @Override
    public void run() {
      drawBox(0, 0, 80, 24, "┌─┐│ │└─┘", 0x1B);
      fill(4, 2, 72, 20, "the quick brown fox jumps over the lazy dog ", 0x1B);
      fill(17, 9, 50, 8, "jumps over the lazy dog the quick brown fox ", 0x03);
      drawBox(15, 8, 50, 8, "         ", 0x70);
      drawBox(17, 9, 46, 6, "╔═╗║ ║╚═╝", 0x70);
      tui.setString(27, 9, " Splash screen dialog box ", 0x70);
      tui.setString(19, 11, "Press enter to continue, Ctrl+E to crash.", 0x70);
      tui.setString(37, 13, "  OK  ", 0x30);
      final long stopTime = System.nanoTime() + 30_000_000_000L;
      final SynchronousQueue<Tui.KeyEvent<Void>> queue = new SynchronousQueue<>();
      tui.addKeyListener(this);
      tui.idle(() -> System.nanoTime() < stopTime && !stopStatus);
      tui.removeKeyListener(this);
    }
  }

  static class Paint implements Runnable, Tui.KeyListener<Paint> {
    private final Tui tui;
    private boolean exit;
    private int x = 44;
    private int y = 13;
    private boolean ink;

    public Paint(Tui tui) {
      this.tui = tui;
    }

    private void move(int x, int y) {
      tui.setString(this.x, this.y, "  ", ink ? 0x07 : 0x70);
      this.x += x * 2;
      this.y += y;
      this.x = Math.min(Math.max(0, this.x), 78);
      this.y = Math.min(Math.max(0, this.y), 23);
      refresh();
    }

    private void refresh() {
      tui.setString(this.x, this.y, "[]", ink ? 0x07 : 0x70);
    }

    @Override
    public void keyTyped(Tui.KeyEvent<Paint> e) {
      switch (e.ev) {
        case "Left":
          move(-1, 0);
          break;
        case "Down":
          move(0, 1);
          break;
        case "Up":
          move(0, -1);
          break;
        case "Right":
          move(1, 0);
          break;
        case "t":
          ink = !ink;
          refresh();
          break;
        case "Alt+i":
          ink = true;
          refresh();
          break;
        case "Alt+p":
          ink = false;
          refresh();
          break;
        case "Enter":
          exit = true;
          break;
      }
    }

    @Override
    public void run() {
      refresh();
      tui.addKeyListener(this);
      tui.idle(() -> !exit);
      tui.removeKeyListener(this);
    }

    public static enum Action implements BiConsumer<String, Paint> {
      LEFT((s, p) -> {}),
      RIGHT((s, p) -> {}),
      ENTER((s, p) -> {});

      private final BiConsumer<String, Paint> consumer;

      Action(BiConsumer<String, Paint> consumer) {
        this.consumer = consumer;
      }

      @Override
      public void accept(String s, Paint paint) {
        consumer.accept(s, paint);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    try (final Tui tui = new TuiConsole()) {
      new Splash(tui).run();
      new Paint(tui).run();
    }
  }

}
