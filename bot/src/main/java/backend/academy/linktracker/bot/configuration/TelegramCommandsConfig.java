package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class TelegramCommandsConfig {
    private static Logger logger = LoggerFactory.getLogger(TelegramCommandsConfig.class);
    private final TelegramBot bot;
    private final CommandDispatcher commandDispatcher;

    @Bean
    ApplicationRunner setTelegramMenuCommandsOnStartup() {
        return args -> {
            BotCommand[] commands = commandDispatcher.getCommands()
                .stream().map(command ->
                    new BotCommand(command.name(), command.description()))
                .toArray(BotCommand[]::new);

            BaseResponse resp = bot.execute(new SetMyCommands(commands));

            if (!resp.isOk()) {
                logger.atError()
                    .addKeyValue("event", "telegram_set_my_commands_failed")
                    .addKeyValue("command_count", commands.length)
                    .addKeyValue("commands", commands)
                    .addKeyValue("telegram_error_code", resp.errorCode())
                    .addKeyValue("telegram_description", resp.description())
                    .log("Failed to set Telegram bot menu commands");
                throw new IllegalStateException("Failed to set bot commands: " + resp.description());
            }

            logger.atInfo()
                .addKeyValue("event", "telegram_set_my_commands_ok")
                .addKeyValue("command_count", commands.length)
                .addKeyValue("commands", commands)
                .log("Telegram bot menu commands set");
        };
    }
}
