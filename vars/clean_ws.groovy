def call() {
    /**
     * Clean workspace - removes all files and directories from the current workspace
     */
    echo "🧹 Cleaning workspace..."
    cleanWs()
    echo "✅ Workspace cleaned successfully"
}