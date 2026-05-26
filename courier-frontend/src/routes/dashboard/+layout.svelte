<script lang="ts">
    import { auth } from '$lib/auth.svelte';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';
    import { page } from '$app/stores';

    let { children } = $props();

    onMount(() => {
        if (!auth.isLoggedIn) {
            goto('/login');
        }
    });

    $effect(() => {
        if (!auth.isLoggedIn) {
            goto('/login');
        }
    });
</script>

{#if auth.isLoggedIn}
    <div class="dashboard-layout">
        <aside class="sidebar">
            <div class="sidebar-header">
                <h3>{auth.role === 'office' ? 'Worker' : auth.role === 'admin' ? 'Admin' : 'Courier'} Panel</h3>
            </div>
            
            <nav class="sidebar-nav">
                {#if auth.role === 'admin'}
                    <a href="/dashboard/admin" class="sidebar-link" class:active={$page.url.pathname === '/dashboard/admin'}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                        </svg>
                        Personnel
                    </a>
                {/if}

                {#if auth.role === 'office' || auth.role === 'admin'}
                    <a href="/dashboard/worker" class="sidebar-link" class:active={$page.url.pathname === '/dashboard/worker'}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                            <line x1="8" y1="21" x2="16" y2="21"></line>
                            <line x1="12" y1="17" x2="12" y2="21"></line>
                        </svg>
                        Parcels
                    </a>
                {/if}

                {#if auth.role === 'courier'}
                    <a href="/dashboard/courier" class="sidebar-link" class:active={$page.url.pathname === '/dashboard/courier'}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="1" y="3" width="15" height="13"></rect>
                            <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
                            <circle cx="5.5" cy="18.5" r="2.5"></circle>
                            <circle cx="18.5" cy="18.5" r="2.5"></circle>
                        </svg>
                        My Deliveries
                    </a>
                {/if}
            </nav>
        </aside>

        <main class="dashboard-content">
            {@render children()}
        </main>
    </div>
{/if}

<style>
    .dashboard-layout {
        display: flex;
        gap: 2rem;
        min-height: calc(100vh - 80px); /* Adjust based on navbar height */
    }

    .sidebar {
        width: 250px;
        min-width: 250px;
        flex-shrink: 0;
        background: var(--surface-color);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-sm);
        border: 1px solid var(--border-color);
        padding: 1.5rem 0;
        height: fit-content;
        position: sticky;
        top: 100px;
        display: flex;
        flex-direction: column;
    }

    .sidebar-header {
        padding: 0 1.5rem 1rem 1.5rem;
        border-bottom: 1px solid var(--border-color);
        margin-bottom: 1rem;
    }

    .sidebar-header h3 {
        margin: 0;
        font-size: 1rem;
        color: var(--text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.05em;
    }

    .sidebar-nav {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .sidebar-link {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.75rem 1.5rem;
        color: var(--text-primary);
        font-weight: 500;
        transition: all var(--transition-fast);
        border-left: 3px solid transparent;
    }

    .sidebar-link:hover {
        background: rgba(14, 165, 233, 0.05);
        color: var(--primary);
    }

    .sidebar-link.active {
        background: rgba(14, 165, 233, 0.1);
        color: var(--primary);
        border-left-color: var(--primary);
        font-weight: 600;
    }

    .dashboard-content {
        flex: 1;
        min-width: 0;
        padding: 0 !important;
        max-width: 100% !important;
        margin: 0 !important;
        overflow: hidden;
    }

    @media (max-width: 768px) {
        .dashboard-layout {
            flex-direction: column;
        }
        
        .sidebar {
            width: 100%;
            position: static;
        }
    }
</style>
