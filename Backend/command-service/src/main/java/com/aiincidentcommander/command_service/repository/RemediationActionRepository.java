package com.aiincidentcommander.command_service.repository;


import com.aiincidentcommander.command_service.model.RemediationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RemediationActionRepository extends JpaRepository<RemediationAction , Long> {

     List<RemediationAction> findByIncidentId( Long incidentId );
}
