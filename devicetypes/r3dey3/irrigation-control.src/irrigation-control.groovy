/**
 *  Irrigation Control
 *
 *  Copyright 2016 Kenny Keslar
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
 */
metadata {
	definition (name: "Irrigation Control", namespace: "r3dey3", author: "Kenny Keslar") {
		attribute "date", "string"
		attribute "currentZone", "string"
        attribute "state", "enum", ["watering", "scheduled"]
        attribute "enabled", "enum", ["on", "off"]
        command "disable"
        command "enable"
        
        command "nextDate"
        command "prevDate"
        command "log"
        command "start"
        command "stop"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2)  {
        standardTile("enabled", "device.enabled", canChangeIcon: false, width: 2, height: 2, decoration: "flat") {
            state "off", label: 'Disabled', action: "enable", icon: "st.switches.switch.off", backgroundColor: "#ff8888"
            state "on", label: 'Enabled', action: "disable", icon: "st.switches.switch.on", backgroundColor: "#79b821"
        }
        standardTile("state", "device.state", canChangeIcon: false, width: 2, height: 2, decoration: "flat") {
            state "watering", label: 'Watering', action: "stop", icon: "st.switches.switch.off", backgroundColor: "#8888ff", nextState:"watering"
            state "scheduled", label: 'Scheduled', action: "start", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"scheduled"
        }
        
        
		standardTile("prevDate", "device.prevDate", canChangeIcon: false, inactiveLabel: false, decoration: "flat") {
			state "prevDate", label:'  ', action:"prevDate", icon:"st.thermostat.thermostat-down", backgroundColor:"#1e9cbb"
		}
		standardTile("nextDate", "device.nextDate", canChangeIcon: false, inactiveLabel: false, decoration: "flat") {
			state "nextDate", label:'  ', action:"nextDate", icon:"st.thermostat.thermostat-up", backgroundColor:"#1e9cbb"
		}        
	    valueTile("date", "device.date", width: 2, height: 2) {
        	state "val", label:'${currentValue}', defaultState: true
    	}
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def update(newState) {
	log.debug "update() - $newState"
    newState.each {
        sendEvent(name: it.key, value: it.value)
    }
}
// handle commands
def start() {
	log.debug "Executing 'start'"
	parent.start()
}

def stop() {
	log.debug "Executing 'stop'"
	parent.stop()
	// TODO: handle 'off' command
}
def enable() {
	sendEvent(name: "enabled", value: "on")
}
def disable() {
	sendEvent(name: "enabled", value: "off")
}
def nextDate() {
	parent.nextDate()
}
def prevDate() {
	parent.prevDate()
}
def log(m) {
	log.debug m
}