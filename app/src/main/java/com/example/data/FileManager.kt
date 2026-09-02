package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File

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

    fun renameFile(projectId: Int, oldName: String, newName: String) {
        val oldFile = File(getProjectDir(projectId), oldName)
        val newFile = File(getProjectDir(projectId), newName)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
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
            "TicTacToe" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Tic-Tac-Toe</h1>\n  <div id=\"status\">Player X's turn</div>\n  <div class=\"board\" id=\"board\"></div>\n  <button id=\"reset\">Reset Game</button>\n  <script src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "style.css", "body {\n  font-family: sans-serif;\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  background-color: #121212;\n  color: #fff;\n  margin-top: 50px;\n}\n.board {\n  display: grid;\n  grid-template-columns: repeat(3, 100px);\n  gap: 5px;\n  margin: 20px 0;\n}\n.cell {\n  width: 100px;\n  height: 100px;\n  background-color: #1e1e1e;\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  font-size: 3em;\n  cursor: pointer;\n  border-radius: 8px;\n  user-select: none;\n}\n.cell.taken {\n  cursor: default;\n}\nbutton {\n  padding: 10px 20px;\n  font-size: 1.2em;\n  background-color: #6200ea;\n  color: white;\n  border: none;\n  border-radius: 5px;\n  cursor: pointer;\n}\nbutton:hover {\n  background-color: #3700b3;\n}")
                writeFile(projectId, "main.js", "const board = document.getElementById('board');\nconst status = document.getElementById('status');\nconst resetBtn = document.getElementById('reset');\nlet cells = Array(9).fill(null);\nlet xIsNext = true;\n\nfunction checkWinner() {\n  const lines = [\n    [0, 1, 2], [3, 4, 5], [6, 7, 8],\n    [0, 3, 6], [1, 4, 7], [2, 5, 8],\n    [0, 4, 8], [2, 4, 6]\n  ];\n  for (let i = 0; i < lines.length; i++) {\n    const [a, b, c] = lines[i];\n    if (cells[a] && cells[a] === cells[b] && cells[a] === cells[c]) {\n      return cells[a];\n    }\n  }\n  return null;\n}\n\nfunction render() {\n  board.innerHTML = '';\n  cells.forEach((val, index) => {\n    const cell = document.createElement('div');\n    cell.className = 'cell ' + (val ? 'taken' : '');\n    cell.innerText = val || '';\n    cell.addEventListener('click', () => handleMove(index));\n    board.appendChild(cell);\n  });\n  const winner = checkWinner();\n  if (winner) {\n    status.innerText = `Winner: ${'$'}winner`;\n  } else if (!cells.includes(null)) {\n    status.innerText = 'Draw!';\n  } else {\n    status.innerText = `Player ${'$'}{xIsNext ? 'X' : 'O'}'s turn`;\n  }\n}\n\nfunction handleMove(index) {\n  if (cells[index] || checkWinner()) return;\n  cells[index] = xIsNext ? 'X' : 'O';\n  xIsNext = !xIsNext;\n  render();\n}\n\nresetBtn.addEventListener('click', () => {\n  cells = Array(9).fill(null);\n  xIsNext = true;\n  render();\n});\n\nrender();")
            }
            "Calculator" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <div class=\"calculator\">\n    <div class=\"display\" id=\"display\">0</div>\n    <div class=\"buttons\">\n      <button class=\"btn\" onclick=\"clearDisplay()\">C</button>\n      <button class=\"btn\" onclick=\"append('/')\">/</button>\n      <button class=\"btn\" onclick=\"append('*')\">*</button>\n      <button class=\"btn\" onclick=\"append('-')\">-</button>\n      <button class=\"btn\" onclick=\"append('7')\">7</button>\n      <button class=\"btn\" onclick=\"append('8')\">8</button>\n      <button class=\"btn\" onclick=\"append('9')\">9</button>\n      <button class=\"btn\" onclick=\"append('+')\">+</button>\n      <button class=\"btn\" onclick=\"append('4')\">4</button>\n      <button class=\"btn\" onclick=\"append('5')\">5</button>\n      <button class=\"btn\" onclick=\"append('6')\">6</button>\n      <button class=\"btn\" onclick=\"calculate()\" style=\"grid-row: span 2; background-color: #6200ea;\">=</button>\n      <button class=\"btn\" onclick=\"append('1')\">1</button>\n      <button class=\"btn\" onclick=\"append('2')\">2</button>\n      <button class=\"btn\" onclick=\"append('3')\">3</button>\n      <button class=\"btn\" onclick=\"append('0')\" style=\"grid-column: span 2;\">0</button>\n      <button class=\"btn\" onclick=\"append('.')\">.</button>\n    </div>\n  </div>\n  <script src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "style.css", "body {\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  height: 100vh;\n  background-color: #050505;\n  font-family: sans-serif;\n  margin: 0;\n}\n.calculator {\n  background-color: #1e1e1e;\n  border-radius: 10px;\n  padding: 20px;\n  box-shadow: 0 4px 10px rgba(0,0,0,0.5);\n  width: 250px;\n}\n.display {\n  background-color: #000;\n  color: #fff;\n  font-size: 2em;\n  text-align: right;\n  padding: 10px;\n  border-radius: 5px;\n  margin-bottom: 20px;\n  min-height: 40px;\n  overflow-x: auto;\n}\n.buttons {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 10px;\n}\n.btn {\n  background-color: #333;\n  color: #fff;\n  border: none;\n  padding: 15px;\n  font-size: 1.2em;\n  border-radius: 5px;\n  cursor: pointer;\n}\n.btn:hover {\n  background-color: #444;\n}")
                writeFile(projectId, "main.js", "const display = document.getElementById('display');\n\nfunction append(value) {\n  if (display.innerText === '0' && value !== '.') {\n    display.innerText = value;\n  } else {\n    display.innerText += value;\n  }\n}\n\nfunction clearDisplay() {\n  display.innerText = '0';\n}\n\nfunction calculate() {\n  try {\n    display.innerText = eval(display.innerText);\n  } catch (e) {\n    display.innerText = 'Error';\n  }\n}")
            }
            "LandingPage" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>Landing Page</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <header class=\"header\">\n    <div class=\"logo\">MyBrand</div>\n    <nav class=\"nav\">\n      <a href=\"#\">Home</a>\n      <a href=\"#\">Features</a>\n      <a href=\"#\">Contact</a>\n    </nav>\n  </header>\n  <section class=\"hero\">\n    <h1>Welcome to the Future</h1>\n    <p>Build amazing products with our new platform.</p>\n    <button class=\"cta-btn\">Get Started</button>\n  </section>\n  <section class=\"features\">\n    <div class=\"feature-card\">\n      <h3>Fast</h3>\n      <p>Lightning fast performance.</p>\n    </div>\n    <div class=\"feature-card\">\n      <h3>Secure</h3>\n      <p>Your data is safe with us.</p>\n    </div>\n    <div class=\"feature-card\">\n      <h3>Reliable</h3>\n      <p>99.9% uptime guaranteed.</p>\n    </div>\n  </section>\n</body>\n</html>")
                writeFile(projectId, "style.css", "body {\n  margin: 0;\n  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n  background-color: #050505;\n  color: #fff;\n}\n.header {\n  display: flex;\n  justify-content: space-between;\n  padding: 20px 40px;\n  background-color: #111;\n  align-items: center;\n}\n.logo {\n  font-size: 1.5rem;\n  font-weight: bold;\n  color: #CBA6F7;\n}\n.nav a {\n  color: #fff;\n  text-decoration: none;\n  margin-left: 20px;\n}\n.nav a:hover {\n  color: #CBA6F7;\n}\n.hero {\n  text-align: center;\n  padding: 100px 20px;\n  background: linear-gradient(135deg, #2D243F, #050505);\n}\n.hero h1 {\n  font-size: 3rem;\n  margin-bottom: 10px;\n}\n.cta-btn {\n  padding: 15px 30px;\n  font-size: 1.2rem;\n  background-color: #CBA6F7;\n  color: #000;\n  border: none;\n  border-radius: 30px;\n  cursor: pointer;\n  margin-top: 20px;\n  font-weight: bold;\n}\n.cta-btn:hover {\n  background-color: #EADDFF;\n}\n.features {\n  display: flex;\n  justify-content: space-around;\n  padding: 50px 20px;\n  flex-wrap: wrap;\n}\n.feature-card {\n  background-color: #111;\n  padding: 30px;\n  border-radius: 10px;\n  width: 25%;\n  text-align: center;\n  margin-bottom: 20px;\n  border: 1px solid #333;\n}")
                writeFile(projectId, "main.js", "console.log('Landing page loaded');")
            }
            "TodoList" -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <div class=\"todo-container\">\n    <h2>My Tasks</h2>\n    <div class=\"input-group\">\n      <input type=\"text\" id=\"taskInput\" placeholder=\"Add a new task...\">\n      <button id=\"addTaskBtn\">Add</button>\n    </div>\n    <ul id=\"taskList\"></ul>\n  </div>\n  <script src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "style.css", "body {\n  display: flex;\n  justify-content: center;\n  padding-top: 50px;\n  background-color: #050505;\n  color: #fff;\n  font-family: sans-serif;\n  margin: 0;\n}\n.todo-container {\n  background-color: #151515;\n  padding: 30px;\n  border-radius: 12px;\n  width: 100%;\n  max-width: 400px;\n  box-shadow: 0 4px 15px rgba(0,0,0,0.5);\n}\n.input-group {\n  display: flex;\n  margin-bottom: 20px;\n}\ninput {\n  flex: 1;\n  padding: 10px;\n  border: 1px solid #333;\n  background-color: #0F0F0F;\n  color: white;\n  border-radius: 5px 0 0 5px;\n  outline: none;\n}\nbutton {\n  padding: 10px 20px;\n  background-color: #CBA6F7;\n  color: #000;\n  border: none;\n  border-radius: 0 5px 5px 0;\n  cursor: pointer;\n  font-weight: bold;\n}\nbutton:hover {\n  background-color: #EADDFF;\n}\nul {\n  list-style: none;\n  padding: 0;\n  margin: 0;\n}\nli {\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  background-color: #202020;\n  padding: 12px;\n  margin-bottom: 8px;\n  border-radius: 5px;\n}\nli.completed span {\n  text-decoration: line-through;\n  color: #888;\n}\n.delete-btn {\n  background-color: transparent;\n  color: #ff5252;\n  border-radius: 5px;\n  padding: 5px;\n}")
                writeFile(projectId, "main.js", "const taskInput = document.getElementById('taskInput');\nconst addTaskBtn = document.getElementById('addTaskBtn');\nconst taskList = document.getElementById('taskList');\n\naddTaskBtn.addEventListener('click', () => {\n  const text = taskInput.value.trim();\n  if (text) {\n    addTask(text);\n    taskInput.value = '';\n  }\n});\n\nfunction addTask(text) {\n  const li = document.createElement('li');\n  const span = document.createElement('span');\n  span.innerText = text;\n  span.addEventListener('click', () => li.classList.toggle('completed'));\n  \n  const delBtn = document.createElement('button');\n  delBtn.innerText = 'X';\n  delBtn.className = 'delete-btn';\n  delBtn.addEventListener('click', () => li.remove());\n  \n  li.appendChild(span);\n  li.appendChild(delBtn);\n  taskList.appendChild(li);\n}")
            }
            else -> {
                writeFile(projectId, "index.html", "<!DOCTYPE html>\n<html>\n<head>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Hello WebCode</h1>\n  <script src=\"main.js\"></script>\n</body>\n</html>")
                writeFile(projectId, "style.css", "body {\n  background: #000000;\n  color: #ffffff;\n  text-align: center;\n  font-family: sans-serif;\n}")
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
