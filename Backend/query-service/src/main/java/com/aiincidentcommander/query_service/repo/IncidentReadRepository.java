package com.aiincidentcommander.query_service.repo;


import com.aiincidentcommander.query_service.model.IncidentReadModel;
import com.aiincidentcommander.query_service.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentReadRepository extends JpaRepository<IncidentReadModel , Long> {

    List<IncidentReadModel> findByStatus(IncidentStatus status);
    List<IncidentReadModel> findBySeverity(String severity);
    List<IncidentReadModel> findByServiceName(String serviceName );
    List<IncidentReadModel> findByStatusIn(List<IncidentStatus> statuses);

}
