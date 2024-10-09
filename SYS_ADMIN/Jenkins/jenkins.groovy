pipeline {
    agent any
    
    tools {
        jdk 'jdk17'
        maven 'maven3'
    }
    
    environment {
        SCANNER_HOME= tool 'sonar-scanner'
    }

    stages {
        
        stage('Git checkout') {
            steps {
               git branch: 'main', credentialsId: 'git-cred', url: 'https://github.com/dianda1/end-to-end-project-repo.git'

            }
        }
        
        stage('Compile') {
            steps {
                sh "mvn compile"
            }
        }
        
        stage('Test') {
            steps {
                sh "mvn test"
            }
        }
        
        stage('File System Scan') {
            steps {
                sh "trivy fs --format table -o trivy-fs-report.html ."
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar') {
                    sh ''' $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName= Corporate-pipeline -Dsonar.projectKey=Corporate-pipeline \
                    -Dsonar.java.binaries=. '''
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token'
                }
            }
        }
        
         
        stage('Build') {
            steps {
               sh "mvn package"  
            }
        }
        
        stage('Publish to Nexus') {
            steps {
                withMaven(globalMavenSettingsConfig: 'type-id-you-can-remember', jdk: 'jdk17', maven: 'maven3', mavenSettingsConfig: '', traceability: true) {
                    sh "mvn deploy"
                }
            }          
        
        }
        
        stage('Build & Tag Docker Image') {
            steps {
               script {
                   withDockerRegistry(credentialsId: 'dockerhub-cred', toolName: 'docker', url: 'docker push dianda87/adoptopenjdk-image:latest') {
                            sh "docker build -t dianda87/boardshack:latest ."
                    }
               }
            }
        }
        
        stage('Docker Image Scan') {
            steps {
                sh "trivy image --format table -o trivy-image-report.html dianda87/adoptopenjdk-image:latest "
            }
        }
        
        stage('Push Docker image') {
            steps {
               script {
                   withDockerRegistry(credentialsId: 'dockerhub-cred', toolName: 'docker', url: 'docker push dianda87/adoptopenjdk-image:latest') {
                       sh "docker push dianda87/boardshack:latest"
                        }
                    }
                
            }
        }

        stage('Deploy To Kubernetes') {
            steps {
                withKubeConfig(caCertificate: '', clusterName: 'kubernetes', contextName: '', credentialsId: 'k8s-cred', namespace: 'webapps', restrictKubeConfigAccess: false, serverUrl: 'https://10.0.1.90:6443') {
                    sh "kubectl apply -f deployment-service.yaml"
                }
                 
            }
        }
        
        stage('Verify Deployment') {
            steps {
                withKubeConfig(caCertificate: '', clusterName: 'kubernetes', contextName: '', credentialsId: 'k8s-cred', namespace: 'webapps', restrictKubeConfigAccess: false, serverUrl: 'https://10.0.1.90:6443') {
                    sh "kubectl get pods -n webapps"
                    sh "kubectl get nodes -n webapps"
                    
                }
                 
            }
        }
        
    }
    post {
    always {
        script {
            def jobName = env.JOB_NAME
            def buildNumber = env.build_number
            def pipelineStatus = currentBuild.result ?: 'UNKNOWN'
            def bannerColor = pipelineStatus.toUpperCase() == 'success' ? 'green' : 'red'

            def body = """
                <html>
                <body>
                <div style ="border: 4px solid ${bannerColor}; padding: 10px;">
                <h2>${jobName} - Build ${buidNumber}</h2>
                <div style = "background-color: ${bannerColor}; padding: 10px;">
                <h3 style ="color: white;">Pipeline Status: ${pipelineStatus.toUpperCase()}</h3>
                </div
                <p>Check the <a href="${BUILD_URL}">console output</a>.</p>
                </div>
                </body>
                </html>
            """
            emailext (
                subject: "${jobName} - Build ${buildNumber} - ${pipelineStatus.toUpperCase()}",
                body: body
                to: 'diandad66@gmail.com'
                from: 'jenkins@example.com'
                replyTo: 'jenkins@example.com'
                mimeType: 'text/html'
                attachmentsPattern: 'trivy-report.html'
            )



        }
    }

    }
    
}
