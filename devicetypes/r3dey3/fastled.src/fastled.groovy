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
	definition (name: "FastLed", namespace: "r3dey3", author: "Kenny Keslar") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Refresh"
		capability "Sensor"
        capability "Polling"
        
        command "reset"     
        command "refresh"

		//Color 1 control
		command "setLevel1"
        command "setColor1"
        command "setAdjustedColor"
        
        //Color 2 control
		command "nextMode"
		command "setLevel2"
        command "setColor2"

		//Level 3
		command "setLevel3"

        //External access for smart apps
        command "setState"
        
        
        //Other attributes
        attribute "mode", "enum", ["single_color", "dual_color", "rainbow", "changing"]
        attribute "level2", "number"
        attribute "level3", "number"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lighting.light21", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lighting.light21", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lighting.light21", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lighting.light21", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"setLevel1"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setAdjustedColor"
			}
		}

		multiAttributeTile(name:"switch2", type: "lighting", width: 6, height: 4){
			tileAttribute ("mode", key: "PRIMARY_CONTROL") {
                attributeState "single_color", label: "Single Color", action: "nextMode", icon:"st.secondary.tools", nextState:"changing"
                attributeState "dual_color", label: "Dual Color", action: "nextMode", icon:"st.secondary.tools", nextState:"changing"
                attributeState "rainbow", label: "Rainbow", action: "nextMode", icon:"st.secondary.tools", nextState:"changing"
                attributeState "changing", label: "Changing Mode", action: "nextMode", nextState: "changing"
			}
			tileAttribute ("device.level2", key: "SLIDER_CONTROL") {
				attributeState "level2", action:"setLevel2"
			}
			tileAttribute ("device.color2", key: "COLOR_CONTROL") {
				attributeState "color2", action:"setColor2"
			}
		}
        controlTile("level3", "level3", "slider", width:"6", height:1) {
        	state "default", label:"", action:"setLevel3"
        }

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
	}
	main(["switch"])
	details(["switch", "switch2", "level3", "refresh", "reset"])
    
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
def setLevel1(percent) { log.debug "setLevel1($percent)"; POST([level: percent]) }
def setAdjustedColor(value) {
	def setValues = [:]
	setValues.color_raw = [hue: value.hue, saturation:value.saturation]
    setValues.color = value.hex
	log.debug "setAdjustedColor($value) = $setValues"; 
    POST(setValues) ;
}
/////////////////////////////////////////////////////////////////////////////////////////////////////
// handle commands
def nextMode() {
	log.debug "nextMode()"
	//log.debug("Executing ${uri}, args=${args}")
	def hubAction = [new physicalgraph.device.HubAction(
		method: "POST",
		path: "/next_mode",
		body: [],
		headers: [Host:getHostAddress() ]
		)]
	return hubAction
}

def setLevel2(percent) { POST(level2: percent)}
def setColor2(value) { POST(color2_raw: [hue: value.hue, saturation:value.saturation]) }
def setLevel3(percent) { POST(level3: percent)}



/////////////////////////////////////////////////////////////////////////////////////////////////////
// Helper functions
def reset() {
	log.debug "reset()"
    POST(mode:"single_color", level:100, color_raw:[hue:16.6666668, saturation:27.450981])
}

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
def setLevel(percent) {
	log.debug "setLevel(percent)"
	setLevel1(percent)
}

def setColor(value) {
	def setValues = [:]
    if (value.saturation) { 
    	setValues.color_raw = [hue: value.hue, saturation:value.saturation]
        setValues.hue = value.hue
        setValues.saturation = value.saturation
    }
    if (value.switch) {
    	setValues.switch = value.switch
     }
    if (value.level > 0) {
    	setValues.level = value.level
     	setValues.switch = "on"
    }
    else if (value.level) {
    	setValues.level = value.level
     	setValues.switch = "off"
	}    
    if (value.hex) {
    	setValues.color = value.hex
    }
    setValues.mode = "single_color"
    

	log.debug "setColor($value) = $setValues"; 
    POST(setValues) ;
}
def setSaturation(percent) {
	log.debug "setSaturation($percent)"
}

def setHue(percent) {
	log.debug "setHue($percent)"
}
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

