document.addEventListener('DOMContentLoaded', () => {
    // Mermaid.js가 로드되었는지 확인
    if (typeof mermaid === 'undefined') {
        console.error('Mermaid 라이브러리가 로드되지 않았습니다.');
        return;
    }
    mermaid.initialize({ startOnLoad: false, theme: 'default' });

    // --- 상태 관리 (State) ---
    class StudioState {
        constructor() {
            this.selected = { USER: null, GROUP: null, PERMISSION: null, POLICY: null };
        }
        select(type, item) {
            this.selected[type] = (this.selected[type]?.id === item.id) ? null : item;
            if (type === 'USER' && this.selected.USER) this.selected.GROUP = null;
            if (type === 'GROUP' && this.selected.GROUP) this.selected.USER = null;
        }
        getSubject() { return this.selected.USER || this.selected.GROUP; }
        getPermission() { return this.selected.PERMISSION; }
    }

    // --- UI 렌더링 및 조작 ---
    class StudioUI {
        constructor(elements) { this.elements = elements; }

        setLoading(button, isLoading, originalText) {
            if (!button) return;
            button.disabled = isLoading;
            button.innerHTML = isLoading ? `<i class="fas fa-spinner fa-spin mr-2"></i> 처리 중...` : originalText;
        }

        renderExplorer(data) {
            const sections = {
                '사용자': { items: data.users, type: 'USER', icon: 'fa-user' },
                '그룹': { items: data.groups, type: 'GROUP', icon: 'fa-users' },
                '권한': { items: data.permissions, type: 'PERMISSION', icon: 'fa-key' },
                '정책': { items: data.policies, type: 'POLICY', icon: 'fa-file-alt' }
            };
            this.elements.explorerListContainer.innerHTML = Object.entries(sections)
                .map(([title, { items, type, icon }]) => this.createAccordionSection(title, items, type, icon))
                .join('');
            this.bindAccordionEvents();
        }
        createAccordionSection(title, items, type, icon) {
            const contentHtml = items?.length > 0
                ? items.map(item => this.createItemHtml(item, type, icon)).join('')
                : '<div class="p-2 text-xs text-slate-400">항목이 없습니다.</div>';
            return `<div class="accordion"><div class="accordion-header"><span class="font-bold">${title}</span><i class="fas fa-chevron-down accordion-icon"></i></div><div class="accordion-content">${contentHtml}</div></div>`;
        }
        createItemHtml(item, type, icon) {
            return `<div class="explorer-item" data-id="${item.id}" data-type="${type}" data-name="${item.name}" data-description="${item.description || ''}"><div class="item-icon"><i class="fas ${icon}"></i></div><div class="item-text"><div class="item-name">${item.name}</div><div class="item-description" title="${item.description || ''}">${item.description || ''}</div></div></div>`;
        }
        updateExplorerSelection(state) {
            this.elements.explorerListContainer.querySelectorAll('.explorer-item').forEach(el => {
                const { id, type } = el.dataset;
                el.classList.toggle('selected', state.selected[type]?.id == id);
            });
        }
        renderInspector(state) {
            const subject = state.getSubject();
            const hasSelection = Object.values(state.selected).some(v => v !== null);
            let detailsHtml = '', actionsHtml = '';

            if (hasSelection) {
                detailsHtml = '<h3 class="font-bold text-lg mb-2 text-slate-200">선택된 항목</h3><div class="space-y-2">';
                Object.values(state.selected).forEach(item => { if (item) detailsHtml += this.buildDetailHtml(item); });
                detailsHtml += '</div>';
            }

            actionsHtml = '<h3 class="font-bold text-lg mb-2 mt-4 border-t border-slate-700 pt-4 text-slate-200">실행 가능한 작업</h3>';
            if (subject) {
                actionsHtml += `<button id="manage-membership-btn" class="w-full dark-btn-primary text-sm py-2" title="'${subject.name}'의 멤버십 관리"><i class='fas fa-edit mr-2'></i>멤버십 및 권한 관리</button>`;
            } else {
                actionsHtml += '<p class="text-sm text-slate-500">주체를 선택하면 관련 작업을 실행할 수 있습니다.</p>';
            }

            this.elements.inspectorContent.innerHTML = hasSelection ? detailsHtml + actionsHtml : '';
            this.elements.inspectorPlaceholder.classList.toggle('hidden', hasSelection);
            this.elements.inspectorContent.classList.toggle('hidden', !hasSelection);
        }
        buildDetailHtml(item) {
            const iconMap = { USER: 'fa-user', GROUP: 'fa-users', PERMISSION: 'fa-key', POLICY: 'fa-file-alt' };
            return `<div class="p-3 bg-slate-800 rounded-md border border-slate-700 text-sm"><p class="text-xs font-semibold uppercase text-app-accent"><i class="fas ${iconMap[item.type] || 'fa-question-circle'} mr-2"></i>${item.type}</p><p class="font-bold text-md text-slate-200">${item.name}</p><p class="text-xs text-slate-400 truncate" title="${item.description}">${item.description || ''}</p></div>`;
        }
        async renderAccessGraph(data) {
            this.showGuide('<div class="flex items-center justify-center p-8 h-full"><i class="fas fa-spinner fa-spin text-3xl text-app-primary"></i><p class="ml-3">그래프 렌더링 중...</p></div>');
            if (!data?.nodes?.length) {
                return this.showError(this.elements.canvasGuide, "그래프 데이터를 생성할 수 없습니다.");
            }
            let mermaidSyntax = 'graph TD\n';
            const nodeClasses = {
                'USER': 'userNode', 'GROUP': 'groupNode', 'ROLE': 'roleNode',
                'PERMISSION_GRANTED': 'permGrantedNode', 'PERMISSION_DENIED': 'permDeniedNode'
            };
            Object.entries(nodeClasses).forEach(([key, val]) => {
                const styles = {
                    userNode: 'fill:#3b82f6,stroke:#2563eb', groupNode: 'fill:#10b981,stroke:#059669',
                    roleNode: 'fill:#8b5cf6,stroke:#7c3aed', permGrantedNode: 'fill:#22c55e,stroke:#16a34a',
                    permDeniedNode: 'fill:#ef4444,stroke:#dc2626'
                };
                mermaidSyntax += ` classDef ${val} ${styles[val]},color:#fff,stroke-width:2px\n`;
            });

            const nodeIdMap = new Map(data.nodes.map((node, i) => [node.id, `node_${i}`]));
            data.nodes.forEach(node => {
                const safeId = nodeIdMap.get(node.id);
                const label = (node.label || 'N/A').replace(/["`]/g, "'").trim();
                mermaidSyntax += ` ${safeId}("${node.type}|${label}")\n`;
                let nodeClass = nodeClasses[node.type];
                if (node.type === 'PERMISSION') {
                    nodeClass = node.properties?.granted ? nodeClasses.PERMISSION_GRANTED : nodeClasses.PERMISSION_DENIED;
                }
                if (nodeClass) mermaidSyntax += ` class ${safeId} ${nodeClass}\n`;
            });

            data.edges.forEach(edge => {
                const fromId = nodeIdMap.get(edge.from), toId = nodeIdMap.get(edge.to);
                const edgeLabel = (edge.label || '').replace(/["`]/g, "'").trim();
                if (fromId && toId) mermaidSyntax += ` ${fromId} --> |"${edgeLabel}"| ${toId}\n`;
            });

            try {
                // [오류 수정] 안정적인 Mermaid API 호출 방식 사용
                const { svg } = await mermaid.render('mermaid-graph-container', mermaidSyntax);
                this.elements.canvasContent.innerHTML = svg;
                this.hideGuide(true);
            } catch (e) {
                console.error('Mermaid 렌더링 오류:', e);
                this.showError(this.elements.canvasGuide, "그래프 렌더링 중 오류가 발생했습니다.");
            }
        }
        showGuide(html) {
            this.elements.canvasGuide.innerHTML = html;
            this.elements.canvasGuide.classList.remove('hidden');
            this.elements.canvasContent.classList.add('hidden');
        }
        hideGuide(isContentReady = false) {
            this.elements.canvasGuide.classList.add('hidden');
            if (isContentReady) this.elements.canvasContent.classList.remove('hidden');
        }
        showError(element, message) {
            element.innerHTML = `<div class="p-8 text-center text-red-500"><i class="fas fa-exclamation-triangle text-2xl mb-2"></i><p class="font-semibold">${message}</p></div>`;
            element.classList.remove('hidden');
            this.elements.canvasContent.classList.add('hidden');
        }
        bindAccordionEvents() {
            this.elements.explorerListContainer.querySelectorAll('.accordion-header').forEach(header => {
                header.addEventListener('click', () => {
                    const content = header.nextElementSibling;
                    const isOpen = header.classList.toggle('open');
                    header.querySelector('.accordion-icon').classList.toggle('rotate-180', isOpen);
                    content.style.maxHeight = isOpen ? `${content.scrollHeight}px` : '0px';
                });
            });
        }
        filterExplorer(term) {
            const searchTerm = term.trim().toLowerCase();
            this.elements.explorerListContainer.querySelectorAll('.explorer-item').forEach(item => {
                const name = (item.dataset.name || '').toLowerCase();
                const description = (item.dataset.description || '').toLowerCase();
                const isVisible = !searchTerm || name.includes(searchTerm) || description.includes(searchTerm);
                item.classList.toggle('hidden', !isVisible);
            });
        }
    }

    // --- API 통신 ---
    class StudioAPI {
        async fetchApi(url, options = {}) {
            try {
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
                const fetchOptions = { ...options, headers: { ...options.headers } };
                if (csrfToken && csrfHeader && options.method?.toUpperCase() !== 'GET') {
                    fetchOptions.headers[csrfHeader] = csrfToken;
                }
                if (options.body && ! (options.body instanceof URLSearchParams)) {
                    fetchOptions.headers['Content-Type'] = 'application/json';
                }
                const response = await fetch(url, fetchOptions);
                if (!response.ok) throw new Error(`서버 오류: ${response.status}`);
                if (response.redirected) { window.location.href = response.url; return null; }
                return response.status === 204 ? null : response.json();
            } catch (error) {
                console.error(`API 오류: ${url}`, error);
                if (typeof showToast === 'function') showToast(error.message, 'error');
                throw error;
            }
        }
        getExplorerItems() { return this.fetchApi('/admin/studio/api/explorer-items'); }
        getAccessPathAsGraph(subjectId, subjectType, permissionId) {
            return this.fetchApi(`/admin/studio/api/access-path-graph?subjectId=${subjectId}&subjectType=${subjectType}&permissionId=${permissionId}`);
        }
    }

    // --- 애플리케이션 총괄 ---
    class StudioApp {
        constructor() {
            this.elements = {
                explorerListContainer: document.getElementById('explorer-list-container'),
                search: document.getElementById('explorer-search'),
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
            this.ui.showGuide('<div class="flex items-center justify-center h-full"><i class="fas fa-spinner fa-spin text-3xl text-app-primary"></i></div>');
            try {
                const data = await this.api.getExplorerItems();
                this.ui.renderExplorer(data);
                this.ui.showGuide('<i class="fas fa-mouse-pointer text-6xl text-slate-300"></i><p class="mt-4 text-lg">왼쪽 탐색기에서 분석할 주체를 선택하세요.</p>');
            } catch (error) {
                this.ui.showError(this.elements.explorerListContainer, '탐색기 목록 로딩 실패');
            }
        }

        bindEventListeners() {
            this.elements.explorerListContainer.addEventListener('click', e => this.handleExplorerClick(e));
            this.elements.search.addEventListener('keyup', e => this.ui.filterExplorer(e.target.value));
            this.elements.inspectorContent.addEventListener('click', e => {
                if (e.target.closest('#manage-membership-btn')) this.handleManageMembershipClick(e);
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

        handleManageMembershipClick(event) {
            event.preventDefault();
            const subject = this.state.getSubject();
            if (!subject) {
                showToast("관리할 주체를 먼저 선택해주세요.", "error");
                return;
            }
            const manageBtn = document.getElementById('manage-membership-btn');
            const originalBtnText = manageBtn ? manageBtn.innerHTML : '';
            if (manageBtn) this.ui.setLoading(manageBtn, true, originalBtnText);

            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/admin/granting-wizard/start';
            form.style.display = 'none';
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            form.appendChild(this.createHiddenInput('_csrf', csrfToken));
            form.appendChild(this.createHiddenInput('subjectId', subject.id));
            form.appendChild(this.createHiddenInput('subjectType', subject.type));
            document.body.appendChild(form);
            form.submit();
        }

        createHiddenInput(name, value) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value;
            return input;
        }

        async updateCanvasAndInspector() {
            this.ui.renderInspector(this.state);
            const subject = this.state.getSubject();
            const permission = this.state.getPermission();

            if (subject && permission) {
                try {
                    const data = await this.api.getAccessPathAsGraph(subject.id, subject.type, permission.id);
                    await this.ui.renderAccessGraph(data);
                } catch (error) {
                    this.ui.showError(this.elements.canvasGuide, '분석 데이터 로딩 실패');
                }
            } else if (subject) {
                this.ui.showGuide(`<div class="text-center"><i class="fas fa-check-circle text-4xl text-green-500"></i><p class="mt-4 text-lg font-bold">'<strong>${subject.name}</strong>' 선택됨.</p><p class="mt-2 text-slate-500">2. 이제 분석하고 싶은 '권한'을 선택하여 접근 경로를 확인하세요.</p></div>`);
            } else {
                this.ui.showGuide('<i class="fas fa-mouse-pointer text-6xl text-slate-300"></i><p class="mt-4 text-lg">왼쪽 탐색기에서 분석할 주체를 선택하세요.</p>');
            }
        }
    }

    new StudioApp().init();
});