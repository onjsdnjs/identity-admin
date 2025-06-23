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
                    permissionIds: Array.from(this.permissions.keys()).map(Number), // 서버 DTO와 일치하도록 수정
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
                    // showToast가 없는 경우를 대비한 안전한 에러 처리
                    if (typeof showToast === 'function') {
                        showToast(error.message, 'error');
                    } else {
                        console.error('Error:', error.message);
                        alert('오류: ' + error.message);
                    }
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
                // HTML의 실제 ID와 JavaScript에서 사용할 키 매핑
                const idMapping = {
                    'rolesPalette': 'roles-palette',
                    'rolesCanvas': 'roles-canvas',
                    'naturalLanguageInput': 'naturalLanguageInput',
                    'generateByAiBtn': 'generateByAiBtn',
                    'aiEnabledCheckbox': 'aiEnabledCheckbox',
                    'trustScoreContainer': 'trustScoreContainer',
                    'trustScoreSlider': 'trustScoreSlider',
                    'trustScoreValueSpan': 'trustScoreValueSpan',
                    'customSpelInput': 'customSpelInput',
                    'permissionsPalette': 'permissionsPalette',
                    'conditionsPalette': 'conditionsPalette',
                    'permissionsCanvas': 'permissionsCanvas',
                    'conditionsCanvas': 'conditionsCanvas',
                    'policyNameInput': 'policyNameInput',
                    'policyDescTextarea': 'policyDescTextarea',
                    'policyEffectSelect': 'policyEffectSelect',
                    'savePolicyBtn': 'savePolicyBtn',
                    'policyPreview': 'policyPreview'
                };

                const elements = {};
                Object.entries(idMapping).forEach(([jsKey, htmlId]) => {
                    const element = document.getElementById(htmlId);
                    if (element) {
                        elements[jsKey] = element;
                        console.log(`Found element: ${htmlId} -> ${jsKey}`);
                    } else {
                        console.warn(`Element not found: ${htmlId}`);
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

            /**
             * [최종 구현] 'AI로 정책 생성' 버튼 클릭 시, 스트리밍 응답을 처리합니다.
             */
            async handleGenerateByAI() {
                const query = this.elements.naturalLanguageInput.value;
                if (!query.trim()) return showToast('요구사항을 입력해주세요.', 'error');

                // UI 초기화
                this.ui.setLoading(this.elements.generateByAiBtn, true);
                const thoughtProcessContainer = document.getElementById('ai-thought-process-container'); // HTML에 이 div가 추가되어야 함
                if (thoughtProcessContainer) {
                    thoughtProcessContainer.innerHTML = '';
                    thoughtProcessContainer.style.display = 'block';
                }

                let fullResponseText = '';

                try {
                    const response = await fetch('/api/ai/policies/generate-from-text/stream', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', [this.api.csrfHeader]: this.api.csrfToken },
                        body: JSON.stringify({ naturalLanguageQuery: query })
                    });

                    if (!response.ok) throw new Error('AI 서비스 연결에 실패했습니다.');

                    const reader = response.body.getReader();
                    const decoder = new TextDecoder();

                    // 스트림 실시간 처리
                    while (true) {
                        const { value, done } = await reader.read();
                        if (done) break;

                        const chunk = decoder.decode(value, { stream: true });
                        fullResponseText += chunk;

                        if (thoughtProcessContainer) {
                            // XSS 방지를 위해 텍스트를 안전하게 추가 (간단한 예시)
                            thoughtProcessContainer.textContent += chunk;
                            thoughtProcessContainer.scrollTop = thoughtProcessContainer.scrollHeight;
                        }
                    }

                    // 스트리밍 완료 후 최종 JSON 파싱 및 UI 채우기
                    this.processFinalAiResponse(fullResponseText);

                } catch (error) {
                    console.error("AI 정책 생성 실패:", error);
                    showToast(error.message, 'error');
                } finally {
                    this.ui.setLoading(this.elements.generateByAiBtn, false);
                    if (thoughtProcessContainer) {
                        // 분석이 끝나면 5초 후 로그 창을 숨길 수 있음
                        // setTimeout(() => { thoughtProcessContainer.style.display = 'none'; }, 5000);
                    }
                }
            }

            /**
             * [신규] 스트리밍 완료 후 전체 텍스트에서 JSON을 추출하고 UI를 채웁니다.
             */
            processFinalAiResponse(fullText) {
                // 특수 구분자 사이의 JSON 데이터만 추출
                const jsonMatch = fullText.match(/<<JSON_START>>([\s\S]*?)<<JSON_END>>/);

                if (jsonMatch && jsonMatch[1]) {
                    try {
                        const finalJson = JSON.parse(jsonMatch[1]);
                        // 이전에 구현한 populateBuilderWithAIData 호출
                        this.populateBuilderWithAIData(finalJson);
                        showToast('AI 정책 초안이 생성되었습니다. 내용을 검토 후 저장하세요.', 'success');
                    } catch (e) {
                        showToast('AI가 반환한 최종 정책 데이터(JSON) 파싱에 실패했습니다.', 'error');
                        console.error("Final JSON parsing error:", e);
                    }
                } else {
                    showToast('AI가 정책 초안을 완성하지 못했습니다. 더 명확한 언어로 다시 시도해보세요.', 'error');
                }
            }

            /**
             * [구현 완료] AI가 생성한 DTO 데이터로 빌더 UI 전체를 채웁니다.
             * @param {object} draftDto - AiGeneratedPolicyDraftDto
             */
            populateBuilderWithAIData(draftDto) {
                if (!draftDto || !draftDto.policyData) {
                    showToast('AI가 정책 초안을 생성하지 못했습니다. 더 명확한 언어로 요청해보세요.', 'error');
                    return;
                }

                const data = draftDto.policyData;
                const maps = {
                    roles: draftDto.roleIdToNameMap || {},
                    permissions: draftDto.permissionIdToNameMap || {},
                    conditions: draftDto.conditionIdToNameMap || {}
                };

                // 1. 모든 캔버스와 상태를 깨끗하게 초기화
                ['role', 'permission', 'condition'].forEach(type => this.state.clear(type));

                // 2. 기본 속성 필드 채우기
                this.elements.policyNameInput.value = data.policyName || '';
                this.elements.policyDescTextarea.value = data.description || '';
                this.elements.policyEffectSelect.value = data.effect || 'ALLOW';

                // 3. 역할, 권한, 조건 캔버스 채우기
                // AI가 반환한 ID 목록을 기반으로, 함께 전달된 이름 매핑 정보를 사용하여 칩을 생성합니다.
                data.roleIds?.forEach(id => {
                    const name = maps.roles[id] || `알 수 없는 역할 (ID: ${id})`;
                    this.state.add('role', String(id), { id, name });
                });

                data.permissionIds?.forEach(id => {
                    const name = maps.permissions[id] || `알 수 없는 권한 (ID: ${id})`;
                    this.state.add('permission', String(id), { id, name });
                });

                if (data.conditions) {
                    Object.keys(data.conditions).forEach(id => {
                        const name = maps.conditions[id] || `알 수 없는 조건 (ID: ${id})`;
                        const params = data.conditions[id];
                        this.state.add('condition', String(id), { id, name, params });
                    });
                }

                // 4. AI 및 전문가용 설정 필드 채우기
                this.state.aiRiskAssessmentEnabled = data.aiRiskAssessmentEnabled || false;
                this.elements.aiEnabledCheckbox.checked = this.state.aiRiskAssessmentEnabled;

                this.state.requiredTrustScore = data.requiredTrustScore || 0.7;
                this.elements.trustScoreSlider.value = this.state.requiredTrustScore * 100;
                this.elements.trustScoreValueSpan.textContent = this.elements.trustScoreSlider.value;
                this.elements.trustScoreContainer.classList.toggle('hidden', !this.state.aiRiskAssessmentEnabled);

                this.state.customConditionSpel = data.customConditionSpel || '';
                this.elements.customSpelInput.value = this.state.customConditionSpel;

                // 5. 변경된 전체 상태를 기반으로 UI를 한번에 다시 렌더링
                this.handleAiToggle(); // 슬라이더 표시 여부 업데이트
                this.ui.renderAll(this.state);
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
                console.log('Sending DTO:', dto); // 디버깅용

                if (!dto.policyName) return showToast('정책 이름은 필수입니다.', 'error');
                if (dto.roleIds.length === 0) return showToast('하나 이상의 역할을 선택해야 합니다.', 'error');
                if (dto.permissionIds.length === 0) return showToast('하나 이상의 권한을 선택해야 합니다.', 'error');

                this.ui.setLoading(this.elements.savePolicyBtn, true);
                try {
                    const result = await this.api.savePolicy(dto);
                    if (typeof showToast === 'function') {
                        showToast(`정책 "${result.name}"이(가) 성공적으로 생성되었습니다.`, 'success');
                    } else {
                        alert(`정책 "${result.name}"이(가) 성공적으로 생성되었습니다.`);
                    }
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