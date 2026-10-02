pipeline {
    agent any

    environment {
        TARGET_HOST = '10.73.76.154'
        TARGET_USER = 'stryker'
        SSH_PASS    = 'GslabGavs@4231'

        REPO_URL    = 'https://github.com/testu4452/profile-crafter.git'
        REPO_BRANCH = 'main'

        APP_DIR     = '/tmp/profile_k8s'

        IMAGE_NAME  = 'ghcr.io/testu4452/profile-crafter'
        IMAGE_TAG   = "${BUILD_NUMBER}"
    }

    options {
        timestamps()
        ansiColor('xterm')
    }

    stages {

        stage('Checkout Repository') {
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
                    echo "===== JAVA ====="
                    java -version || true

                    echo "===== DOCKER ====="
                    docker --version || true

                    echo "===== KUBECTL ====="
                    kubectl version --client || true
                '
                """
            }
        }

        stage('Build Application') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    set -e

                    cd ${APP_DIR}

                    if [ -f gradlew ]; then
                        chmod +x gradlew
                        ./gradlew clean bootJar -x test
                    elif [ -f pom.xml ]; then
                        mvn clean package -DskipTests
                    else
                        echo "No supported build tool found"
                        exit 1
                    fi

                    echo "Build completed"
                '
                """
            }
        }

        stage('Verify Jar') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    cd ${APP_DIR}

                    echo "Generated artifacts:"
                    find build/libs -name "*.jar" -type f
                '
                """
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    set -e

                    cd ${APP_DIR}

                    docker build \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest .
                '
                """
            }
        }

        stage('Login and Push to GHCR') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-creds',
                        usernameVariable: 'GH_USER',
                        passwordVariable: 'GH_TOKEN'
                    )
                ]) {

                    sh """
                    sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} "
                        echo '${GH_TOKEN}' | docker login ghcr.io \
                            -u '${GH_USER}' \
                            --password-stdin

                        docker push ${IMAGE_NAME}:${IMAGE_TAG}

                        docker push ${IMAGE_NAME}:latest

                        docker logout ghcr.io
                    "
                    """
                }
            }
        }

        stage('Create GHCR Pull Secret') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-creds',
                        usernameVariable: 'GH_USER',
                        passwordVariable: 'GH_TOKEN'
                    )
                ]) {

                    sh """
                    sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} "
                        kubectl create secret docker-registry ghcr-secret \
                            --docker-server=ghcr.io \
                            --docker-username='${GH_USER}' \
                            --docker-password='${GH_TOKEN}' \
                            --dry-run=client -o yaml | kubectl apply -f -
                    "
                    """
                }
            }
        }

        stage('Deploy To Kubernetes') {
            steps {
                sh """
                sshpass -p '${SSH_PASS}' ssh -o StrictHostKeyChecking=no ${TARGET_USER}@${TARGET_HOST} '
                    cat > /tmp/profile-deployment.yaml <<EOF
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
      imagePullSecrets:
      - name: ghcr-secret

      containers:
      - name: profile-app
        image: ${IMAGE_NAME}:${IMAGE_TAG}
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
  - protocol: TCP
    port: 80
    targetPort: 8080

  type: NodePort
EOF

                    kubectl apply -f /tmp/profile-deployment.yaml

                    kubectl rollout restart deployment/profile-app || true

                    kubectl rollout status deployment/profile-app --timeout=300s

                    echo "===== PODS ====="
                    kubectl get pods -o wide

                    echo "===== SERVICES ====="
                    kubectl get svc
                '
                """
            }
        }
    }

    post {

        success {
            echo '✅ Application successfully built, pushed to GHCR and deployed to Kubernetes.'
        }

        failure {
            echo '❌ Pipeline failed.'
        }

        always {
            cleanWs(deleteDirs: true)
        }
    }
}
