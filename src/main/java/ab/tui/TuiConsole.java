package ab.tui;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TextColor.ANSI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class TuiConsole implements Tui {

  public static final TextColor[] TEXT_COLORS = {
      ANSI.BLACK, ANSI.BLUE, ANSI.GREEN, ANSI.CYAN,
      ANSI.RED, ANSI.MAGENTA, ANSI.YELLOW, ANSI.WHITE,
      ANSI.BLACK_BRIGHT, ANSI.BLUE_BRIGHT, ANSI.GREEN_BRIGHT, ANSI.CYAN_BRIGHT,
      ANSI.RED_BRIGHT, ANSI.MAGENTA_BRIGHT, ANSI.YELLOW_BRIGHT, ANSI.WHITE_BRIGHT
  };
  private final Terminal terminal;
  private final Screen screen;
  private final Deque<Consumer<String>> keyListeners = new ConcurrentLinkedDeque<>();

  public TuiConsole() {
    DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory()
        .setTerminalEmulatorTitle("")
        .setUnixTerminalCtrlCBehaviour(UnixLikeTerminal.CtrlCBehaviour.TRAP)
        .setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.getDefaultOfSize(27));
    try {
      final Font font = Font.createFont(Font.TRUETYPE_FONT, Files.newInputStream(Paths.get("assets/font.ttf")))
          .deriveFont(31.9F);
      terminalFactory.setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.newInstance(font));
    } catch (IOException | FontFormatException ignore) {
    }

    try {
      terminal = terminalFactory.createTerminal();
      screen = new TerminalScreen(terminal);
      screen.startScreen();
      screen.setCursorPosition(null);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() {
    try {
      screen.stopScreen();
      terminal.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void setString(int x, int y, String s, int attr) {
    final TextCharacter[] cs = TextCharacter.fromString(s, TEXT_COLORS[attr & 15], TEXT_COLORS[attr >> 4 & 7]);
    for (TextCharacter c : cs) screen.setCharacter(x++, y, c);
  }

  private void screenRefresh() {
    try {
      screen.refresh();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private KeyStroke terminalPollInput() {
    try {
      return terminal.pollInput();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void threadSleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ignore) {
    }
  }

  @Override
  public void idle(BooleanSupplier run) {
    do {
      screenRefresh();
      KeyStroke key = terminalPollInput();
      if (key == null) {
        threadSleep(10);
        continue;
      }
      String keyNotation;
      switch (key.getKeyType()) {
        case Character:
          keyNotation = key.getCharacter().toString();
          break;
        case EOF:
          throw new IllegalStateException("EOF");
        case ArrowLeft:
        case ArrowDown:
        case ArrowUp:
        case ArrowRight:
          keyNotation = key.getKeyType().toString().substring(5);
          break;
        default:
          keyNotation = key.getKeyType().toString();
      }
      if (key.isShiftDown()) keyNotation = "Shift+" + keyNotation;
      if (key.isAltDown()) keyNotation = "Alt+" + keyNotation;
      if (key.isCtrlDown()) keyNotation = "Ctrl+" + keyNotation;
      String finalKeyNotation = keyNotation;
      keyListeners.forEach(keyListener -> keyListener.accept(finalKeyNotation));
    } while (run.getAsBoolean());
  }

  @Override
  public void addKeyListener(Consumer<String> keyListener) {
    keyListeners.addFirst(keyListener);
  }

  @Override
  public void removeKeyListener(Consumer<String> keyListener) {
    keyListeners.remove(keyListener);
  }

}
