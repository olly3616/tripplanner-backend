package com.voyage.activity.repository;

import com.voyage.activity.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Limit limit);

    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);
}
