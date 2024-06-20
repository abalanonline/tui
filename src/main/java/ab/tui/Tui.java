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
   * To make key events consistent among different software, this guideline should be used.
   * Modifier keys have first letter capitalized and concatenated with "+" in this order "Ctrl", "Alt", "Shift".
   * If shift alternates the character, it must be omitted from the notation. Use "Alt+?" instead of "Alt+Shift+/".
   * Examples: "Up", "Down", "Tab", "Esc", "Ctrl+Alt+Shift+F1", "Ctrl+Alt+a", "@"
   * @param keyListener null to unset
   */
  void setKeyListener(Consumer<String> keyListener);

  @Override
  void close();
}
