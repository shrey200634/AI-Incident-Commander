package com.aiincidentcommander.query_service.repo;

import com.aiincidentcommander.query_service.model.DlqRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DlqRecordRepository extends JpaRepository<DlqRecord, Long> {
    List<DlqRecord> findByReplayedFalse();
}