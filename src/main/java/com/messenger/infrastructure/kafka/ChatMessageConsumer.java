package com.messenger.infrastructure.kafka;

import com.messenger.chat.dto.ChatMessageResponse;
import com.messenger.chat.entity.ChatMessage;
import com.messenger.chat.entity.MessageType;
import com.messenger.chat.event.ChatMessageEvent;
import com.messenger.chat.repository.ChatMessageRepository;
import com.messenger.infrastructure.sharding.ShardKeyHolder;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "chat.message.sent",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "false"
    )
    public void consumeMessage(String jsonMessage) {
        try {
            ChatMessageEvent event = objectMapper.readValue(jsonMessage, ChatMessageEvent.class);
            consumeEvent(event);
        } catch (Exception e) {
            log.error("[kafka-consume-failed] payload={}, error={}", jsonMessage, e.getMessage(), e);
        }
    }

    // Reusable processing path for both Kafka listener and direct fallback.
    public void consumeEvent(ChatMessageEvent event) {
        try {
            ChatMessage chatMessage = ChatMessage.builder()
                    .chatRoomId(event.getChatRoomId())
                    .senderId(event.getSenderId())
                    .content(event.getContent())
                    .messageType(parseMessageType(event.getMessageType()))
                    .attachmentUrl(event.getAttachmentUrl())
                    .attachmentName(event.getAttachmentName())
                    .attachmentContentType(event.getAttachmentContentType())
                    .attachmentSize(event.getAttachmentSize())
                    .mentions(event.getMentions())
                    .sentAt(event.getSentAt() != null ? event.getSentAt() : LocalDateTime.now())
                    .build();

            ChatMessage savedMessage;
            try {
                ShardKeyHolder.set(event.getChatRoomId());
                savedMessage = chatMessageRepository.save(chatMessage);
            } finally {
                ShardKeyHolder.clear();
            }

            String senderProfileImage;
            try {
                ShardKeyHolder.set(0L);
                senderProfileImage = userRepository.findById(event.getSenderId())
                        .map(User::getProfileImage)
                        .orElse(null);
            } finally {
                ShardKeyHolder.clear();
            }

            ChatMessageResponse response = ChatMessageResponse.from(
                    savedMessage,
                    event.getSenderName(),
                    senderProfileImage,
                    0
            );

            messagingTemplate.convertAndSend("/topic/chatroom/" + event.getChatRoomId(), response);
        } catch (Exception e) {
            log.error("[message-process-failed] roomId={}, senderId={}, error={}",
                    event.getChatRoomId(), event.getSenderId(), e.getMessage(), e);
        }
    }

    private MessageType parseMessageType(String type) {
        try {
            return type != null ? MessageType.valueOf(type) : MessageType.TEXT;
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }
}
