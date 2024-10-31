import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

fun connectToDatabase(dbPath: String): Connection? {
    return try {
        val url = "jdbc:sqlite:$dbPath"
        DriverManager.getConnection(url).also {
            println("Connection to SQLite has been established.")
        }
    } catch (e: SQLException) {
        println("Failed to connect to the database: ${e.message}")
        null
    }
}

fun Connection.ifNotExistCreateTable() {
    this.createStatement().use {
        it.execute(
            """
        -- 文件表
        CREATE TABLE IF NOT EXISTS fileItem (
            id   INTEGER
            primary key,
            name TEXT not null,
            path TEXT not null unique
        );
        """.trimIndent()
        )
        it.execute(
            """
    -- 标签表
    CREATE TABLE IF NOT EXISTS tag (
        id INTEGER PRIMARY KEY,
        name TEXT NOT NULL
    );
    """.trimIndent()
        )
        it.execute(
            """
    -- 关联表，用于链接文件和标签的多对多关系
    CREATE TABLE IF NOT EXISTS fileItemTag (
        file_id INTEGER,
        tag_id INTEGER,
        FOREIGN KEY (file_id) REFERENCES fileItem(id),
        FOREIGN KEY (tag_id) REFERENCES tag(id)
    );
    """.trimIndent()
        )
    }


}

fun Connection.insertFileItem(fileItem: FileItem) {
    var fileId: Int? = null
    this.prepareStatement("INSERT INTO fileItem (path, name) VALUES (?, ?) RETURNING id").use {
        it.setString(1, fileItem.path)
        it.setString(2, fileItem.name)
        it.executeQuery().use { resultSet ->
            if (resultSet.next()) {
                fileId = resultSet.getInt("id")
            }
        }
    }
    fileItem.tags.forEach {
        this.prepareStatement("insert into fileItemTag (file_id,tag_id) values (?,?)").use { preparedStatement ->
            preparedStatement.setInt(1, fileId!!)
            preparedStatement.setInt(2, it.id!!)
            preparedStatement.executeUpdate()
        }
    }
    println("inserted successfully.")
}

fun Connection.insertTag(tag: Tag) {
    val sql = "INSERT INTO tag (name) VALUES (?)"
    this.prepareStatement(sql).use {
        it.setString(1, tag.name)
        it.executeUpdate()
        println("inserted successfully.")
    }
}

fun Connection.queryAllFileItems(): List<FileItem> {
    val fileItemMap = mutableMapOf<Int, FileItem>()
    this.createStatement().use {
        it.executeQuery(
            """
            SELECT fileItem.id, fileItem.name, fileItem.path, tag.id AS tag_id, tag.name AS tag_name
            FROM fileItem
                     LEFT JOIN fileItemTag ON fileItem.id = fileItemTag.file_id
                     LEFT JOIN tag ON tag.id = fileItemTag.tag_id
            ORDER BY fileItem.id
        """.trimIndent()
        ).use { resultSet ->
            while (resultSet.next()) {
                val id = resultSet.getInt("id")
                val fileItem = if (fileItemMap.contains(id)) {
                    val fileItem = fileItemMap[id]
                    fileItem!!
                } else {
                    val fileItem = FileItem().apply { this.id = id }
                    fileItemMap[id] = fileItem
                    fileItem
                }
                fileItem.name = resultSet.getString("name")
                fileItem.path = resultSet.getString("path")
                val tag = Tag()
                tag.id = resultSet.getInt("tag_id")
                tag.name = resultSet.getString("tag_name")
                fileItem.tags.add(tag)
            }
        }
    }
    return fileItemMap.values.toList()
}

fun Connection.getAllTags(): List<Tag> {
    return this.createStatement().use {
        it.executeQuery("select * from tag").useAndPackageData { resultSet ->
            val tag = Tag()
            tag.id = resultSet.getInt("id")
            tag.name = resultSet.getString("name")
            tag
        }
    }
}

fun <T> ResultSet.useAndPackageData(row: (ResultSet) -> T): List<T> {
    return this.use {
        val list = mutableListOf<T>()
        while (it.next()) {
            list.add(row(it))
        }
        list
    }
}

data class FileItem(
    var name: String = "",
    var tags: SnapshotStateList<Tag> = mutableStateListOf(),
    var path: String = "",
) {
    var id: Int? = null
}

data class Tag(var name: String) {
    var id: Int? = null

    constructor() : this("")
}

fun main() {
    connection()?.use {
//        createTable()
//        insertTag(Tag("Work"))
//        insertTag(Tag("Personal"))
//        insertTag(Tag("Important"))
//        insertTag(Tag("Study"))
//        val allTags = it.getAllTags()
//        it.insertFileItem(FileItem("Document1.pdf", listOf(allTags[0], allTags[1]), "/path/to/Document1.pdf"))
//        it.insertFileItem(FileItem("Photo.png", listOf(allTags[2]), "/path/to/Photo.png"))
//        it.insertFileItem(FileItem("Assignment.docx", listOf(allTags[3]), "/path/to/Assignment.docx"))
//        it.insertFileItem(FileItem("Notes.txt", listOf(allTags[2], allTags[3]), "/path/to/Notes.txt"))
        val fileItems = it.queryAllFileItems()
        println(fileItems)
    }
}

fun connection() = connectToDatabase("${System.getProperty("user.dir")}/data.db")