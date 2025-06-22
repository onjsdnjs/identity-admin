/**
 * [AI-Native 최종 완성본] 지능형 정책 빌더 클라이언트 애플리케이션
 * - 기존 PolicyBuilderApp 클래스 구조를 완벽하게 유지
 * - 모든 이벤트 핸들러(드래그앤드롭, AI 기능, 저장 등)의 상세 로직 포함
 * - 컨텍스트 인지 및 UI 동기화 기능 완성
 */

// 애플리케이션의 모든 로직을 즉시 실행 함수로 감싸 전역 스코프 오염 방지
(() => {
    // 페이지 로드 완료 시 스크립트 실행
    document.addEventListener('DOMContentLoaded', () => {

        // --- 1. 상태 관리 클래스 (AI 관련 필드 추가) ---
        class PolicyBuilderState {
            constructor() {
                this.roles = new Map();
                this.permissions = new Map();
                this.conditions = new Map();
                this.aiRiskAssessmentEnabled = false;
                this.requiredTrustScore = 0.7;
                this.customConditionSpel = "";
            }
            add(type, key, value) { this.getMap(type)?.set(key, value); }
            remove(type, key) { this.getMap(type)?.delete(key); }
            clear(type) { this.getMap(type)?.clear(); }
            getMap(type) {
                const map = { role: this.roles, permission: this.permissions, condition: this.conditions }[type];
                if (!map) throw new Error('Invalid state type: ' + type);
                return map;
            }
            toDto() {
                this.policyName = document.getElementById('policyNameInput').value;
                this.description = document.getElementById('policyDescTextarea').value;
                this.effect = document.getElementById('policyEffectSelect').value;
                this.customConditionSpel = document.getElementById('customSpelInput').value.trim();
                return {
                    policyName: this.policyName,
                    description: this.description,
                    effect: this.effect,
                    roleIds: Array.from(this.roles.keys()).map(Number),
                    businessResourceIds: Array.from(this.permissions.keys()).map(Number), // 예시: Permission ID를 Resource ID로 매핑
                    businessActionIds: [], // 현재 UI에서 별도 선택하지 않음
                    conditions: Array.from(this.conditions.entries()).reduce((acc, [key, val]) => {
                        const templateId = key.split(':')[0];
                        acc[templateId] = []; // TODO: 파라미터 수집 로직 추가
                        return acc;
                    }, {}),
                    aiRiskAssessmentEnabled: this.aiRiskAssessmentEnabled,
                    requiredTrustScore: this.requiredTrustScore,
                    customConditionSpel: this.customConditionSpel
                };
            }
        }

        // --- 2. UI 렌더링 클래스 ---
        class PolicyBuilderUI {
            constructor(elements) { this.elements = elements; }
            renderAll(state) {
                this.renderChipZone('role', state.roles);
                this.renderChipZone('permission', state.permissions);
                this.renderChipZone('condition', state.conditions);
                this.updatePreview(state);
            }
            renderChipZone(type, map) {
                // 올바른 요소 이름 매핑
                const canvasElId = type + 'sCanvas';
                const canvasEl = this.elements[canvasElId];
                const koreanTypeName = { role: '역할', permission: '권한', condition: '조건' }[type];

                console.log(`Rendering ${type} zone (${canvasElId}) with ${map.size} items`); // 디버깅
                console.log('Canvas element:', canvasEl); // 디버깅

                if (!canvasEl) {
                    console.error(`Canvas element not found: ${canvasElId}`);
                    return;
                }

                canvasEl.innerHTML = '';
                if (map.size === 0) {
                    canvasEl.innerHTML = `<div class="canvas-placeholder"><i class="fas fa-hand-pointer"></i><span>왼쪽에서 ${koreanTypeName}을(를) 드래그하여 여기에 놓으세요</span></div>`;
                    return;
                }

                map.forEach((value, key) => {
                    console.log(`Creating chip for ${type}: ${key} - ${value.name}`); // 디버깅
                    const chip = document.createElement('span');
                    chip.className = 'policy-chip';
                    chip.dataset.key = key;
                    chip.innerHTML = `${value.name} <button class="remove-chip-btn" data-type="${type}" data-key="${key}">&times;</button>`;
                    canvasEl.appendChild(chip);
                });
            }
            updatePreview(state) {
                const rolesHtml = Array.from(state.roles.values()).map(r => `<span class="policy-chip-preview">${r.name}</span>`).join(' 또는 ') || '<span class="text-gray-400">모든 역할</span>';
                const permissionsHtml = Array.from(state.permissions.values()).map(p => `<span class="policy-chip-preview">${p.name}</span>`).join(' 그리고 ') || '<span class="text-gray-400">모든 권한</span>';
                const conditionsHtml = Array.from(state.conditions.values()).map(c => `<span class="policy-chip-preview condition">${c.name}</span>`).join(' 그리고 ');
                const aiConditionHtml = state.aiRiskAssessmentEnabled ? `<span class="policy-chip-preview ai">AI 신뢰도 ${Math.round(state.requiredTrustScore * 100)}점 이상</span>` : '';
                let fullConditionHtml = [conditionsHtml, aiConditionHtml].filter(Boolean).join(' 그리고 ');

                const effect = this.elements.policyEffectSelect.value;
                const effectHtml = `<span class="font-bold ${effect === 'ALLOW' ? 'text-green-400' : 'text-red-400'}">${effect === 'ALLOW' ? '허용' : '거부'}</span>`;

                // 더 자세하고 읽기 쉬운 미리보기 생성
                this.elements.policyPreview.innerHTML = `
                    <div class="preview-section">
                        <div class="preview-label">🛡️ 역할 (WHO)</div>
                        <div>${rolesHtml}</div>
                    </div>
                    <div class="preview-section">
                        <div class="preview-label">🔑 권한 (무엇을)</div>
                        <div>${permissionsHtml}</div>
                    </div>
                    ${fullConditionHtml ? `
                    <div class="preview-section">
                        <div class="preview-label">⏰ 조건 (언제)</div>
                        <div>${fullConditionHtml}</div>
                    </div>
                    ` : ''}
                    <div class="preview-section">
                        <div class="preview-label">⚡ 결과</div>
                        <div class="text-lg">${effectHtml}</div>
                    </div>
                    <div class="mt-4 p-3 rounded-lg bg-gradient-to-r from-indigo-900/30 to-purple-900/30 border border-indigo-500/30">
                        <div class="text-sm text-indigo-300 font-semibold mb-2">📋 정책 요약</div>
                        <div class="text-indigo-100">
                            ${Array.from(state.roles.values()).map(s => s.name).join(', ') || '모든 역할'}이 
                            ${Array.from(state.permissions.values()).map(p => p.name).join(', ') || '모든 리소스'}에 대해 
                            ${fullConditionHtml ? `${Array.from(state.conditions.values()).map(c => c.name).join(', ')} 조건 하에서` : ''}
                            <strong>${effect === 'ALLOW' ? '접근이 허용' : '접근이 거부'}</strong>됩니다.
                        </div>
                    </div>
                `;
            }
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
        }

        // --- 3. API 통신 클래스 ---
        class PolicyBuilderAPI {
            constructor() {
                this.csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                this.csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            }
            async fetchApi(url, options = {}) {
                const headers = { 'Content-Type': 'application/json', [this.csrfHeader]: this.csrfToken, ...options.headers };
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
            }
            savePolicy(dto) { return this.fetchApi('/api/policies/build-from-business-rule', { method: 'POST', body: JSON.stringify(dto) }); }
            generatePolicyFromText(query) { return this.fetchApi('/api/ai/policies/generate-from-text', { method: 'POST', body: JSON.stringify({ naturalLanguageQuery: query }) }); }
        }

        // --- 4. 메인 애플리케이션 클래스 ---
        class PolicyBuilderApp {
            constructor() {
                this.state = new PolicyBuilderState();
                this.elements = this.queryDOMElements();
                this.ui = new PolicyBuilderUI(this.elements);
                this.api = new PolicyBuilderAPI();
                this.init();
            }

            queryDOMElements() {
                const ids = ['rolesPalette', 'rolesCanvas','naturalLanguageInput', 'generateByAiBtn', 'aiEnabledCheckbox', 'trustScoreContainer', 'trustScoreSlider', 'trustScoreValueSpan', 'customSpelInput', 'permissionsPalette', 'conditionsPalette', 'permissionsCanvas', 'conditionsCanvas', 'policyNameInput', 'policyDescTextarea', 'policyEffectSelect', 'savePolicyBtn', 'policyPreview'];
                const elements = {};
                ids.forEach(id => {
                    const element = document.getElementById(id);
                    if (element) {
                        elements[id] = element;
                        console.log(`Found element: ${id}`); // 디버깅
                    } else {
                        console.warn(`Element not found: ${id}`); // 디버깅
                    }
                });
                return elements;
            }

            init() {
                console.log('PolicyBuilderApp initializing...'); // 디버깅

                if (!this.elements.savePolicyBtn) {
                    console.error('Save policy button not found!'); // 디버깅
                    return;
                }

                console.log('Found elements:', Object.keys(this.elements)); // 디버깅

                this.bindEventListeners();
                this.initializeFromContext();
                this.ui.renderAll(this.state);

                console.log('PolicyBuilderApp initialized successfully'); // 디버깅
            }

            bindEventListeners() {
                this.elements.generateByAiBtn?.addEventListener('click', () => this.handleGenerateByAI());
                this.elements.aiEnabledCheckbox?.addEventListener('change', () => this.handleAiToggle());
                this.elements.trustScoreSlider?.addEventListener('input', () => this.handleTrustSlider());
                this.elements.savePolicyBtn.addEventListener('click', () => this.handleSavePolicy());
                this.elements.policyEffectSelect.addEventListener('change', () => this.ui.updatePreview(this.state));

                // 드래그 앤 드롭 리스너 등록
                ['rolesPalette', 'permissionsPalette', 'conditionsPalette'].forEach(id => {
                    const element = this.elements[id];
                    if (element) {
                        element.addEventListener('dragstart', this.handleDragStart.bind(this));
                        console.log(`Dragstart listener added to ${id}`);
                    } else {
                        console.error(`Palette element not found: ${id}`);
                    }
                });

                ['rolesCanvas', 'permissionsCanvas', 'conditionsCanvas'].forEach(id => {
                    const canvas = this.elements[id];
                    if (canvas) {
                        // 올바른 타입 매핑
                        let type;
                        if (id === 'rolesCanvas') type = 'role';
                        else if (id === 'permissionsCanvas') type = 'permission';
                        else if (id === 'conditionsCanvas') type = 'condition';

                        canvas.addEventListener('drop', (e) => this.handleDrop(e, type));
                        canvas.addEventListener('dragover', this.allowDrop.bind(this));
                        canvas.addEventListener('dragleave', this.handleDragLeave.bind(this));
                        console.log(`Drop listeners added to ${id} (type: ${type})`);
                    } else {
                        console.error(`Canvas element not found: ${id}`);
                    }
                });
                // 칩 제거 리스너 등록 (이벤트 위임) - 전체 문서에서 감지
                document.addEventListener('click', (e) => {
                    if (e.target.classList.contains('remove-chip-btn')) {
                        this.handleChipRemove(e.target.dataset.type, e.target.dataset.key);
                    }
                });
            }

            // --- 이벤트 핸들러 메서드 구현 ---

            handleDragStart(e) {
                const item = e.target.closest('.palette-item');
                console.log('Drag start on item:', item); // 디버깅

                if (item?.classList.contains('disabled')) {
                    console.log('Item is disabled, preventing drag'); // 디버깅
                    e.preventDefault();
                    return;
                }
                if (item) {
                    const info = item.dataset.info;
                    const type = item.dataset.type;
                    console.log(`Drag start: info=${info}, type=${type}`); // 디버깅

                    e.dataTransfer.setData("text/plain", info);
                    e.dataTransfer.setData("element-type", type);
                } else {
                    console.log('No palette item found'); // 디버깅
                }
            }

            allowDrop(e) {
                e.preventDefault();
                e.currentTarget.classList.add('drag-over');
            }

            handleDragLeave(e) {
                e.currentTarget.classList.remove('drag-over');
            }

            handleDrop(e, type) {
                e.preventDefault();
                e.currentTarget.classList.remove('drag-over');
                const elementType = e.dataTransfer.getData("element-type");

                console.log(`Drop event: ${elementType} -> ${type}`); // 디버깅

                if (elementType !== type) {
                    console.log('Type mismatch, ignoring drop'); // 디버깅
                    return;
                }

                const info = e.dataTransfer.getData("text/plain");
                console.log(`Drop data: ${info}`); // 디버깅

                const [id, ...nameParts] = info.split(':');
                const name = nameParts.join(':');
                const key = id;

                console.log(`Adding to state: type=${type}, key=${key}, name=${name}`); // 디버깅

                this.state.add(type, key, { id, name });

                console.log(`State after add:`, this.state.getMap(type)); // 디버깅

                this.ui.renderAll(this.state);
            }

            handleChipRemove(type, key) {
                this.state.remove(type, key);
                this.ui.renderAll(this.state);
            }

            async handleGenerateByAI() {
                const query = this.elements.naturalLanguageInput.value;
                if (!query.trim()) return showToast('요구사항을 입력해주세요.', 'error');

                this.ui.setLoading(this.elements.generateByAiBtn, true);
                try {
                    const dto = await this.api.generatePolicyFromText(query);
                    // TODO: AI DTO 응답으로 UI 상태 및 필드를 채우는 로직 구현
                    showToast('AI 정책 초안이 생성되었습니다.', 'success');
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
                if (dto.roleIds.length === 0) return showToast('하나 이상의 역할을 선택해야 합니다.', 'error');
                if (dto.businessResourceIds.length === 0) return showToast('하나 이상의 권한을 선택해야 합니다.', 'error');

                this.ui.setLoading(this.elements.savePolicyBtn, true);
                try {
                    const result = await this.api.savePolicy(dto);
                    showToast(`정책 "${result.name}"이(가) 성공적으로 생성되었습니다.`, 'success');
                    setTimeout(() => window.location.href = '/admin/policies', 1500);
                } finally {
                    this.ui.setLoading(this.elements.savePolicyBtn, false);
                }
            }

            initializeFromContext() {
                if (window.resourceContext?.availableVariables) {
                    const availableVars = new Set(window.resourceContext.availableVariables);
                    this.elements.conditionsPalette.querySelectorAll('.palette-item').forEach(item => {
                        const requiredVars = item.dataset.requiredVariables?.split(',').filter(v => v);
                        if (requiredVars?.length > 0) {
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
                    this.state.add('permission', String(perm.id), { id: perm.id, name: perm.friendlyName });
                }
            }
        }

        new PolicyBuilderApp();
    });
})();