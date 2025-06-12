class PolicyBuilderApp {
    constructor() {
        this.state = {
            subjects: new Map(),
            permissions: new Map(),
            conditions: new Map()
        };

        this.elements = {
            subjectsPalette: document.getElementById('subjects-palette'),
            permissionsPalette: document.getElementById('permissions-palette'),
            conditionsPalette: document.getElementById('conditions-palette'),
            subjectsCanvas: document.getElementById('subjects-canvas'),
            permissionsCanvas: document.getElementById('permissions-canvas'),
            conditionsCanvas: document.getElementById('conditions-canvas'),
            policyPreview: document.getElementById('policy-preview'),
            saveBtn: document.getElementById('save-policy-btn')
        };

        this.init();
    }

    init() {
        this.bindEventListeners();
    }

    bindEventListeners() {
        this.elements.subjectsPalette.addEventListener('click', e => this.handlePaletteClick(e, 'subject'));
        this.elements.permissionsPalette.addEventListener('click', e => this.handlePaletteClick(e, 'permission'));
        this.elements.conditionsPalette.addEventListener('click', e => this.handlePaletteClick(e, 'condition'));

        this.elements.subjectsCanvas.addEventListener('click', e => this.handleChipRemove(e, 'subject'));
        this.elements.permissionsCanvas.addEventListener('click', e => this.handleChipRemove(e, 'permission'));
        this.elements.conditionsCanvas.addEventListener('click', e => this.handleChipRemove(e, 'condition'));

        this.elements.saveBtn.addEventListener('click', () => this.savePolicy());
    }

    handlePaletteClick(event, type) {
        const item = event.target.closest('.palette-item');
        if (!item) return;
        const [id, name] = item.dataset.info.split(':');
        const mapKey = type === 'subject' ? item.dataset.info : id;

        if (type === 'subject') this.state.subjects.set(mapKey, { id, name });
        if (type === 'permission') this.state.permissions.set(mapKey, { id, name });
        if (type === 'condition') this.state.conditions.set(mapKey, { id, name });

        this.renderCanvas();
    }

    handleChipRemove(event, type) {
        const chip = event.target.closest('.policy-chip');
        if (!chip) return;
        const mapKey = chip.dataset.key;

        if (type === 'subject') this.state.subjects.delete(mapKey);
        if (type === 'permission') this.state.permissions.delete(mapKey);
        if (type === 'condition') this.state.conditions.delete(mapKey);

        this.renderCanvas();
    }

    renderCanvas() {
        this.renderChipZone(this.elements.subjectsCanvas, this.state.subjects, 'subject');
        this.renderChipZone(this.elements.permissionsCanvas, this.state.permissions, 'permission');
        this.renderChipZone(this.elements.conditionsCanvas, this.state.conditions, 'condition');
        this.updatePreview();
    }

    renderChipZone(canvasEl, map, type) {
        canvasEl.innerHTML = '';
        if (map.size === 0) {
            canvasEl.innerHTML = `<span class="text-gray-400">왼쪽에서 ${this.getKoreanTypeName(type)}을 선택하세요.</span>`;
            return;
        }
        map.forEach((value, key) => {
            const chip = document.createElement('span');
            chip.className = 'policy-chip bg-indigo-100 text-indigo-800 text-sm font-medium mr-2 px-2.5 py-1 rounded-full flex items-center';
            chip.dataset.key = key;
            chip.innerHTML = `${value.name} <button class="ml-2 text-indigo-500 hover:text-indigo-800">&times;</button>`;
            canvasEl.appendChild(chip);
        });
    }

    updatePreview() {
        const subjects = Array.from(this.state.subjects.values()).map(s => s.name).join(', ');
        const permissions = Array.from(this.state.permissions.values()).map(p => p.name).join(', ');
        const conditions = Array.from(this.state.conditions.values()).map(c => c.name).join(' 그리고 ');

        let previewText = '';
        if (subjects) previewText += `[${subjects}] 주체는 `;
        if (permissions) previewText += `[${permissions}] 권한으로 `;
        if (conditions) previewText += `[${conditions}] 조건 하에 `;
        if(previewText) previewText += "접근을 허용합니다.";

        this.elements.policyPreview.textContent = previewText || '';
    }

    async savePolicy() {
        const visualPolicyDto = {
            name: document.getElementById('policy-name').value,
            description: document.getElementById('policy-desc').value,
            effect: document.getElementById('policy-effect').value,
            subjects: Array.from(this.state.subjects.keys()).map(key => {
                const [type, id] = key.split(':');
                return { id: Number(id), type };
            }),
            permissions: Array.from(this.state.permissions.keys()).map(id => ({ id: Number(id) })),
            conditions: Array.from(this.state.conditions.keys()).map(id => ({ conditionKey: id, params: {} }))
        };

        if (!visualPolicyDto.name || visualPolicyDto.subjects.length === 0 || visualPolicyDto.permissions.length === 0) {
            alert("정책 이름, 주체, 권한은 필수 항목입니다.");
            return;
        }

        // API 호출
        try {
            const response = await fetch('/admin/policy-builder/api/build', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', /* CSRF Token */ },
                body: JSON.stringify(visualPolicyDto)
            });
            if (!response.ok) throw new Error('정책 생성에 실패했습니다.');
            const createdPolicy = await response.json();
            alert(`성공: 정책 "${createdPolicy.name}" (ID: ${createdPolicy.id}) 이(가) 생성되었습니다.`);
            window.location.href = '/admin/policies';
        } catch (error) {
            alert(error.message);
        }
    }

    getKoreanTypeName(type) {
        if (type === 'subject') return '주체';
        if (type === 'permission') return '권한';
        if (type === 'condition') return '조건';
        return '';
    }
}

new PolicyBuilderApp();