package com.voyage.activity.repository;

import com.voyage.activity.domain.ActivityLog;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByTripIdOrderByCreatedAtDesc(Long tripId, Limit limit);
}
