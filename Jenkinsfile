pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kaniko
    image: harbor.harbor.svc.cluster.local/gcr-proxy/kaniko-project/executor:debug
    command:
    - sleep
    args:
    - "9999999"
    volumeMounts:
    - name: docker-config
      mountPath: /kaniko/.docker
  volumes:
  - name: docker-config
    secret:
      secretName: harbor-kaniko
      items:
      - key: .dockerconfigjson
        path: config.json
'''
        }
    }

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref'],
                [key: 'ref_type', value: '$.ref_type'],
                [key: 'repository_name', value: '$.repository.name']
            ],
            token: 'erp-api-webhook',
            printContributedVariables: true,
            printPostContent: true,
            silentResponse: false,
            regexpFilterText: '$ref_type',
            regexpFilterExpression: '^tag$'
        )
    }

    environment {
        HARBOR_URL = '192.168.1.82:30002'
        HARBOR_PROJECT = 'morning-harvest'
        IMAGE_NAME = 'erp-api'
    }

    stages {
        stage('Info') {
            steps {
                script {
                    // 從 webhook payload 取得 tag 名稱
                    env.TAG_VERSION = env.ref ?: 'unknown'
                    echo "Harbor: ${env.HARBOR_URL}/${env.HARBOR_PROJECT}/${env.IMAGE_NAME}"
                    echo "Tag Version: ${env.TAG_VERSION}"
                }
            }
        }

        stage('Build and Push with Kaniko') {
            steps {
                container('kaniko') {
                    sh """
                        /kaniko/executor \
                            --context=dir://\${WORKSPACE} \
                            --dockerfile=\${WORKSPACE}/Dockerfile \
                            --destination=\${HARBOR_URL}/\${HARBOR_PROJECT}/\${IMAGE_NAME}:\${TAG_VERSION} \
                            --destination=\${HARBOR_URL}/\${HARBOR_PROJECT}/\${IMAGE_NAME}:latest \
                            --insecure \
                            --skip-tls-verify
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Successfully built and pushed ${env.IMAGE_NAME}:${env.TAG_VERSION} and ${env.IMAGE_NAME}:latest"
        }
        failure {
            echo "Build failed for ${env.IMAGE_NAME}:${env.TAG_VERSION}"
        }
    }
}
