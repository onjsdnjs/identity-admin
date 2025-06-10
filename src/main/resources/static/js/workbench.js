/**
 * ===================================================================
 * IAM Command Center - Unified Workbench (v3.0)
 * -------------------------------------------------------------------
 * 최종 버전: 기존 상세 관리 페이지와의 유기적 통합 기능 구현
 * ===================================================================
 */
class WorkbenchApp {
    constructor() {
        this.state = { currentView: 'resources' };
        this.cacheDOMElements();
        this.loadingSpinner = `<div class="flex items-center justify-center p-8"><svg class="animate-spin h-8 w-8 text-app-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg></div>`;
        this.bindEventListeners();
        this.loadExplorerData();
    }

    cacheDOMElements() {
        this.elements = {
            viewSelector: document.getElementById('view-selector'),
            searchInput: document.getElementById('explorer-search-input'),
            explorerList: document.getElementById('explorer-list'),
            workspace: document.getElementById('workspace'),
            workspacePlaceholder: document.getElementById('workspace-placeholder')
        };
    }

    async fetchAPI(url, options = {}) {
        try {
            const defaultHeaders = { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (csrfToken && csrfHeader) defaultHeaders[csrfHeader] = csrfToken;

            const response = await fetch(url, { ...options, headers: { ...defaultHeaders, ...options.headers } });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류: ${response.status}` }));
                throw new Error(errorData.message || '알 수 없는 오류가 발생했습니다.');
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            showToast(error.message, 'error');
            throw error;
        }
    }

    bindEventListeners() {
        this.elements.viewSelector.addEventListener('change', e => {
            this.state.currentView = e.target.value;
            this.elements.searchInput.value = '';
            this.clearWorkspace();
            this.loadExplorerData();
        });

        this.elements.searchInput.addEventListener('input', e => {
            clearTimeout(this.state.debounceTimer);
            this.state.debounceTimer = setTimeout(() => this.loadExplorerData(e.target.value), 300);
        });

        this.elements.explorerList.addEventListener('click', e => {
            const itemElement = e.target.closest('.explorer-item');
            if (itemElement) this.handleItemSelect(itemElement);
        });
    }

    async loadExplorerData(keyword = '') {
        this.elements.explorerList.innerHTML = this.loadingSpinner;
        let url = `/api/workbench/${this.state.currentView}?keyword=${encodeURIComponent(keyword)}`;
        if (this.state.currentView === 'subjects') url = '/api/workbench/metadata/subjects';

        try {
            let data = await this.fetchAPI(url);
            let items = this.state.currentView === 'resources' ? data.content : data;
            this.renderExplorerList(items);
        } catch (error) {
            this.elements.explorerList.innerHTML = `<div class="p-4 text-center text-red-500">목록 로딩 실패.</div>`;
        }
    }

    renderExplorerList(data) {
        this.elements.explorerList.innerHTML = '';
        let itemsHtml = '';
        switch (this.state.currentView) {
            case 'resources':
                itemsHtml = data.map(item => this.getExplorerItemHtml(item.id, 'resource', item.friendlyName, item.resourceIdentifier)).join('');
                break;
            case 'subjects':
                itemsHtml += `<div class="explorer-header">그룹</div>`;
                itemsHtml += data.groups.map(item => this.getExplorerItemHtml(item.id, 'group', item.name, item.description)).join('');
                itemsHtml += `<div class="explorer-header">사용자</div>`;
                itemsHtml += data.users.map(item => this.getExplorerItemHtml(item.id, 'user', item.name, item.username)).join('');
                break;
            case 'roles':
                itemsHtml = data.map(item => this.getExplorerItemHtml(item.id, 'role', item.roleName, item.roleDesc)).join('');
                break;
        }
        this.elements.explorerList.innerHTML = itemsHtml || `<div class="p-4 text-center text-slate-500">결과가 없습니다.</div>`;
    }

    getExplorerItemHtml(id, type, title, subtitle) {
        return `
            <div class="explorer-item" data-id="${id}" data-type="${type}" data-name="${title}">
                <p class="font-semibold text-app-dark-gray">${title}</p>
                <p class="text-xs text-slate-500 truncate" title="${subtitle || ''}">${subtitle || ''}</p>
            </div>`;
    }

    handleItemSelect(element) {
        document.querySelectorAll('.explorer-item.selected').forEach(el => el.classList.remove('selected'));
        element.classList.add('selected');

        const { id, type, name } = element.dataset;
        this.renderWorkspace(id, type, name);
    }

    renderWorkspace(id, type, name) {
        this.elements.workspacePlaceholder.classList.add('hidden');
        const workspaceHtml = `
            <div class="flex items-center justify-between mb-6">
                <div>
                    <p class="text-sm font-semibold text-app-accent uppercase">${type}</p>
                    <h2 class="text-2xl font-bold text-app-secondary">${name}</h2>
                </div>
                <a href="/admin/${type}s/${id}" target="_blank" class="action-button">
                    <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                    상세 편집 / 권한 관리
                </a>
            </div>
            <div class="border-t pt-4">
                <h3 class="font-bold mb-2">요약된 접근 권한 현황</h3>
                <div id="workspace-entitlements" class="space-y-2">
                    ${this.loadingSpinner}
                </div>
            </div>
        `;
        this.elements.workspace.innerHTML = workspaceHtml;
        this.loadWorkspaceEntitlements(id, type);
    }

    async loadWorkspaceEntitlements(id, type) {
        const container = document.getElementById('workspace-entitlements');
        try {
            let entitlements;
            if(type === 'resource'){
                entitlements = await this.fetchAPI(`/api/workbench/entitlements/by-resource?resourceId=${id}`);
            } else {
                entitlements = await this.fetchAPI(`/api/workbench/entitlements/by-subject?type=${type.toUpperCase()}&id=${id}`);
            }

            if (!entitlements || entitlements.length === 0) {
                container.innerHTML = `<div class="p-4 text-center text-slate-500 bg-slate-50 rounded-lg">정의된 접근 권한이 없습니다.</div>`;
                return;
            }

            // Entitlement 정보를 기반으로 UI 렌더링
            const cardsHtml = entitlements.map(ent => `
                <div class="entitlement-card">
                    <p><strong class="text-gray-800">${ent.subjectName}</strong> (이)가 <strong class="text-gray-800">${ent.resourceName}</strong>에 대해</p>
                    <p class="text-app-accent font-semibold">${ent.actions.join(', ')}</p>
                    <p class="text-xs text-gray-500 mt-1">조건: ${ent.conditions.join(' ')}</p>
                </div>
            `).join('');
            container.innerHTML = cardsHtml;

        } catch (error) {
            container.innerHTML = `<div class="p-4 text-center text-red-500">권한 정보를 불러오는 데 실패했습니다.</div>`;
        }
    }

    clearWorkspace() {
        this.elements.workspace.innerHTML = '';
        this.elements.workspace.appendChild(this.elements.workspacePlaceholder);
        this.elements.workspacePlaceholder.classList.remove('hidden');
    }
}

// 애플리케이션 시작
document.addEventListener('DOMContentLoaded', () => new WorkbenchApp());

// CSS 추가 (workbench.html 하단 또는 별도 css 파일)
const style = document.createElement('style');
style.textContent = `
    .explorer-item { padding: 0.75rem; border-radius: 0.5rem; cursor: pointer; transition: background-color 0.2s; border: 1px solid transparent; }
    .explorer-item:hover { background-color: #f3f4f6; }
    .explorer-item.selected { background-color: #e0e7ff; border-color: #6366f1; }
    .explorer-header { padding: 0.75rem 0.5rem 0.25rem; font-size: 0.75rem; font-weight: 600; color: #475569; text-transform: uppercase; }
    .action-button { display: inline-flex; align-items: center; padding: 0.5rem 1rem; background-color: #4f46e5; color: white; border-radius: 0.5rem; font-weight: 500; transition: background-color 0.2s; }
    .action-button:hover { background-color: #4338ca; }
    .entitlement-card { background-color: #f8fafc; border: 1px solid #e2e8f0; padding: 1rem; border-radius: 0.5rem; }
`;
document.head.appendChild(style);