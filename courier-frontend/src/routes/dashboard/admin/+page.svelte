<script lang="ts">
    import { slide, fade } from 'svelte/transition';
    import { onMount } from 'svelte';

    interface Employee {
        id: number;
        firstName: string;
        lastName: string;
        role: "office" | "courier";
        login: string;
        regions: string[];
        dateAdded: string;
    }

    let employees = $state<Employee[]>([]);
    let regions = $state<{id: number, name: string}[]>([]);

    onMount(async () => {
        try {
            const empRes = await fetch("http://localhost:8080/api/employees");
            if (empRes.ok) employees = await empRes.json();
            
            const regRes = await fetch("http://localhost:8080/api/regions");
            if (regRes.ok) regions = await regRes.json();
        } catch (e) {
            console.error("Failed to fetch initial data", e);
        }
    });

    let showAddModal = $state(false);

    let newEmp = $state({
        firstName: "",
        lastName: "",
        pesel: "",
        login: "",
        password: "",
        role: "courier",
        regions: ["", ""]
    });

    let showEditModal = $state(false);
    let editingId = $state<number | null>(null);
    let editEmp = $state({
        firstName: "",
        lastName: "",
        login: "",
        role: "courier",
        regions: ["", ""]
    });

    let isSubmitting = $state(false);

    async function handleAddEmployee(e: SubmitEvent) {
        e.preventDefault();
        isSubmitting = true;

        try {
            const res = await fetch("http://localhost:8080/api/employees", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    ...newEmp,
                    regions: newEmp.role === 'courier' ? newEmp.regions.filter(r => r.trim() !== '') : []
                })
            });

            if (res.ok) {
                const added = await res.json();
                employees = [...employees, added];
                
                showAddModal = false;
                newEmp = {
                    firstName: "",
                    lastName: "",
                    pesel: "",
                    login: "",
                    password: "",
                    role: "courier",
                    regions: ["", ""]
                };
                
                // Refresh regions list in case new ones were added
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
            try {
                const res = await fetch(`http://localhost:8080/api/employees/${id}`, {
                    method: "DELETE"
                });
                if (res.ok) {
                    employees = employees.filter(e => e.id !== id);
                }
            } catch (e) {
                console.error("Failed to delete employee", e);
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
            login: emp.login,
            role: emp.role,
            regions: regs
        };
        showEditModal = true;
    }

    async function handleEditEmployee(e: SubmitEvent) {
        e.preventDefault();
        isSubmitting = true;

        try {
            const res = await fetch(`http://localhost:8080/api/employees/${editingId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    ...editEmp,
                    regions: editEmp.role === 'courier' ? editEmp.regions.filter(r => r.trim() !== '') : []
                })
            });

            if (res.ok) {
                const updated = await res.json();
                employees = employees.map(emp => emp.id === editingId ? updated : emp);
                showEditModal = false;
                editingId = null;
                
                const regRes = await fetch("http://localhost:8080/api/regions");
                if (regRes.ok) regions = await regRes.json();
            }
        } catch (e) {
            console.error("Failed to edit employee", e);
        } finally {
            isSubmitting = false;
        }
    }
</script>

<div class="dashboard-panel animate-fade-in">
    <div class="panel-header">
        <div>
            <h2>Admin Dashboard</h2>
            <p style="color: var(--text-secondary);">Manage system personnel across all regions.</p>
        </div>
        <button class="btn btn-primary" onclick={() => showAddModal = true}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 0.5rem;">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Add New Employee
        </button>
    </div>

    <div class="stats-grid">
        <div class="glass-panel stat-card">
            <div class="stat-icon" style="background: rgba(79, 70, 229, 0.1); color: var(--primary);">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
            </div>
            <div class="stat-info">
                <h3>Total Personnel</h3>
                <div class="stat-value">{employees.length}</div>
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
                <h3>Active Couriers</h3>
                <div class="stat-value">{employees.filter(e => e.role === 'courier').length}</div>
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
                <h3>Office Workers</h3>
                <div class="stat-value">{employees.filter(e => e.role === 'office').length}</div>
            </div>
        </div>
    </div>

    <div class="glass-panel" style="padding: 0; overflow: hidden;">
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
                {#each employees as emp}
                    <tr in:slide>
                        <td style="font-weight: 500;">{emp.firstName} {emp.lastName}</td>
                        <td>
                            <span class="badge" class:badge-success={emp.role === 'courier'} class:badge-warning={emp.role === 'office'}>
                                {emp.role === 'courier' ? 'COURIER' : 'OFFICE WORKER'}
                            </span>
                        </td>
                        <td style="font-family: monospace; color: var(--text-secondary);">{emp.login}</td>
                        <td>{emp.regions.length > 0 ? emp.regions.join(" ↔ ") : "N/A"}</td>
                        <td style="color: var(--text-tertiary);">{emp.dateAdded}</td>
                        <td style="text-align: right; display: flex; justify-content: flex-end; gap: 0.5rem;">
                            <button class="btn btn-outline" style="padding: 0.4rem; color: var(--primary); border-color: transparent;" title="Edit" onclick={() => openEditModal(emp)}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                </svg>
                            </button>
                            <button class="btn btn-outline" style="padding: 0.4rem; color: var(--danger); border-color: transparent;" title="Remove" onclick={() => deleteEmployee(emp.id)}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="3 6 5 6 21 6"></polyline>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                </svg>
                            </button>
                        </td>
                    </tr>
                {/each}
            </tbody>
        </table>
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
                    <label>PESEL</label>
                    <input bind:value={newEmp.pesel} type="text" class="input-field" pattern="\d{11}" title="11 digits required" required />
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>System Login</label>
                        <input bind:value={newEmp.login} type="text" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Temporary Password</label>
                        <input bind:value={newEmp.password} type="password" class="input-field" required />
                    </div>
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>Role</label>
                        <select bind:value={newEmp.role} class="input-field">
                            <option value="courier">Courier</option>
                            <option value="office">Office Worker</option>
                        </select>
                    </div>
                    
                    {#if newEmp.role === 'courier'}
                        <div class="input-group" style="display: flex; gap: 0.5rem; flex-direction: column;">
                            <label>Assigned Cities</label>
                            <datalist id="regions-list">
                                {#each regions as r}
                                    <option value={r.name}></option>
                                {/each}
                            </datalist>
                            <div style="display: flex; gap: 0.5rem;">
                                <input bind:value={newEmp.regions[0]} list="regions-list" class="input-field" placeholder="City 1" required />
                                <span style="align-self: center;">↔</span>
                                <input bind:value={newEmp.regions[1]} list="regions-list" class="input-field" placeholder="City 2 (Optional)" />
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

                <div class="grid-2">
                    <div class="input-group">
                        <label>System Login</label>
                        <input bind:value={editEmp.login} type="text" class="input-field" required />
                    </div>
                </div>

                <div class="grid-2">
                    <div class="input-group">
                        <label>Role</label>
                        <select bind:value={editEmp.role} class="input-field">
                            <option value="courier">Courier</option>
                            <option value="office">Office Worker</option>
                        </select>
                    </div>
                    
                    {#if editEmp.role === 'courier'}
                        <div class="input-group" style="display: flex; gap: 0.5rem; flex-direction: column;">
                            <label>Assigned Cities</label>
                            <datalist id="regions-list-edit">
                                {#each regions as r}
                                    <option value={r.name}></option>
                                {/each}
                            </datalist>
                            <div style="display: flex; gap: 0.5rem;">
                                <input bind:value={editEmp.regions[0]} list="regions-list-edit" class="input-field" placeholder="City 1" required />
                                <span style="align-self: center;">↔</span>
                                <input bind:value={editEmp.regions[1]} list="regions-list-edit" class="input-field" placeholder="City 2 (Optional)" />
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

    .data-table tbody tr:hover {
        background-color: rgba(79, 70, 229, 0.02);
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
        max-width: 600px;
        margin: 1rem;
        max-height: 90vh;
        overflow-y: auto;
    }

    .grid-2 {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
    }
</style>
