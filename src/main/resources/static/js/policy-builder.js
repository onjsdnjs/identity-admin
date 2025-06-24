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

                // 🔥 간단하고 견고한 스트리밍 구현
                // 🔥 개선된 스트리밍 처리 로직
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
                        let cleanFullText = ''; // 🔥 data: 제거된 텍스트
                        const reader = response.body.getReader();
                        const decoder = new TextDecoder('utf-8');

                        // 초기 로그 메시지 표시
                        if (thoughtLog) {
                            thoughtLog.innerHTML = '<div style="color: #6c757d; font-style: italic;">🤖 AI가 정책을 분석하고 있습니다...</div><br>';
                        }

                        console.log('🔥 스트리밍 읽기 시작...');

                        while (true) {
                            const { value, done } = await reader.read();

                            if (done) {
                                console.log('🔥 스트리밍 완료');
                                break;
                            }

                            const chunk = decoder.decode(value, { stream: true });
                            console.log('🔥 수신된 청크:', JSON.stringify(chunk));

                            // SSE 형식 파싱
                            const lines = chunk.split('\n');

                            for (const line of lines) {
                                console.log('🔥 처리할 라인:', JSON.stringify(line));

                                if (line.startsWith('data: ')) {
                                    const data = line.substring(6).trim();
                                    console.log('🔥 추출된 데이터:', JSON.stringify(data));

                                    if (data && data !== '[DONE]') {
                                        fullText += data;
                                        cleanFullText += data; // 🔥 data: 접두사 없이 저장

                                        // 실시간 표시
                                        if (thoughtLog) {
                                            this.displayStreamingData(thoughtLog, data);
                                        }
                                    } else if (data === '[DONE]') {
                                        console.log('🔥 스트리밍 완료 신호 수신');
                                        break;
                                    }
                                } else if (line.startsWith('event: ') || line.startsWith('id: ')) {
                                    // SSE 메타데이터는 무시
                                    continue;
                                } else if (line.trim() === '') {
                                    // 빈 라인은 무시
                                    continue;
                                } else {
                                    // data: 접두사 없는 데이터도 처리 (서버 설정에 따라)
                                    const trimmedLine = line.trim();
                                    if (trimmedLine && trimmedLine !== '[DONE]') {
                                        fullText += trimmedLine;
                                        cleanFullText += trimmedLine; // 🔥 깨끗한 텍스트에도 추가
                                        if (thoughtLog) {
                                            this.displayStreamingData(thoughtLog, trimmedLine);
                                        }
                                    }
                                }
                            }
                        }

                        console.log('🔥 스트리밍 완료, 전체 길이:', fullText.length);
                        console.log('🔥 깨끗한 텍스트 길이:', cleanFullText.length);
                        console.log('🔥 깨끗한 텍스트 미리보기:', cleanFullText.substring(0, 300) + '...');

                        // 🔥 깨끗한 텍스트로 JSON 추출 시도
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

