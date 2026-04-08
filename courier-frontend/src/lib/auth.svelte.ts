type Role = "courier" | "admin" | "office";
class AuthState {
    isLoggedIn = $state(false);
    user = $state("");
    role = $state<Role | "">();

    login(username: string, role: Role) {
        this.isLoggedIn = true;
        this.user = username;
        this.role = role;
    }

    logout() {
        this.isLoggedIn = false;
        this.user = "";
        this.role = "";
    }
}

export const auth = new AuthState();