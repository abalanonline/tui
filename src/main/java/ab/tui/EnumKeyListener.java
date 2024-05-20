package ab.tui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EnumKeyListener implements Consumer<String> {

  interface Enum<T> extends BiConsumer<String, T> {
  }

  private final Object listeningObject;
  private final Map<String, EnumKeyListener.Enum<Object>> enumMap;
  private final Map<String, String> bindings;

  private EnumKeyListener(Object o, Map<String, EnumKeyListener.Enum<Object>> enumMap, Map<String, String> bindings) {
    this.listeningObject = o;
    this.enumMap = enumMap;
    this.bindings = bindings;
  }

  @Override
  public void accept(String s) {
    String binding = bindings.get(s);
    if (binding != null) {
      Optional.ofNullable(enumMap.get(binding))
          .orElseThrow(() -> new IllegalStateException("method not found: " + binding))
          .accept(s, listeningObject);
    } else {
      Optional.ofNullable(enumMap.get(s.replace('+', '_').toUpperCase()))
          .or(() -> Optional.ofNullable(enumMap.get("DEFAULT")))
          .ifPresent(tuiEnum -> tuiEnum.accept(s, listeningObject));
    }
  }

  public static <T> Consumer<String> createEnumKeyListener(String keyBindings, T t, Class<?> enumClass) {
    Map<String, EnumKeyListener.Enum<Object>> enumMap = new HashMap<>();
    for (Object enumConstant : enumClass.getEnumConstants()) {
      String name = ((java.lang.Enum<?>) enumConstant).name();
      EnumKeyListener.Enum<Object> tuiEnum = (EnumKeyListener.Enum) enumConstant;
      enumMap.put(name.toUpperCase(), tuiEnum);
      enumMap.put(name, tuiEnum);
    }

    Map<String, String> bindings = new HashMap<>();
    for (String binding : keyBindings.split("\n")) {
      String[] s = binding.split(":", 2);
      if (s.length < 2) continue;
      String v = s[1].trim();
      s = s[0].split(",");
      for (String k : s) {
        bindings.put(k.trim(), v);
      }
    }
    return new EnumKeyListener(t, enumMap, bindings);
  }

}
