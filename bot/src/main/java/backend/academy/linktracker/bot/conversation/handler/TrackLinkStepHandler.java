package backend.academy.linktracker.bot.conversation.handler;

import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.conversation.TrackDialogState;
import backend.academy.linktracker.bot.conversation.TrackStep;
import backend.academy.linktracker.contract.link.dto.ParsedSupportedLink;
import backend.academy.linktracker.contract.link.exception.UnsupportedLinkFormatException;
import backend.academy.linktracker.contract.link.parser.SupportedLinkParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackLinkStepHandler implements TrackStepHandler {
    private final TrackDialogStateRepository trackDialogStateRepository;
    private final SupportedLinkParser supportedLinkParser;
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public boolean supports(TrackDialogState state) {
        return state.step() == TrackStep.WAITING_LINK;
    }

    @Override
    public void handle(long chatId, String rawText, TrackDialogState state) {
        try {
            ParsedSupportedLink parsedLink = supportedLinkParser.parse(rawText);

            trackDialogStateRepository.save(
                chatId,
                TrackDialogState.waitingTags(parsedLink.uri())
            );

            telegramSender.sendPlain(
                chatId,
                messageService.get("link.accept.success")
            );
        } catch (UnsupportedLinkFormatException e) {
            telegramSender.sendPlain(
                chatId,
                messageService.get("link.accept.fail")
            );
        }
    }
}
