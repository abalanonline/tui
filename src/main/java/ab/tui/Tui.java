package ab.tui;

import java.util.function.Consumer;

public interface Tui extends AutoCloseable {

  /**
   * @param attr in CGA order - flash, br, bg, bb, bright, fr, fg, fb
   */
  void print(int x, int y, String s, int attr);

  // also paint, repaint, refresh
  void update();

  /**
   * @param keyListener null to unset
   */
  void setKeyListener(Consumer<String> keyListener);

  @Override
  void close();
}
