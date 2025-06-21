// static/js/resource-workbench.js

// '권한 정의 & 정책 설정' 버튼 클릭 시
async function defineAndSetupPolicy(button) {
    const resourceId = button.dataset.resourceId;
    const form = button.closest('tr').querySelector('form'); // 해당 행의 form을 찾음
    const formData = new FormData(form);

    // 1. 먼저 '권한 정의' API를 호출하여 Permission을 생성합니다.
    try {
        const response = await fetch(`/admin/workbench/resources/${resourceId}/define`, {
            method: 'POST',
            body: new URLSearchParams(formData) // form-urlencoded 데이터로 전송
        });
        const result = await response.json(); // { permissionId: 123, permissionName: "..." }

        if (!response.ok) throw new Error(result.message || '권한 생성 실패');

        // 2. 성공 시, 선택 모달을 띄웁니다.
        showPolicySetupModal(resourceId, result.permissionId, result.friendlyName);

    } catch (error) {
        showToast('권한 생성 중 오류: ' + error.message, 'error');
    }
}

function showPolicySetupModal(resourceId, permissionId, permissionName) {
    document.getElementById('modal-permission-name').textContent = permissionName;
    document.getElementById('quickGrantPermissionId').value = permissionId;
    document.getElementById('advancedPolicyResourceId').value = resourceId;
    document.getElementById('advancedPolicyPermissionId').value = permissionId;
    document.getElementById('policySetupModal').classList.remove('hidden');
}

function closePolicySetupModal() {
    document.getElementById('policySetupModal').classList.add('hidden');
}

function selectQuickGrant() {
    // 빠른 권한 부여(마법사) 폼을 제출
    document.getElementById('quickGrantForm').submit();
}

function selectAdvancedPolicy() {
    // 상세 정책 설정(빌더) 폼을 제출
    document.getElementById('advancedPolicyForm').submit();
}