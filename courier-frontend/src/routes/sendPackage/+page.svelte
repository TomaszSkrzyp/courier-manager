<script lang="ts">
    import { auth } from "../../lib/auth.svelte";
    import { goto } from "$app/navigation";

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

    const deliveryModes = [
        { id: 1, name: "Standard courier" },
        { id: 2, name: "Express courier" },
    ];

    const regions = [
        { id: 1, name: "Mazowieckie" },
        { id: 2, name: "Małopolskie" },
        { id: 3, name: "Wielkopolskie" },
        { id: 4, name: "Dolnośląskie" },
    ];

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

    function generateId(): string {
        return Math.floor(Math.random() * 900000000 + 100000000).toString();
    }

    function handleSubmit() {
        generatedId = generateId();
        submitted = true;
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

    function goToTracking() {
        goto(`/tracking/${generatedId}`);
    }
</script>

<div style="max-width: 520px; padding: 1rem;">
    <header>
        <h1>Send a package</h1>
    </header>

    <hr />

    {#if submitted}
        <div style="border: 2px solid green; padding: 1rem;">
            <h2>Package sent!</h2>
            <p>Package ID: <strong>{generatedId}</strong></p>
            <p>Sender: {form.senderStreet} {form.senderBuildingNumber}, {form.senderPostalCode}</p>
            <p>Recipient: {form.recipientStreet} {form.recipientBuildingNumber}, {form.recipientPostalCode}</p>
            <div style="display: flex; gap: 1rem; margin-top: 1rem;">
                <button onclick={reset}>Send another</button>
                <button onclick={goToTracking}>🔍 Track package</button>
            </div>
        </div>
    {:else}

        <h2>Contact</h2>

        <label>
            Phone number:<br />
            <input bind:value={form.phoneNumber} type="tel" placeholder="+48 000 000 000" />
        </label><br /><br />

        <hr />

        <h2>Sender address</h2>

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 2;">
                Street:<br />
                <input bind:value={form.senderStreet} type="text" placeholder="123 Example St" />
            </label>
            <label style="flex: 1;">
                Building number:<br />
                <input bind:value={form.senderBuildingNumber} type="text" placeholder="1/2" />
            </label>
        </div><br />

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 1;">
                Postal code:<br />
                <input bind:value={form.senderPostalCode} type="text" placeholder="00-000" maxlength="6" />
            </label>
            <label style="flex: 1;">
                Region:<br />
                <select bind:value={form.senderRegionId}>
                    <option value={0} disabled>Select region</option>
                    {#each regions as r}
                        <option value={r.id}>{r.name}</option>
                    {/each}
                </select>
            </label>
        </div><br />

        <hr />

        <h2>Recipient address</h2>

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 2;">
                Street:<br />
                <input bind:value={form.recipientStreet} type="text" placeholder="123 Example St" />
            </label>
            <label style="flex: 1;">
                Building number:<br />
                <input bind:value={form.recipientBuildingNumber} type="text" placeholder="3/4" />
            </label>
        </div><br />

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 1;">
                Postal code:<br />
                <input bind:value={form.recipientPostalCode} type="text" placeholder="00-000" maxlength="6" />
            </label>
            <label style="flex: 1;">
                Region:<br />
                <select bind:value={form.recipientRegionId}>
                    <option value={0} disabled>Select region</option>
                    {#each regions as r}
                        <option value={r.id}>{r.name}</option>
                    {/each}
                </select>
            </label>
        </div><br />

        <hr />

        <h2>Package details</h2>

        <label>
            Weight (kg):<br />
            <input bind:value={form.weight} type="number" min="0" step="0.1" />
        </label><br /><br />

        <div style="display: flex; gap: 1rem;">
            <label>
                Height (cm):<br />
                <input bind:value={form.height} type="number" min="0" />
            </label>
            <label>
                Width (cm):<br />
                <input bind:value={form.width} type="number" min="0" />
            </label>
            <label>
                Length (cm):<br />
                <input bind:value={form.length} type="number" min="0" />
            </label>
        </div><br />

        <label>
            Fragility:<br />
            <select bind:value={form.fragility}>
                <option value="no">No — standard package</option>
                <option value="yes">Yes — fragile contents</option>
            </select>
        </label><br /><br />

        <label>
            Delivery mode:<br />
            <select bind:value={form.deliveryModeId}>
                <option value={0} disabled>Select delivery mode</option>
                {#each deliveryModes as m}
                    <option value={m.id}>{m.name}</option>
                {/each}
            </select>
        </label><br /><br />

        <label>
            Notes:<br />
            <textarea bind:value={form.comment} placeholder="Optional notes for the shipment..."></textarea>
        </label><br /><br />

        <button onclick={handleSubmit}>Send package</button>
    {/if}
</div>