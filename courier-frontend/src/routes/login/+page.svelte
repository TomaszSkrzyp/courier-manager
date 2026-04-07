<script lang="ts">
	let user = $state("");
	let pass = $state("");
	let isLoggedIn = $state(false);

	function signin(e: SubmitEvent) {
		e.preventDefault();
		if (user === "admin" && pass === "1234") {
			isLoggedIn = true;
		} else {
			alert("Access Denied");
		}
	}
</script>

<div style="padding: 2rem; font-family: sans-serif; max-width: 300px; margin: 0 auto;">
	{#if !isLoggedIn}
		<h2>Sign In</h2>
		<form onsubmit={signin} style="display: flex; flex-direction: column; gap: 0.5rem;">
			<input bind:value={user} placeholder="Username" style="padding: 0.5rem;" />
			<input type="password" bind:value={pass} placeholder="Password" style="padding: 0.5rem;" />
			<button type="submit" style="padding: 0.5rem;">Login</button>
		</form>
	{:else}
		<p>Logged in as <strong>{user}</strong>.</p>
		<button onclick={() => (isLoggedIn = false)} style="padding: 0.5rem;">Logout</button>
	{/if}
</div>
