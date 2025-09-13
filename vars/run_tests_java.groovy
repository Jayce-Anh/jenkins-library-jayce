def call(Map params = [:]) {
    /**
     * Run Java unit tests with dependency installation
     * @param params.testCommand - Custom test command (optional)
     * @param params.skipInstall - Skip dependency installation (default: false)
     * @param params.useAgent - Use dedicated Docker agent for testing (default: false)
     * @param params.mavenImage - Maven Docker image to use as agent (default: maven:3.8-openjdk-11)
     * @param params.gradleImage - Gradle Docker image to use as agent (default: gradle:7.6-jdk11)
     * @param params.javaVersion - Java version (8, 11, 17, 21) - default: 11
     * @param params.buildTool - Build tool (maven, gradle) - auto-detected if not specified
     */
    def testCommand = params.testCommand
    def skipInstall = params.skipInstall ?: false
    def useAgent = params.useAgent ?: false
    def mavenImage = params.mavenImage ?: 'maven:3.8-openjdk-11'
    def gradleImage = params.gradleImage ?: 'gradle:7.6-jdk11'
    def javaVersion = params.javaVersion ?: '11'
    def buildTool = params.buildTool
    
    echo "🧪 Running Java tests..."
    echo "☕ Java version: ${javaVersion}"
    
    // Auto-detect build tool if not specified
    if (!buildTool) {
        if (fileExists('pom.xml')) {
            buildTool = 'maven'
        } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
            buildTool = 'gradle'
        } else {
            error "❌ No recognized Java build file found (pom.xml, build.gradle). This doesn't appear to be a Java project."
        }
    }
    
    echo "🔧 Build tool: ${buildTool}"
    
    // Set default test command if not provided
    if (!testCommand) {
        switch(buildTool) {
            case 'gradle':
                testCommand = './gradlew clean test'
                break
            default:
                testCommand = 'mvn clean test'
        }
    }
    
    if (useAgent) {
        runJavaTestsWithAgent(testCommand, skipInstall, buildTool, mavenImage, gradleImage)
    } else {
        runJavaTestsLocal(testCommand, skipInstall, buildTool)
    }
    
    echo "✅ Java tests completed successfully"
}

def runJavaTestsWithAgent(testCommand, skipInstall, buildTool, mavenImage, gradleImage) {
    def dockerImage = buildTool == 'gradle' ? gradleImage : mavenImage
    echo "☕ Running Java tests with Docker agent: ${dockerImage}"
    
    if (buildTool == 'maven') {
        sh """
            docker run --rm \\
                -v \$(pwd):/workspace \\
                -v ~/.m2:/root/.m2 \\
                -w /workspace \\
                ${mavenImage} \\
                ${testCommand}
        """
    } else {
        // For Gradle, handle wrapper and cache
        sh """
            docker run --rm \\
                -v \$(pwd):/workspace \\
                -v ~/.gradle:/root/.gradle \\
                -w /workspace \\
                ${gradleImage} \\
                bash -c "chmod +x gradlew && ${testCommand}"
        """
    }
}

def runJavaTestsLocal(testCommand, skipInstall, buildTool) {
    echo "☕ Running Java tests locally"
    
    if (buildTool == 'maven') {
        // Check if Maven is available
        def mavenExists = sh(script: 'which mvn', returnStatus: true) == 0
        if (!mavenExists) {
            error "❌ Maven not found on Jenkins agent. Please install Maven or use Docker agent (useAgent: true)."
        }
        
        echo "📥 Running Maven tests..."
        sh testCommand
        
    } else if (buildTool == 'gradle') {
        // Check if Gradle wrapper exists, otherwise check for system Gradle
        if (fileExists('gradlew')) {
            echo "📥 Using Gradle wrapper..."
            sh "chmod +x gradlew && ${testCommand}"
        } else {
            def gradleExists = sh(script: 'which gradle', returnStatus: true) == 0
            if (!gradleExists) {
                error "❌ Gradle not found on Jenkins agent and no gradlew wrapper found. Please install Gradle, add gradlew wrapper, or use Docker agent (useAgent: true)."
            }
            echo "📥 Using system Gradle..."
            sh testCommand.replace('./gradlew', 'gradle')
        }
    }
}