package ab.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TextColor.ANSI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class TuiConsole implements Tui, Runnable {

  public static final TextColor[] TEXT_COLORS = {
      ANSI.BLACK, ANSI.BLUE, ANSI.GREEN, ANSI.CYAN,
      ANSI.RED, ANSI.MAGENTA, ANSI.YELLOW, ANSI.WHITE,
      ANSI.BLACK_BRIGHT, ANSI.BLUE_BRIGHT, ANSI.GREEN_BRIGHT, ANSI.CYAN_BRIGHT,
      ANSI.RED_BRIGHT, ANSI.MAGENTA_BRIGHT, ANSI.YELLOW_BRIGHT, ANSI.WHITE_BRIGHT
  };
  private final Terminal terminal;
  private final Screen screen;
  private Consumer<String> keyListener = null;
  private boolean stop;
  private final Thread thread;
  private final Dimension padding;

  public TuiConsole() {
    this(builder());
  }

  private TuiConsole(Builder builder) {
    padding = builder.padding == null ? new Dimension() : builder.padding;
    DefaultTerminalFactory terminalFactory = builder.terminalFactory;
    try {
      terminal = terminalFactory.createTerminal();
      screen = new TerminalScreen(terminal);
      screen.startScreen();
      screen.setCursorPosition(null);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    thread = new Thread(this);
    thread.start();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory()
        .setTerminalEmulatorTitle("")
        .setUnixTerminalCtrlCBehaviour(UnixLikeTerminal.CtrlCBehaviour.TRAP);
    private Dimension padding;
    public Builder monochrome() {
      terminalFactory.setTerminalEmulatorColorConfiguration(LanternaColor.VGA_MONO);
      return this;
    }
    public Builder withColor(int... color) {
      int[] c = Arrays.copyOf(color, 19);
      if (color.length <= 16) {
        c[16] = c[0];
        c[17] = c[7];
        c[18] = c[15];
      }
      terminalFactory.setTerminalEmulatorColorConfiguration(LanternaColor.fromColor(c));
      return this;
    }
    public Builder withSize(int w, int h) {
      terminalFactory.setInitialTerminalSize(new TerminalSize(w, h));
      return this;
    }
    public Builder withPadding(int w, int h) {
      padding = new Dimension(w, h);
      return this;
    }
    public Builder withFont(InputStream fontStream, double fontSize) {
      try {
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont((float) fontSize);
        terminalFactory.setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.newInstance(font));
      } catch (HeadlessException | FontFormatException | IOException e) {
        throw new IllegalStateException();
      }
      return this;
    }
    public Builder withFontSize(int fontSize) {
      try {
        terminalFactory.setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.getDefaultOfSize(fontSize));
      } catch (HeadlessException ignore) {
      }
      return this;
    }
    public TuiConsole build() {
      return new TuiConsole(this);
    }
  }

  @Override
  public void close() {
    stop = true;
    try {
      if (!thread.equals(Thread.currentThread())) thread.join();
      screen.stopScreen();
      terminal.close();
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void print(int x, int y, String s, int attr) {
    x += padding.width;
    y += padding.height;
    if (stop) throw new IllegalStateException("closed");
    final TextCharacter[] cs = TextCharacter.fromString(s, TEXT_COLORS[attr & 15], TEXT_COLORS[attr >> 4 & 7]);
    for (TextCharacter c : cs) screen.setCharacter(x++, y, c);
  }

  @Override
  public void update() {
    try {
      screen.refresh();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String getCharacterNotation(KeyStroke key) {
    char c = key.getCharacter();
    switch (c) {
      case '\u001A': c = key.isShiftDown() ? 'Z' : 'z'; break;
      case '\u001C': c = key.isShiftDown() ? '|' : '\\'; break;
      case '\u001D': c = key.isShiftDown() ? '}' : ']'; break;
      default:
        return "?";
    }
    StringBuilder keyNotation = new StringBuilder();
    if (key.isCtrlDown()) keyNotation.append("Ctrl+");
    if (key.isAltDown()) keyNotation.append("Alt+");
    keyNotation.append(c);
    return keyNotation.toString();
  }

  private String getKeyNotation(KeyStroke key) {
    StringBuilder keyNotation = new StringBuilder();
    if (key.isCtrlDown()) keyNotation.append("Ctrl+");
    if (key.isAltDown()) keyNotation.append("Alt+");
    if (key.isShiftDown() && key.getKeyType() != KeyType.Character) keyNotation.append("Shift+");
    switch (key.getKeyType()) {
      case Character:
        char character = key.getCharacter();
        if (character < 0x20) return getCharacterNotation(key);
        keyNotation.append(character);
        break;
      case EOF:
        throw new IllegalStateException("EOF");
      case ArrowLeft:
      case ArrowDown:
      case ArrowUp:
      case ArrowRight:
        keyNotation.append(key.getKeyType().toString().substring(5));
        break;
      default:
        keyNotation.append(key.getKeyType());
    }
    return keyNotation.toString();
  }

  @Override
  public void run() {
    try {
      while (!stop) {
        KeyStroke key = terminal.pollInput();
        if (key == null) {
          Thread.sleep(10);
          continue;
        }
        Consumer<String> keyListener = this.keyListener;
        if (keyListener != null) try {
          keyListener.accept(getKeyNotation(key));
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

}
