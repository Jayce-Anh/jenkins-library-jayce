def call(Map params = [:]) {
    /**
     * Run Trivy security scan on Docker images
     * @param params.imageName - Docker image name to scan
     * @param params.imageTag - Docker image tag to scan
     * @param params.format - Output format (table, json, sarif) - default: table
     * @param params.outputFile - Output file path (optional)
     * @param params.severity - Severity levels to report (UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL)
     * @param params.useAgent - Use dedicated Trivy Docker agent (default: false)
     * @param params.trivyImage - Trivy Docker image to use as agent (default: aquasec/trivy:latest)
     */
    def imageName = params.imageName
    def imageTag = params.imageTag
    def format = params.format ?: 'table'
    def outputFile = params.outputFile
    def severity = params.severity ?: 'HIGH,CRITICAL'
    def useAgent = params.useAgent ?: false
    def trivyImage = params.trivyImage ?: 'aquasec/trivy:latest'
    
    echo "🔒 Running Trivy security scan..."
    
    if (useAgent) {
        runWithTrivyAgent(trivyImage, imageName, imageTag, format, outputFile, severity)
    } else {
        runLocalTrivy(imageName, imageTag, format, outputFile, severity)
    }
}

def runWithTrivyAgent(trivyImage, imageName, imageTag, format, outputFile, severity) {
    echo "🐳 Using Trivy Docker agent: ${trivyImage}"
    
    // Create reports directory
    sh 'mkdir -p trivy-reports'
    
    if (imageName && imageTag) {
        def fullImageName = "${imageName}:${imageTag}"
        def defaultOutputFile = "trivy-reports/scan-${imageTag}.txt"
        def output = outputFile ?: defaultOutputFile
        
        echo "🐳 Scanning image: ${fullImageName}"
        echo "📄 Output format: ${format}"
        echo "⚠️ Severity filter: ${severity}"
        
        // Run Trivy in Docker container with Docker socket mounted
        sh """
            docker run --rm \\
                -v /var/run/docker.sock:/var/run/docker.sock \\
                -v \$(pwd)/trivy-reports:/reports \\
                ${trivyImage} \\
                image \\
                --format ${format} \\
                --output /reports/scan-${imageTag}.txt \\
                --severity ${severity} \\
                ${fullImageName}
        """
        
        echo "✅ Security scan completed. Report saved to: ${output}"
        
    } else {
        echo "⚠️ No image specified, scanning filesystem..."
        sh """
            docker run --rm \\
                -v \$(pwd):/workspace \\
                -v \$(pwd)/trivy-reports:/reports \\
                ${trivyImage} \\
                fs \\
                --format ${format} \\
                --output /reports/filesystem-scan.txt \\
                --severity ${severity} \\
                /workspace
        """
        echo "✅ Filesystem scan completed"
    }
    
    // Archive the scan results
    archiveArtifacts artifacts: 'trivy-reports/*', allowEmptyArchive: true
}

def runLocalTrivy(imageName, imageTag, format, outputFile, severity) {
    // Create reports directory
    sh 'mkdir -p trivy-reports'
    
    if (imageName && imageTag) {
        def fullImageName = "${imageName}:${imageTag}"
        def defaultOutputFile = "trivy-reports/scan-${imageTag}.txt"
        def output = outputFile ?: defaultOutputFile
        
        echo "🐳 Scanning image: ${fullImageName}"
        echo "📄 Output format: ${format}"
        echo "⚠️ Severity filter: ${severity}"
        
        sh """
            trivy image \\
                --format ${format} \\
                --output ${output} \\
                --severity ${severity} \\
                ${fullImageName}
        """
        
        echo "✅ Security scan completed. Report saved to: ${output}"
        
        // Archive the scan results
        archiveArtifacts artifacts: 'trivy-reports/*', allowEmptyArchive: true
        
    } else {
        echo "⚠️ No image specified, scanning filesystem..."
        sh """
            trivy fs \\
                --format ${format} \\
                --output trivy-reports/filesystem-scan.txt \\
                --severity ${severity} \\
                .
        """
        echo "✅ Filesystem scan completed"
    }
}