package release

data class LibraryInfo(
    val artifactId: String,
    val groupId: String,
    val version: Version,
) {
    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) {
        val name = "${major}.${minor}.${patch}"
    }
}