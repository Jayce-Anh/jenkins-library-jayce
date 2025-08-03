def call(String repoUrl, String branch = 'main') {
    /**
     * Clone a Git repository
     * @param repoUrl - The Git repository URL
     * @param branch - The branch to checkout (default: main)
     */
    echo "📥 Cloning repository: ${repoUrl}"
    echo "🌿 Branch: ${branch}"
    
    git branch: branch, url: repoUrl
    
    echo "✅ Repository cloned successfully"
}