/**
 * [완벽한 스트리밍 시스템] 지능형 정책 빌더 클라이언트 애플리케이션
 * - 한글 마커 지원 (===JSON시작===, ===JSON끝===)
 * - 자연스러운 한국어 AI 응답 처리
 * - 향상된 키워드 매핑 로직
 */

(() => {
    console.log('🌟 policy-builder.js 스크립트 로드됨');

    document.addEventListener('DOMContentLoaded', () => {
        console.log('🌟 DOMContentLoaded 이벤트 발생 - PolicyBuilderApp 초기화 시작');

        try {
            // --- 1. 상태 관리 클래스 ---
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
                    if (!map) throw new Error('유효하지 않은 상태 타입입니다: ' + type);
                    return map;
                }

                toDto() {
                    const policyNameEl = document.getElementById('policyNameInput');
                    const policyDescEl = document.getElementById('policyDescTextarea');
                    const policyEffectEl = document.getElementById('policyEffectSelect');
                    const customSpelEl = document.getElementById('customSpelInput');

                    return {
                        policyName: policyNameEl?.value || '',
                        description: policyDescEl?.value || '',
                        effect: policyEffectEl?.value || 'ALLOW',
                        roleIds: Array.from(this.roles.keys()).map(Number),
                        permissionIds: Array.from(this.permissions.keys()).map(Number),
                        conditions: Array.from(this.conditions.entries()).reduce((acc, [key, val]) => {
                            const templateId = key.split(':')[0];
                            acc[templateId] = [];
                            return acc;
                        }, {}),
                        aiRiskAssessmentEnabled: this.aiRiskAssessmentEnabled,
                        requiredTrustScore: this.requiredTrustScore,
                        customConditionSpel: customSpelEl?.value?.trim() || ''
                    };
                }
            }

            // --- 2. UI 렌더링 클래스 ---
            class PolicyBuilderUI {
                constructor(elements) {
                    this.elements = elements;
                }

                renderAll(state) {
                    this.renderChipZone('role', state.roles);
                    this.renderChipZone('permission', state.permissions);
                    this.renderChipZone('condition', state.conditions);
                    this.updatePreview(state);
                }

                // PolicyBuilderUI 클래스의 renderChipZone 메서드 수정
                renderChipZone(type, map) {
                    const canvasElId = type + 'sCanvas';
                    const canvasEl = this.elements[canvasElId];
                    const koreanTypeName = { role: '역할', permission: '권한', condition: '조건' }[type];

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
                        const chip = document.createElement('span');
                        chip.className = 'policy-chip';
                        chip.dataset.key = key;
                        chip.dataset.type = type;

                        const removeBtn = document.createElement('button');
                        removeBtn.className = 'remove-chip-btn';
                        removeBtn.innerHTML = '&times;';
                        removeBtn.dataset.type = type;
                        removeBtn.dataset.key = key;

                        // 버튼에 직접 이벤트 리스너 추가
                        removeBtn.addEventListener('click', () => {
                            // PolicyBuilderApp 인스턴스에 접근하기 위해 이벤트 발생
                            const event = new CustomEvent('removeChip', {
                                detail: { type, key }
                            });
                            document.dispatchEvent(event);
                        });

                        // 조건 칩의 경우, 유효성 검증 결과에 따라 아이콘 추가
                        if (type === 'condition' && value.isValidated) {
                            const iconClass = value.isCompatible ? 'fa-check-circle text-green-500' : 'fa-exclamation-triangle text-red-500';
                            const icon = document.createElement('i');
                            icon.className = `fas ${iconClass} ml-2`;

                            chip.appendChild(document.createTextNode(value.name + ' '));
                            chip.appendChild(icon);
                            chip.appendChild(document.createTextNode(' '));
                            chip.appendChild(removeBtn);

                            if (!value.isCompatible) {
                                chip.title = value.reason;
                                chip.classList.add('invalid-chip');
                            }
                        } else {
                            chip.appendChild(document.createTextNode(value.name + ' '));
                            chip.appendChild(removeBtn);
                        }

                        canvasEl.appendChild(chip);
                    });
                }

                updatePreview(state) {
                    if (!this.elements.policyPreview) return;

                    const rolesHtml = Array.from(state.roles.values()).map(r => `<span class="policy-chip-preview">${r.name}</span>`).join(' 또는 ') || '<span class="text-gray-400">모든 역할</span>';
                    const permissionsHtml = Array.from(state.permissions.values()).map(p => `<span class="policy-chip-preview">${p.name}</span>`).join(' 그리고 ') || '<span class="text-gray-400">모든 권한</span>';
                    const conditionsHtml = Array.from(state.conditions.values()).map(c => `<span class="policy-chip-preview condition">${c.name}</span>`).join(' 그리고 ');
                    const aiConditionHtml = state.aiRiskAssessmentEnabled ? `<span class="policy-chip-preview ai">AI 신뢰도 ${Math.round(state.requiredTrustScore * 100)}점 이상</span>` : '';
                    let fullConditionHtml = [conditionsHtml, aiConditionHtml].filter(Boolean).join(' 그리고 ');

                    const effect = this.elements.policyEffectSelect?.value || 'ALLOW';
                    const effectHtml = `<span class="font-bold ${effect === 'ALLOW' ? 'text-green-400' : 'text-red-400'}">${effect === 'ALLOW' ? '허용' : '거부'}</span>`;

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
                    const headers = {
                        'Content-Type': 'application/json',
                        ...(this.csrfToken && this.csrfHeader ? { [this.csrfHeader]: this.csrfToken } : {}),
                        ...options.headers
                    };
                    try {
                        const response = await fetch(url, { ...options, headers });
                        if (!response.ok) {
                            const errorData = await response.json().catch(() => ({ message: `서버 오류 (${response.status})` }));
                            throw new Error(errorData.message);
                        }
                        return response.status === 204 ? null : response.json();
                    } catch (error) {
                        if (typeof showToast === 'function') {
                            showToast(error.message, 'error');
                        } else {
                            console.error('Error:', error.message);
                        }
                        throw error;
                    }
                }

                async validateCondition(resourceIdentifier, conditionSpel) {
                    return this.fetchApi('/api/ai/policies/validate-condition', {
                        method: 'POST',
                        body: JSON.stringify({ resourceIdentifier, conditionSpel })
                    });
                }

                savePolicy(dto) {
                    return this.fetchApi('/api/policies/build-from-business-rule', {
                        method: 'POST',
                        body: JSON.stringify(dto)
                    });
                }

                async generatePolicyFromText(query) {
                    return this.fetchApi('/api/ai/policies/generate-from-text', {
                        method: 'POST',
                        body: JSON.stringify({ naturalLanguageQuery: query })
                    });
                }

                async generatePolicyFromTextStream(query) {
                    const headers = {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream',
                        'Cache-Control': 'no-cache'
                    };

                    if (this.csrfToken && this.csrfHeader) {
                        headers[this.csrfHeader] = this.csrfToken;
                    }

                    return fetch('/api/ai/policies/generate-from-text/stream', {
                        method: 'POST',
                        headers: headers,
                        body: JSON.stringify({ naturalLanguageQuery: query })
                    });
                }
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
                    const elements = {};
                    const idMapping = {
                        // AI 기능 UI
                        naturalLanguageInput: 'naturalLanguageInput',
                        generateByAiBtn: 'generateByAiBtn',
                        thoughtProcessContainer: 'ai-thought-process-container',
                        thoughtProcessLog: 'ai-thought-process',
                        aiEnabledCheckbox: 'aiEnabledCheckbox',
                        trustScoreContainer: 'trustScoreContainer',
                        trustScoreSlider: 'trustScoreSlider',
                        trustScoreValueSpan: 'trustScoreValueSpan',
                        customSpelInput: 'customSpelInput',
                        // 팔레트
                        rolesPalette: 'roles-palette',
                        permissionsPalette: 'permissionsPalette',
                        conditionsPalette: 'conditionsPalette',
                        // 캔버스
                        rolesCanvas: 'roles-canvas',
                        permissionsCanvas: 'permissions-canvas',
                        conditionsCanvas: 'conditions-canvas',
                        // 속성 및 저장
                        policyNameInput: 'policyNameInput',
                        policyDescTextarea: 'policyDescTextarea',
                        policyEffectSelect: 'policyEffectSelect',
                        savePolicyBtn: 'savePolicyBtn',
                        policyPreview: 'policyPreview'
                    };

                    for (const [jsKey, htmlId] of Object.entries(idMapping)) {
                        elements[jsKey] = document.getElementById(htmlId);
                        if (!elements[jsKey]) {
                            console.warn(`Element not found: ${htmlId} (mapped to ${jsKey})`);
                        }
                    }
                    return elements;
                }

                init() {
                    console.log('=== PolicyBuilderApp init 시작 ===');

                    if (!this.elements.savePolicyBtn) {
                        console.error("❌ 정책 빌더의 필수 UI 요소(저장 버튼)를 찾을 수 없습니다.");
                        return;
                    }

                    this.bindEventListeners();
                    this.initializeFromContext();
                    this.ui.renderAll(this.state);

                    console.log('=== PolicyBuilderApp init 완료 ===');
                }

                bindEventListeners() {
                    // AI 기능 이벤트 리스너
                    if (this.elements.generateByAiBtn) {
                        this.elements.generateByAiBtn.addEventListener('click', (e) => {
                            e.preventDefault();
                            this.handleGenerateByAI();
                        });
                        console.log('✅ AI 생성 버튼 이벤트 리스너 추가 완료');
                    }

                    if (this.elements.aiEnabledCheckbox) {
                        this.elements.aiEnabledCheckbox.addEventListener('change', () => this.handleAiToggle());
                    }

                    if (this.elements.trustScoreSlider) {
                        this.elements.trustScoreSlider.addEventListener('input', () => this.handleTrustSlider());
                    }

                    if (this.elements.savePolicyBtn) {
                        this.elements.savePolicyBtn.addEventListener('click', () => this.handleSavePolicy());
                    }

                    if (this.elements.policyEffectSelect) {
                        this.elements.policyEffectSelect.addEventListener('change', () => this.ui.updatePreview(this.state));
                    }

                    // 드래그 앤 드롭 리스너 등록
                    ['rolesPalette', 'permissionsPalette', 'conditionsPalette'].forEach(jsKey => {
                        const element = this.elements[jsKey];
                        if (element) {
                            element.addEventListener('dragstart', this.handleDragStart.bind(this));
                        }
                    });

                    ['rolesCanvas', 'permissionsCanvas', 'conditionsCanvas'].forEach(jsKey => {
                        const canvas = this.elements[jsKey];
                        if (canvas) {
                            let type;
                            if (jsKey === 'rolesCanvas') type = 'role';
                            else if (jsKey === 'permissionsCanvas') type = 'permission';
                            else if (jsKey === 'conditionsCanvas') type = 'condition';

                            canvas.addEventListener('drop', async (e) => this.handleDrop(e, type));
                            canvas.addEventListener('dragover', this.allowDrop.bind(this));
                            canvas.addEventListener('dragleave', this.handleDragLeave.bind(this));
                        }
                    });

                    // 칩 제거 리스너
                    document.addEventListener('click', (e) => {
                        if (e.target.classList.contains('remove-chip-btn')) {
                            this.handleChipRemove(e.target.dataset.type, e.target.dataset.key);
                        }
                    });

                    document.addEventListener('removeChip', (e) => {
                        const { type, key } = e.detail;
                        this.handleChipRemove(type, key);
                    });
                }

                // 드래그 앤 드롭 이벤트 핸들러
                handleDragStart(e) {
                    const item = e.target.closest('.palette-item');
                    if (item?.classList.contains('disabled')) {
                        e.preventDefault();
                        return;
                    }
                    if (item) {
                        const info = item.dataset.info;
                        const type = item.dataset.type;
                        e.dataTransfer.setData("text/plain", info);
                        e.dataTransfer.setData("element-type", type);
                    }
                }

                allowDrop(e) {
                    e.preventDefault();
                    e.currentTarget.classList.add('drag-over');
                }

                handleDragLeave(e) {
                    e.currentTarget.classList.remove('drag-over');
                }

                // 드래그 앤 드롭 이벤트 핸들러
                handleDragStart(e) {
                    const item = e.target.closest('.palette-item');
                    if (item?.classList.contains('disabled')) {
                        e.preventDefault();
                        return;
                    }
                    if (item) {
                        const info = item.dataset.info;
                        const type = item.dataset.type;
                        e.dataTransfer.setData("text/plain", info);
                        e.dataTransfer.setData("element-type", type);
                    }
                }

                allowDrop(e) {
                    e.preventDefault();
                    e.currentTarget.classList.add('drag-over');
                }

                handleDragLeave(e) {
                    e.currentTarget.classList.remove('drag-over');
                }

                async handleDrop(e, type) {
                    e.preventDefault();
                    e.currentTarget.classList.remove('drag-over');
                    const elementType = e.dataTransfer.getData("element-type");

                    if (elementType !== type) return;

                    const info = e.dataTransfer.getData("text/plain");
                    const [id, ...nameParts] = info.split(':');
                    const name = nameParts.join(':');

                    // 조건인 경우 먼저 AI 검증 수행
                    if (type === 'condition' && window.resourceContext) {
                        const spelTemplate = this.findSpelForCondition(id);
                        if (spelTemplate) {
                            // 검증 중 표시
                            this.showLoadingModal('[ AI ] 조건 호환성 검증 중...');

                            try {
                                const resourceIdentifier = window.resourceContext.resourceIdentifier;
                                const response = await this.api.validateCondition(resourceIdentifier, spelTemplate);

                                this.hideLoadingModal();

                                if (!response.isCompatible) {
                                    // 호환되지 않으면 드롭 취소하고 이유 표시
                                    this.showValidationErrorModal(name, response.reason);
                                    return; // 드롭 중단
                                }
                            } catch (error) {
                                this.hideLoadingModal();
                                this.showMessage('조건 검증 중 오류가 발생했습니다.', 'error');
                                return; // 드롭 중단
                            }
                        }
                    }

                    // 검증 통과하거나 조건이 아닌 경우 정상적으로 추가
                    this.state.add(type, id, { id, name });
                    this.ui.renderAll(this.state);
                }

                findSpelForCondition(conditionId) {
                    const item = this.elements.conditionsPalette.querySelector(`.palette-item[data-info^="${conditionId}:"]`);
                    return item ? item.dataset.spel : null;
                }

                showLoadingModal(message) {
                    // 기존 모달이 있으면 제거
                    this.hideLoadingModal();

                    const modal = document.createElement('div');
                    modal.id = 'validation-loading-modal';
                    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 999998;
    `;

                    const loadingContent = document.createElement('div');
                    loadingContent.style.cssText = `
        background-color: #1f2937;
        border-radius: 0.5rem;
        padding: 1.5rem;
        display: flex;
        align-items: center;
        gap: 1rem;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    `;

                    loadingContent.innerHTML = `
        <div style="
            width: 2rem;
            height: 2rem;
            border: 3px solid #4f46e5;
            border-top-color: transparent;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        "></div>
        <span style="color: white; font-size: 1rem;">${message}</span>
        <style>
            @keyframes spin {
                to { transform: rotate(360deg); }
            }
        </style>
    `;

                    modal.appendChild(loadingContent);
                    document.body.appendChild(modal);
                }

                hideLoadingModal() {
                    const modal = document.getElementById('validation-loading-modal');
                    if (modal) {
                        modal.remove();
                    }
                }

                showValidationErrorModal(conditionName, reason) {
                    // 기존 모달 제거
                    const existingModal = document.getElementById('validation-error-modal');
                    if (existingModal) {
                        existingModal.remove();
                    }

                    const modal = document.createElement('div');
                    modal.id = 'validation-error-modal';
                    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.75);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 999999;
    `;

                    const modalContent = document.createElement('div');
                    modalContent.style.cssText = `
        background-color: #1f2937;
        border-radius: 0.5rem;
        padding: 1.5rem;
        max-width: 28rem;
        margin: 0 1rem;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
        border: 1px solid rgba(75, 85, 99, 0.3);
    `;

                    modalContent.innerHTML = `
        <div style="display: flex; align-items: flex-start; margin-bottom: 1rem;">
            <div style="flex-shrink: 0;">
                <i class="fas fa-exclamation-triangle" style="color: #ef4444; font-size: 1.5rem;"></i>
            </div>
            <div style="margin-left: 0.75rem;">
                <h3 style="font-size: 1.125rem; font-weight: 600; color: #ffffff; margin: 0;">조건 호환성 오류</h3>
                <p style="color: #d1d5db; margin-top: 0.25rem; margin-bottom: 0;">'${conditionName}' 조건은 현재 리소스에서 사용할 수 없습니다.</p>
            </div>
        </div>
        <div style="background-color: #374151; border-radius: 0.375rem; padding: 0.75rem; margin-bottom: 1rem;">
            <p style="font-size: 0.875rem; color: #d1d5db; margin: 0;"><strong>AI 분석 결과:</strong></p>
            <p style="font-size: 0.875rem; color: #9ca3af; margin: 0.25rem 0 0 0;">${reason}</p>
        </div>
        <button id="close-validation-modal" style="
            width: 100%;
            background-color: #4f46e5;
            color: white;
            font-weight: 500;
            padding: 0.5rem 1rem;
            border-radius: 0.375rem;
            border: none;
            cursor: pointer;
            transition: background-color 0.2s;
        " onmouseover="this.style.backgroundColor='#4338ca'" onmouseout="this.style.backgroundColor='#4f46e5'">
            확인
        </button>
    `;

                    modal.appendChild(modalContent);
                    document.body.appendChild(modal);

                    // 이벤트 리스너 추가
                    const closeButton = document.getElementById('close-validation-modal');
                    const closeModal = () => {
                        modal.remove();
                    };

                    closeButton.addEventListener('click', closeModal);
                    modal.addEventListener('click', (e) => {
                        if (e.target === modal) {
                            closeModal();
                        }
                    });

                    // 디버깅을 위한 로그
                    console.log('Validation error modal shown:', { conditionName, reason });
                }

                handleChipRemove(type, key) {
                    this.state.remove(type, key);
                    this.ui.renderAll(this.state);
                }

                findSpelForCondition(conditionId) {
                    const item = this.elements.conditionsPalette.querySelector(`.palette-item[data-info^="${conditionId}:"]`);
                    return item ? item.dataset.spel : null;
                }

                async validateConditionRealtime(conditionId, spel) {
                    const resourceIdentifier = window.resourceContext.resourceIdentifier;
                    const chip = this.elements.conditionsCanvas.querySelector(`[data-key="${conditionId}"]`);
                    if (chip) chip.innerHTML += ' <i class="fas fa-spinner fa-spin"></i>'; // 검증 중 표시

                    try {
                        const response = await this.api.validateCondition(resourceIdentifier, spel); // 신규 API 호출
                        const conditionState = this.state.conditions.get(conditionId);
                        conditionState.isValidated = true;
                        conditionState.isCompatible = response.isCompatible;
                        conditionState.reason = response.reason;
                    } catch (error) {
                        const conditionState = this.state.conditions.get(conditionId);
                        conditionState.isValidated = true;
                        conditionState.isCompatible = false;
                        conditionState.reason = "호환성 검증 중 오류가 발생했습니다.";
                    } finally {
                        this.ui.renderAll(this.state); // 검증 결과를 반영하여 UI 다시 렌더링
                    }
                }

                handleChipRemove(type, key) {
                    this.state.remove(type, key);
                    this.ui.renderAll(this.state);
                }

                // 🔥 개선된 스트리밍 AI 처리
                async handleGenerateByAI() {
                    console.log('🚀 AI 정책 생성 시작');

                    const query = this.elements.naturalLanguageInput?.value;
                    if (!query || !query.trim()) {
                        this.showMessage('요구사항을 입력해주세요.', 'error');
                        return;
                    }

                    this.ui.setLoading(this.elements.generateByAiBtn, true);
                    const thoughtContainer = this.elements.thoughtProcessContainer;
                    const thoughtLog = this.elements.thoughtProcessLog;

                    if (thoughtContainer && thoughtLog) {
                        thoughtLog.innerHTML = '<div style="color: #6c757d; font-style: italic;">🤖 AI가 정책을 분석하고 있습니다...</div><br>';
                        thoughtContainer.classList.remove('hidden');
                    }

                    try {
                        // 🔥 스트리밍 우선 시도 (간단한 버전)
                        console.log('🔥 스트리밍 API 시도...');
                        const success = await this.trySimpleStreaming(query, thoughtLog);

                        if (!success) {
                            // 스트리밍 실패시 일반 API로 fallback
                            console.log('🔥 스트리밍 실패, 일반 API로 fallback...');
                            const response = await this.api.generatePolicyFromText(query);

                            if (response && response.policyData) {
                                this.populateBuilderWithAIData(response);
                                this.showMessage('AI 정책 초안이 성공적으로 생성되었습니다!', 'success');
                            } else {
                                throw new Error('유효한 정책 데이터를 받지 못했습니다');
                            }
                        }
                    } catch (error) {
                        console.error('🔥 API 실패:', error);
                        this.showMessage('AI 정책 생성에 실패했습니다: ' + error.message, 'error');
                    } finally {
                        this.ui.setLoading(this.elements.generateByAiBtn, false);
                        if (thoughtContainer) {
                            setTimeout(() => thoughtContainer.classList.add('hidden'), 3000);
                        }
                    }
                }

                // 🔥 개선된 스트리밍 구현
                async trySimpleStreaming(query, thoughtLog) {
                    try {
                        console.log('🔥 스트리밍 API 호출 시작...');

                        const response = await this.api.generatePolicyFromTextStream(query);

                        if (!response.ok) {
                            console.warn('스트리밍 API 응답 실패:', response.status, response.statusText);
                            return false;
                        }

                        if (!response.body) {
                            console.warn('스트리밍 응답 본문 없음');
                            return false;
                        }

                        console.log('🔥 스트리밍 응답 헤더:', response.headers.get('content-type'));

                        let fullText = '';
                        let cleanFullText = '';
                        const reader = response.body.getReader();
                        const decoder = new TextDecoder('utf-8');

                        // 초기 로그 메시지 표시
                        if (thoughtLog) {
                            thoughtLog.innerHTML = '<div style="color: #6c757d; font-style: italic;">🤖 AI가 정책을 분석하고 있습니다...</div><br>';
                        }

                        console.log('🔥 스트리밍 읽기 시작...');

                        // SSE 파싱을 위한 버퍼
                        let buffer = '';
                        let jsonStarted = false;
                        let jsonBuffer = '';

                        while (true) {
                            const { value, done } = await reader.read();

                            if (done) {
                                console.log('🔥 스트리밍 완료');
                                // 버퍼에 남은 데이터 처리
                                if (buffer) {
                                    this.processSSELine(buffer, thoughtLog);
                                    cleanFullText += this.extractDataFromSSE(buffer);
                                }
                                break;
                            }

                            const chunk = decoder.decode(value, { stream: true });
                            buffer += chunk;

                            // 줄 단위로 SSE 파싱
                            const lines = buffer.split('\n');

                            // 마지막 줄은 불완전할 수 있으므로 버퍼에 보관
                            buffer = lines.pop() || '';

                            for (const line of lines) {
                                if (line.trim() === '') continue; // 빈 줄 무시

                                if (line.startsWith('data: ')) {
                                    const data = line.substring(6);

                                    if (data === '[DONE]') {
                                        console.log('🔥 스트리밍 완료 신호 수신');
                                        break;
                                    }

                                    cleanFullText += data;

                                    // JSON 블록 감지
                                    if (data.includes('===JSON시작===')) {
                                        jsonStarted = true;
                                        jsonBuffer = data;
                                    } else if (jsonStarted && !data.includes('===JSON끝===')) {
                                        jsonBuffer += data;
                                    } else if (data.includes('===JSON끝===')) {
                                        jsonBuffer += data;
                                        jsonStarted = false;

                                        // 완전한 JSON 블록 표시
                                        if (thoughtLog) {
                                            this.displayStreamingData(thoughtLog, jsonBuffer);
                                        }
                                        jsonBuffer = '';
                                    } else if (!jsonStarted && thoughtLog) {
                                        // 일반 텍스트 실시간 표시
                                        this.displayStreamingData(thoughtLog, data);
                                    }
                                }
                            }
                        }

                        console.log('🔥 스트리밍 완료, 전체 길이:', cleanFullText.length);
                        console.log('🔥 깨끗한 텍스트 미리보기:', cleanFullText.substring(0, 300) + '...');

                        // JSON 추출 시도
                        const jsonData = this.extractSimpleJson(cleanFullText);
                        if (jsonData) {
                            this.populateBuilderWithAIData(jsonData);
                            this.showMessage('AI 정책 초안이 스트리밍으로 생성되었습니다!', 'success');
                            return true;
                        }

                        console.warn('스트리밍에서 JSON 추출 실패');
                        return false;

                    } catch (error) {
                        console.error('스트리밍 오류:', error);
                        if (thoughtLog) {
                            thoughtLog.innerHTML += `<div style="color: #dc3545;">❌ 스트리밍 오류: ${error.message}</div>`;
                        }
                        return false;
                    }
                }

// SSE 라인에서 데이터 추출
                extractDataFromSSE(line) {
                    if (line.startsWith('data: ')) {
                        return line.substring(6);
                    }
                    return '';
                }

// 🔥 개선된 JSON 추출 메서드
                extractSimpleJson(text) {
                    console.log('🔥 간단 JSON 추출 시도...');
                    console.log('🔥 전체 텍스트 길이:', text.length);

                    try {
                        // 1. 한국어 JSON 마커 방식 (최우선)
                        const koreanMarkerRegex = /===JSON시작===([\s\S]*?)===JSON끝===/;
                        const koreanMatch = text.match(koreanMarkerRegex);

                        if (koreanMatch) {
                            try {
                                let jsonStr = koreanMatch[1].trim();
                                console.log('🔥 한국어 마커로 추출된 JSON:', jsonStr.substring(0, 200) + '...');

                                // JSON 정제 - 주석 제거 및 클린업
                                jsonStr = this.cleanJsonString(jsonStr);

                                const parsed = JSON.parse(jsonStr);
                                console.log('🔥 한국어 마커 JSON 파싱 성공:', parsed);

                                // policyData가 있으면 그대로 반환, 없으면 래핑
                                if (parsed.policyData) {
                                    return parsed;
                                } else {
                                    return {
                                        policyData: parsed,
                                        roleIdToNameMap: this.createIdToNameMap('role', parsed.roleIds),
                                        permissionIdToNameMap: this.createIdToNameMap('permission', parsed.permissionIds),
                                        conditionIdToNameMap: this.createIdToNameMap('condition', Object.keys(parsed.conditions || {}))
                                    };
                                }
                            } catch (e) {
                                console.log('🔥 한국어 마커 JSON 파싱 실패:', e.message);
                            }
                        }

                        // 2. 영어 마커 방식들
                        const markerPatterns = [
                            /<<JSON_START>>([\s\S]*?)<<JSON_END>>/,
                            /<<<JSON_START>>>([\s\S]*?)<<<JSON_END>>>/,
                            /JSON_START([\s\S]*?)JSON_END/,
                            /\*\*JSON\*\*([\s\S]*?)\*\*\/JSON\*\*/,
                        ];

                        for (const pattern of markerPatterns) {
                            const match = text.match(pattern);
                            if (match) {
                                try {
                                    let jsonStr = match[1].trim();
                                    jsonStr = this.cleanJsonString(jsonStr);
                                    const parsed = JSON.parse(jsonStr);
                                    console.log('🔥 영어 마커 JSON 파싱 성공:', parsed);

                                    if (parsed.policyData) {
                                        return parsed;
                                    } else {
                                        return {
                                            policyData: parsed,
                                            roleIdToNameMap: this.createIdToNameMap('role', parsed.roleIds),
                                            permissionIdToNameMap: this.createIdToNameMap('permission', parsed.permissionIds),
                                            conditionIdToNameMap: this.createIdToNameMap('condition', Object.keys(parsed.conditions || {}))
                                        };
                                    }
                                } catch (e) {
                                    console.log('🔥 영어 마커 JSON 파싱 실패:', e.message);
                                    continue;
                                }
                            }
                        }

                        // 3. 중괄호 기반 추출 (fallback)
                        const jsonMatch = text.match(/\{[\s\S]*"policyName"[\s\S]*"effect"[\s\S]*\}/);
                        if (jsonMatch) {
                            try {
                                let jsonStr = jsonMatch[0];
                                jsonStr = this.cleanJsonString(jsonStr);
                                const parsed = JSON.parse(jsonStr);
                                console.log('🔥 중괄호 기반 JSON 파싱 성공:', parsed);

                                if (parsed.policyData) {
                                    return parsed;
                                } else {
                                    return {
                                        policyData: parsed,
                                        roleIdToNameMap: this.createIdToNameMap('role', parsed.roleIds),
                                        permissionIdToNameMap: this.createIdToNameMap('permission', parsed.permissionIds),
                                        conditionIdToNameMap: this.createIdToNameMap('condition', Object.keys(parsed.conditions || {}))
                                    };
                                }
                            } catch (e) {
                                console.log('🔥 중괄호 기반 JSON 파싱 실패:', e.message);
                            }
                        }

                        console.warn('🔥 JSON 추출 실패 - 모든 방법 시도함');
                        return null;

                    } catch (error) {
                        console.error('🔥 JSON 추출 오류:', error);
                        return null;
                    }
                }

// 🔥 JSON 문자열 정제 메서드
                cleanJsonString(jsonStr) {
                    console.log('🔥 JSON 정제 시작, 원본 길이:', jsonStr.length);

                    // 1. 마크다운 코드 블록 제거
                    let cleaned = jsonStr
                        .replace(/```json\s*/g, '')
                        .replace(/```\s*/g, '');

                    // 2. 주석 제거 (// 스타일)
                    cleaned = cleaned.split('\n').map(line => {
                        // 문자열 내부가 아닌 // 주석만 제거
                        let inString = false;
                        let result = '';
                        for (let i = 0; i < line.length; i++) {
                            if (line[i] === '"' && (i === 0 || line[i-1] !== '\\')) {
                                inString = !inString;
                            }
                            if (!inString && line[i] === '/' && line[i+1] === '/') {
                                break; // 주석 시작, 나머지 줄 무시
                            }
                            result += line[i];
                        }
                        return result.trim();
                    }).join('\n');

                    // 3. /* */ 스타일 주석 제거
                    cleaned = cleaned.replace(/\/\*[\s\S]*?\*\//g, '');

                    // 4. 잘못된 쉼표 제거
                    cleaned = cleaned
                        .replace(/,\s*}/g, '}')
                        .replace(/,\s*]/g, ']')
                        .replace(/,(\s*[}\]])/g, '$1');

                    // 5. 불필요한 공백 정리
                    cleaned = cleaned
                        .replace(/\s+/g, ' ')
                        .replace(/\s*:\s*/g, ':')
                        .replace(/\s*,\s*/g, ',')
                        .replace(/\s*{\s*/g, '{')
                        .replace(/\s*}\s*/g, '}')
                        .replace(/\s*\[\s*/g, '[')
                        .replace(/\s*]\s*/g, ']');

                    // 6. conditional 필드 제거 (있는 경우)
                    if (cleaned.includes('"conditional"')) {
                        cleaned = cleaned.replace(/"conditional"\s*:\s*(true|false)\s*,?/g, '');
                    }

                    console.log('🔥 정제된 JSON 길이:', cleaned.length);
                    console.log('🔥 정제된 JSON 미리보기:', cleaned.substring(0, 200) + '...');

                    return cleaned.trim();
                }

