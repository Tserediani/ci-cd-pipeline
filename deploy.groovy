pipeline {
    agent any
    environment {
        NEXUS_REGISTRY = 'host.docker.internal:8082'
        IMAGE_BASE = 'my-python-app'
        VERSION_ID = "${params.version_id}"
        FASTAPI_APP_ID = '910fb03e-7bc1-4c67-92a1-58362e57827a'
        CONTAINER_URL = "${NEXUS_REGISTRY}/${IMAGE_BASE}:${VERSION_ID}"
    }
    stages {
        stage('Get latest image from the registry') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-docker-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASSWORD'
                )]) {
                    echo 'Logging into Nexus Registry...'
                    // 🔒 The Proper Way: Triple single quotes protect your secrets natively
                    sh 'echo $NEXUS_PASSWORD | docker login $NEXUS_REGISTRY -u $NEXUS_USER --password-stdin'
                    sh 'echo "Getting the image"'
                    sh "docker pull $CONTAINER_URL"
                    sh "docker logout $NEXUS_REGISTRY"
                }
            }
        }
        stage('Deploy to FastAPI Cloud') {
            steps {
                withCredentials([string(credentialsId: 'fastapi-app', variable: 'FASTAPI_TOKEN')]) {
                    echo 'Launching deployment workspace inside a container...'
                    sh '''
                                docker run --rm \
                                -e FASTAPI_CLOUD_TOKEN="$FASTAPI_TOKEN" \
                                -e FASTAPI_CLOUD_APP_ID="$FASTAPI_APP_ID" \
                                $CONTAINER_URL \
                                sh -c "uv run fastapi deploy"
                            '''
                    echo 'FastAPI Cloud deployment successfully triggered!'
                }
            }
        }
    }
}
