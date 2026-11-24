package com.example.desktopenv.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.desktopenv.model.FileEntry
import com.example.desktopenv.model.FileRules
import java.io.ByteArrayOutputStream

/**
 * Acesso a arquivos pelo Storage Access Framework.
 *
 * O app só enxerga as pastas que o usuário escolheu com o seletor do sistema
 * (ACTION_OPEN_DOCUMENT_TREE). Não há acesso amplo ao armazenamento e não é pedida
 * a permissão MANAGE_EXTERNAL_STORAGE. Todas as operações falham de forma explícita
 * (Result.failure) em vez de fingir sucesso.
 */
class FileRepository(private val context: Context) {

    private val resolver = context.contentResolver

    /** Mantém o acesso à pasta escolhida depois que o app for reiniciado. */
    fun takeAccess(treeUri: String): Result<Unit> = runCatching {
        resolver.takePersistableUriPermission(Uri.parse(treeUri), GRANT_FLAGS)
    }

    fun releaseAccess(treeUri: String) {
        runCatching { resolver.releasePersistableUriPermission(Uri.parse(treeUri), GRANT_FLAGS) }
    }

    /** Nome exibido da pasta raiz autorizada, ou o próprio URI se o provedor não informar. */
    fun rootName(treeUri: String): String =
        runCatching { DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.name }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: treeUri

    fun list(documentUri: String): Result<List<FileEntry>> = runCatching {
        val dir = docFile(documentUri)
        check(dir.isDirectory) { "não é uma pasta" }
        dir.listFiles().map { it.toEntry() }
    }

    fun rename(documentUri: String, newName: String): Result<Unit> = runCatching {
        require(FileRules.isValidName(newName)) { "nome inválido" }
        check(docFile(documentUri).renameTo(newName.trim())) { "não foi possível renomear" }
    }

    fun delete(documentUri: String): Result<Unit> = runCatching {
        check(docFile(documentUri).delete()) { "não foi possível excluir" }
    }

    fun createFolder(parentUri: String, name: String): Result<Unit> = runCatching {
        require(FileRules.isValidName(name)) { "nome inválido" }
        val parent = docFile(parentUri)
        check(parent.isDirectory) { "destino não é uma pasta" }
        check(parent.createDirectory(name.trim()) != null) { "não foi possível criar a pasta" }
    }

    /** Copia um arquivo ou pasta para dentro de [targetDirUri]. Pastas são copiadas recursivamente. */
    fun copy(sourceUri: String, targetDirUri: String): Result<Unit> = runCatching {
        check(!isSameOrInside(targetDirUri, sourceUri)) { "não é possível copiar uma pasta para dentro dela mesma" }
        val target = docFile(targetDirUri)
        check(target.isDirectory) { "destino não é uma pasta" }
        copyInto(docFile(sourceUri), target, depth = 0)
    }

    /** Copia e, só depois de copiar com sucesso, remove a origem. Se a cópia falhar, a origem é preservada. */
    fun move(sourceUri: String, targetDirUri: String): Result<Unit> {
        copy(sourceUri, targetDirUri).onFailure { return Result.failure(it) }
        return runCatching {
            check(docFile(sourceUri).delete()) { "copiado, mas não foi possível remover a origem" }
        }
    }

    /** Decodifica uma imagem reduzida para caber em [maxSide] pixels, para não estourar a memória. */
    fun loadImage(documentUri: String, maxSide: Int = 1024): Result<Bitmap> = runCatching {
        val uri = Uri.parse(documentUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "imagem inválida" }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        checkNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }) {
            "não foi possível decodificar a imagem"
        }
    }

    /** Lê até [maxBytes] de um arquivo de texto. Arquivos maiores aparecem truncados na visualização. */
    fun readText(documentUri: String, maxBytes: Int = MAX_TEXT_PREVIEW_BYTES): Result<String> = runCatching {
        resolver.openInputStream(Uri.parse(documentUri))?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (out.size() < maxBytes) {
                val read = input.read(buffer, 0, minOf(buffer.size, maxBytes - out.size()))
                if (read < 0) break
                out.write(buffer, 0, read)
            }
            String(out.toByteArray(), Charsets.UTF_8)
        } ?: error("não foi possível ler o arquivo")
    }

    /** Abre o arquivo com um app do sistema. Retorna false se nenhum app estiver disponível. */
    fun openExternal(entry: FileEntry): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(entry.uri), entry.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun copyInto(source: DocumentFile, targetDir: DocumentFile, depth: Int) {
        check(depth <= MAX_DEPTH) { "pasta muito profunda" }
        if (source.isDirectory) {
            // Lista os filhos antes de criar a pasta de destino, para não copiar o que acabou de ser criado.
            val children = source.listFiles().toList()
            val newDir = targetDir.createDirectory(source.name.orEmpty())
                ?: error("não foi possível criar a pasta ${source.name}")
            children.forEach { copyInto(it, newDir, depth + 1) }
        } else {
            val mime = source.type ?: "application/octet-stream"
            val newFile = targetDir.createFile(mime, source.name.orEmpty())
                ?: error("não foi possível criar ${source.name}")
            val input = resolver.openInputStream(source.uri) ?: error("não foi possível ler ${source.name}")
            input.use { inp ->
                val output = resolver.openOutputStream(newFile.uri) ?: error("não foi possível gravar ${source.name}")
                output.use { out -> inp.copyTo(out) }
            }
        }
    }

    /**
     * Guarda contra copiar uma pasta para dentro de si mesma. Compara os ids de documento,
     * que são caminhos nos provedores de sistema de arquivos. Em provedores com ids opacos
     * a comparação não detecta o caso, e [MAX_DEPTH] impede a recursão infinita.
     */
    private fun isSameOrInside(targetUri: String, sourceUri: String): Boolean {
        val target = documentId(targetUri) ?: return false
        val source = documentId(sourceUri) ?: return false
        return target == source || target.startsWith("$source/")
    }

    private fun documentId(uri: String): String? =
        runCatching { DocumentsContract.getDocumentId(Uri.parse(uri)) }.getOrNull()

    private fun docFile(uri: String): DocumentFile =
        DocumentFile.fromTreeUri(context, Uri.parse(uri)) ?: error("URI de arquivo inválido")

    private fun DocumentFile.toEntry(): FileEntry = FileEntry(
        uri = uri.toString(),
        name = name.orEmpty(),
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        lastModified = lastModified(),
        mimeType = type,
    )

    companion object {
        private const val MAX_DEPTH = 64
        const val MAX_TEXT_PREVIEW_BYTES = 64 * 1024
        private const val GRANT_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
