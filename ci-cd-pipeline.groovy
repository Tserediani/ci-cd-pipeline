pipeline {
    agent any

    environment {
        // 🌐 FIXED: Changed 'localhost' to the host shortcut so Jenkins container can find Nexus
        NEXUS_REGISTRY = 'host.docker.internal:8082'

        // 🏷️ FIXED: Separated base name from tags to prevent multi-tag naming errors
        IMAGE_BASE     = 'my-python-app'
        IMAGE_TAG      = "${BUILD_NUMBER}"
        CONTAINER_NAME = 'app-test-runner'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Pulling latest code from GitHub...'
                git branch: 'main', url: 'https://github.com/Tserediani/ci-cd-pipeline'
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'
                // Builds a clean tag for local testing
                sh "docker build -t ${IMAGE_BASE}:local ."
            }
        }

        stage('Docker Test') {
            steps {
                echo 'Running tests inside the Docker container...'
                sh "docker run --name ${CONTAINER_NAME} --rm ${IMAGE_BASE}:local uv run pytest"
            }
        }

        stage('Push to Nexus Registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-docker-credentials',
                                         usernameVariable: 'NEXUS_USER',
                                         passwordVariable: 'NEXUS_PASSWORD')]) {
                    echo 'Logging into Nexus Registry...'
                    // 🔒 The Proper Way: Triple single quotes protect your secrets natively
                    sh '''
                echo "$NEXUS_PASSWORD" | docker login $NEXUS_REGISTRY -u $NEXUS_USER --password-stdin

                echo "Tagging image for Nexus..."
                docker tag $IMAGE_BASE:local $NEXUS_REGISTRY/$IMAGE_BASE:$IMAGE_TAG
                docker tag $IMAGE_BASE:local $NEXUS_REGISTRY/$IMAGE_BASE:latest

                echo "Pushing images..."
                # 🚀 Pushing both in one go prevents Docker from resetting the connection
                docker push $NEXUS_REGISTRY/$IMAGE_BASE:$IMAGE_TAG
                docker push $NEXUS_REGISTRY/$IMAGE_BASE:latest

                docker logout $NEXUS_REGISTRY
            '''
                                         }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up local build artifacts...'
            sh """
                docker ps -a -q --filter name=${CONTAINER_NAME} | xargs -r docker rm -f
            """
        }
        success {
            echo 'Build and tests passed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check the container test logs above.'
        }
    }
}
