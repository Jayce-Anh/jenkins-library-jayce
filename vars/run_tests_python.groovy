def call(Map params = [:]) {
    /**
     * Run Python unit tests with dependency installation
     * @param params.testCommand - Custom test command (default: pytest)
     * @param params.skipInstall - Skip dependency installation (default: false)
     * @param params.useAgent - Use dedicated Docker agent for testing (default: false)
     * @param params.pythonImage - Python Docker image to use as agent (default: python:3.9-alpine)
     * @param params.pythonVersion - Python version to use (3.8, 3.9, 3.10, 3.11) - default: 3.9
     * @param params.testFramework - Test framework (pytest, unittest, nose2) - default: pytest
     */
    def testCommand = params.testCommand
    def skipInstall = params.skipInstall ?: false
    def useAgent = params.useAgent ?: false
    def pythonImage = params.pythonImage ?: 'python:3.9-alpine'
    def pythonVersion = params.pythonVersion ?: '3.9'
    def testFramework = params.testFramework ?: 'pytest'
    
    echo "🧪 Running Python tests..."
    echo "🐍 Python version: ${pythonVersion}"
    echo "🧪 Test framework: ${testFramework}"
    
    if (!fileExists('requirements.txt') && !fileExists('setup.py') && !fileExists('pyproject.toml')) {
        error "❌ No Python project files found (requirements.txt, setup.py, or pyproject.toml). This doesn't appear to be a Python project."
    }
    
    // Set default test command if not provided
    if (!testCommand) {
        switch(testFramework) {
            case 'unittest':
                testCommand = 'python -m unittest discover'
                break
            case 'nose2':
                testCommand = 'nose2'
                break
            default:
                testCommand = 'pytest'
        }
    }
    
    if (useAgent) {
        runPythonTestsWithAgent(testCommand, skipInstall, pythonImage, testFramework)
    } else {
        runPythonTestsLocal(testCommand, skipInstall, testFramework)
    }
    
    echo "✅ Python tests completed successfully"
}

def runPythonTestsWithAgent(testCommand, skipInstall, pythonImage, testFramework) {
    echo "🐍 Running Python tests with Docker agent: ${pythonImage}"
    
    def installCmd = ''
    if (!skipInstall) {
        installCmd = '''
            if [ -f requirements.txt ]; then pip install -r requirements.txt; fi &&
            if [ -f setup.py ]; then pip install -e .; fi &&
            if [ -f pyproject.toml ]; then pip install -e .; fi &&
        '''
        
        // Install test framework if not in requirements
        if (testFramework == 'pytest') {
            installCmd += 'pip install pytest &&'
        } else if (testFramework == 'nose2') {
            installCmd += 'pip install nose2 &&'
        }
    }
    
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            -w /workspace \\
            ${pythonImage} \\
            sh -c "${installCmd} ${testCommand}"
    """
}

def runPythonTestsLocal(testCommand, skipInstall, testFramework) {
    echo "🐍 Running Python tests locally"
    
    // Check if Python is available
    def pythonExists = sh(script: 'which python3 || which python', returnStatus: true) == 0
    if (!pythonExists) {
        error "❌ Python not found on Jenkins agent. Please install Python or use Docker agent (useAgent: true)."
    }
    
    if (!skipInstall) {
        echo "📥 Installing dependencies..."
        
        // Install pip if not available
        sh 'python3 -m pip --version || python -m pip --version || curl https://bootstrap.pypa.io/get-pip.py | python'
        
        // Install dependencies
        if (fileExists('requirements.txt')) {
            sh 'python3 -m pip install -r requirements.txt || python -m pip install -r requirements.txt'
        }
        if (fileExists('setup.py')) {
            sh 'python3 -m pip install -e . || python -m pip install -e .'
        }
        if (fileExists('pyproject.toml')) {
            sh 'python3 -m pip install -e . || python -m pip install -e .'
        }
        
        // Install test framework if needed
        if (testFramework == 'pytest') {
            sh 'python3 -m pip install pytest || python -m pip install pytest'
        } else if (testFramework == 'nose2') {
            sh 'python3 -m pip install nose2 || python -m pip install nose2'
        }
    }
    
    echo "🧪 Running tests..."
    sh "python3 -m ${testCommand} || python -m ${testCommand} || ${testCommand}"
}