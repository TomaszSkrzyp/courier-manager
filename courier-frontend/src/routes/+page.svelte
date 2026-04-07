<script lang="ts">
	let isNumberValid = $state(true); // Toggle for demo purposes
	let showResults = $state(false);

	const updates = [
		{ date: "2024-03-20 14:30", status: "Out for delivery" },
		{ date: "2024-03-20 08:15", status: "Arrived at logistics center" },
		{ date: "2024-03-19 22:45", status: "In transit" },
		{ date: "2024-03-19 10:00", status: "Shipment registered" }
	];

	function handleSubmit(e: SubmitEvent) {
		e.preventDefault();
		showResults = true;
	}
</script>

<div style="padding: 2rem; font-family: sans-serif; max-width: 600px; margin: 0 auto;">
	<form onsubmit={handleSubmit} style="display: flex; gap: 0.5rem; margin-bottom: 2rem;">
		<input
			type="text"
			pattern={"\\d{1,24}"}
			minlength="1"
			maxlength="24"
			required
			title="Must be between 1 and 24 digits"
			placeholder="Tracking number (max 24 digits)..."
			style="flex: 1; padding: 0.5rem;"
			oninput={() => (showResults = false)}
		/>
		<button type="submit" style="padding: 0.5rem 1rem;">Track</button>
	</form>

	{#if showResults}
		{#if !isNumberValid}
			<p style="color: red;">Incorrect tracking number.</p>
		{:else}
			<ul style="padding: 0; list-style: none;">
				{#each updates as update}
					<li style="margin-bottom: 0.5rem;">
						<small style="color: gray;">{update.date}</small> - <strong>{update.status}</strong>
					</li>
				{/each}
			</ul>
		{/if}
	{/if}

	<div style="margin-top: 3rem; font-size: 0.9rem; display: flex; justify-content: space-between;">
		<label><input type="checkbox" bind:checked={isNumberValid} /> Simulate valid number</label>
		<a href="/login" style="color: inherit; text-decoration: none;">Login</a>
	</div>
</div>
