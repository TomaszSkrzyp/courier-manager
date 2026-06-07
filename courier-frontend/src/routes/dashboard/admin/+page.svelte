<script lang="ts">
    import { slide, fade } from 'svelte/transition';
    import { onMount } from 'svelte';
    import { trimObject } from '$lib';

    interface Employee {
        id: number;
        firstName: string;
        lastName: string;
        role: string;
        login: string;
        regions: string[];
        dateAdded: string;
    }

    let employees = $state<Employee[]>([]);
    let regions = $state<{id: number, name: string}[]>([]);
    let activeTab = $state("staff"); // 'staff', 'pricing', 'regions'
    
    // Pagination state for employees
    let currentPage = $state(0);
    let totalPages = $state(0);
    let totalElements = $state(0);
    let isLoadingData = $state(false);

    let newRegionName = $state("");
    let showAddRegionModal = $state(false);
    let showAddPriceModal = $state(false);
    let showEditRegionModal = $state(false);
    let editingRegionId = $state<number | null>(null);
    let editRegionName = $state("");

    let errorMessage = $state("");
    let modalErrorMessage = $state("");

    $effect(() => {
        activeTab;
        errorMessage = "";
        modalErrorMessage = "";
    });

    async function fetchEmployees(page = 0) {
        isLoadingData = true;
        errorMessage = "";
        try {
            const res = await fetch(`http://localhost:8080/api/employees?page=${page}&size=10`);
            if (res.ok) {
                const data = await res.json();
                if (data && data.content) {
                    employees = data.content;
                    currentPage = data.number;
                    totalPages = data.totalPages;
                    totalElements = data.totalElements;
                } else {
                    employees = [];
                    errorMessage = "Invalid data format from server.";
                }
            } else {
                errorMessage = `Server error: ${res.status}`;
            }
        } catch (e) {
            console.error("Failed to fetch employees", e);
            errorMessage = "Connection failed.";
        } finally {
            isLoadingData = false;
        }
    }

    async function handleAddRegion(e: SubmitEvent) {
        e.preventDefault();
        if (!newRegionName.trim()) return;
        isSubmitting = true;
        try {
            const res = await fetch("http://localhost:8080/api/regions", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(trimObject({ name: newRegionName }))
            });
            if (res.ok) {
                const added = await res.json();
                regions = [...regions, added];
                newRegionName = "";
                showAddRegionModal = false;
            }
        } catch (e) {
            console.error("Failed to add region", e);
        } finally {
            isSubmitting = false;
        }
    }

    async function handleDeleteRegion(id: number) {
        if(confirm("Are you sure you want to remove this region? This may affect assigned couriers.")) {
            errorMessage = "";
            try {
                const res = await fetch(`http://localhost:8080/api/regions/${id}`, {
                    method: "DELETE"
                });
                if (res.ok) {
                    regions = regions.filter(r => r.id !== id);
                    fetchEmployees(currentPage);
                } else {
                    try {
                        const errorData = await res.json();
                        console.log("Full error data:", errorData);
                        // Exhaustive check for any message field
                        errorMessage = errorData.message || errorData.error || errorData.detail || errorData.title || JSON.stringify(errorData);
                        
                        // If we still have just "Bad Request" or similar generic text, and it's 400
                        if (errorMessage === "Bad Request" && res.status === 400) {
                            errorMessage = "Region is in use (cannot delete due to active parcels or couriers).";
                        }
                    } catch (e) {
                        console.error("Error parsing response", e);
                        errorMessage = `Error ${res.status}: ${res.statusText}`;
                    }
                }
            } catch (e) {
                console.error("Failed to delete region", e);
                errorMessage = "Connection error.";
            }
        }
    }

    function openEditRegionModal(region: {id: number, name: string}) {
        editingRegionId = region.id;
        editRegionName = region.name;
        showEditRegionModal = true;
    }

    async function handleUpdateRegion(e: SubmitEvent) {
        e.preventDefault();
        if (!editRegionName.trim()) return;
        isSubmitting = true;
        try {
            const res = await fetch(`http://localhost:8080/api/regions/${editingRegionId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(trimObject({ name: editRegionName }))
            });
            if (res.ok) {
                const updated = await res.json();
                regions = regions.map(r => r.id === editingRegionId ? updated : r);
                showEditRegionModal = false;
                editingRegionId = null;
                editRegionName = "";

                fetchEmployees(currentPage);
            }
        } catch (e) {
            console.error("Failed to update region", e);
        } finally {
            isSubmitting = false;
        }
    }

    let showAddModal = $state(false);
    let newEmp = $state({
        firstName: "",
        lastName: "",
        pesel: "",
        login: "",
        password: "",
        role: "COURIER",
        regions: ["", ""]
    });

    let showEditModal = $state(false);
    let editingId = $state<number | null>(null);
    let editEmp = $state({
        firstName: "",
        lastName: "",
        pesel: "",
        login: "",
        password: "",
        role: "COURIER",
        regions: ["", ""]
    });

    interface PriceDelta {
        deltaId: number;
        weightDelta: number;
        lengthDelta: number;
        widthDelta: number;
        heightDelta: number;
        normalModeDelta: number;
        expressModeDelta: number;
        createdAt: string;
    }
    let priceDeltas = $state<PriceDelta[]>([]);
    let newPriceDelta = $state({
        weightDelta: 0,
        lengthDelta: 0,
        widthDelta: 0,
        heightDelta: 0,
        normalModeDelta: 0,
        expressModeDelta: 0
    });

    let isSubmitting = $state(false);

    onMount(async () => {
        try {
            fetchEmployees(0);
            
            const regRes = await fetch("http://localhost:8080/api/regions");
            if (regRes.ok) regions = await regRes.json();

            const pdRes = await fetch("http://localhost:8080/api/price-deltas");
            if (pdRes.ok) {
                priceDeltas = await pdRes.json();
                if (priceDeltas.length > 0) {
                    const latest = priceDeltas[0];
                    newPriceDelta = { ...latest };
                }
            }
        } catch (e) {
            console.error("Failed to fetch initial data", e);
        }
    });

    function handlePageChange(newPage: number) {
        if (newPage >= 0 && newPage < totalPages) {
            fetchEmployees(newPage);
        }
    }

    async function handleAddPriceDelta(e: SubmitEvent) {
        e.preventDefault();
        isSubmitting = true;
        try {
            const res = await fetch("http://localhost:8080/api/price-deltas", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(trimObject(newPriceDelta))
            });
            if (res.ok) {
                const added = await res.json();
                priceDeltas = [added, ...priceDeltas];
                showAddPriceModal = false;
            }
        } catch (e) {
            console.error("Failed to add price delta", e);
        } finally {
            isSubmitting = false;
        }
    }

    async function handleAddEmployee(e: SubmitEvent) {
        e.preventDefault();
        isSubmitting = true;

        try {
            const res = await fetch("http://localhost:8080/api/employees", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(trimObject({
                    ...newEmp,
                    regions: newEmp.role === 'COURIER' ? newEmp.regions.filter(r => r.trim() !== '') : []
                }))
            });

            if (res.ok) {
                fetchEmployees(currentPage);
                
                showAddModal = false;
                newEmp = {
                    firstName: "",
                    lastName: "",
                    pesel: "",
                    login: "",
                    password: "",
                    role: "COURIER",
                    regions: ["", ""]
                };
                
                // Refresh regions list
                const regRes = await fetch("http://localhost:8080/api/regions");
                if (regRes.ok) regions = await regRes.json();
            }
        } catch (e) {
            console.error("Failed to add employee", e);
        } finally {
            isSubmitting = false;
        }
    }

    async function deleteEmployee(id: number) {
        if(confirm("Are you sure you want to remove this employee?")) {
            errorMessage = "";
            try {
                const res = await fetch(`http://localhost:8080/api/employees/${id}`, {
                    method: "DELETE"
                });
                if (res.ok) {
                    fetchEmployees(currentPage);
                } else {
                    try {
                        const errorData = await res.json();
                        errorMessage = errorData.message || `Error ${res.status}: ${res.statusText}`;
                    } catch {
                        errorMessage = `Failed to delete employee. Error ${res.status}.`;
                    }
                }
            } catch (e) {
                console.error("Failed to delete employee", e);
                errorMessage = "Connection error.";
            }
        }
    }

    function openEditModal(emp: Employee) {
        editingId = emp.id;
        
        let regs = ["", ""];
        if (emp.regions && emp.regions.length > 0) regs[0] = emp.regions[0];
        if (emp.regions && emp.regions.length > 1) regs[1] = emp.regions[1];
        
        editEmp = {
            firstName: emp.firstName,
            lastName: emp.lastName,
            pesel: (emp as any).pesel || "", // Ensure pesel is included
            login: emp.login,
            password: "",
            role: emp.role,
            regions: regs
        };
        showEditModal = true;
    }

    async function handleEditEmployee(e: SubmitEvent) {
        e.preventDefault();
        isSubmitting = true;
        modalErrorMessage = "";

        try {
            const res = await fetch(`http://localhost:8080/api/employees/${editingId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(trimObject({
                    ...editEmp,
                    regions: editEmp.role === 'COURIER' ? editEmp.regions.filter(r => r.trim() !== '') : []
                }))
            });

            if (res.ok) {
                fetchEmployees(currentPage);
                showEditModal = false;
                editingId = null;
                
                const regRes = await fetch("http://localhost:8080/api/regions");
                if (regRes.ok) regions = await regRes.json();
            } else {
                try {
                    const errorData = await res.json();
                    let rawMessage = errorData.message || "";
                    
                    // User friendly extraction of validation errors
                    if (rawMessage.includes("default message [")) {
                        const matches = rawMessage.match(/default message \[(.*?)\]/g);
                        if (matches && matches.length > 0) {
                            // Take the last match which is usually the specific constraint message
                            modalErrorMessage = matches[matches.length - 1].replace("default message [", "").replace("]", "");
                        } else {
                            modalErrorMessage = "Invalid input data. Please check all fields.";
                        }
                    } else {
                        modalErrorMessage = rawMessage || `Server error ${res.status}`;
                    }
                } catch {
                    modalErrorMessage = "Failed to update employee. Please try again.";
                }
            }
        } catch (e) {
            console.error("Failed to edit employee", e);
            modalErrorMessage = "Connection error. Check your internet.";
        } finally {
            isSubmitting = false;
        }
    }
</script>

<div class="dashboard-panel animate-fade-in">
    <div class="panel-header">
        <div class="header-main">
            <h2>Admin Dashboard</h2>
            <div class="tabs-row">
                <button class="tab-btn" class:active={activeTab === 'staff'} onclick={() => activeTab = 'staff'}>Staff Management</button>
                <button class="tab-btn" class:active={activeTab === 'pricing'} onclick={() => activeTab = 'pricing'}>Pricing Rules</button>
                <button class="tab-btn" class:active={activeTab === 'regions'} onclick={() => activeTab = 'regions'}>Region Management</button>
            </div>
        </div>
        <div class="header-actions">
            {#if activeTab === 'staff'}
                <button class="btn btn-primary action-btn" onclick={() => showAddModal = true}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Add Employee
                </button>
            {:else if activeTab === 'regions'}
                <button class="btn btn-primary action-btn" onclick={() => showAddRegionModal = true}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Add Region
                </button>
            {:else if activeTab === 'pricing'}
                <button class="btn btn-primary action-btn" onclick={() => showAddPriceModal = true}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    New Pricing Rule
                </button>
            {/if}
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
                <button class="btn-action" style="color: var(--text-tertiary);" onclick={() => errorMessage = ""}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>
        {/if}

        {#if activeTab === 'staff'}
            <div in:fade={{duration: 200}}>
                <div class="stats-grid">
                    <div class="glass-panel stat-card">
                        <div class="stat-icon" style="background: rgba(15, 23, 42, 0.1); color: var(--primary);">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                            </svg>
                        </div>
                        <div class="stat-info">
                            <h3>Total Personnel</h3>
                            <div class="stat-value">{totalElements}</div>
                        </div>
                    </div>

                    <div class="glass-panel stat-card">
                        <div class="stat-icon" style="background: rgba(16, 185, 129, 0.1); color: var(--secondary);">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="1" y="3" width="15" height="13"></rect>
                                <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
                                <circle cx="5.5" cy="18.5" r="2.5"></circle>
                                <circle cx="18.5" cy="18.5" r="2.5"></circle>
                            </svg>
                        </div>
                        <div class="stat-info">
                            <h3>Active Staff</h3>
                            <div class="stat-value">{totalElements}</div>
                        </div>
                    </div>

                    <div class="glass-panel stat-card">
                        <div class="stat-icon" style="background: rgba(245, 158, 11, 0.1); color: var(--warning);">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                                <line x1="8" y1="21" x2="16" y2="21"></line>
                                <line x1="12" y1="17" x2="12" y2="21"></line>
                            </svg>
                        </div>
                        <div class="stat-info">
                            <h3>Regions</h3>
                            <div class="stat-value">{regions.length}</div>
                        </div>
                    </div>
                </div>

                <div class="glass-panel" style="padding: 0; overflow: hidden;">
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Role</th>
                                    <th>Login</th>
                                    <th>Region</th>
                                    <th>Date Added</th>
                                    <th style="text-align: right;">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {#if isLoadingData}
                                    <tr>
                                        <td colspan="6" style="text-align: center; padding: 4rem;">
                                            <div class="spinner"></div>
                                            <p style="color: var(--text-tertiary); margin-top: 1rem;">Loading personnel...</p>
                                        </td>
                                    </tr>
                                {:else}
                                    {#each employees as emp}
                                        <tr in:slide>
                                            <td style="font-weight: 500;">{emp.firstName} {emp.lastName}</td>
                                            <td>
                                                {#if emp.role === 'ADMIN'}
                                                    <span class="badge badge-admin">SYSTEM ADMIN</span>
                                                {:else if emp.role === 'COURIER'}
                                                    <span class="badge badge-success">COURIER</span>
                                                {:else}
                                                    <span class="badge badge-warning">OFFICE WORKER</span>
                                                {/if}
                                            </td>
                                            <td style="font-family: monospace; color: var(--text-secondary);">{emp.login}</td>
                                            <td>{emp.regions.length > 0 ? emp.regions.join(" ↔ ") : "N/A"}</td>
                                            <td style="color: var(--text-tertiary);">{emp.dateAdded}</td>
                                            <td style="text-align: right;">
                                                <div class="actions-cell">
                                                    <button class="btn-action btn-edit" title="Edit" onclick={() => openEditModal(emp)}>
                                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                        </svg>
                                                    </button>
                                                    {#if emp.role !== 'ADMIN'}
                                                        <button class="btn-action btn-delete" title="Remove" onclick={() => deleteEmployee(emp.id)}>
                                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                                <polyline points="3 6 5 6 21 6"></polyline>
                                                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                                            </svg>
                                                        </button>
                                                    {:else}
                                                        <div style="width: 34px;"></div> <!-- Placeholder to keep alignment -->
                                                    {/if}
                                                </div>
                                            </td>
                                        </tr>
                                    {/each}
                                {/if}
                            </tbody>
                        </table>
                    </div>

                    <div class="pagination-bar">
                        <div class="pagination-info">
                            Showing <strong>{employees.length}</strong> of <strong>{totalElements}</strong> personnel
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
        {:else if activeTab === 'regions'}
            <div in:fade={{duration: 200}}>
                <div class="glass-panel" style="padding: 0; overflow: hidden;">
                    <div style="padding: 1.5rem; border-bottom: 1px solid var(--border-color);">
                        <h3>Current Regions</h3>
                        <p style="color: var(--text-tertiary); font-size: 0.9rem; margin-top: 0.5rem;">List of all service areas.</p>
                    </div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th style="text-align: right;">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {#each regions as region}
                                <tr>
                                    <td style="font-family: monospace; color: var(--text-tertiary);">#{region.id}</td>
                                    <td style="font-weight: 500;">{region.name}</td>
                                    <td style="text-align: right;">
                                        <div class="actions-cell">
                                            <button class="btn-action btn-edit" title="Edit" onclick={() => openEditRegionModal(region)}>
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                </svg>
                                            </button>
                                            <button class="btn-action btn-delete" title="Remove" onclick={() => handleDeleteRegion(region.id)}>
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <polyline points="3 6 5 6 21 6"></polyline>
                                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                                </svg>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            {:else}
                                <tr>
                                    <td colspan="3" style="text-align: center; padding: 2rem; color: var(--text-tertiary);">No regions defined.</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>
            </div>
        {:else}
            <div in:fade={{duration: 200}}>
                <div class="glass-panel" style="padding: 0; overflow: hidden;">
                    <div style="padding: 1.5rem; border-bottom: 1px solid var(--border-color);">
                        <h3>Pricing History</h3>
                        <p style="color: var(--text-tertiary); font-size: 0.9rem; margin-top: 0.5rem;">Latest active rule is shown at the top.</p>
                    </div>
                    <div style="overflow-x: auto;">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Date Set</th>
                                    <th>Base (N/E)</th>
                                    <th>Weight</th>
                                    <th>L / W / H</th>
                                </tr>
                            </thead>
                            <tbody>
                                {#each priceDeltas as pd, idx}
                                    <tr>
                                        <td style="white-space: nowrap;">
                                            {new Date(pd.createdAt).toLocaleDateString()}
                                            {#if idx === 0}
                                                <span class="badge badge-success" style="margin-left: 0.5rem;">ACTIVE</span>
                                            {/if}
                                        </td>
                                        <td style="font-weight: 600;">${pd.normalModeDelta} / ${pd.expressModeDelta}</td>
                                        <td>${pd.weightDelta}</td>
                                        <td style="color: var(--text-secondary); font-size: 0.85rem;">
                                            {pd.lengthDelta} / {pd.widthDelta} / {pd.heightDelta}
                                        </td>
                                    </tr>
                                {:else}
                                    <tr>
                                        <td colspan="5" style="text-align: center; padding: 2rem; color: var(--text-tertiary);">No pricing rules found.</td>
                                    </tr>
                                {/each}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        {/if}
    </div>
</div>

{#if showAddModal}
    <div class="modal-backdrop" transition:fade>
        <div class="glass-panel modal-content" in:slide>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h3>Register New Personnel</h3>
                <button class="btn" style="padding: 0.5rem; background: transparent;" onclick={() => showAddModal = false}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>

            <form onsubmit={handleAddEmployee}>
                <div class="grid-2">
                    <div class="input-group">
                        <label>First Name</label>
                        <input bind:value={newEmp.firstName} type="text" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Last Name</label>
                        <input bind:value={newEmp.lastName} type="text" class="input-field" required />
                    </div>
                </div>

                <div class="input-group">
                    <label for="pesel-input">PESEL</label>
                    <input 
                        id="pesel-input"
                        bind:value={newEmp.pesel} 
                        oninput={(e) => {
                            newEmp.pesel = e.currentTarget.value.replace(/\D/g, '').substring(0, 11);
                        }}
                        type="text" 
                        inputmode="numeric"
                        class="input-field" 
                        placeholder="Enter 11-digit PESEL"
                        required 
                        minlength="11"
                        maxlength="11"
                        title="PESEL must be exactly 11 digits"
                    />
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>System Login</label>
                        <input bind:value={newEmp.login} type="text" class="input-field" required minlength="3" />
                    </div>
                    <div class="input-group">
                        <label>Password</label>
                        <input bind:value={newEmp.password} type="password" class="input-field" required minlength="6" />
                    </div>
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>Role</label>
                        <select bind:value={newEmp.role} class="input-field">
                            <option value="COURIER">Courier</option>
                            <option value="WORKER">Office Worker</option>
                        </select>
                    </div>
                    
                    {#if newEmp.role === 'COURIER'}
                        <div class="input-group" style="display: flex; gap: 0.5rem; flex-direction: column;">
                            <label>Assigned Cities</label>
                            <div style="display: flex; gap: 0.5rem;">
                                <select bind:value={newEmp.regions[0]} class="input-field" required>
                                    <option value="" disabled>Select City 1</option>
                                    {#each regions as r}
                                        <option value={r.name}>{r.name}</option>
                                    {/each}
                                </select>
                                <span style="align-self: center;">↔</span>
                                <select bind:value={newEmp.regions[1]} class="input-field">
                                    <option value="">None (City 2)</option>
                                    {#each regions as r}
                                        <option value={r.name}>{r.name}</option>
                                    {/each}
                                </select>
                            </div>
                        </div>
                    {:else}
                        <div class="input-group">
                            <label>Assigned City</label>
                            <input type="text" class="input-field" value="Not Applicable" disabled />
                        </div>
                    {/if}
                </div>

                <div style="margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;">
                    <button type="button" class="btn btn-outline" onclick={() => showAddModal = false}>Cancel</button>
                    <button type="submit" class="btn btn-primary" disabled={isSubmitting}>
                        {isSubmitting ? 'Registering...' : 'Register Employee'}
                    </button>
                </div>
            </form>
        </div>
    </div>
{/if}

{#if showAddRegionModal}
    <div class="modal-backdrop" transition:fade>
        <div class="glass-panel modal-content" in:slide>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h3>Add New Region</h3>
                <button class="btn" style="padding: 0.5rem; background: transparent;" onclick={() => showAddRegionModal = false}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>

            <form onsubmit={handleAddRegion}>
                <div class="input-group">
                    <label>Region/City Name</label>
                    <input bind:value={newRegionName} type="text" class="input-field" placeholder="e.g. Radom" required />
                </div>

                <div style="margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;">
                    <button type="button" class="btn btn-outline" onclick={() => showAddRegionModal = false}>Cancel</button>
                    <button type="submit" class="btn btn-primary" disabled={isSubmitting}>
                        {isSubmitting ? 'Adding...' : 'Add Region'}
                    </button>
                </div>
            </form>
        </div>
    </div>
{/if}

{#if showAddPriceModal}
    <div class="modal-backdrop" transition:fade>
        <div class="glass-panel modal-content" in:slide>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h3>Set New Pricing Rule</h3>
                <button class="btn" style="padding: 0.5rem; background: transparent;" onclick={() => showAddPriceModal = false}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>

            <form onsubmit={handleAddPriceDelta}>
                <div class="grid-2">
                    <div class="input-group">
                        <label>Normal Mode ($)</label>
                        <input bind:value={newPriceDelta.normalModeDelta} type="number" step="0.1" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Express Mode ($)</label>
                        <input bind:value={newPriceDelta.expressModeDelta} type="number" step="0.1" class="input-field" required />
                    </div>
                </div>
                <div class="grid-2" style="margin-top: 1rem;">
                    <div class="input-group">
                        <label>Weight ($/kg)</label>
                        <input bind:value={newPriceDelta.weightDelta} type="number" step="0.01" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Length ($/cm)</label>
                        <input bind:value={newPriceDelta.lengthDelta} type="number" step="0.01" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Width ($/cm)</label>
                        <input bind:value={newPriceDelta.widthDelta} type="number" step="0.01" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Height ($/cm)</label>
                        <input bind:value={newPriceDelta.heightDelta} type="number" step="0.01" class="input-field" required />
                    </div>
                </div>

                <div style="margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;">
                    <button type="button" class="btn btn-outline" onclick={() => showAddPriceModal = false}>Cancel</button>
                    <button type="submit" class="btn btn-primary" disabled={isSubmitting}>
                        {isSubmitting ? 'Saving...' : 'Set as Active Rule'}
                    </button>
                </div>
            </form>
        </div>
    </div>
{/if}

{#if showEditModal}
    <div class="modal-backdrop" transition:fade>
        <div class="glass-panel modal-content" in:slide>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h3>Edit Personnel</h3>
                <button class="btn" style="padding: 0.5rem; background: transparent;" onclick={() => showEditModal = false}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>

            <form onsubmit={handleEditEmployee}>
                {#if modalErrorMessage}
                    <div class="glass-panel error-message" style="margin-bottom: 1.5rem; padding: 0.75rem 1rem;" transition:slide>
                        <span style="font-size: 0.9rem;">{modalErrorMessage}</span>
                    </div>
                {/if}
                <div class="grid-2">
                    <div class="input-group">
                        <label>First Name</label>
                        <input bind:value={editEmp.firstName} type="text" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Last Name</label>
                        <input bind:value={editEmp.lastName} type="text" class="input-field" required />
                    </div>
                </div>

                <div class="input-group">
                    <label for="edit-pesel-input">PESEL</label>
                    <input 
                        id="edit-pesel-input"
                        bind:value={editEmp.pesel} 
                        oninput={(e) => {
                            editEmp.pesel = e.currentTarget.value.replace(/\D/g, '').substring(0, 11);
                        }}
                        type="text" 
                        inputmode="numeric"
                        class="input-field" 
                        placeholder="11-digit PESEL"
                        required 
                        minlength="11"
                        maxlength="11"
                        title="PESEL must be exactly 11 digits"
                    />
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>System Login</label>
                        <input bind:value={editEmp.login} type="text" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>New Password (Optional)</label>
                        <input bind:value={editEmp.password} type="password" class="input-field" placeholder="Leave blank to keep current" />
                    </div>
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>Role</label>
                        <select bind:value={editEmp.role} class="input-field">
                            <option value="COURIER">Courier</option>
                            <option value="WORKER">Office Worker</option>
                        </select>
                    </div>
                    
                    {#if editEmp.role === 'COURIER'}
                        <div class="input-group" style="display: flex; gap: 0.5rem; flex-direction: column;">
                            <label>Assigned Cities</label>
                            <div style="display: flex; gap: 0.5rem;">
                                <select bind:value={editEmp.regions[0]} class="input-field" required>
                                    <option value="" disabled>Select City 1</option>
                                    {#each regions as r}
                                        <option value={r.name}>{r.name}</option>
                                    {/each}
                                </select>
                                <span style="align-self: center;">↔</span>
                                <select bind:value={editEmp.regions[1]} class="input-field">
                                    <option value="">None (City 2)</option>
                                    {#each regions as r}
                                        <option value={r.name}>{r.name}</option>
                                    {/each}
                                </select>
                            </div>
                        </div>
                    {:else}
                        <div class="input-group">
                            <label>Assigned City</label>
                            <input type="text" class="input-field" value="Not Applicable" disabled />
                        </div>
                    {/if}
                </div>

                <div style="margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;">
                    <button type="button" class="btn btn-outline" onclick={() => showEditModal = false}>Cancel</button>
                    <button type="submit" class="btn btn-primary" disabled={isSubmitting}>
                        {isSubmitting ? 'Saving...' : 'Save Changes'}
                    </button>
                </div>
            </form>
        </div>
    </div>
{/if}

{#if showEditRegionModal}
    <div class="modal-backdrop" transition:fade>
        <div class="glass-panel modal-content" in:slide>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h3>Edit Region</h3>
                <button class="btn" style="padding: 0.5rem; background: transparent;" onclick={() => showEditRegionModal = false}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>

            <form onsubmit={handleUpdateRegion}>
                <div class="input-group">
                    <label>Region/City Name</label>
                    <input bind:value={editRegionName} type="text" class="input-field" required />
                </div>

                <div style="margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;">
                    <button type="button" class="btn btn-outline" onclick={() => showEditRegionModal = false}>Cancel</button>
                    <button type="submit" class="btn btn-primary" disabled={isSubmitting}>
                        {isSubmitting ? 'Saving...' : 'Save Changes'}
                    </button>
                </div>
            </form>
        </div>
    </div>
{/if}

<style>
    .tab-content-wrapper {
        min-height: 500px;
        position: relative;
    }

    .dashboard-panel {
        padding: 2rem;
    }

    .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
        min-height: 80px; /* Fixed height to prevent layout jumping */
    }

    .header-main {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .tabs-row {
        display: flex;
        gap: 1rem;
    }

    .header-actions {
        display: flex;
        align-items: center;
    }

    .action-btn {
        padding: 0.75rem 1.5rem;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        white-space: nowrap;
    }

    .stats-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 1.5rem;
        margin-bottom: 2rem;
    }

    @media (max-width: 768px) {
        .stats-grid {
            grid-template-columns: 1fr;
        }
    }

    .stat-card {
        display: flex;
        align-items: center;
        gap: 1.5rem;
        padding: 1.5rem;
    }

    .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: var(--radius-lg);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .stat-info h3 {
        font-size: 0.875rem;
        color: var(--text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.05em;
        margin: 0 0 0.5rem 0;
    }

    .stat-value {
        font-size: 2rem;
        font-weight: 700;
        color: var(--text-primary);
        line-height: 1;
    }

    .tab-btn {
        padding: 0.6rem 1.2rem;
        border: none;
        background: transparent;
        color: var(--text-secondary);
        font-weight: 600;
        cursor: pointer;
        border-bottom: 2px solid transparent;
        transition: all 0.2s;
        font-size: 0.95rem;
    }

    .tab-btn:hover {
        color: var(--primary);
        background: rgba(14, 165, 233, 0.1);
    }

    .tab-btn.active {
        color: var(--bg-color);
        border-bottom-color: var(--primary);
        background: var(--primary);
    }

    .badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 0.4rem 1rem;
        border-radius: 9999px;
        font-size: 0.7rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        min-width: 140px;
        line-height: 1;
        text-align: center;
    }

    .badge-success {
        background: rgba(16, 185, 129, 0.1);
        color: var(--secondary);
    }

    .badge-warning {
        background: rgba(245, 158, 11, 0.1);
        color: var(--warning);
    }

    .badge-admin {
        background: rgba(15, 23, 42, 0.1);
        color: var(--primary);
    }

    .data-table {
        width: 100%;
        border-collapse: collapse;
        table-layout: auto;
    }

    .data-table th, .data-table td {
        padding: 1rem 1.2rem;
        text-align: left;
        border-bottom: 1px solid var(--border-color);
        vertical-align: middle;
    }

    /* Fix actions alignment */
    .actions-cell {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
        align-items: center;
        height: 100%;
        min-height: 40px;
    }

    .data-table th {
        font-weight: 600;
        color: var(--text-secondary);
        background-color: rgba(0,0,0,0.02);
        font-size: 0.8rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        white-space: nowrap;
    }

    .data-table tbody tr:hover {
        background-color: rgba(14, 165, 233, 0.05);
    }

    .btn-action {
        padding: 0.4rem;
        border-radius: var(--radius-sm);
        border: 1px solid transparent;
        background: transparent;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .btn-edit {
        color: var(--primary);
    }

    .btn-edit:hover {
        background: rgba(14, 165, 233, 0.1);
        border-color: rgba(14, 165, 233, 0.2);
    }

    .btn-delete {
        color: var(--danger);
    }

    .btn-delete:hover {
        background: rgba(239, 68, 68, 0.1);
        border-color: rgba(239, 68, 68, 0.2);
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
        max-height: 90vh;
        overflow-y: auto;
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

    @keyframes spin {
        to { transform: rotate(360deg); }
    }
</style>
