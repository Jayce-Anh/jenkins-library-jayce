def call(Map params = [:]) {
    /**
     * Run Laravel/PHP unit tests with dependency installation
     * @param params.testCommand - Custom test command (default: php artisan test)
     * @param params.skipInstall - Skip dependency installation (default: false)
     * @param params.useAgent - Use dedicated Docker agent for testing (default: false)
     * @param params.phpImage - PHP Docker image to use as agent (default: php:8.1-cli)
     * @param params.phpVersion - PHP version (7.4, 8.0, 8.1, 8.2, 8.3) - default: 8.1
     * @param params.testFramework - Test framework (phpunit, pest) - auto-detected if not specified
     * @param params.envFile - Environment file to use (.env.testing, .env.local) - default: .env.testing
     */
    def testCommand = params.testCommand
    def skipInstall = params.skipInstall ?: false
    def useAgent = params.useAgent ?: false
    def phpImage = params.phpImage ?: 'php:8.1-cli'
    def phpVersion = params.phpVersion ?: '8.1'
    def testFramework = params.testFramework
    def envFile = params.envFile ?: '.env.testing'
    
    echo "🧪 Running Laravel/PHP tests..."
    echo "🐘 PHP version: ${phpVersion}"
    
    // Check for Laravel/PHP project files
    def isLaravel = fileExists('artisan')
    def hasComposer = fileExists('composer.json')
    
    if (!hasComposer) {
        error "❌ composer.json not found. This doesn't appear to be a PHP project."
    }
    
    // Auto-detect test framework if not specified
    if (!testFramework) {
        if (fileExists('phpunit.xml') || fileExists('phpunit.xml.dist')) {
            testFramework = 'phpunit'
        } else if (fileExists('pest.php') || sh(script: 'grep -q "pestphp/pest" composer.json', returnStatus: true) == 0) {
            testFramework = 'pest'
        } else {
            testFramework = 'phpunit' // default
        }
    }
    
    echo "🧪 Test framework: ${testFramework}"
    echo "🏠 Laravel project: ${isLaravel ? 'Yes' : 'No'}"
    
    // Set default test command if not provided
    if (!testCommand) {
        if (isLaravel) {
            switch(testFramework) {
                case 'pest':
                    testCommand = 'php artisan test --pest'
                    break
                default:
                    testCommand = 'php artisan test'
            }
        } else {
            switch(testFramework) {
                case 'pest':
                    testCommand = './vendor/bin/pest'
                    break
                default:
                    testCommand = './vendor/bin/phpunit'
            }
        }
    }
    
    if (useAgent) {
        runLaravelTestsWithAgent(testCommand, skipInstall, phpImage, isLaravel, envFile)
    } else {
        runLaravelTestsLocal(testCommand, skipInstall, isLaravel, envFile)
    }
    
    echo "✅ Laravel/PHP tests completed successfully"
}

def runLaravelTestsWithAgent(testCommand, skipInstall, phpImage, isLaravel, envFile) {
    echo "🐘 Running Laravel/PHP tests with Docker agent: ${phpImage}"
    
    def setupCmd = ''
    if (!skipInstall) {
        setupCmd = '''
            # Install Composer dependencies
            composer install --no-interaction --prefer-dist --optimize-autoloader &&
        '''
        
        if (isLaravel) {
            setupCmd += """
            # Setup Laravel environment
            if [ ! -f .env ]; then cp ${envFile} .env 2>/dev/null || cp .env.example .env 2>/dev/null || echo "APP_ENV=testing" > .env; fi &&
            php artisan key:generate --ansi &&
            php artisan config:cache &&
            """
        }
    }
    
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            -w /workspace \\
            ${phpImage} \\
            bash -c "
                # Install system dependencies
                apt-get update && apt-get install -y git unzip libzip-dev && 
                docker-php-ext-install zip &&
                
                # Install Composer
                curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/local/bin --filename=composer &&
                
                ${setupCmd}
                
                # Run tests
                ${testCommand}
            "
    """
}

def runLaravelTestsLocal(testCommand, skipInstall, isLaravel, envFile) {
    echo "🐘 Running Laravel/PHP tests locally"
    
    // Check if PHP is available
    def phpExists = sh(script: 'which php', returnStatus: true) == 0
    if (!phpExists) {
        error "❌ PHP not found on Jenkins agent. Please install PHP or use Docker agent (useAgent: true)."
    }
    
    // Check if Composer is available
    def composerExists = sh(script: 'which composer', returnStatus: true) == 0
    if (!composerExists) {
        error "❌ Composer not found on Jenkins agent. Please install Composer or use Docker agent (useAgent: true)."
    }
    
    // Show PHP version
    sh 'php --version'
    
    if (!skipInstall) {
        echo "📥 Installing PHP dependencies with Composer..."
        sh 'composer install --no-interaction --prefer-dist --optimize-autoloader'
        
        if (isLaravel) {
            echo "🏠 Setting up Laravel environment..."
            
            // Setup environment file
            sh """
                if [ ! -f .env ]; then 
                    cp ${envFile} .env 2>/dev/null || cp .env.example .env 2>/dev/null || echo "APP_ENV=testing" > .env
                fi
            """
            
            // Generate application key and cache config
            sh '''
                php artisan key:generate --ansi
                php artisan config:cache
            '''
        }
    }
    
    echo "🧪 Running tests..."
    sh testCommand
}