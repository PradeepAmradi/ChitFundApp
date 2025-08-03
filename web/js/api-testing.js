// API Testing JavaScript for ChitFund App

// Global variables
let currentToken = null;

// Utility functions
function getBaseUrl() {
    return document.getElementById('baseUrl').value.trim() || 'http://localhost:8080';
}

function showSection(sectionName) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    
    // Remove active class from all nav buttons
    document.querySelectorAll('.nav button').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected section
    document.getElementById(sectionName + '-section').style.display = 'block';
    
    // Add active class to clicked button
    event.target.classList.add('active');
}

function toggleEndpoint(header) {
    const body = header.nextElementSibling;
    const arrow = header.querySelector('.arrow');
    
    if (body.classList.contains('active')) {
        body.classList.remove('active');
        arrow.classList.remove('rotated');
    } else {
        body.classList.add('active');
        arrow.classList.add('rotated');
    }
}

function showLoading(containerId) {
    const container = document.getElementById(containerId);
    container.innerHTML = '<div class="response-container"><div class="response-status">Loading...<span class="loading"></span></div></div>';
}

function showResponse(containerId, response, status, isError = false) {
    const container = document.getElementById(containerId);
    const statusClass = isError ? 'error' : 'success';
    const statusText = isError ? `Error ${status}` : `Success ${status}`;
    
    container.innerHTML = `
        <div class="response-container">
            <div class="response-status ${statusClass}">
                Response: ${statusText}
            </div>
            <div class="response-body">${JSON.stringify(response, null, 2)}</div>
        </div>
    `;
}

async function makeApiCall(endpoint, method = 'GET', body = null, headers = {}) {
    const url = getBaseUrl() + endpoint;
    const config = {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...headers
        }
    };
    
    if (body) {
        config.body = JSON.stringify(body);
    }
    
    if (currentToken) {
        config.headers['Authorization'] = `Bearer ${currentToken}`;
    }
    
    try {
        const response = await fetch(url, config);
        const data = await response.json();
        
        // Store token if it's in the response
        if (data.token) {
            currentToken = data.token;
        }
        
        return {
            data,
            status: response.status,
            ok: response.ok
        };
    } catch (error) {
        return {
            data: { error: error.message },
            status: 0,
            ok: false
        };
    }
}

// Authentication API calls
async function testLogin() {
    const email = document.getElementById('login-email').value.trim();
    const mobile = document.getElementById('login-mobile').value.trim();
    
    if (!email && !mobile) {
        alert('Please provide either email or mobile number');
        return;
    }
    
    const requestBody = {};
    if (email) requestBody.email = email;
    if (mobile) requestBody.mobile = mobile;
    
    showLoading('login-response');
    
    const result = await makeApiCall('/api/v1/auth/login', 'POST', requestBody);
    showResponse('login-response', result.data, result.status, !result.ok);
}

async function testVerifyOtp() {
    const email = document.getElementById('verify-email').value.trim();
    const mobile = document.getElementById('verify-mobile').value.trim();
    const otp = document.getElementById('verify-otp').value.trim();
    
    if (!otp) {
        alert('Please provide OTP');
        return;
    }
    
    if (!email && !mobile) {
        alert('Please provide either email or mobile number');
        return;
    }
    
    const requestBody = { otp };
    if (email) requestBody.email = email;
    if (mobile) requestBody.mobile = mobile;
    
    showLoading('verify-otp-response');
    
    const result = await makeApiCall('/api/v1/auth/verify-otp', 'POST', requestBody);
    showResponse('verify-otp-response', result.data, result.status, !result.ok);
}

// User API calls
async function testGetProfile() {
    showLoading('profile-response');
    
    const result = await makeApiCall('/api/v1/users/profile');
    showResponse('profile-response', result.data, result.status, !result.ok);
}

// Chit API calls
async function testGetChits() {
    showLoading('chits-response');
    
    const result = await makeApiCall('/api/v1/chits');
    showResponse('chits-response', result.data, result.status, !result.ok);
}

