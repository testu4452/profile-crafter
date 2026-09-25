pipeline {
    agent any

    environment {
        TARGET_HOST = '10.73.76.154'
        TARGET_USER = 'stryker'
        SSH_PASS    = 'GslabGavs@4231'

        REPO_URL    = 'https://github.com/testu4452/profile-crafter.git'
        REPO_BRANCH = 'main'

        APP_DIR     = '/tmp/profile_k8s'

        IMAGE_NAME  = '10.73.76.154:5000/profile-app'
        IMAGE_TAG   = "${BUILD_NUMBER}"
    }

    options {
        timestamps()
        ansiColor('xterm')
    }

    stages {

        stage('Checkout') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    rm -rf ${APP_DIR}
                    git clone -b ${REPO_BRANCH} ${REPO_URL} ${APP_DIR}
                    echo "Repository cloned successfully"
                '
                """
            }
        }

        stage('Verify Environment') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    java -version
                    javac -version
                    docker --version
                    kubectl version --client || true
                '
                """
            }
        }

        stage('Build Application') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    cd ${APP_DIR}

                    if [ -f gradlew ]; then
                        chmod +x gradlew
                        ./gradlew clean bootJar -x test -x compileTestJava
                    elif [ -f pom.xml ]; then
                        mvn clean package -DskipTests
                    else
                        echo "No supported build tool found"
                        exit 1
                    fi
                '
                """
            }
        }

        stage('Locate Jar') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    cd ${APP_DIR}
                    echo "Generated JAR files:"
                    find build/libs -name "*.jar"
                '
                """
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    cd ${APP_DIR}

                    docker build \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest .
                '
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${IMAGE_NAME}:latest

                    echo "Registry Images:"
                    curl -s http://localhost:5000/v2/_catalog || true
                '
                """
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    cd ${APP_DIR}

                    cat > deployment.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: profile-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: profile-app
  template:
    metadata:
      labels:
        app: profile-app
    spec:
      containers:
      - name: profile-app
        image: ${IMAGE_NAME}:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: profile-app-service
spec:
  selector:
    app: profile-app
  ports:
  - port: 80
    targetPort: 8080
  type: NodePort
EOF

                    kubectl apply -f deployment.yaml

                    kubectl rollout status deployment/profile-app --timeout=300s

                    kubectl get pods
                    kubectl get svc
                '
                """
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully.'
        }

        failure {
            echo '❌ Pipeline failed.'
        }

        always {
            cleanWs(deleteDirs: true)
        }
    }
}
