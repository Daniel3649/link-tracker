package backend.academy.linktracker;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

@org.testcontainers.junit.jupiter.Testcontainers
abstract class AbstractBotScrapperE2ETest {
    protected static final Duration ASSERTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    protected static final Path BOT_JAR = findBootJarUnchecked(Path.of("bot/target"), Path.of("../bot/target"));

    protected static final Path SCRAPPER_JAR =
            findBootJarUnchecked(Path.of("scrapper/target"), Path.of("../scrapper/target"));

    protected static final Path MIGRATIONS_DIR =
            findExistingPathUnchecked(Path.of("migrations"), Path.of("../migrations"));

    protected static final WireMockServer MOCK =
            new WireMockServer(wireMockConfig().dynamicPort());

    protected static final String POSTGRES_DB = "link_tracker";
    protected static final String POSTGRES_USER = "postgres";
    protected static final String POSTGRES_PASSWORD = "postgres";

    static {
        MOCK.start();
        stubTelegramGetUpdatesEmptyByDefault();
        Testcontainers.exposeHostPorts(MOCK.port());
    }

    protected static final Network NETWORK = Network.newNetwork();
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(90);

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName(POSTGRES_DB)
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASSWORD)
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres");

    @Container
    protected static final GenericContainer<?> SCRAPPER = new GenericContainer<>(
                    new ImageFromDockerfile("localhost/linktracker-scrapper-e2e:latest", false)
                            .withFileFromPath("app.jar", SCRAPPER_JAR)
                            .withFileFromPath("migrations", MIGRATIONS_DIR)
                            .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre")
                                    .copy("app.jar", "/app.jar")
                                    .copy("migrations", "/migrations")
                                    .entryPoint("java", "-jar", "/app.jar")
                                    .build()))
            .dependsOn(POSTGRES)
            .withNetwork(NETWORK)
            .withNetworkAliases("scrapper")
            .withAccessToHost(true)
            .withExposedPorts(8081)
            .withEnv("SERVER_PORT", "8081")
            .withEnv("POSTGRES_URL", "jdbc:postgresql://postgres:5432/" + POSTGRES_DB)
            .withEnv("POSTGRES_USER", POSTGRES_USER)
            .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
            .withEnv("APP_DATABASE_ACCESS_TYPE", "SQL")
            .withEnv("APP_LIQUIBASE_CHANGE_LOG", "file:/migrations/master.xml")
            .withEnv("APP_BOT_TRANSPORT", "http")
            .withEnv("APP_BOT_BASE_URL", "http://bot:8080")
            .withEnv("APP_GITHUB_BASE_URL", "http://host.testcontainers.internal:" + MOCK.port())
            .withEnv("APP_STACKOVERFLOW_BASE_URL", "http://host.testcontainers.internal:" + MOCK.port())
            .withEnv("GITHUB_TOKEN", "dummy-github-token")
            .withEnv("STACKOVERFLOW_KEY", "dummy-stackoverflow-key")
            .withEnv("STACKOVERFLOW_ACCESS_KEY", "dummy-stackoverflow-access-key")
            .withEnv("APP_SCHEDULER_LINK_CHECK_DELAY_MS", "1h")
            .waitingFor(new WaitAllStrategy()
                    .withStrategy(Wait.forListeningPort())
                    .withStrategy(Wait.forLogMessage(".*Started ScrapperApplication.*", 1))
                    .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
            .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

    @Container
    protected static final GenericContainer<?> BOT = new GenericContainer<>(
                    new ImageFromDockerfile("localhost/linktracker-bot-e2e:latest", false)
                            .withFileFromPath("app.jar", BOT_JAR)
                            .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre")
                                    .copy("app.jar", "/app.jar")
                                    .entryPoint("java", "-jar", "/app.jar")
                                    .build()))
            .dependsOn(SCRAPPER)
            .withNetwork(NETWORK)
            .withNetworkAliases("bot")
            .withAccessToHost(true)
            .withExposedPorts(8080)
            .withEnv("SERVER_PORT", "8080")
            .withEnv("TELEGRAM_TOKEN", "test-token")
            .withEnv("APP_TELEGRAM_URL", "http://host.testcontainers.internal:" + MOCK.port() + "/bot")
            .withEnv("APP_TELEGRAM_UPDATE_LISTENER_SLEEP", "200ms")
            .withEnv("APP_TELEGRAM_INIT_COMMANDS_ON_STARTUP", "false")
            .withEnv("APP_TELEGRAM_DEBUG", "true")
            .withEnv("APP_SCRAPPER_TRANSPORT", "http")
            .withEnv("APP_SCRAPPER_BASE_URL", "http://scrapper:8081")
            .waitingFor(new WaitAllStrategy()
                    .withStrategy(Wait.forListeningPort())
                    .withStrategy(Wait.forLogMessage(".*Started BotApplication.*", 1))
                    .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
            .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

    protected String scrapperBaseUrl() {
        return "http://" + SCRAPPER.getHost() + ":" + SCRAPPER.getMappedPort(8081);
    }

    protected void resetMocks() {
        MOCK.resetAll();
        stubTelegramGetUpdatesEmptyByDefault();
    }

    private static void stubTelegramGetUpdatesEmptyByDefault() {
        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates")).atPriority(10).willReturn(okJson("""
                { "ok": true, "result": [] }
                """)));
    }

    protected void stubTelegramGetUpdatesOnceThenEmpty(String text, long chatId) {
        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-updates")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(singleUpdateJson(1, chatId, text)))
                .willSetStateTo("EMPTY"));

        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-updates")
                .whenScenarioStateIs("EMPTY")
                .willReturn(okJson("""
                { "ok": true, "result": [] }
                """)));
    }

    protected void stubTelegramTrackFlow(long chatId, URI link, String tags) {
        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-track-flow")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(okJson(singleUpdateJson(1, chatId, "/start")))
                .willSetStateTo("TRACK"));

        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-track-flow")
                .whenScenarioStateIs("TRACK")
                .willReturn(okJson(singleUpdateJson(2, chatId, "/track")))
                .willSetStateTo("LINK"));

        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-track-flow")
                .whenScenarioStateIs("LINK")
                .willReturn(okJson(singleUpdateJson(3, chatId, link.toString())))
                .willSetStateTo("TAGS"));

        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-track-flow")
                .whenScenarioStateIs("TAGS")
                .willReturn(okJson(singleUpdateJson(4, chatId, tags)))
                .willSetStateTo("EMPTY"));

        MOCK.stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("telegram-track-flow")
                .whenScenarioStateIs("EMPTY")
                .willReturn(okJson("""
                { "ok": true, "result": [] }
                """)));
    }

    protected void stubTelegramSendMessageOk(long chatId) {
        MOCK.stubFor(post(urlMatching("/bot[^/]+/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                      "ok": true,
                      "result": {
                        "message_id": 1,
                        "date": 1700000000,
                        "chat": { "id": %d, "type": "private" },
                        "text": "ok"
                      }
                    }
                    """.formatted(chatId))));
    }

    protected void stubTelegramSetMyCommandsOk() {
        MOCK.stubFor(post(urlMatching("/bot[^/]+/setMyCommands"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    { "ok": true, "result": true }
                    """)));
    }

    protected void stubGitHubEndpoints() {
        stubGitHubRepo("octocat", "Hello-World", "\"etag-1\"");
        stubGitHubRepo("spring-projects", "spring-boot", "\"etag-2\"");
    }

    protected void stubGitHubRepo(String owner, String repo, String etag) {
        MOCK.stubFor(get(urlPathEqualTo("/repos/" + owner + "/" + repo))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", etag)
                        .withBody("""
                    {
                      "id": 1,
                      "name": "%s",
                      "full_name": "%s/%s"
                    }
                    """.formatted(repo, owner, repo))));

        MOCK.stubFor(get(urlPathEqualTo("/repos/" + owner + "/" + repo + "/activity"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    [
                      {
                        "id": 1001,
                        "activity_type": "push",
                        "timestamp": "2026-03-15T10:00:00Z"
                      }
                    ]
                    """)));
    }

    protected HttpResponse<String> registerChat(long chatId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(scrapperBaseUrl() + "/tg-chat/" + chatId))
                .timeout(HTTP_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> getLinks(long chatId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(scrapperBaseUrl() + "/links"))
                .timeout(HTTP_TIMEOUT)
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .GET()
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected String singleUpdateJson(int updateId, long chatId, String text) {
        return """
            {
              "ok": true,
              "result": [
                {
                  "update_id": %d,
                  "message": {
                    "message_id": %d,
                    "from": { "id": 1, "is_bot": false, "first_name": "Test" },
                    "chat": { "id": %d, "type": "private" },
                    "date": 1700000000,
                    "text": "%s"
                  }
                }
              ]
            }
            """.formatted(updateId, updateId, chatId, escapeJson(text));
    }

    protected static Path findBootJarUnchecked(Path... candidates) {
        for (Path candidate : candidates) {
            try {
                if (Files.exists(candidate)) {
                    return findBootJar(candidate);
                }
            } catch (IOException ignored) {
            }
        }
        throw new IllegalStateException("Boot jar not found. Checked: " + Arrays.toString(candidates));
    }

    protected static Path findExistingPathUnchecked(Path... candidates) {
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Path not found. Checked: " + Arrays.toString(candidates));
    }

    protected static Path findBootJar(Path targetDir) throws IOException {
        try (Stream<Path> files = Files.list(targetDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return !name.startsWith("original-")
                                && !name.contains("-plain")
                                && !name.contains("-sources")
                                && !name.contains("-javadoc");
                    })
                    .min(Comparator.comparing(Path::toString))
                    .orElseThrow(() -> new IllegalStateException("Boot jar not found in " + targetDir));
        }
    }

    protected static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
