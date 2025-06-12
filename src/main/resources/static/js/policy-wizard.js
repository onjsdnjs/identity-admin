class PolicyWizardApp {
    constructor() {
        this.state = {
            currentStep: 1,
            totalSteps: 3,
            contextId: document.getElementById('wizardContextId').value,
            subjects: {
                users: new Set(),
                groups: new Set()
            },
            permissions: new Set()
        };

        this.elements = {
            steps: document.querySelectorAll('.wizard-card'),
            indicators: document.querySelectorAll('.step-item'),
            prevBtn: document.getElementById('prev-btn'),
            nextBtn: document.getElementById('next-btn'),
            commitBtn: document.getElementById('commit-btn')
        };

        this.api = new WizardApi(this.state.contextId);
        this.init();
    }

    init() {
        this.bindEventListeners();
        this.updateView();
    }

    bindEventListeners() {
        this.elements.nextBtn.addEventListener('click', () => this.nextStep());
        this.elements.prevBtn.addEventListener('click', () => this.prevStep());
        this.elements.commitBtn.addEventListener('click', () => this.commitPolicy());
    }

    updateView() {
        this.elements.steps.forEach((stepEl, index) => {
            stepEl.classList.toggle('active', (index + 1) === this.state.currentStep);
        });

        this.elements.indicators.forEach((indicatorEl, index) => {
            const stepNum = index + 1;
            indicatorEl.classList.remove('step-active', 'step-complete', 'step-inactive');
            if (stepNum < this.state.currentStep) {
                indicatorEl.classList.add('step-complete');
            } else if (stepNum === this.state.currentStep) {
                indicatorEl.classList.add('step-active');
            } else {
                indicatorEl.classList.add('step-inactive');
            }
        });

        this.elements.prevBtn.disabled = this.state.currentStep === 1;
        this.elements.nextBtn.classList.toggle('hidden', this.state.currentStep === this.state.totalSteps);
        this.elements.commitBtn.classList.toggle('hidden', this.state.currentStep !== this.state.totalSteps);
    }

    async nextStep() {
        if (this.state.currentStep >= this.state.totalSteps) return;

        const success = await this.saveCurrentStepData();
        if (success) {
            this.state.currentStep++;
            if (this.state.currentStep === this.state.totalSteps) {
                this.generateReviewSummary();
            }
            this.updateView();
        }
    }

    prevStep() {
        if (this.state.currentStep <= 1) return;
        this.state.currentStep--;
        this.updateView();
    }

    async saveCurrentStepData() {
        this.elements.nextBtn.disabled = true;
        try {
            if (this.state.currentStep === 1) {
                const userIds = Array.from(document.getElementById('subject-users').selectedOptions).map(opt => Number(opt.value));
                const groupIds = Array.from(document.getElementById('subject-groups').selectedOptions).map(opt => Number(opt.value));
                if (userIds.length === 0 && groupIds.length === 0) {
                    showToast('하나 이상의 사용자 또는 그룹을 선택해야 합니다.', 'error');
                    return false;
                }
                await this.api.saveSubjects({ userIds, groupIds });
            } else if (this.state.currentStep === 2) {
                const permissionIds = Array.from(document.querySelectorAll('input[name="permissions"]:checked')).map(chk => Number(chk.value));
                if (permissionIds.length === 0) {
                    showToast('하나 이상의 권한을 선택해야 합니다.', 'error');
                    return false;
                }
                await this.api.savePermissions({ permissionIds });
            }
            return true;
        } catch(error) {
            showToast('단계 저장 중 오류가 발생했습니다.', 'error');
            return false;
        } finally {
            this.elements.nextBtn.disabled = false;
        }
    }

    generateReviewSummary() {
        const users = Array.from(document.getElementById('subject-users').selectedOptions).map(opt => opt.text);
        const groups = Array.from(document.getElementById('subject-groups').selectedOptions).map(opt => opt.text);
        const permissions = Array.from(document.querySelectorAll('input[name="permissions"]:checked')).map(chk => chk.parentElement.querySelector('p.font-semibold').textContent);

        const summaryHtml = `
            <div class="space-y-3">
                <div>
                    <p class="font-semibold text-gray-600">주체 (Who):</p>
                    <p class="text-sm pl-2"><span class="math-inline">\{\[\.\.\.users, \.\.\.groups\]\.join\(', '\) \|\| '선택된 주체 없음'\}</p\>
</div>
<div>
<p class="font-semibold text-gray-600">권한 (What):</p>
<p class="text-sm pl-2">{permissions.join(', ') || '선택된 권한 없음'}</p>
</div>
<div>
<p class="font-semibold text-gray-600">효과 (How):</p>
<p class="text-sm pl-2 text-green-600 font-bold">접근 허용 (ALLOW)</p>
</div>
</div>
`;
        document.getElementById('review-summary').innerHTML = summaryHtml;
    }

    async commitPolicy() {
        const policyName = document.getElementById('policyName').value;
        const policyDescription = document.getElementById('policyDescription').value;
        if (!policyName) {
            showToast('정책 이름은 필수입니다.', 'error');
            return;
        }

        this.elements.commitBtn.disabled = true;
        this.elements.commitBtn.textContent = '생성 중...';

        try {
            const result = await this.api.commitPolicy({ policyName, policyDescription });
            showToast(`정책(ID: ${result.id})이 성공적으로 생성되었습니다!`, 'success');
            setTimeout(() => {
                window.location.href = '/admin/policies';
            }, 1500);
        } catch (error) {
            showToast('정책 생성에 실패했습니다.', 'error');
            this.elements.commitBtn.disabled = false;
            this.elements.commitBtn.textContent = '정책 생성 및 적용';
        }
    }
}

class WizardApi {
    constructor(contextId) {
        this.basePath = `/admin/policy-wizard/${contextId}`;
    }

    async fetchApi(path, options = {}) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const fetchOptions = { method: 'POST', ...options };
            if (!fetchOptions.headers) fetchOptions.headers = {};
            if (options.body) fetchOptions.headers['Content-Type'] = 'application/json';
            if (csrfToken && csrfHeader) fetchOptions.headers[csrfHeader] = csrfToken;

            const response = await fetch(this.basePath + path, fetchOptions);
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `서버 오류 (상태: ${response.status})`}));
                throw new Error(errorData.message);
            }
            return response.status === 204 ? null : response.json();
        } catch (error) {
            console.error(`API Error fetching ${this.basePath + path}:`, error);
            throw error;
        }
    }

    saveSubjects(data) { return this.fetchApi('/subjects', { body: JSON.stringify(data) }); }
    savePermissions(data) { return this.fetchApi('/permissions', { body: JSON.stringify(data) }); }
    commitPolicy(data) { return this.fetchApi('/commit', { body: JSON.stringify(data) }); }
}

document.addEventListener('DOMContentLoaded', () => new PolicyWizardApp());