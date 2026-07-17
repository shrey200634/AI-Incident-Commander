package com.aiincidentcommander.query_service.repo;

import com.aiincidentcommander.query_service.model.ActionReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionReadRepo extends JpaRepository<ActionReadModel , Long> {

    List<ActionReadModel> findByIncidentId(Long id );

}
