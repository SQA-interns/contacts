package si.contactgroups.security;

import java.util.UUID;

/**
 * The caller of a request: the User found or created for the verified token (ADR-04) and the
 * verified flag of that token. It carries no Role: the Role is read from the database on every
 * request (SR-03).
 */
public record AuthenticatedUser(UUID userId, String email, boolean emailVerified) {}
