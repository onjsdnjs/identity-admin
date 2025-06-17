/**
 * [최종 수정본] 권한 부여 마법사 클라이언트 애플리케이션
 * DOM 요소 검색 시점을 init()으로 옮기고, null 체크를 강화하여 안정성을 확보합니다.
 */

// 1. 상태 관리 클래스
class PolicyWizardState {
    constructor(contextId) {
        this.currentStep = 1;
        this.totalSteps = 3;
        this.contextId = contextId;
        this.isPermissionPreselected = false;
    }

    nextStep() {
        let next = this.currentStep + 1;
        if (this.isPermissionPreselected && this.currentStep === 1) {
            next = 3;
        }
        if (next <= this.totalSteps) {
            this.currentStep = next;
        }
    }

    prevStep() {
        let prev = this.currentStep - 1;
        if (this.isPermissionPreselected && this.currentStep === 3) {
            prev = 1;
        }
        if (prev >= 1) {
            this.currentStep = prev;
        }
    }
}

// 2. UI 렌더링 클래스
class PolicyWizardUI {
    constructor() {
        this.elements = {
            steps: null, indicators: null, prevBtn: null,
            nextBtn: null, commitBtn: null
        };
    }

    queryElements() {
        this.elements.steps = document.querySelectorAll('.wizard-card');
        this.elements.indicators = document.querySelectorAll('.step-item');
        this.elements.prevBtn = document.getElementById('prev-btn');
        this.elements.nextBtn = document.getElementById('next-btn');
        this.elements.commitBtn = document.getElementById('commit-btn');
    }

    updateView(state) {
        if (!this.elements.steps || !this.elements.indicators) return;

        this.elements.steps.forEach((stepEl, index) => {
            stepEl.style.display = (index + 1) === state.currentStep ? 'block' : 'none';
        });

        this.elements.indicators.forEach((indicatorEl, index) => {
            const stepNum = index + 1;
            indicatorEl.classList.remove('step-active', 'step-complete', 'step-inactive');
            if (state.isPermissionPreselected && stepNum === 2) {
                indicatorEl.classList.add('step-complete');
            } else if (stepNum < state.currentStep) {
                indicatorEl.classList.add('step-complete');
            } else if (stepNum === state.currentStep) {
                indicatorEl.classList.add('step-active');
            } else {
                indicatorEl.classList.add('step-inactive');
            }
        });

        if (this.elements.prevBtn) this.elements.prevBtn.disabled = state.currentStep === 1;
        if (this.elements.nextBtn) this.elements.nextBtn.style.display = state.currentStep === state.totalSteps ? 'none' : 'inline-block';
        if (this.elements.commitBtn) this.elements.commitBtn.style.display = state.currentStep === state.totalSteps ? 'inline-block' : 'none';
    }

    generateReviewSummary() {
        const summaryEl = document.getElementById('review-summary');
        if(!summaryEl) return;

        const users = Array.from(document.getElementById('subject-users').selectedOptions).map(opt => opt.text);
        const groups = Array.from(document.getElementById('subject-groups').selectedOptions).map(opt => opt.text);
        const subjectsText = [...users, ...groups].join(', ') || '선택된 주체 없음';

        let permissionsText = '선택된 권한 없음';
        if (window.preselectedPermission) {
            permissionsText = window.preselectedPermission.friendlyName || window.preselectedPermission.name;
        } else {
            const selectedPerms = Array.from(document.querySelectorAll('input[name="permissions"]:checked')).map(chk => chk.parentElement.querySelector('p.font-semibold').textContent);
            if(selectedPerms.length > 0) permissionsText = selectedPerms.join(', ');
        }

        summaryEl.innerHTML = `
            <div class="space-y-3">
                <div><p class="font-semibold text-gray-400">주체 (Who):</p><p class="text-sm pl-2"><span class="math-inline">\{subjectsText\}</p\></div\>
<div><p class="font-semibold text-gray-400">권한 (What):</p><p class="text-sm pl-2">{permissionsText}</p></div>
<div><p class="font-semibold text-gray-400">효과 (How):</p><p class="text-sm pl-2 text-green-400 font-bold">접근 허용 (ALLOW)</p></div>
</div>`;
    }

    setLoading(button, isLoading, originalText) {
        if (!button) return;
        button.disabled = isLoading;
        button.innerHTML = isLoading ? '<i class="fas fa-spinner fa-spin mr-2"></i> 처리 중...' : originalText;
    }
}

// 3. API 통신 클래스
class WizardApi {
    constructor(contextId) { this.basePath = `/admin/policy-wizard/${contextId}`; }
    async fetchApi(path, options = {}) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const fetchOptions = { method: 'POST', ...options, headers: { ...options.headers } };
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
        const contextIdEl = document.getElementById('wizardContextId');
        if (!contextIdEl) { console.error('Wizard Context ID를 찾을 수 없습니다.'); return; }
        this.state = new PolicyWizardState(contextIdEl.value);
        this.ui = new PolicyWizardUI();
        this.api = new WizardApi(contextIdEl.value);
        if (window.preselectedPermission) this.state.isPermissionPreselected = true;
    }

    init() {
        this.ui.queryElements();
        this.bindEventListeners();
        this.ui.updateView(this.state);
    }

    bindEventListeners() {
        if (this.ui.elements.nextBtn) this.ui.elements.nextBtn.addEventListener('click', () => this.handleNextStep());
        if (this.ui.elements.prevBtn) this.ui.elements.prevBtn.addEventListener('click', () => this.handlePrevStep());
        if (this.ui.elements.commitBtn) this.ui.elements.commitBtn.addEventListener('click', () => this.handleCommit());
    }

    async handleNextStep() {
        if (this.state.currentStep >= this.state.totalSteps) return;
        this.ui.setLoading(this.ui.elements.nextBtn, true, '다음');
        const success = await this.saveCurrentStepData();
        this.ui.setLoading(this.ui.elements.nextBtn, false, '다음');
        if (success) {
            this.state.nextStep();
            if (this.state.currentStep === this.state.totalSteps) this.ui.generateReviewSummary();
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
                if (this.state.isPermissionPreselected) return true;
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
            console.log(`${error.message}`);
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
            setTimeout(() => { window.location.href = '/admin/policies'; }, 1500);
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
    const app = new PolicyWizardApp();
    if (app.state) { // app 객체가 정상적으로 생성되었는지 확인
        app.init();
    }
});