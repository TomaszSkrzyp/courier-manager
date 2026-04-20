<script lang="ts">
    import { auth } from "../../lib/auth.svelte";
    import { goto } from "$app/navigation";
    import { onMount } from "svelte"; // Dodaj to!

    $effect(() => {
        if (!auth.isLoggedIn) goto("/login");
    });

    type Status = "None" | "Collected" | "Delivered" | "Damaged" | "Lost";

    interface Package {
        id: number;         
        trackingNumber: string;
        city: string;
        status: Status;
        verified: boolean;
    }

    const STATUS_ORDER: Status[] = ["None", "Collected", "Delivered", "Damaged", "Lost"];
    
    let packages = $state<Package[]>([]);

    async function fetchParcels() {
        try {
            const res = await fetch("http://localhost:8080/api/parcels");
            if (res.ok) {
                packages = await res.json();
            }
        } catch (err) {
            console.error("Błąd połączenia:", err);
        }
    }

    onMount(() => {
        fetchParcels();
    });

    const isStaff = $derived(auth.role === "admin" || auth.role === "office");

    function canChange(currentStatus: Status, targetStatus: Status): boolean {
        if (isStaff) return true; 
        const currentIndex = STATUS_ORDER.indexOf(currentStatus);
        const targetIndex = STATUS_ORDER.indexOf(targetStatus);
        return targetIndex === currentIndex + 1;
    }

    function toggleStatus(pkg: Package, newStatus: Status) {
        if (canChange(pkg.status, newStatus)) {
            pkg.status = newStatus;
        }
    }
</script>
<div>
    <header>
        <h1>Employee View</h1>
        <p>Logged as: <strong>{auth.user}</strong> ({auth.role})</p>
    </header>
    <button 
            onclick={() => { auth.logout(); goto("/"); }}
            style="position: absolute; top: 20px; right: 20px; padding: 10px; cursor: pointer;"
        >
            Logout ({auth.user})
    </button>

    <hr />

    {#each packages as pkg}
        <div style="margin-bottom: 2rem; border: 1px solid black; padding: 1rem;">
            <div>
                <strong>ID: {pkg.trackingNumber}</strong> | City: {pkg.city}
                <label style="margin-left: 20px;">
                    <input 
                        type="checkbox" 
                        bind:checked={pkg.verified} 
                        disabled={!isStaff} 
                    />
                    Verified
                </label>
            </div>

            <p>Status flow:</p>
            <div style="display: flex; gap: 10px;">
                {#each STATUS_ORDER as s}
                    <label style="border: 1px solid gray; padding: 5px; opacity: {(!canChange(pkg.status, s) && pkg.status !== s) ? '0.3' : '1'}">
                        <input 
                            type="checkbox" 
                            checked={pkg.status === s} 
                            disabled={!canChange(pkg.status, s) && pkg.status !== s}
                            onchange={() => toggleStatus(pkg, s)}
                        />
                        {s}
                    </label>
                {/each}
            </div>
        </div>
    {/each}
</div>