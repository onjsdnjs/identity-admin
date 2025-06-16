/**
 * [최종 리팩토링] 권한 부여 마법사 클라이언트 애플리케이션
 * 역할을 분리하여 코드의 구조를 개선하고 유지보수성을 높입니다.
 * - PolicyWizardState: 애플리케이션의 상태 관리
 * - PolicyWizardUI: 모든 DOM 조작 및 렌더링 담당
 * - WizardApi: 서버와의 모든 통신 담당
 * - PolicyWizardApp: 위 컴포넌트들을 조정하는 메인 컨트롤러
 */

// 1. 상태 관리 클래스
class PolicyWizardState {
    constructor(contextId) {
        this.currentStep = 1;
        this.totalSteps = 3;
        this.contextId = contextId;
    }

    nextStep() {
        if (this.currentStep < this.totalSteps) this.currentStep++;
    }

    prevStep() {
        if (this.currentStep > 1) this.currentStep--;
    }
}

// 2. UI 렌더링 클래스
class PolicyWizardUI {
    constructor(elements) {
        this.elements = elements;
    }

    updateView(state) {
        this.elements.steps.forEach((stepEl, index) => {
            stepEl.classList.toggle('active', (index + 1) === state.currentStep);
        });

        this.elements.indicators.forEach((indicatorEl, index) => {
            const stepNum = index + 1;
            indicatorEl.classList.remove('step-active', 'step-complete', 'step-inactive');
            if (stepNum < state.currentStep) {
                indicatorEl.classList.add('step-complete');
            } else if (stepNum === state.currentStep) {
                indicatorEl.classList.add('step-active');
            } else {
                indicatorEl.classList.add('step-inactive');
            }
        });

        this.elements.prevBtn.disabled = state.currentStep === 1;
        this.elements.nextBtn.classList.toggle('hidden', state.currentStep === state.totalSteps);
        this.elements.commitBtn.classList.toggle('hidden', state.currentStep !== state.totalSteps);
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

    setLoading(button, isLoading, originalText) {
        button.disabled = isLoading;
        button.innerHTML = isLoading
            ? '<i class="fas fa-spinner fa-spin mr-2"></i> 처리 중...'
            : originalText;
    }
}

// 3. API 통신 클래스
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
                const errorData = await response.json().catch(() => ({ message: `서버 오류 (상태: ${response.status})` }));
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

// 4. 메인 애플리케이션 클래스
class PolicyWizardApp {
    constructor() {
        const contextId = document.getElementById('wizardContextId').value;
        this.state = new PolicyWizardState(contextId);
        this.ui = new PolicyWizardUI({
            steps: document.querySelectorAll('.wizard-card'),
            indicators: document.querySelectorAll('.step-item'),
            prevBtn: document.getElementById('prev-btn'),
            nextBtn: document.getElementById('next-btn'),
            commitBtn: document.getElementById('commit-btn')
        });
        this.api = new WizardApi(contextId);
        this.init();
    }

    init() {
        this.bindEventListeners();
        this.ui.updateView(this.state);
    }

    bindEventListeners() {
        this.ui.elements.nextBtn.addEventListener('click', () => this.handleNextStep());
        this.ui.elements.prevBtn.addEventListener('click', () => this.handlePrevStep());
        this.ui.elements.commitBtn.addEventListener('click', () => this.handleCommit());
    }

    async handleNextStep() {
        if (this.state.currentStep >= this.state.totalSteps) return;

        this.ui.setLoading(this.ui.elements.nextBtn, true, '다음');
        const success = await this.saveCurrentStepData();
        this.ui.setLoading(this.ui.elements.nextBtn, false, '다음');

        if (success) {
            this.state.nextStep();
            if (this.state.currentStep === this.state.totalSteps) {
                this.ui.generateReviewSummary();
            }
            this.ui.updateView(this.state);
        }
    }

    handlePrevStep() {
        if (this.state.currentStep <= 1) return;
        this.state.prevStep();
        this.ui.updateView(this.state);
    }

    async saveCurrentStepData() {
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
            showToast(`단계 저장 중 오류 발생: ${error.message}`, 'error');
            return false;
        }
    }

    async handleCommit() {
        const policyName = document.getElementById('policyName').value;
        const policyDescription = document.getElementById('policyDescription').value;
        if (!policyName) {
            showToast('정책 이름은 필수입니다.', 'error');
            return;
        }

        this.ui.setLoading(this.ui.elements.commitBtn, true, '정책 생성 및 적용');

        try {
            const result = await this.api.commitPolicy({ policyName, policyDescription });
            showToast(`정책(ID: ${result.id})이 성공적으로 생성되었습니다!`, 'success');
            setTimeout(() => {
                window.location.href = '/admin/policies';
            }, 1500);
        } catch (error) {
            showToast(`정책 생성 실패: ${error.message}`, 'error');
            this.ui.setLoading(this.ui.elements.commitBtn, false, '정책 생성 및 적용');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if(typeof showToast !== 'function') {
        window.showToast = (message, type) => alert(`[${type.toUpperCase()}] ${message}`);
    }
    new PolicyWizardApp();
});