package ab.tui;

public class Main {

  public static void main(String[] args) {
    try (Tui tui = new TuiConsole()) {
      TuiUtil.testTui(tui);
    }
  }

}
