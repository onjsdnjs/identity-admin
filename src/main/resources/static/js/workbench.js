document.addEventListener('DOMContentLoaded', () => {
    // ================== DOM 요소 캐싱 ==================
    const resourceSearchInput = document.getElementById('resource-search-input');
    const resourceListContainer = document.getElementById('resource-list');
    const entitlementSubtitle = document.getElementById('entitlement-subtitle');
    const entitlementListContainer = document.getElementById('entitlement-list');
    const grantForm = document.getElementById('grant-form');
    const grantSubjectsSelect = document.getElementById('grant-subjects');
    const grantActionsSelect = document.getElementById('grant-actions');
    const grantReasonInput = document.getElementById('grant-reason');

    let selectedResource = null;
    let debounceTimer;

    // ================== API 호출 헬퍼 ==================
    const fetchAPI = async (url, options = {}) => {
        try {
            const defaultHeaders = { 'Content-Type': 'application/json' };
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (csrfToken && csrfHeader) {
                defaultHeaders[csrfHeader] = csrfToken;
            }

            const response = await fetch(url, {
                ...options,
                headers: { ...defaultHeaders, ...options.headers },
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류: ${response.status}` }));
                throw new Error(errorData.message || '알 수 없는 오류가 발생했습니다.');
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            console.error('API Error:', error);
            showToast(error.message, 'error');
            throw error; // 오류를 다시 던져서 호출한 쪽에서 처리할 수 있게 함
        }
    };

    // ================== 렌더링 함수 ==================
    const renderResources = (resources) => {
        resourceListContainer.innerHTML = '';
        if (!resources || resources.length === 0) {
            resourceListContainer.innerHTML = `<div class="p-4 text-center text-slate-500">결과가 없습니다.</div>`;
            return;
        }
        resources.forEach(res => {
            const div = document.createElement('div');
            div.className = 'p-3 rounded-lg cursor-pointer transition-colors border border-transparent hover:border-app-accent hover:bg-blue-50';
            div.dataset.resourceId = res.id;
            div.innerHTML = `
                <p class="font-semibold text-app-dark-gray">${res.friendlyName}</p>
                <p class="text-xs text-slate-500 truncate" title="${res.resourceIdentifier}">${res.resourceIdentifier}</p>
                <p class="text-xs text-blue-600 mt-1">${res.serviceOwner}</p>
            `;
            div.addEventListener('click', () => handleResourceSelect(div, res));
            resourceListContainer.appendChild(div);
        });
    };

    const renderEntitlements = (entitlements) => {
        entitlementListContainer.innerHTML = '';
        if (!entitlements || entitlements.length === 0) {
            entitlementListContainer.innerHTML = `<div class="p-4 text-center text-slate-500">부여된 접근 권한이 없습니다.</div>`;
            return;
        }
        entitlements.forEach(ent => {
            const card = document.createElement('div');
            card.className = 'bg-slate-50 p-4 rounded-lg border';
            const actionsHtml = ent.actions.length > 0 ? `<p><span class="font-semibold text-gray-700">행위:</span> ${ent.actions.join(', ')}</p>` : '';
            card.innerHTML = `
                <div class="flex justify-between items-start">
                    <div>
                        <p class="font-bold text-app-primary">${ent.subjectName}</p>
                        <p class="text-xs text-slate-400">유형: ${ent.subjectType} / 정책 ID: ${ent.policyId}</p>
                    </div>
                    <button data-policy-id="${ent.policyId}" class="revoke-btn text-xs bg-error text-white px-2 py-1 rounded hover:bg-red-700 transition-colors">회수</button>
                </div>
                <div class="mt-2 text-sm space-y-1">
                    ${actionsHtml}
                    <p class="text-xs text-gray-600"><span class="font-semibold">적용 규칙:</span> ${ent.conditions.join(' ')}</p>
                </div>
            `;
            entitlementListContainer.appendChild(card);
        });
    };

    // ================== 데이터 로드 및 이벤트 핸들러 ==================
    const loadAndRenderResources = async (keyword = '') => {
        try {
            resourceListContainer.innerHTML = `<div class="p-4 text-center text-slate-500">검색 중...</div>`;
            const data = await fetchAPI(`/api/workbench/resources?keyword=${encodeURIComponent(keyword)}`);
            renderResources(data.content);
        } catch (error) {
            resourceListContainer.innerHTML = `<div class="p-4 text-center text-error">리소스 로딩 실패.</div>`;
        }
    };

    const handleResourceSelect = (element, resource) => {
        selectedResource = resource;
        document.querySelectorAll('#resource-list > div').forEach(el => el.classList.remove('bg-app-accent', 'text-white', 'border-app-accent'));
        element.classList.add('bg-app-accent', 'text-white', 'border-app-accent');
        entitlementSubtitle.textContent = `'${resource.friendlyName}' 리소스의 권한 현황`;
        loadAndRenderEntitlements(resource.id);
    };

    const loadAndRenderEntitlements = async (resourceId) => {
        try {
            entitlementListContainer.innerHTML = `<div class="p-4 text-center text-slate-500">권한 정보 로딩 중...</div>`;
            const data = await fetchAPI(`/api/workbench/entitlements/by-resource?resourceId=${resourceId}`);
            renderEntitlements(data);
        } catch (error) {
            entitlementListContainer.innerHTML = `<div class="p-4 text-center text-error">권한 정보 로딩 실패.</div>`;
        }
    };

    const initGrantForm = async () => {
        try {
            const [subjectsData, actionsData] = await Promise.all([
                fetchAPI('/api/workbench/metadata/subjects'),
                fetchAPI('/api/workbench/metadata/actions')
            ]);

            grantSubjectsSelect.innerHTML = '';
            const groupOptGroup = document.createElement('optgroup');
            groupOptGroup.label = '그룹';
            subjectsData.groups.forEach(g => groupOptGroup.appendChild(new Option(`${g.name}`, `GROUP_${g.id}`)));
            grantSubjectsSelect.appendChild(groupOptGroup);

            const userOptGroup = document.createElement('optgroup');
            userOptGroup.label = '사용자';
            subjectsData.users.forEach(u => userOptGroup.appendChild(new Option(`${u.name} (${u.username})`, `USER_${u.id}`)));
            grantSubjectsSelect.appendChild(userOptGroup);

            grantActionsSelect.innerHTML = '';
            actionsData.forEach(a => grantActionsSelect.appendChild(new Option(`${a.name} (${a.actionType})`, a.id)));
        } catch (error) {
            grantSubjectsSelect.innerHTML = `<option>주체 로딩 실패</option>`;
            grantActionsSelect.innerHTML = `<option>행위 로딩 실패</option>`;
        }
    };

    // ================== 이벤트 리스너 바인딩 ==================
    resourceSearchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            loadAndRenderResources(e.target.value);
        }, 300);
    });

    entitlementListContainer.addEventListener('click', async (e) => {
        if (e.target.classList.contains('revoke-btn')) {
            const policyId = e.target.dataset.policyId;
            if (confirm(`정말로 이 권한(정책 ID: ${policyId})을 회수하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) {
                try {
                    await fetchAPI('/api/workbench/revocations', {
                        method: 'DELETE',
                        body: JSON.stringify({ policyId: parseInt(policyId), revokeReason: 'Revoked by admin from workbench' })
                    });
                    showToast('권한이 성공적으로 회수되었습니다.', 'success');
                    if (selectedResource) loadAndRenderEntitlements(selectedResource.id);
                } catch (error) { /* fetchAPI에서 이미 처리 */ }
            }
        }
    });

    grantForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!selectedResource) {
            showToast('먼저 좌측에서 리소스를 선택해주세요.', 'error');
            return;
        }

        const selectedSubjects = Array.from(grantSubjectsSelect.selectedOptions).map(opt => {
            const [type, id] = opt.value.split('_');
            return { id: parseInt(id), type };
        });

        if (selectedSubjects.length === 0) {
            showToast('하나 이상의 주체를 선택해주세요.', 'error');
            return;
        }

        const selectedActionIds = Array.from(grantActionsSelect.selectedOptions).map(opt => parseInt(opt.value));
        const reason = grantReasonInput.value;
        if (!reason) {
            showToast('권한 부여 사유를 입력해주세요.', 'error');
            return;
        }

        const grantRequest = {
            subjects: selectedSubjects,
            resourceIds: [selectedResource.id],
            actionIds: selectedActionIds,
            grantReason: reason
        };

        try {
            await fetchAPI('/api/workbench/grants', {
                method: 'POST',
                body: JSON.stringify(grantRequest)
            });
            showToast('새로운 권한이 성공적으로 부여되었습니다.', 'success');
            loadAndRenderEntitlements(selectedResource.id);
            grantForm.reset();
        } catch (error) { /* fetchAPI에서 이미 처리 */ }
    });

    // ================== 초기화 실행 ==================
    initGrantForm();
    loadAndRenderResources();
});