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
	definition (name: "REST Switch", namespace: "r3dey3", author: "Kenny Keslar") {
		capability "Actuator"
		capability "Switch"
        capability "Polling"
        
        capability "Refresh"

        //External access for smart apps
        command "setState"
        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", nextState:"off", backgroundColor: "#79b821"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", nextState:"off", backgroundColor: "#ffffff"
			}
		}

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
	}
	main(["switch"])
	details(["switch", "refresh"])
    
}

// parse events into attributes
def parse(description) {
	//log.debug "parse() - $description"
	def results = []
    def msg = parseLanMessage(description)
    def json = msg.json
    //log.debug "Result: $msg"
    if (msg.status == 200) {
    	log.debug "Result: $json"
        //results << createEvent(name: "switch", value: json.enabled)
        json.each {
        	def n = it.getKey()
        	if (n != "status") {
            	def v = it.getValue()
        		results << createEvent(name: n, value: v) //, displayed:false)
        		//log.debug "Status: $n = $v"
        	}
        }
    }
	results
}




/////////////////////////////////////////////////////////////////////////////////////////////////////
// Helper functions
def refresh() {
	log.debug "refresh()"
    GET()
}


private GET() {
  //log.debug("Executing get api to " + getHostAddress())
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
	path: "/status",
    headers: [HOST:getHostAddress()]
  )
  return hubAction
}
private POST(args=[]) {
	//log.debug("POST args=${args}")
	def hubAction = [new physicalgraph.device.HubAction(
		method: "POST",
		path: "/control",
		body: args,
		headers: [Host:getHostAddress() ]
		)] //, delayAction(100)]
	return hubAction
}
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
private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}



/////////////////////////////////////////////////////////////////////////////////////////////////////
// Color control commands
/////////////////////////////////////////////////////////////////////////////////////////////////////
// Switch commands
def on() { POST([switch: "on"]) }
def off() {	POST([switch: "off"]) }

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Poll interface
def poll() {
	log.debug "poll()"
	refresh()
}

