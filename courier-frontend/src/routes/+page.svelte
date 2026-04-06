<script>
	// Runes for tracking our simple state
	let isLoggedIn = $state(false);
	let user = $state("");
	let pass = $state("");

	function signin(e) {
		e.preventDefault();
		// In a real app, you'd fetch() to your backend here
		if (user === "admin" && pass === "1234") {
			isLoggedIn = true;
		} else {
			alert("Access Denied");
		}
	}
</script>

<main>
	{#if !isLoggedIn}
		<div class="login-box">
			<h2>Sign In</h2>
			<form onsubmit={signin}>
				<input bind:value={user} placeholder="Username" />
				<input type="password" bind:value={pass} placeholder="Password" />
				<button type="submit">Login</button>
			</form>
		</div>
	{:else}
		<div class="info-box">
			<h1>Success!</h1>
			<p>You are now <strong>logged in</strong> as {user}.</p>
			<button onclick={() => isLoggedIn = false}>Logout</button>
		</div>
	{/if}
</main>

<style>
	:global(body) {
		background-color: #f4f7f6;
		margin: 0;
		display: flex;
		justify-content: center;
		align-items: center;
		height: 100vh;
		font-family: sans-serif;
	}

	.login-box, .info-box {
		background: white;
		padding: 2rem;
		border-radius: 12px;
		box-shadow: 0 10px 25px rgba(0,0,0,0.1);
		text-align: center;
		width: 300px;
	}

	form {
		display: flex;
		flex-direction: column;
		gap: 0.8rem;
	}

	input {
		padding: 10px;
		border: 1px solid #ddd;
		border-radius: 4px;
	}

	button {
		padding: 10px;
		background: #4caf50;
		color: white;
		border: none;
		border-radius: 4px;
		cursor: pointer;
		font-weight: bold;
	}

	button:hover {
		background: #45a049;
	}

	h1 { color: #2e7d32; }
</style>