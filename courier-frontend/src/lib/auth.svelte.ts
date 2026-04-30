type Role = "courier" | "admin" | "office";
class AuthState {
    isLoggedIn = $state(false);
    user = $state("");
    role = $state<Role | "">();
    userId = $state<number | null>(null);

    login(username: string, role: Role, id: number) {
        this.isLoggedIn = true;
        this.user = username;
        this.role = role;
        this.userId = id;
    }

    logout() {
        this.isLoggedIn = false;
        this.user = "";
        this.role = "";
        this.userId = null;
    }
}

export const auth = new AuthState();