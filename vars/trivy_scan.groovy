def call(Map params = [:]) {
    /**
     * Run Trivy security scan on Docker images
     * @param params.imageName - Docker image name to scan
     * @param params.imageTag - Docker image tag to scan
     * @param params.format - Output format (table, json, sarif) - default: table
     * @param params.outputFile - Output file path (optional)
     * @param params.severity - Severity levels to report (UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL)
     */
    def imageName = params.imageName
    def imageTag = params.imageTag
    def format = params.format ?: 'table'
    def outputFile = params.outputFile
    def severity = params.severity ?: 'HIGH,CRITICAL'
    
    echo "🔒 Running Trivy security scan..."
    
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