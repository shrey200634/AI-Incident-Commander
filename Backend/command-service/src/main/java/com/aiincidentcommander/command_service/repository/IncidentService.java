package com.aiincidentcommander.command_service.repository;

import com.aiincidentcommander.command_service.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentService  extends JpaRepository<Incident , Long> {

}
