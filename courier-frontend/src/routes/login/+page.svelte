
<script lang="ts">
	import {auth}  from "../../lib/auth.svelte";
	import { goto } from "$app/navigation";

	let user = $state("");
	let pass = $state("");
	const VALID_USERS = {
        admin: { pass: "1234", role: "admin" },
        courier: { pass: "1234", role: "courier" },
        office: { pass: "1234", role: "office" }
    } as const;
	function signin(e: SubmitEvent) {
		e.preventDefault();
		//irl this will be done via API call. we could fetch the region here too
		const account = VALID_USERS[user.toLowerCase() as keyof typeof VALID_USERS];
		console.log("Attempting login with", account);
		if (account && account.pass === pass) {
            auth.login(user, account.role);
			goto("/employeeView");
        } else {
            alert("Access denied");
        }
	}
</script>

<div style="padding: 2rem; font-family: sans-serif; max-width: 300px; margin: 0 auto;">
		<h2>Sign In</h2>
		<form onsubmit={signin} style="display: flex; flex-direction: column; gap: 0.5rem;">
			<input bind:value={user} placeholder="Username" style="padding: 0.5rem;" />
			<input type="password" bind:value={pass} placeholder="Password" style="padding: 0.5rem;" />
			<button type="submit" style="padding: 0.5rem;">Login</button>
		</form>
</div>
