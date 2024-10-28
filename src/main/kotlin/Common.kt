fun MutableList<FileItem>.ifNotExistThenAdd(fileItem: FileItem) {
    if (this.none { it.path == fileItem.path }) {
        this.add(fileItem)
    }
}