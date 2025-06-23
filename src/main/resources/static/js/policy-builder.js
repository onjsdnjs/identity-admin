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
                        chip.innerHTML = `${value.name} <button class="remove-chip-btn" data-type="${type}" data-key="${key}">&times;</button>`;
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

                            canvas.addEventListener('drop', (e) => this.handleDrop(e, type));
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

                handleDrop(e, type) {
                    e.preventDefault();
                    e.currentTarget.classList.remove('drag-over');
                    const elementType = e.dataTransfer.getData("element-type");

                    if (elementType !== type) return;

                    const info = e.dataTransfer.getData("text/plain");
                    const [id, ...nameParts] = info.split(':');
                    const name = nameParts.join(':');

                    this.state.add(type, id, { id, name });
                    this.ui.renderAll(this.state);
                }

                handleChipRemove(type, key) {
                    this.state.remove(type, key);
                    this.ui.renderAll(this.state);
                }

                // 🔥 개선된 스트리밍 AI 처리
                /*async handleGenerateByAI() {
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
                        thoughtLog.textContent = '';
                        thoughtContainer.classList.remove('hidden');
                    }

                    try {
                        // 스트리밍 API 시도
                        await this.tryStreamingAPI(query, thoughtLog);
                    } catch (streamError) {
                        console.warn('🔥 스트리밍 실패, fallback 시도:', streamError);
                        try {
                            // Fallback to 일반 API
                            await this.tryRegularAPI(query, thoughtLog);
                        } catch (fallbackError) {
                            console.error('🔥 모든 API 실패:', fallbackError);
                            this.showMessage('AI 정책 생성에 실패했습니다: ' + fallbackError.message, 'error');
                        }
                    } finally {
                        this.ui.setLoading(this.elements.generateByAiBtn, false);
                        if (thoughtContainer) {
                            setTimeout(() => thoughtContainer.classList.add('hidden'), 5000);
                        }
                    }
                }*/

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
                        thoughtLog.textContent = 'AI가 정책을 분석하고 있습니다...';
                        thoughtContainer.classList.remove('hidden');
                    }

                    try {
                        // 🔥 스트리밍 제거 - 일반 API만 사용
                        console.log('🔥 일반 API 시도...');
                        const response = await this.api.generatePolicyFromText(query);

                        if (response && response.policyData) {
                            this.populateBuilderWithAIData(response);
                            this.showMessage('AI 정책 초안이 성공적으로 생성되었습니다!', 'success');
                        } else {
                            throw new Error('유효한 정책 데이터를 받지 못했습니다');
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

                async tryStreamingAPI(query, thoughtLog) {
                    console.log('🔥 스트리밍 API 시도...');

                    const response = await this.api.generatePolicyFromTextStream(query);

                    if (!response.ok) {
                        throw new Error(`스트리밍 API 오류: ${response.status}`);
                    }

                    if (!response.body) {
                        throw new Error('응답 본문이 없습니다');
                    }

                    let fullResponse = '';
                    let buffer = '';
                    const reader = response.body.getReader();
                    const decoder = new TextDecoder('utf-8');

                    console.log('🔥 스트림 읽기 시작');

                    while (true) {
                        const { value, done } = await reader.read();
                        if (done) break;

                        // 🔥 개선된 디코딩
                        const chunk = decoder.decode(value, { stream: true });
                        buffer += chunk;

                        // 완전한 라인들을 처리
                        const lines = buffer.split('\n');
                        buffer = lines.pop() || '';

                        for (const line of lines) {
                            if (line.startsWith('data: ')) {
                                const data = line.substring(6).trim();
                                if (data && data !== '[DONE]') {
                                    // ERROR 체크
                                    if (data.startsWith('ERROR:')) {
                                        throw new Error(data.substring(6).trim());
                                    }

                                    fullResponse += data;
                                    if (thoughtLog) {
                                        thoughtLog.textContent += data;
                                        thoughtLog.scrollTop = thoughtLog.scrollHeight;
                                    }
                                }
                            }
                        }
                    }

                    // 남은 버퍼 처리
                    if (buffer.trim()) {
                        fullResponse += buffer;
                        if (thoughtLog) {
                            thoughtLog.textContent += buffer;
                        }
                    }

                    console.log('🔥 스트리밍 완료, 응답 길이:', fullResponse.length);
                    console.log('🔥 전체 응답 미리보기:', fullResponse.substring(0, 200) + '...');

                    // JSON 파싱 및 처리
                    this.processAIResponse(fullResponse);
                }

                async tryRegularAPI(query, thoughtLog) {
                    console.log('🔥 일반 API 시도...');

                    if (thoughtLog) {
                        thoughtLog.textContent = 'AI가 정책을 분석하고 있습니다...';
                    }

                    const response = await this.api.generatePolicyFromText(query);

                    if (response && response.policyData) {
                        this.populateBuilderWithAIData(response);
                        this.showMessage('AI 정책 초안이 성공적으로 생성되었습니다!', 'success');
                    } else {
                        throw new Error('유효한 정책 데이터를 받지 못했습니다');
                    }
                }

                processAIResponse(fullText) {
                    console.log('🔥 AI 응답 처리 시작');
                    console.log('🔥 전체 텍스트 길이:', fullText.length);
                    console.log('🔥 첫 300자:', fullText.substring(0, 300));

                    try {
                        const jsonData = this.extractJsonFromResponse(fullText);
                        console.log('🔥 JSON 추출 성공:', jsonData);
                        this.handleParsedAIData(jsonData);
                        return;
                    } catch (error) {
                        console.warn('🔥 JSON 추출 실패:', error);

                        // Fallback: 텍스트 분석으로 기본 데이터 생성
                        const extractedData = this.extractDataFromText(fullText);
                        if (extractedData) {
                            this.handleParsedAIData(extractedData);
                            return;
                        }

                        throw new Error('AI 응답에서 유효한 정책 데이터를 찾을 수 없습니다');
                    }
                }

                /**
                 * 🔥 개선된 JSON 추출 메서드 - 한글 마커 지원
                 */
                extractJsonFromResponse(text) {
                    console.log('🔥 JSON 추출 시도...');

                    // 1. 한글 마커 방식 (===JSON시작===, ===JSON끝===)
                    let startMarker = '===JSON시작===';
                    let endMarker = '===JSON끝===';
                    let startIndex = text.indexOf(startMarker);
                    let endIndex = text.indexOf(endMarker);

                    if (startIndex !== -1 && endIndex !== -1 && endIndex > startIndex) {
                        const jsonText = text.substring(startIndex + startMarker.length, endIndex).trim();
                        console.log('🔥 한글 마커로 추출된 JSON:', jsonText);
                        return JSON.parse(this.cleanJsonString(jsonText));
                    }

                    // 2. 영어 마커 방식 (JSON_RESULT_START/END)
                    startMarker = 'JSON_RESULT_START';
                    endMarker = 'JSON_RESULT_END';
                    startIndex = text.indexOf(startMarker);
                    endIndex = text.indexOf(endMarker);

                    if (startIndex !== -1 && endIndex !== -1 && endIndex > startIndex) {
                        const jsonText = text.substring(startIndex + startMarker.length, endIndex).trim();
                        console.log('🔥 영어 마커로 추출된 JSON:', jsonText);
                        return JSON.parse(this.cleanJsonString(jsonText));
                    }

                    // 3. 구형 마커 방식 (<<<JSON_START>>>)
                    startMarker = '<<<JSON_START>>>';
                    endMarker = '<<<JSON_END>>>';
                    startIndex = text.indexOf(startMarker);
                    endIndex = text.indexOf(endMarker);

                    if (startIndex !== -1 && endIndex !== -1 && endIndex > startIndex) {
                        const jsonText = text.substring(startIndex + startMarker.length, endIndex).trim();
                        console.log('🔥 구형 마커로 추출된 JSON:', jsonText);
                        return JSON.parse(this.cleanJsonString(jsonText));
                    }

                    // 4. 마크다운 코드 블록 제거
                    const markdownPatterns = [
                        /```json\s*([\s\S]*?)\s*```/i,
                        /```\s*([\s\S]*?)\s*```/i
                    ];

                    for (const pattern of markdownPatterns) {
                        const match = text.match(pattern);
                        if (match && match[1]) {
                            const jsonText = match[1].trim();
                            console.log('🔥 마크다운에서 추출된 JSON:', jsonText);

                            try {
                                const parsed = JSON.parse(this.cleanJsonString(jsonText));
                                if (this.isValidPolicyData(parsed)) {
                                    return parsed;
                                }
                            } catch (e) {
                                console.warn('🔥 마크다운 JSON 파싱 실패:', e);
                            }
                        }
                    }

                    // 5. 중괄호 방식으로 JSON 객체 추출
                    const jsonStart = text.indexOf('{');
                    const jsonEnd = this.findMatchingBrace(text, jsonStart);

                    if (jsonStart !== -1 && jsonEnd !== -1) {
                        const jsonText = text.substring(jsonStart, jsonEnd + 1);
                        console.log('🔥 중괄호로 추출된 JSON:', jsonText);

                        try {
                            const parsed = JSON.parse(this.cleanJsonString(jsonText));
                            if (this.isValidPolicyData(parsed)) {
                                return parsed;
                            }
                        } catch (e) {
                            console.warn('🔥 중괄호 JSON 파싱 실패:', e);
                        }
                    }

                    throw new Error('유효한 JSON을 찾을 수 없습니다');
                }

                /**
                 * 매칭되는 중괄호 찾기
                 */
                findMatchingBrace(text, start) {
                    if (start === -1 || start >= text.length || text.charAt(start) !== '{') {
                        return -1;
                    }

                    let braceCount = 1;
                    for (let i = start + 1; i < text.length; i++) {
                        const char = text.charAt(i);
                        if (char === '{') {
                            braceCount++;
                        } else if (char === '}') {
                            braceCount--;
                            if (braceCount === 0) {
                                return i;
                            }
                        }
                    }
                    return -1;
                }

                /**
                 * JSON 문자열 정제
                 */
                cleanJsonString(jsonStr) {
                    if (!jsonStr) return jsonStr;

                    console.log('🔥 JSON 정제 시작:', jsonStr.substring(0, 100));

                    // 1. 기본 정제 - 한글 보존
                    let cleaned = jsonStr
                        .replace(/\r\n/g, '\n')
                        .replace(/\r/g, '\n')
                        .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '') // 제어 문자만 제거
                        .replace(/\n\s*\n/g, '\n')
                        .trim();

                    // 2. JSON 객체 범위 찾기
                    const jsonStart = cleaned.indexOf('{');
                    const jsonEnd = this.findMatchingBrace(cleaned, jsonStart);

                    if (jsonStart !== -1 && jsonEnd !== -1) {
                        cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
                    }

                    // 3. trailing comma 제거
                    cleaned = cleaned.replace(/,(\s*[}\]])/g, '$1');

                    console.log('🔥 정제된 JSON:', cleaned.substring(0, 100));
                    return cleaned;
                }

                /**
                 * 유효한 정책 데이터인지 확인
                 */
                isValidPolicyData(data) {
                    return data && (data.policyName || data.roleIds || data.permissionIds);
                }

                /**
                 * 텍스트에서 기본 정보 추출 (최후의 수단)
                 */
                extractDataFromText(text) {
                    console.log('🔥 텍스트에서 데이터 추출 시도');

                    const extractedData = {
                        policyName: "AI 생성 정책",
                        description: "AI가 분석한 정책입니다",
                        roleIds: [],
                        permissionIds: [],
                        conditions: {},
                        aiRiskAssessmentEnabled: false,
                        requiredTrustScore: 0.7,
                        customConditionSpel: "",
                        effect: "ALLOW"
                    };

                    // 🔥 향상된 키워드 기반 매핑
                    if (window.allRoles) {
                        window.allRoles.forEach(role => {
                            const roleName = role.roleName.toLowerCase();
                            const textLower = text.toLowerCase();

                            if (textLower.includes(roleName) ||
                                (textLower.includes('개발') && roleName.includes('개발')) ||
                                (textLower.includes('관리자') && roleName.includes('관리')) ||
                                (textLower.includes('사용자') && roleName.includes('사용자')) ||
                                (textLower.includes('팀') && roleName.includes('팀'))) {
                                extractedData.roleIds.push(role.id);
                                extractedData.policyName = `${role.roleName} 접근 정책`;
                                console.log('🔥 역할 키워드 매핑:', role.roleName, role.id);
                            }
                        });
                    }

                    if (window.allPermissions) {
                        window.allPermissions.forEach(permission => {
                            const permName = (permission.friendlyName || permission.name).toLowerCase();
                            const textLower = text.toLowerCase();

                            if (textLower.includes(permName) ||
                                (textLower.includes('조회') && permName.includes('조회')) ||
                                (textLower.includes('데이터') && permName.includes('데이터')) ||
                                (textLower.includes('고객') && permName.includes('고객')) ||
                                (textLower.includes('수정') && permName.includes('수정')) ||
                                (textLower.includes('삭제') && permName.includes('삭제')) ||
                                (textLower.includes('읽기') && permName.includes('읽기'))) {
                                extractedData.permissionIds.push(permission.id);
                                console.log('🔥 권한 키워드 매핑:', permission.friendlyName, permission.id);
                            }
                        });
                    }

                    if (window.allConditions) {
                        window.allConditions.forEach(condition => {
                            const condName = condition.name.toLowerCase();
                            const textLower = text.toLowerCase();

                            if ((textLower.includes('업무시간') || textLower.includes('평일') || textLower.includes('근무시간')) &&
                                (condName.includes('업무') || condName.includes('시간'))) {
                                extractedData.conditions[condition.id] = [];
                                console.log('🔥 조건 키워드 매핑:', condition.name, condition.id);
                            }
                        });
                    }

                    // 최소한 하나의 구성 요소가 있는 경우에만 반환
                    if (extractedData.roleIds.length > 0 || extractedData.permissionIds.length > 0) {
                        console.log('🔥 텍스트에서 추출된 데이터:', extractedData);
                        return extractedData;
                    }

                    return null;
                }

                handleParsedAIData(jsonData) {
                    console.log('🔥 파싱된 데이터 처리:', jsonData);
                    console.log('🔥 실제 받은 매핑 데이터:');
                    console.log('- roleIdToNameMap:', jsonData.roleIdToNameMap);
                    console.log('- permissionIdToNameMap:', jsonData.permissionIdToNameMap);
                    console.log('- conditionIdToNameMap:', jsonData.conditionIdToNameMap);

                    // 실제 이름을 조회하여 매핑하는 함수
                    // 기존 코드를 이렇게 수정
                    const buildNameMaps = (jsonData) => {
                        const maps = {
                            roles: {},
                            permissions: {},
                            conditions: {}
                        };

                        // 🔥 역할 이름 매핑 개선
                        if (jsonData.roleIds && window.allRoles) {
                            jsonData.roleIds.forEach(id => {
                                console.log(`🔥 역할 ID ${id} 찾는 중... (타입: ${typeof id})`);

                                // 모든 가능한 방법으로 찾기
                                let role = window.allRoles.find(r => r.id === Number(id)) ||
                                    window.allRoles.find(r => r.id == id) ||
                                    window.allRoles.find(r => String(r.id) === String(id));

                                if (role) {
                                    // 모든 가능한 이름 필드 시도
                                    const roleName = role.roleName || role.name || role.displayName || `역할${id}`;
                                    maps.roles[id] = roleName;
                                    console.log(`🔥 역할 매핑 성공: ID=${id}, Name=${roleName}, 전체객체:`, role);
                                } else {
                                    maps.roles[id] = `역할 (ID: ${id})`;
                                    console.log(`🔥 역할 매핑 실패: ID=${id} - 해당 역할 없음`);
                                    console.log('사용 가능한 역할들:', window.allRoles.map(r => ({ id: r.id, type: typeof r.id })));
                                }
                            });
                        }

                        // 🔥 권한 이름 매핑 개선
                        if (jsonData.permissionIds && window.allPermissions) {
                            jsonData.permissionIds.forEach(id => {
                                const permission = window.allPermissions.find(p => p.id === Number(id));
                                if (permission) {
                                    maps.permissions[id] = permission.friendlyName;  // 실제 이름 사용
                                    console.log(`🔥 권한 매핑 성공: ID=${id}, Name=${permission.friendlyName}`);
                                } else {
                                    maps.permissions[id] = `권한 (ID: ${id})`;  // fallback
                                    console.log(`🔥 권한 매핑 실패: ID=${id}`);
                                }
                            });
                        }

                        // 조건은 기존과 동일...
                        if (jsonData.conditions && window.allConditions) {
                            Object.keys(jsonData.conditions).forEach(id => {
                                const condition = window.allConditions.find(c => c.id === Number(id));
                                maps.conditions[id] = condition ? condition.name : `조건 (ID: ${id})`;
                            });
                        }

                        return maps;
                    };

                    // AiGeneratedPolicyDraftDto 형식으로 변환
                    const maps = buildNameMaps(jsonData);
                    const mockDto = {
                        policyData: jsonData,
                        roleIdToNameMap: maps.roles,
                        permissionIdToNameMap: maps.permissions,
                        conditionIdToNameMap: maps.conditions
                    };

                    this.populateBuilderWithAIData(mockDto);
                    this.showMessage('AI 정책 초안이 생성되었습니다!', 'success');
                }

                populateBuilderWithAIData(draftDto) {
                    console.log('🔥 AI 데이터로 빌더 채우기:', draftDto);

                    console.log('🔥 서버에서 온 roleIdToNameMap:', draftDto.roleIdToNameMap);
                    console.log('🔥 서버에서 온 permissionIdToNameMap:', draftDto.permissionIdToNameMap);
                    console.log('🔥 서버에서 온 conditionIdToNameMap:', draftDto.conditionIdToNameMap);
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
                    if (window.resourceContext?.availableVariables) {
                        const availableVars = new Set(window.resourceContext.availableVariables);
                        if (this.elements.conditionsPalette) {
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