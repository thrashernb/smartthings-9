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

	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", nextState:"off", backgroundColor: "#79b821"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", nextState:"on", backgroundColor: "#ffffff"
			}
		}

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
	}
	main(["switch"])
	details(["switch", "refresh"])
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Response Parsing
def parse(description) {
	log.trace "parse($description)"
    
	def results = []
    def msg = parseLanMessage(description)
    def json = msg.json
    log.trace "MESSAGE Result: $msg"
    if (msg.status == 200 || msg.header.startsWith('NOTIFY')) {
    	log.debug "JSON Result: $json"
        json.each {
        	def n = it.getKey()
        	if (n != "status") {
            	def v = it.getValue()
        		results << createEvent(name: n, value: v)
        	}
        }
    }
  
	results
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Communication functions
private GET() {
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
	path: "/status",
    headers: [HOST:getHostAddress()]
  )
  return hubAction
}
private POST(args=[]) {
	def hubAction = [new physicalgraph.device.HubAction(
		method: "POST",
		path: "/control",
		body: args,
		headers: [Host:getHostAddress() ]
		), delayAction(100)]
	return hubAction
}

private SUBSCRIBE() {
    log.trace "SUBSCRIBE()"
    def address = getCallBackAddress()
    def ip = getHostAddress()

    def result = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        path: '/subscribe',
        headers: [
            HOST: ip,
            CALLBACK: "<http://${address}/>",
            NT: "upnp:event",
            TIMEOUT: "Second-120"
        ],
    )

    return result
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
private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
    
}
private getCallBackAddress() {
	device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Switch commands
def on() { return POST([switch: "on"]) }
def off() {	return POST([switch: "off"]) }

/////////////////////////////////////////////////////////////////////////////////////////////////////
// Refresh interface
def refresh() {
	log.trace "refresh()"
    return [SUBSCRIBE(),GET()]
}
/////////////////////////////////////////////////////////////////////////////////////////////////////
// Poll interface
def poll() {
	log.trace "poll()"
	return GET()
}

