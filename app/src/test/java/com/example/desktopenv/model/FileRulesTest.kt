package com.example.desktopenv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileRulesTest {

    private fun file(name: String, mime: String? = null, dir: Boolean = false) =
        FileEntry(uri = "u/$name", name = name, isDirectory = dir, sizeBytes = 0L, lastModified = 0L, mimeType = mime)

    @Test
    fun valid_names_are_accepted() {
        assertTrue(FileRules.isValidName("relatório final.pdf"))
        assertTrue(FileRules.isValidName("  nova pasta  "))
    }

    @Test
    fun invalid_names_are_rejected() {
        assertFalse(FileRules.isValidName(""))
        assertFalse(FileRules.isValidName("   "))
        assertFalse(FileRules.isValidName("."))
        assertFalse(FileRules.isValidName(".."))
        assertFalse(FileRules.isValidName("a/b"))
        assertFalse(FileRules.isValidName("a\\b"))
        assertFalse(FileRules.isValidName("tab\there"))
        assertFalse(FileRules.isValidName("x".repeat(201)))
    }

    @Test
    fun folders_come_first_then_names_ignoring_case() {
        val sorted = FileRules.sortForDisplay(
            listOf(
                file("beta.txt"),
                file("Zeta", dir = true),
                file("alfa.txt"),
                file("apps", dir = true),
            ),
        )
        assertEquals(listOf("apps", "Zeta", "alfa.txt", "beta.txt"), sorted.map { it.name })
    }

    @Test
    fun kind_comes_from_mime_type_then_extension() {
        assertEquals(FileKind.FOLDER, FileRules.kindOf(file("pasta", dir = true)))
        assertEquals(FileKind.IMAGE, FileRules.kindOf(file("foto", mime = "image/jpeg")))
        assertEquals(FileKind.TEXT, FileRules.kindOf(file("nota.md")))
        assertEquals(FileKind.IMAGE, FileRules.kindOf(file("foto.PNG")))
        assertEquals(FileKind.OTHER, FileRules.kindOf(file("planilha.xlsx", mime = "application/vnd.ms-excel")))
    }
}
