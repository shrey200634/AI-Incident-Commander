package com.aiincidentcommander.notification_service.repository;

import com.aiincidentcommander.notification_service.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository  extends JpaRepository<NotificationLog , Long> {
}