async function testCreateChit() {
    const name = document.getElementById('chit-name').value.trim();
    const fundAmount = parseInt(document.getElementById('chit-amount').value);
    const tenure = parseInt(document.getElementById('chit-tenure').value);
    const memberCount = parseInt(document.getElementById('chit-members').value);
    const startMonth = document.getElementById('chit-start').value;
    const payoutMethod = document.getElementById('chit-payout').value;
    
    if (!name || !fundAmount || !tenure || !memberCount || !startMonth || !payoutMethod) {
        alert('Please fill in all required fields');
        return;
    }
    
    const requestBody = {
        name,
        fundAmount,
        tenure,
        memberCount,
        startMonth,
        payoutMethod
    };
    
    showLoading('create-chit-response');
    
    const result = await makeApiCall('/api/v1/chits', 'POST', requestBody);
    showResponse('create-chit-response', result.data, result.status, !result.ok);
}

async function testGetChitById() {
    const chitId = document.getElementById('get-chit-id').value.trim();
    
    if (!chitId) {
        alert('Please provide Chit ID');
        return;
    }
    
    showLoading('get-chit-response');
    
    const result = await makeApiCall(`/api/v1/chits/${chitId}`);
    showResponse('get-chit-response', result.data, result.status, !result.ok);
}

async function testInviteMember() {
    const chitId = document.getElementById('invite-chit-id').value.trim();
    const email = document.getElementById('invite-email').value.trim();
    const mobile = document.getElementById('invite-mobile').value.trim();
    
    if (!chitId) {
        alert('Please provide Chit ID');
        return;
    }
    
    if (!email && !mobile) {
        alert('Please provide either email or mobile number');
        return;
    }
    
    const requestBody = { chitId };
    if (email) requestBody.email = email;
    if (mobile) requestBody.mobile = mobile;
    
    showLoading('invite-response');
    
    const result = await makeApiCall(`/api/v1/chits/${chitId}/invite`, 'POST', requestBody);
    showResponse('invite-response', result.data, result.status, !result.ok);
}

async function testJoinChit() {
    const chitId = document.getElementById('join-chit-id').value.trim();
    
    if (!chitId) {
        alert('Please provide Chit ID');
        return;
    }
    
    const requestBody = { chitId };
    
    showLoading('join-response');
    
    const result = await makeApiCall(`/api/v1/chits/${chitId}/join`, 'POST', requestBody);
    showResponse('join-response', result.data, result.status, !result.ok);
}

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    // Set default start month to current month
    const now = new Date();
    const currentMonth = now.toISOString().substr(0, 7);
    document.getElementById('chit-start').value = currentMonth;
    
    // Check if backend is running
    checkBackendStatus();
});

async function checkBackendStatus() {
    try {
        const response = await fetch(getBaseUrl() + '/health');
        if (response.ok) {
            console.log('Backend is running');
        } else {
            console.warn('Backend returned status:', response.status);
        }
    } catch (error) {
        console.warn('Backend not accessible:', error.message);
        showBackendWarning();
    }
}

function showBackendWarning() {
    const warning = document.createElement('div');
    warning.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #ffc107;
        color: #212529;
        padding: 15px;
        border-radius: 5px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        z-index: 1000;
        max-width: 300px;
    `;
    warning.innerHTML = `
        <strong>⚠️ Backend Not Running</strong><br>
        Make sure your ChitFund backend is running on ${getBaseUrl()}
        <button onclick="this.parentElement.remove()" style="float: right; background: none; border: none; font-size: 18px; cursor: pointer;">×</button>
    `;
    document.body.appendChild(warning);
    
    // Auto-remove after 10 seconds
    setTimeout(() => {
        if (warning.parentElement) {
            warning.remove();
        }
    }, 10000);
}

// Export functions for testing
window.apiTesting = {
    testLogin,
    testVerifyOtp,
    testGetProfile,
    testGetChits,
    testCreateChit,
    testGetChitById,
    testInviteMember,
    testJoinChit,
    showSection,
    toggleEndpoint
};
