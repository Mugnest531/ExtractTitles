# ExtractTitles

`ExtractTitles` — плагин титулов для **Paper 1.21.x** (совместим с Purpur, архитектура подготовлена под Folia).

## Возможности
- Покупка титулов через `Vault` или `PlayerPoints`
- Постоянные и временные титулы
- Активация/деактивация титулов
- Полноценное GUI-меню (`/titles`)
- PlaceholderAPI expansion
- Эффекты активного титула:
  - `max_health`
  - `potion`
  - `permission`
  - `command` (не выполняется при join/reapply)
  - `mining_3x3`
- Хранилище: `SQLite` или `MySQL`
- Гибкая настройка сообщений и меню через конфиги

## Требования
- **Java 21**
- **Paper 1.21.x**
- Рекомендуется:
  - PlaceholderAPI
  - Vault (если используете валюту Vault)
  - PlayerPoints (если используете валюту PlayerPoints)

## Установка
1. Скопируйте `ExtractTitles.jar` в папку `plugins`.
2. (Опционально) установите `PlaceholderAPI`, `Vault`, `PlayerPoints`.
3. Запустите сервер один раз.
4. Настройте файлы в `plugins/ExtractTitles/`.
5. Выполните `/extracttitles reload` или перезапустите сервер.

## Сборка из исходников
```bash
gradle clean build
```
Готовый файл:
- `build/libs/ExtractTitles-1.0.0.jar`

## Команды
### Игрок
- `/titles` — открыть меню
- `/extracttitles menu` — открыть меню
- `/extracttitles buy <id>`
- `/extracttitles info <id>`
- `/extracttitles activate <id>`
- `/extracttitles deactivate`

### Админ
- `/extracttitles list`
- `/extracttitles reload`
- `/extracttitles give <игрок> <id>`
- `/extracttitles remove <игрок> <id>`
- `/extracttitles granttemp <игрок> <id> <1d>`
- `/extracttitles clearactive <игрок>`
- `/extracttitles debug [игрок]`

## Права
- `extracttitles.use` — базовый доступ
- `extracttitles.admin.list`
- `extracttitles.admin.edit`
- `extracttitles.admin.give`
- `extracttitles.admin.remove`
- `extracttitles.admin.granttemp`
- `extracttitles.admin.clearactive`
- `extracttitles.admin.debug`
- `extracttitles.admin.buybypass`

## PlaceholderAPI
- `%extracttitles_active_title%`
- `%extracttitles_active_title_name%`
- `%extracttitles_active_title_raw%`
- `%extracttitles_owned_count%`
- `%extracttitles_total_count%`
- `%extracttitles_active_title_or_none%`
- `%extracttitles_active_title_or_blank%`
- `%extracttitles_has_title_<id>%`
- `%extracttitles_title_expiry_<id>%`
- `%extracttitles_title_remaining_<id>%`

Примеры:
- `%extracttitles_has_title_warrior%`
- `%extracttitles_title_remaining_miner%`

## Конфиги
Папка: `plugins/ExtractTitles/`
- `config.yml` — общие настройки плагина
- `storage.yml` — SQLite/MySQL
- `messages.yml` — все сообщения
- `menus.yml` — GUI (слоты, лор, кнопки, карточки)
- `titles.yml` — все титулы и их эффекты
- `effects.yml` — шаблоны эффектов (`templates`) для переиспользования

## Важное по кодировке
- Все `.yml` редактируйте в **UTF-8 без BOM**.
- Рекомендуемые редакторы: VS Code / Notepad++ (UTF-8).

## Быстрый пример титула (`titles.yml`)
```yml
titles:
  warrior:
    display-name: "<gradient:#ff9f43:#ee5253><bold>Воин</bold></gradient>"
    raw: "<#ff9f43>[Воин]</#ff9f43>"
    visible: "<#ff9f43>[Воин]</#ff9f43>"
    description: "Бонус к максимальному здоровью."
    icon: NETHERITE_SWORD
    menu-icons:
      not-owned:
        material: BARRIER
        glow: false
      owned-inactive:
        material: NETHERITE_SWORD
        glow: false
      owned-active:
        material: NETHERITE_SWORD
        glow: true
    enabled: true
    purchase-options:
      permanent:
        enabled: true
        cost: 450
      temporary: {}
    effects:
      - type: max_health
        amount: 4.0
```

## Пример шаблона эффекта (`effects.yml`)
```yml
templates:
  speed_boost:
    - type: potion
      effect: SPEED
      amplifier: 1
      ambient: false
      particles: true
      icon: true
```
Использование в титуле:
```yml
effects:
  - template: speed_boost
```

## Поддержка
Если что-то не работает:
1. Проверьте, что версия Java = 21
2. Проверьте кодировку `.yml` (UTF-8 без BOM)
3. Проверьте зависимости (`/plugins`)
4. Выполните `/extracttitles debug`