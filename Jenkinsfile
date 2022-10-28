@Library('utils') import devops.utils.httpLib
def util = new httpLib(this,"./nodes.json","okaraev:1103697b3d2ff26b183b82eaf9ef849d67","http://localhost:8080")

pipeline{
    agent{
        label 'builtin'
    }

    stages{
        stage('Checkout'){
            steps{
                script{
                  util.removeOfflineNodes()
                }
            }
        }
    }
}