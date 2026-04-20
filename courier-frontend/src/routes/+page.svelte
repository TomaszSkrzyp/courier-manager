<script lang="ts">
    let trackingValue = $state("");
    let foundParcel = $state<any>(null);
    let isNumberValid = $state(true);
    let showResults = $state(false);

    async function handleSubmit(e: SubmitEvent) {
        e.preventDefault();

        if (!trackingValue || trackingValue.length !== 24) {
            isNumberValid = false;
            showResults = true;
            return;
        }

        try {
            const res = await fetch(`http://localhost:8080/api/parcels/track/${trackingValue}`);
            if (res.ok) {
                foundParcel = await res.json();
                isNumberValid = true;
            } else {
                foundParcel = null;
                isNumberValid = false;
            }
        } catch (err) {
            console.error("Connection error");
            isNumberValid = false;
        }
        showResults = true;
    }
</script>
<div style="padding: 2rem; font-family: sans-serif; max-width: 600px; margin: 0 auto;">
	<form onsubmit={handleSubmit} style="display: flex; gap: 0.5rem; margin-bottom: 2rem;">
		<input
			type="text"
			pattern={"\\d{1,24}"}
            bind:value={trackingValue}
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
    {:else if foundParcel}
        <div style="background: #f4f4f4; padding: 1rem; border-radius: 8px;">
            <h3>Parcel Found!</h3>
            <p>Tracking Number: <strong>{foundParcel.trackingNumber}</strong></p>
            <p>Destination: <strong>{foundParcel.city}</strong></p>
            <p>Current Status: {foundParcel.status}</p>
            
        </div>
    {/if}
	{/if}

	<div style="margin-top: 3rem; font-size: 0.9rem; display: flex; justify-content: space-between;">
		<label><input type="checkbox" bind:checked={isNumberValid} /> Simulate valid number</label>
		<div style="display: flex; gap: 1rem;">
			<a href="/login" style="color: inherit; text-decoration: none;">Login</a>
			<a href="/sendPackage" style="color: inherit; text-decoration: none;">Send Package</a>
		</div>
	</div>
</div>