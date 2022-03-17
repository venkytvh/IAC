pipeline 
{
    agent any 
    stages 
	{
        stage('Pre-Requisites')
        {
          steps {
              script {
              echo "Database Backup"
              sh '''
              echo $db
               echo $curlpre
              databasename=${db// /%20}
              curlpreexecutor=${curlpre// /%20}
              echo $databasename
              if [ ! -z "$db" -a "$db" != " " ] ; then
                     echo "if condition"
                curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/Documentdb_ondemand_backup/buildWithParameters?db=$databasename"
                sleep 10
                 BUILD_STATUS=$(curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/Documentdb_ondemand_backup/lastBuild/api/json"| jq -r '.result')
             echo "The Build status is $BUILD_STATUS"
             sleep 10
                    curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "The database backup has taken '"${databasename}:${BUILD_STATUS}"' ",
                             
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
              else
                  echo "else condition"
                   curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "There is no request for database backup",
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
              fi
               if  [ ! -z "$curlpre" -a "$curlpre" != " " ] ; then
                     echo "elif condition"
                curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/curlexecutor/buildWithParameters?curl=$curlpreexecutor&email=$employeeemail&jiraid=$jiraid"
                sleep 20
                 BUILD_STATUS=$(curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/curlexecutor/lastBuild/api/json"| jq -r '.result')
             echo "The Build status is $BUILD_STATUS"
                    curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "Check your email for the Pre-curl execution response file '"${BUILD_STATUS}"' ",
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
            else
                  echo "else condition"
                   curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "There is no request for Pre-curl execution",
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
            fi
              '''
              }
          }
        }
        stage('Service Tags') 
		{
          steps {
            script {
                echo "build"
				sh '''
                for uitag in $tags
                do
                    job=$(echo $uitag | cut -d ':' -f 1)
                    tag=$(echo $uitag | cut -d ':' -f 2)
                    echo "The job name $job"
                    echo "The tag value is $tag"
                    curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/prod_deploy_$job/buildWithParameters?tag=$tag"
                    sleep 8
                done
                sleep 240
                '''
            }
          }
        } 
        stage('Service Tag jira notification')  
        {
            steps {
            sh '''
            str=""
             for uitag in $tags
             do
             job=$(echo $uitag | cut -d ':' -f 1)
             tag=$(echo $uitag | cut -d ':' -f 2)
             curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/prod_deploy_$job//lastBuild/consoleText" > out.txt
             status=$(cat out.txt | grep -iw Finished | cut -d " " -f 3)
             context=$(cat out.txt | grep -iw httpget | cut -d "," -f 7 | cut -d ":" -f 4 )
             contextpath=$(echo${context%%/actuator*} | tr -d '"/' )
             swagger=https://apis.solvezy.com/monitoring/$contextpath/swagger-ui.html
             if [ -z $status ];then
             status=incorrectservicename
             swagger=noswaggerlink
             fi
             echo $
             str+="$job:$tag:$status:$swagger "
             echo "The Build status is $str"
             rm out.txt
             done
             curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "The execution status is '"${str}"' ",
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
                '''
            }
        }
        stage('Post-Requisites') 
		{
             steps {
              script {
              sh '''
              echo $curlpost
              curlpostexecutor=${curlpre// /%20}
            if  [ ! -z "$curlpost" -a "$curlpost" != " " ] ; then
                     echo "if condition"
                curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/curlexecutor/buildWithParameters?curl=$curlpostexecutor&email=$employeeemail&jiraid=$jiraid"
                sleep 20
                 BUILD_STATUS=$(curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/curlexecutor/lastBuild/api/json"| jq -r '.result')
             echo "The Build status is $BUILD_STATUS"
                    curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "Check your email for the Post-curl response file '"${BUILD_STATUS}"' ",
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
              else
                  echo "else condition"
                   curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/3/issue/$jiraid/comment" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
            --data '{
                "body": {
                "type": "doc",
                "version": 1,
                "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                             "text": "There is no request for Post-curl execution",
                             "type": "text"
						}
						]		
				}	
				]
			}
			}'
              fi

              '''
              }
              
             }
        } 
        stage('Email notification')
        {
        steps {
            sh '''
             str=""
             for uitag in $tags
             do
             job=$(echo $uitag | cut -d ':' -f 1)
             tag=$(echo $uitag | cut -d ':' -f 2)
             curl -X POST -u admin:Standard@1234 "https://jenkins-prod.solvezy.net/job/prod_deploy_$job//lastBuild/consoleText" > out.txt
             status=$(cat out.txt | grep -iw Finished | cut -d " " -f 3)
             context=$(cat out.txt | grep -iw httpget | cut -d "," -f 7 | cut -d ":" -f 4 )
             contextpath=$(echo${context%%/actuator*} | tr -d '"/' )
             swagger=https://apis.solvezy.com/monitoring/$contextpath/swagger-ui.html
             if [ -z $status ];then
             status=incorrectservicename
             swagger=noswaggerlink
             fi
             str+="$job:$tag:$status:$swagger\n"
             done
             echo -e $str
                echo -e "The services are deployed in Prod\n Service:Tag:Status:Sawagger\n${str}" |  mail -s "Service has been Deployed" "${employeeemail}" badal.sahu@solvezy.com
             '''
          }
        }
        stage('Update ticket status')
        {
        steps {
            sh '''
             curl --request POST \
                --url "https://solvindia.atlassian.net/rest/api/2/issue/$jiraid/transitions" \
                --user 'devops-support@solvezy.com:utADnvBJANg6E50nSQWz3068' \
                --header 'Accept: application/json' \
                --header 'Content-Type: application/json' \
                --data '{
                "transition": {
                "id": "71"
                }
             }'
             '''
          }
        }
    }
}