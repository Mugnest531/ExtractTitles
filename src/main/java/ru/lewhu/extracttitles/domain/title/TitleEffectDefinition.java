package ru.lewhu.extracttitles.domain.title;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TitleEffectDefinition {
    private final String type;
    private final Map<String, Object> options;

    public TitleEffectDefinition(String type, Map<String, Object> options) {
        this.type = type == null ? "" : type.toLowerCase();
        this.options = options == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(options));
    }

    public String type() {
        return type;
    }

    public Map<String, Object> options() {
        return options;
    }
}
