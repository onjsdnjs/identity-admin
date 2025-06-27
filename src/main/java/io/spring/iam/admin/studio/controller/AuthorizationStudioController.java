package io.spring.iam.admin.studio.controller;

import io.spring.iam.admin.support.visualization.dto.GraphDataDto;
import io.spring.iam.admin.studio.dto.ExplorerItemDto;
import io.spring.iam.admin.studio.dto.InitiateGrantRequestDto;
import io.spring.iam.admin.studio.dto.SimulationRequestDto;
import io.spring.iam.admin.studio.service.StudioActionService;
import io.spring.iam.admin.studio.service.StudioExplorerService;
import io.spring.iam.admin.studio.service.StudioVisualizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/studio")
@RequiredArgsConstructor
public class AuthorizationStudioController {

    private final StudioExplorerService explorerService;
    private final StudioVisualizerService visualizerService;
    private final StudioActionService actionService;

    @GetMapping
    public String studio(Model model) {
        model.addAttribute("activePage", "studio");
        return "admin/studio";
    }

    @GetMapping("/api/subject-details")
    public ResponseEntity<Map<String, Object>> getSubjectDetails(@RequestParam Long subjectId, @RequestParam String subjectType) {
        Map<String, Object> details = visualizerService.getSubjectDetails(subjectId, subjectType);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/api/explorer-items")
    public ResponseEntity<Map<String, List<ExplorerItemDto>>> getExplorerItems() {
        return ResponseEntity.ok(explorerService.getExplorerItems());
    }

    @GetMapping("/api/access-path")
    public ResponseEntity<?> analyzeAccessPath(@RequestParam Long subjectId, @RequestParam String subjectType, @RequestParam Long permissionId) {
        return ResponseEntity.ok(visualizerService.analyzeAccessPath(subjectId, subjectType, permissionId));
    }

    @GetMapping("/api/access-path-graph")
    public ResponseEntity<GraphDataDto> analyzeAccessPathAsGraph(@RequestParam Long subjectId, @RequestParam String subjectType, @RequestParam Long permissionId) {
        return ResponseEntity.ok(visualizerService.analyzeAccessPathAsGraph(subjectId, subjectType, permissionId));
    }

    @GetMapping("/api/effective-permissions")
    public ResponseEntity<?> getEffectivePermissions(@RequestParam Long subjectId, @RequestParam String subjectType) {
        return ResponseEntity.ok(visualizerService.getEffectivePermissionsForSubject(subjectId, subjectType));
    }

    @PostMapping("/api/simulate")
    public ResponseEntity<?> runSimulation(@RequestBody SimulationRequestDto request) {
        return ResponseEntity.ok(actionService.runPolicySimulation(request));
    }

    @PostMapping("/api/initiate-grant")
    public ResponseEntity<?> initiateGrant(@RequestBody InitiateGrantRequestDto request) {
        return ResponseEntity.ok(actionService.initiateGrantWorkflow(request));
    }
}