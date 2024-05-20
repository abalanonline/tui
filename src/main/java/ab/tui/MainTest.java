package ab.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainTest {

  public static void test1() throws IOException {
    final Terminal terminal = new DefaultTerminalFactory()
        //.setInitialTerminalSize(new TerminalSize(36, 3))
        //.setTerminalEmulatorFontConfiguration(SwingTerminalFontConfiguration.getDefaultOfSize(32))
        .createTerminal();
    terminal.enterPrivateMode();
    terminal.putCharacter('\n');
    terminal.flush();
    System.out.println("tui");

    for (boolean exit = true; exit; exit = !exit) {
      terminal.setCursorPosition(0, 0);
      for (char c : Instant.now().toString().toCharArray()) {
        terminal.putCharacter(c);
      }
      terminal.flush();
      KeyStroke key = terminal.pollInput();
      if (key == null) exit = false;
    }

    KeyStroke key = terminal.pollInput();
    if (key != null) {
      switch (key.getKeyType()) {
        case Character:
          int b = 0;
          switch (key.getCharacter()) {
            case '1':
              b = 0x0001;
              break;
            case '2':
              b = 0x0002;
              break;
          }
      }
    }
    terminal.putCharacter('\n');
    terminal.setCursorVisible(true);
    terminal.flush();
    terminal.close();
  }

  public static void test2() throws IOException {
    // Setup terminal and screen layers
    Terminal terminal = new DefaultTerminalFactory().createTerminal();
    Screen screen = new TerminalScreen(terminal);
    screen.startScreen();

    // Create panel to hold components
    Panel panel = new Panel();
    panel.setLayoutManager(new GridLayout(2));

    panel.addComponent(new Label("Forename"));
    panel.addComponent(new TextBox());

    panel.addComponent(new Label("Surname"));
    panel.addComponent(new TextBox());

    panel.addComponent(new EmptySpace(new TerminalSize(0,0))); // Empty space underneath labels
    panel.addComponent(new Button("Submit"));

    // Create window to hold the panel
    BasicWindow window = new BasicWindow();
//    window.setComponent(panel);

    // Create gui and start gui
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
    gui.addWindowAndWait(window);

  }

  public static void test3() throws IOException, FontFormatException {
    final Font font = Font.createFont(Font.TRUETYPE_FONT, Files.newInputStream(Paths.get("assets/font.ttf")));
    Terminal terminal = new DefaultTerminalFactory()
        .setTerminalEmulatorFontConfiguration(AWTTerminalFontConfiguration.newInstance(font))
        .createTerminal();
    Screen screen = new TerminalScreen(terminal);
    screen.startScreen();
    screen.setCursorPosition(null);
    screen.setCharacter(2, 2, TextCharacter.fromCharacter('c')[0]);
    for (int i = 0; i < 5; i++) {
      screen.setCharacter(2 + i, 3, TextCharacter.fromCharacter('*')[0]);
      try {
        screen.refresh();
        Thread.sleep(1_000);
      } catch (InterruptedException e) {
      }
    }
    screen.stopScreen();
    terminal.close();
  }

  public static void test4() throws Exception {
    final Map<String, String> map = new LinkedHashMap<>();
    map.put("Ctrl+C", "quit");
    map.put("Ctrl+R", "random");
    try (Tui1 tui = new Tui1(map, Actions.class)) {
      tui.run();
    }
  }

  public static void main(String[] args) throws Exception {
    test4();
  }

}
