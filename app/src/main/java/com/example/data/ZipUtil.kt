package com.example.data

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtil {
    fun zipDirectory(dir: File, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = dir.toPath().relativize(file.toPath()).toString()
                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        bis.copyTo(zos)
                    }
                }
                zos.closeEntry()
            }
        }
    }
}
