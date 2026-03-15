# Настройка конфигов ExtractTitles

Этот файл описывает **только настройку конфигов** плагина.

## config.yml

- `purchase.currency-provider`
  - Глобальный провайдер валюты для покупок.
  - Значения: `PLAYERPOINTS` или `VAULT`.

- `placeholders.none-word`
  - Текст для случаев, когда активного титула нет.
  - Используется в плейсхолдерах вида `..._or_none`.

- `validation.expiry-check-interval-seconds`
  - Интервал проверки истечения временных титулов.
  - Чем меньше значение, тем быстрее обновляется статус, но выше нагрузка.

- `menu.auto-refresh-ticks`
  - Частота обновления открытого меню (тик = 1/20 секунды).
  - Нужно для актуального отображения оставшегося времени.

## storage.yml

- `type`
  - Тип хранилища: `sqlite` или `mysql`.

- `sqlite.file`
  - Имя файла SQLite-базы.
  - Пример: `extracttitles.db`.

- `mysql.host`, `mysql.port`, `mysql.database`, `mysql.username`, `mysql.password`
  - Подключение к MySQL.

- `mysql.ssl`
  - Включение SSL для MySQL (если поддерживается вашей БД/хостингом).

- Параметры пула (`pool.*`, если есть в вашем файле)
  - Настройки производительности и стабильности подключения.

## messages.yml

Файл для всех сообщений плагина.

- `expire-time-format`
  - Формат даты истечения временного титула.
  - Пример: `dd.MM.yyyy HH:mm`.

- `duration-forever`
  - Как отображать бессрочный титул (`навсегда`, `неограничено` и т.д.).

- `help-user`, `help-admin`
  - Тексты справки для игроков и админов.

- `reason-*`
  - Человекочитаемые причины ошибок покупки/выдачи.

### Переменные в сообщениях

- `{title}` — отображаемое имя титула
- `{player}` — имя игрока
- `{reason}` — причина ошибки
- `{duration}` — длительность в читабельном формате
- `{expire_at}` — дата истечения
- `{cost}` — стоимость
- `{purchase_options}` — варианты покупки
- `{owned}` — количество купленных титулов
- `{active}` — активный титул

## menus.yml

Файл настройки GUI.

- `menu.title`, `menu.size`
  - Заголовок и размер меню.

- `menu.filler.material`, `menu.filler.name`, `menu.filler.slots`
  - Заполнитель интерфейса.

- `menu.info.*`
  - Карточка информации игрока.
  - В `lore` доступны: `{balance}`, `{owned_total}`, `{active}`, `{active_expiry}`, `{player}`.

- `menu.title-slots`
  - Список слотов для карточек титулов.

- `menu.navigation.*`
  - Кнопки предыдущей/следующей страницы и индикатор страницы.

### Карточки титулов

- `menu.cards.separator`
  - Разделительная линия в карточке.
  - Можно выводить в lore через `{line}`.

- `menu.cards.name.not-owned`
- `menu.cards.name.owned-inactive`
- `menu.cards.name.owned-active`
  - Имя предмета карточки по состоянию.
  - Здесь можно полностью настраивать стиль (цвет, градиент, bold и т.д.).

- `menu.cards.not-owned`
- `menu.cards.owned-inactive`
- `menu.cards.owned-active`
  - Lore карточек по состояниям.

### Переменные для карточек

- `{name}` — display-name титула
- `{title_id}` — внутренний ID титула
- `{description}` — описание титула
- `{cost}` — стоимость
- `{purchase_options}` — доступные варианты покупки
- `{owned_expiry}` — срок владения (навсегда/остаток)
- `{line}` — разделитель из `menu.cards.separator`

## titles.yml

Главный файл титулов (`titles.<id>`).

- `display-name`
  - Красивое название титула.

- `raw`
  - Сырой формат (часто для плейсхолдеров).

- `visible`
  - Видимый формат для чатов/таба/scoreboard.

- `description`
  - Описание титула в GUI.

- `icon`
  - Базовая иконка титула.

- `menu-icons.not-owned.material`, `menu-icons.not-owned.glow`
- `menu-icons.owned-inactive.material`, `menu-icons.owned-inactive.glow`
- `menu-icons.owned-active.material`, `menu-icons.owned-active.glow`
  - Иконки и перелив по состояниям.

- `enabled`
  - Включен ли титул.

- `purchase-options.permanent.enabled`
- `purchase-options.permanent.cost`
  - Покупка навсегда.

- `purchase-options.temporary.<duration>: <price>`
  - Покупка на время.
  - Примеры ключей длительности: `1h`, `1d`, `7d`, `30d`.

- `effects`
  - Список эффектов титула (встроенные или шаблоны).

## effects.yml

Файл шаблонов эффектов.

- `templates.<template_id>`
  - Переиспользуемый набор эффектов.

Подключение шаблона в `titles.yml`:

```yml
effects:
  - template: speed_boost
```

## Встроенные типы эффектов

- `max_health`
  - Параметры: `amount`

- `potion`
  - Параметры: `effect`, `amplifier`, `ambient`, `particles`, `icon`

- `permission`
  - Параметр: `nodes` (список прав)

- `command`
  - Параметры: `on-activate`, `on-deactivate`

- `mining_3x3`
  - Параметры: `tools`, `worlds`, `disabled-blocks`

## Форматирование текста

- Поддерживается MiniMessage (`<#RRGGBB>`, `<gradient:...>`, `<bold>` и т.д.).
- Для стабильной загрузки YAML используйте **UTF-8 без BOM**.