// 🔥 스트리밍 데이터 표시 메서드 분리
                displayStreamingData(thoughtLog, data) {
                    try {
                        // HTML 안전 처리
                        let displayData = data
                            .replace(/&/g, '&amp;')
                            .replace(/</g, '&lt;')
                            .replace(/>/g, '&gt;');

                        // 스마트한 개행 처리
                        displayData = displayData
                            .replace(/\*\*([^*]+)\*\*/g, '<br><br><strong>$1</strong><br>')
                            .replace(/([.!?])([가-힣A-Z])/g, '$1<br><br>$2')
                            .replace(/([a-z])([A-Z가-힣])/g, '$1<br>$2')
                            .replace(/(\d)([가-힣A-Z])/g, '$1<br>$2')
                            .replace(/([가-힣])([A-Z])/g, '$1<br>$2')
                            .replace(/→/g, '<br>→ ')
                            .replace(/(\([^)]*\))/g, '<br>$1<br>')
                            .replace(/:/g, ':<br>')
                            .replace(/JSON/g, '<br><span style="color: #007acc; font-weight: bold;">JSON</span><br>')
                            .replace(/<br>{2,}/g, '<br><br>');

                        // 키워드 색상 강조
                        displayData = displayData
                            .replace(/분석|구성|매핑/g, '<span style="color: #28a745;">🔍 $&</span>')
                            .replace(/역할|권한|조건/g, '<span style="color: #fd7e14;">📋 $&</span>')
                            .replace(/정책/g, '<span style="color: #dc3545;">🎯 $&</span>');

                        thoughtLog.innerHTML += displayData;
                        thoughtLog.scrollTop = thoughtLog.scrollHeight;

                    } catch (error) {
                        console.error('스트리밍 데이터 표시 오류:', error);
                        thoughtLog.innerHTML += data; // 오류 시 원본 텍스트 표시
                    }
                }

                // 🔥 간단한 JSON 추출 (복잡한 로직 제거)
                extractSimpleJson(text) {
                    console.log('🔥 간단 JSON 추출 시도...');
                    console.log('🔥 전체 텍스트 길이:', text.length);
                    console.log('🔥 텍스트 끝부분 500자:', text.substring(Math.max(0, text.length - 500)));
                    
                    try {
                        // 1. JSON 마커 방식 (다양한 패턴)
                        const markerPatterns = [
                            // 🔥 서버에서 사용하는 한국어 마커 (가장 우선)
                            /===JSON시작===([\s\S]*?)===JSON끝===/,
                            /===JSON시작===([\s\S]*)/,  // 끝 마커가 없는 경우
                            /([\s\S]*?)===JSON끝===/,   // 시작 마커가 없는 경우
                            
                            // 기존 영어 마커들
                            /<<JSON_START>>([\s\S]*?)<<JSON_END>>/,
                            /<<<JSON_START>>>([\s\S]*?)<<<JSON_END>>>/,
                            /JSON_START([\s\S]*?)JSON_END/,
                            /\*\*JSON\*\*([\s\S]*?)\*\*\/JSON\*\*/,
                        ];
                        
                        for (const pattern of markerPatterns) {
                            const match = text.match(pattern);
                            if (match) {
                                try {
                                    const jsonStr = match[1].trim();
                                    console.log('🔥 마커로 추출된 JSON:', jsonStr.substring(0, 200) + '...');
                                    
                                    // JSON 유효성 검사 전에 간단한 정제
                                    let cleanedJson = jsonStr
                                        .replace(/```json\s*/g, '')  // 마크다운 제거
                                        .replace(/```\s*/g, '')      // 마크다운 제거
                                        .replace(/^[^{]*({.*})[^}]*$/s, '$1')  // 앞뒤 잡다한 텍스트 제거
                                        .trim();
                                    
                                    const parsed = JSON.parse(cleanedJson);
                                    console.log('🔥 마커 JSON 파싱 성공:', parsed);
                                    return parsed;
                                } catch (e) {
                                    console.log('🔥 마커 JSON 파싱 실패:', e.message);
                                    console.log('🔥 실패한 JSON 내용:', match[1]?.substring(0, 100) + '...');
                                    continue;
                                }
                            }
                        }
                        
                        // 2. 중괄호 기반 추출 (더 관대하게)
                        const jsonCandidates = [];
                        
                        // 2-1. 가장 큰 중괄호 블록 찾기
                        let maxStart = -1, maxEnd = -1, maxLength = 0;
                        
                        for (let i = 0; i < text.length; i++) {
                            if (text[i] === '{') {
                                const end = this.findMatchingBrace(text, i);
                                if (end > i) {
                                    const length = end - i + 1;
                                    if (length > maxLength) {
                                        maxStart = i;
                                        maxEnd = end;
                                        maxLength = length;
                                    }
                                    
                                    // 후보로 추가
                                    const candidate = text.substring(i, end + 1);
                                    if (candidate.length > 50) { // 너무 짧은 건 제외
                                        jsonCandidates.push(candidate);
                                    }
                                }
                            }
                        }
                        
                        console.log('🔥 JSON 후보 개수:', jsonCandidates.length);
                        
                        // 2-2. 후보들을 시도 (긴 것부터)
                        jsonCandidates.sort((a, b) => b.length - a.length);
                        
                        for (const candidate of jsonCandidates) {
                            try {
                                console.log('🔥 JSON 후보 시도:', candidate.substring(0, 100) + '...');
                                const parsed = JSON.parse(candidate);
                                
                                // policyData 또는 roleIds가 있으면 유효한 응답으로 간주
                                if (parsed.policyData || parsed.roleIds || parsed.policyName) {
                                    console.log('🔥 유효한 JSON 발견:', parsed);
                                    return parsed;
                                }
                            } catch (e) {
                                console.log('🔥 JSON 후보 파싱 실패:', e.message);
                                continue;
                            }
                        }
                        
                        // 3. 키워드 기반 추출 (한국어 패턴)
                        const patterns = [
                            /"policyName"[\s\S]*?"effect"[\s\S]*?"ALLOW"/,
                            /"roleIds"[\s\S]*?\[[\s\S]*?\]/,
                            /"permissionIds"[\s\S]*?\[[\s\S]*?\]/,
                            /\{[\s\S]*?"policyName"[\s\S]*?\}/,
                            // 🔥 깨진 응답에서 자주 나타나는 패턴들 추가
                            /"고객데이터조회정책"[\s\S]*?"ALLOW"/,
                            /"평업무.*고객.*데이터.*조회"[\s\S]*?\[[\s\S]*?\]/,
                            /["'](\d+)["'][\s\S]*?false[\s\S]*?[",]/  // ID 패턴
                        ];
                        
                        for (const pattern of patterns) {
                            const match = text.match(pattern);
                            if (match) {
                                try {
                                    console.log('🔥 패턴 매치:', pattern.toString());
                                    console.log('🔥 매치된 내용:', match[0]);
                                    
                                    // 매치된 부분을 확장해서 완전한 JSON 찾기
                                    const matchStart = text.indexOf(match[0]);
                                    
                                    // 🔥 더 관대한 JSON 경계 찾기
                                    let jsonStart = matchStart;
                                    let jsonEnd = matchStart + match[0].length - 1;
                                    
                                    // 앞쪽에서 { 찾기 (더 멀리까지)
                                    for (let i = matchStart - 1; i >= Math.max(0, matchStart - 200); i--) {
                                        if (text[i] === '{') {
                                            jsonStart = i;
                                            break;
                                        }
                                    }
                                    
                                    // 뒤쪽에서 } 찾기 (더 멀리까지)
                                    for (let i = jsonEnd; i < Math.min(text.length, jsonEnd + 200); i++) {
                                        if (text[i] === '}') {
                                            jsonEnd = i;
                                            break;
                                        }
                                    }
                                    
                                    if (jsonStart < jsonEnd) {
                                        const expandedJson = text.substring(jsonStart, jsonEnd + 1);
                                        console.log('🔥 확장된 JSON 시도:', expandedJson);
                                        
                                        // �� JSON 수정 시도 (일반적인 오류 패턴 수정)
                                        let fixedJson = expandedJson
                                            .replace(/["'](\d+)["']\s*:\s*\[/g, '"$1": [')  // ID 키 정규화
                                            .replace(/,(\s*[}\]])/g, '$1')                   // 끝의 잉여 콤마 제거
                                            .replace(/([}\]])\s*,/g, '$1')                   // 잉여 콤마 제거
                                            .replace(/"\s*,\s*"/g, '", "')                   // 문자열 간 콤마 정규화
                                            .replace(/:\s*"([^"]*)"(\s*[,}\]])/g, ': "$1"$2'); // 문자열 값 정규화
                                        
                                        const parsed = JSON.parse(fixedJson);
                                        console.log('🔥 패턴 기반 JSON 성공:', parsed);
                                        return parsed;
                                    }
                                } catch (e) {
                                    console.log('🔥 패턴 기반 JSON 실패:', e.message);
                                    continue;
                                }
                            }
                        }
                        
                        console.warn('🔥 JSON 추출 실패 - 모든 방법 시도함');
                        return null;
                        
                    } catch (error) {
                        console.error('🔥 JSON 추출 오류:', error);
                        return null;
                    }
                }

                // 🔥 중괄호 매칭 헬퍼 메서드
                findMatchingBrace(text, start) {
                    if (start >= text.length || text[start] !== '{') {
                        return -1;
                    }
                    
                    let braceCount = 1;
                    for (let i = start + 1; i < text.length; i++) {
                        if (text[i] === '{') {
                            braceCount++;
                        } else if (text[i] === '}') {
                            braceCount--;
                            if (braceCount === 0) {
                                return i;
                            }
                        }
                    }
                    return -1;
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