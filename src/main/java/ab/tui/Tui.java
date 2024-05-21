package ab.tui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public interface Tui extends AutoCloseable {

  /**
   * @param attr is in CGA order - flash, rb, gb, bb, bright, rf, gf, bf
   */
  void setString(int x, int y, String s, int attr);

  void idle(BooleanSupplier run);

  void addKeyListener(Consumer<String> keyListener);

  void removeKeyListener(Consumer<String> keyListener);

  @Override
  void close();
}
