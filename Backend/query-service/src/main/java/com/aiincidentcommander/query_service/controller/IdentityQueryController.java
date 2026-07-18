package com.aiincidentcommander.query_service.controller;

import com.aiincidentcommander.query_service.dto.IncidentDetailDto;
import com.aiincidentcommander.query_service.dto.IncidentReadDto;
import com.aiincidentcommander.query_service.model.IncidentStatus;
import com.aiincidentcommander.query_service.service.IncidentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/query/imcidents")
public class IdentityQueryController {
    private final IncidentQueryService service ;

    @GetMapping
    public ResponseEntity<List<IncidentReadDto>> getAllIncident(){
        return ResponseEntity.ok(service.getAllIncident());
    }

    @GetMapping("/active")
    public  ResponseEntity<List<IncidentReadDto>> getAllActiveIncident(){
        return ResponseEntity.ok(service.getActiveIncident());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentReadDto> getByIncidentId(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getIncidentById(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<IncidentDetailDto> getIncidentDetail(
            @PathVariable("id") Long id){
        return ResponseEntity.ok(service.getIncidentDetail(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<IncidentReadDto>> getByStatus(
            @PathVariable("status")IncidentStatus status){
        return ResponseEntity.ok(service.getIncidentByStatus(status));
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<IncidentReadDto>> getBySeverity(
            @PathVariable("severity") String severity){
        return ResponseEntity.ok(service.getBySeverity(severity));
    }

    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<IncidentReadDto>> getByName (
            @PathVariable("serviceName") String serviceName){
        return ResponseEntity.ok(service.getByServiceName(serviceName));
    }


}
