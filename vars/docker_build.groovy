def call(Map params) {
    /**
     * Build Docker image
     * @param params.imageName - The name of the Docker image
     * @param params.imageTag - The tag for the Docker image
     * @param params.dockerfile - Path to Dockerfile (default: Dockerfile)
     * @param params.context - Build context path (default: .)
     */
    def imageName = params.imageName
    def imageTag = params.imageTag
    def dockerfile = params.dockerfile ?: 'Dockerfile'
    def context = params.context ?: '.'
    
    echo "🐳 Building Docker image: ${imageName}:${imageTag}"
    echo "📄 Dockerfile: ${dockerfile}"
    echo "📁 Context: ${context}"
    
    // Check if Dockerfile exists
    if (!fileExists(dockerfile)) {
        error "❌ Dockerfile not found: ${dockerfile}"
    }
    
    // Check Docker availability
    try {
        sh 'docker --version'
        echo "✅ Docker is available"
    } catch (Exception e) {
        error "❌ Docker is not available on this agent: ${e.getMessage()}"
    }
    
    // List files to debug
    echo "📋 Files in current directory:"
    sh 'ls -la'
    
    try {
        def image = docker.build("${imageName}:${imageTag}", "-f ${dockerfile} ${context}")
        echo "✅ Docker image built successfully: ${imageName}:${imageTag}"
        return image
    } catch (Exception e) {
        error "❌ Docker build failed: ${e.getMessage()}"
    }
}