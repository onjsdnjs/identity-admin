document.addEventListener('DOMContentLoaded', async () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let metadata = {};

    // 1. 메타데이터 로드 및 Select2 초기화
    async function initializeForm() {
        try {
            const response = await fetch('/api/workbench/metadata/authoring-metadata');
            if (!response.ok) throw new Error('메타데이터 로드 실패');
            metadata = await response.json();

            // 사용자 선택
            $('#subjectUserIds').select2({
                placeholder: '사용자를 선택하세요',
                data: metadata.subjects.users.map(u => ({ id: u.id, text: `${u.name} (${u.username})` }))
            });

            // 그룹 선택
            $('#subjectGroupIds').select2({
                placeholder: '그룹을 선택하세요',
                data: metadata.subjects.groups.map(g => ({ id: g.id, text: g.name }))
            });

            // 자원 선택
            $('#businessResourceIds').select2({
                placeholder: '자원을 선택하세요',
                data: metadata.resources.map(r => ({ id: r.id, text: r.name }))
            });

            // 행위 선택
            $('#businessActionIds').select2({
                placeholder: '자원을 먼저 선택하세요',
                data: metadata.actions.map(a => ({ id: a.id, text: a.name }))
            });

            // 워크벤치에서 전달된 초기값 설정
            if (initialSelection.userId) {
                $('#subjectUserIds').val(initialSelection.userId).trigger('change');
            }
            if (initialSelection.groupId) {
                $('#subjectGroupIds').val(initialSelection.groupId).trigger('change');
            }
            if (initialSelection.resourceId) {
                $('#businessResourceIds').val(initialSelection.resourceId).trigger('change');
            }


        } catch (error) {
            showToast(error.message, 'error');
        }
    }

    // 2. 조건 추가 버튼 이벤트 리스너
    document.getElementById('add-condition-btn').addEventListener('click', () => {
        const container = document.getElementById('condition-container');
        const conditionIndex = container.children.length;

        const conditionHtml = `
            <div class="condition-item grid grid-cols-12 gap-4 items-center p-4 border rounded-lg">
                <div class="col-span-5">
                    <select class="condition-template-select form-input" data-index="${conditionIndex}">
                        <option value="">조건 유형 선택</option>
                        ${metadata.conditionTemplates.map(t => `<option value="${t.id}" data-params="${t.parameterCount}">${t.name}</option>`).join('')}
                    </select>
                </div>
                <div class="col-span-6 condition-params-container">
                    </div>
                <div class="col-span-1">
                    <button type="button" class="text-red-500 hover:text-red-700 remove-condition-btn">&times;</button>
                </div>
            </div>
        `;
        container.insertAdjacentHTML('beforeend', conditionHtml);
    });

    // 3. 조건 유형 변경 및 삭제 이벤트 위임
    document.getElementById('condition-container').addEventListener('change', (e) => {
        if (e.target.classList.contains('condition-template-select')) {
            const selectedOption = e.target.options[e.target.selectedIndex];
            const paramCount = parseInt(selectedOption.dataset.params || '0', 10);
            const paramsContainer = e.target.closest('.condition-item').querySelector('.condition-params-container');
            paramsContainer.innerHTML = '';
            for (let i = 0; i < paramCount; i++) {
                paramsContainer.innerHTML += `<input type="text" class="form-input mt-1 condition-param" placeholder="파라미터 ${i + 1} 입력">`;
            }
        }
    });
    document.getElementById('condition-container').addEventListener('click', (e) => {
        if (e.target.classList.contains('remove-condition-btn')) {
            e.target.closest('.condition-item').remove();
        }
    });


    // 4. 폼 제출 이벤트
    document.getElementById('policy-form').addEventListener('submit', async (e) => {
        e.preventDefault();

        const formData = new FormData(e.target);
        const policyDto = {
            policyName: formData.get('policyName'),
            description: formData.get('description'),
            subjectUserIds: $('#subjectUserIds').val().map(Number),
            subjectGroupIds: $('#subjectGroupIds').val().map(Number),
            businessResourceIds: $('#businessResourceIds').val().map(Number),
            businessActionIds: $('#businessActionIds').val().map(Number),
            conditions: {}
        };

        // 동적으로 추가된 조건들을 수집
        document.querySelectorAll('.condition-item').forEach(item => {
            const templateId = item.querySelector('.condition-template-select').value;
            if (templateId) {
                const params = Array.from(item.querySelectorAll('.condition-param')).map(input => input.value);
                policyDto.conditions[templateId] = params;
            }
        });

        try {
            const response = await fetch('/api/admin/authoring/policies', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify(policyDto)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '정책 생성에 실패했습니다.');
            }
            showToast('정책이 성공적으로 생성되었습니다.', 'success');
            setTimeout(() => window.location.href = '/admin/policies', 1500);

        } catch (error) {
            showToast(error.message, 'error');
        }
    });

    initializeForm();
});