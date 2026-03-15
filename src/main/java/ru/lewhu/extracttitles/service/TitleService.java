package ru.lewhu.extracttitles.service;

import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.storage.repository.TitleDefinitionRepository;

import java.util.Collection;
import java.util.Optional;

public final class TitleService {
    private final TitleDefinitionRepository repository;

    public TitleService(TitleDefinitionRepository repository) {
        this.repository = repository;
    }

    public void load() {
        repository.load();
    }

    public Collection<TitleDefinition> all() {
        return repository.all();
    }

    public Optional<TitleDefinition> find(String id) {
        return repository.byId(id);
    }

    public boolean exists(String id) {
        return repository.exists(id);
    }
}
