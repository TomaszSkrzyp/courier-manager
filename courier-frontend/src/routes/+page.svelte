<script lang="ts">
    import { fade, slide } from 'svelte/transition';

    let trackingValue = $state("");
    let foundParcel = $state<any>(null);
    let isNumberValid = $state(true);
    let showResults = $state(false);
    let isLoading = $state(false);

    async function handleSubmit(e: SubmitEvent) {
        e.preventDefault();

        if (!trackingValue || trackingValue.length !== 24) {
            isNumberValid = false;
            showResults = true;
            return;
        }

        isLoading = true;
        showResults = false;

        try {
            // Simulate network delay for animation
            await new Promise(r => setTimeout(r, 600));
            const res = await fetch(`http://localhost:8080/api/parcels/track/${trackingValue}`);
            if (res.ok) {
                foundParcel = await res.json();
                isNumberValid = true;
            } else {
                foundParcel = null;
                isNumberValid = false;
            }
        } catch (err) {
            console.error("Connection error", err);
            foundParcel = null;
            isNumberValid = false;
        } finally {
            isLoading = false;
            showResults = true;
        }
    }

    const statuses = ["Created", "Collected", "In Transit", "Out for Delivery", "Delivered"];
    function getStatusIndex(status: string) {
        // Map backend status to our visual timeline
        if (!status) return 0;
        const s = status.toUpperCase();
        if (s.includes("CREATE") || s.includes("REGISTER") || s.includes("NONE")) return 0;
        if (s.includes("COLLECT") || s.includes("PICKUP")) return 1;
        if (s.includes("TRANSIT") || s.includes("HUB") || s.includes("SORT")) return 2;
        if (s.includes("OUT")) return 3;
        if (s.includes("DELIVER")) return 4;
        return 2; // Default fallback
    }
</script>

<div class="tracking-hero">
    <div class="hero-content animate-fade-in">
        <h1 style="font-size: 3rem; margin-bottom: 0.5rem; background: linear-gradient(to right, var(--primary), var(--secondary)); -webkit-background-clip: text; color: transparent;">
            Track Your Package
        </h1>
        <p style="font-size: 1.2rem; color: var(--text-secondary); margin-bottom: 2.5rem;">
            Enter your 24-digit tracking number to see real-time updates.
        </p>

        <form onsubmit={handleSubmit} class="search-form">
            <div class="search-bar">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input
                    type="text"
                    pattern={"\\d{1,24}"}
                    bind:value={trackingValue}
                    minlength="1"
                    maxlength="24"
                    required
                    title="Must be between 1 and 24 digits"
                    placeholder="e.g. 123456789012345678901234"
                    oninput={() => (showResults = false)}
                />
                <button type="submit" class="btn btn-primary" disabled={isLoading}>
                    {isLoading ? 'Searching...' : 'Track'}
                </button>
            </div>
        </form>


    </div>
</div>

