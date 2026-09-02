package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class FileManager(private val context: Context) {

    private fun getProjectDir(projectId: Int): File {
        val dir = File(context.filesDir, "projects/project_$projectId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listFiles(projectId: Int): List<File> {
        val dir = getProjectDir(projectId)
        return dir.listFiles()?.toList() ?: emptyList()
    }

    fun readFile(file: File): String {
        return if (file.exists()) file.readText() else ""
    }

    fun writeFile(projectId: Int, fileName: String, content: String) {
        val file = File(getProjectDir(projectId), fileName)
        file.writeText(content)
    }

    fun deleteFile(projectId: Int, fileName: String) {
        val file = File(getProjectDir(projectId), fileName)
        if (file.exists()) {
            file.delete()
        }
    }
    
    fun importFileFromUri(projectId: Int, uri: Uri, fileName: String) {
        try {
            val destFile = File(getProjectDir(projectId), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun applyTemplate(projectId: Int, template: String) {
        when (template) {
            "Bootstrap" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n</head>\n<body>\n  <div class=\"container text-center\">\n    <h1 class=\"mt-5 text-primary\">Hello Bootstrap</h1>\n    <button class=\"btn btn-success\">Click Me</button>\n  </div>\n  <script src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "style.css", "/* Custom CSS */")
                writeFile(projectId, "main.js", "// JS here")
            }
            "Tailwind" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <script src=\"https://cdn.tailwindcss.com\"></script>\n</head>\n<body class=\"bg-slate-900 text-white flex items-center justify-center h-screen\">\n  <div class=\"text-center\">\n    <h1 class=\"text-4xl font-bold text-blue-400\">Hello Tailwind</h1>\n    <button class=\"mt-4 px-4 py-2 bg-blue-600 rounded-lg hover:bg-blue-500\">Click Me</button>\n  </div>\n</body>\n</html>")
                writeFile(projectId, "style.css", "/* Custom CSS */")
                writeFile(projectId, "main.js", "// JS here")
            }
            "Three.js" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <style>body { margin: 0; }</style>\n</head>\n<body>\n  <script type=\"module\" src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "main.js", "import * as THREE from 'https://unpkg.com/three/build/three.module.js';\n\nconst scene = new THREE.Scene();\nconst camera = new THREE.PerspectiveCamera( 75, window.innerWidth / window.innerHeight, 0.1, 1000 );\nconst renderer = new THREE.WebGLRenderer();\nrenderer.setSize( window.innerWidth, window.innerHeight );\ndocument.body.appendChild( renderer.domElement );\n\nconst geometry = new THREE.BoxGeometry();\nconst material = new THREE.MeshBasicMaterial( { color: 0x00ff00 } );\nconst cube = new THREE.Mesh( geometry, material );\nscene.add( cube );\n\ncamera.position.z = 5;\n\nfunction animate() {\n\trequestAnimationFrame( animate );\n\tcube.rotation.x += 0.01;\n\tcube.rotation.y += 0.01;\n\trenderer.render( scene, camera );\n}\nanimate();")
            }
            else -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Hello WebCode</h1>\n  <script src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "style.css", "body {\n  background: #1E1E2E;\n  color: white;\n  text-align: center;\n  font-family: sans-serif;\n}")
                writeFile(projectId, "main.js", "console.log('Hello from JS');")
            }
        }
    }
    
    // Kept for backward compatibility when loading old projects
    fun createDefaultFilesIfNeeded(projectId: Int, html: String, css: String, js: String) {
        val dir = getProjectDir(projectId)
        if (dir.listFiles()?.isEmpty() != false) {
            writeFile(projectId, "index.html", html.ifEmpty { "<!DOCTYPE html>\n<html>\n<head>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Hello WebCode</h1>\n  <script src=\"main.js\"></script>\n</body>\n</html>" })
            writeFile(projectId, "style.css", css)
            writeFile(projectId, "main.js", js)
        }
    }
    
    fun getProjectUrl(projectId: Int): String {
        return "https://appassets.androidplatform.net/projects/project_$projectId/index.html"
    }
}
