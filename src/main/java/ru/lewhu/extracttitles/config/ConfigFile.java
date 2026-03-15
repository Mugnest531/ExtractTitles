package ru.lewhu.extracttitles.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public final class ConfigFile {
    private static final DateTimeFormatter BROKEN_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JavaPlugin plugin;
    private final String name;
    private File file;
    private FileConfiguration configuration;

    public ConfigFile(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public void load() {
        ensureDataFolder();
        file = new File(plugin.getDataFolder(), name);

        if (!file.exists()) {
            plugin.saveResource(name, false);
        }

        configuration = loadSafely(file);
    }

    public void reload() {
        if (file == null) {
            load();
            return;
        }
        configuration = loadSafely(file);
    }

    public FileConfiguration config() {
        return configuration;
    }

    public void save() {
        if (configuration == null || file == null) {
            return;
        }
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config " + name, e);
        }
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder.");
        }
    }

    private FileConfiguration loadSafely(File source) {
        try {
            String text = readTextWithEncodingFallback(source);
            String sanitized = sanitizeControls(text);

            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(sanitized);
            return yaml;
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Cannot load " + source.getPath() + ". File is broken, restoring default copy.", ex);
            backupBrokenFile(source);
            if (source.exists() && !source.delete()) {
                plugin.getLogger().warning("Failed to delete broken file " + source.getPath());
            }
            plugin.saveResource(name, false);

            YamlConfiguration restored = new YamlConfiguration();
            try {
                restored.load(source);
            } catch (IOException | InvalidConfigurationException restoreEx) {
                plugin.getLogger().log(Level.SEVERE,
                        "Cannot load restored file " + source.getPath(), restoreEx);
            }
            return restored;
        }
    }

    private String readTextWithEncodingFallback(File source) throws IOException {
        byte[] data = Files.readAllBytes(source.toPath());

        try {
            return decodeStrict(data, StandardCharsets.UTF_8);
        } catch (CharacterCodingException ignored) {
            Charset cp1251 = Charset.forName("windows-1251");
            return new String(data, cp1251);
        }
    }

    private String decodeStrict(byte[] data, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer out = decoder.decode(ByteBuffer.wrap(data));
        return out.toString();
    }

    private String sanitizeControls(String in) {
        StringBuilder out = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                out.append(c);
                continue;
            }
            if ((c >= 0x00 && c <= 0x1F) || (c >= 0x7F && c <= 0x9F)) {
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private void backupBrokenFile(File source) {
        if (!source.exists()) {
            return;
        }
        String suffix = LocalDateTime.now().format(BROKEN_SUFFIX);
        File backup = new File(source.getParentFile(), source.getName() + ".broken-" + suffix);
        try {
            Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().warning("Backup created: " + backup.getName());
        } catch (IOException copyEx) {
            plugin.getLogger().log(Level.SEVERE, "Failed to backup broken file " + source.getPath(), copyEx);
        }
    }
}