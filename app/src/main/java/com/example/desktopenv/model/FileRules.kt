package com.example.desktopenv.model

/** Entrada de uma pasta autorizada. [uri] é o URI do documento no provedor do Android. */
data class FileEntry(
    val uri: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
)

/** Itens copiados ou recortados, colados na pasta atual de qualquer janela de arquivos. */
data class FileClipboard(val uris: List<String>, val move: Boolean)

/** Tipo de arquivo para decidir como abrir ou pré-visualizar. */
enum class FileKind { FOLDER, IMAGE, TEXT, OTHER }

/** Regras de nomes, ordenação e classificação. Lógica pura, sem Android. */
object FileRules {

    private const val MAX_NAME_LENGTH = 200
    private val TEXT_EXTENSIONS = setOf("txt", "md", "csv", "json", "xml", "log", "kt", "java", "py", "html")

    /** Um nome é válido se não estiver vazio, não tiver separadores nem controles, e não for "." ou "..". */
    fun isValidName(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || n == "." || n == "..") return false
        if (n.length > MAX_NAME_LENGTH) return false
        if (n.contains('/') || n.contains('\\')) return false
        return n.none { it.isISOControl() }
    }

    /** Pastas primeiro, depois arquivos; dentro de cada grupo, por nome sem diferenciar maiúsculas. */
    fun sortForDisplay(entries: List<FileEntry>): List<FileEntry> =
        entries.sortedWith(
            compareBy<FileEntry>({ !it.isDirectory }, { it.name.lowercase() }),
        )

    fun kindOf(entry: FileEntry): FileKind {
        if (entry.isDirectory) return FileKind.FOLDER
        val mime = entry.mimeType.orEmpty()
        if (mime.startsWith("image/")) return FileKind.IMAGE
        if (mime.startsWith("text/")) return FileKind.TEXT
        val ext = entry.name.substringAfterLast('.', "").lowercase()
        return when {
            ext in TEXT_EXTENSIONS -> FileKind.TEXT
            ext in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp") -> FileKind.IMAGE
            else -> FileKind.OTHER
        }
    }
}
