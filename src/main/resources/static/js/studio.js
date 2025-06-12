/**
 * [최종 리팩토링] Authorization Studio 클라이언트 애플리케이션
 * 역할을 분리하여 코드의 구조를 개선하고 유지보수성을 높입니다.
 * - StudioState: 애플리케이션의 상태 관리
 * - StudioUI: 모든 DOM 조작 및 렌더링 담당
 * - StudioAPI: 서버와의 모든 통신 담당
 * - StudioApp: 위 컴포넌트들을 조정하는 메인 컨트롤러
 */

// 1. 상태 관리 클래스
class StudioState {
    constructor() {
        this.selected = {
            USER: null,
            GROUP: null,
            PERMISSION: null,
            POLICY: null
        };
    }

    /**
     * 아이템 선택 상태를 업데이트(토글)합니다.
     * @param {string} type - 아이템 타입 (USER, GROUP 등)
     * @param {object} item - 선택된 아이템 정보 {id, name, description, type}
     */
    select(type, item) {
        // 이미 선택된 아이템을 다시 클릭하면 선택 해제
        if (this.selected[type] && this.selected[type].id === item.id) {
            this.selected[type] = null;
        } else {
            this.selected[type] = item;
        }

        // 주체(Subject)는 사용자 또는 그룹 중 하나만 선택되도록 보장합니다.
        if (type === 'USER' && this.selected.USER) {
            this.selected.GROUP = null;
        }
        if (type === 'GROUP' && this.selected.GROUP) {
            this.selected.USER = null;
        }
    }

    getSubject() {
        return this.selected.USER || this.selected.GROUP;
    }

    getPermission() {
        return this.selected.PERMISSION;
    }

    getSelectedItemsForAction() {
        const subject = this.getSubject();
        const permission = this.getPermission();
        if (!subject || !permission) return null;

        const request = {
            userIds: subject.type === 'USER' ? [subject.id] : [],
            groupIds: subject.type === 'GROUP' ? [subject.id] : [],
            permissionIds: [permission.id]
        };
        return request;
    }
}

// 2. UI 렌더링 및 조작 클래스
class StudioUI {
    constructor(elements) {
        this.elements = elements;
    }

    renderExplorer(data) {
        const usersHtml = this.createSectionHtml('사용자', data.users, 'USER');
        const groupsHtml = this.createSectionHtml('그룹', data.groups, 'GROUP');
        const permissionsHtml = this.createSectionHtml('권한', data.permissions, 'PERMISSION');
        const policiesHtml = this.createSectionHtml('정책', data.policies, 'POLICY');
        this.elements.explorerListContainer.innerHTML = usersHtml + groupsHtml + permissionsHtml + policiesHtml;
    }

    createSectionHtml(title, items, type) {
        if (!items || items.length === 0) return '';
        const itemsHtml = items.map(item => `
            <div class="explorer-item" data-id="${item.id}" data-type="${type}" data-name="${item.name}" data-description="${item.description || ''}">
                <div class="item-name">${item.name}</div>
                <div class="item-description" title="${item.description || ''}">${item.description || ''}</div>
            </div>
        `).join('');
        return `<h3 class="explorer-header">${title}</h3><div class="explorer-section-body">${itemsHtml}</div>`;
    }

    updateExplorerSelection(state) {
        this.elements.explorerListContainer.querySelectorAll('.explorer-item').forEach(el => {
            const { id, type } = el.dataset;
            const selectedItem = state.selected[type];
            el.classList.toggle('selected', selectedItem && selectedItem.id == id);
        });
    }

    renderAccessPath(subject, permission, data) {
        const pathHtml = data.nodes.map((node, index) => `
            <div class="path-node">
                <div class="path-icon">${index + 1}</div>
                <div class="path-details">
                    <div class="path-type">${node.type}</div>
                    <div class="path-name">${node.name}</div>
                </div>
            </div>
            ${index < data.nodes.length - 1 ? '<div class="path-arrow"><i class="fas fa-arrow-down"></i></div>' : ''}
        `).join('');
        const resultColor = data.accessGranted ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
        const resultIcon = data.accessGranted ? 'fa-check-circle' : 'fa-times-circle';
        this.elements.canvasContent.innerHTML = `
            <h2 class="text-xl font-bold mb-2 text-center">접근 경로 분석</h2>
            <p class="text-sm text-center text-slate-500 mb-6">'${subject.name}' <i class="fas fa-long-arrow-alt-right mx-2"></i> '${permission.name}'</p>
            <div class="path-container">${pathHtml}</div>
            <div class="mt-6 p-4 rounded-lg text-center ${resultColor}">
                <h3 class="font-semibold">최종 결론</h3>
                <p class="text-lg font-bold"><i class="fas ${resultIcon} mr-2"></i>${data.finalReason}</p>
            </div>`;
    }

