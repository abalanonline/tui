package ab.tui;

import java.util.function.BooleanSupplier;

public interface Tui extends AutoCloseable {

  /**
   * @param attr is in CGA order - flash, rb, gb, bb, bright, rf, gf, bf
   */
  void setString(int x, int y, String s, int attr);

  void idle(BooleanSupplier run);

  void addKeyListener(KeyListener<?> keyListener);

  void removeKeyListener(KeyListener<?> keyListener);

  interface KeyListener<T extends KeyListener<T>> {
    void keyTyped(KeyEvent<T> e);
  }

  public static class KeyEvent<T> {
    public final T t;
    public final String ev;
    public KeyEvent(T t, String ev) {
      this.t = t;
      this.ev = ev;
    }
  }

}
