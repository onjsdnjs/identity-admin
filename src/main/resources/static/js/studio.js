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

        return {
            userIds: subject.type === 'USER' ? [subject.id] : [],
            groupIds: subject.type === 'GROUP' ? [subject.id] : [],
            permissionIds: [permission.id]
        };
    }
}

// 2. UI 렌더링 및 조작 클래스
class StudioUI {
    constructor(elements) {
        this.elements = elements;
        mermaid.initialize({
            startOnLoad: false,
            theme: 'default',
            flowchart: {
                useMaxWidth: true,
                htmlLabels: false,  // HTML 비활성화
                curve: 'basis'
            }
        });
    }

    renderExplorer(data) {
        const usersHtml = this.createAccordionSection('사용자', [{ items: data.users, type: 'USER', icon: 'fa-user' }]);
        const groupsHtml = this.createAccordionSection('그룹', [{ items: data.groups, type: 'GROUP', icon: 'fa-users' }]);
        const permissionsHtml = this.createAccordionSection('권한', [{ items: data.permissions, type: 'PERMISSION', icon: 'fa-key' }]);
        const policiesHtml = this.createAccordionSection('정책', [{ items: data.policies, type: 'POLICY', icon: 'fa-file-alt' }]);
        this.elements.explorerListContainer.innerHTML = usersHtml + groupsHtml + permissionsHtml + policiesHtml;
        this.bindAccordionEvents();
    }

    createAccordionSection(title, subSections) {
        let contentHtml = '';
        subSections.forEach(sub => {
            if (sub.items && sub.items.length > 0) {
                contentHtml += sub.items.map(item => this.createItemHtml(item, sub.type, sub.icon)).join('');
            }
        });
        if (contentHtml === '') contentHtml = '<div class="p-2 text-xs text-slate-400">항목이 없습니다.</div>';

        return `<div class="accordion"><div class="accordion-header"><span class="font-bold">${title}</span><i class="fas fa-chevron-down accordion-icon"></i></div><div class="accordion-content">${contentHtml}</div></div>`;
    }

