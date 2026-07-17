package com.aiincidentcommander.command_service.controller;

import com.aiincidentcommander.command_service.dto.ActionProposed;
import com.aiincidentcommander.command_service.dto.CreateIncident;
import com.aiincidentcommander.command_service.dto.IncidentResponse;
import com.aiincidentcommander.command_service.dto.RemediationActionResponse;
import com.aiincidentcommander.command_service.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService service ;


    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident (
            @Valid @RequestBody CreateIncident request)
    {
        IncidentResponse response = service.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @PostMapping("/{id}/actions")
    public  ResponseEntity<RemediationActionResponse> proposeAction (
            @PathVariable("id") Long id ,
            @Valid @RequestBody ActionProposed request
            ) {
        RemediationActionResponse response = service.proposeResponse(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
}
