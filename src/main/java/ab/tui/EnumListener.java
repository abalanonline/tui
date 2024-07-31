/*
 * Copyright (C) 2024 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.tui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EnumListener<T> implements Consumer<String> {

  public interface Enum<T> extends BiConsumer<T, String> {
  }

  private final T listeningObject;
  private final Map<String, Enum<T>> enumMap;
  private final Map<String, String> bindings;

  public EnumListener(T object, Class<? extends Enum<T>> enumClass, String keyBindings) {
    this.listeningObject = object;

    enumMap = new HashMap<>();
    for (Enum<T> enumConstant : enumClass.getEnumConstants()) {
      String name = ((java.lang.Enum<?>) enumConstant).name();
      enumMap.put(name.toUpperCase(), enumConstant);
      enumMap.put(name, enumConstant);
    }

    bindings = new HashMap<>();
    for (String binding : keyBindings.split("\n")) {
      String[] s = binding.split(":", 2);
      if (s.length < 2) continue;
      String v = s[1].trim();
      s = s[0].split(",");
      for (String k : s) {
        bindings.put(k.trim(), v);
      }
    }
  }

  @Override
  public void accept(String s) {
    String binding = bindings.get(s);
    if (binding != null) {
      Optional.ofNullable(enumMap.get(binding))
          .orElseThrow(() -> new IllegalStateException("method not found: " + binding))
          .accept(listeningObject, s);
    } else {
      Optional.ofNullable(enumMap.get(s.replace('+', '_').toUpperCase()))
          .or(() -> Optional.ofNullable(enumMap.get("DEFAULT")))
          .ifPresent(tuiEnum -> tuiEnum.accept(listeningObject, s));
    }
  }

}
