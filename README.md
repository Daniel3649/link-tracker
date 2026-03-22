# LinkTracker

`LinkTracker` состоит из двух основных сервисов:
- `bot` принимает команды из Telegram
- `scrapper` хранит отслеживаемые ссылки, опрашивает GitHub и StackOverflow и отправляет обновления в `bot`

Для `scrapper` используются PostgreSQL, Liquibase и два режима доступа к данным:
- `SQL`
- `ORM`

`in-memory` репозитории больше не используются.

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

## PostgreSQL

Локальная база и отдельный контейнер миграций поднимаются через [compose.yaml](./compose.yaml):

```bash
docker compose up -d
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

Параметры PostgreSQL по умолчанию:

- `POSTGRES_DB=link_tracker`
- `POSTGRES_USER=postgres`
- `POSTGRES_PASSWORD=postgres`
- `POSTGRES_PORT=5432`

## Миграции

Liquibase changelog лежит в [migrations/master.xml](./migrations/master.xml).

Первая миграция схемы:

- [migrations/001-init-schema.sql](./migrations/001-init-schema.sql)

По умолчанию `scrapper` использует:

- `spring.liquibase.change-log=file:./migrations/master.xml`

Миграции можно запускать двумя способами:

- отдельным контейнером `liquibase-migrations` из `compose.yaml`
- автоматически при старте `scrapper`

Если нужно перезапустить только контейнер миграций:

```bash
docker compose up liquibase-migrations
```

## Режимы Доступа К Данным

Для `scrapper` доступны два режима:

- `SQL`
- `ORM`

Выбор делается через переменную:

```bash
APP_DATABASE_ACCESS_TYPE=SQL
```

или

```bash
APP_DATABASE_ACCESS_TYPE=ORM
```

Если переменная не указана, используется `SQL`.

## Запуск Scrapper

Минимальный набор переменных для локального запуска:

```bash
export POSTGRES_URL=jdbc:postgresql://localhost:5432/link_tracker
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
export APP_DATABASE_ACCESS_TYPE=SQL
export GITHUB_TOKEN=your_github_token
export STACKOVERFLOW_KEY=your_stackoverflow_key
export STACKOVERFLOW_ACCESS_KEY=your_stackoverflow_access_token
```

Запуск:

```bash
mvn -pl scrapper -am spring-boot:run
```

`scrapper` стартует на `http://localhost:8081`.

## Запуск Bot

Минимальный набор переменных:

```bash
export TELEGRAM_TOKEN=your_telegram_bot_token
export APP_SCRAPPER_BASE_URL=http://localhost:8081
```

Запуск:

```bash
mvn -pl bot -am spring-boot:run
```

`bot` стартует на `http://localhost:8080`.

## Полезные Файлы Конфигурации

- [scrapper application.yaml](./scrapper/src/main/resources/application.yaml)
- [bot application.yaml](./bot/src/main/resources/application.yaml)

При необходимости можно использовать локальные `.env` файлы:

- `scrapper/.env`
- `bot/.env`

## Тесты

Полный набор тестов `scrapper`:

```bash
mvn -pl scrapper -am test
```

Только `bot`:

```bash
mvn -pl bot -am test
```

End-to-end тесты:

```bash
mvn -pl e2e-tests -am test
```

Для integration и e2e тестов нужен запущенный Docker daemon, потому что используется Testcontainers.

## Быстрые Команды Проверки

Сборка всего проекта:

```bash
mvn clean verify
```

Сборка без тестов:

```bash
mvn clean package -DskipTests
```

Только `scrapper`:

```bash
mvn -pl scrapper -am package
```

## Что Проверить, Если Scrapper Не Стартует

1. Поднят ли PostgreSQL:

```bash
docker compose ps
```

2. Совпадают ли `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

3. Указан ли корректный `APP_DATABASE_ACCESS_TYPE`

4. Есть ли обязательные токены:
- `GITHUB_TOKEN`
- `STACKOVERFLOW_KEY`
- `STACKOVERFLOW_ACCESS_KEY`

5. Доступен ли changelog:
- `file:./migrations/master.xml`

## Дополнительно

Полезную справочную информацию по шаблону проекта можно посмотреть в [HELP.md](./HELP.md).
