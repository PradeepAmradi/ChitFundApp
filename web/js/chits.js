// Chit management functionality
class ChitManager {
    constructor() {
        this.currentChit = null;
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Any chit-specific event listeners can be added here
    }

    async getChitDetails(chitId) {
        try {
            const response = await chitAPI.getChitDetails(chitId);
            if (response.success) {
                this.currentChit = response.data;
                return response.data;
            } else {
                throw new Error(response.message || 'Failed to load chit details');
            }
        } catch (error) {
            console.error('Error getting chit details:', error);
            throw error;
        }
    }

    async inviteMember(chitId, email) {
        try {
            if (!Utils.validateEmail(email)) {
                throw new Error('Please enter a valid email address');
            }

            const response = await chitAPI.inviteMember(chitId, email);
            if (response.success) {
                return response;
            } else {
                throw new Error(response.message || 'Failed to send invitation');
            }
        } catch (error) {
            console.error('Error inviting member:', error);
            throw error;
        }
    }

    async joinChit(chitId) {
        try {
            const response = await chitAPI.joinChit(chitId);
            if (response.success) {
                return response;
            } else {
                throw new Error(response.message || 'Failed to join chit');
            }
        } catch (error) {
            console.error('Error joining chit:', error);
            throw error;
        }
    }

    generateChitSummary(chit) {
        const monthlyAmount = Math.floor(chit.fundAmount / chit.memberCount / 100); // Convert to rupees
        const totalContribution = Math.floor(chit.fundAmount * chit.tenure / 100);

        return `
            <div class="chit-summary">
                <h4>Chit Summary</h4>
                <div class="summary-grid">
                    <div class="summary-item">
                        <label>Total Fund:</label>
                        <span>${Utils.formatIndianCurrency(chit.fundAmount)}</span>
                    </div>
                    <div class="summary-item">
                        <label>Monthly Contribution:</label>
                        <span>₹${monthlyAmount.toLocaleString('en-IN')}</span>
                    </div>
                    <div class="summary-item">
                        <label>Your Total Contribution:</label>
                        <span>₹${totalContribution.toLocaleString('en-IN')}</span>
                    </div>
                    <div class="summary-item">
                        <label>Duration:</label>
                        <span>${chit.tenure} months</span>
                    </div>
                    <div class="summary-item">
                        <label>Expected Return:</label>
                        <span>${Utils.formatIndianCurrency(chit.fundAmount)}</span>
                    </div>
                </div>
            </div>
        `;
    }

    generateMembersList(members) {
        if (!members || members.length === 0) {
            return `
                <div class="empty-state">
                    <i class="fas fa-users"></i>
                    <h4>No members yet</h4>
                    <p>Invite members to join this chit</p>
                </div>
            `;
        }

        return `
            <div class="members-list">
                <h4>Members (${members.length})</h4>
                <div class="members-grid">
                    ${members.map(member => `
                        <div class="member-card">
                            <div class="member-info">
                                <h5>${member.userName}</h5>
                                <span class="status-badge status-${member.status.toLowerCase()}">
                                    ${member.status}
                                </span>
                            </div>
                            <div class="member-details">
                                <small>Joined: ${Utils.formatDate(member.joinedAt)}</small>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    generatePaymentHistory(payments) {
        if (!payments || payments.length === 0) {
            return `
                <div class="empty-state">
                    <i class="fas fa-credit-card"></i>
                    <h4>No payments yet</h4>
                    <p>Payment history will appear here</p>
                </div>
            `;
        }

        return `
            <div class="payment-history">
                <h4>Payment History</h4>
                <div class="payments-list">
                    ${payments.map(payment => `
                        <div class="payment-item">
                            <div class="payment-info">
                                <span class="payment-month">${Utils.formatMonth(payment.month)}</span>
                                <span class="payment-amount">${Utils.formatIndianCurrency(payment.amount)}</span>
                            </div>
                            <span class="status-badge status-${payment.status.toLowerCase()}">
                                ${payment.status}
                            </span>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    validateChitCreation(data) {
        const errors = [];

        if (!data.name || data.name.trim() === '') {
            errors.push('Chit name is required');
        }

        if (!Utils.validateChitAmount(data.fundAmount)) {
            errors.push('Fund amount must be between ₹1L and ₹50L');
        }

        if (!Utils.validateTenure(data.tenure)) {
            errors.push('Tenure must be between 12 and 24 months');
        }

        if (!Utils.validateMemberCount(data.memberCount)) {
            errors.push('Member count must be between 10 and 25 (multiples of 5)');
        }

        if (!data.startMonth) {
            errors.push('Start month is required');
        } else {
            const startDate = new Date(data.startMonth);
            const currentDate = new Date();
            if (startDate < currentDate) {
                errors.push('Start month cannot be in the past');
            }
        }

        if (!data.payoutMethod) {
            errors.push('Payout method is required');
        }

        return errors;
    }

    calculateChitMetrics(chit) {
        const monthlyContribution = Math.floor(chit.fundAmount / chit.memberCount / 100);
        const totalContribution = monthlyContribution * chit.tenure;
        const potentialSavings = Math.floor(chit.fundAmount / 100) - totalContribution;
        
        return {
            monthlyContribution,
            totalContribution,
            potentialSavings,
            completionPercentage: chit.members ? (chit.members.length / chit.memberCount) * 100 : 0
        };
    }

    getChitStatusColor(status) {
        switch (status) {
            case 'OPEN':
                return '#ffc107';
            case 'ACTIVE':
                return '#28a745';
            case 'CLOSED':
                return '#dc3545';
            default:
                return '#6c757d';
        }
    }

    formatChitStatus(status) {
        switch (status) {
            case 'OPEN':
                return 'Open for joining';
            case 'ACTIVE':
                return 'Active';
            case 'CLOSED':
                return 'Completed';
            default:
                return status;
        }
    }

    generateChitProgressBar(current, total) {
        const percentage = (current / total) * 100;
        return `
            <div class="progress-bar">
                <div class="progress-label">
                    Members: ${current}/${total} (${Math.round(percentage)}%)
                </div>
                <div class="progress-track">
                    <div class="progress-fill" style="width: ${percentage}%"></div>
                </div>
            </div>
        `;
    }
}

// Initialize chit manager
document.addEventListener('DOMContentLoaded', () => {
    window.chitManager = new ChitManager();
});