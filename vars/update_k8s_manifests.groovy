def call(Map params) {
    /**
     * Update Kubernetes manifests with new image tags
     * @param params.imageTag - New image tag to update
     * @param params.manifestsPath - Path to Kubernetes manifests directory
     * @param params.gitCredentials - Jenkins credentials ID for Git operations
     * @param params.gitUserName - Git user name for commits
     * @param params.gitUserEmail - Git user email for commits
     * @param params.gitRepo - Git repository URL (optional, uses current repo if not specified)
     * @param params.gitBranch - Git branch to push to (default: master)
     */
    def imageTag = params.imageTag
    def manifestsPath = params.manifestsPath ?: 'kubernetes'
    def gitCredentials = params.gitCredentials
    def gitUserName = params.gitUserName
    def gitUserEmail = params.gitUserEmail
    def gitRepo = params.gitRepo
    def gitBranch = params.gitBranch ?: 'master'
    
    echo "🔄 Updating Kubernetes manifests..."
    echo "🏷️ New image tag: ${imageTag}"
    echo "📁 Manifests path: ${manifestsPath}"
    
    withCredentials([usernamePassword(credentialsId: gitCredentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
        sh """
            # Configure Git
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            echo "📝 Updating image tags in YAML files..."
            
            # Update main app image tags
            find ${manifestsPath} -name "*.yaml" -o -name "*.yml" | while read file; do
                echo "Processing: \$file"
                sed -i 's|image: .*easy_shop-app:.*|image: \${env.IMAGE_NAME}:${imageTag}|g' "\$file"
                sed -i 's|image: .*migration:.*|image: \${env.MIGRATION_IMAGE_NAME}:${imageTag}|g' "\$file"
            done
            
            # Check if there are any changes
            if git diff --quiet ${manifestsPath}; then
                echo "📭 No changes detected in manifests"
            else
                echo "📤 Changes detected, committing..."
                
                # Stage changes
                git add ${manifestsPath}/
                
                # Commit changes
                git commit -m "🚀 Update image tags to ${imageTag} [skip ci]"
                
                # Push changes
                if [ -n "${gitRepo}" ]; then
                    echo "🔗 Pushing to repository: ${gitRepo}"
                    git push https://\${GIT_USERNAME}:\${GIT_PASSWORD}@\${gitRepo#https://} HEAD:${gitBranch}
                else
                    echo "🔗 Pushing to current repository"
                    git push origin HEAD:${gitBranch}
                fi
                
                echo "✅ Manifests updated and pushed successfully"
            fi
        """
    }
    
    echo "✅ Kubernetes manifests update completed"
}