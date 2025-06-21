/**
 * [AI-Native 최종본] 지능형 정책 빌더 클라이언트 애플리케이션
 * - 기존의 안정적인 상태/UI/API 클래스 구조를 유지
 * - AI 기반 자연어 정책 생성 기능 추가
 * - AI 실시간 리스크 평가 UI 제어 기능 추가
 * - 리소스 워크벤치로부터 전달된 컨텍스트 인지 기능 추가
 * - 드래그 앤 드롭 기능 포함
 */

/**
 * 전역 유틸리티 함수
 */
const showToast = (message, type = 'info') => {
    const container = document.getElementById('toast-container');
    if (!container) {
        console.warn('Toast container not found. Using alert.');
        alert(`[${type.toUpperCase()}] ${message}`);
        return;
    }
    // ... (toast.js의 나머지 구현)
};

const allowDrop = (ev) => {
    ev.preventDefault();
    ev.currentTarget.classList.add('drag-over');
};

const handleDragStart = (ev) => {
    ev.dataTransfer.setData("text/plain", ev.target.dataset.info);
    ev.dataTransfer.setData("element-type", ev.target.dataset.type);
};

const handleDragLeave = (ev) => {
    ev.currentTarget.classList.remove('drag-over');
};


/**
 * 1. 상태 관리 클래스 (AI 관련 필드 추가)
 */
class PolicyBuilderState {
    constructor() {
        this.subjects = new Map();
        this.permissions = new Map();
        this.conditions = new Map();

        this.policyName = '';
        this.description = '';
        this.effect = 'ALLOW';

        this.aiRiskAssessmentEnabled = false;
        this.requiredTrustScore = 0.7; // 기본값 0.7 (70점)
        this.customConditionSpel = "";
    }

    add(type, key, value) { this.getMap(type)?.set(key, value); }
    remove(type, key) { this.getMap(type)?.delete(key); }
    clear(type) { this.getMap(type)?.clear(); }
    getMap(type) {
        if (type === 'subject') return this.subjects;
        if (type === 'permission') return this.permissions;
        if (type === 'condition') return this.conditions;
        return null;
    }

    // 최종 저장 시 백엔드 DTO 형식으로 변환
    toDto() {
        // UI 필드에서 최신 값들을 state로 동기화
        this.policyName = document.getElementById('policy-name').value;
        this.description = document.getElementById('policy-desc').value;
        this.effect = document.getElementById('policy-effect').value;
        this.customConditionSpel = document.getElementById('custom-condition-spel').value.trim();

        // DTO 생성
        return {
            policyName: this.policyName,
            description: this.description,
            effect: this.effect,
            subjectUserIds: Array.from(this.subjects.keys()).filter(k => k.startsWith('USER:')).map(k => Number(k.split(':')[1])),
            subjectGroupIds: Array.from(this.subjects.keys()).filter(k => k.startsWith('GROUP:')).map(k => Number(k.split(':')[1])),
            businessResourceIds: [], // DTO 형식에 맞춤 (현재 UI에서는 Permission ID로 대체)
            businessActionIds: [],   // DTO 형식에 맞춤
            conditions: Array.from(this.conditions.entries()).reduce((acc, [key, val]) => {
                const templateId = key.split(':')[0];
                // TODO: 조건별 파라미터 입력 UI 구현 시, 여기서 파라미터 값 수집 필요
                acc[templateId] = [];
                return acc;
            }, {}),
            aiRiskAssessmentEnabled: this.aiRiskAssessmentEnabled,
            requiredTrustScore: this.requiredTrustScore,
            customConditionSpel: this.customConditionSpel
        };
    }
}

/**
 * 2. UI 렌더링 및 조작 클래스
 */
class PolicyBuilderUI {
    constructor(elements) {
        this.elements = elements;
    }

    renderAll(state) {
        this.renderChipZone(this.elements.subjectsCanvas, state.subjects, 'subject');
        this.renderChipZone(this.elements.permissionsCanvas, state.permissions, 'permission');
        this.renderChipZone(this.elements.conditionsCanvas, state.conditions, 'condition');
        this.updatePreview(state);
    }

