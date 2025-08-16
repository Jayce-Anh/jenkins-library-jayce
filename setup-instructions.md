# 🚀 Quick Setup Instructions


## Step 1: Create GitHub Repository

1. Go to GitHub.com
2. Click "New Repository"
3. Name it: `jenkins-shared-library` (or your preferred name)
4. **Don't** initialize with README (we already have one)
5. Click "Create repository"

## Step 2: Push to GitHub

```bash
# Replace 'your-username' with your actual GitHub username
git remote add origin https://github.com/your-username/jenkins-shared-library.git
git add .
git commit -m "First commit"
git push -u origin main
```

## Step 3: Configure in Jenkins

1. Go to **Manage Jenkins** > **Configure System**
2. Scroll to **Global Pipeline Libraries**
3. Click **Add** and fill in:
   - **Name**: `Shared`
   - **Default version**: `main`
   - **Retrieval method**: Modern SCM
   - **Source Code Management**: Git
   - **Project Repository**: `https://github.com/your-username/jenkins-shared-library.git`

## Step 4: Test Your Pipeline

Your `Jenkinsfile` is already configured to use the shared library with:
```groovy
@Library('share-jayce') _
```

Run your Jenkins pipeline and it should now work! 🎉

## 🔧 Troubleshooting

- **Library not found**: Check the repository URL and library name in Jenkins configuration
- **Function errors**: Check Jenkins console logs for detailed error messages
- **Git permissions**: Make sure Jenkins can access your GitHub repository

## 📝 Notes

- The library name in Jenkins (`Shared`) must match the name in your Jenkinsfile
- You can use different branches by changing the "Default version" setting
- Private repositories require additional authentication setup