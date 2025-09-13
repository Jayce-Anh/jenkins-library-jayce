def call(Map params = [:]) {
    /**
     * Run .NET unit tests with dependency installation
     * @param params.testCommand - Custom test command (default: dotnet test)
     * @param params.skipInstall - Skip dependency restoration (default: false)
     * @param params.useAgent - Use dedicated Docker agent for testing (default: false)
     * @param params.dotnetImage - .NET Docker image to use as agent (default: mcr.microsoft.com/dotnet/sdk:6.0)
     * @param params.dotnetVersion - .NET version (3.1, 5.0, 6.0, 7.0, 8.0) - default: 6.0
     * @param params.configuration - Build configuration (Debug, Release) - default: Debug
     * @param params.framework - Target framework (net6.0, net7.0, net8.0) - auto-detected if not specified
     */
    def testCommand = params.testCommand ?: 'dotnet test'
    def skipInstall = params.skipInstall ?: false
    def useAgent = params.useAgent ?: false
    def dotnetImage = params.dotnetImage ?: 'mcr.microsoft.com/dotnet/sdk:6.0'
    def dotnetVersion = params.dotnetVersion ?: '6.0'
    def configuration = params.configuration ?: 'Debug'
    def framework = params.framework
    
    echo "🧪 Running .NET tests..."
    echo "🔷 .NET version: ${dotnetVersion}"
    echo "⚙️ Configuration: ${configuration}"
    
    // Check for .NET project files
    def projectFiles = sh(script: 'find . -name "*.csproj" -o -name "*.fsproj" -o -name "*.vbproj" -o -name "*.sln" | head -5', returnStdout: true).trim()
    if (!projectFiles) {
        error "❌ No .NET project files found (*.csproj, *.fsproj, *.vbproj, *.sln). This doesn't appear to be a .NET project."
    }
    
    echo "📁 Found .NET project files:"
    projectFiles.split('\n').each { file ->
        echo "  - ${file}"
    }
    
    // Add framework parameter if specified
    def fullTestCommand = testCommand
    if (framework) {
        fullTestCommand += " --framework ${framework}"
    }
    fullTestCommand += " --configuration ${configuration}"
    
    if (useAgent) {
        runDotNetTestsWithAgent(fullTestCommand, skipInstall, dotnetImage)
    } else {
        runDotNetTestsLocal(fullTestCommand, skipInstall)
    }
    
    echo "✅ .NET tests completed successfully"
}

def runDotNetTestsWithAgent(testCommand, skipInstall, dotnetImage) {
    echo "🔷 Running .NET tests with Docker agent: ${dotnetImage}"
    
    def restoreCmd = skipInstall ? '' : 'dotnet restore &&'
    
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            -w /workspace \\
            ${dotnetImage} \\
            bash -c "${restoreCmd} ${testCommand}"
    """
}

def runDotNetTestsLocal(testCommand, skipInstall) {
    echo "🔷 Running .NET tests locally"
    
    // Check if .NET CLI is available
    def dotnetExists = sh(script: 'which dotnet', returnStatus: true) == 0
    if (!dotnetExists) {
        error "❌ .NET CLI not found on Jenkins agent. Please install .NET SDK or use Docker agent (useAgent: true)."
    }
    
    // Show .NET version
    sh 'dotnet --version'
    
    if (!skipInstall) {
        echo "📥 Restoring .NET dependencies..."
        sh 'dotnet restore'
    }
    
    echo "🧪 Running .NET tests..."
    sh testCommand
    
    // Optionally publish test results
    echo "📊 Test execution completed"
}