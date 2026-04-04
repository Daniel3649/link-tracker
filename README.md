# LinkTracker

`LinkTracker` состоит из двух сервисов:
- `bot` принимает команды из Telegram
- `scrapper` хранит отслеживаемые ссылки, проверяет GitHub и StackOverflow и отправляет обновления в `bot`

Для `scrapper` используются PostgreSQL, Liquibase и два режима доступа к данным:
- `SQL`
- `ORM`

`in-memory` репозитории в `scrapper` больше не используются.

## Структура

- `bot/` — Telegram-бот
- `scrapper/` — сервис хранения ссылок и проверки обновлений
- `migrations/` — Liquibase-миграции
- `e2e-tests/` — end-to-end тесты
- `link-tracker-contract/` — общие DTO и парсеры ссылок

## Требования

- JDK 25
- Maven 3.9+
- Docker Desktop или другой работающий Docker daemon
- IntelliJ IDEA

## Какие файлы нужно заполнить

Для локального запуска нужны два файла:
- [scrapper/.env.properties](./scrapper/.env.properties)
- [bot/.env.properties](./bot/.env.properties)

Пример для `scrapper/.env.properties`:

```properties
POSTGRES_URL=jdbc:postgresql://localhost:5433/my_link_tracker
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
APP_DATABASE_ACCESS_TYPE=SQL
GITHUB_TOKEN=your_github_token
STACKOVERFLOW_KEY=your_stackoverflow_key
STACKOVERFLOW_ACCESS_KEY=your_stackoverflow_access_token
```

Пример для `bot/.env.properties`:

```properties
TELEGRAM_TOKEN=your_telegram_bot_token
APP_SCRAPPER_BASE_URL=http://localhost:8081
```

## PostgreSQL

Локальная база и контейнер с миграциями поднимаются через [compose.yaml](./compose.yaml).

Если работаешь только через IntelliJ IDEA, открой встроенный `Terminal` и выполни:

```bash
docker compose up -d postgres liquibase-migrations
```

Проверка:

```bash
docker compose ps
docker compose logs postgres
docker compose logs liquibase-migrations
```

Остановка:

```bash
docker compose down
```

Полная очистка данных:

```bash
docker compose down -v
```

Поведение такое:
- контейнер `postgres` создаёт саму базу данных
- `liquibase-migrations` создаёт таблицы внутри этой базы

Если volume уже существует, база не создаётся заново. Для полного сброса нужен `docker compose down -v`.

## Миграции

Liquibase changelog лежит в [migrations/master.xml](./migrations/master.xml).

Первая миграция схемы:
- [migrations/001-init-schema.sql](./migrations/001-init-schema.sql)

`scrapper` умеет применять миграции автоматически при старте. Дополнительно миграции запускаются отдельным контейнером `liquibase-migrations` из [compose.yaml](./compose.yaml).

## Режимы Доступа К Данным

Для `scrapper` доступны два режима:
- `SQL`
- `ORM`

Выбор делается через `APP_DATABASE_ACCESS_TYPE` в [scrapper/.env.properties](./scrapper/.env.properties).

Примеры:

```properties
APP_DATABASE_ACCESS_TYPE=SQL
```

или

```properties
APP_DATABASE_ACCESS_TYPE=ORM
```

Если переменная не указана, используется `SQL`.

## Запуск В IntelliJ IDEA

### 1. Открой проект

Открой в IntelliJ IDEA корень проекта:
- [link-tracker](.)

Дождись, пока IDEA импортирует Maven-проект.

### 2. Подними PostgreSQL

Открой встроенный `Terminal` в IDEA и выполни:

```bash
docker compose up -d postgres liquibase-migrations
```

### 3. Запусти `scrapper`

В IntelliJ IDEA открой класс:
- [ScrapperApplication.java](./scrapper/src/main/java/backend/academy/linktracker/scrapper/ScrapperApplication.java)

Запусти его через зелёную кнопку `Run`.

Что важно:
- рабочая директория должна быть корнем проекта `link-tracker`
- `scrapper` читает настройки из [scrapper/.env.properties](./scrapper/.env.properties)
- сервис стартует на `http://localhost:8081`

### 4. Запусти `bot`

В IntelliJ IDEA открой класс:
- [BotApplication.java](./bot/src/main/java/backend/academy/linktracker/bot/BotApplication.java)

Запусти его через зелёную кнопку `Run`.

Что важно:
- рабочая директория должна быть корнем проекта `link-tracker`
- `bot` читает настройки из [bot/.env.properties](./bot/.env.properties)
- сервис стартует на `http://localhost:8080`

### 5. Что должно быть в итоге

После запуска:
- `postgres` поднят в Docker
- `liquibase-migrations` успешно отработал
- `scrapper` слушает `8081`
- `bot` слушает `8080`

## Полезные Файлы Конфигурации

- [scrapper application.yaml](./scrapper/src/main/resources/application.yaml)
- [bot application.yaml](./bot/src/main/resources/application.yaml)
- [compose.yaml](./compose.yaml)

Полезную для разработки проекта информацию вы можете найти в файле [HELP.md](./HELP.md).

## Переключение HTTP и gRPC

Взаимодействие между `bot` и `scrapper` можно переключать между `http` и `grpc` через `application.yaml`.
Переключение выполняется отдельно для каждого направления вызовов:

- `bot -> scrapper`: свойство `app.scrapper.transport`
- `scrapper -> bot`: свойство `app.bot.transport`

Порты по умолчанию:

- `bot`: HTTP `8080`, gRPC `9090`
- `scrapper`: HTTP `8081`, gRPC `9091`

Важно: переключатель меняет исходящий transport клиента. HTTP-контроллеры и gRPC-серверы могут быть доступны одновременно.

### Режим `http/http`

`bot/src/main/resources/application.yaml`

```yaml
app:
  scrapper:
    transport: http
    base-url: http://localhost:8081
```

`scrapper/src/main/resources/application.yaml`

```yaml
app:
  bot:
    transport: http
    base-url: http://localhost:8080
```

### Режим `grpc/http`

`bot/src/main/resources/application.yaml`

```yaml
app:
  scrapper:
    transport: grpc
    grpc-address: localhost:9091
```

`scrapper/src/main/resources/application.yaml`

```yaml
app:
  bot:
    transport: http
    base-url: http://localhost:8080
```

### Режим `grpc/grpc`

`bot/src/main/resources/application.yaml`

```yaml
app:
  scrapper:
    transport: grpc
    grpc-address: localhost:9091
```

`scrapper/src/main/resources/application.yaml`

```yaml
app:
  bot:
    transport: grpc
    grpc-address: localhost:9090
```

