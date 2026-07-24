package com.aiincidentcommander.command_service.controller;

import com.aiincidentcommander.command_service.dto.*;
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

    @PostMapping("/{id}/actions/{actionId}/reject")
    public ResponseEntity<RemediationActionResponse> rejectAction(
            @PathVariable("id") Long id,
            @PathVariable("actionId") Long actionId,
            @RequestBody RejectActionRequest request) {
        RemediationActionResponse response = service.rejectAction(id, actionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/actions")
    public  ResponseEntity<RemediationActionResponse> proposeAction (
            @PathVariable("id") Long id ,
            @Valid @RequestBody ActionProposed request
            ) {
        RemediationActionResponse response = service.proposeResponse(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @GetMapping("/{id}")
    public  ResponseEntity<IncidentResponse> getIncident(
            @PathVariable("id") Long id )
            {
                IncidentResponse response = service.getIncident(id);
                return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public  ResponseEntity<IncidentResponse> updateStatus (
            @PathVariable("id") Long id ,
            @Valid @RequestBody UpdateStatusRequest request ){
        IncidentResponse response = service.updateIncidentStatus(id , request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/actions/{actionId}/approve")
    public ResponseEntity<RemediationActionResponse> approveAction(
            @PathVariable("id") Long id,
            @PathVariable("actionId") Long actionId,
            @Valid @RequestBody ApproveActionRequest request) {
        RemediationActionResponse response = service.approveAction(id, actionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/actions/{actionId}/execute")
    public  ResponseEntity<RemediationActionResponse> executeAction(
            @PathVariable("id") Long id ,
            @PathVariable("actionId") Long actionId) {
                RemediationActionResponse response = service.executeAction(id, actionId);
                return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/actions/{actionId}/rollback")
    public  ResponseEntity<RemediationActionResponse> rollbackAction(
            @PathVariable("id") Long id ,
            @PathVariable("actionId") Long actionId ){
        RemediationActionResponse response= service.rollBackAction(id,actionId);
        return ResponseEntity.ok(response);
    }

}
