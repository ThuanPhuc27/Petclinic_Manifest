pipeline {
    agent any

    parameters {
        string(name: 'admin_server_branch', defaultValue: 'main', description: 'Branch cho admin-server')
        string(name: 'api_gateway_branch', defaultValue: 'main', description: 'Branch cho api-gateway')
        string(name: 'config_server_branch', defaultValue: 'main', description: 'Branch cho config-server')
        string(name: 'customer_service_branch', defaultValue: 'main', description: 'Branch cho customer-service')
        string(name: 'discovery_server_branch', defaultValue: 'main', description: 'Branch cho discovery-server')
        string(name: 'genai_service_branch', defaultValue: 'main', description: 'Branch cho genai-service')
        string(name: 'vets_service_branch', defaultValue: 'main', description: 'Branch cho vets-service')
        string(name: 'visits_service_branch', defaultValue: 'main', description: 'Branch cho visits-service')
    }

    environment {
        DOCKERHUB_USERNAME = 'thuanlp' // Thay bằng Docker Hub username của bạn
        SPRING_REPO = 'https://github.com/ThuanPhuc27/spring-petclinic-microservices.git'
        MANIFEST_REPO = 'https://github.com/ThuanPhuc27/Petclinic_Manifest.git' // Repo manifest của bạn
    }

    stages {
        stage('Checkout Manifest Repo') {
            steps {
                git branch: 'main', credentialsId: 'github', url: "${MANIFEST_REPO}"
            }
        }

        stage('Build and Push Docker Image Tags') {
            steps {
                script {
                    def services = [
                        [name: "admin-server", branch: params.admin_server_branch],
                        [name: "api-gateway", branch: params.api_gateway_branch],
                        [name: "config-server", branch: params.config_server_branch],
                        [name: "customer-service", branch: params.customer_service_branch],
                        [name: "discovery-server", branch: params.discovery_server_branch],
                        [name: "genai-service", branch: params.genai_service_branch],
                        [name: "vets-service", branch: params.vets_service_branch],
                        [name: "visits-service", branch: params.visits_service_branch]
                    ]

                    def commitMessages = []

                    services.each { service ->
                        echo "Updating ${service.name} from branch: ${service.branch}"

                        def imageTag = ""

                        if (service.branch == 'main') {
                            imageTag = 'latest'
                        } else {
                            // Lấy commit ID từ repo spring-petclinic-microservices
                            imageTag = sh(script: "git ls-remote ${SPRING_REPO} ${service.branch} | awk '{print \$1}' | cut -c1-7", returnStdout: true).trim()
                        }

                        echo "Using image tag: ${imageTag}"

                        // Cập nhật image tag vào deployment.yaml của service
                        sh """
                        sed -i 's|image:.*|image: ${DOCKERHUB_USERNAME}/spring-petclinic-${service.name}:${imageTag}|' dev/${service.name}/deployment.yml
                        """

                        // Thêm mô tả ngắn gọn vào commit message
                        commitMessages.add("${service.name}:${imageTag}")
                    }

                    // Commit tất cả thay đổi với mô tả ngắn gọn
                    def commitMessage = commitMessages.join(', ')
                    sh """
                    git add .
                    git commit -m "Update Docker tags: ${commitMessage}"
                    git push origin main
                    """
                }
            }
        }
    }
}
