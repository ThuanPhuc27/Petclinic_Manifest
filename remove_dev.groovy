pipeline {
    agent any

    parameters {
        string(name: 'branch', defaultValue: 'main', description: 'Branch cần comment các file YAML')
    }

    environment {
        GIT_REPO = 'https://github.com/ThuanPhuc27/Petclinic_Manifest.git'
    }

    stages {
        stage('Checkout Manifest Repo') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'petclinic_github', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    script {
                        // Clone lại repo nếu thư mục không tồn tại
                        def repoDir = 'Petclinic_Manifest'
                        if (!fileExists(repoDir)) {
                            sh """
                                git clone https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/ThuanPhuc27/Petclinic_Manifest.git
                            """
                        } else {
                            // Nếu repo đã tồn tại, pull mới nhất
                            sh """
                                cd Petclinic_Manifest
                                git pull origin ${params.branch}
                            """
                        }
                    }
                }
            }
        }

        stage('Comment YAML Files') {
            steps {
                dir('Petclinic_Manifest/dev') {
                    script {
                        // Tìm tất cả các file .yml trong thư mục dev và comment tất cả các dòng trong đó
                        sh """
                            find . -name "*.yml" -exec sed -i 's/^/#/' {} \\;
                        """
                    }
                }
            }
        }

        stage('Commit and Push Changes') {
            steps {
                dir('Petclinic_Manifest') {
                    withCredentials([usernamePassword(credentialsId: 'petclinic_github', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                        sh """
                            git remote set-url origin https://github.com/ThuanPhuc27/Petclinic_Manifest.git
                            git config user.name "jenkins-bot"
                            git config user.email "jenkins-bot@lptdevops.com"

                            git add .
                            git commit -m "Commented out all YAML files in dev folder"
                            git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/ThuanPhuc27/Petclinic_Manifest.git ${params.branch}
                        """
                    }
                }
            }
        }
    }
}
