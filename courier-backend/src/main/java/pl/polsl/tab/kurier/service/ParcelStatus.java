package pl.polsl.tab.kurier.service;

/**
 * Canonical parcel status names used throughout the system.
 * These must match the values seeded in data.sql.
 *
 * Lifecycle:
 *   REGISTERED → PENDING_PICKUP → IN_TRANSIT → AT_HUB → OUT_FOR_DELIVERY → DELIVERED
 *                                                     ↘ LOST / DAMAGED / UNDELIVERED
 */
public final class ParcelStatus {

    private ParcelStatus() {}

    /** Paczka przyjęta do systemu, czeka na weryfikację przez Workera. */
    public static final String REGISTERED = "REGISTERED";

    /** Zweryfikowana przez Workera, czeka aż kurier ją odbierze z regionu nadawcy. */
    public static final String PENDING_PICKUP = "PENDING_PICKUP";

    /** Kurier odebrał paczkę i jedzie do następnego hub-u (next_region). */
    public static final String IN_TRANSIT = "IN_TRANSIT";

    /** Paczka dotarła do pośredniego hub-u, czeka na dalszy transport. */
    public static final String AT_HUB = "AT_HUB";

    /** Paczka jest w regionie docelowym, kurier jedzie do odbiorcy. */
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";

    /** Dostarczona pomyślnie. Terminal status. */
    public static final String DELIVERED = "DELIVERED";

    /** Próba dostarczenia nieudana (np. nikogo nie było). */
    public static final String UNDELIVERED = "UNDELIVERED";

    /** Paczka zgłoszona jako zagubiona. Terminal status. */
    public static final String LOST = "LOST";

    /** Paczka zgłoszona jako zniszczona. Terminal status. */
    public static final String DAMAGED = "DAMAGED";
}
