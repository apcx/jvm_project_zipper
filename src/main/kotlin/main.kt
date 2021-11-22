import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun main(args: Array<String>) {
    println(args.toList())
    if (args.isNotEmpty()) zip(args[0], args.size >= 2)
}

fun zip(path: String, include_git: Boolean = false) {
    val root = File(path)
    val directories = mutableListOf<File>()
    ZipOutputStream(File(root.parent, "${root.name}.zip").outputStream().buffered()).use { zip ->
        root.walk().onEnter {
            val add = when (it.name) {
                "build" -> false
                ".git" -> include_git
                else -> !it.ignored()
            }
            if (add) directories += it
            add
        }.onLeave { directories -= it }.forEach { file ->
            if (file.isFile && !file.ignored()) {
                if (file.parentFile in directories) zip.addDirectories(directories, root.parentFile, file.parentFile)
                val name = file.toRelativeString(root.parentFile)
                println("zip $name")
                val entry = ZipEntry(name)
                entry.time = file.lastModified()
                entry.size = file.length()
                zip.putNextEntry(entry)
                file.inputStream().buffered().use { it.copyTo(zip) }
            }
        }
    }
}

fun File.ignored(): Boolean {
    val lines = (if (isDirectory) this else parentFile)("git", "check-ignore", "$this")
    return lines.isNotEmpty() && !lines[0].startsWith("fatal")
}

operator fun File.invoke(vararg command: String): List<String> {
    val lines = mutableListOf<String>()
    val process = ProcessBuilder(*command).directory(this).redirectErrorStream(true).start()
    process.inputStream.bufferedReader().use { it.forEachLine(lines::add) }
    process.waitFor()
    return lines
}

fun ZipOutputStream.addDirectories(directories: MutableCollection<File>, outer: File, directory: File) {
    if (directory.parentFile in directories) addDirectories(directories, outer, directory.parentFile)

    val name = "${directory.toRelativeString(outer)}${File.separator}"
    println("zip $name")
    val entry = ZipEntry(name)
    entry.time = directory.lastModified()
    putNextEntry(entry)
    directories -= directory
}