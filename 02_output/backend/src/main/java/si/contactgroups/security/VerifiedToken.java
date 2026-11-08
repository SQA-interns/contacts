package si.contactgroups.security;

/**
 * The claims the backend reads from a verified Firebase ID token (firebase-id-token.schema.json).
 */
public record VerifiedToken(String uid, String email, boolean emailVerified, String name) {}