// ID to Name 매핑 생성 헬퍼
                createIdToNameMap(type, ids) {
                    if (!ids || !Array.isArray(ids)) return {};

                    const map = {};
                    const dataSource = type === 'role' ? window.allRoles :
                        type === 'permission' ? window.allPermissions :
                            type === 'condition' ? window.allConditions : [];

                    ids.forEach(id => {
                        const item = dataSource.find(item => item.id == id);
                        if (item) {
                            map[id] = type === 'role' ? item.roleName :
                                type === 'permission' ? item.friendlyName :
                                    type === 'condition' ? item.name : '';
                        }
                    });

                    return map;
                }

// 🔥 스트리밍 데이터 표시 개선
                displayStreamingData(thoughtLog, data) {
                    try {
                        // JSON 블록은 코드 블록으로 표시
                        if (data.includes('===JSON시작===') || data.includes('===JSON끝===')) {
                            thoughtLog.innerHTML += `<div style="background: #1e1e1e; padding: 10px; border-radius: 5px; margin: 10px 0;">
                <pre style="color: #4fc3f7; font-family: monospace; margin: 0; white-space: pre-wrap;">${this.escapeHtml(data)}</pre>
            </div>`;
                        } else {
                            // 일반 텍스트는 포맷팅하여 표시
                            let displayData = this.escapeHtml(data);

                            // 키워드 하이라이팅
                            displayData = displayData
                                .replace(/분석|구성|매핑/g, '<span style="color: #28a745;">🔍 $&</span>')
                                .replace(/역할|권한|조건/g, '<span style="color: #fd7e14;">📋 $&</span>')
                                .replace(/정책/g, '<span style="color: #dc3545;">🎯 $&</span>')
                                .replace(/\*\*([^*]+)\*\*/g, '<br><strong>$1</strong><br>');

                            thoughtLog.innerHTML += displayData + ' ';
                        }

                        thoughtLog.scrollTop = thoughtLog.scrollHeight;
                    } catch (error) {
                        console.error('스트리밍 데이터 표시 오류:', error);
                        thoughtLog.innerHTML += this.escapeHtml(data) + ' ';
                    }
                }

