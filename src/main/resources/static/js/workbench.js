/**
 * ===================================================================
 * IAM Command Center - Unified Workbench (v3.0)
 * -------------------------------------------------------------------
 * 이 스크립트는 단일 페이지에서 모든 IAM 관련 관리 작업을 처리합니다.
 * - 상태 기반의 동적 UI 렌더링
 * - 컨텍스트 인식 상세 정보 및 액션 패널
 * - 모든 데이터는 백엔드 API를 통해 비동기적으로 로드됩니다.
 * ===================================================================
 */
class WorkbenchApp {
    /**
     * 애플리케이션 초기화
     */
    constructor() {
        // 1. 애플리케이션의 상태 관리
        this.state = {
            currentView: 'resources', // 현재 탐색기 뷰 (resources, subjects, roles)
            selectedItemId: null,
            selectedItemType: null,
            selectedItemName: '',
            debounceTimer: null,
            metadataCache: {} // API 응답 캐싱용
        };
        // 2. DOM 요소 캐싱
        this.cacheDOMElements();
        // 3. 이벤트 리스너 바인딩
        this.bindEventListeners();
        // 4. 초기 데이터 로드
        this.loadExplorerData();
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
        };
    }

    /**
     * 모든 UI 상호작용을 위한 이벤트 리스너를 등록합니다.
     */
    bindEventListeners() {
        // 탐색기: 뷰(관점) 변경
        this.elements.viewSelector.addEventListener('change', (e) => {
            this.state.currentView = e.target.value;
            this.elements.searchInput.value = '';
            this.elements.searchInput.placeholder = `${e.target.options[e.target.selectedIndex].text} 검색...`;
            this.clearWorkspace('관점이 변경되었습니다. 좌측에서 항목을 선택해주세요.');
            this.loadExplorerData();
        });

        // 탐색기: 검색어 입력 (디바운싱 적용)
        this.elements.searchInput.addEventListener('input', (e) => {
            clearTimeout(this.state.debounceTimer);
            this.state.debounceTimer = setTimeout(() => {
                this.loadExplorerData(e.target.value);
            }, 300);
        });

        // 탐색기: 목록에서 항목 선택 (이벤트 위임)
        this.elements.explorerList.addEventListener('click', (e) => {
            const itemElement = e.target.closest('.explorer-item');
            if (itemElement) {
                this.handleItemSelect(itemElement);
            }
        });

        // 워크스페이스: 동적으로 생성된 요소들에 대한 이벤트 처리 (이벤트 위임)
        this.elements.workspace.addEventListener('click', (e) => {
            if (e.target.classList.contains('revoke-btn')) {
                this.handleRevokeClick(e.target);
            }
            // ... 향후 탭 클릭, 버튼 클릭 등 추가 가능
        });

        this.elements.workspace.addEventListener('submit', (e) => {
            if (e.target && e.target.id === 'grant-form') {
                this.handleGrantSubmit(e);
            }
        });
    }

    // ================== 4. 핵심 로직 및 데이터 로딩 ==================

    /**
     * API 통신을 위한 중앙 헬퍼 함수
     */
    async fetchAPI(url, options = {}) {
        try {
            const defaultHeaders = { 'Content-Type': 'application/json' };
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (csrfToken && csrfHeader) defaultHeaders[csrfHeader] = csrfToken;

            const response = await fetch(url, { ...options, headers: { ...defaultHeaders, ...options.headers } });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류: ${response.status}` }));
                throw new Error(errorData.message || '알 수 없는 오류.');
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            showToast(error.message, 'error');
            throw error;
        }
    }

    /**
     * 현재 선택된 뷰(관점)에 따라 탐색기 목록을 API로 로드하고 렌더링합니다.
     */
    async loadExplorerData(keyword = '') {
        this.renderExplorerLoading();
        let url, data;
        try {
            switch (this.state.currentView) {
                case 'subjects':
                    data = await this.fetchAPI('/api/workbench/metadata/subjects');
                    // 검색은 클라이언트 사이드에서 간단히 처리 (실제로는 백엔드 검색 지원 필요)
                    if (keyword) {
                        const lowerKeyword = keyword.toLowerCase();
                        data.users = data.users.filter(u => u.name.toLowerCase().includes(lowerKeyword) || u.username.toLowerCase().includes(lowerKeyword));
                        data.groups = data.groups.filter(g => g.name.toLowerCase().includes(lowerKeyword));
                    }
                    break;
                case 'roles':
                    data = await this.fetchAPI(`/api/workbench/metadata/roles?keyword=${encodeURIComponent(keyword)}`);
                    break;
                case 'resources':
                default:
                    const pageData = await this.fetchAPI(`/api/workbench/resources?keyword=${encodeURIComponent(keyword)}`);
                    data = pageData.content;
                    break;
            }
            this.renderExplorerList(data);
        } catch (error) {
            this.elements.explorerList.innerHTML = `<div class="p-4 text-center text-error">데이터 로딩 실패.</div>`;
        }
    }

    /**
     * 탐색기에서 특정 항목을 선택했을 때의 모든 로직을 처리합니다.
     */
    handleItemSelect(element) {
        this.state.selectedItemId = element.dataset.id;
        this.state.selectedItemType = element.dataset.type;
        this.state.selectedItemName = element.querySelector('p.font-semibold')?.textContent || '';

        document.querySelectorAll('.explorer-item.selected').forEach(el => el.classList.remove('selected'));
        element.classList.add('selected');

        this.renderWorkspace();
    }

    /**
     * 선택된 항목의 타입에 따라 워크스페이스 전체를 다시 렌더링합니다.
     */
    async renderWorkspace() {
        this.renderWorkspaceLoading();
        const { selectedItemId: id, selectedItemType: type, selectedItemName: name } = this.state;

        if (!id || !type) {
            this.clearWorkspace();
            return;
        }

        let detailHtml = '';
        try {
            if (type === 'resource') {
                const entitlements = await this.fetchAPI(`/api/workbench/entitlements/by-resource?resourceId=${id}`);
                detailHtml = this.getResourceDetailHtml(name, entitlements);
            } else if (type === 'user' || type === 'group') {
                const entitlements = await this.fetchAPI(`/api/workbench/entitlements/by-subject?type=${type.toUpperCase()}&id=${id}`);
                detailHtml = this.getSubjectDetailHtml(name, type, entitlements);
            }
            // ... 역할(Role)에 대한 상세 뷰 추가 ...

            this.elements.workspace.innerHTML = detailHtml;
            // 권한 부여 폼이 있다면, 메타데이터 로드
            if (document.getElementById('grant-form')) {
                this.initGrantForm();
            }

        } catch (error) {
            this.elements.workspace.innerHTML = `<div class="p-8 text-center text-error">상세 정보 로딩에 실패했습니다.</div>`;
        }
    }

    /**
     * 권한 부여 폼에 필요한 메타데이터(주체, 행위 목록)를 API로 가져와 채웁니다.
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

            // 리소스에 따라 가능한 행위가 달라지므로, 캐시하지 않음
            const actionsData = await this.fetchAPI(`/api/workbench/metadata/actions?resourceId=${this.state.selectedItemId}`);

            // 주체 목록 채우기
            subjectsSelect.innerHTML = `
                <optgroup label="그룹">${subjectsData.groups.map(g => `<option value="GROUP_${g.id}">${g.name}</option>`).join('')}</optgroup>
                <optgroup label="사용자">${subjectsData.users.map(u => `<option value="USER_${u.id}">${u.name} (${u.username})</option>`).join('')}</optgroup>`;

            // 행위 목록 채우기
            actionsSelect.innerHTML = actionsData.map(a => `<option value="${a.id}">${a.name}</option>`).join('');

        } catch (error) { /* 에러는 fetchAPI에서 이미 처리 */ }
    }

    // ================== 5. 동적 이벤트 핸들링 ==================

    /**
     * 권한 회수 버튼 클릭을 처리합니다.
     */
    async handleRevokeClick(target) {
        const policyId = target.dataset.policyId;
        if (confirm(`정말로 이 권한(정책 ID: ${policyId})을 회수하시겠습니까?`)) {
            try {
                await this.fetchAPI('/api/workbench/revocations', {
                    method: 'DELETE',
                    body: JSON.stringify({ policyId: parseInt(policyId), revokeReason: 'Revoked from workbench' })
                });
                showToast('권한이 회수되었습니다.', 'success');
                this.renderWorkspace(); // 워크스페이스 리프레시
            } catch (error) { /* 에러는 fetchAPI에서 이미 처리 */ }
        }
    }

    /**
     * 권한 부여 폼 제출을 처리합니다.
     */
    async handleGrantSubmit(event) {
        event.preventDefault();
        const form = event.target;
        const grantSubjectsSelect = form.querySelector('#grant-subjects');
        const grantActionsSelect = form.querySelector('#grant-actions');
        const grantReasonInput = form.querySelector('#grant-reason');

        const selectedSubjects = Array.from(grantSubjectsSelect.selectedOptions).map(opt => {
            const [type, id] = opt.value.split('_');
            return { id: parseInt(id), type };
        });

        if (selectedSubjects.length === 0) {
            showToast('하나 이상의 주체를 선택해주세요.', 'error'); return;
        }

        const grantRequest = {
            subjects: selectedSubjects,
            resourceIds: [this.state.selectedItemId],
            actionIds: Array.from(grantActionsSelect.selectedOptions).map(opt => parseInt(opt.value)),
            grantReason: grantReasonInput.value
        };

        try {
            await this.fetchAPI('/api/workbench/grants', { method: 'POST', body: JSON.stringify(grantRequest) });
            showToast('권한이 부여되었습니다.', 'success');
            this.renderWorkspace(); // 워크스페이스 리프레시
        } catch(error) { /* 에러는 fetchAPI에서 이미 처리 */ }
    }

    // ================== 6. UI 템플릿 및 헬퍼 ==================
    renderExplorerLoading() { this.elements.explorerList.innerHTML = `<div class="p-4 text-center text-slate-500">로딩 중...</div>`; }
    renderWorkspaceLoading() { this.elements.workspace.innerHTML = `<div class="p-8 text-center text-slate-500">로딩 중...</div>`; }
    clearWorkspace(message) {
        this.elements.workspace.innerHTML = `
            <div class="flex flex-col items-center justify-center h-full text-slate-500">
                <p class="mt-4 text-lg">${message || '좌측 탐색기에서 항목을 선택해주세요.'}</p>
            </div>`;
    }

    getExplorerItemHtml(item, type) { /* ... */ }

    getResourceDetailHtml(resourceName, entitlements) {
        const entitlementCards = (!entitlements || entitlements.length === 0)
            ? `<div class="p-4 text-center text-slate-500">부여된 접근 권한이 없습니다.</div>`
            : entitlements.map(ent => ``).join('');

        return `
            <div class="grid grid-cols-10 gap-6">
                <div class="col-span-6">
                    <h2 class="text-xl font-bold text-app-secondary">권한 현황: ${resourceName}</h2>
                    <div class="mt-4 space-y-3">${entitlementCards}</div>
                </div>
                <div class="col-span-4 bg-gray-50 p-4 rounded-lg">
                    <h2 class="text-xl font-bold text-app-secondary">새 권한 부여</h2>
                    </div>
            </div>`;
    }

    getSubjectDetailHtml(subjectName, type, entitlements) { /* ... */ }
}

// 애플리케이션 인스턴스 생성 및 실행
new WorkbenchApp();