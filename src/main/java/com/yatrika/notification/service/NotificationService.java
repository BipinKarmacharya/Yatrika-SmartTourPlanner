package com.yatrika.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.yatrika.notification.domain.DeviceToken;
import com.yatrika.notification.domain.Notification;
import com.yatrika.notification.repository.DeviceTokenRepository;
import com.yatrika.notification.repository.NotificationRepository;
import com.yatrika.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void createAndSend(User user, String title, String message, Notification.NotificationType type, Long refId) {
        // 1. DUPLICATE CHECK: Prevent sending the same type of alert for the same trip twice in 24 hours
        java.time.LocalDateTime twentyFourHoursAgo = java.time.LocalDateTime.now().minusHours(24);
        boolean alreadySent = notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(
                user.getId(), type, refId, twentyFourHoursAgo);

        if (alreadySent) {
            log.info("Notification of type {} already sent for trip {} in the last 24h. Skipping.", type, refId);
            return;
        }

        // 2. Save to Database
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(refId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // 3. Fetch tokens and send Push
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user.getId());
        for (DeviceToken deviceToken : tokens) {
            sendFcmMessage(deviceToken.getToken(), title, message);
        }
    }

    private void sendFcmMessage(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM message: {}", response);
        } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
            // Check if the token is invalid or unregistered
            String errorCode = e.getMessagingErrorCode().name();
            if (errorCode.equals("UNREGISTERED") || errorCode.equals("INVALID_ARGUMENT")) {
                log.warn("Token {} is invalid. Removing from database. Error: {}", token, errorCode);
                deviceTokenRepository.deleteByToken(token);
            } else {
                log.error("Firebase error: {} - {}", errorCode, e.getMessage());
            }
        } catch (Exception e) {
            log.error("General error sending FCM message: {}", e.getMessage());
        }
    }
}