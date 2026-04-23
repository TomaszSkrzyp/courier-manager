<script lang="ts">
	import { auth } from "../../lib/auth.svelte";
	import { goto } from "$app/navigation";

	let user = $state("");
	let pass = $state("");
	let errorMsg = $state("");

	const VALID_USERS = {
        admin: { pass: "1234", role: "admin" },
        courier: { pass: "1234", role: "courier" },
        office: { pass: "1234", role: "office" }
    } as const;

	function signin(e: SubmitEvent) {
		e.preventDefault();
		errorMsg = "";
		
		const account = VALID_USERS[user.toLowerCase() as keyof typeof VALID_USERS];
		if (account && account.pass === pass) {
            auth.login(user, account.role);
			if (account.role === 'admin') goto("/dashboard/admin");
			else if (account.role === 'courier') goto("/dashboard/courier");
			else goto("/dashboard/worker");
        } else {
            errorMsg = "Invalid username or password";
        }
	}
</script>

<div class="login-container animate-fade-in">
	<div class="glass-panel login-card">
		<div class="text-center" style="text-align: center; margin-bottom: 2rem;">
			<div class="icon-wrapper">
				<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
					<circle cx="12" cy="7" r="4"></circle>
				</svg>
			</div>
			<h2>Welcome Back</h2>
			<p style="color: var(--text-secondary);">Sign in to access your dashboard</p>
		</div>

		{#if errorMsg}
			<div class="error-banner animate-slide-in">
				{errorMsg}
			</div>
		{/if}

		<form onsubmit={signin}>
			<div class="input-group">
				<label for="username">Username</label>
				<input 
					id="username"
					bind:value={user} 
					placeholder="Enter your username" 
					class="input-field"
					required
				/>
			</div>
			
			<div class="input-group">
				<label for="password">Password</label>
				<input 
					id="password"
					type="password" 
					bind:value={pass} 
					placeholder="Enter your password" 
					class="input-field"
					required
				/>
			</div>
			
			<button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">
				Sign In
			</button>
		</form>

		<div class="demo-credentials">
			<p>Demo accounts (pass: 1234):</p>
			<div style="display: flex; gap: 0.5rem; justify-content: center; margin-top: 0.5rem;">
				<span class="badge badge-info">admin</span>
				<span class="badge badge-warning">office</span>
				<span class="badge badge-success">courier</span>
			</div>
		</div>
	</div>
</div>

<style>
	.login-container {
		display: flex;
		justify-content: center;
		align-items: center;
		min-height: calc(100vh - 150px);
	}

	.login-card {
		width: 100%;
		max-width: 400px;
	}

	.icon-wrapper {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 80px;
		height: 80px;
		background: rgba(79, 70, 229, 0.1);
		border-radius: 50%;
		margin-bottom: 1rem;
	}

	.error-banner {
		background-color: rgba(239, 68, 68, 0.1);
		color: var(--danger);
		padding: 0.75rem;
		border-radius: var(--radius-md);
		margin-bottom: 1.5rem;
		text-align: center;
		font-weight: 500;
		border: 1px solid rgba(239, 68, 68, 0.2);
	}

	.demo-credentials {
		margin-top: 2rem;
		text-align: center;
		font-size: 0.875rem;
		color: var(--text-tertiary);
		padding-top: 1.5rem;
		border-top: 1px solid var(--border-color);
	}
</style>
