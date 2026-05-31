package com.fleetwise.api.notification.service;

import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.notification.dto.NotificationResponse;
import com.fleetwise.api.notification.entity.Notification;
import com.fleetwise.api.notification.entity.NotificationType;
import com.fleetwise.api.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void create(UUID userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .title(title)
                        .message(message)
                        .type(type)
                        .read(false)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .forEach(notification -> notification.setRead(true));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}