    renderChipZone(canvasEl, map, type) {
        canvasEl.innerHTML = '';
        const koreanTypeName = { subject: '주체', permission: '권한', condition: '조건' }[type];
        if (map.size === 0) {
            canvasEl.innerHTML = `<p class="text-dark-muted text-center"><i class="fas fa-hand-pointer mr-2"></i>왼쪽에서 ${koreanTypeName}을(를) 드래그하여 여기에 놓으세요</p>`;
            return;
        }
        map.forEach((value, key) => {
            const chip = document.createElement('span');
            chip.className = 'policy-chip';
            chip.dataset.key = key;
            chip.innerHTML = `${value.name} <button class="remove-chip-btn" data-type="${type}" data-key="${key}">&times;</button>`;
            canvasEl.appendChild(chip);
        });
    }

    updatePreview(state) {
        const subjectsHtml = Array.from(state.subjects.values()).map(s => `<span class="policy-chip-preview">${s.name}</span>`).join(' 또는 ') || '<span class="text-gray-400">모든 주체</span>';
        const permissionsHtml = Array.from(state.permissions.values()).map(p => `<span class="policy-chip-preview">${p.name}</span>`).join(' 그리고 ') || '<span class="text-gray-400">모든 권한</span>';
        const conditionsHtml = Array.from(state.conditions.values()).map(c => `<span class="policy-chip-preview condition">${c.name}</span>`).join(' 그리고 ');
        const aiConditionHtml = state.aiRiskAssessmentEnabled ? `<span class="policy-chip-preview ai">AI 신뢰도 ${state.requiredTrustScore * 100}점 이상</span>` : '';

        let fullConditionHtml = [conditionsHtml, aiConditionHtml].filter(Boolean).join(' 그리고 ');
        if (fullConditionHtml) {
            fullConditionHtml = `<div class="flex items-center gap-2 mt-2"><span class="font-bold text-gray-300 w-16">조건:</span><div class="flex flex-wrap gap-1">${fullConditionHtml}</div></div>`;
        }

        const effect = this.elements.policyEffectSelect.value;
        const effectHtml = `<span class="font-bold ${effect === 'ALLOW' ? 'text-green-400' : 'text-red-400'}">${effect}</span>`;

        this.elements.policyPreview.innerHTML = `
            <div class="flex items-center gap-2"><span class="font-bold text-gray-300 w-16">주체:</span><div class="flex flex-wrap gap-1">${subjectsHtml}</div></div>
            <div class="flex items-center gap-2 mt-2"><span class="font-bold text-gray-300 w-16">권한:</span><div class="flex flex-wrap gap-1">${permissionsHtml}</div></div>
            ${fullConditionHtml}
            <div class="flex items-center gap-2 mt-2"><span class="font-bold text-gray-300 w-16">결과:</span><div>${effectHtml}</div></div>
        `;
    }

    setLoading(button, isLoading) {
        if (!button) return;
        const originalHtml = button.dataset.originalHtml || button.innerHTML;
        if (isLoading) {
            if (!button.dataset.originalHtml) {
                button.dataset.originalHtml = originalHtml;
            }
            button.disabled = true;
            button.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> 처리 중...';
        } else {
            button.disabled = false;
            button.innerHTML = originalHtml;
            delete button.dataset.originalHtml;
        }
    }
}


/**
 * 4. 메인 애플리케이션 클래스
 */
class PolicyBuilderApp {
    constructor() {
        this.state = new PolicyBuilderState();
        this.elements = this.queryDOMElements();
        this.ui = new PolicyBuilderUI(this.elements);
        this.api = new PolicyBuilderAPI(); // API 클래스는 필요 시 사용
        this.init();
    }

    queryDOMElements() {
        const ids = [
            'naturalLanguageInput', 'generateByAiBtn', 'aiEnabledCheckbox',
            'trustScoreContainer', 'trustScoreSlider', 'trustScoreValueSpan',
            'customSpelInput', 'subjectsPalette', 'permissionsPalette', 'conditionsPalette',
            'subjectsCanvas', 'permissionsCanvas', 'conditionsCanvas', 'policyNameInput',
            'policyDescTextarea', 'policyEffectSelect', 'savePolicyBtn', 'policyPreview'
        ];
        const elements = {};
        ids.forEach(id => elements[id] = document.getElementById(id));
        return elements;
    }

    init() {
        if (!this.elements.savePolicyBtn) {
            console.error("정책 빌더의 필수 UI 요소가 없습니다. HTML 구조를 확인하세요.");
            return;
        }
        this.bindEventListeners();
        this.initializeFromContext();
        this.ui.renderAll(this.state);
    }

