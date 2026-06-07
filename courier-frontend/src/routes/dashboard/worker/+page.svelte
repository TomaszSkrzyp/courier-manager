<script lang="ts">
    import { auth } from '$lib/auth.svelte';
    import { onMount } from 'svelte';
    import { fade, slide } from 'svelte/transition';
    import { trimObject } from '$lib';

    type Status = string;

    interface Package {
        id: number;
        trackingNumber: string;
        city: string;
        status: Status;
        verified: boolean;
        date: string;
        deliveryMode: string;
        // Detailed info
        address: string;
        senderCity: string;
        weight: number;
        height: number;
        width: number;
        length: number;
        fragility: string;
        expectedDelivery: string;
        price: number;
        phoneNumber: string;
        comment: string;
        currentRegion: string;
        showDetails?: boolean;
    }

    interface Region {
        id: number;
        name: string;
    }

    let packages = $state<Package[]>([]);
    let statuses = $state<string[]>(["None", "Created", "Collected", "In Transit", "Out for Delivery", "Delivered", "Failed", "Lost", "Damaged"]);
    let regions = $state<Region[]>([]);
    
    let activeTab = $state("manage"); // 'manage' or 'reports'
    let isLoadingData = $state(false);
    let searchQuery = $state("");
    let filterStatus = $state("All");

    // Pagination state
    let currentPage = $state(0);
    let totalPages = $state(0);
    let totalElements = $state(0);

    let errorMessage = $state("");

    $effect(() => {
        activeTab;
        errorMessage = "";
    });

    async function fetchPackages(page = 0) {
        isLoadingData = true;
        errorMessage = "";
        try {
            const params = new URLSearchParams({
                page: page.toString(),
                size: "10",
                status: filterStatus
            });
            if (searchQuery.trim()) {
                params.append("search", searchQuery.trim());
            }

            const res = await fetch(`http://localhost:8080/api/parcels?${params.toString()}`);
            if (res.ok) {
                const data = await res.json();
                
                // Handle different response formats (Paginated vs Direct List)
                let content = [];
                if (data && data.content && Array.isArray(data.content)) {
                    content = data.content;
                    currentPage = data.number ?? 0;
                    totalPages = data.totalPages ?? 1;
                    totalElements = data.totalElements ?? content.length;
                } else if (Array.isArray(data)) {
                    content = data;
                    currentPage = 0;
                    totalPages = 1;
                    totalElements = data.length;
                } else {
                    throw new Error("Invalid data structure received");
                }

                packages = content.map((p: any) => ({
                    id: p.parcelId || Math.random(),
                    trackingNumber: p.trackingNumber || "1234567890",
                    city: p.city || "N/A",
                    status: p.status || "REGISTERED",
                    verified: p.verified || false,
                    date: p.date || new Date().toLocaleDateString(),
                    deliveryMode: p.deliveryMode || "NORMAL",
                    address: p.address || "N/A",
                    senderCity: p.senderCity || "N/A",
                    weight: Number(p.weight) || 0,
                    height: Number(p.height) || 0,
                    width: Number(p.width) || 0,
                    length: Number(p.length) || 0,
                    fragility: p.fragility || "no",
                    expectedDelivery: p.expectedDelivery || "N/A",
                    price: Number(p.price) || 0,
                    phoneNumber: p.phoneNumber || "N/A",
                    comment: p.comment || "",
                    currentRegion: p.currentRegion || "N/A",
                    showDetails: false
                }));
            } else {
                errorMessage = `Server error: ${res.status} ${res.statusText}`;
            }
        } catch (err) {
            console.error("Fetch error:", err);
            errorMessage = "Failed to load data. Please check server connection.";
        } finally {
            isLoadingData = false;
        }
    }

    // Debounce search and status change
    let searchTimeout: any;
    $effect(() => {
        const query = searchQuery;
        const status = filterStatus;
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            fetchPackages(0);
        }, 400);
    });

    onMount(async () => {
        fetchPackages(0);
        try {
            const statusRes = await fetch("http://localhost:8080/api/statuses");
            if (statusRes.ok) {
                const statusData = await statusRes.json();
                statuses = statusData.map((s: any) => s.name);
            }
        } catch (err) {
            console.error("Failed to fetch statuses", err);
        }

        try {
            const regionRes = await fetch("http://localhost:8080/api/regions");
            if (regionRes.ok) {
                regions = await regionRes.json();
            }
        } catch (err) {
            console.error("Failed to fetch regions", err);
        }
    });

    function handlePageChange(newPage: number) {
        if (newPage >= 0 && newPage < totalPages) {
            fetchPackages(newPage);
        }
    }

    function toggleDetails(pkg: Package) {
        pkg.showDetails = !pkg.showDetails;
    }

    async function toggleVerified(pkg: Package) {
        try {
            const res = await fetch(`http://localhost:8080/api/parcels/${pkg.id}/verify`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ verified: !pkg.verified })
            });
            if (res.ok) {
                pkg.verified = !pkg.verified;
            }
        } catch (e) {
            console.error("Failed to verify", e);
        }
    }

    async function changeStatus(pkg: Package, newStatus: Status) {
        try {
            const res = await fetch(`http://localhost:8080/api/parcels/${pkg.id}/status`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(trimObject({ status: newStatus, comment: "Status updated by worker", employeeId: auth.userId?.toString() }))
            });
            if (res.ok) {
                pkg.status = newStatus;
            }
        } catch (e) {
            console.error("Failed to change status", e);
        }
    }

    // Report generation
    let isGeneratingReport = $state(false);
    let reportReady = $state(false);
    let reportUrl = $state("");
    let reportFilename = $state("parcels_report.pdf");
    let startDate = $state(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]);
    let endDate = $state(new Date().toISOString().split('T')[0]);
    let today = new Date().toISOString().split('T')[0];
    let selectedRegionId = $state<number | null>(null);

    async function generateReport() {
        if (startDate > today || endDate > today) {
            errorMessage = "Cannot generate report for future dates.";
            return;
        }
        if (startDate > endDate) {
            errorMessage = "Start date cannot be later than end date.";
            return;
        }
        isGeneratingReport = true;
        reportReady = false;
        
        try {
            const start = `${startDate}T00:00:00`;
            const end = `${endDate}T23:59:59`;
            let url = `http://localhost:8080/api/reports/parcels?startDate=${start}&endDate=${end}`;
            if (selectedRegionId) {
                url += `&regionId=${selectedRegionId}`;
            }
            const res = await fetch(url);
            if (res.ok) {
                // Try to get filename from header
                const disposition = res.headers.get('Content-Disposition');
                if (disposition && disposition.includes('filename=')) {
                    const filenameMatch = disposition.match(/filename="?([^";]+)"?/);
                    if (filenameMatch && filenameMatch[1]) {
                        reportFilename = filenameMatch[1];
                    }
                }

                const blob = await res.blob();
                if (reportUrl) URL.revokeObjectURL(reportUrl);
                reportUrl = URL.createObjectURL(blob);
                reportReady = true;
            } else {
                console.error("Failed to generate report");
            }
        } catch (err) {
            console.error("Error generating report:", err);
        } finally {
            isGeneratingReport = false;
        }
    }
