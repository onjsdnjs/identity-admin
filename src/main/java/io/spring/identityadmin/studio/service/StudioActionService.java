package io.spring.identityadmin.studio.service;

import io.spring.identityadmin.studio.dto.InitiateGrantRequestDto;
import io.spring.identityadmin.studio.dto.SimulationRequestDto;
import io.spring.identityadmin.studio.dto.SimulationResultDto;
import io.spring.identityadmin.studio.dto.WizardInitiationDto;
import io.spring.identityadmin.workflow.wizard.dto.WizardContext;

public interface StudioActionService {
    SimulationResultDto runPolicySimulation(SimulationRequestDto simulationRequest);
    WizardContext initiateGrantWorkflow(InitiateGrantRequestDto grantRequest);
}