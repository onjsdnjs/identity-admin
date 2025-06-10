/**
 * ===================================================================
 * IAM Command Center - Unified Workbench (v3.0 - Final & Working)
 * -------------------------------------------------------------------
 * 이 스크립트는 단일 페이지에서 모든 IAM 관련 관리 작업을 처리합니다.
 * - 상태 기반의 동적 UI 렌더링 (자원, 주체, 역할 뷰)
 * - 컨텍스트를 인식하는 상세 정보 및 액션 패널
 * - 기존 상세 관리 페이지와의 유기적 통합 (상세 편집 링크)
 * - 백엔드 REST API 와의 완전한 비동기 통신 및 오류 처리
 * ===================================================================
 */
class WorkbenchApp {
    /**
     * 애플리케이션 초기화
     */
    constructor() {
        // 1. 애플리케이션의 상태 관리
        this.state = {
            currentView: 'resources',
            selectedItem: null, // {id, type, name}
            debounceTimer: null,
            metadataCache: {} // API 응답 캐싱용 (주체, 역할 등)
        };
        // 2. DOM 요소 캐싱
        this.cacheDOMElements();
        // 3. 로딩 스피너 템플릿
        this.loadingSpinner = `<div class="flex items-center justify-center p-8"><svg class="animate-spin h-8 w-8 text-app-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg></div>`;
        // 4. 이벤트 리스너 바인딩
        this.bindEventListeners();
        // 5. 초기 데이터 로드
        this.init();
    }

    /**
     * UI의 핵심 DOM 요소들을 미리 찾아 변수에 할당합니다.
     */
    cacheDOMElements() {
        this.elements = {
            viewSelector: document.getElementById('view-selector'),
            searchInput: document.getElementById('explorer-search-input'),
            explorerList: document.getElementById('explorer-list'),
            workspace: document.getElementById('workspace'),
            workspacePlaceholder: document.getElementById('workspace-placeholder') ? document.getElementById('workspace-placeholder').cloneNode(true) : null
        };
    }

