def call(Map params) {
    /**
     * Push Docker image to registry
     * @param params.imageName - The name of the Docker image
     * @param params.imageTag - The tag for the Docker image
     * @param params.credentials - Jenkins credentials ID for registry authentication
     * @param params.registry - Registry URL (optional, defaults to Docker Hub)
     */
    def imageName = params.imageName
    def imageTag = params.imageTag
    def credentials = params.credentials
    def registry = params.registry
    
    echo "🚀 Pushing Docker image: ${imageName}:${imageTag}"
    
    try {
        if (registry) {
            echo "📡 Registry: ${registry}"
            
            // Handle ECR registry
            if (registry.contains('amazonaws.com')) {
                echo "🔒 Detected AWS ECR registry"
                def region = getECRRegion(registry)
                
                withCredentials([aws(credentialsId: credentials, region: region)]) {
                    // Login to ECR
                    sh "aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${registry}"
                    
                    // Push image with specific tag
                    sh "docker push ${imageName}:${imageTag}"
                    
                    // Tag and push as latest
                    sh "docker tag ${imageName}:${imageTag} ${imageName}:latest"
                    sh "docker push ${imageName}:latest"
                }
            } else {
                // Handle other registries (Docker Hub, Harbor, etc.)
                withCredentials([usernamePassword(credentialsId: credentials, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASS')]) {
                    // Login to registry
                    sh "echo \$REGISTRY_PASS | docker login ${registry} --username \$REGISTRY_USER --password-stdin"
                    
                    // Push image with specific tag
                    sh "docker push ${imageName}:${imageTag}"
                    
                    // Tag and push as latest
                    sh "docker tag ${imageName}:${imageTag} ${imageName}:latest"
                    sh "docker push ${imageName}:latest"
                }
            }
        } else {
            // Push to Docker Hub (default registry)
            echo "🐳 Pushing to Docker Hub"
            withCredentials([usernamePassword(credentialsId: credentials, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                // Login to Docker Hub
                sh "echo \$DOCKER_PASS | docker login --username \$DOCKER_USER --password-stdin"
                
                // Push image with specific tag
                sh "docker push ${imageName}:${imageTag}"
                
                // Tag and push as latest
                sh "docker tag ${imageName}:${imageTag} ${imageName}:latest"
                sh "docker push ${imageName}:latest"
            }
        }
        
        echo "✅ Image pushed successfully: ${imageName}:${imageTag}"
    } catch (Exception e) {
        error "❌ Docker push failed: ${e.getMessage()}"
    }
}

// Helper function to extract ECR region from registry URL
def getECRRegion(String registryUrl) {
    def matcher = registryUrl =~ /\.([^.]+)\.amazonaws\.com/
    return matcher ? matcher[0][1] : 'us-east-1'
}