// HTML 이스케이프 헬퍼
                escapeHtml(text) {
                    const div = document.createElement('div');
                    div.textContent = text;
                    return div.innerHTML;
                }

                populateBuilderWithAIData(draftDto) {
                    console.log('🔥 AI 데이터로 빌더 채우기:', draftDto);

                    if (!draftDto || !draftDto.policyData) {
                        this.showMessage('AI가 정책 초안을 생성하지 못했습니다.', 'error');
                        return;
                    }

                    const data = draftDto.policyData;
                    const maps = {
                        roles: draftDto.roleIdToNameMap || {},
                        permissions: draftDto.permissionIdToNameMap || {},
                        conditions: draftDto.conditionIdToNameMap || {}
                    };

                    console.log('🔥 이름 매핑 정보:', maps);

                    // 상태 초기화
                    ['role', 'permission', 'condition'].forEach(type => this.state.clear(type));

                    // 기본 필드 설정
                    if (this.elements.policyNameInput) {
                        this.elements.policyNameInput.value = data.policyName || '';
                    }
                    if (this.elements.policyDescTextarea) {
                        this.elements.policyDescTextarea.value = data.description || '';
                    }
                    if (this.elements.policyEffectSelect) {
                        this.elements.policyEffectSelect.value = data.effect || 'ALLOW';
                    }

                    // 역할 추가 (실제 이름과 함께)
                    if (data.roleIds && Array.isArray(data.roleIds)) {
                        data.roleIds.forEach(id => {
                            const name = maps.roles[id] || `역할 (ID: ${id})`;
                            console.log(`🔥 역할 추가: ID=${id}, Name=${name}`);
                            this.state.add('role', String(id), { id, name });
                        });
                    }

                    // 권한 추가 (실제 이름과 함께)
                    if (data.permissionIds && Array.isArray(data.permissionIds)) {
                        data.permissionIds.forEach(id => {
                            const name = maps.permissions[id] || `권한 (ID: ${id})`;
                            console.log(`🔥 권한 추가: ID=${id}, Name=${name}`);
                            this.state.add('permission', String(id), { id, name });
                        });
                    }

                    // 조건 추가 (실제 이름과 함께)
                    if (data.conditions && typeof data.conditions === 'object') {
                        Object.keys(data.conditions).forEach(id => {
                            const name = maps.conditions[id] || `조건 (ID: ${id})`;
                            console.log(`🔥 조건 추가: ID=${id}, Name=${name}`);
                            this.state.add('condition', String(id), { id, name });
                        });
                    }

                    // AI 설정
                    this.state.aiRiskAssessmentEnabled = data.aiRiskAssessmentEnabled || false;
                    if (this.elements.aiEnabledCheckbox) {
                        this.elements.aiEnabledCheckbox.checked = this.state.aiRiskAssessmentEnabled;
                    }

                    this.state.requiredTrustScore = data.requiredTrustScore || 0.7;
                    if (this.elements.trustScoreSlider) {
                        this.elements.trustScoreSlider.value = this.state.requiredTrustScore * 100;
                    }
                    if (this.elements.trustScoreValueSpan) {
                        this.elements.trustScoreValueSpan.textContent = Math.round(this.state.requiredTrustScore * 100);
                    }

                    // UI 업데이트
                    this.handleAiToggle();
                    this.ui.renderAll(this.state);

                    console.log('🔥 최종 상태:', {
                        roles: Array.from(this.state.roles.entries()),
                        permissions: Array.from(this.state.permissions.entries()),
                        conditions: Array.from(this.state.conditions.entries())
                    });
                }

                handleAiToggle() {
                    if (this.elements.aiEnabledCheckbox) {
                        this.state.aiRiskAssessmentEnabled = this.elements.aiEnabledCheckbox.checked;
                    }
                    if (this.elements.trustScoreContainer) {
                        this.elements.trustScoreContainer.classList.toggle('hidden', !this.state.aiRiskAssessmentEnabled);
                    }
                    this.ui.updatePreview(this.state);
                }

                handleTrustSlider() {
                    if (this.elements.trustScoreSlider) {
                        this.state.requiredTrustScore = this.elements.trustScoreSlider.value / 100.0;
                        if (this.elements.trustScoreValueSpan) {
                            this.elements.trustScoreValueSpan.textContent = this.elements.trustScoreSlider.value;
                        }
                    }
                    this.ui.updatePreview(this.state);
                }

                async handleSavePolicy() {
                    const dto = this.state.toDto();

                    if (!dto.policyName) {
                        this.showMessage('정책 이름은 필수입니다.', 'error');
                        return;
                    }
                    if (dto.roleIds.length === 0) {
                        this.showMessage('하나 이상의 역할을 선택해야 합니다.', 'error');
                        return;
                    }
                    if (dto.permissionIds.length === 0) {
                        this.showMessage('하나 이상의 권한을 선택해야 합니다.', 'error');
                        return;
                    }

                    this.ui.setLoading(this.elements.savePolicyBtn, true);
                    try {
                        const result = await this.api.savePolicy(dto);
                        this.showMessage(`정책 "${result.name}"이(가) 성공적으로 생성되었습니다.`, 'success');
                        setTimeout(() => window.location.href = '/admin/policies', 1500);
                    } catch (error) {
                        console.error('정책 저장 오류:', error);
                        this.showMessage('정책 저장 중 오류가 발생했습니다.', 'error');
                    } finally {
                        this.ui.setLoading(this.elements.savePolicyBtn, false);
                    }
                }

                initializeFromContext() {
                    if (window.resourceContext) {
                        const availableParamTypes = new Set(
                            (window.resourceContext.parameterTypes || []).map(p => p.type)
                        );
                        if (window.resourceContext.returnObjectType) {
                            availableParamTypes.add(window.resourceContext.returnObjectType);
                        }

                        // 조건 팔레트의 각 아이템을 순회하며 호환성 검사
                        this.elements.conditionsPalette.querySelectorAll('.palette-item').forEach(item => {
                            // 예시: data-required-type="Document" 와 같은 속성이 템플릿에 있다고 가정
                            const requiredType = item.dataset.requiredType;

                            if (requiredType && !availableParamTypes.has(requiredType)) {
                                item.classList.add('disabled'); // 호환되지 않으면 비활성화
                                item.title = `이 조건은 '${requiredType}' 타입의 정보가 필요하지만, 현재 리소스는 제공하지 않습니다.`;
                            }
                        });
                    }
                    if (window.preselectedPermission) {
                        const perm = window.preselectedPermission;
                        this.state.add('permission', String(perm.id), { id: perm.id, name: perm.friendlyName });
                    }
                }

                showMessage(message, type) {
                    if (typeof showToast === 'function') {
                        showToast(message, type);
                    } else {
                        alert(message);
                    }
                }
            }

            new PolicyBuilderApp();
            console.log('🌟 PolicyBuilderApp 초기화 성공!');
        } catch (error) {
            console.error('❌ PolicyBuilderApp 초기화 실패:', error);
        }
    });
})();