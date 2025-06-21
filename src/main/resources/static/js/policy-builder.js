/**
 * [AI-Native 최종본] 지능형 정책 빌더 클라이언트 애플리케이션
 */

// 애플리케이션의 모든 로직을 즉시 실행 함수로 감싸 전역 스코프 오염 방지
(() => {
    // 페이지 로드 완료 시 스크립트 실행
    document.addEventListener('DOMContentLoaded', () => {

        // --- 상태 관리 (State) ---
        const state = {
            subjects: new Map(),
            permissions: new Map(),
            conditions: new Map(),
            aiRiskAssessmentEnabled: false,
            requiredTrustScore: 0.7, // 기본값 70점
            customConditionSpel: ""
        };

        // --- DOM 요소 캐싱 ---
        const elements = {
            naturalLanguageInput: document.getElementById('natural-language-input'),
            generateByAiBtn: document.getElementById('generate-by-ai-btn'),
            aiEnabledCheckbox: document.getElementById('ai-risk-assessment-enabled'),
            trustScoreContainer: document.getElementById('trust-score-slider-container'),
            trustScoreSlider: document.getElementById('required-trust-score'),
            trustScoreValueSpan: document.getElementById('trust-score-value'),
            customSpelInput: document.getElementById('custom-condition-spel'),
            subjectsPalette: document.getElementById('subjects-palette'),
            permissionsPalette: document.getElementById('permissions-palette'),
            conditionsPalette: document.getElementById('conditions-palette'),
            subjectsCanvas: document.getElementById('subjects-canvas'),
            permissionsCanvas: document.getElementById('permissions-canvas'),
            conditionsCanvas: document.getElementById('conditions-canvas'),
            policyNameInput: document.getElementById('policy-name'),
            policyDescTextarea: document.getElementById('policy-desc'),
            policyEffectSelect: document.getElementById('policy-effect'),
            savePolicyBtn: document.getElementById('save-policy-btn'),
            policyPreview: document.getElementById('policy-preview')
        };

        // CSRF 토큰 정보
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        // --- API 통신 ---
        const api = {
            async fetchApi(url, options = {}) {
                const headers = { 'Content-Type': 'application/json', [csrfHeader]: csrfToken, ...options.headers };
                try {
                    const response = await fetch(url, { ...options, headers });
                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({ message: `서버 오류 (${response.status})` }));
                        throw new Error(errorData.message);
                    }
                    return response.status === 204 ? null : response.json();
                } catch (error) {
                    showToast(error.message, 'error');
                    throw error;
                }
            },
            savePolicy(dto) {
                // TODO: 이 API 엔드포인트는 BusinessPolicyController에 신규 구현 필요
                return this.fetchApi('/api/policies/build-from-business-rule', { method: 'POST', body: JSON.stringify(dto) });
            },
            generatePolicyFromText(query) {
                return this.fetchApi('/api/ai/policies/generate-from-text', { method: 'POST', body: JSON.stringify({ naturalLanguageQuery: query }) });
            }
        };

        // --- UI 렌더링 ---
        const ui = {
            renderAll() {
                this.renderChipZone('subject', state.subjects);
                this.renderChipZone('permission', state.permissions);
                this.renderChipZone('condition', state.conditions);
                this.updatePreview();
            },
            renderChipZone(type, map) {
                const canvasEl = elements[type + 'Canvas'];
                const koreanTypeName = { subject: '주체', permission: '권한', condition: '조건' }[type];
                canvasEl.innerHTML = '';
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
            },
            updatePreview() { /* ... 이전 답변과 동일 ... */ },
            setLoading(button, isLoading) {
                if (!button) return;
                const originalHtml = button.dataset.originalHtml || button.innerHTML;
                if (isLoading) {
                    if (!button.dataset.originalHtml) button.dataset.originalHtml = originalHtml;
                    button.disabled = true;
                    button.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> 처리 중...';
                } else {
                    button.disabled = false;
                    button.innerHTML = button.dataset.originalHtml || originalHtml;
                    delete button.dataset.originalHtml;
                }
            }
        };

        // --- 이벤트 핸들러 ---
        function handleDragStart(e) {
            if (e.target.classList.contains('disabled')) {
                e.preventDefault();
                return;
            }
            e.dataTransfer.setData("text/plain", e.target.dataset.info);
            e.dataTransfer.setData("element-type", e.target.dataset.type);
        }

        function allowDrop(e) {
            e.preventDefault();
            e.currentTarget.classList.add('drag-over');
        }

        function handleDragLeave(e) {
            e.currentTarget.classList.remove('drag-over');
        }

        function handleDrop(e, type) {
            e.preventDefault();
            e.currentTarget.classList.remove('drag-over');
            const elementType = e.dataTransfer.getData("element-type");
            if (elementType !== type) return;

            const info = e.dataTransfer.getData("text/plain");
            const [id, ...nameParts] = info.split(':');
            const name = nameParts.join(':');
            const key = (type === 'subject') ? info : id;

            state[type + 's'].set(key, { id, name });
            ui.renderAll();
        }

        async function handleGenerateByAI() {
            const query = elements.naturalLanguageInput.value;
            if (!query.trim()) return showToast('요구사항을 입력해주세요.', 'error');

            ui.setLoading(elements.generateByAiBtn, true);
            try {
                const dto = await api.generatePolicyFromText(query);
                // TODO: AI DTO 응답으로 UI 상태 및 필드를 채우는 로직 구현
                showToast('AI 정책 초안 생성 완료.', 'success');
            } finally {
                ui.setLoading(elements.generateByAiBtn, false);
            }
        }

        function handleAiToggle() {
            state.aiRiskAssessmentEnabled = elements.aiEnabledCheckbox.checked;
            elements.trustScoreContainer.classList.toggle('hidden', !state.aiRiskAssessmentEnabled);
            ui.updatePreview();
        }

        function handleTrustSlider() {
            state.requiredTrustScore = elements.trustScoreSlider.value / 100.0;
            elements.trustScoreValueSpan.textContent = elements.trustScoreSlider.value;
            ui.updatePreview();
        }

        async function handleSavePolicy() {
            // 현재 UI 상태를 DTO로 변환
            state.policyName = elements.policyNameInput.value;
            state.description = elements.policyDescTextarea.value;
            state.effect = elements.policyEffectSelect.value;
            state.customConditionSpel = elements.customSpelInput.value.trim();
            const dto = state.toDto();

            if (!dto.policyName) return showToast('정책 이름은 필수입니다.', 'error');

            ui.setLoading(elements.savePolicyBtn, true);
            try {
                // BusinessPolicyServiceImpl에 DTO를 전송하는 API 호출
                const result = await api.savePolicy(dto);
                showToast(`정책 "${result.name}"이(가) 성공적으로 생성되었습니다.`, 'success');
                setTimeout(() => window.location.href = '/admin/policies', 1500);
            } finally {
                ui.setLoading(elements.savePolicyBtn, false);
            }
        }

        // --- 초기화 로직 ---
        function init() {
            // 이벤트 리스너 바인딩
            elements.generateByAiBtn.addEventListener('click', handleGenerateByAI);
            elements.aiEnabledCheckbox.addEventListener('change', handleAiToggle);
            elements.trustScoreSlider.addEventListener('input', handleTrustSlider);
            elements.savePolicyBtn.addEventListener('click', handleSavePolicy);

            document.querySelectorAll('.palette-section').forEach(p => p.addEventListener('dragstart', handleDragStart));
            document.querySelectorAll('.canvas-zone').forEach(c => {
                const type = c.id.replace('Canvas', '');
                c.addEventListener('drop', e => handleDrop(e, type));
                c.addEventListener('dragover', allowDrop);
                c.addEventListener('dragleave', handleDragLeave);
            });
            document.querySelector('.col-span-6').addEventListener('click', e => {
                if (e.target.classList.contains('remove-chip-btn')) {
                    state[e.target.dataset.type + 's'].delete(e.target.dataset.key);
                    ui.renderAll();
                }
            });

            // 컨텍스트 인지 기능 초기화
            if (window.resourceContext && window.resourceContext.availableVariables) {
                const availableVars = new Set(window.resourceContext.availableVariables);
                elements.conditionsPalette.querySelectorAll('.palette-item').forEach(item => {
                    const requiredVars = item.dataset.requiredVariables?.split(',') || [];
                    if (requiredVars.length > 0 && requiredVars[0]) {
                        const isCompatible = requiredVars.every(v => availableVars.has(v.trim()));
                        if (!isCompatible) {
                            item.classList.add('disabled');
                            item.title = '현재 리소스 컨텍스트에서는 사용할 수 없는 조건입니다.';
                        }
                    }
                });
            }
            if (window.preselectedPermission) {
                const perm = window.preselectedPermission;
                state.permissions.set(String(perm.id), { id: perm.id, name: perm.friendlyName });
            }

            ui.renderAll();
        }

        init();
    });
})();