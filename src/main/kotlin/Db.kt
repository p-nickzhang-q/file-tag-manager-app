import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cn.hutool.core.exceptions.ValidateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

fun connectToDatabase(dbPath: String): Connection {
    return try {
        val url = "jdbc:sqlite:$dbPath"
        DriverManager.getConnection(url).also {
            println("Connection to SQLite has been established.")
        }
    } catch (e: SQLException) {
        throw ValidateException("Failed to connect to the database: ${e.message}")
    }
}

suspend fun ifNotExistCreateTable() = withContext(Dispatchers.IO) {
    connection.createStatement().use {
        it.execute(
            """
        -- 文件表
        CREATE TABLE IF NOT EXISTS fileItem (
            name TEXT not null,
            path TEXT not null unique,
            id   TEXT primary key
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
        file_id TEXT,
        tag_id INTEGER,
        FOREIGN KEY (file_id) REFERENCES fileItem(id),
        FOREIGN KEY (tag_id) REFERENCES tag(id)
    );
    """.trimIndent()
        )
    }


}

suspend fun insertFileItem(fileItem: FileItem) = withContext(Dispatchers.IO) {
    connection.prepareStatement("INSERT INTO fileItem (path, name) VALUES (?, ?) RETURNING id").use {
        it.setString(1, fileItem.path)
        it.setString(2, fileItem.name)
        it.executeQuery().use { resultSet ->
            if (resultSet.next()) {
                fileItem.id = resultSet.getInt("id")
            }
        }
    }
    fileItem.tags.forEach {
        insertFileTag(fileItem.id!!, it.id!!)
    }
}

suspend fun removeFileItem(fileId: Int) = withContext(Dispatchers.IO) {
    connection.prepareStatement("delete from fileItem where id = ?").use {
        it.setInt(1, fileId)
        it.executeUpdate()
    }
    connection.prepareStatement("delete from fileItemTag where file_id = ?").use {
        it.setInt(1, fileId)
        it.executeUpdate()
    }
}

suspend fun insertFileTag(fileId: Int, tagId: Int) = withContext(Dispatchers.IO) {
    connection.prepareStatement("insert into fileItemTag (file_id,tag_id) values (?,?)").use { preparedStatement ->
        preparedStatement.setInt(1, fileId)
        preparedStatement.setInt(2, tagId)
        preparedStatement.executeUpdate()
    }
}

suspend fun removeFileTag(fileId: Int, tagId: Int) = withContext(Dispatchers.IO) {
    connection.prepareStatement("delete from fileItemTag where tag_id = ? and file_id = ?").use { preparedStatement ->
        preparedStatement.setInt(1, tagId)
        preparedStatement.setInt(2, fileId)
        preparedStatement.executeUpdate()
    }
}

suspend fun insertTag(tag: Tag) {
    withContext(Dispatchers.IO) {
        val sql = "INSERT INTO tag (name) VALUES (?) RETURNING id"
        connection.prepareStatement(sql).use {
            it.setString(1, tag.name)
            it.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    tag.id = resultSet.getInt("id")
                }
            }
        }
    }
}

suspend fun deleteTag(tagId: Int) {
    withContext(Dispatchers.IO) {
        val sql = "delete from tag where id = ?"
        connection.prepareStatement(sql).use {
            it.setInt(1, tagId)
            it.executeUpdate()
        }
    }
}

suspend fun updateTag(tag: Tag) {
    withContext(Dispatchers.IO) {
        val sql = "update tag set name = ? where id = ?"
        connection.prepareStatement(sql).use {
            it.setString(1, tag.name)
            it.setInt(2, tag.id!!)
            it.executeUpdate()
        }
    }
}

suspend fun queryAllFileItems(): List<FileItem> {
    return withContext(Dispatchers.IO) {
        val fileItemMap = mutableMapOf<Int, FileItem>()
        connection.createStatement().use {
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
                    val tagId = resultSet.getInt("tag_id")
                    if (tagId != 0) {
                        val tag = Tag()
                        tag.id = tagId
                        tag.name = resultSet.getString("tag_name")
                        fileItem.tags.add(tag)
                    }
                }
            }
        }
        return@withContext fileItemMap.values.toList()
    }
}

suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
    return@withContext connection.createStatement().use {
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
    var selected by mutableStateOf(false)
}

data class Tag(var name: String) {
    var id: Int? = null

    constructor() : this("")
}

fun main() {
    val code = fileKey("/home/dev/ComposeForDesktopDemo")
    println(code)
}


val connection = connectToDatabase("${System.getProperty("user.dir")}/data.db")