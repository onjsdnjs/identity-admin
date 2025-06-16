document.addEventListener('DOMContentLoaded', () => {
    const wizardContainer = document.getElementById('granting-wizard-container');
    if (!wizardContainer) return;

    const contextId = wizardContainer.dataset.contextId;
    const selectionPanel = document.getElementById('selection-panel');
    const assignmentList = document.getElementById('assignment-list');
    const simulationResultContainer = document.getElementById('simulation-result-container');
    const saveButton = document.getElementById('save-button');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let initialAssignmentIds = new Set();

    const allCheckboxes = selectionPanel.querySelectorAll('input[type="checkbox"]');

    const updateInitialAssignments = () => {
        initialAssignmentIds = new Set();
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                initialAssignmentIds.add(cb.value);
            }
        });
        updateAssignmentPanel();
    };

    const debounce = (func, delay) => {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    };

    const runSimulation = async () => {
        const currentAssignmentIds = new Set();
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                currentAssignmentIds.add(cb.value);
            }
        });

        const added = [...currentAssignmentIds].filter(id => !initialAssignmentIds.has(id));
        const removed = [...initialAssignmentIds].filter(id => currentAssignmentIds.has(id)); // This logic seems wrong, should be !currentAssignmentIds.has(id)

        // Corrected logic for removed IDs
        const actuallyRemoved = [...initialAssignmentIds].filter(id => !currentAssignmentIds.has(id));

        const changes = {
            added: added.map(id => ({ targetId: Number(id), targetType: 'GROUP' })),
            removedGroupIds: actuallyRemoved.map(id => Number(id))
        };

        simulationResultContainer.innerHTML = '<div class="flex items-center justify-center h-full"><i class="fas fa-spinner fa-spin text-3xl text-indigo-400"></i></div>';

        try {
            const response = await fetch(`/admin/granting-wizard/${contextId}/simulate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
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

    const updateAssignmentPanel = () => {
        assignmentList.innerHTML = '';
        allCheckboxes.forEach(cb => {
            if (cb.checked) {
                const label = document.querySelector(`label[for="${cb.id}"]`).textContent;
                const isInitial = initialAssignmentIds.has(cb.value);
                const itemHtml = `
                    <div class="p-3 rounded-md flex justify-between items-center ${isInitial ? 'bg-slate-700/50' : 'bg-green-500/20'}">
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

    // 이벤트 리스너
    selectionPanel.addEventListener('change', (e) => {
        if (e.target.type === 'checkbox') {
            updateAssignmentPanel();
            debounce(runSimulation, 500)();
        }
    });

    saveButton.addEventListener('click', () => {
        // 최종 할당 정보를 form에 담아 제출
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/admin/granting-wizard/${contextId}/commit`;

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = csrfHeader;
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
                typeInput.value = 'GROUP'; // 현재는 그룹만 가정
                form.appendChild(typeInput);
                index++;
            }
        });

        document.body.appendChild(form);
        form.submit();
    });

    // 초기화
    updateInitialAssignments();
});