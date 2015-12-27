/**
 *  Copyright 2015 SmartThings
 *
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

		command "setAdjustedColor"
                command "reset"        
                command "refresh"
                command "on2"
                command "off2"
                command "setLevel2"
                command "setColor2"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setAdjustedColor"
			}
		}

		multiAttributeTile(name:"switch2", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch2", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'2nd Color', action:"off2", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'2nd Color', action:"on2", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'Turning On', action:"off2", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOff"
				attributeState "turningOff", label:'Turning Off', action:"on2", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level2", key: "SLIDER_CONTROL") {
				attributeState "level", action:"setLevel2"
			}
			tileAttribute ("device.color2", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor2"
			}
		}

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
	}

	main(["switch"])
	details(["switch", "switch2", "refresh", "reset"])
}

// parse events into attributes
def parse(description) {
	//log.debug "parse() - $description"
	def results = []
    def msg = parseLanMessage(description)
    def json = msg.json
    //log.debug "Result: $msg"
    if (msg.status == 200) {
    	//log.debug "Result: $json"
        //results << createEvent(name: "switch", value: json.enabled)
        json.each {
        	def n = it.getKey()
        	if (n != "status") {
            	def v = it.getValue()
        		results << createEvent(name: n, value: v)
        		log.debug "Status: $n = $v"
        	}
        }
    }
	results
}

// handle commands
def on() { POST([switch: "on"]) }
def off() {	POST([switch: "off"]) }
def setLevel(percent) { POST([level: percent]) }
def setColor(value) {
	log.debug "setColor: ${value}, $this"
    POST([color: value.hex])
}


def on2() { POST([switch2: "on"]) }
def off2() { POST([switch2: "off"]) }
def setLevel2(percent) { POST([level: percent]) }
def setColor2(value) {
	log.debug "setColor: ${value}, $this"
    POST([color2: value.hex])
}


def setSaturation(percent) {
	log.debug "setSaturation($percent)"
	//sendEvent(name: "saturation", value: percent)
}

def setHue(percent) {
	log.debug "setHue($percent)"
	//sendEvent(name: "hue", value: percent)
}

def reset() {
	log.debug "Executing 'reset'"
    def value = [level:100, hex:"#90C638", saturation:56, hue:23]
    setAdjustedColor(value)
}

def setAdjustedColor(value) {
	if (value) {
        log.trace "setAdjustedColor: ${value}"
        def adjusted = value + [:]
        // Needed because color picker always sends 1.00
        adjusted.level = null 
        setColor(adjusted)
    }
}

def refresh() {
	log.debug "Executing 'refresh'"
    GET()
}

def rgbToHSV(red, green, blue) {
	float r = red / 255f
	float g = green / 255f
	float b = blue / 255f
	float max = [r, g, b].max()
	float delta = max - [r, g, b].min()
	def hue = 13
	def saturation = 0
	if (max && delta) {
		saturation = 100 * delta / max
		if (r == max) {
			hue = ((g - b) / delta) * 100 / 6
		} else if (g == max) {
			hue = (2 + (b - r) / delta) * 100 / 6
		} else {
			hue = (4 + (r - g) / delta) * 100 / 6
		}
	}
	[hue: hue, saturation: saturation, value: max * 100]
}

def huesatToRGB(float hue, float sat) {
	while(hue >= 100) hue -= 100
	int h = (int)(hue / 100 * 6)
	float f = hue / 100 * 6 - h
	int p = Math.round(255 * (1 - (sat / 100)))
	int q = Math.round(255 * (1 - (sat / 100) * f))
	int t = Math.round(255 * (1 - (sat / 100) * (1 - f)))
	switch (h) {
		case 0: return [255, t, p]
		case 1: return [q, 255, p]
		case 2: return [p, 255, t]
		case 3: return [p, q, 255]
		case 4: return [t, p, 255]
		case 5: return [255, p, q]
	}
}


private GET() {
  log.debug("Executing get api to " + getHostAddress())
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
	path: "/status",
    headers: [HOST:getHostAddress()]
  )
  return hubAction
}
private POST(args=[]) {
	log.debug("Executing ${uri}, args=${args}")
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