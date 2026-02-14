package com.messenger.chat.controller;

import com.messenger.chat.dto.ChatMessageRequest;
import com.messenger.chat.dto.ChatMessageResponse;
import com.messenger.chat.event.ChatMessageEvent;
import com.messenger.chat.service.ChatMessageService;
import com.messenger.common.dto.ApiResponse;
import com.messenger.infrastructure.kafka.ChatMessageProducer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatMessageController {

    private static final long MAX_UPLOAD_SIZE = 20 * 1024 * 1024L;
    private static final String CHAT_UPLOAD_DIR = "uploads/chat";

    private final ChatMessageService chatMessageService;
    private final ChatMessageProducer chatMessageProducer;

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<ChatMessageResponse> messages = chatMessageService.getMessageHistory(roomId, page, size);
        return ResponseEntity.ok(ApiResponse.success("메시지 이력을 조회했습니다.", messages));
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @PathVariable Long roomId,
            @RequestBody ChatMessageRequest request,
            HttpSession session) {

        Long senderId = (Long) session.getAttribute("userId");
        String senderName = (String) session.getAttribute("displayName");

        publishMessageEvent(roomId, senderId, senderName, request);
        return ResponseEntity.ok(ApiResponse.success("메시지가 전송되었습니다."));
    }

    @PostMapping("/{roomId}/messages/file")
    public ResponseEntity<ApiResponse<Void>> sendFileMessage(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            HttpSession session) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("파일이 비어 있습니다."));
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("파일 크기는 20MB 이하여야 합니다."));
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }

        Path uploadPath = Paths.get(CHAT_UPLOAD_DIR).toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String storedName = UUID.randomUUID() + ext;
        Path target = uploadPath.resolve(storedName);
        file.transferTo(target.toFile());

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        boolean image = contentType.startsWith("image/");

        ChatMessageRequest request = new ChatMessageRequest();
        request.setChatRoomId(roomId);
        request.setContent((content != null && !content.isBlank()) ? content : originalName);
        request.setMessageType(image ? "IMAGE" : "FILE");
        request.setAttachmentUrl("/uploads/chat/" + storedName);
        request.setAttachmentName(originalName);
        request.setAttachmentContentType(contentType);
        request.setAttachmentSize(file.getSize());

        Long senderId = (Long) session.getAttribute("userId");
        String senderName = (String) session.getAttribute("displayName");
        publishMessageEvent(roomId, senderId, senderName, request);

        return ResponseEntity.ok(ApiResponse.success("파일이 전송되었습니다."));
    }

    private void publishMessageEvent(Long roomId, Long senderId, String senderName, ChatMessageRequest request) {
        log.info("[REST 메시지 전송] roomId={}, senderId={}, type={}",
                roomId,
                senderId,
                request.getMessageType());

        ChatMessageEvent event = ChatMessageEvent.builder()
                .chatRoomId(roomId)
                .senderId(senderId != null ? senderId : 0L)
                .senderName(senderName != null ? senderName : "익명")
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .attachmentContentType(request.getAttachmentContentType())
                .attachmentSize(request.getAttachmentSize())
                .sentAt(LocalDateTime.now())
                .build();

        chatMessageProducer.sendMessage(event);
    }
}
