<script lang="ts">
    import { auth } from '$lib/auth.svelte';
    import { onMount } from 'svelte';
    import { fade, slide } from 'svelte/transition';

    interface Package {
        id: number;
        trackingNumber: string;
        city: string;
        status: string;
        address: string;
        expectedDelivery: string;
        deliveryMode: string;
    }

    let assignedPackages = $state<Package[]>([]);
    let isLoading = $state(true);

    // Modal state for failed deliveries
    let showCommentModal = $state(false);
    let selectedPackage = $state<Package | null>(null);
    let selectedStatus = $state("");
    let commentText = $state("");
    let commentError = $state("");

    onMount(async () => {
        try {
            // Using ID 1 for demonstration
            const res = await fetch("http://localhost:8080/api/parcels/courier/1");
            if (res.ok) {
                const data = await res.json();
                assignedPackages = data.map((p: any) => ({
                    id: p.parcelId,
                    trackingNumber: p.trackingNumber,
                    city: p.city,
                    status: p.status,
                    address: p.address || "No address provided",
                    expectedDelivery: p.expectedDelivery,
                    deliveryMode: p.deliveryMode || "NORMAL"
                }));
            }
        } catch (e) {
            console.error("Failed to fetch assigned packages", e);
        } finally {
            isLoading = false;
        }
    });

    async function handleSuccess(pkg: Package) {
        try {
            const res = await fetch(`http://localhost:8080/api/parcels/${pkg.id}/status`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ status: "Delivered", comment: "Successful delivery", employeeId: "1" })
            });
            if (res.ok) {
                assignedPackages = assignedPackages.map(p => p.id === pkg.id ? { ...p, status: 'Delivered' } : p);
            }
        } catch (e) {
            console.error("Failed to mark as delivered", e);
        }
    }

    function openIssueModal(pkg: Package, status: string) {
        selectedPackage = pkg;
        selectedStatus = status;
        commentText = "";
        commentError = "";
        showCommentModal = true;
    }

    async function submitIssue() {
        if (!commentText.trim()) {
            commentError = "A written comment is mandatory for this status.";
            return;
        }

        if (selectedPackage) {
            try {
                const res = await fetch(`http://localhost:8080/api/parcels/${selectedPackage.id}/status`, {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ status: selectedStatus, comment: commentText, employeeId: "1" })
                });
                
                if (res.ok) {
                    assignedPackages = assignedPackages.map(p => 
                        p.id === selectedPackage!.id 
                            ? { ...p, status: selectedStatus } 
                            : p
                    );
                }
            } catch (e) {
                console.error("Failed to submit issue", e);
            }
        }
        
        showCommentModal = false;
        selectedPackage = null;
    }
</script>

