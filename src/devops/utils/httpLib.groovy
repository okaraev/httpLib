#!/usr/bin/env groovy

package devops.utils

import groovy.json.*

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

class httpLib implements Serializable {
    def steps
    def localFilePath
    def jenkinsUserToken
    def jenkinsServerAddress
    httpLib(steps,localFile,userToken,serverAddress) {
        this.steps = steps
        this.localFilePath = localFile
        this.jenkinsUserToken = userToken
        this.jenkinsServerAddress = serverAddress
    }

    def getLocalNodes(){
        def exceptionwhilereading = false
        def String localJson
        def localArray
        try{
            localJson = steps.readFile(file: this.localFilePath, encoding: "utf8")
        }catch(e){
            localArray = [["Node":"builtin", "Tries":0]]
            exceptionwhilereading = true
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

    def saveLocal(local,remote){
        def list = []
        for(def a = 0; a < local.size(); a++){
            for(def c = 0; c < remote.computer.size(); c++){
                def remoteComp = remote.computer[c]
                if(remoteComp.offline && !remoteComp.temporarilyOffline){
                    if(local[a].Node == remoteComp.displayName){
                        if(local[a].Tries != 3){
                            def element = [:]
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
        steps.writeFile(text: text, file: this.localFilePath, encoding: "utf8")
    }

    def getRemoteNodes(){
        def url = "${this.jenkinsServerAddress}/computer/api/json?pretty=true"
        def basicAuth = 'Basic '+"${this.jenkinsUserToken}".getBytes('iso-8859-1').encodeBase64()
        def jsonResponse = steps.httpRequest(customHeaders: [[name: 'Authorization', value: basicAuth]], url: url, quiet: true)
        
        def resultMap = steps.readJSON text: jsonResponse.content
        return resultMap
    }

    def getRemovableNodes(remoteNodes,localNodes){
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

    def removeNode(String node){
        def url = "${this.jenkinsServerAddress}/computer/${node}/doDelete"
        def basicAuth = 'Basic '+"${this.jenkinsUserToken}".getBytes('iso-8859-1').encodeBase64()
        def jsonResponse = steps.httpRequest(customHeaders: [[name: 'Authorization', value: basicAuth]], url: url, quiet: true, httpMode: 'POST')
        if(jsonResponse.status != 302){
            throw("Cannot remove node: ${jsonResponse.content}")
        }
    }

    def removeOfflineNodes(){
        def localNodes = this.getLocalNodes()
        def remoteNodes = this.getRemoteNodes()
        def nodes = this.getRemovableNodes(remoteNodes,localNodes)
        for(def b=0; b < nodes.size(); b++){
            this.removeNode(nodes[b].Name)
        }
        this.saveLocal(localNodes,remoteNodes)
    }
}