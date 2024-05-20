package ab.tui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Main {

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
    public void accept(String s) {
      int[] rainbow = {1, 3, 2, 6, 4, 5};
      debug.add(s);
      for (int i = debug.size() - 1, y = 21; i >= 0 && y > 1; i--, y--) {
        int bg = rainbow[i % rainbow.length] << 4;
        drawBox(4, y, 72, 1, "   ", bg);
        tui.setString(8, y, debug.get(i), bg + 15);
      }
      if ("Enter".equals(s)) stopStatus = true;
      if ("Ctrl+e".equals(s)) throw new OutOfMemoryError();
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
      tui.addKeyListener(this);
      tui.idle(() -> System.nanoTime() < stopTime && !stopStatus);
      tui.removeKeyListener(this);
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

    public void keyTyped(String event) {
      String keyBindings = Action.KEY_BINDINGS;
      Class<?> enumClass = Action.class;
      Map<String, EnumKeyListener.Enum<Object>> enumMap = new HashMap<>();
      for (Object enumConstant : enumClass.getEnumConstants()) {
        String name = ((Enum<?>) enumConstant).name();
        EnumKeyListener.Enum<Object> tuiEnum = (EnumKeyListener.Enum) enumConstant;
        enumMap.put(name.toUpperCase(), tuiEnum);
        enumMap.put(name, tuiEnum);
      }

      Enum<?>[] enumConstants = (Enum<?>[]) enumClass.getEnumConstants();
      EnumKeyListener.Enum<?>[] enumConstants2 = (EnumKeyListener.Enum<?>[]) enumClass.getEnumConstants();
      Set<String> enumNames = Arrays.stream(enumConstants)
          .map(Enum::name).collect(Collectors.toSet());

      HashMap<String, String> bindings = new HashMap<>();
      for (String binding : keyBindings.split("\n")) {
        String[] s = binding.split(":", 2);
        if (s.length < 2) continue;
        String v = s[1].trim();
        s = s[0].split(",");
        for (String k : s) {
          bindings.put(k.trim(), v);
        }
      }

      String binding = bindings.get(event);
      if (binding != null) {
        Optional.ofNullable(enumMap.get(binding))
            .orElseThrow(() -> new IllegalStateException("method not found: " + binding))
            .accept(event, this);
      } else {
        Optional.ofNullable(enumMap.get(event.replace('+', '_').toUpperCase()))
            .or(() -> Optional.ofNullable(enumMap.get("DEFAULT")))
            .ifPresent(tuiEnum -> tuiEnum.accept(event, this));
      }

    }

    @Override
    public void run() {
      refresh();
      Consumer<String> keyListener = EnumKeyListener.createEnumKeyListener(Action.KEY_BINDINGS, this, Action.class);
      tui.addKeyListener(keyListener);
      tui.idle(() -> !exit);
      tui.removeKeyListener(keyListener);
    }

    public static enum Action implements EnumKeyListener.Enum<Paint> {
      LEFT((s, p) -> { p.move(-1, 0); }),
      DOWN((s, p) -> { p.move(0, 1); }),
      UP((s, p) -> { p.move(0, -1); }),
      RIGHT((s, p) -> { p.move(1, 0); }),
      INK((s, p) -> { p.ink = true; p.refresh(); }),
      PAPER((s, p) -> { p.ink = false; p.refresh(); }),
      TOGGLE((s, p) -> { p.ink = !p.ink; p.refresh(); }),
      ENTER((s, p) -> { p.exit = true; });

      public static final String KEY_BINDINGS =
          "F1, Alt+i: INK\n" +
          "F2, t: TOGGLE\n" +
          "F3, Alt+p: PAPER\n" +
          "Ctrl+e: haltCatchFire";

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
