pipeline {
  agent any

  environment {
    APP_NAME = 'profile-crafter'
    IMAGE_REPO = "${env.DOCKERHUB_REPO ?: 'your-dockerhub-username/profile-crafter'}"
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    K8S_NAMESPACE = "${env.K8S_NAMESPACE ?: 'default'}"
    KUBECONFIG_CRED = "${env.KUBECONFIG_CRED ?: 'kubeconfig'}"
  }

  options {
    timestamps()
    ansiColor('xterm')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Detect Build Tool') {
      steps {
        script {
          if (fileExists('pom.xml')) {
            env.BUILD_TOOL = 'maven'
          } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
            env.BUILD_TOOL = 'gradle'
          } else if (fileExists('package.json')) {
            env.BUILD_TOOL = 'npm'
          } else {
            error 'No supported build descriptor found (pom.xml, build.gradle, package.json)'
          }
          echo "Detected build tool: ${env.BUILD_TOOL}"
        }
      }
    }

    stage('Build') {
      steps {
        script {
          if (env.BUILD_TOOL == 'maven') {
            sh 'mvn -B clean package -DskipTests'
          } else if (env.BUILD_TOOL == 'gradle') {
            sh './gradlew clean build -x test || gradle clean build -x test'
          } else if (env.BUILD_TOOL == 'npm') {
            sh 'npm ci || npm install'
            sh 'npm run build --if-present'
          }
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        sh '''#!/bin/bash -e
          docker build -t ${IMAGE_REPO}:${IMAGE_TAG} .
          docker tag ${IMAGE_REPO}:${IMAGE_TAG} ${IMAGE_REPO}:latest
        '''
      }
    }

    stage('Push Docker Image') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''#!/bin/bash -e
            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
            docker push ${IMAGE_REPO}:${IMAGE_TAG}
            docker push ${IMAGE_REPO}:latest
            docker logout
          '''
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KCFG')]) {
          sh '''#!/bin/bash -e
            export KUBECONFIG=${KCFG}
            kubectl apply -f k8s/namespace.yaml || true
            kubectl apply -f k8s/deployment.yaml -n ${K8S_NAMESPACE}
            kubectl apply -f k8s/service.yaml -n ${K8S_NAMESPACE}
            if [ -f k8s/ingress.yaml ]; then
              kubectl apply -f k8s/ingress.yaml -n ${K8S_NAMESPACE}
            fi
            kubectl set image deployment/${APP_NAME} ${APP_NAME}=${IMAGE_REPO}:${IMAGE_TAG} -n ${K8S_NAMESPACE}
            kubectl rollout status deployment/${APP_NAME} -n ${K8S_NAMESPACE} --timeout=180s
          '''
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: '**/target/*.jar, **/build/libs/*.jar, **/dist/**', allowEmptyArchive: true
    }
    success {
      echo 'Pipeline completed successfully.'
    }
    failure {
      echo 'Pipeline failed.'
    }
  }
}
