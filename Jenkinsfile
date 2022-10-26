@Library('utils') import devops.utils.httpLib

def util = new httpLib(this)


pipeline{
    agent{
        label 'builtin'
    }

    stages{
        stage('Checkout'){
            steps{
                script{
                  localNodes = util.getLocalNodes(localFilePath)
                  remoteNodes = util.getRemoteNodes("http://localhost:8080","okaraev:1103697b3d2ff26b183b82eaf9ef849d67")
                  nodes = util.getRemovableNodes(remoteNodes,localNodes,localFilePath)
                  for(b=0; b < nodes.size(); b++){
                      util.removeNode("http://localhost:8080","okaraev:1103697b3d2ff26b183b82eaf9ef849d67",nodes[b].Name)
                  }
                  util.saveLocal(localNodes,remoteNodes,localFilePath)
                }
            }
        }
    }
}