package ru.lewhu.extracttitles.service.effect;

import ru.lewhu.extracttitles.domain.title.TitleDefinition;

import java.util.Map;

public record AppliedEffect(String titleId, TitleDefinition title, TitleEffect effect, Map<String, Object> options) {
}
