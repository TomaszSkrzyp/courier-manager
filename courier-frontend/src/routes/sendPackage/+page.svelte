<script lang="ts">
    import { slide } from 'svelte/transition';

    interface NewPackage {
        phoneNumber: string;

        senderStreet: string;
        senderBuildingNumber: string;
        senderPostalCode: string;
        senderRegionId: number;

        recipientStreet: string;
        recipientBuildingNumber: string;
        recipientPostalCode: string;
        recipientRegionId: number;

        weight: number;
        height: number;
        width: number;
        length: number;
        fragility: string;
        deliveryModeId: number;
        comment: string;
    }

    let deliveryModes = $state<{id: number, name: string}[]>([]);

    import { onMount } from 'svelte';

    let regions = $state<{id: number, name: string}[]>([]);

    onMount(async () => {
        try {
            const regRes = await fetch("http://localhost:8080/api/regions/active");
            if (regRes.ok) regions = await regRes.json();

            const dmRes = await fetch("http://localhost:8080/api/delivery-modes");
            if (dmRes.ok) deliveryModes = await dmRes.json();
        } catch (e) {
            console.error("Failed to fetch form data", e);
        }
    });

    let form = $state<NewPackage>({
        phoneNumber: "",
        senderStreet: "",
        senderBuildingNumber: "",
        senderPostalCode: "",
        senderRegionId: 0,
        recipientStreet: "",
        recipientBuildingNumber: "",
        recipientPostalCode: "",
        recipientRegionId: 0,
        weight: 0,
        height: 0,
        width: 0,
        length: 0,
        fragility: "no",
        deliveryModeId: 0,
        comment: "",
    });

    let submitted = $state(false);
    let generatedId = $state("");
    let isSubmitting = $state(false);
    let errorMessage = $state("");


    async function handleSubmit(e: SubmitEvent) {
        e.preventDefault();
        isSubmitting = true;
        errorMessage = "";

        try {
            const res = await fetch("http://localhost:8080/api/parcels", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(form)
            });

            if (res.ok) {
                const created = await res.json();
                generatedId = created.trackingNumber;
                submitted = true;
            } else {
                const text = await res.text();
                errorMessage = text || "Failed to create parcel. Please check if regions are connected by couriers.";
                console.error("Failed to create parcel", text);
            }
        } catch (e) {
            errorMessage = "Connection error. Please try again later.";
            console.error("Error creating parcel", e);
        } finally {
            isSubmitting = false;
        }
    }

    function reset() {
        submitted = false;
        generatedId = "";
        form = {
            phoneNumber: "",
            senderStreet: "", senderBuildingNumber: "", senderPostalCode: "", senderRegionId: 0,
            recipientStreet: "", recipientBuildingNumber: "", recipientPostalCode: "", recipientRegionId: 0,
            weight: 0, height: 0, width: 0, length: 0,
            fragility: "no", deliveryModeId: 0, comment: "",
        };
    }
</script>

