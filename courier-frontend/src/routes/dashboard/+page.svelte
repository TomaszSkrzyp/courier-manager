<script lang="ts">
    import { auth } from '$lib/auth.svelte';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';

    onMount(() => {
        if (!auth.isLoggedIn) {
            goto('/login');
        } else {
            if (auth.role === 'admin') goto('/dashboard/admin');
            else if (auth.role === 'courier') goto('/dashboard/courier');
            else goto('/dashboard/worker');
        }
    });
</script>

<div style="display: flex; justify-content: center; align-items: center; height: 50vh;">
    <div style="text-align: center;">
        <div class="icon-wrapper" style="margin: 0 auto 1rem auto; animation: spin 2s linear infinite;">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2">
                <line x1="12" y1="2" x2="12" y2="6"></line>
                <line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line>
                <line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
                <line x1="16.24" y1="4.93" x2="19.07" y2="7.76"></line>
            </svg>
        </div>
        <p>Redirecting to your dashboard...</p>
    </div>
</div>

<style>
    @keyframes spin {
        100% { transform: rotate(360deg); }
    }
</style>
