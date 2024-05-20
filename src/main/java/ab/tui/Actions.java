package ab.tui;

import java.util.function.BiConsumer;

public enum Actions implements BiConsumer<String, BigApp> {
  RANDOM((s, o) -> {
    o.random();
  }),
  QUIT((s, o) -> {}),
  camelCase((s, o) -> {}),
  snake_case((s, o) -> {});

  private final BiConsumer<String, BigApp> consumer;

  Actions(BiConsumer<String, BigApp> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void accept(String s, BigApp o) {
    this.consumer.accept(s, o);
  }

}
