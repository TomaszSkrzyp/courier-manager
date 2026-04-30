<script lang="ts">
    import { auth } from '$lib/auth.svelte';
    import { onMount } from 'svelte';
    import { slide } from 'svelte/transition';

    type Status = string;

    interface Package {
        id: number;
        trackingNumber: string;
        city: string;
        status: Status;
        verified: boolean;
        date: string;
        deliveryMode: string;
    }

    let packages = $state<Package[]>([]);
    let statuses = $state<string[]>(["None", "Created", "Collected", "In Transit", "Out for Delivery", "Delivered", "Failed", "Lost", "Damaged"]);
    let filteredPackages = $derived(packages.filter(p => {
        if (filterStatus && filterStatus !== 'All' && p.status !== filterStatus) return false;
        if (searchQuery && !p.trackingNumber.includes(searchQuery) && !p.city.toLowerCase().includes(searchQuery.toLowerCase())) return false;
        return true;
    }));

    let filterStatus = $state("All");
    let searchQuery = $state("");
    let activeTab = $state("manage"); // 'manage' or 'reports'

    onMount(async () => {
        try {
            const res = await fetch("http://localhost:8080/api/parcels");
            if (res.ok) {
                const data = await res.json();
                packages = data.map((p: any) => ({
                    id: p.parcelId || Math.random(),
                    trackingNumber: p.trackingNumber || "1234567890",
                    city: p.city || "Warsaw",
                    status: p.status || "Created",
                    verified: p.verified || false,
                    date: p.date || new Date().toLocaleDateString(),
                    deliveryMode: p.deliveryMode || "NORMAL"
                }));
            }

            const statusRes = await fetch("http://localhost:8080/api/statuses");
            if (statusRes.ok) {
                const statusData = await statusRes.json();
                statuses = statusData.map((s: any) => s.name);
            }
        } catch (err) {
            console.error("Connection error.", err);
        }
    });

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
                body: JSON.stringify({ status: newStatus, comment: "Status updated by worker", employeeId: "1" })
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

    async function generateReport() {
        isGeneratingReport = true;
        reportReady = false;
        
        try {
            const res = await fetch("http://localhost:8080/api/reports/parcels");
            if (res.ok) {
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

    {#if activeTab === 'manage'}
        <div in:slide>
            <div class="glass-panel filter-bar">
                <div class="input-group" style="margin: 0; flex: 2;">
                    <input 
                        type="text" 
                        bind:value={searchQuery} 
                        placeholder="Search by Tracking Number or City..." 
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

            <div class="table-container glass-panel">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Tracking Number</th>
                            <th>City</th>
                            <th>Mode</th>
                            <th>Status</th>
                            <th>Verified</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each filteredPackages as pkg}
                            <tr>
                                <td style="font-family: monospace; font-weight: 600;">{pkg.trackingNumber}</td>
                                <td>{pkg.city}</td>
                                <td>
                                    <span class="badge" class:badge-primary={pkg.deliveryMode === 'EXPRESS'} class:badge-outline={pkg.deliveryMode === 'NORMAL'}>
                                        {pkg.deliveryMode}
                                    </span>
                                </td>
                                <td>
                                    <span class="badge" class:badge-success={pkg.status === 'Delivered'} class:badge-warning={pkg.status === 'In Transit' || pkg.status === 'Out for Delivery'} class:badge-danger={['Lost', 'Damaged', 'Failed'].includes(pkg.status)} class:badge-info={pkg.status === 'Created' || pkg.status === 'Collected'}>
                                        {pkg.status}
                                    </span>
                                </td>
                                <td>
                                    <button 
                                        class="toggle-btn" 
                                        class:active={pkg.verified}
                                        onclick={() => toggleVerified(pkg)}
                                    >
                                        <div class="toggle-knob"></div>
                                    </button>
                                </td>
                                <td>
                                    <select 
                                        class="action-select" 
                                        value={pkg.status}
                                        onchange={(e) => changeStatus(pkg, e.currentTarget.value as Status)}
                                    >
                                        {#each statuses as s}
                                            <option value={s}>{s}</option>
                                        {/each}
                                    </select>
                                </td>
                            </tr>
                        {:else}
                            <tr>
                                <td colspan="5" style="text-align: center; padding: 2rem; color: var(--text-tertiary);">
                                    No packages found matching your criteria.
                                </td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        </div>
    {:else if activeTab === 'reports'}
        <div class="glass-panel" in:slide>
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem;">
                <div>
                    <h3>Generate System Report</h3>
                    <p style="color: var(--text-secondary);">Compile a comprehensive PDF report of all parcel statuses and worker performance for the selected region.</p>
                </div>
                <button class="btn btn-primary" onclick={generateReport} disabled={isGeneratingReport}>
                    {#if isGeneratingReport}
                        Generating...
                    {:else}
                        Generate Report
                    {/if}
                </button>
            </div>

            {#if isGeneratingReport}
                <div class="loading-state" style="padding: 2rem 0;">
                    <div class="spinner"></div>
                    <p>Compiling data...</p>
                </div>
            {/if}

            {#if reportReady}
                <div class="report-result animate-fade-in">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--secondary)" stroke-width="2" style="margin-bottom: 1rem;">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                        <polyline points="10 9 9 9 8 9"></polyline>
                    </svg>
                    <h4>Report Generated Successfully</h4>
                    <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">The Daily Operations Report (Warsaw Region) is ready for download.</p>
                    <a href={reportUrl} download="parcels_report.pdf" class="btn btn-outline" style="text-decoration: none; display: inline-flex; align-items: center; justify-content: center;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 0.5rem;">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                            <polyline points="7 10 12 15 17 10"></polyline>
                            <line x1="12" y1="15" x2="12" y2="3"></line>
                        </svg>
                        Download PDF
                    </a>
                </div>
            {/if}
        </div>
    {/if}
</div>

<style>
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
        color: white;
        box-shadow: var(--shadow-sm);
    }

    .filter-bar {
        display: flex;
        gap: 1rem;
        padding: 1rem;
        margin-bottom: 1.5rem;
    }

    .search-input {
        background-image: url("data:image/svg+xml,%3Csvg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='%239CA3AF' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='11' cy='11' r='8'%3E%3C/circle%3E%3Cline x1='21' y1='21' x2='16.65' y2='16.65'%3E%3C/line%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-position: 1rem center;
        padding-left: 3rem;
    }

    .table-container {
        overflow-x: auto;
        padding: 0;
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
    }

    .data-table tr:last-child td {
        border-bottom: none;
    }

    .data-table tbody tr:hover {
        background-color: rgba(79, 70, 229, 0.02);
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

    .report-result {
        border: 2px dashed var(--secondary);
        border-radius: var(--radius-lg);
        padding: 3rem;
        text-align: center;
        background: rgba(16, 185, 129, 0.05);
    }

    .spinner {
        width: 30px;
        height: 30px;
        border: 3px solid rgba(79, 70, 229, 0.3);
        border-radius: 50%;
        border-top-color: var(--primary);
        animation: spin 1s ease-in-out infinite;
        margin: 0 auto 1rem auto;
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }
</style>
