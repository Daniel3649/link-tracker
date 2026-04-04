# LinkTracker

LinkTracker – Telegram-бот, который отслеживает изменения на веб-страницах и оперативно информирует пользователя о них.

Это шаблон проекта, который вам необходимо взять за основу для разработки своей системы.
В данном файле должна находиться инструкция для ассистента по запуску и настройке бота.

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