    renderEffectivePermissions(subject, data) {
        const permsHtml = data.map(perm => `
            <div class="p-3 border rounded-md mb-2 bg-white hover:shadow-md transition-shadow">
                <p class="font-semibold text-gray-800">${perm.permissionDescription}</p>
                <p class="text-sm text-slate-600 mt-1">획득 경로: <span class="font-mono text-xs bg-slate-100 p-1 rounded">${perm.origin}</span></p>
            </div>`).join('');
        this.elements.canvasContent.innerHTML = `
            <h2 class="text-xl font-bold mb-4">'${subject.name}'의 유효 권한 목록</h2>
            <div class="space-y-2">${permsHtml.length > 0 ? permsHtml : '<p class="text-slate-500 p-4 bg-slate-50 rounded-md text-center">부여된 권한이 없습니다.</p>'}</div>`;
    }

    renderInspector(state) {
        const subject = state.getSubject();
        const permission = state.getPermission();

        this.elements.inspectorContent.innerHTML = '';

        let detailsHtml = '<h3 class="font-bold text-lg mb-2">선택된 항목</h3><div class="space-y-2">';
        let hasSelection = false;

        Object.values(state.selected).forEach(item => {
            if (item) {
                detailsHtml += this.buildDetailHtml(item);
                hasSelection = true;
            }
        });
        detailsHtml += '</div>';

        let actionsHtml = '<h3 class="font-bold text-lg mb-2 mt-4 border-t pt-4">관련 작업</h3>';
        if (subject && permission) {
            actionsHtml += `<button id="grant-btn" class="w-full btn-primary text-sm py-2">권한 부여 마법사 시작</button>`;
        } else {
            actionsHtml += '<p class="text-sm text-slate-500">주체와 권한을 함께 선택하여<br/>권한을 부여할 수 있습니다.</p>';
        }

        if (hasSelection) {
            this.elements.inspectorContent.innerHTML = detailsHtml + actionsHtml;
            this.elements.inspectorPlaceholder.classList.add('hidden');
            this.elements.inspectorContent.classList.remove('hidden');
        } else {
            this.elements.inspectorContent.classList.add('hidden');
            this.elements.inspectorPlaceholder.classList.remove('hidden');
        }
    }

    buildDetailHtml(item) {
        const icon = { USER: 'fa-user', GROUP: 'fa-users', PERMISSION: 'fa-key', POLICY: 'fa-file-alt' }[item.type] || 'fa-question-circle';
        return `
            <div class="p-3 bg-white rounded-md border text-sm">
                <p class="text-xs font-semibold uppercase text-app-accent"><i class="fas ${icon} mr-2"></i>${item.type}</p>
                <p class="font-bold text-md text-gray-800">${item.name}</p>
            </div>
        `;
    }

    resetCanvas() {
        this.elements.canvasContent.classList.add('hidden');
        this.elements.canvasContent.innerHTML = '';
        this.elements.canvasPlaceholder.classList.remove('hidden');
    }

    showLoading(element) {
        this.elements.canvasPlaceholder.classList.add('hidden');
        this.elements.canvasContent.classList.remove('hidden');
        element.innerHTML = '<div class="flex items-center justify-center p-8 h-full"><i class="fas fa-spinner fa-spin text-3xl text-app-primary"></i></div>';
    }

    showError(element, message) {
        element.innerHTML = `<div class="p-4 text-center text-red-500">${message}</div>`;
    }
}

