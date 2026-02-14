$(document).ready(function () {

    var stompClient = null;
    var connected = false;
    var lastMessageId = 0;
    var lastMessageDate = null;
    var pollingInterval = null;

    var $messageArea = $('#messageArea');
    var $messageInput = $('#messageInput');
    var $messageForm = $('#messageForm');
    var $connectionStatus = $('#connectionStatus');
    var $loadingIndicator = $('#loadingIndicator');
    var $fileInput = $('#fileInput');
    var $attachBtn = $('#attachBtn');
    var $confirmFileSendBtn = $('#confirmFileSendBtn');
    var fileConfirmModalEl = document.getElementById('fileConfirmModal');
    var fileConfirmModal = fileConfirmModalEl ? new bootstrap.Modal(fileConfirmModalEl) : null;
    var pendingFile = null;
    var pendingFileObjectUrl = null;

    $attachBtn.on('click', function () {
        $fileInput.trigger('click');
    });

    $fileInput.on('change', function () {
        var file = $fileInput[0].files[0];
        if (!file) return;
        openFileConfirmModal(file);
    });

    if (fileConfirmModalEl) {
        fileConfirmModalEl.addEventListener('hidden.bs.modal', function () {
            if (pendingFileObjectUrl) {
                URL.revokeObjectURL(pendingFileObjectUrl);
                pendingFileObjectUrl = null;
            }
            pendingFile = null;
            $fileInput.val('');
        });
    }

    $confirmFileSendBtn.on('click', function () {
        if (!pendingFile) {
            return;
        }
        var caption = $messageInput.val().trim();
        sendFileMessage(pendingFile, caption);
        if (fileConfirmModal) {
            fileConfirmModal.hide();
        }
    });

    $messageForm.on('submit', function (e) {
        e.preventDefault();

        var file = $fileInput[0].files[0];
        var content = $messageInput.val().trim();

        if (file) {
            openFileConfirmModal(file);
            return;
        }

        if (!content) return;
        sendTextMessage(content);
    });

    function sendTextMessage(content) {
        $.ajax({
            url: '/api/chat/rooms/' + ROOM_ID + '/messages',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                chatRoomId: ROOM_ID,
                content: content,
                messageType: 'TEXT'
            }),
            success: function () {
                markAsRead();
            },
            error: function (xhr) {
                console.error('[chat] send text failed:', xhr.status, xhr.responseText);
                alert('메시지 전송에 실패했습니다.');
            }
        });

        $messageInput.val('');
        $messageInput.focus();
    }

    function sendFileMessage(file, content) {
        var formData = new FormData();
        formData.append('file', file);
        if (content) {
            formData.append('content', content);
        }

        $.ajax({
            url: '/api/chat/rooms/' + ROOM_ID + '/messages/file',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                markAsRead();
            },
            error: function (xhr) {
                console.error('[chat] send file failed:', xhr.status, xhr.responseText);
                alert(xhr.responseJSON ? xhr.responseJSON.message : '파일 전송에 실패했습니다.');
            }
        });

        $fileInput.val('');
        $messageInput.val('');
        $messageInput.focus();
    }

    function openFileConfirmModal(file) {
        pendingFile = file;
        $('#fileConfirmName').text(file.name);
        $('#fileConfirmSize').text(formatFileSize(file.size));

        if (pendingFileObjectUrl) {
            URL.revokeObjectURL(pendingFileObjectUrl);
            pendingFileObjectUrl = null;
        }

        if (file.type && file.type.indexOf('image/') === 0) {
            pendingFileObjectUrl = URL.createObjectURL(file);
            $('#filePreviewImage').attr('src', pendingFileObjectUrl);
            $('#filePreviewImageWrap').show();
            $('#filePreviewGeneric').hide();
        } else {
            $('#filePreviewImage').attr('src', '');
            $('#filePreviewImageWrap').hide();
            $('#filePreviewGeneric').show();
        }

        if (fileConfirmModal) {
            fileConfirmModal.show();
        }
    }

    function connect() {
        try {
            var socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);

            stompClient.debug = function (str) {
                console.log('[STOMP] ' + str);
            };

            stompClient.connect({}, onConnected, onConnectionError);
        } catch (e) {
            console.error('[chat] websocket init failed:', e);
            startPolling();
        }
    }

    function onConnected() {
        connected = true;
        hideConnectionStatus();

        stompClient.subscribe('/topic/chatroom/' + ROOM_ID, onMessageReceived);
    }

    function onConnectionError(error) {
        connected = false;
        console.error('[chat] websocket connection failed:', error);
        showConnectionStatus('실시간 연결 실패 - 자동 새로고침 모드');
        startPolling();
    }

    function onMessageReceived(message) {
        try {
            var msg = JSON.parse(message.body);

            if (msg.id && msg.id > lastMessageId) {
                lastMessageId = msg.id;
                appendMessage(msg);
                scrollToBottom();
                markAsRead();
            } else if (!msg.id) {
                appendMessage(msg);
                scrollToBottom();
            }
        } catch (e) {
            console.error('[chat] parse failed:', e, message.body);
        }
    }

    function appendMessage(msg) {
        var isMyMessage = (msg.senderId == USER_ID);
        var messageClass = isMyMessage ? 'message-mine' : 'message-other';

        if (msg.sentAt) {
            var msgDate = new Date(msg.sentAt);
            var dateKey = msgDate.getFullYear() + '-' + (msgDate.getMonth() + 1) + '-' + msgDate.getDate();
            if (lastMessageDate !== dateKey) {
                lastMessageDate = dateKey;
                var dateLabel = msgDate.getFullYear() + '년 ' + (msgDate.getMonth() + 1) + '월 ' + msgDate.getDate() + '일';
                var weekdays = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
                dateLabel += ' ' + weekdays[msgDate.getDay()];
                $messageArea.append(
                    '<div class="date-separator">' +
                    '  <span class="date-separator-label">' + dateLabel + '</span>' +
                    '</div>'
                );
            }
        }

        if (msg.messageType === 'SYSTEM') {
            $messageArea.append(
                '<div class="message-system text-center text-muted my-2">' +
                '<small>' + escapeHtml(msg.content) + '</small>' +
                '</div>'
            );
            return;
        }

        var timeStr = formatTime(msg.sentAt);

        var unreadHtml = '';
        if (isMyMessage && msg.unreadCount !== undefined && msg.unreadCount > 0) {
            unreadHtml = '<span class="unread-count">' + msg.unreadCount + '</span>';
        }

        var senderAvatarHtml = '';
        if (!isMyMessage) {
            if (msg.senderProfileImage) {
                senderAvatarHtml = '<div class="member-avatar" style="overflow:hidden; padding:0; flex-shrink:0;">' +
                    '<img src="' + msg.senderProfileImage + '" style="width:100%; height:100%; object-fit:cover; border-radius:50%;"></div>';
            } else {
                var senderInitial = msg.senderName ? msg.senderName.charAt(0) : '?';
                senderAvatarHtml = '<div class="member-avatar" style="flex-shrink:0;">' + escapeHtml(senderInitial) + '</div>';
            }
        }

        var bodyHtml = renderMessageBody(msg);

        var html =
            '<div class="message-wrapper ' + messageClass + '">' +
            (!isMyMessage ? '<div class="d-flex align-items-start gap-8">' + senderAvatarHtml + '<div>' : '') +
            '  <div class="message-bubble">' +
            (isMyMessage ? '' : '<div class="message-sender">' + escapeHtml(msg.senderName) + '</div>') +
            bodyHtml +
            '    <div class="message-time">' + unreadHtml + timeStr + '</div>' +
            '  </div>' +
            (!isMyMessage ? '</div></div>' : '') +
            '</div>';

        $messageArea.append(html);
    }

    function renderMessageBody(msg) {
        var contentHtml = msg.content ? '<div class="message-content">' + escapeHtml(msg.content) + '</div>' : '';

        if (msg.messageType === 'IMAGE' && msg.attachmentUrl) {
            return '<div class="message-content" style="margin-bottom:6px;">' +
                '<a href="' + msg.attachmentUrl + '" target="_blank" rel="noopener">' +
                '<img src="' + msg.attachmentUrl + '" alt="image" style="max-width:220px; border-radius:10px; border:1px solid rgba(255,255,255,0.15);">' +
                '</a></div>' + contentHtml;
        }

        if (msg.messageType === 'FILE' && msg.attachmentUrl) {
            var name = msg.attachmentName ? escapeHtml(msg.attachmentName) : 'download';
            return '<div class="message-content" style="margin-bottom:6px;">' +
                '<a href="' + msg.attachmentUrl + '" target="_blank" rel="noopener" class="nav-btn" style="display:inline-block; font-size:12px;">[FILE] ' + name + '</a>' +
                '</div>' + contentHtml;
        }

        return contentHtml;
    }

    function markAsRead() {
        $.post('/api/chat/rooms/' + ROOM_ID + '/read');
    }

    function loadMessageHistory() {
        $loadingIndicator.show();

        $.ajax({
            url: '/api/chat/rooms/' + ROOM_ID + '/messages',
            method: 'GET',
            data: { page: 0, size: 50 },
            success: function (response) {
                $loadingIndicator.hide();

                if (response.data && response.data.length > 0) {
                    response.data.forEach(function (msg) {
                        if (msg.id && msg.id > lastMessageId) {
                            lastMessageId = msg.id;
                        }
                        appendMessage(msg);
                    });
                } else {
                    $messageArea.append(
                        '<div class="text-center text-muted py-3" id="emptyMessage">' +
                        '첫 번째 메시지를 보내보세요.' +
                        '</div>'
                    );
                }
                scrollToBottom();
                markAsRead();
            },
            error: function (xhr) {
                $loadingIndicator.hide();
                if (xhr.status === 401 || xhr.status === 403) {
                    window.location.href = '/auth/login';
                }
            }
        });
    }

    function startPolling() {
        if (pollingInterval) return;

        pollingInterval = setInterval(function () {
            $.ajax({
                url: '/api/chat/rooms/' + ROOM_ID + '/messages',
                method: 'GET',
                data: { page: 0, size: 50 },
                success: function (response) {
                    if (response.data && response.data.length > 0) {
                        var hasNew = false;
                        response.data.forEach(function (msg) {
                            if (msg.id && msg.id > lastMessageId) {
                                lastMessageId = msg.id;
                                appendMessage(msg);
                                hasNew = true;
                            }
                        });
                        if (hasNew) {
                            $('#emptyMessage').remove();
                            scrollToBottom();
                            markAsRead();
                        }
                    }
                }
            });
        }, 3000);
    }

    function scrollToBottom() {
        $messageArea.scrollTop($messageArea[0].scrollHeight);
    }

    function showConnectionStatus(message) {
        $connectionStatus.text(message).removeClass('d-none');
    }

    function hideConnectionStatus() {
        $connectionStatus.addClass('d-none');
    }

    function formatTime(dateTimeStr) {
        if (!dateTimeStr) return '';
        var date = new Date(dateTimeStr);
        if (isNaN(date.getTime())) return '';
        var hours = date.getHours();
        var minutes = date.getMinutes();
        var ampm = hours >= 12 ? '오후' : '오전';
        hours = hours % 12;
        if (hours === 0) hours = 12;
        minutes = minutes < 10 ? '0' + minutes : minutes;
        return ampm + ' ' + hours + ':' + minutes;
    }

    function formatFileSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

    $messageInput.on('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            $messageForm.submit();
        }
    });

    loadMessageHistory();
    connect();

    setTimeout(function () {
        if (!connected) {
            startPolling();
        }
    }, 5000);

    setTimeout(function () {
        startPolling();
    }, 10000);

    $messageInput.focus();
});
