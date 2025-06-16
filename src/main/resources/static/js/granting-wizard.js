document.addEventListener('DOMContentLoaded', () => {
    const wizardContainer = document.getElementById('granting-wizard-container');
    if (!wizardContainer) return;

    // --- DOM 요소 및 데이터 캐싱 ---
    const contextId = wizardContainer.dataset.contextId;
    const assignmentType = wizardContainer.dataset.assignmentType;
    const selectionPanel = document.getElementById('selection-panel');
    const assignmentList = document.getElementById('assignment-list');
    const simulationResultContainer = document.getElementById('simulation-result-container');
    const saveButton = document.getElementById('save-button');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const allCheckboxes = selectionPanel.querySelectorAll('.assignment-checkbox');

    let initialAssignmentIds = new Set();

    // --- 초기화 함수 (완성) ---
    const initialize = () => {
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                initialAssignmentIds.add(cb.value);
            }
        });
        updateAssignmentPanel();
    };

    // --- 유틸리티 함수 ---
    const debounce = (func, delay) => {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    };

    // --- 핵심 로직 함수 ---
    const runSimulation = async () => {
        const currentAssignmentIds = new Set();
        allCheckboxes.forEach(cb => {
            if (cb.checked) currentAssignmentIds.add(cb.value);
        });

        const addedAssignments = [...currentAssignmentIds]
            .filter(id => !initialAssignmentIds.has(id))
            .map(id => ({ targetId: Number(id), targetType: assignmentType }));

        const removedIds = [...initialAssignmentIds]
            .filter(id => !currentAssignmentIds.has(id))
            .map(id => Number(id));

        const changes = {
            added: addedAssignments,
            // 현재 설계상으로는 added에 모든 최종 목록이 담겨 서버에서 처리하므로, removed는 향후 확장용
            removedGroupIds: assignmentType === 'GROUP' ? removedIds : [],
            removedRoleIds: assignmentType === 'ROLE' ? removedIds : []
        };

        simulationResultContainer.innerHTML = '<div class="flex items-center justify-center h-full"><i class="fas fa-spinner fa-spin text-3xl text-indigo-400"></i><p class="ml-3 text-slate-400">분석 중...</p></div>';

        try {
            const response = await fetch(`/admin/granting-wizard/${contextId}/simulate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify(changes)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: '시뮬레이션 서버 오류' }));
                throw new Error(errorData.message);
            }
            const result = await response.json();
            renderSimulationResult(result);
        } catch (error) {
            simulationResultContainer.innerHTML = `<div class="p-4 text-red-400 bg-red-900/20 rounded-lg">${error.message}</div>`;
        }
    };

    const handleSave = () => {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/admin/granting-wizard/${contextId}/commit`;

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf'; // Thymeleaf가 처리하는 기본 이름
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);

        let index = 0;
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                const idInput = document.createElement('input');
                idInput.type = 'hidden';
                idInput.name = `assignments[${index}].targetId`;
                idInput.value = cb.value;
                form.appendChild(idInput);

                const typeInput = document.createElement('input');
                typeInput.type = 'hidden';
                typeInput.name = `assignments[${index}].targetType`;
                typeInput.value = assignmentType;
                form.appendChild(typeInput);
                index++;
            }
        });

        document.body.appendChild(form);
        saveButton.disabled = true;
        saveButton.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>저장 중...';
        form.submit();
    };

    // --- UI 렌더링 함수 ---
    const updateAssignmentPanel = () => {
        assignmentList.innerHTML = '';
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                const label = document.querySelector(`label[for="${cb.id}"]`).textContent;
                const isInitial = initialAssignmentIds.has(cb.value);
                const itemHtml = `
                    <div class="p-3 rounded-md flex justify-between items-center transition-all ${isInitial ? 'bg-slate-700/50' : 'bg-green-500/20 animate-pulse'}">
                        <span>${label}</span>
                        <button type="button" data-id="${cb.id}" class="remove-assignment-btn text-slate-400 hover:text-white">×</button>
                    </div>`;
                assignmentList.insertAdjacentHTML('beforeend', itemHtml);
            }
        });

        document.querySelectorAll('.remove-assignment-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const checkbox = document.getElementById(btn.dataset.id);
                if(checkbox) {
                    checkbox.checked = false;
                    checkbox.dispatchEvent(new Event('change', { bubbles: true }));
                }
            });
        });
    };

    const renderSimulationResult = (result) => {
        if (!result || !result.impactDetails) {
            simulationResultContainer.innerHTML = `<div class="p-4 text-slate-400">분석 결과가 없습니다.</div>`;
            return;
        }

        let html = `<div class="p-2 bg-slate-800 rounded-md mb-4 text-center text-slate-300 font-semibold">${result.summary}</div>`;
        const gained = result.impactDetails.filter(d => d.impactType === 'PERMISSION_GAINED');
        const lost = result.impactDetails.filter(d => d.impactType === 'PERMISSION_LOST');

        if (gained.length > 0) {
            html += `<div class="mb-4">
                        <h4 class="font-bold text-green-400 mb-2"><i class="fas fa-plus-circle mr-2"></i>획득할 권한 (${gained.length})</h4>
                        <ul class="space-y-1 text-sm list-disc list-inside text-slate-300">
                            ${gained.map(d => `<li>${d.permissionName} <span class="text-xs text-slate-500">(${d.reason})</span></li>`).join('')}
                        </ul>
                     </div>`;
        }
        if (lost.length > 0) {
            html += `<div class="mb-4">
                        <h4 class="font-bold text-red-400 mb-2"><i class="fas fa-minus-circle mr-2"></i>상실할 권한 (${lost.length})</h4>
                        <ul class="space-y-1 text-sm list-disc list-inside text-slate-300">
                            ${lost.map(d => `<li>${d.permissionName}</li>`).join('')}
                        </ul>
                      </div>`;
        }

        if(gained.length === 0 && lost.length === 0) {
            html += `<div class="text-center text-slate-500">권한 변경사항이 없습니다.</div>`;
        }
        simulationResultContainer.innerHTML = html;
    };

    // --- 이벤트 리스너 바인딩 ---
    selectionPanel.addEventListener('change', (e) => {
        if (e.target.classList.contains('assignment-checkbox')) {
            updateAssignmentPanel();
            debounce(runSimulation, 500)();
        }
    });

    saveButton.addEventListener('click', handleSave);

    // --- 초기화 실행 ---
    initialize();
});