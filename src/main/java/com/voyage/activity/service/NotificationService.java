package com.voyage.activity.service;

import com.voyage.activity.domain.Notification;
import com.voyage.activity.dto.NotificationResponse;
import com.voyage.activity.repository.NotificationRepository;
import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_RESULTS = 100;

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, Limit.of(MAX_RESULTS));
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        notification.markRead(Instant.now());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        Instant now = Instant.now();
        notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                .forEach(n -> n.markRead(now));
    }
}
