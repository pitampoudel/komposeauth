package pitampoudel.komposeauth.core.domain

/**
 * What a refusal asks the client to do, sent as the [PROPERTY] of the problem body beside its
 * human-readable `detail`, for the refusals a client is expected to act on rather than only show.
 */
enum class ErrorCode {
    /** Repeat the request with the account's current password, as `currentPassword`. */
    CURRENT_PASSWORD_REQUIRED,

    /** Sign in again (`prompt=login` for a client of the authorization server), then repeat the request. */
    RECENT_SIGN_IN_REQUIRED;

    companion object {
        const val PROPERTY = "code"
    }
}
