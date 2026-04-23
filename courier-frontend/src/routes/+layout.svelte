<script lang="ts">
	import '../app.css';
	import favicon from '$lib/assets/favicon.svg';
	import { auth } from '$lib/auth.svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';

	let { children } = $props();

	function handleLogout() {
		auth.logout();
		goto('/login');
	}

	$effect(() => {
		// Just to trigger reactivity if auth changes
		auth.isLoggedIn;
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Courier Manager</title>
</svelte:head>

<div class="app-container">
	<nav class="navbar">
		<a href="/" class="navbar-brand">
			<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
				<line x1="8" y1="21" x2="16" y2="21"></line>
				<line x1="12" y1="17" x2="12" y2="21"></line>
			</svg>
			SwiftCourier
		</a>

		<div class="navbar-nav">
			<a href="/" class="nav-link" class:active={$page.url.pathname === '/'}>Track</a>
			<a href="/sendPackage" class="nav-link" class:active={$page.url.pathname === '/sendPackage'}>Send Package</a>
			
			{#if auth.isLoggedIn}
				{#if auth.role === 'admin'}
					<a href="/dashboard/admin" class="nav-link" class:active={$page.url.pathname.includes('/admin')}>Admin</a>
				{/if}
				{#if auth.role === 'office'}
					<a href="/dashboard/worker" class="nav-link" class:active={$page.url.pathname.includes('/worker')}>Worker</a>
				{/if}
				{#if auth.role === 'courier'}
					<a href="/dashboard/courier" class="nav-link" class:active={$page.url.pathname.includes('/courier')}>Courier</a>
				{/if}
				
				<div class="user-profile">
					<div>
						<div style="font-weight: 600; font-size: 0.9rem;">{auth.user}</div>
						<div class="user-role">{auth.role}</div>
					</div>
					<button onclick={handleLogout} class="btn btn-outline" style="padding: 0.4rem 1rem; font-size: 0.875rem;">Logout</button>
				</div>
			{:else}
				<a href="/login" class="btn btn-primary" style="padding: 0.5rem 1.2rem;">Sign In</a>
			{/if}
		</div>
	</nav>

	<main>
		{@render children()}
	</main>
</div>
