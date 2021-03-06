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
 */
metadata {
	definition (name: "Arduino Multi ThingShield", namespace: "r3dey3", author: "Kenny Keslar") {

		fingerprint profileId: "0104", deviceId: "0138", inClusters: "0000"
		capability "Switch"
        command "send", ["string"]
        command "notify", ["string","string"]
        command "settingsMap"
	}

	// Simulator metadata
	simulator {
		// status messages
		status "ping": "catchall: 0104 0000 01 01 0040 00 6A67 00 00 0000 0A 00 0A70696E67"
		status "hello": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A48656c6c6f20576f726c6421"
	}

	// UI tile definitions
	tiles {
		standardTile("shield", "device.shield", width: 2, height: 2) {
			state "default", icon:"st.shields.shields.arduino", backgroundColor:"#ffffff"
		}

		main "shield"
		details "shield"
	}
    
}

// Parse incoming device messages to generate events
def parse(String description) {
	def value = zigbee.parse(description)?.text
	def name = value && value != "ping" ? "response" : null
	def result = createEvent(name: name, value: value)
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def send(String text) {
	log.trace "Send $text"
	zigbee.smartShield(text: text).format()
}

def on() {}
def off() {}

def notify(String arg1, Stringarg2) {
	log.trace "notify($arg1, $arg2)"
}
def settingsMap(arg1, arg2) {
	log.trace "settingsMap($arg1,$arg2)"
}