{#if showResults}
    <div class="results-container" in:slide>
        {#if !isNumberValid || (!foundParcel && !isNumberValid)}
            <div class="glass-panel text-center" style="border-left: 4px solid var(--danger);">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="2" style="margin-bottom: 1rem;">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="15" y1="9" x2="9" y2="15"></line>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
                <h3>Package Not Found</h3>
                <p style="color: var(--text-secondary);">Please check your tracking number and try again.</p>
            </div>
        {:else if foundParcel}
            {@const currentIndex = getStatusIndex(foundParcel.status)}
            <div class="glass-panel animate-fade-in">
                <div class="parcel-header">
                    <div>
                        <div style="font-size: 0.875rem; color: var(--text-tertiary); text-transform: uppercase;">Tracking Number</div>
                        <h2 style="margin: 0;">{foundParcel.trackingNumber}</h2>
                    </div>
                    <div style="text-align: right; display: flex; flex-direction: column; gap: 0.5rem; align-items: flex-end;">
                        <span class="badge" class:badge-primary={foundParcel.deliveryMode === 'EXPRESS'} class:badge-outline={foundParcel.deliveryMode === 'NORMAL'}>
                            {foundParcel.deliveryMode}
                        </span>
                        <span class="badge" style="font-size: 1rem; padding: 0.5rem 1rem; background: var(--secondary); color: white;">
                            {foundParcel.status || "In Transit"}
                        </span>
                    </div>
                </div>

                <div class="parcel-details">
                    <div class="detail-item">
                        <span class="detail-label">Destination</span>
                        <span class="detail-value">{foundParcel.city || "Unknown"}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Expected Delivery</span>
                        <span class="detail-value">{foundParcel.expectedDelivery || "Pending"}</span>
                    </div>
                </div>

                <div class="timeline-container">
                    <div class="timeline-track"></div>
                    <div class="timeline-progress" style="width: {(currentIndex / (statuses.length - 1)) * 100}%"></div>
                    
                    {#each statuses as status, i}
                        <div class="timeline-step" class:active={i <= currentIndex}>
                            <div class="timeline-point">
                                {#if i < currentIndex}
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                                        <polyline points="20 6 9 17 4 12"></polyline>
                                    </svg>
                                {/if}
                            </div>
                            <div class="timeline-label">{status}</div>
                        </div>
                    {/each}
                </div>
            </div>
        {/if}
    </div>
{/if}

<style>
    .tracking-hero {
        padding: 4rem 1rem;
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .hero-content {
        max-width: 600px;
        width: 100%;
    }

    .search-form {
        width: 100%;
    }

    .search-bar {
        display: flex;
        align-items: center;
        background: var(--surface-color);
        border-radius: var(--radius-full);
        padding: 0.5rem 0.5rem 0.5rem 1.5rem;
        box-shadow: var(--shadow-md);
        border: 1px solid var(--border-color);
        transition: all var(--transition-normal);
    }

    .search-bar:focus-within {
        box-shadow: var(--shadow-lg), 0 0 0 3px rgba(79, 70, 229, 0.2);
        border-color: var(--primary);
    }

    .search-icon {
        color: var(--text-tertiary);
        margin-right: 0.5rem;
    }

    .search-bar input {
        flex: 1;
        border: none;
        outline: none;
        background: transparent;
        font-size: 1.1rem;
        color: var(--text-primary);
        font-family: var(--font-family);
    }

    .search-bar button {
        border-radius: var(--radius-full);
        padding: 0.75rem 2rem;
    }

    .results-container {
        max-width: 800px;
        margin: 0 auto 4rem auto;
    }

    .parcel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-bottom: 1.5rem;
        border-bottom: 1px solid var(--border-color);
        margin-bottom: 1.5rem;
    }

    .parcel-details {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1.5rem;
        margin-bottom: 2.5rem;
    }

    .detail-item {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .detail-label {
        font-size: 0.875rem;
        color: var(--text-secondary);
        font-weight: 500;
    }

    .detail-value {
        font-size: 1.125rem;
        font-weight: 600;
        color: var(--text-primary);
    }

    .timeline-container {
        position: relative;
        padding: 2rem 0;
        display: flex;
        justify-content: space-between;
    }

    .timeline-track {
        position: absolute;
        top: 2.75rem;
        left: 0;
        right: 0;
        height: 4px;
        background: var(--border-color);
        z-index: 0;
    }

    .timeline-progress {
        position: absolute;
        top: 2.75rem;
        left: 0;
        height: 4px;
        background: var(--primary);
        z-index: 1;
        transition: width 1s ease-in-out;
    }

    .timeline-step {
        position: relative;
        z-index: 2;
        display: flex;
        flex-direction: column;
        align-items: center;
        width: 20%;
    }

    .timeline-point {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: var(--surface-color);
        border: 4px solid var(--border-color);
        margin-bottom: 0.75rem;
        transition: all 0.5s ease;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .timeline-step.active .timeline-point {
        background: var(--primary);
        border-color: var(--primary);
    }

    .timeline-label {
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--text-tertiary);
        text-align: center;
        transition: color 0.5s ease;
    }

    .timeline-step.active .timeline-label {
        color: var(--primary);
        font-weight: 600;
    }
</style>