<div class="page-container animate-fade-in">
    <div class="header-section">
        <h1>Send a Package</h1>
        <p style="color: var(--text-secondary);">Fill in the details below to generate a tracking number.</p>
    </div>

    {#if submitted}
        <div class="glass-panel text-center animate-fade-in" style="max-width: 600px; margin: 0 auto; border-top: 4px solid var(--secondary);">
            <div class="icon-wrapper" style="background: rgba(16, 185, 129, 0.1); margin: 0 auto 1.5rem auto;">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--secondary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
            </div>
            <h2>Package Successfully Sent!</h2>
            <p style="color: var(--text-secondary); margin-bottom: 2rem;">Your package has been registered in our system.</p>
            
            <div class="tracking-card">
                <span style="font-size: 0.875rem; color: var(--text-tertiary); text-transform: uppercase;">Tracking Number</span>
                <div class="tracking-number">{generatedId}</div>
            </div>
            
            <div style="display: flex; gap: 1rem; justify-content: center; margin-top: 2rem;">
                <button onclick={reset} class="btn btn-outline">Send Another</button>
                <a href="/" class="btn btn-primary">Track Package</a>
            </div>
        </div>
    {:else}
        <form onsubmit={handleSubmit} class="form-layout" in:slide>
            <!-- Sender Information -->
            <div class="glass-panel form-section">
                <h3 class="section-title">
                    <span class="step-number">1</span> Sender Details
                </h3>
                
                <div class="input-group">
                    <label>Phone Number</label>
                    <input bind:value={form.phoneNumber} type="tel" placeholder="+48 000 000 000" class="input-field" required />
                </div>
                
                <div class="grid-2">
                    <div class="input-group" style="grid-column: span 2;">
                        <label>Street Name</label>
                        <input bind:value={form.senderStreet} type="text" placeholder="e.g. Kwiatowa" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Building No.</label>
                        <input bind:value={form.senderBuildingNumber} type="text" placeholder="e.g. 15A" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Postal Code</label>
                        <input 
                            bind:value={form.senderPostalCode} 
                            oninput={(e) => {
                                let v = e.currentTarget.value.replace(/\D/g, '');
                                if (v.length > 2) v = v.substring(0, 2) + '-' + v.substring(2, 5);
                                form.senderPostalCode = v;
                            }}
                            type="text" 
                            placeholder="00-000" 
                            class="input-field" 
                            required 
                            minlength="6"
                            maxlength="6"
                        />
                    </div>
                    <div class="input-group" style="grid-column: span 2;">
                        <label>Region</label>
                        <select bind:value={form.senderRegionId} class="input-field" required>
                            <option value={0} disabled>Select a region</option>
                            {#each regions as r}
                                <option value={r.id}>{r.name}</option>
                            {/each}
                        </select>
                    </div>
                </div>
            </div>

            <!-- Recipient Information -->
            <div class="glass-panel form-section">
                <h3 class="section-title">
                    <span class="step-number">2</span> Recipient Details
                </h3>
                
                <div class="grid-2">
                    <div class="input-group" style="grid-column: span 2;">
                        <label>Street Name</label>
                        <input bind:value={form.recipientStreet} type="text" placeholder="e.g. Długa" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Building No.</label>
                        <input bind:value={form.recipientBuildingNumber} type="text" placeholder="e.g. 42" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Postal Code</label>
                        <input 
                            bind:value={form.recipientPostalCode} 
                            oninput={(e) => {
                                let v = e.currentTarget.value.replace(/\D/g, '');
                                if (v.length > 2) v = v.substring(0, 2) + '-' + v.substring(2, 5);
                                form.recipientPostalCode = v;
                            }}
                            type="text" 
                            placeholder="00-000" 
                            class="input-field" 
                            required 
                            minlength="6"
                            maxlength="6"
                        />
                    </div>
                    <div class="input-group" style="grid-column: span 2;">
                        <label>Region</label>
                        <select bind:value={form.recipientRegionId} class="input-field" required>
                            <option value={0} disabled>Select a region</option>
                            {#each regions as r}
                                <option value={r.id}>{r.name}</option>
                            {/each}
                        </select>
                    </div>
                </div>
            </div>

            <!-- Package Details -->
            <div class="glass-panel form-section" style="grid-column: 1 / -1;">
                <h3 class="section-title">
                    <span class="step-number">3</span> Package Specifications
                </h3>
                
                <div class="grid-4">
                    <div class="input-group">
                        <label>Weight (kg)</label>
                        <input bind:value={form.weight} type="number" min="0.1" step="0.1" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Length (cm)</label>
                        <input bind:value={form.length} type="number" min="1" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Width (cm)</label>
                        <input bind:value={form.width} type="number" min="1" class="input-field" required />
                    </div>
                    <div class="input-group">
                        <label>Height (cm)</label>
                        <input bind:value={form.height} type="number" min="1" class="input-field" required />
                    </div>
                </div>

                <div class="grid-2" style="margin-top: 1rem;">
                    <div class="input-group">
                        <label>Delivery Mode</label>
                        <select bind:value={form.deliveryModeId} class="input-field" required>
                            <option value={0} disabled>Select mode</option>
                            {#each deliveryModes as m}
                                <option value={m.id}>{m.name}</option>
                            {/each}
                        </select>
                        <p style="font-size: 0.75rem; color: var(--text-tertiary); margin-top: 0.5rem;">
                            EXPRESS: 1 day/region • NORMAL: 1.5 days/region
                        </p>
                    </div>
                    <div class="input-group">
                        <label>Fragile Contents?</label>
                        <select bind:value={form.fragility} class="input-field">
                            <option value="no">No — Normal Handling</option>
                            <option value="yes">Yes — Handle with Care</option>
                        </select>
                    </div>
                </div>
                
                <div class="input-group" style="margin-top: 1rem;">
                    <label>Additional Notes</label>
                    <textarea bind:value={form.comment} placeholder="Instructions for the courier..." class="input-field" rows="3" style="resize: vertical;"></textarea>
                </div>
            </div>

            <div class="submit-section" style="flex-direction: column; align-items: flex-end; gap: 1rem;">
                {#if errorMessage}
                    <div class="glass-panel" style="background: rgba(239, 68, 68, 0.1); border-color: var(--danger); padding: 1rem; color: var(--danger); font-weight: 500; width: 100%;">
                        {errorMessage}
                    </div>
                {/if}
                <button type="submit" class="btn btn-primary btn-large" disabled={isSubmitting}>
                    {#if isSubmitting}
                        Processing...
                    {:else}
                        Submit Package Order
                    {/if}
                </button>
            </div>
        </form>
    {/if}
</div>

<style>
    .page-container {
        max-width: 1000px;
        margin: 0 auto;
        padding-bottom: 4rem;
    }

    .header-section {
        margin-bottom: 3rem;
        text-align: center;
    }

    .form-layout {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 2rem;
    }

    @media (max-width: 768px) {
        .form-layout {
            grid-template-columns: 1fr;
        }
    }

    .section-title {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 1.5rem;
        padding-bottom: 1rem;
        border-bottom: 1px solid var(--border-color);
    }

    .step-number {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: var(--primary);
        color: white;
        font-size: 1rem;
        font-weight: 700;
    }

    .grid-2 {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
    }

    .grid-4 {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 1rem;
    }

    @media (max-width: 640px) {
        .grid-4 {
            grid-template-columns: 1fr 1fr;
        }
    }

    .submit-section {
        grid-column: 1 / -1;
        display: flex;
        justify-content: flex-end;
        margin-top: 1rem;
    }

    .btn-large {
        padding: 1rem 3rem;
        font-size: 1.125rem;
    }

    .tracking-card {
        background: var(--surface-color);
        border: 2px dashed var(--border-color);
        border-radius: var(--radius-md);
        padding: 1.5rem;
        margin-top: 1rem;
        display: inline-block;
    }

    .tracking-number {
        font-size: 2rem;
        font-weight: 800;
        letter-spacing: 2px;
        color: var(--primary);
        font-family: monospace;
    }
</style>