</script>

<div class="dashboard-panel animate-fade-in">
    <div class="panel-header">
        <div>
            <h2>Worker Dashboard</h2>
            <p style="color: var(--text-secondary);">Manage parcels, verify shipments, and generate reports.</p>
        </div>
        
        <div class="tabs">
            <button class="tab-btn" class:active={activeTab === 'manage'} onclick={() => activeTab = 'manage'}>
                Manage Parcels
            </button>
            <button class="tab-btn" class:active={activeTab === 'reports'} onclick={() => activeTab = 'reports'}>
                System Reports
            </button>
        </div>
    </div>

    <div class="tab-content-wrapper">
        {#if errorMessage}
            <div class="glass-panel error-message animate-slide-down" transition:slide>
                <div style="display: flex; align-items: center; gap: 0.75rem;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    <span style="color: var(--text-primary); font-weight: 500;">{errorMessage}</span>
                </div>
                <button class="btn-action" style="color: var(--text-tertiary); background: transparent; border: none; cursor: pointer;" onclick={() => errorMessage = ""}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>
        {/if}

        {#if activeTab === 'manage'}
            <div in:fade={{duration: 200}}>
                <div class="glass-panel filter-bar">
                    <div class="input-group" style="margin: 0; flex: 2;">
                        <input 
                            type="text" 
                            bind:value={searchQuery} 
                            placeholder="Search by Tracking Number or Region..." 
                            class="input-field search-input"
                        />
                    </div>
                    
                    <div class="input-group" style="margin: 0; flex: 1;">
                        <select bind:value={filterStatus} class="input-field">
                            <option value="All">All Statuses</option>
                            {#each statuses as s}
                                <option value={s}>{s}</option>
                            {/each}
                        </select>
                    </div>
                </div>

                <div class="glass-panel" style="padding: 0; overflow: hidden;">
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th style="width: 40px;"></th>
                                    <th>Tracking Number</th>
                                    <th>Destination</th>
                                    <th>Price</th>
                                    <th>Mode</th>
                                    <th>Status</th>
                                    <th>Verified</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {#if isLoadingData}
                                    <tr>
                                        <td colspan="8" style="text-align: center; padding: 4rem;">
                                            <div class="spinner"></div>
                                            <p style="color: var(--text-tertiary); margin-top: 1rem;">Loading parcels...</p>
                                        </td>
                                    </tr>
                                {:else if errorMessage}
                                    <tr>
                                        <td colspan="8" style="text-align: center; padding: 4rem; color: var(--danger);">
                                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-bottom: 1rem;">
                                                <circle cx="12" cy="12" r="10"></circle>
                                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                                            </svg>
                                            <p>{errorMessage}</p>
                                            <button class="btn btn-outline btn-sm" style="margin-top: 1rem;" onclick={() => fetchPackages(currentPage)}>Try Again</button>
                                        </td>
                                    </tr>
                                {:else}
                                    {#each packages as pkg}
                                        <tr class:row-active={pkg.showDetails}>
                                            <td>
                                                <button class="btn-icon" onclick={() => toggleDetails(pkg)} title="Toggle Details">
                                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="transform: rotate({pkg.showDetails ? '90deg' : '0deg'}); transition: transform 0.2s;">
                                                        <polyline points="9 18 15 12 9 6"></polyline>
                                                    </svg>
                                                </button>
                                            </td>
                                            <td style="font-family: monospace; font-weight: 600;">{pkg.trackingNumber}</td>
                                            <td>{pkg.city}</td>
                                            <td style="font-weight: 600; color: var(--secondary);">${pkg.price.toFixed(2)}</td>
                                            <td>
                                                <span class="badge" class:badge-primary={pkg.deliveryMode === 'EXPRESS'} class:badge-outline={pkg.deliveryMode === 'NORMAL'}>
                                                    {pkg.deliveryMode}
                                                </span>
                                            </td>
                                            <td>
                                                <span class="badge" class:badge-success={pkg.status === 'DELIVERED'} class:badge-warning={['IN_TRANSIT', 'OUT_FOR_DELIVERY'].includes(pkg.status)} class:badge-danger={['LOST', 'DAMAGED', 'FAILED'].includes(pkg.status)} class:badge-info={['REGISTERED', 'PENDING_PICKUP', 'AT_HUB'].includes(pkg.status)}>
                                                    {#if pkg.status === 'AT_HUB'}
                                                        HUB: {pkg.currentRegion}
                                                    {:else}
                                                        {pkg.status.replace(/_/g, ' ')}
                                                    {/if}
                                                </span>
                                            </td>
                                            <td>
                                                <button 
                                                    class="toggle-btn" 
                                                    class:active={pkg.verified}
                                                    disabled={pkg.status !== 'AT_HUB' && !pkg.verified}
                                                    style="opacity: {pkg.status !== 'AT_HUB' && !pkg.verified ? 0.4 : 1}; cursor: {pkg.status !== 'AT_HUB' && !pkg.verified ? 'not-allowed' : 'pointer'}"
                                                    onclick={() => toggleVerified(pkg)}
                                                    title={pkg.status === 'AT_HUB' ? 'Verify parcel arrival at hub' : 'Only parcels at hub can be verified'}
                                                >
                                                    <div class="toggle-knob"></div>
                                                </button>
                                            </td>
                                            <td>
                                                <div style="font-size: 0.8rem; color: var(--text-tertiary);">
                                                    {#if pkg.status === 'AT_HUB'}
                                                        Await Verification
                                                    {:else if pkg.status === 'REGISTERED'}
                                                        Await Pickup
                                                    {:else}
                                                        In Process
                                                    {/if}
                                                </div>
                                            </td>
                                        </tr>
                                        {#if pkg.showDetails}
                                            <tr transition:slide>
                                                <td colspan="8" style="padding: 0;">
                                                    <div class="details-panel animate-fade-in">
                                                        <div class="grid-responsive">
                                                            <div class="details-section">
                                                                <h5>Sender & Destination</h5>
                                                                <div class="detail-item">
                                                                    <span class="label">From:</span>
                                                                    <span class="value">{pkg.senderCity}</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">To:</span>
                                                                    <span class="value">{pkg.city}</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">Address:</span>
                                                                    <span class="value">{pkg.address}</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">Phone:</span>
                                                                    <span class="value">{pkg.phoneNumber}</span>
                                                                </div>
                                                            </div>
                                                            <div class="details-section">
                                                                <h5>Physical Attributes</h5>
                                                                <div class="detail-item">
                                                                    <span class="label">Weight:</span>
                                                                    <span class="value">{pkg.weight} kg</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">Dimensions:</span>
                                                                    <span class="value">{pkg.length}×{pkg.width}×{pkg.height} cm</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">Fragile:</span>
                                                                    <span class="value badge" class:badge-danger={pkg.fragility === 'yes'}>{pkg.fragility.toUpperCase()}</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">Total Price:</span>
                                                                    <span class="value" style="color: var(--secondary); font-weight: 700;">${pkg.price.toFixed(2)}</span>
                                                                </div>
                                                            </div>
                                                            <div class="details-section">
                                                                <h5>Timing & Notes</h5>
                                                                <div class="detail-item">
                                                                    <span class="label">Created:</span>
                                                                    <span class="value">{pkg.date}</span>
                                                                </div>
                                                                <div class="detail-item">
                                                                    <span class="label">Expected:</span>
                                                                    <span class="value">{pkg.expectedDelivery}</span>
                                                                </div>
                                                                {#if pkg.comment}
                                                                    <div class="detail-item" style="flex-direction: column; align-items: flex-start; gap: 0.25rem;">
                                                                        <span class="label">Customer Comment:</span>
                                                                        <span class="value" style="font-style: italic; font-weight: 400; line-height: 1.4; background: rgba(0,0,0,0.05); padding: 0.5rem; border-radius: 4px; width: 100%;">
                                                                            "{pkg.comment}"
                                                                        </span>
                                                                    </div>
                                                                {/if}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        {/if}
                                    {/each}
                                    {#if !isLoadingData && packages.length === 0}
                                        <tr>
                                            <td colspan="8" style="text-align: center; padding: 2rem; color: var(--text-tertiary);">
                                                No packages found.
                                            </td>
                                        </tr>
                                    {/if}
                                {/if}
                            </tbody>
                        </table>
                    </div>

                    <div class="pagination-bar">
                        <div class="pagination-info">
                            Showing <strong>{packages.length}</strong> of <strong>{totalElements}</strong> parcels
                        </div>
                        <div class="pagination-controls">
                            <button 
                                class="btn btn-outline btn-sm" 
                                disabled={currentPage === 0 || isLoadingData}
                                onclick={() => handlePageChange(currentPage - 1)}
                            >
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="15 18 9 12 15 6"></polyline>
                                </svg>
                                Previous
                            </button>
                            <span class="page-indicator">Page <strong>{currentPage + 1}</strong> of <strong>{totalPages || 1}</strong></span>
                            <button 
                                class="btn btn-outline btn-sm" 
                                disabled={currentPage >= totalPages - 1 || isLoadingData}
                                onclick={() => handlePageChange(currentPage + 1)}
                            >
                                Next
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="9 18 15 12 9 6"></polyline>
                                </svg>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        {:else if activeTab === 'reports'}
            <div class="basic-panel" in:fade={{duration: 200}}>
                <h3>Generate System Report</h3>
                <p>Select date range and optional region filter.</p>
                
                <div class="basic-form" style="max-width: none; display: flex; gap: 1rem; align-items: flex-end; flex-wrap: wrap;">
                    <div class="input-group" style="margin-bottom: 0; flex: 1; min-width: 150px;">
                        <label for="startDate">From:</label>
                        <input type="date" id="startDate" bind:value={startDate} max={today} class="input-field" />
                    </div>
                    <div class="input-group" style="margin-bottom: 0; flex: 1; min-width: 150px;">
                        <label for="endDate">To:</label>
                        <input type="date" id="endDate" bind:value={endDate} max={today} class="input-field" />
                    </div>
                    <div class="input-group" style="margin-bottom: 0; flex: 1; min-width: 150px;">
                        <label for="regionFilter">Region:</label>
                        <select id="regionFilter" bind:value={selectedRegionId} class="input-field">
                            <option value={null}>All Regions</option>
                            {#each regions as r}
                                <option value={r.id}>{r.name}</option>
                            {/each}
                        </select>
                    </div>

                    <button class="btn btn-primary" onclick={generateReport} disabled={isGeneratingReport} style="flex: 1; min-width: 150px; height: 42px;">
                        {isGeneratingReport ? 'Generating...' : 'Generate Report'}
                    </button>
                </div>

                {#if reportReady}
                    <div style="margin-top: 2rem; padding: 1.5rem; border: 1px solid var(--border-color); border-radius: 8px; text-align: center;">
                        <p style="margin-bottom: 1rem;">Report is ready: <strong>{reportFilename}</strong></p>
                        <a href={reportUrl} download={reportFilename} class="btn btn-outline">
                            Download PDF
                        </a>
                    </div>
                {/if}
            </div>
        {/if}
    </div>
</div>

<style>
    .tab-content-wrapper {
        min-height: 400px;
        position: relative;
    }

    .dashboard-panel {
        padding: 2rem;
    }

    .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        margin-bottom: 2rem;
    }

    .tabs {
        display: flex;
        background: var(--surface-color);
        padding: 0.25rem;
        border-radius: var(--radius-lg);
        border: 1px solid var(--border-color);
    }

    .tab-btn {
        padding: 0.5rem 1.5rem;
        background: transparent;
        border: none;
        border-radius: var(--radius-md);
        font-weight: 500;
        color: var(--text-secondary);
        cursor: pointer;
        transition: all var(--transition-fast);
    }

    .tab-btn.active {
        background: var(--primary);
        color: var(--bg-color);
        box-shadow: var(--shadow-sm);
    }

    .filter-bar {
        display: flex;
        gap: 1rem;
        padding: 1rem;
        margin-bottom: 1.5rem;
        border: 1px solid var(--border-color);
        border-radius: 8px;
    }

    .search-input {
        flex: 1;
    }

    .basic-panel {
        padding: 1.5rem;
        border: 1px solid var(--border-color);
        border-radius: 8px;
    }

    .basic-form {
        max-width: 400px;
        margin-top: 1.5rem;
    }

    .table-responsive {
        width: 100%;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
    }

    .data-table {
        width: 100%;
        border-collapse: collapse;
    }

    .data-table th, .data-table td {
        padding: 1rem 1.5rem;
        text-align: left;
        border-bottom: 1px solid var(--border-color);
    }

    .data-table th {
        font-weight: 600;
        color: var(--text-secondary);
        background-color: rgba(0,0,0,0.02);
        font-size: 0.875rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        white-space: nowrap;
    }

    .data-table tr:last-child td {
        border-bottom: none;
    }

    .data-table tbody tr:hover {
        background-color: rgba(14, 165, 233, 0.05);
    }

    .row-active {
        background-color: rgba(14, 165, 233, 0.08) !important;
    }

    .btn-icon {
        background: transparent;
        border: none;
        color: var(--text-tertiary);
        cursor: pointer;
        padding: 0.25rem;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        transition: all 0.2s;
    }

    .btn-icon:hover {
        background: rgba(0,0,0,0.05);
        color: var(--primary);
    }

    .details-panel {
        background: rgba(0,0,0,0.02);
        padding: 1.5rem 3rem;
        border-bottom: 1px solid var(--border-color);
    }

    .grid-responsive {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1.5rem;
    }

    .details-section h5 {
        margin: 0 0 1rem 0;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-tertiary);
    }

    .detail-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 0.5rem;
        font-size: 0.875rem;
    }

    .detail-item .label {
        color: var(--text-secondary);
    }

    .detail-item .value {
        font-weight: 500;
        color: var(--text-primary);
    }

    /* Toggle Switch */
    .toggle-btn {
        width: 44px;
        height: 24px;
        border-radius: 12px;
        background-color: var(--border-color);
        border: none;
        position: relative;
        cursor: pointer;
        transition: background-color var(--transition-fast);
        padding: 0;
    }

    .toggle-btn.active {
        background-color: var(--secondary);
    }

    .toggle-knob {
        width: 20px;
        height: 20px;
        background-color: white;
        border-radius: 50%;
        position: absolute;
        top: 2px;
        left: 2px;
        transition: transform var(--transition-fast);
        box-shadow: var(--shadow-sm);
    }

    .toggle-btn.active .toggle-knob {
        transform: translateX(20px);
    }

    .action-select {
        padding: 0.5rem;
        border-radius: var(--radius-md);
        border: 1px solid var(--border-color);
        background: var(--surface-color);
        color: var(--text-primary);
        font-family: var(--font-family);
        font-size: 0.875rem;
    }

    .pagination-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem;
        background: rgba(0,0,0,0.02);
        border-top: 1px solid var(--border-color);
    }

    .pagination-info {
        font-size: 0.875rem;
        color: var(--text-secondary);
    }

    .pagination-controls {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .page-indicator {
        font-size: 0.875rem;
        color: var(--text-primary);
    }

    .btn-sm {
        padding: 0.4rem 0.8rem;
        font-size: 0.8125rem;
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
    }

    .report-result {
        border: 2px dashed var(--secondary);
        border-radius: var(--radius-lg);
        padding: 3rem;
        text-align: center;
        background: rgba(16, 185, 129, 0.05);
    }

    .error-message {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 1.5rem;
        background: rgba(239, 68, 68, 0.05);
        border: 1px solid rgba(239, 68, 68, 0.2);
        border-radius: var(--radius-lg);
        margin-bottom: 2rem;
    }

    .spinner {
        width: 30px;
        height: 30px;
        border: 3px solid rgba(15, 23, 42, 0.1);
        border-radius: 50%;
        border-top-color: var(--primary);
        animation: spin 1s ease-in-out infinite;
        margin: 0 auto 1rem auto;
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }
</style>
