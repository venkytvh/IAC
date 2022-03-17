def call(Closure body) {
  def configuration = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = configuration
  body()
  pipeline {
    parameters {
      string(name: 'tag', defaultValue: 'v0.04', description: 'tag to deploy')
    }
    agent any
    environment {
      APP_NAME = "${configuration.application_name}"
      APP_VERSION = "${params.tag}"
      ENVIRONMENT="${configuration.environment}"
      CONTEXT_PATH="${configuration.context_path}"
      SSM_PATH="${configuration.ssm_path}"
      TAGGED_IMAGE_NAME = "$APP_NAME:$APP_VERSION"
      ECR = '188367751657.dkr.ecr.ap-south-1.amazonaws.com'
      ECRCRED = "ecr:ap-south-1:pp-kenya-${APP_NAME}"
      IMAGE = "$ECR/$TAGGED_IMAGE_NAME"
      NAMESPACE = "2048-game"
      ENV_FILE = "${ENVIRONMENT}.env"
      SECRETS_FILE = "${ENVIRONMENT}.secrets"
      CONFIG_MAP = "$APP_NAME-config"
      SECRET = "$APP_NAME-secret"
      DEPLOY_ARN_KEY = "${ENVIRONMENT}_DEPLOY_ARN"
      EKS_CLUSTER_NAME_KEY = "${ENVIRONMENT}_EKS_CLUSTER_NAME"
      EKS_ROLE_SESSION_NAME = "eks-role-${ENVIRONMENT}"
      DEPLOY_ARN = sh(returnStdout: true, script: "echo \$$DEPLOY_ARN_KEY")
      EKS_CLUSTER_NAME = sh(returnStdout: true, script: "echo \$$EKS_CLUSTER_NAME_KEY")
    }
    stages {
      stage('Checkout configuration and secrets') {
        steps {
          withCredentials([usernamePassword(credentialsId: "$BITBUCKET_ACCESS_TOKEN_CREDENTIALS_KEY", usernameVariable: "KEY", passwordVariable: "SECRET")]) {
            sh '''
              rm -rf ./environment-configuration || true
              #rm -rf ./environment-secrets || true
              set +x
              ACCESS_TOKEN=$(curl -X POST -u "$KEY:$SECRET" https://bitbucket.org/site/oauth2/access_token -d grant_type=client_credentials | jq -r .access_token) &> /dev/null
              git clone https://x-token-auth:${ACCESS_TOKEN}@bitbucket.org/solvezydevblr/environment-configuration.git
              #git clone https://x-token-auth:${ACCESS_TOKEN}@bitbucket.org/solvezydevblr/environment-secrets.git
              set -x
              cp ./environment-configuration/${ENV_FILE} ./${ENV_FILE}
              #cp ./environment-secrets/${SECRETS_FILE} ./${SECRETS_FILE}
              export AWS_DEFAULT_REGION="ap-south-1"
              aws sts get-caller-identity
              #aws ssm get-parameters-by-path --path /${SSM_PATH} --with-decryption | jq -r '.Parameters| .[] | .Name + "=" + .Value + ""' | tr -d /${SSM_PATH} > ${SECRETS_FILE}
              aws ssm get-parameters-by-path --path /${SSM_PATH} --with-decryption | jq -r '.Parameters| .[] | .Name + "=" + .Value + ""' | cut -d"/" -f3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30 > ${SECRETS_FILE}
              #rm -rf ./environment-secrets
              rm -rf ./environment-configuration
            '''
          }
        }
      }
      stage ('Deploying to Environment') {
        steps {
          script {
            sh '''
              set +x
              source /etc/environment
              tempCred=$(aws sts assume-role --role-arn $DEPLOY_ARN --role-session-name $EKS_ROLE_SESSION_NAME)
              access_key=`echo $tempCred | jq -r .Credentials.AccessKeyId`
              secret_key=`echo $tempCred | jq -r .Credentials.SecretAccessKey`
              session=`echo $tempCred | jq -r .Credentials.SessionToken`
              export AWS_ACCESS_KEY_ID=${access_key}
              export AWS_SECRET_ACCESS_KEY=${secret_key}
              export AWS_SESSION_TOKEN=${session}
              export AWS_DEFAULT_REGION="ap-south-1"
              aws sts get-caller-identity
              set -x
              aws eks --region ap-south-1 update-kubeconfig --name $EKS_CLUSTER_NAME
              kubectl delete deployment pp-kenya-${APP_NAME} || true
              kubectl delete service pp-kenya-${APP_NAME} || true
              kubectl delete configmap pp-kenya-${CONFIG_MAP} || true
              kubectl create configmap pp-kenya-${CONFIG_MAP} --from-env-file ./${ENV_FILE}
              kubectl delete secret pp-kenya-${SECRET}   || true
              kubectl create secret generic pp-kenya-${SECRET} --from-env-file ./${SECRETS_FILE}
              kubectl run pp-kenya-${APP_NAME} --overrides='{"spec":{"template":{"metadata": { "annotations": { "sidecar.istio.io/proxyCPU": "25m", "sidecar.istio.io/proxyMemory": "100Mi" } },"spec":{"containers":[{"name":"'pp-kenya-${APP_NAME}'","image":"'$IMAGE'","resources":{"requests":{"memory":"100Mi","cpu":"25m"},"limits":{"memory":"500Mi","cpu":"500m"}},"readinessProbe": { "httpGet": { "path": "/'${CONTEXT_PATH}'/actuator/health/ping", "port": 8080, "scheme": "HTTP" }, "initialDelaySeconds": 180, "timeoutSeconds": 10, "periodSeconds": 10, "successThreshold": 1, "failureThreshold": 5 },"envFrom":[{"configMapRef":{"name":"'pp-kenya-${CONFIG_MAP}'"}},{"secretRef":{"name":"'pp-kenya-${SECRET}'"}}]}]}}}}' --replicas=1 --labels=app=pp-kenya-${APP_NAME} --image=$IMAGE --port=8080
              kubectl expose deployment pp-kenya-${APP_NAME} --type=ClusterIP --name=pp-kenya-${APP_NAME} --port 8080
              kubectl annotate --overwrite svc pp-kenya-${APP_NAME} prometheus.io/scrape="true" prometheus.io/path="/${CONTEXT_PATH}/actuator/prometheus" prometheus.io/port="8080"
            '''
          }
        }
      }
      stage('Clean up configuration and secrets') {
        steps {
          sh("rm ./${ENV_FILE}")
          sh("rm ./${SECRETS_FILE}")
        }
      }
    }
  }

}
