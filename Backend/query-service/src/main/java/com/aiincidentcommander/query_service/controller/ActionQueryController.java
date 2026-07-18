package com.aiincidentcommander.query_service.controller;

import com.aiincidentcommander.query_service.dto.ActionReadDto;
import com.aiincidentcommander.query_service.service.ActionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/query")
public class ActionQueryController {

    private ActionQueryService service ;


    @GetMapping("/incidents/{incidentId}/actions")
    public ResponseEntity<List<ActionReadDto>> getActionsByIncident(
            @PathVariable("incidentId") Long incidentId) {
        return ResponseEntity.ok(service.getActionByIncidentId(incidentId));
    }

    @GetMapping("/actions/{id}")
    public  ResponseEntity<ActionReadDto> getByActionId (
            @PathVariable("id") Long id){
        return ResponseEntity.ok(service.getActionById(id));
    }

}
