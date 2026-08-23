package com.aiincidentcommander.command_service.services;

import com.aiincidentcommander.command_service.dto.ApproveActionRequest;
import com.aiincidentcommander.command_service.dto.CreateIncident;
import com.aiincidentcommander.command_service.dto.RejectActionRequest;
import com.aiincidentcommander.command_service.dto.RemediationActionResponse;
import com.aiincidentcommander.command_service.event.KafkaEventPublisher;
import com.aiincidentcommander.command_service.exception.ActionNotFoundException;
import com.aiincidentcommander.command_service.exception.IncidentNotFoundException;
import com.aiincidentcommander.command_service.exception.InvalidStateTransitionException;
import com.aiincidentcommander.command_service.model.ActionStatus;
import com.aiincidentcommander.command_service.model.Incident;
import com.aiincidentcommander.command_service.model.IncidentStatus;
import com.aiincidentcommander.command_service.model.RemediationAction;
import com.aiincidentcommander.command_service.repository.IncidentRep;
import com.aiincidentcommander.command_service.repository.RemediationActionRepository;
import com.aiincidentcommander.command_service.service.DockerExecutionService;
import com.aiincidentcommander.command_service.service.IncidentService;
import com.aiincidentcommander.command_service.service.KafkaAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private IncidentRep incidentRep;
    @Mock private RemediationActionRepository actionRepository;
    @Mock private KafkaEventPublisher kafkaEventPublisher;
    @Mock private DockerExecutionService dockerExecutionService;
    @Mock private KafkaAdminService kafkaAdminService;

    @InjectMocks
    private IncidentService incidentService;

    private Incident incident;
    private RemediationAction proposedAction;

    @BeforeEach
    void setUp() {
        incident = Incident.builder()
                .id(1L)
                .serviceName("order-service")
                .severity("HIGH")
                .status(IncidentStatus.ACTION_PROPOSED)
                .build();

        proposedAction = RemediationAction.builder()
                .id(10L)
                .incidentId(1L)
                .actionType("RESTART_SERVICE")
                .status(ActionStatus.PROPOSED)
                .build();
    }

    // ---------- createIncident ----------

    @Test
    void createIncident_savesAndPublishesEvent() {
        CreateIncident request = new CreateIncident();
        request.setServiceName("order-service");
        request.setSeverity("HIGH");

        Incident saved = Incident.builder()
                .id(1L)
                .serviceName("order-service")
                .severity("HIGH")
                .status(IncidentStatus.NEW)
                .build();
        when(incidentRep.save(any(Incident.class))).thenReturn(saved);

        var response = incidentService.createIncident(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getServiceName()).isEqualTo("order-service");
        verify(incidentRep).save(any(Incident.class));
        verify(kafkaEventPublisher).publish(eq("incident.created"), eq(1L), any());
    }

    // ---------- rejectAction ----------

    @Test
    void rejectAction_happyPath_transitionsToInvestigating() {
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.of(proposedAction));

        RejectActionRequest request = new RejectActionRequest();
        request.setReason("not needed");

        RemediationActionResponse response = incidentService.rejectAction(1L, 10L, request);

        assertThat(response.getStatus()).isEqualTo(ActionStatus.REJECTED);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        verify(actionRepository).save(proposedAction);
        verify(kafkaEventPublisher).publish(anyString(), eq(1L), any());
    }

    @Test
    void rejectAction_alreadyRejected_isIdempotentAndDoesNotPublishAgain() {
        proposedAction.setStatus(ActionStatus.REJECTED);
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.of(proposedAction));

        RemediationActionResponse response =
                incidentService.rejectAction(1L, 10L, new RejectActionRequest());

        assertThat(response.getStatus()).isEqualTo(ActionStatus.REJECTED);
        verify(actionRepository, never()).save(any());
        verify(kafkaEventPublisher, never()).publish(anyString(), anyLong(), any());
    }

    @Test
    void rejectAction_wrongState_throwsInvalidStateTransition() {
        proposedAction.setStatus(ActionStatus.EXECUTED);
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.of(proposedAction));

        assertThatThrownBy(() ->
                incidentService.rejectAction(1L, 10L, new RejectActionRequest()))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(actionRepository, never()).save(any());
    }

    @Test
    void rejectAction_incidentNotFound_throws() {
        when(incidentRep.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                incidentService.rejectAction(1L, 10L, new RejectActionRequest()))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void rejectAction_actionNotFound_throws() {
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                incidentService.rejectAction(1L, 10L, new RejectActionRequest()))
                .isInstanceOf(ActionNotFoundException.class);
    }

    // ---------- approveAction ----------

    @Test
    void approveAction_happyPath_setsApprovedByAndTransitions() {
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.of(proposedAction));

        ApproveActionRequest request = new ApproveActionRequest();
        request.setApprovedBy("shrey");

        RemediationActionResponse response = incidentService.approveAction(1L, 10L, request);

        assertThat(response.getStatus()).isEqualTo(ActionStatus.APPROVED);
        assertThat(response.getApprovedBy()).isEqualTo("shrey");
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.WAITING_APPROVAL);
        verify(kafkaEventPublisher).publish(anyString(), eq(1L), any());
    }

    @Test
    void approveAction_alreadyApproved_isIdempotent() {
        proposedAction.setStatus(ActionStatus.APPROVED);
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.of(proposedAction));

        ApproveActionRequest request = new ApproveActionRequest();
        request.setApprovedBy("shrey");

        RemediationActionResponse response = incidentService.approveAction(1L, 10L, request);

        assertThat(response.getStatus()).isEqualTo(ActionStatus.APPROVED);
        verify(actionRepository, never()).save(any());
        verify(kafkaEventPublisher, never()).publish(anyString(), anyLong(), any());
    }

    @Test
    void approveAction_wrongState_throwsInvalidStateTransition() {
        proposedAction.setStatus(ActionStatus.REJECTED);
        when(incidentRep.findById(1L)).thenReturn(Optional.of(incident));
        when(actionRepository.findById(10L)).thenReturn(Optional.of(proposedAction));

        ApproveActionRequest request = new ApproveActionRequest();
        request.setApprovedBy("shrey");

        assertThatThrownBy(() -> incidentService.approveAction(1L, 10L, request))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}