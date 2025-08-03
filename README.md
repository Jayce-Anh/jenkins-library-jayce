# 🚀 Jenkins Shared Library

A collection of reusable Jenkins pipeline functions to make your CI/CD life easier!

## 📁 **Library Structure**

```
jenkins-shared-library/
├── vars/                    # ✨ Global Pipeline Functions
├── src/                     # 🔧 Shared Groovy Classes  
├── resources/               # 📂 Static Files & Templates like YAML, k8s Manifest, template, ...
```

### 🌟 **`vars/` Folder**
Contains **global pipeline functions** that you can call directly in your Jenkinsfiles.

**Current Functions:**
- `clean_ws()` - Clean workspace
- `clone(repoUrl, branch)` - Clone Git repositories  
- `docker_build(params)` - Build Docker images
- `docker_push(params)` - Push Docker images to registry
- `run_tests(params)` - Run unit tests (auto-detects framework)
- `trivy_scan(params)` - Security scanning with Trivy
- `update_k8s_manifests(params)` - Update Kubernetes manifests

**Usage Example:**
```groovy
@Library('Shared') _

pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                run_tests()
            }
        }
        stage('Build') {
            steps {
                docker_build([
                    imageName: 'my-app',
                    imageTag: env.BUILD_NUMBER
                ])
            }
        }
    }
}
```

### 🔧 **`src/` Folder**
Contains **shared Groovy classes** for complex logic and utilities.

**Purpose:**
- Helper classes for complex operations
- Data models and structures  
- Utility functions that don't need to be global pipeline steps

**Structure:**
```
src/
└── com/
    └── company/
        └── MyUtilityClass.groovy
```

**Usage Example:**
```groovy
// In vars/my_function.groovy
import com.company.MyUtilityClass

def call() {
    def util = new MyUtilityClass()
    return util.processData()
}
```

### 📂 **`resources/` Folder**
Stores **static files** and templates that your pipeline functions need.

**What goes here:**
- Configuration files (JSON, YAML, properties)
- Script templates
- Docker compose files
- Kubernetes manifest templates
- Shell scripts

**Usage Example:**
```groovy
// In vars/deploy.groovy
def call() {
    def template = libraryResource 'kubernetes/deployment-template.yaml'
    writeFile file: 'deployment.yaml', text: template
    sh 'kubectl apply -f deployment.yaml'
}
```

## 🚀 **Quick Start**

1. **Setup Library in Jenkins:**
   - Go to **Manage Jenkins** → **Configure System**
   - Add **Global Pipeline Library** named `Shared`
   - Point to this repository

2. **Use in Jenkinsfile:**
   ```groovy
   @Library('Shared') _
   ...
   ```

