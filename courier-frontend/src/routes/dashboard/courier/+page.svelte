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
        // Physical info
        senderCity: string;
        senderAddress: string;
        weight: number;
        height: number;
        width: number;
        length: number;
        fragility: string;
        nextRegion: string;
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
            if (!auth.userId) return;
            const res = await fetch(`http://localhost:8080/api/parcels/courier/${auth.userId}`);
            if (res.ok) {
                const data = await res.json();
                assignedPackages = data.map((p: any) => ({
                    id: p.parcelId,
                    trackingNumber: p.trackingNumber,
                    city: p.city,
                    status: p.status,
                    address: p.address || "No address provided",
                    expectedDelivery: p.expectedDelivery,
                    deliveryMode: p.deliveryMode || "NORMAL",
                    senderCity: p.senderCity || "N/A",
                    senderAddress: p.senderAddress || "N/A",
                    weight: p.weight || 0,
                    height: p.height || 0,
                    width: p.width || 0,
                    length: p.length || 0,
                    fragility: p.fragility || "no",
                    nextRegion: p.nextRegion || "N/A"
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
                body: JSON.stringify({ status: "DELIVERED", comment: "Successful delivery", employeeId: auth.userId?.toString() })
            });
            if (res.ok) {
                assignedPackages = assignedPackages.map(p => p.id === pkg.id ? { ...p, status: 'DELIVERED' } : p);
            }
        } catch (e) {
            console.error("Failed to mark as delivered", e);
        }
    }

    async function handlePickup(pkg: Package) {
        try {
            const res = await fetch(`http://localhost:8080/api/parcels/${pkg.id}/status`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ status: "IN_TRANSIT", comment: "Package picked up by courier", employeeId: auth.userId?.toString() })
            });
            if (res.ok) {
                assignedPackages = assignedPackages.map(p => p.id === pkg.id ? { ...p, status: 'IN_TRANSIT' } : p);
            }
        } catch (e) {
            console.error("Failed to pick up package", e);
        }
    }

    async function handleAdvance(pkg: Package) {
        try {
            const res = await fetch(`http://localhost:8080/api/parcels/${pkg.id}/advance`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ employeeId: auth.userId?.toString() })
            });
            if (res.ok) {
                assignedPackages = assignedPackages.filter(p => p.id !== pkg.id);
            }
        } catch (e) {
            console.error("Failed to advance package", e);
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
                    body: JSON.stringify({ status: selectedStatus, comment: commentText, employeeId: auth.userId?.toString() })
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
            <p style="color: var(--text-secondary);">Welcome back, {auth.user}. You have {assignedPackages.filter(p => p.status === 'OUT_FOR_DELIVERY').length} packages to deliver today.</p>
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
                <div class="glass-panel package-card" class:completed={pkg.status === 'DELIVERED'} class:failed={['LOST', 'DAMAGED', 'FAILED', 'UNDELIVERED'].includes(pkg.status)} in:slide>
                    <div class="pkg-info">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
                            <div>
                                <span class="badge" class:badge-warning={pkg.status === 'OUT_FOR_DELIVERY'} class:badge-success={pkg.status === 'DELIVERED'} class:badge-primary={['PENDING_PICKUP', 'AT_HUB', 'IN_TRANSIT'].includes(pkg.status)} class:badge-danger={['LOST', 'DAMAGED', 'FAILED', 'UNDELIVERED'].includes(pkg.status)}>
                                    {pkg.status.replace(/_/g, ' ')}
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

                        <div class="route-container" class:pickup-mode={pkg.status === 'PENDING_PICKUP'}>
                            <div class="route-step from">
                                <div class="route-label">FROM</div>
                                <div class="route-content">
                                    {#if pkg.status === 'PENDING_PICKUP' || pkg.status === 'IN_TRANSIT'}
                                        <div class="route-main">{pkg.senderAddress}</div>
                                        <div class="route-sub">{pkg.senderCity} <span class="loc-tag">Sender</span></div>
                                    {:else}
                                        <div class="route-main">{pkg.nextRegion} Hub</div>
                                        <div class="route-sub">Current Location</div>
                                    {/if}
                                </div>
                            </div>

                            <div class="route-divider">
                                <div class="route-line"></div>
                                <div class="route-icon">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                                        <polyline points="9 18 15 12 9 6"></polyline>
                                    </svg>
                                </div>
                            </div>

                            <div class="route-step to">
                                <div class="route-label">TO</div>
                                <div class="route-content">
                                    {#if pkg.status === 'OUT_FOR_DELIVERY'}
                                        <div class="route-main">{pkg.address}</div>
                                        <div class="route-sub">{pkg.city} <span class="loc-tag">Recipient</span></div>
                                    {:else if pkg.status === 'PENDING_PICKUP'}
                                        <div class="route-main">{pkg.senderAddress}</div>
                                        <div class="route-sub">{pkg.senderCity} <span class="loc-tag">Pickup Point</span></div>
                                    {:else}
                                        <div class="route-main">{pkg.nextRegion} Hub</div>
                                        <div class="route-sub">Target Hub</div>
                                    {/if}
                                </div>
                            </div>
                        </div>

                        <div class="physical-info">
                            <div class="phys-item">
                                <span class="phys-label">Weight</span>
                                <span class="phys-value">{pkg.weight} kg</span>
                            </div>
                            <div class="phys-item">
                                <span class="phys-label">Size (L×W×H)</span>
                                <span class="phys-value">{pkg.length}×{pkg.width}×{pkg.height} cm</span>
                            </div>
                            <div class="phys-item">
                                <span class="phys-label">Fragile</span>
                                <span class="phys-value" style="color: {pkg.fragility === 'yes' ? 'var(--danger)' : 'inherit'}; font-weight: {pkg.fragility === 'yes' ? '700' : 'inherit'};">
                                    {pkg.fragility.toUpperCase()}
                                </span>
                            </div>
                        </div>
                    </div>

                    {#if ['OUT_FOR_DELIVERY', 'PENDING_PICKUP', 'IN_TRANSIT', 'AT_HUB'].includes(pkg.status)}
                        <div class="action-bar">
                            {#if pkg.status === 'PENDING_PICKUP'}
                                <button class="btn btn-primary" onclick={() => handlePickup(pkg)}>
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M21 8l-2-2H5L3 8v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8z"></path>
                                        <path d="M3 8h18"></path>
                                        <path d="M10 12h4"></path>
                                    </svg>
                                    Pick Up Package
                                </button>
                            {:else if pkg.status === 'IN_TRANSIT'}
                                <button class="btn btn-secondary" style="background-color: var(--primary); color: var(--bg-color);" onclick={() => handleAdvance(pkg)}>
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                                        <polyline points="9 22 9 12 15 12 15 22"></polyline>
                                    </svg>
                                    Arrived at Hub
                                </button>
                            {:else if pkg.status === 'OUT_FOR_DELIVERY'}
                                <button class="btn btn-success" onclick={() => handleSuccess(pkg)}>
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="20 6 9 17 4 12"></polyline>
                                    </svg>
                                    Delivered
                                </button>
                            {:else if pkg.status === 'AT_HUB'}
                                <button class="btn btn-primary" onclick={() => handlePickup(pkg)}>
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M21 8l-2-2H5L3 8v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8z"></path>
                                        <path d="M3 8h18"></path>
                                        <path d="M10 12h4"></path>
                                    </svg>
                                    Pick Up from Hub
                                </button>
                            {/if}
                            
                            <div class="dropdown">
                                <button class="btn btn-outline issue-btn">
                                    Report Issue
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="6 9 12 15 18 9"></polyline>
                                    </svg>
                                </button>
                                <div class="dropdown-content">
                                    <button onclick={() => openIssueModal(pkg, 'UNDELIVERED')}>Recipient absent</button>
                                    <button onclick={() => openIssueModal(pkg, 'DAMAGED')}>Package damaged</button>
                                    <button onclick={() => openIssueModal(pkg, 'LOST')}>Lost in transit</button>
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

    .route-container {
        display: flex;
        align-items: center;
        gap: 1rem;
        background: var(--bg-color);
        padding: 1.25rem;
        border-radius: var(--radius-md);
        border: 1px solid var(--border-color);
        margin-bottom: 1.5rem;
        position: relative;
    }

    .route-container.pickup-mode {
        border-color: var(--primary);
        background: rgba(79, 70, 229, 0.03);
    }

    .route-step {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .route-label {
        font-size: 0.65rem;
        font-weight: 800;
        letter-spacing: 0.05em;
        color: var(--text-tertiary);
    }

    .route-step.from .route-label {
        color: var(--primary);
    }

    .route-step.to .route-label {
        color: var(--secondary);
    }

    .route-main {
        font-weight: 700;
        font-size: 1rem;
        color: var(--text-primary);
        line-height: 1.2;
    }

    .route-sub {
        font-size: 0.8125rem;
        color: var(--text-secondary);
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .loc-tag {
        font-size: 0.7rem;
        padding: 0.1rem 0.4rem;
        background: rgba(0,0,0,0.05);
        border-radius: 4px;
        color: var(--text-tertiary);
        font-weight: 600;
    }

    .route-divider {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        position: relative;
        padding: 0 0.5rem;
    }

    .route-line {
        height: 40px;
        width: 1px;
        background: var(--border-color);
        display: none; /* Only for vertical layout if needed */
    }

    .route-icon {
        color: var(--border-color);
        background: var(--bg-color);
        z-index: 1;
    }

    .physical-info {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 1rem;
        padding-top: 1rem;
        border-top: 1px dashed var(--border-color);
    }

    .phys-item {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .phys-label {
        font-size: 0.75rem;
        color: var(--text-tertiary);
        text-transform: uppercase;
        letter-spacing: 0.025em;
    }

    .phys-value {
        font-size: 0.9375rem;
        font-weight: 600;
        color: var(--text-primary);
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
