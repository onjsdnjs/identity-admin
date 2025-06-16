document.addEventListener('DOMContentLoaded', () => {
    const wizardContainer = document.getElementById('granting-wizard-container');
    if (!wizardContainer) return;

    // --- 유틸리티 함수 (오류 수정을 위해 상단으로 이동) ---
    const debounce = (func, delay) => {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    };

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
    const debouncedRunSimulation = debounce(runSimulation, 500); // 정상적으로 debounce 함수 사용

    // --- 초기화 함수 ---
    const initialize = () => {
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                initialAssignmentIds.add(cb.value);
            }
        });
        updateAssignmentPanel();
    };

    // --- 핵심 로직 함수 (Phase 3 구현 포함) ---
    async function runSimulation() {
        const addedAssignments = [];
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                addedAssignments.push({ targetId: Number(cb.value), targetType: assignmentType });
            }
        });

        const changes = { added: addedAssignments };
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
    }

    const handleSave = () => {
        saveButton.disabled = true;
        saveButton.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>저장 중...';

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/admin/granting-wizard/${contextId}/commit`;
        form.style.display = 'none';

        form.appendChild(createHiddenInput('_csrf', csrfToken));

        let index = 0;
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                form.appendChild(createHiddenInput(`added[${index}].targetId`, cb.value));
                form.appendChild(createHiddenInput(`added[${index}].targetType`, assignmentType));
                index++;
            }
        });

        document.body.appendChild(form);
        form.submit();
    };

    const createHiddenInput = (name, value) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        return input;
    };


    // --- UI 렌더링 함수 ---
    const updateAssignmentPanel = () => {
        assignmentList.innerHTML = '';
        let hasAssignments = false;
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                hasAssignments = true;
                const label = document.querySelector(`label[for="${cb.id}"]`).textContent;
                const isInitial = initialAssignmentIds.has(cb.value);
                const itemHtml = `
                    <div class="p-3 rounded-md flex justify-between items-center transition-all ${isInitial ? 'bg-slate-700/50' : 'bg-green-800/30 border border-green-600'}">
                        <span class="font-medium">${label} ${!isInitial ? '<span class="text-xs text-green-400">(추가됨)</span>' : ''}</span>
                        <button type="button" data-id="${cb.id}" class="remove-assignment-btn text-slate-400 hover:text-white text-xl leading-none">×</button>
                    </div>`;
                assignmentList.insertAdjacentHTML('beforeend', itemHtml);
            }
        });

        initialAssignmentIds.forEach(id => {
            const checkbox = document.getElementById('item-' + id);
            if(checkbox && !checkbox.checked) {
                const label = document.querySelector(`label[for="${checkbox.id}"]`).textContent;
                const itemHtml = `
                    <div class="p-3 rounded-md flex justify-between items-center transition-all bg-red-800/30 border border-red-600 opacity-60">
                        <span class="font-medium line-through">${label} <span class="text-xs text-red-400">(제거됨)</span></span>
                        <button type="button" data-id="${checkbox.id}" class="restore-assignment-btn text-slate-400 hover:text-white text-xl leading-none">+</button>
                    </div>`;
                assignmentList.insertAdjacentHTML('beforeend', itemHtml);
                hasAssignments = true;
            }
        });

        if (!hasAssignments) {
            assignmentList.innerHTML = `<div class="text-center text-slate-500 p-8">할당된 항목이 없습니다.</div>`;
        }

        document.querySelectorAll('.remove-assignment-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const checkbox = document.getElementById(btn.dataset.id);
                if(checkbox) {
                    checkbox.checked = false;
                    checkbox.dispatchEvent(new Event('change', { bubbles: true }));
                }
            });
        });

        document.querySelectorAll('.restore-assignment-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const checkbox = document.getElementById(btn.dataset.id);
                if(checkbox) {
                    checkbox.checked = true;
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
                        <ul class="space-y-1 text-sm list-inside text-slate-300">
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
            debouncedRunSimulation();
        }
    });

    saveButton.addEventListener('click', handleSave);

    // --- 초기화 실행 ---
    initialize();
});