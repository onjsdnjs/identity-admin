/**
 * Authorization Studio
 * @description IAM 시스템의 모든 인가 관계를 탐색, 분석, 관리하는 중앙 허브의 클라이언트 사이드 애플리케이션입니다.
 * @version 1.0 (Phase 2 완료 기준)
 */
class AuthorizationStudioApp {
    /**
     * 애플리케이션을 초기화하고, 주요 DOM 요소를 캐싱하며, 초기 데이터 로드를 시작합니다.
     */
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

        this.state = {
            selectedUser: null,
            selectedGroup: null,
            selectedPermission: null,
            selectedPolicy: null,
        };

        this.api = new StudioApi();
        this.init();
    }

    /**
     * 애플리케이션의 이벤트 리스너를 바인딩하고 초기 데이터를 로드합니다.
     */
    async init() {
        this.elements.loader.style.display = 'flex';
        this.bindEventListeners();
        try {
            const explorerData = await this.api.getExplorerItems();
            this.renderExplorer(explorerData);
        } catch (error) {
            this.showError(this.elements.explorerListContainer, '탐색기 목록 로딩 실패');
            console.error('Failed to initialize studio:', error);
        } finally {
            this.elements.loader.style.display = 'none';
        }
    }

    /**
     * UI 요소에 대한 모든 이벤트 리스너를 등록합니다.
     */
    bindEventListeners() {
        this.elements.explorerListContainer.addEventListener('click', (e) => this.handleExplorerClick(e));
        this.elements.search.addEventListener('input', (e) => this.filterExplorer(e.target.value.toLowerCase()));
    }

    /**
     * API로부터 받은 데이터로 Explorer 패널을 렌더링합니다.
     * @param {object} data - 주체, 권한, 정책 목록을 포함하는 데이터
     */
    renderExplorer(data) {
        const usersHtml = this.createSectionHtml('사용자', data.users, 'USER');
        const groupsHtml = this.createSectionHtml('그룹', data.groups, 'GROUP');
        const permissionsHtml = this.createSectionHtml('권한', data.permissions, 'PERMISSION');
        const policiesHtml = this.createSectionHtml('정책', data.policies, 'POLICY');

        this.elements.explorerListContainer.innerHTML = usersHtml + groupsHtml + permissionsHtml + policiesHtml;
    }

    /**
     * Explorer의 각 섹션(주체, 권한 등)에 대한 HTML을 생성합니다.
     * @param {string} title - 섹션 제목
     * @param {Array} items - 표시할 아이템 배열
     * @param {string} type - 아이템 타입 (USER, GROUP, PERMISSION, POLICY)
     * @returns {string} 생성된 HTML 문자열
     */
    createSectionHtml(title, items, type) {
        if (!items || items.length === 0) return '';
        const itemsHtml = items.map(item => `
            <div class="explorer-item" data-id="${item.id}" data-type="${type}" data-name="${item.name}" data-description="${item.description || ''}">
                <div class="item-name">${item.name}</div>
                <div class="item-description" title="${item.description || ''}">${item.description || ''}</div>
            </div>
        `).join('');

        return `<h3 class="explorer-header">${title}</h3>${itemsHtml}`;
    }

    /**
     * Explorer 패널의 아이템 클릭 이벤트를 처리합니다.
     * @param {Event} e - 클릭 이벤트 객체
     */
    handleExplorerClick(e) {
        const item = e.target.closest('.explorer-item');
        if (!item) return;

        const { id, type, name, description } = item.dataset;
        const key = `selected${type.charAt(0).toUpperCase() + type.slice(1).toLowerCase()}`;

        // 아이템 선택/해제 토글 로직
        if (item.classList.contains('selected')) {
            item.classList.remove('selected');
            this.state[key] = null;
        } else {
            // 같은 타입의 다른 선택은 해제
            this.elements.explorerListContainer.querySelectorAll(`.explorer-item[data-type="${type}"].selected`).forEach(el => el.classList.remove('selected'));
            item.classList.add('selected');
            this.state[key] = { id, name, description, type };
        }

        // 주체는 사용자 또는 그룹 중 하나만 선택 가능
        if (type === 'USER' && this.state.selectedUser) this.clearSelection('Group');
        if (type === 'GROUP' && this.state.selectedGroup) this.clearSelection('User');

        this.updateCanvasAndInspector();
    }

    /**
     * 특정 타입의 선택 상태를 해제합니다.
     * @param {string} typeName - 해제할 타입 이름 (예: 'User', 'Group')
     */
    clearSelection(typeName) {
        const key = `selected${typeName}`;
        this.state[key] = null;
        const type = typeName.toUpperCase();
        const selectedEl = this.elements.explorerListContainer.querySelector(`.explorer-item[data-type="${type}"].selected`);
        if (selectedEl) selectedEl.classList.remove('selected');
    }

    /**
     * 현재 선택된 상태에 따라 Canvas와 Inspector 패널을 업데이트합니다.
     */
    async updateCanvasAndInspector() {
        this.showLoading(this.elements.canvasContent);
        this.elements.canvasPlaceholder.classList.add('hidden');
        this.elements.canvasContent.classList.remove('hidden');

        const subject = this.state.selectedUser || this.state.selectedGroup;
        const permission = this.state.selectedPermission;

        try {
            // 시나리오 1: 주체와 권한이 모두 선택된 경우 -> 접근 경로 분석
            if (subject && permission) {
                const data = await this.api.getAccessPath(subject.id, subject.type, permission.id);
                this.renderAccessPath(subject, permission, data);
            }
            // 시나리오 2: 주체만 선택된 경우 -> 유효 권한 목록 표시
            else if (subject) {
                const data = await this.api.getEffectivePermissions(subject.id, subject.type);
                this.renderEffectivePermissions(subject, data);
            }
            // 시나리오 3: 그 외의 경우 (권한만 선택 등) -> 플레이스홀더 표시
            else {
                this.resetCanvas();
            }
        } catch (error) {
            this.showError(this.elements.canvasContent, '분석 데이터를 불러오는 데 실패했습니다.');
            console.error('Failed to update canvas:', error);
        }
    }

    /**
     * 접근 경로 분석 결과를 Canvas에 렌더링합니다.
     * @param {object} subject - 선택된 주체 정보
     * @param {object} permission - 선택된 권한 정보
     * @param {object} data - API로부터 받은 접근 경로 데이터
     */
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
            </div>
         `;
    }

    /**
     * 유효 권한 목록을 Canvas에 렌더링합니다.
     * @param {object} subject - 선택된 주체 정보
     * @param {Array} data - API로부터 받은 유효 권한 목록 데이터
     */
    renderEffectivePermissions(subject, data) {
        const permsHtml = data.map(perm => `
            <div class="p-3 border rounded-md mb-2 bg-white hover:shadow-md transition-shadow">
                <p class="font-semibold text-gray-800">${perm.permissionDescription}</p>
                <p class="text-sm text-slate-600 mt-1">획득 경로: <span class="font-mono text-xs bg-slate-100 p-1 rounded">${perm.origin}</span></p>
            </div>
         `).join('');

        this.elements.canvasContent.innerHTML = `
            <h2 class="text-xl font-bold mb-4">'${subject.name}'의 유효 권한 목록</h2>
            <div class="space-y-2">${permsHtml.length > 0 ? permsHtml : '<p class="text-slate-500 p-4 bg-slate-50 rounded-md text-center">부여된 권한이 없습니다.</p>'}</div>
        `;
    }

    /**
     * Canvas 패널을 초기 상태로 되돌립니다.
     */
    resetCanvas() {
        this.elements.canvasContent.classList.add('hidden');
        this.elements.canvasContent.innerHTML = '';
        this.elements.canvasPlaceholder.classList.remove('hidden');
    }

    /**
     * Explorer 목록을 검색어로 필터링합니다.
     * @param {string} term - 검색어
     */
    filterExplorer(term) {
        document.querySelectorAll('.explorer-item').forEach(item => {
            const name = item.dataset.name.toLowerCase();
            const description = item.dataset.description.toLowerCase();
            item.style.display = (name.includes(term) || description.includes(term)) ? 'block' : 'none';
        });
    }

    showLoading(element) {
        element.innerHTML = '<div class="flex items-center justify-center p-8"><i class="fas fa-spinner fa-spin text-3xl text-app-primary"></i></div>';
    }

    showError(element, message) {
        element.innerHTML = `<div class="p-4 text-center text-red-500">${message}</div>`;
    }
}

/**
 * 서버 API 호출을 담당하는 헬퍼 클래스입니다.
 */
class StudioApi {
    async fetchApi(url) {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류 (상태: ${response.status})`}));
                throw new Error(errorData.message);
            }
            return response.json();
        } catch (error) {
            console.error(`API Error fetching ${url}:`, error);
            if(typeof showToast === 'function') showToast(error.message, 'error');
            throw error;
        }
    }

    getExplorerItems() {
        return this.fetchApi('/admin/studio/api/explorer-items');
    }

    getAccessPath(subjectId, subjectType, permissionId) {
        return this.fetchApi(`/admin/studio/api/access-path?subjectId=${subjectId}&subjectType=${subjectType}&permissionId=${permissionId}`);
    }

    getEffectivePermissions(subjectId, subjectType) {
        return this.fetchApi(`/admin/studio/api/effective-permissions?subjectId=${subjectId}&subjectType=${subjectType}`);
    }
}

// DOM 콘텐츠가 로드되면 애플리케이션을 시작합니다.
document.addEventListener('DOMContentLoaded', () => new AuthorizationStudioApp());