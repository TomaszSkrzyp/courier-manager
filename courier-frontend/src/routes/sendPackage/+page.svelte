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
        { id: 1, name: "Kurier standardowy" },
        { id: 2, name: "Kurier ekspresowy" },
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
        fragility: "nie",
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
        // tutaj dodać wywołanie API
    }

    function reset() {
        submitted = false;
        generatedId = "";
        form = {
            phoneNumber: "",
            senderStreet: "", senderBuildingNumber: "", senderPostalCode: "", senderRegionId: 0,
            recipientStreet: "", recipientBuildingNumber: "", recipientPostalCode: "", recipientRegionId: 0,
            weight: 0, height: 0, width: 0, length: 0,
            fragility: "nie", deliveryModeId: 0, comment: "",
        };
    }

    function goToTracking() {
        goto(`/tracking/${generatedId}`);
    }
</script>

<div style="max-width: 520px; padding: 1rem;">
    <header>
        <h1>Nadawanie paczki</h1>
    </header>

    <hr />

    {#if submitted}
        <div style="border: 2px solid green; padding: 1rem;">
            <h2>Paczka nadana!</h2>
            <p>ID paczki: <strong>{generatedId}</strong></p>
            <p>Nadawca: {form.senderStreet} {form.senderBuildingNumber}, {form.senderPostalCode}</p>
            <p>Odbiorca: {form.recipientStreet} {form.recipientBuildingNumber}, {form.recipientPostalCode}</p>
            <div style="display: flex; gap: 1rem; margin-top: 1rem;">
                <button onclick={reset}>Nadaj kolejną</button>
                <button onclick={goToTracking}>🔍 Śledź paczkę</button>
            </div>
        </div>
    {:else}

        <!-- KONTAKT -->
        <h2>Kontakt</h2>

        <label>
            Numer telefonu:<br />
            <input bind:value={form.phoneNumber} type="tel" placeholder="+48 000 000 000" />
        </label><br /><br />

        <hr />

        <!-- ADRES NADAWCY -->
        <h2>Adres nadawcy</h2>

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 2;">
                Ulica:<br />
                <input bind:value={form.senderStreet} type="text" placeholder="ul. Przykładowa" />
            </label>
            <label style="flex: 1;">
                Nr budynku:<br />
                <input bind:value={form.senderBuildingNumber} type="text" placeholder="1/2" />
            </label>
        </div><br />

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 1;">
                Kod pocztowy:<br />
                <input bind:value={form.senderPostalCode} type="text" placeholder="00-000" maxlength="6" />
            </label>
            <label style="flex: 1;">
                Region:<br />
                <select bind:value={form.senderRegionId}>
                    <option value={0} disabled>Wybierz region</option>
                    {#each regions as r}
                        <option value={r.id}>{r.name}</option>
                    {/each}
                </select>
            </label>
        </div><br />

        <hr />

        <!-- ADRES ODBIORCY -->
        <h2>Adres odbiorcy</h2>

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 2;">
                Ulica:<br />
                <input bind:value={form.recipientStreet} type="text" placeholder="ul. Przykładowa" />
            </label>
            <label style="flex: 1;">
                Nr budynku:<br />
                <input bind:value={form.recipientBuildingNumber} type="text" placeholder="3/4" />
            </label>
        </div><br />

        <div style="display: flex; gap: 1rem;">
            <label style="flex: 1;">
                Kod pocztowy:<br />
                <input bind:value={form.recipientPostalCode} type="text" placeholder="00-000" maxlength="6" />
            </label>
            <label style="flex: 1;">
                Region:<br />
                <select bind:value={form.recipientRegionId}>
                    <option value={0} disabled>Wybierz region</option>
                    {#each regions as r}
                        <option value={r.id}>{r.name}</option>
                    {/each}
                </select>
            </label>
        </div><br />

        <hr />

        <!-- SZCZEGÓŁY PACZKI -->
        <h2>Szczegóły paczki</h2>

        <label>
            Waga (kg):<br />
            <input bind:value={form.weight} type="number" min="0" step="0.1" />
        </label><br /><br />

        <div style="display: flex; gap: 1rem;">
            <label>
                Wysokość (cm):<br />
                <input bind:value={form.height} type="number" min="0" />
            </label>
            <label>
                Szerokość (cm):<br />
                <input bind:value={form.width} type="number" min="0" />
            </label>
            <label>
                Długość (cm):<br />
                <input bind:value={form.length} type="number" min="0" />
            </label>
        </div><br />

        <label>
            Kruchość:<br />
            <select bind:value={form.fragility}>
                <option value="nie">Nie — standardowa paczka</option>
                <option value="tak">Tak — zawartość krucha</option>
            </select>
        </label><br /><br />

        <label>
            Tryb dostawy:<br />
            <select bind:value={form.deliveryModeId}>
                <option value={0} disabled>Wybierz tryb dostawy</option>
                {#each deliveryModes as m}
                    <option value={m.id}>{m.name}</option>
                {/each}
            </select>
        </label><br /><br />

        <label>
            Uwagi:<br />
            <textarea bind:value={form.comment} placeholder="Opcjonalne uwagi do przesyłki..."></textarea>
        </label><br /><br />

        <button onclick={handleSubmit}>Nadaj paczkę</button>
    {/if}
</div>