@Library('utils') import devops.utils.httpLib
def String localFilePath = "./nodes.json"

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
                  print("localnodes: ",localNodes)
                  print("remotenodes: ",remoteNodes)
                  print("localfilepath: ",localFilePath)
                  util.saveLocal(localNodes,remoteNodes,localFilePath)
                }
            }
        }
    }
}