<div class="dashboard-panel animate-fade-in">
    <div class="panel-header">
        <div>
            <h2>My Deliveries</h2>
            <p style="color: var(--text-secondary);">Welcome back, {auth.user}. You have {assignedPackages.filter(p => p.status === 'Out for Delivery').length} packages to deliver today.</p>
        </div>
        <div class="date-badge">
            {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })}
        </div>
    </div>

    {#if isLoading}
        <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading your route...</p>
        </div>
    {:else if assignedPackages.length === 0}
        <div class="empty-state">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M8 12h8"></path>
            </svg>
            <p>You have no packages assigned to you right now.</p>
        </div>
    {:else}
        <div class="package-list">
            {#each assignedPackages as pkg (pkg.id)}
                <div class="glass-panel package-card" class:completed={pkg.status === 'Delivered'} class:failed={pkg.status === 'Lost' || pkg.status === 'Destroyed' || pkg.status === 'Failed'} in:slide>
                    <div class="pkg-info">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
                            <div>
                                <span class="badge" class:badge-warning={pkg.status === 'Out for Delivery'} class:badge-success={pkg.status === 'Delivered'} class:badge-danger={pkg.status !== 'Out for Delivery' && pkg.status !== 'Delivered'}>
                                    {pkg.status}
                                </span>
                                <span class="badge" class:badge-primary={pkg.deliveryMode === 'EXPRESS'} class:badge-outline={pkg.deliveryMode === 'NORMAL'} style="margin-left: 0.5rem;">
                                    {pkg.deliveryMode}
                                </span>
                                <h3 style="margin: 0.5rem 0 0.25rem 0; font-family: monospace;">{pkg.trackingNumber}</h3>
                            </div>
                            <div class="expected-time">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <circle cx="12" cy="12" r="10"></circle>
                                    <polyline points="12 6 12 12 16 14"></polyline>
                                </svg>
                                {pkg.expectedDelivery}
                            </div>
                        </div>

                        <div class="address-box">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2" style="flex-shrink: 0;">
                                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                                <circle cx="12" cy="10" r="3"></circle>
                            </svg>
                            <div>
                                <div style="font-weight: 600;">{pkg.address}</div>
                                <div style="color: var(--text-secondary); font-size: 0.875rem;">{pkg.city}</div>
                            </div>
                        </div>
                    </div>

                    {#if pkg.status === 'Out for Delivery'}
                        <div class="action-bar">
                            <button class="btn btn-success" onclick={() => handleSuccess(pkg)}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                Delivered
                            </button>
                            
                            <div class="dropdown">
                                <button class="btn btn-outline issue-btn">
                                    Report Issue
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="6 9 12 15 18 9"></polyline>
                                    </svg>
                                </button>
                                <div class="dropdown-content">
                                    <button onclick={() => openIssueModal(pkg, 'Failed Delivery')}>Recipient absent</button>
                                    <button onclick={() => openIssueModal(pkg, 'Damaged')}>Package damaged</button>
                                    <button onclick={() => openIssueModal(pkg, 'Lost')}>Lost in transit</button>
                                </div>
                            </div>
                        </div>
                    {/if}
                </div>
            {/each}
        </div>
    {/if}
</div>

{#if showCommentModal}
    <div class="modal-backdrop" transition:fade>
        <div class="glass-panel modal-content">
            <h3 style="color: var(--danger); display: flex; align-items: center; gap: 0.5rem;">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                    <line x1="12" y1="9" x2="12" y2="13"></line>
                    <line x1="12" y1="17" x2="12.01" y2="17"></line>
                </svg>
                Report Issue: {selectedStatus}
            </h3>
            
            <p style="margin-bottom: 1.5rem; color: var(--text-secondary);">
                You are marking package <strong style="font-family: monospace;">{selectedPackage?.trackingNumber}</strong> as <strong>{selectedStatus}</strong>.
            </p>

            <div class="input-group">
                <label>Mandatory Comment <span style="color: var(--danger);">*</span></label>
                <textarea 
                    bind:value={commentText} 
                    class="input-field" 
                    rows="4" 
                    placeholder="Provide details about why the delivery failed..."
                    style="border-color: {commentError ? 'var(--danger)' : 'var(--border-color)'}"
                ></textarea>
                {#if commentError}
                    <div style="color: var(--danger); font-size: 0.875rem; margin-top: 0.5rem;">{commentError}</div>
                {/if}
            </div>

            <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 2rem;">
                <button class="btn btn-outline" onclick={() => showCommentModal = false}>Cancel</button>
                <button class="btn btn-danger" onclick={submitIssue}>Confirm Status</button>
            </div>
        </div>
    </div>
{/if}

<style>
    .dashboard-panel {
        padding: 2rem;
    }

    .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
    }

    .date-badge {
        background: var(--surface-color);
        padding: 0.75rem 1.5rem;
        border-radius: var(--radius-full);
        box-shadow: var(--shadow-sm);
        border: 1px solid var(--border-color);
        font-weight: 500;
        color: var(--primary);
    }

    .package-list {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
    }

    .package-card {
        padding: 0;
        overflow: hidden;
        display: flex;
        flex-direction: column;
    }

    .package-card.completed {
        opacity: 0.7;
        border-left: 4px solid var(--secondary);
    }

    .package-card.failed {
        border-left: 4px solid var(--danger);
    }

    .pkg-info {
        padding: 1.5rem;
    }

    .expected-time {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.875rem;
        color: var(--text-tertiary);
        background: rgba(0,0,0,0.03);
        padding: 0.25rem 0.75rem;
        border-radius: var(--radius-full);
    }

    .address-box {
        display: flex;
        align-items: center;
        gap: 1rem;
        background: var(--bg-color);
        padding: 1rem;
        border-radius: var(--radius-md);
        border: 1px solid var(--border-color);
    }

    .action-bar {
        display: flex;
        gap: 1rem;
        padding: 1rem 1.5rem;
        background: rgba(0,0,0,0.02);
        border-top: 1px solid var(--border-color);
    }

    .btn-success {
        background-color: var(--secondary);
        color: white;
        flex: 1;
        display: flex;
        gap: 0.5rem;
    }
    
    .btn-success:hover {
        background-color: var(--secondary-hover);
    }

    .dropdown {
        position: relative;
        display: inline-block;
        flex: 1;
    }

    .issue-btn {
        width: 100%;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .dropdown-content {
        display: none;
        position: absolute;
        bottom: 100%;
        left: 0;
        width: 100%;
        margin-bottom: 0.5rem;
        background-color: var(--surface-color);
        min-width: 160px;
        box-shadow: var(--shadow-lg);
        border-radius: var(--radius-md);
        z-index: 10;
        border: 1px solid var(--border-color);
        overflow: hidden;
    }

    .dropdown:hover .dropdown-content {
        display: block;
        animation: fadeIn 0.2s;
    }

    .dropdown-content button {
        color: var(--text-primary);
        padding: 0.75rem 1rem;
        text-decoration: none;
        display: block;
        width: 100%;
        text-align: left;
        background: none;
        border: none;
        border-bottom: 1px solid var(--border-color);
        cursor: pointer;
        transition: background var(--transition-fast);
    }

    .dropdown-content button:last-child {
        border-bottom: none;
    }

    .dropdown-content button:hover {
        background-color: rgba(239, 68, 68, 0.1);
        color: var(--danger);
    }

    .loading-state, .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 4rem 0;
        color: var(--text-tertiary);
    }

    .spinner {
        width: 40px;
        height: 40px;
        border: 3px solid rgba(79, 70, 229, 0.3);
        border-radius: 50%;
        border-top-color: var(--primary);
        animation: spin 1s ease-in-out infinite;
        margin-bottom: 1rem;
    }

    .modal-backdrop {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        backdrop-filter: blur(4px);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 100;
    }

    .modal-content {
        width: 100%;
        max-width: 500px;
        margin: 1rem;
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }
</style>
