package io.spring.identityadmin.admin.studio.service;

import io.spring.identityadmin.admin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.admin.studio.dto.SimulationRequestDto;
import io.spring.identityadmin.admin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.admin.workflow.wizard.dto.WizardContext;

public interface StudioActionService {
    SimulationResultDto runPolicySimulation(SimulationRequestDto simulationRequest);
    WizardContext initiateGrantWorkflow(InitiateGrantRequestDto grantRequest);
}