    createItemHtml(item, type, icon) {
        return `
            <div class="explorer-item" data-id="${item.id}" data-type="${type}" data-name="${item.name}" data-description="${item.description || ''}">
                <div class="item-icon"><i class="fas ${icon}"></i></div>
                <div class="item-text">
                    <div class="item-name">${item.name}</div>
                    <div class="item-description" title="${item.description || ''}">${item.description || ''}</div>
                </div>
            </div>`;
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
                <div class="path-icon">${node.type.substring(0, 1)}</div>
                <div class="path-details">
                    <p class="path-type">${node.type}</p>
                    <p class="path-name">${node.name}</p>
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

    renderInspector(state, analysisData) {
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

        let actionsHtml = '<h3 class="font-bold text-lg mb-2 mt-4 border-t pt-4">실행 가능한 작업</h3>';

        if (subject && permission) {
            // [수정] analysisData를 확인하여 접근 허용 여부를 판단합니다.
            let isAccessGranted = false;
            if (analysisData && analysisData.nodes) {
                const permNode = analysisData.nodes.find(n => n.type === 'PERMISSION');
                if (permNode && permNode.properties) {
                    isAccessGranted = permNode.properties.granted;
                }
            }

            if (isAccessGranted) {
                actionsHtml += `<div class="p-3 text-center bg-green-100 text-green-800 rounded-lg text-sm font-semibold"><i class="fas fa-check-circle mr-2"></i>이미 권한을 보유하고 있습니다.</div>`;
            } else {
                const title = `'${subject.name}'에게 '${permission.name}' 권한을 부여하는 정책을 생성합니다.`;
                actionsHtml += `<button id="grant-btn" class="w-full btn-primary text-sm py-2" title="${title}">권한 부여하기</button>`;
            }
        } else {
            actionsHtml += '<p class="text-sm text-slate-500">주체와 권한을 함께 선택하면<br/>관련 작업을 실행할 수 있습니다.</p>';
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

    async renderAccessGraph(subject, permission, data) {
        if (!data || !data.nodes || data.nodes.length === 0) {
            this.showError(this.elements.canvasContent, "그래프 데이터를 생성할 수 없습니다.");
            return;
        }

        // Mermaid 초기화 확인
        if (typeof mermaid === 'undefined') {
            this.showError(this.elements.canvasContent, "Mermaid 라이브러리가 로드되지 않았습니다.");
            return;
        }

        this.elements.canvasContent.innerHTML = `
        <div id="mermaid-graph-wrapper">
            <div class="mermaid" id="mermaid-graph-container"></div>
        </div>`;

        const container = document.getElementById('mermaid-graph-container');
        if (!container) {
            this.showError(this.elements.canvasContent, "Mermaid 컨테이너를 생성할 수 없습니다.");
            return;
        }

        // Mermaid 문법 생성
        let mermaidSyntax = 'graph TD\n';

        // 스타일 정의
        mermaidSyntax += '    classDef userNode fill:#3b82f6,stroke:#2563eb,color:#fff,stroke-width:2px\n';
        mermaidSyntax += '    classDef groupNode fill:#10b981,stroke:#059669,color:#fff,stroke-width:2px\n';
        mermaidSyntax += '    classDef roleNode fill:#8b5cf6,stroke:#7c3aed,color:#fff,stroke-width:2px\n';
        mermaidSyntax += '    classDef permGrantedNode fill:#22c55e,stroke:#16a34a,color:#fff,stroke-width:3px\n';
        mermaidSyntax += '    classDef permDeniedNode fill:#ef4444,stroke:#dc2626,color:#fff,stroke-width:3px\n';

        // 노드 ID 매핑 (특수문자 처리)
        const nodeIdMap = new Map();

        // 노드 생성
        data.nodes.forEach((node, index) => {
            const safeId = `node_${index}`;
            nodeIdMap.set(node.id, safeId);

            // 레이블 안전하게 처리
            const label = this.sanitizeMermaidLabel(node.label || 'N/A');
            const nodeText = `${node.type}|${label}`;

            // 노드 정의
            mermaidSyntax += `    ${safeId}["${nodeText}"]\n`;

            // 스타일 적용
            switch(node.type) {
                case 'USER':
                    mermaidSyntax += `    class ${safeId} userNode\n`;
                    break;
                case 'GROUP':
                    mermaidSyntax += `    class ${safeId} groupNode\n`;
                    break;
                case 'ROLE':
                    mermaidSyntax += `    class ${safeId} roleNode\n`;
                    break;
                case 'PERMISSION':
                    if (node.properties?.granted) {
                        mermaidSyntax += `    class ${safeId} permGrantedNode\n`;
                    } else {
                        mermaidSyntax += `    class ${safeId} permDeniedNode\n`;
                    }
                    break;
            }
        });

        // 엣지 생성
        data.edges.forEach(edge => {
            const fromId = nodeIdMap.get(edge.from);
            const toId = nodeIdMap.get(edge.to);
            const edgeLabel = this.sanitizeMermaidLabel(edge.label || '');

            if (edgeLabel.includes('거부')) {
                mermaidSyntax += `    ${fromId} -.-> |"${edgeLabel}"| ${toId}\n`;
            } else {
                mermaidSyntax += `    ${fromId} --> |"${edgeLabel}"| ${toId}\n`;
            }
        });

        // Mermaid 렌더링
        container.textContent = mermaidSyntax;
        container.removeAttribute('data-processed');

        try {
            await mermaid.init(undefined, container);
            // 또는 mermaid 버전에 따라
            // mermaid.contentLoaded();
        } catch (error) {
            console.error('Mermaid 렌더링 오류:', error);
            this.showError(this.elements.canvasContent, "그래프 렌더링 중 오류가 발생했습니다.");
        }

        this.elements.canvasContent.classList.remove('hidden');
        this.elements.canvasGuide.classList.add('hidden');
    }

    sanitizeMermaidLabel(text) {
        return text
            .replace(/["`]/g, "'")     // 따옴표 처리
            .replace(/\n/g, ' ')       // 줄바꿈 제거
            .replace(/[<>{}]/g, '')    // 특수문자 제거
            .replace(/\|/g, '\\|')     // 파이프 이스케이프
            .trim();
    }

    buildDetailHtml(item) {
        const icon = { USER: 'fa-user', GROUP: 'fa-users', PERMISSION: 'fa-key', POLICY: 'fa-file-alt' }[item.type] || 'fa-question-circle';
        return `
            <div class="p-3 bg-white rounded-md border text-sm">
                <p class="text-xs font-semibold uppercase text-app-accent"><i class="fas ${icon} mr-2"></i>${item.type}</p>
                <p class="font-bold text-md text-gray-800">${item.name}</p>
            </div>`;
    }

    resetCanvas() {
        this.elements.canvasContent.classList.add('hidden');
        this.elements.canvasContent.innerHTML = '';
        this.elements.canvasGuide.classList.remove('hidden');
    }

    showLoading(element) {
        element.innerHTML = '<div class="flex items-center justify-center p-8 h-full"><i class="fas fa-spinner fa-spin text-3xl text-app-primary"></i></div>';
    }

    showError(element, message) {
        element.innerHTML = `<div class="p-8 text-center text-red-600 rounded-lg bg-red-50 border border-red-200">
                                <i class="fas fa-exclamation-triangle text-2xl mb-2"></i>
                                <p class="font-semibold">${message}</p>
                             </div>`;
    }

    showGuide(htmlMessage) {
        this.elements.canvasContent.innerHTML = '';
        this.elements.canvasContent.classList.add('hidden');
        this.elements.canvasGuide.innerHTML = `<div class="flex flex-col items-center justify-center h-full text-slate-500 text-center">${htmlMessage}</div>`;
        this.elements.canvasGuide.classList.remove('hidden');
    }

    hideGuide() {
        this.elements.canvasGuide.classList.add('hidden');
        this.elements.canvasContent.classList.remove('hidden');
    }

    bindAccordionEvents() {
        this.elements.explorerListContainer.querySelectorAll('.accordion-header').forEach(header => {
            header.addEventListener('click', () => {
                const content = header.nextElementSibling;
                const isOpen = header.classList.toggle('open');
                header.querySelector('.accordion-icon').classList.toggle('rotate-180', isOpen);
                if (isOpen) {
                    content.style.maxHeight = content.scrollHeight + "px";
                } else {
                    content.style.maxHeight = "0px";
                }
            });
        });
    }

    filterExplorer(term) {
        const searchTerm = term.trim().toLowerCase();

        // 모든 아코디언 섹션을 순회
        this.elements.explorerListContainer.querySelectorAll('.accordion').forEach(accordion => {
            const header = accordion.querySelector('.accordion-header');
            const content = accordion.querySelector('.accordion-content');
            const items = content.querySelectorAll('.explorer-item');
            let visibleCount = 0;

            // 각 아이템의 표시 여부 결정
            items.forEach(item => {
                const name = (item.dataset.name || '').toLowerCase();
                const description = (item.dataset.description || '').toLowerCase();

                // 검색어가 없으면 모두 표시
                if (!searchTerm) {
                    item.style.setProperty('display', 'flex', 'important');
                    visibleCount++;
                } else {
                    // 검색어가 있으면 매칭되는 것만 표시
                    const matches = name.includes(searchTerm) || description.includes(searchTerm);
                    if (matches) {
                        item.style.setProperty('display', 'flex', 'important');
                        visibleCount++;
                    } else {
                        item.style.setProperty('display', 'none', 'important');
                    }
                }
            });

            // 검색어가 있고 매칭되는 아이템이 있으면 아코디언 열기
            if (searchTerm && visibleCount > 0) {
                accordion.style.display = 'block';
                if (!header.classList.contains('open')) {
                    header.classList.add('open');
                    const icon = header.querySelector('.accordion-icon');
                    if (icon) icon.classList.add('rotate-180');
                }
                // DOM 업데이트 후 높이 재계산
                requestAnimationFrame(() => {
                    content.style.maxHeight = content.scrollHeight + "px";
                });
            }
            // 검색어가 있고 매칭되는 아이템이 없으면 섹션 숨기기
            else if (searchTerm && visibleCount === 0) {
                accordion.style.display = 'none';
                content.style.maxHeight = '0px';
            }
            // 검색어가 없으면 모든 섹션 표시하고 아코디언 닫기
            else if (!searchTerm) {
                accordion.style.display = 'block';
                if (header.classList.contains('open')) {
                    header.classList.remove('open');
                    const icon = header.querySelector('.accordion-icon');
                    if (icon) icon.classList.remove('rotate-180');
                    content.style.maxHeight = '0px';
                }
            }
        });

        // 검색 결과가 하나도 없을 때 메시지 표시
        const allAccordions = this.elements.explorerListContainer.querySelectorAll('.accordion');
        let totalVisibleItems = 0;
        allAccordions.forEach(acc => {
            if (acc.style.display !== 'none') {
                const visibleItems = acc.querySelectorAll('.explorer-item[style*="display: flex"]');
                totalVisibleItems += visibleItems.length;
            }
        });

        // 기존 "검색 결과 없음" 메시지 제거
        const noResultMsg = this.elements.explorerListContainer.querySelector('.no-search-results');
        if (noResultMsg) noResultMsg.remove();

        // 검색어가 있고 결과가 없을 때
        if (searchTerm && totalVisibleItems === 0) {
            const noResultsHtml = `
                <div class="no-search-results p-8 text-center text-slate-400">
                    <i class="fas fa-search text-4xl mb-4"></i>
                    <p class="text-lg font-semibold">검색 결과가 없습니다</p>
                    <p class="text-sm mt-2">'${searchTerm}'와(과) 일치하는 항목을 찾을 수 없습니다.</p>
                </div>`;
            this.elements.explorerListContainer.insertAdjacentHTML('beforeend', noResultsHtml);
        }
    }
}

// 3. API 통신 클래스
class StudioAPI {
    async fetchApi(url, options = {}) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const fetchOptions = { ...options };
            if (!fetchOptions.headers) {
                fetchOptions.headers = {};
            }
            if (options.body && !(options.body instanceof URLSearchParams)) {
                fetchOptions.headers['Content-Type'] = 'application/json';
            }
            if (csrfToken && csrfHeader && options.method && options.method.toUpperCase() !== 'GET') {
                fetchOptions.headers[csrfHeader] = csrfToken;
            }
            const response = await fetch(url, fetchOptions);
            if (response.redirected) {
                window.location.href = response.url;
                return null;
            }
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류 (상태: ${response.status})` }));
                throw new Error(errorData.message);
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            console.error(`API Error fetching ${url}:`, error);
            if (typeof showToast === 'function') showToast(error.message || 'API 통신 오류', 'error');
            throw error;
        }
    }
    getExplorerItems() { return this.fetchApi('/admin/studio/api/explorer-items'); }
    getAccessPath(subjectId, subjectType, permissionId) { return this.fetchApi(`/admin/studio/api/access-path?subjectId=${subjectId}&subjectType=${subjectType}&permissionId=${permissionId}`); }
    getAccessPathAsGraph(subjectId, subjectType, permissionId) {
        return this.fetchApi(`/admin/studio/api/access-path-graph?subjectId=${subjectId}&subjectType=${subjectType}&permissionId=${permissionId}`);
    }
    getEffectivePermissions(subjectId, subjectType) { return this.fetchApi(`/admin/studio/api/effective-permissions?subjectId=${subjectId}&subjectType=${subjectType}`); }

}

// 4. 메인 애플리케이션 클래스
class StudioApp {
    constructor() {
        this.elements = {
            explorerListContainer: document.getElementById('explorer-list-container'),
            loader: document.getElementById('explorer-loader'),
            search: document.getElementById('explorer-search'),
            canvasPanel: document.getElementById('canvas-panel'),
            canvasGuide: document.getElementById('canvas-guide'),
            canvasContent: document.getElementById('canvas-content'),
            inspectorPanel: document.getElementById('inspector-panel'),
            inspectorPlaceholder: document.getElementById('inspector-placeholder'),
            inspectorContent: document.getElementById('inspector-content'),
        };
        this.state = new StudioState();
        this.ui = new StudioUI(this.elements);
        this.api = new StudioAPI();
    }

    init() {
        this.bindEventListeners();
        this.loadInitialData();
    }

    async loadInitialData() {
        this.ui.showLoading(this.ui.elements.explorerListContainer);
        this.ui.showGuide('<i class="fas fa-mouse-pointer text-6xl text-slate-300"></i><p class="mt-4 text-lg font-bold">1. 왼쪽 탐색기에서 분석할 \'주체\'를 선택하세요.</p>');
        try {
            const data = await this.api.getExplorerItems();
            this.ui.renderExplorer(data);
        } catch (error) {
            this.ui.showError(this.ui.elements.explorerListContainer, '탐색기 목록 로딩 실패');
        }
    }

    bindEventListeners() {
        this.elements.explorerListContainer.addEventListener('click', e => this.handleExplorerClick(e));
        this.elements.search.addEventListener('keyup', e => this.ui.filterExplorer(e.target.value.toLowerCase()));
        this.elements.inspectorPanel.addEventListener('click', e => {
            if (e.target.closest('#grant-btn')) this.handleGrantClick();
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
        // renderInspector를 try 블록 안으로 이동시키고, API 결과(data)를 전달합니다.
        const subject = this.state.getSubject();
        const permission = this.state.getPermission();

        if (!subject) {
            this.ui.showGuide('<i class="fas fa-mouse-pointer text-6xl text-slate-300"></i><p class="mt-4 text-lg font-bold">1. 왼쪽 탐색기에서 분석할 \'주체\'를 선택하세요.</p><p class="text-sm text-slate-400">사용자 또는 그룹을 클릭할 수 있습니다.</p>');
            this.ui.renderInspector(this.state, null); // 선택이 없을 때도 Inspector는 업데이트
            return;
        }
        if (!permission) {
            this.ui.showGuide(`<div class="text-center"><i class="fas fa-check-circle text-4xl text-green-500"></i><p class="mt-4 text-lg font-bold">'<strong>${subject.name}</strong>' 선택됨.</p><p class="mt-2 text-slate-500">2. 이제 분석하고 싶은 '권한'을 선택하여 접근 경로를 확인하세요.</p></div>`);
            this.ui.renderInspector(this.state, null); // 선택이 없을 때도 Inspector는 업데이트
            return;
        }

        this.ui.hideGuide();
        this.ui.showLoading(this.ui.elements.canvasContent);

        try {
            const data = await this.api.getAccessPathAsGraph(subject.id, subject.type, permission.id);
            await this.ui.renderAccessGraph(subject, permission, data);
            // API 호출 성공 후, 그 결과를 renderInspector에 전달
            this.ui.renderInspector(this.state, data);
        } catch (error) {
            this.ui.showError(this.ui.elements.canvasContent, '분석 데이터 로딩 실패');
            // 에러 발생 시에도 Inspector는 현재 선택 상태만으로 업데이트
            this.ui.renderInspector(this.state, null);
        }
    }

    handleGrantClick() {
        const grantData = this.state.getSelectedItemsForAction();
        if(!grantData) {
            showToast("권한을 부여할 주체와 권한을 모두 선택해주세요.", "error");
            return;
        }

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/admin/policy-wizard/start';

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const dataForForm = { ...grantData };

        Object.entries(dataForForm).forEach(([key, value]) => {
            if (Array.isArray(value)) {
                value.forEach(v => this.createHiddenInput(form, key, v));
            } else {
                this.createHiddenInput(form, key, value);
            }
        });

        this.createHiddenInput(form, csrfHeader, csrfToken);
        document.body.appendChild(form);
        form.submit();
    }

    createHiddenInput(form, name, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if(typeof showToast !== 'function') {
        window.showToast = (message, type) => alert(`[${type.toUpperCase()}] ${message}`);
    }
    new StudioApp().init();
});