// 3. API 통신 클래스
class StudioAPI {
    async fetchApi(url, options = {}) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const fetchOptions = { ...options };
            if (!fetchOptions.headers) fetchOptions.headers = {};
            if (options.body && !(options.body instanceof URLSearchParams)) {
                fetchOptions.headers['Content-Type'] = 'application/json';
            }
            if (csrfToken && csrfHeader && options.method && options.method.toUpperCase() !== 'GET') {
                fetchOptions.headers[csrfHeader] = csrfToken;
            }
            const response = await fetch(url, fetchOptions);
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류 (상태: ${response.status})`}));
                throw new Error(errorData.message);
            }
            if(response.redirected) {
                window.location.href = response.url;
                return null;
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            console.error(`API Error fetching ${url}:`, error);
            if(typeof showToast === 'function') showToast(error.message || 'API 통신 오류', 'error');
            throw error;
        }
    }
    getExplorerItems() { return this.fetchApi('/admin/studio/api/explorer-items'); }
    getAccessPath(subjectId, subjectType, permissionId) { return this.fetchApi(`/admin/studio/api/access-path?subjectId=${subjectId}&subjectType=${subjectType}&permissionId=${permissionId}`); }
    getEffectivePermissions(subjectId, subjectType) { return this.fetchApi(`/admin/studio/api/effective-permissions?subjectId=${subjectId}&subjectType=${subjectType}`); }

    // 이 메서드는 직접 호출되지 않고, form submit으로 대체됩니다.
    // initiateGrant(request) {
    //     return this.fetchApi('/admin/policy-wizard/start', {
    //         method: 'POST',
    //         body: JSON.stringify(request)
    //     });
    // }
}

// 4. 메인 애플리케이션 클래스
class StudioApp {
    constructor() {
        this.elements = {
            explorerListContainer: document.getElementById('explorer-list-container'),
            loader: document.getElementById('explorer-loader'),
            canvasPanel: document.getElementById('canvas-panel'),
            canvasPlaceholder: document.getElementById('canvas-placeholder'),
            canvasContent: document.getElementById('canvas-content'),
            inspectorPanel: document.getElementById('inspector-panel'),
            inspectorPlaceholder: document.getElementById('inspector-placeholder'),
            inspectorContent: document.getElementById('inspector-content'),
            search: document.getElementById('explorer-search'),
        };
        this.state = new StudioState();
        this.ui = new StudioUI(this.elements);
        this.api = new StudioAPI();
        this.init();
    }

    init() {
        this.bindEventListeners();
        this.loadInitialData();
    }

    async loadInitialData() {
        this.ui.showLoading(this.ui.elements.explorerListContainer);
        try {
            const data = await this.api.getExplorerItems();
            this.ui.renderExplorer(data);
        } catch (error) {
            this.ui.showError(this.ui.elements.explorerListContainer, '탐색기 목록 로딩 실패');
        }
    }

    bindEventListeners() {
        this.elements.explorerListContainer.addEventListener('click', e => this.handleExplorerClick(e));
        this.elements.search.addEventListener('input', e => this.filterExplorer(e.target.value.toLowerCase()));
        this.elements.inspectorPanel.addEventListener('click', e => {
            if (e.target.id === 'grant-btn') this.handleGrantClick();
        });
    }

    handleExplorerClick(e) {
        const itemEl = e.target.closest('.explorer-item');
        if (!itemEl) return;
        const { id, type, name, description } = itemEl.dataset;
        this.state.select(type, { id: Number(id), name, description, type });
        this.ui.updateExplorerSelection(this.state);
        this.updateCanvasAndInspector();
    }

    async updateCanvasAndInspector() {
        this.ui.showLoading(this.elements.canvasContent);
        this.ui.renderInspector(this.state); // Inspector 먼저 렌더링

        const subject = this.state.getSubject();
        const permission = this.state.getPermission();
        try {
            if (subject && permission) {
                const data = await this.api.getAccessPath(subject.id, subject.type, permission.id);
                this.ui.renderAccessPath(subject, permission, data);
            } else if (subject) {
                const data = await this.api.getEffectivePermissions(subject.id, subject.type);
                this.ui.renderEffectivePermissions(subject, data);
            } else {
                this.ui.resetCanvas();
            }
        } catch (error) {
            this.ui.showError(this.ui.elements.canvasContent, '분석 데이터 로딩 실패');
        }
    }

    handleGrantClick() {
        const grantData = this.state.getSelectedItemsForAction();
        if(!grantData) {
            showToast("권한을 부여할 주체와 권한을 모두 선택해주세요.", "error");
            return;
        }

        // Studio에서는 마법사 시작만 담당.
        // 마법사 페이지로 POST 리다이렉트하기 위해 임시 form을 생성하여 submit합니다.
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/admin/policy-wizard/start';

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const dataForForm = {
            ...grantData,
            policyName: `Studio 빠른 권한 부여`,
            policyDescription: `${this.state.getSubject().name}에게 ${this.state.getPermission().name} 권한을 부여합니다.`
        };

        for(const key in dataForForm) {
            const value = dataForForm[key];
            if (Array.isArray(value)) {
                value.forEach(v => {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = key;
                    input.value = v;
                    form.appendChild(input);
                });
            } else {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = value;
                form.appendChild(input);
            }
        }

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = csrfHeader;
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);

        document.body.appendChild(form);
        form.submit();
    }

    filterExplorer(term) {
        this.elements.explorerListContainer.querySelectorAll('.explorer-item').forEach(item => {
            const name = item.dataset.name.toLowerCase();
            const description = item.dataset.description.toLowerCase();
            item.style.display = (name.includes(term) || description.includes(term)) ? '' : 'block';
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // toast.js가 로드되어 있다고 가정
    if(typeof showToast !== 'function') {
        window.showToast = (message, type) => alert(`[${type.toUpperCase()}] ${message}`);
    }
    new StudioApp();
});