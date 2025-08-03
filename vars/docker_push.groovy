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
    
    if (registry) {
        echo "📡 Registry: ${registry}"
        
        // Handle ECR registry
        if (registry.contains('amazonaws.com')) {
            echo "🔒 Detected AWS ECR registry"
            docker.withRegistry("https://${registry}", "ecr:${getECRRegion(registry)}:${credentials}") {
                def image = docker.image("${imageName}:${imageTag}")
                image.push()
                image.push('latest')
            }
        } else {
            // Handle other registries (Docker Hub, Harbor, etc.)
            docker.withRegistry("https://${registry}", credentials) {
                def image = docker.image("${imageName}:${imageTag}")
                image.push()
                image.push('latest')
            }
        }
    } else {
        // Push to Docker Hub (default registry)
        echo "🐳 Pushing to Docker Hub"
        docker.withRegistry('', credentials) {
            def image = docker.image("${imageName}:${imageTag}")
            image.push()
            image.push('latest')
        }
    }
    
    echo "✅ Image pushed successfully: ${imageName}:${imageTag}"
}

// Helper function to extract ECR region from registry URL
def getECRRegion(String registryUrl) {
    def matcher = registryUrl =~ /\.([^.]+)\.amazonaws\.com/
    return matcher ? matcher[0][1] : 'us-east-1'
}