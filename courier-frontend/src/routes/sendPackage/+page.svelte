<script lang="ts">
    import { auth } from "../../lib/auth.svelte";
    import { goto } from "$app/navigation";



    interface NewPackage {
        recipientName: string;
        city: string;
        address: string;
        weight: number;
        notes: string;
    }

    let form = $state<NewPackage>({
        recipientName: "",
        city: "",
        address: "",
        weight: 0,
        notes: ""
    });

    let submitted = $state(false);
    let generatedId = $state("");

    function generateId(): string {
        return Math.floor(Math.random() * 900000000 + 100000000).toString();
    }

    function handleSubmit() {
        generatedId = generateId();
        submitted = true;
        // tutaj możesz dodać wywołanie API
    }

    function reset() {
        submitted = false;
        generatedId = "";
        form = { recipientName: "", city: "", address: "", weight: 0, notes: "" };
    }
</script>

<div>
    <header>
        <h1>Nadawanie paczki</h1>
    </header>


    <hr />

    {#if submitted}
        <div style="border: 2px solid green; padding: 1rem;">
            <h2>✅ Paczka nadana!</h2>
            <p>ID paczki: <strong>{generatedId}</strong></p>
            <p>Odbiorca: {form.recipientName}, {form.city}</p>
            <button onclick={reset}>Nadaj kolejną</button>
        </div>
    {:else}
        <div style="max-width: 400px;">
            <label>
                Imię i nazwisko odbiorcy:<br />
                <input bind:value={form.recipientName} type="text" />
            </label><br /><br />

            <label>
                Miasto:<br />
                <input bind:value={form.city} type="text" />
            </label><br /><br />

            <label>
                Adres:<br />
                <input bind:value={form.address} type="text" />
            </label><br /><br />

            <label>
                Waga (kg):<br />
                <input bind:value={form.weight} type="number" min="0" step="0.1" />
            </label><br /><br />

            <label>
                Uwagi:<br />
                <textarea bind:value={form.notes}></textarea>
            </label><br /><br />

            <button onclick={handleSubmit}>Nadaj paczkę</button>
        </div>
    {/if}
</div>