    bindEventListeners() {
        // AI 기능 이벤트
        this.elements.generateByAiBtn?.addEventListener('click', () => this.handleGenerateByAI());
        this.elements.aiEnabledCheckbox?.addEventListener('change', () => this.handleAiToggle());
        this.elements.trustScoreSlider?.addEventListener('input', () => this.handleTrustSlider());

        // 드래그 앤 드롭
        this.elements.subjectsPalette?.addEventListener('dragstart', handleDragStart);
        this.elements.permissionsPalette?.addEventListener('dragstart', handleDragStart);
        this.elements.conditionsPalette?.addEventListener('dragstart', handleDragStart);

        ['subjectsCanvas', 'permissionsCanvas', 'conditionsCanvas'].forEach(id => {
            const canvas = this.elements[id];
            if(canvas) {
                const type = id.replace('Canvas', '');
                canvas.addEventListener('drop', (e) => this.handleDrop(e, type));
            }
        });

        // 칩 제거 (이벤트 위임)
        document.querySelector('.col-span-6')?.addEventListener('click', (e) => {
            if (e.target.classList.contains('remove-chip-btn')) {
                this.state.remove(e.target.dataset.type, e.target.dataset.key);
                this.ui.renderAll(this.state);
            }
        });

        // 저장 버튼
        this.elements.savePolicyBtn.addEventListener('click', () => this.handleSavePolicy());
    }

    initializeFromContext() {
        if (window.resourceContext && window.resourceContext.availableVariables) {
            const availableVars = new Set(window.resourceContext.availableVariables);
            this.elements.conditionsPalette.querySelectorAll('.palette-item').forEach(item => {
                const requiredVars = item.dataset.requiredVariables?.split(',') || [];
                const isCompatible = requiredVars.every(v => v && availableVars.has(v.trim()));
                if (requiredVars.length > 0 && !isCompatible) {
                    item.classList.add('disabled', 'opacity-50', 'cursor-not-allowed');
                    item.draggable = false;
                    item.title = "이 조건은 현재 선택된 리소스에서 사용할 수 없습니다.";
                }
            });
        }
        if (window.preselectedPermission) {
            const perm = window.preselectedPermission;
            this.state.add('permission', String(perm.id), { id: perm.id, name: perm.friendlyName });
        }
    }

    handleDrop(ev, type) {
        ev.preventDefault();
        ev.currentTarget.classList.remove('drag-over');
        const elementType = ev.dataTransfer.getData("element-type");
        if(elementType !== type) return;

        const info = ev.dataTransfer.getData("text/plain");
        const [id, ...nameParts] = info.split(':');
        const name = nameParts.join(':');
        const key = (type === 'subject') ? info : id;

        this.state.add(type, key, { id, name });
        this.ui.renderAll(this.state);
    }

    async handleGenerateByAI() {
        const query = this.elements.naturalLanguageInput.value;
        if (!query.trim()) return showToast('요구사항을 입력해주세요.', 'error');

        this.ui.setLoading(this.elements.generateByAiBtn, true);
        try {
            // API 클래스 인스턴스 사용
            const policyDto = await this.api.generatePolicyFromText(query);
            // TODO: AI DTO를 기반으로 UI 상태를 채우는 로직 구현
            showToast('AI 정책 초안 생성 완료.', 'success');
        } finally {
            this.ui.setLoading(this.elements.generateByAiBtn, false);
        }
    }

    handleAiToggle() {
        this.state.aiRiskAssessmentEnabled = this.elements.aiEnabledCheckbox.checked;
        this.elements.trustScoreContainer.classList.toggle('hidden', !this.state.aiRiskAssessmentEnabled);
        this.ui.updatePreview(this.state);
    }

    handleTrustSlider() {
        this.state.requiredTrustScore = this.elements.trustScoreSlider.value / 100.0;
        this.elements.trustScoreValueSpan.textContent = this.elements.trustScoreSlider.value;
        this.ui.updatePreview(this.state);
    }

    async handleSavePolicy() {
        const dto = this.state.toDto();
        if (!dto.policyName) return showToast('정책 이름은 필수입니다.', 'error');

        this.ui.setLoading(this.elements.savePolicyBtn, true);
        try {
            // TODO: DTO를 백엔드로 전송하는 API 호출 구현
            // await this.api.savePolicy(dto);
            console.log("Saving DTO:", dto);
            showToast('정책 저장을 요청했습니다. (구현 필요)', 'info');
        } finally {
            this.ui.setLoading(this.elements.savePolicyBtn, false);
        }
    }
}

// 애플리케이션 인스턴스 생성
document.addEventListener('DOMContentLoaded', () => {
    new PolicyBuilderApp();
});