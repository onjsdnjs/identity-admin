package io.spring.iam.admin.studio.service;

import io.spring.iam.admin.studio.dto.InitiateGrantRequestDto;
import io.spring.iam.admin.studio.dto.SimulationRequestDto;
import io.spring.iam.admin.studio.dto.SimulationResultDto;
import io.spring.iam.admin.workflow.wizard.dto.WizardContext;

public interface StudioActionService {
    SimulationResultDto runPolicySimulation(SimulationRequestDto simulationRequest);
    WizardContext initiateGrantWorkflow(InitiateGrantRequestDto grantRequest);
}