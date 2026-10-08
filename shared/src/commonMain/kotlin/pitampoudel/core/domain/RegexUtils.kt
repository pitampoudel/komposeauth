package pitampoudel.core.domain

fun isUrlValid(url: String): Boolean {
    val pattern = "^((https?|ftp)://)?" + // Protocol
            "(([a-zA-Z0-9._-]+\\.[a-zA-Z]{2,})|" + // Domain name
            "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))" + // OR IP address
            "(:[0-9]{1,5})?" + // Port
            "(/\\S*)?$" // Path
    return Regex(pattern).matches(url)
}
/**
 * A deliberately loose check: one @, no spaces, and a dotted domain. Stricter patterns refused real
 * addresses — `name+tag@gmail.com` and any top-level domain longer than four letters.
 */
fun String?.isValidEmail(): Boolean {
    return this?.matches(EMAIL) == true
}

private val EMAIL = Regex("^[^\\s@]+@([^\\s@.]+\\.)+[^\\s@.]{2,}$")