package backend.academy.linktracker.bot.track.handler;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.exception.chat.ChatNotRegisteredException;
import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.client.ScrapperClientException;
import backend.academy.linktracker.bot.exception.client.ScrapperUnavailableException;
import backend.academy.linktracker.bot.exception.link.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.track.TrackDialogState;
import backend.academy.linktracker.bot.track.TrackStep;
import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

@Component
@RequiredArgsConstructor
public class TrackTagsStepHandler implements TrackStepHandler {
    private final TrackDialogStateRepository trackDialogStateRepository;
    private final ScrapperClient scrapperClient;
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public boolean supports(TrackDialogState state) {
        return state.step() == TrackStep.WAITING_TAGS;
    }

    @Override
    public void handle(long chatId, String rawText, TrackDialogState state) {
        URI link = state.link();
        Set<String> tags = parseTags(rawText);

        AddLinkRequest request = new AddLinkRequest(link, tags, List.of());

        try {
            scrapperClient.addLink(chatId, request);
            telegramSender.sendPlain(chatId, messageService.get("link.add.success"));
            trackDialogStateRepository.deleteByChatId(chatId);
        } catch (LinkAlreadyTrackedException e) {
            telegramSender.sendPlain(chatId, messageService.get("link.add.already-tracked"));
            trackDialogStateRepository.deleteByChatId(chatId);
        } catch (ChatNotRegisteredException e) {
            telegramSender.sendPlain(chatId, messageService.get("link.add.chat-not-registered"));
            trackDialogStateRepository.deleteByChatId(chatId);
        } catch (InvalidScrapperRequestException e) {
            trackDialogStateRepository.save(chatId, TrackDialogState.waitingLink());
            telegramSender.sendPlain(
                chatId,
                messageService.get("link.add.invalid-scrapper-request")
            );
        } catch (ScrapperUnavailableException e) {
            telegramSender.sendPlain(chatId, messageService.get("link.add.scrapper-is-unavailable"));
            trackDialogStateRepository.deleteByChatId(chatId);
        } catch (ScrapperClientException e) {
            telegramSender.sendPlain(chatId, messageService.get("link.add.client-error"));
            trackDialogStateRepository.deleteByChatId(chatId);
        }
    }

    private Set<String> parseTags(String rawText) {
        String normalized = rawText == null ? "" : rawText.strip();

        if (normalized.isBlank() || normalized.equals("-")) {
            return Set.of();
        }

        return Arrays.stream(normalized.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                Set::copyOf
            ));
    }
}
