#!/usr/bin/env groovy

package devops.utils

import groovy.json.*

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

class httpLib implements Serializable {
    def steps
    httpLib(steps) {this.steps = steps}

    def getLocalNodes(String filePath){
        def exceptionwhilereading = false
        def String localJson
        def localArray
        try{
            localJson = steps.readFile(file: filePath, encoding: "utf8")
        }catch(e){
            localArray = [["Node":"builtin", "Tries":0]]
            exceptionwhilereading = true
            println("Got an exception: ${e}")
        }
        if(!exceptionwhilereading){
            localArray = steps.readJSON text: localJson
        }
        return localArray
    }

    def checkThreshold(local,String node){
        def shouldRemove = false
        for(def z = 0; z < local.size(); z++){
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
        def list = []
        for(def a = 0; a < local.size(); a++){
            for(def c = 0; c < remote.computer.size(); c++){
                def remoteComp = remote.computer[c]
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    if(local[a].Node == remoteComp.displayName){
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
        for(def cd = 0; cd < remote.computer.size(); cd++){
            def remoteComp = remote.computer[cd]
            def remoteFound = false
            for(def ab = 0; ab < local.size(); ab++){
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    if(local[ab].Node == remoteComp.displayName){
                        remoteFound = true
                        break
                    }
                }
            }
            if(!remoteFound){
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    def map = [:]
                    map.Node = remoteComp.displayName
                    map.Tries = 1
                    list.add(map)
                }
            }
        }
        def text = JsonOutput.toJson(list).toString()
        steps.writeFile(text: text, file: filePath, encoding: "utf8")
    }

    def getRemoteNodes(String serverAddress,String userNameToken){
        def url = "${serverAddress}/computer/api/json?pretty=true"
        def basicAuth = 'Basic '+"${userNameToken}".getBytes('iso-8859-1').encodeBase64()
        def jsonResponse = steps.httpRequest(customHeaders: [[name: 'Authorization', value: basicAuth]], url: url, quiet: true)
        
        def resultMap = steps.readJSON text: jsonResponse.content
        return resultMap
    }

    def getRemovableNodes(remoteNodes,localNodes,filePath){
        def array = []
        for(def i=0; i < remoteNodes.computer.size(); i++){
            if(remoteNodes.computer[i].offline && !remoteNodes.computer[i].temporarilyOffline){
                if (this.checkThreshold(localNodes,remoteNodes.computer[i].displayName)){
                    def map = [:]
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
        def jsonResponse = steps.httpRequest(customHeaders: [[name: 'Authorization', value: basicAuth]], url: url, quiet: true, httpMode: 'POST')
        if(jsonResponse.status != 302){
            throw("Cannot remove node: ${jsonResponse.content}")
        }
    }
}