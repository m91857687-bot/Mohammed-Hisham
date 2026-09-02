sed -i 's/\\${winner}/${'\''$'\''}{winner}/g' app/src/main/java/com/example/data/FileManager.kt
sed -i 's/\\${xIsNext ? '\''X'\'' : '\''O'\''}/${'\''$'\''}{xIsNext ? '\''X'\'' : '\''O'\''}/g' app/src/main/java/com/example/data/FileManager.kt