    /**
     * API 통신을 위한 중앙 헬퍼 함수. CSRF 토큰 및 에러 처리를 포함합니다.
     */
    async fetchAPI(url, options = {}) {
        try {
            const defaultHeaders = { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (csrfToken && csrfHeader) defaultHeaders[csrfHeader] = csrfToken;

            const response = await fetch(url, { ...options, headers: { ...defaultHeaders, ...options.headers } });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 통신 오류 (상태 코드: ${response.status})` }));
                throw new Error(errorData.message || '알 수 없는 오류가 발생했습니다.');
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            showToast(error.message, 'error');
            throw error;
        }
    }

    /**
     * 모든 UI 상호작용을 위한 이벤트 리스너를 등록합니다.
     */
    bindEventListeners() {
        this.elements.viewSelector.addEventListener('change', (e) => this.handleViewChange(e.target.value));
        this.elements.searchInput.addEventListener('input', (e) => {
            clearTimeout(this.state.debounceTimer);
            this.state.debounceTimer = setTimeout(() => this.loadExplorerData(e.target.value), 300);
        });
        this.elements.explorerList.addEventListener('click', (e) => {
            const itemElement = e.target.closest('.explorer-item');
            if (itemElement) this.handleItemSelect(itemElement);
        });
        this.elements.workspace.addEventListener('click', (e) => {
            const revokeButton = e.target.closest('.revoke-btn');
            if (revokeButton) this.handleRevokeClick(revokeButton);
        });
        this.elements.workspace.addEventListener('submit', (e) => {
            if (e.target && e.target.id === 'grant-form') this.handleGrantSubmit(e);
        });
    }

    /**
     * 애플리케이션을 시작합니다.
     */
    init() {
        this.handleViewChange(this.elements.viewSelector.value);
    }

    /**
     * 탐색기 뷰(관점) 변경을 처리합니다.
     */
    handleViewChange(newView) {
        this.state.currentView = newView;
        this.elements.searchInput.value = '';
        const selectedOption = this.elements.viewSelector.querySelector(`option[value="${newView}"]`);
        this.elements.searchInput.placeholder = `${selectedOption.textContent.split(' ')[0]} 검색...`;
        this.clearWorkspace();
        this.loadExplorerData();
    }

    /**
     * 현재 선택된 뷰(관점)에 따라 탐색기 목록을 API로 로드하고 렌더링합니다.
     */
    async loadExplorerData(keyword = '') {
        this.elements.explorerList.innerHTML = this.loadingSpinner;
        let url, data;
        try {
            switch (this.state.currentView) {
                case 'subjects':
                    url = '/api/workbench/metadata/subjects';
                    break;
                case 'roles':
                    url = `/api/workbench/metadata/roles`; // 검색은 백엔드에서 추가 구현 필요
                    break;
                case 'resources':
                default:
                    url = `/api/workbench/resources?keyword=${encodeURIComponent(keyword)}`;
                    break;
            }
            data = await this.fetchAPI(url);
            this.renderExplorerList(data);
        } catch (error) {
            this.elements.explorerList.innerHTML = `<div class="p-4 text-center text-red-500">목록 로딩 실패.</div>`;
        }
    }

    /**
     * 조회된 데이터를 기반으로 탐색기 목록 HTML을 생성하고 표시합니다.
     */
    renderExplorerList(data) {
        this.elements.explorerList.innerHTML = '';
        let itemsHtml = '';
        const keyword = this.elements.searchInput.value.toLowerCase();

        switch (this.state.currentView) {
            case 'resources':
                itemsHtml = (data.content || [])
                    .map(item => this.getExplorerItemHtml(item.id, 'resource', item.friendlyName, item.resourceIdentifier)).join('');
                break;
            case 'subjects':
                itemsHtml += `<div class="explorer-header">그룹</div>`;
                itemsHtml += (data.groups || []).filter(g => g.name.toLowerCase().includes(keyword))
                    .map(item => this.getExplorerItemHtml(item.id, 'group', item.name, item.description)).join('');
                itemsHtml += `<div class="explorer-header">사용자</div>`;
                itemsHtml += (data.users || []).filter(u => u.name.toLowerCase().includes(keyword) || u.username.toLowerCase().includes(keyword))
                    .map(item => this.getExplorerItemHtml(item.id, 'user', item.name, item.username)).join('');
                break;
            case 'roles':
                itemsHtml = (data || []).filter(r => r.roleName.toLowerCase().includes(keyword))
                    .map(item => this.getExplorerItemHtml(item.id, 'role', item.roleName, item.roleDesc)).join('');
                break;
        }
        this.elements.explorerList.innerHTML = itemsHtml || `<div class="p-4 text-center text-slate-500">결과가 없습니다.</div>`;
    }

    getExplorerItemHtml(id, type, title, subtitle) {
        return `
            <div class="explorer-item" data-id="${id}" data-type="${type}" data-name="${title || ''}">
                <p class="font-semibold text-app-dark-gray">${title || '이름 없음'}</p>
                <p class="text-xs text-slate-500 truncate" title="${subtitle || ''}">${subtitle || '설명 없음'}</p>
            </div>`;
    }

    /**
     * 탐색기에서 특정 항목을 선택했을 때의 모든 로직을 처리합니다.
     */
    handleItemSelect(element) {
        const { id, type, name } = element.dataset;
        if (this.state.selectedItem && this.state.selectedItem.id == id && this.state.selectedItem.type === type) return;

        this.state.selectedItem = { id: parseInt(id), type, name };
        document.querySelectorAll('.explorer-item.selected').forEach(el => el.classList.remove('selected'));
        element.classList.add('selected');

        this.renderWorkspace();
    }

    /**
     * 선택된 항목의 타입에 따라 워크스페이스 전체를 다시 렌더링합니다.
     */
    async renderWorkspace() {
        this.elements.workspacePlaceholder?.classList.add('hidden');
        this.elements.workspace.innerHTML = this.loadingSpinner;

        const { id, type, name } = this.state.selectedItem;
        const detailPageUrl = `/admin/${type}s/${id}`;

        const headerHtml = `
            <div class="flex items-start justify-between mb-6 pb-4 border-b">
                <div>
                    <p class="text-sm font-semibold text-app-accent uppercase">${type}</p>
                    <h2 class="text-2xl font-bold text-app-secondary">${name}</h2>
                </div>
                <a href="${detailPageUrl}" target="_blank" title="새 탭에서 전체 수정 페이지 열기" class="action-button bg-gray-600 hover:bg-gray-800">
                    <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
                    상세 관리
                </a>
            </div>`;

        const contentHtml = `
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div>
                    <h3 class="font-bold text-lg text-gray-800 mb-3">권한 현황 요약</h3>
                    <div id="workspace-entitlements" class="space-y-2">${this.loadingSpinner}</div>
                </div>
                <div id="action-panel" class="bg-gray-50 p-4 rounded-lg">
                    <h3 class="font-bold text-lg text-gray-800 mb-3">빠른 작업</h3>
                    <div id="action-form-container"></div>
                </div>
            </div>`;

        this.elements.workspace.innerHTML = headerHtml + contentHtml;
        this.loadAndRenderDetails();
        this.renderActionForm();
    }

    /**
     * 워크스페이스의 권한 현황 데이터를 로드하고 렌더링합니다.
     */
    async loadAndRenderDetails() {
        const container = document.getElementById('workspace-entitlements');
        const { id, type } = this.state.selectedItem;
        try {
            const endpoint = (type === 'resource')
                ? `/api/workbench/entitlements/by-resource?resourceId=${id}`
                : `/api/workbench/entitlements/by-subject?type=${type.toUpperCase()}&id=${id}`;
            const entitlements = await this.fetchAPI(endpoint);

            if (!entitlements || entitlements.length === 0) {
                container.innerHTML = `<div class="p-4 text-center text-slate-500 bg-slate-100 rounded-lg">정의된 접근 권한이 없습니다.</div>`;
                return;
            }

            const cardsHtml = entitlements.map(ent => `
                <div class="border rounded-lg p-3 text-sm">
                    <div class="flex justify-between items-start">
                        <p><strong class="text-gray-800">${ent.subjectName}</strong> (이)가 <strong class="text-gray-800">${ent.resourceName}</strong>에 대해</p>
                        <button data-policy-id="${ent.policyId}" class="revoke-btn text-xs bg-error text-white px-2 py-1 rounded hover:bg-red-700">회수</button>
                    </div>
                    <p class="text-app-accent font-semibold">${ent.actions.join(', ')}</p>
                    <p class="text-xs text-gray-500 mt-1"><strong>규칙:</strong> ${ent.conditions.join(' ')}</p>
                </div>`).join('');
            container.innerHTML = cardsHtml;
        } catch (error) {
            container.innerHTML = `<div class="p-4 text-center text-red-500">권한 정보를 불러오는 데 실패했습니다.</div>`;
        }
    }

    /**
     * 워크스페이스의 액션 패널에 적절한 폼을 렌더링합니다.
     */
    renderActionForm() {
        const container = document.getElementById('action-form-container');
        // 현재는 '리소스'를 선택했을 때만 '권한 부여' 폼을 보여줍니다.
        if (this.state.selectedItemType === 'resource') {
            container.innerHTML = `
                <form id="grant-form" class="space-y-4">
                    <div>
                        <label for="grant-subjects" class="block text-sm font-medium text-gray-700">누구에게</label>
                        <select id="grant-subjects" name="subjects" multiple class="mt-1 block w-full border-app-border rounded-md h-32"></select>
                    </div>
                    <div>
                        <label for="grant-actions" class="block text-sm font-medium text-gray-700">어떤 행위를</label>
                        <select id="grant-actions" name="actions" multiple class="mt-1 block w-full border-app-border rounded-md h-24"></select>
                    </div>
                    <div>
                        <label for="grant-reason" class="block text-sm font-medium text-gray-700">사유</label>
                        <input type="text" id="grant-reason" required class="mt-1 block w-full border-app-border rounded-md">
                    </div>
                    <button type="submit" class="w-full bg-app-accent hover:bg-app-accent-hover text-white font-bold py-2 px-4 rounded-lg">부여하기</button>
                </form>`;
            this.initGrantForm();
        } else {
            container.innerHTML = `<p class="text-sm text-slate-500">선택된 항목에 대한 빠른 작업이 없습니다.</p>`;
        }
    }

    /**
     * 권한 부여 폼의 select box들을 API 데이터로 채웁니다.
     */
    async initGrantForm() {
        const subjectsSelect = document.getElementById('grant-subjects');
        const actionsSelect = document.getElementById('grant-actions');
        if (!subjectsSelect || !actionsSelect) return;

        try {
            if (!this.state.metadataCache.subjects) {
                this.state.metadataCache.subjects = await this.fetchAPI('/api/workbench/metadata/subjects');
            }
            const subjectsData = this.state.metadataCache.subjects;
            const actionsData = await this.fetchAPI(`/api/workbench/metadata/actions?resourceId=${this.state.selectedItem.id}`);

            subjectsSelect.innerHTML = `
                <optgroup label="그룹">${subjectsData.groups.map(g => `<option value="GROUP_${g.id}">${g.name}</option>`).join('')}</optgroup>
                <optgroup label="사용자">${subjectsData.users.map(u => `<option value="USER_${u.id}">${u.name}</option>`).join('')}</optgroup>`;

            actionsSelect.innerHTML = actionsData.map(a => `<option value="${a.id}">${a.name}</option>`).join('');
        } catch (error) { /* 에러는 fetchAPI에서 처리 */ }
    }

    async handleRevokeClick(target) {
        const policyId = target.dataset.policyId;
        if (confirm(`정말로 이 권한(정책 ID: ${policyId})을 회수하시겠습니까?`)) {
            try {
                await this.fetchAPI('/api/workbench/revocations', {
                    method: 'DELETE',
                    body: JSON.stringify({ policyId: parseInt(policyId), revokeReason: 'Revoked from workbench' })
                });
                showToast('권한이 회수되었습니다.', 'success');
                this.renderWorkspace();
            } catch (error) { /* 에러는 fetchAPI에서 처리 */ }
        }
    }

    async handleGrantSubmit(event) {
        event.preventDefault();
        const form = event.target;
        const grantRequest = {
            subjects: Array.from(form.querySelector('#grant-subjects').selectedOptions).map(opt => {
                const [type, id] = opt.value.split('_');
                return { id: parseInt(id), type };
            }),
            resourceIds: [this.state.selectedItem.id],
            actionIds: Array.from(form.querySelector('#grant-actions').selectedOptions).map(opt => parseInt(opt.value)),
            grantReason: form.querySelector('#grant-reason').value
        };

        if (grantRequest.subjects.length === 0 || grantRequest.actionIds.length === 0) {
            showToast('주체와 행위를 하나 이상 선택해야 합니다.', 'error');
            return;
        }

        try {
            await this.fetchAPI('/api/workbench/grants', { method: 'POST', body: JSON.stringify(grantRequest) });
            showToast('새로운 권한이 부여되었습니다.', 'success');
            this.renderWorkspace();
        } catch(error) { /* 에러는 fetchAPI에서 처리 */ }
    }

    clearWorkspace() {
        if(this.elements.workspacePlaceholder) {
            this.elements.workspace.innerHTML = '';
            this.elements.workspace.appendChild(this.elements.workspacePlaceholder);
            this.elements.workspacePlaceholder.classList.remove('hidden');
        }
    }
}

// 애플리케이션 시작
document.addEventListener('DOMContentLoaded', () => new WorkbenchApp());