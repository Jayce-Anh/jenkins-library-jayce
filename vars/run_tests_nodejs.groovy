def call(Map params = [:]) {
    /**
     * Run Node.js unit tests with dependency installation
     * @param params.testCommand - Custom test command (default: npm test)
     * @param params.skipInstall - Skip dependency installation (default: false)
     * @param params.useAgent - Use dedicated Docker agent for testing (default: false)
     * @param params.nodeImage - Node.js Docker image to use as agent (default: node:18-alpine)
     * @param params.packageManager - Package manager to use (npm, yarn, pnpm) - default: npm
     */
    def testCommand = params.testCommand ?: 'npm test'
    def skipInstall = params.skipInstall ?: false
    def useAgent = params.useAgent ?: false
    def nodeImage = params.nodeImage ?: 'node:18-alpine'
    def packageManager = params.packageManager ?: 'npm'
    
    echo "🧪 Running Node.js tests..."
    echo "📦 Package manager: ${packageManager}"
    
    if (!fileExists('package.json')) {
        error "❌ package.json not found. This doesn't appear to be a Node.js project."
    }
    
    if (useAgent) {
        runNodeTestsWithAgent(testCommand, skipInstall, nodeImage, packageManager)
    } else {
        runNodeTestsLocal(testCommand, skipInstall, packageManager)
    }
    
    echo "✅ Node.js tests completed successfully"
}

def runNodeTestsWithAgent(testCommand, skipInstall, nodeImage, packageManager) {
    echo "📦 Running Node.js tests with Docker agent: ${nodeImage}"
    
    def installCmd = ''
    if (!skipInstall) {
        switch(packageManager) {
            case 'yarn':
                installCmd = 'yarn install --frozen-lockfile || yarn install &&'
                break
            case 'pnpm':
                installCmd = 'pnpm install --frozen-lockfile || pnpm install &&'
                break
            default:
                installCmd = 'npm ci || npm install &&'
        }
    }
    
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            -w /workspace \\
            ${nodeImage} \\
            sh -c "${installCmd} ${testCommand}"
    """
}

def runNodeTestsLocal(testCommand, skipInstall, packageManager) {
    echo "📦 Running Node.js tests locally"
    
    // Check if Node.js is available
    def nodeExists = sh(script: 'which node', returnStatus: true) == 0
    if (!nodeExists) {
        error "❌ Node.js not found on Jenkins agent. Please install Node.js or use Docker agent (useAgent: true)."
    }
    
    // Check package manager availability
    def pmExists = sh(script: "which ${packageManager}", returnStatus: true) == 0
    if (!pmExists && packageManager != 'npm') {
        echo "⚠️ ${packageManager} not found, falling back to npm"
        packageManager = 'npm'
    }
    
    if (!skipInstall) {
        echo "📥 Installing dependencies with ${packageManager}..."
        switch(packageManager) {
            case 'yarn':
                sh 'yarn install --frozen-lockfile || yarn install'
                break
            case 'pnpm':
                sh 'pnpm install --frozen-lockfile || pnpm install'
                break
            default:
                sh 'npm ci || npm install'
        }
    }
    
    echo "🧪 Running tests..."
    sh testCommand
}