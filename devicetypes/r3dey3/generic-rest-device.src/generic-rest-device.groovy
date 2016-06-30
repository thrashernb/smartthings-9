/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Author: Kenny K
 */
metadata {
	definition (name: "Generic REST Device", namespace: "r3dey3", author: "Kenny Keslar") {
        capability "Polling"
        
        capability "Refresh"
        command "log", ["string"]
        command "sync", ["string","string"]
        command "parseResponse"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
	}
	main(["refresh"])
	details(["refresh"])
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Response Parsing
def parse(description) {
	log.trace "parse($description)"
}

def parseResponse(resp) {
	def json = resp.json
    log.debug "JSON Result: $json"
    json.each {
    	def n = it.key
        if (n != "status") {
            def v = it.value
            sendEvent(name: n, value: v)
        }
    }
}    
////////////////////////////////////////////////////////////////////////////////////////////////////
// Communication functions
private GET() {
    log.trace "Calling parent.GET  - ${parent.GET}"
    parent.GET(this, "/status")
}
private POST(args=[]) {
	parent.POST(this, '/control', args)
}
private SUBSCRIBE() {
	parent.SUBSCRIBE(this, '/subscribe', getCallBackAddress())
}
/////////////////////////////////////////////////////////////////////////////////////////////////////
//helper methods
private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
def getHostAddress() {
    return getDataValue("ip")+":"+getDataValue("port")
    
}
private getCallBackAddress() {

	def cb = device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
    log.debug cb
    return cb
}

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Switch commands
def on() { return POST([switch: "on"]) }
def off() { return POST([switch: "off"]) }

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Refresh interface
def refresh() {
	log.trace "refresh()"
    GET()
    SUBSCRIBE()
}
/////////////////////////////////////////////////////////////////////////////////////////////////////
// Poll interface
def poll() {
	log.trace "poll()"
	return GET()
}
def log(msg) {
 log.debug "From Parent-> "+ msg
 return null
}
 
def sync(ip, port) {
	def existingIp = getDataValue("ip")
	def existingPort = getDataValue("port")
	if (ip && ip != existingIp) {
		updateDataValue("ip", ip)
	}
	if (port && port != existingPort) {
		updateDataValue("port", port)
	}
}