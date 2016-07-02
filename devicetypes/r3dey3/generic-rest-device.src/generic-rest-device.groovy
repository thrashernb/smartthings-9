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
// Allow parent to log when we call it
def log(msg) {
	//log.debug "From Parent -> $msg"
}
////////////////////////////////////////////////////////////////////////////////////////////////////
// Communication functions
private GET() {
    parent.GET(this, "/status")
}
private POST(args=[]) {
	parent.POST(this, '/control', args)
}
/////////////////////////////////////////////////////////////////////////////////////////////////////
//helper methods
private getCallBackAddress() {
	def cb = device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
    log.debug cb
    return cb
}

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Refresh interface
def refresh() {
	log.trace "refresh()"
    GET()
}
/////////////////////////////////////////////////////////////////////////////////////////////////////
// Poll interface
def poll() {
	log.trace "poll()"
	GET()
}