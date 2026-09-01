package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.ProjectEntity
import java.io.OutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

object ZipHelper {

    fun exportProjectToZip(context: Context, project: ProjectEntity, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zos ->
                addEntry(zos, "index.html", project.htmlContent)
                addEntry(zos, "style.css", project.cssContent)
                addEntry(zos, "script.js", project.jsContent)
            }
        }
    }

    private fun addEntry(zos: ZipOutputStream, fileName: String, content: String) {
        val entry = ZipEntry(fileName)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    fun importProjectFromZip(context: Context, uri: Uri, projectName: String): ProjectEntity? {
        var html = ""
        var css = ""
        var js = ""

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val content = zis.bufferedReader().readText()
                    when (entry.name) {
                        "index.html" -> html = content
                        "style.css" -> css = content
                        "script.js" -> js = content
                    }
                    entry = zis.nextEntry
                }
            }
        } ?: return null

        return ProjectEntity(
            name = projectName,
            htmlContent = html,
            cssContent = css,
            jsContent = js
        )
    }
}
