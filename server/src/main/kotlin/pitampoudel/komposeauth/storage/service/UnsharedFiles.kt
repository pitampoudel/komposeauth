package pitampoudel.komposeauth.storage.service

/**
 * Deletes the files at [addresses], which rows about to be deleted name, except a file a kept row
 * also names ([kept]): an old KYC record and its owner's new one name the same file, and so did two
 * organizations saved in the same second while logos were named by the time. They are compared by
 * name in the bucket, since one file can have had more than one address.
 *
 * Answers the addresses left behind: in no listed bucket, or not deleted. Never throws, so a bucket
 * that can't be reached costs a log line rather than the server's start.
 */
fun StorageService.deleteUnsharedFiles(addresses: Collection<String>, kept: Collection<String>): List<String> {
    val all = addresses.distinct()
    val keptNames = runCatching { kept.mapNotNull { blobNameOf(it) }.toSet() }.getOrElse { return all }
    return all.filter { address ->
        val name = runCatching { blobNameOf(address) }.getOrNull() ?: return@filter true
        name !in keptNames && runCatching { !delete(address) }.getOrDefault(true)
    }
}
