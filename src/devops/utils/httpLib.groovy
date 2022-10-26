#!/usr/bin/env groovy

package devops.utils

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

class httpLib {
    httpLib(){}

    def getLocalNodes(String filePath){
        exceptionwhilereading = false
        def String localJson
        def localArray
        try{
            localJson = readFile(file: filePath, encoding: "utf8")
        }catch(e){
            localArray = [["Node":"builtin", "Tries":0]]
            exceptionwhilereading = true
            println("Got an exception: ${e}")
        }
        if(!exceptionwhilereading){
            localArray = readJSON text: localJson
        }
        return localArray
    }

    def checkThreshold(local,String node){
        shouldRemove = false
        for(z = 0; z < local.size(); z++){
            if(local[z].Node == node){
                if(local[z].Tries == 3){
                    shouldRemove = true
                    break
                }
            }
        }
        return shouldRemove
    }

    def saveLocal(local,remote,String filePath){
        list = []
        for(a = 0; a < local.size(); a++){
            for(c = 0; c < remote.computer.size(); c++){
                remoteComp = remote.computer[c]
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    node = remoteComp.displayName
                    if(local[a].Node == node){
                        if(local[a].Tries != 3){
                            element = [:]
                            element.Node = local[a].Node
                            element.Tries = local[a].Tries+1
                            list.add(element)
                        }
                        break
                    }
                }
            }
        }
        for(cd = 0; cd < remote.computer.size(); cd++){
            remoteComp = remote.computer[cd]
            remoteFound = false
            for(ab = 0; ab < local.size(); ab++){
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    node = remoteComp.displayName
                    if(local[ab].Node == node){
                        remoteFound = true
                        break
                    }
                }
            }
            if(!remoteFound){
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    map = [:]
                    map.Node = remoteComp.displayName
                    map.Tries = 1
                    list.add(map)
                }
            }
        }
        try{
            text = JsonOutput.toJson(list).toString()   
        }catch(e){
            println("Caught an exception on converting to json: ${e}")
        }
        try{
            writeFile(text: text, file: filePath, encoding: "utf8")   
        }catch(e){
            println("Caught an exception: ${e}")
        }
    }

    def getRemoteNodes(String serverAddress,String userNameToken){
        def url = "${serverAddress}/computer/api/json?pretty=true"
        def basicAuth = 'Basic '+"${userNameToken}".getBytes('iso-8859-1').encodeBase64()
        def jsonResponse = httpRequest customHeaders: [[name: 'Authorization', value: basicAuth]], url: url, quiet: true
        
        def resultMap =  readJSON text: jsonResponse.content
        return resultMap
    }

    def getRemovableNodes(remoteNodes,localNodes,filePath){
        def array = []
        for(i=0; i < remoteNodes.computer.size(); i++){
            if(remoteNodes.computer[i].offline && !remoteNodes.computer[i].temporarilyOffline){
                shouldRemove = checkThreshold(localNodes,remoteNodes.computer[i].displayName)
                if (shouldRemove){
                    map = [:]
                    map.Name = remoteNodes.computer[i].displayName
                    map.offlineReason = remoteNodes.computer[i].offlineCauseReason
                    map.temporaryOffline = remoteNodes.computer[i].temporarilyOffline
                    array.add(map)
                }
            }
        }
        return array
    }

    def removeNode(String serverAddress, String userNameToken, String node){
        def url = "${serverAddress}/computer/${node}/doDelete"
        def basicAuth = 'Basic '+"${userNameToken}".getBytes('iso-8859-1').encodeBase64()
        def jsonResponse = httpRequest customHeaders: [[name: 'Authorization', value: basicAuth]], url: url, quiet: true, httpMode: 'POST'
        if(jsonResponse.status != 302){
            throw("Cannot remove node: ${jsonResponse.content}")
        }
    }
}