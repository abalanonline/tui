package ab.tui;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class Tui1 implements AutoCloseable, Runnable {

  private final Terminal terminal;
  private final Screen screen;
  private final Map<String, String> map;


  public Tui1(Map<String, String> map, Class<? extends Enum> enumClass) {
    try {
      terminal = new DefaultTerminalFactory()
          .setUnixTerminalCtrlCBehaviour(UnixLikeTerminal.CtrlCBehaviour.TRAP)
          .createTerminal();
      screen = new TerminalScreen(terminal);
      screen.startScreen();
      screen.setCursorPosition(null);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    this.map = map;
    final Enum<?> camelCase = Enum.valueOf(enumClass, "camelCase");
    final Enum<?> snake_case = Enum.valueOf(enumClass, "snake_case");
//    Actions.valueOf()
  }

  @Override
  public void run() {
    for (boolean exit = true; exit; exit = !exit) {
      KeyStroke key = null;
      try {
        key = terminal.pollInput();
      } catch (IOException e) {
      }
      if (key == null) {
        try {
          Thread.sleep(1);
          exit = false;
        } catch (InterruptedException ignore) {
          // exit == true
        }
        continue;
      }
      String keyNotation = "Unknown";
      final KeyType keyType = key.getKeyType();
      switch (keyType) {
        case Character:
          keyNotation = key.getCharacter().toString();
          break;
        case EOF:
        case Escape:
          continue; // exit == true
      }
      if (key.isCtrlDown()) keyNotation = "Ctrl+" + keyNotation.toUpperCase();
      final String action = map.get(keyNotation);
      if (action != null) {

      }
      System.out.println(keyNotation);
      exit = false;
    }
  }

  @Override
  public void close() throws IOException {
    screen.stopScreen();
    terminal.close();
  }
}
