/**
 * [최종 리팩토링] 시각적 정책 빌더 애플리케이션
 * 역할을 분리하여 코드의 구조를 개선하고 유지보수성을 높입니다.
 * - PolicyBuilderState: 애플리케이션의 상태 관리
 * - PolicyBuilderUI: 모든 DOM 조작 및 렌더링 담당
 * - PolicyBuilderAPI: 서버와의 모든 통신 담당
 * - PolicyBuilderApp: 위 컴포넌트들을 조정하는 메인 컨트롤러
 */

// 1. 상태 관리 클래스
class PolicyBuilderState {
    constructor() {
        this.subjects = new Map();
        this.permissions = new Map();
        this.conditions = new Map();
    }

    add(type, key, value) {
        this.getMap(type).set(key, value);
    }

    remove(type, key) {
        this.getMap(type).delete(key);
    }

    getMap(type) {
        if (type === 'subject') return this.subjects;
        if (type === 'permission') return this.permissions;
        if (type === 'condition') return this.conditions;
        throw new Error('Invalid state type');
    }

    toDto() {
        return {
            name: document.getElementById('policy-name').value,
            description: document.getElementById('policy-desc').value,
            effect: document.getElementById('policy-effect').value,
            subjects: Array.from(this.subjects.keys()).map(key => {
                const [type, id] = key.split(':');
                return { id: Number(id), type };
            }),
            permissions: Array.from(this.permissions.keys()).map(id => ({ id: Number(id) })),
            conditions: Array.from(this.conditions.keys()).map(id => ({ conditionKey: id, params: {} }))
        };
    }
}

// 2. UI 렌더링 클래스
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
        if (map.size === 0) {
            canvasEl.innerHTML = `<span class="text-gray-400">왼쪽에서 ${this.getKoreanTypeName(type)}을(를) 선택하세요.</span>`;
            return;
        }
        map.forEach((value, key) => {
            const chip = document.createElement('span');
            chip.className = 'policy-chip bg-indigo-100 text-indigo-800 text-sm font-medium mr-2 mb-2 px-3 py-1 rounded-full flex items-center shadow-sm';
            chip.dataset.key = key;
            chip.innerHTML = `${value.name} <button class="ml-2 font-bold text-indigo-400 hover:text-indigo-700">&times;</button>`;
            canvasEl.appendChild(chip);
        });
    }

    updatePreview(state) {
        const subjects = Array.from(state.subjects.values()).map(s => `<strong>${s.name}</strong>`).join(', ');
        const permissions = Array.from(state.permissions.values()).map(p => `<strong>${p.name}</strong>`).join(', ');
        const conditions = Array.from(state.conditions.values()).map(c => `<strong>${c.name}</strong>`).join(' 그리고 ');
        const effect = document.getElementById('policy-effect').value === 'ALLOW' ? '허용' : '거부';

        let previewText = '';
        if (subjects) {
            previewText += `[${subjects}] 주체는 `;
        } else {
            previewText += `[모든 주체]는 `;
        }
        if (permissions) {
            previewText += `[${permissions}] 권한에 대해 `;
        } else {
            previewText += `[모든 권한]에 대해 `;
        }
        if (conditions) {
            previewText += `[${conditions}] 조건 하에 `;
        }
        previewText += `접근을 <strong class="<span class="math-inline">\{effect \=\=\= '허용' ? 'text\-green\-600' \: 'text\-red\-600'\}"\></span>{effect}</strong>합니다.`;

        this.elements.policyPreview.innerHTML = previewText;
    }

    setLoading(isLoading) {
        this.elements.saveBtn.disabled = isLoading;
        this.elements.saveBtn.innerHTML = isLoading
            ? '<i class="fas fa-spinner fa-spin mr-2"></i> 저장 중...'
            : '<i class="fas fa-save mr-2"></i> 정책 저장하기';
    }

    getKoreanTypeName(type) {
        if (type === 'subject') return '주체';
        if (type === 'permission') return '권한';
        if (type === 'condition') return '조건';
        return '';
    }
}

// 3. API 통신 클래스
class PolicyBuilderAPI {
    async buildPolicy(dto) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const response = await fetch('/admin/policy-builder/api/build', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(dto)
        });
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '정책 생성에 실패했습니다.');
        }
        return response.json();
    }
}

// 4. 메인 애플리케이션 클래스
class PolicyBuilderApp {
    constructor() {
        this.state = new PolicyBuilderState();
        this.ui = new PolicyBuilderUI({
            subjectsPalette: document.getElementById('subjects-palette'),
            permissionsPalette: document.getElementById('permissions-palette'),
            conditionsPalette: document.getElementById('conditions-palette'),
            subjectsCanvas: document.getElementById('subjects-canvas'),
            permissionsCanvas: document.getElementById('permissions-canvas'),
            conditionsCanvas: document.getElementById('conditions-canvas'),
            policyPreview: document.getElementById('policy-preview'),
            saveBtn: document.getElementById('save-policy-btn')
        });
        this.api = new PolicyBuilderAPI();
        this.init();
    }

    init() {
        this.bindEventListeners();
        this.ui.renderAll(this.state);
    }

    bindEventListeners() {
        this.ui.elements.subjectsPalette.addEventListener('click', e => this.handlePaletteClick(e, 'subject'));
        this.ui.elements.permissionsPalette.addEventListener('click', e => this.handlePaletteClick(e, 'permission'));
        this.ui.elements.conditionsPalette.addEventListener('click', e => this.handlePaletteClick(e, 'condition'));

        this.ui.elements.subjectsCanvas.addEventListener('click', e => this.handleChipRemove(e, 'subject'));
        this.ui.elements.permissionsCanvas.addEventListener('click', e => this.handleChipRemove(e, 'permission'));
        this.ui.elements.conditionsCanvas.addEventListener('click', e => this.handleChipRemove(e, 'condition'));

        document.getElementById('policy-effect').addEventListener('change', () => this.ui.updatePreview(this.state));
        this.ui.elements.saveBtn.addEventListener('click', () => this.savePolicy());
    }

    handlePaletteClick(event, type) {
        const item = event.target.closest('.palette-item');
        if (!item) return;

        const [id, ...nameParts] = item.dataset.info.split(':');
        const name = nameParts.join(':');
        const key = (type === 'subject') ? item.dataset.info : id;

        this.state.add(type, key, { id, name });
        this.ui.renderAll(this.state);
    }

    handleChipRemove(event, type) {
        const chip = event.target.closest('.policy-chip');
        if (!chip) return;
        const key = chip.dataset.key;

        this.state.remove(type, key);
        this.ui.renderAll(this.state);
    }

    async savePolicy() {
        const dto = this.state.toDto();
        if (!dto.name || dto.subjects.length === 0 || dto.permissions.length === 0) {
            showToast("정책 이름, 주체, 권한은 필수 항목입니다.", "error");
            return;
        }

        this.ui.setLoading(true);
        try {
            const createdPolicy = await this.api.buildPolicy(dto);
            showToast(`성공: 정책 "${createdPolicy.name}" (ID: ${createdPolicy.id}) 이(가) 생성되었습니다.`, 'success');
            setTimeout(() => window.location.href = '/admin/policies', 1500);
        } catch (error) {
            showToast(error.message, 'error');
        } finally {
            this.ui.setLoading(false);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // toast.js가 로드되어 있다고 가정
    if(typeof showToast !== 'function') {
        window.showToast = (message, type) => alert(`[${type}] ${message}`);
    }
    new PolicyBuilderApp();
});