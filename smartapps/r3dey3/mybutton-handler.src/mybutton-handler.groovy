/**
 *  MyButton Handler
 *
 *  Copyright 2015 Kenny Keslar
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
definition(
    name: "MyButton Handler",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "MyButtonHandler",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: false)


preferences {
	section("Which Button") {
    input "thebutton", "capability.button", title: "Button", multiple: false, required: true
  }
}

def installed() {
  // subscribe to any change to the "button" attribute
  // if we wanted to only subscribe to the button be held, we would use
  // subscribe(thebutton, "button.held", buttonHeldHandler), for example.
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def buttonHandler(evt) {
  if (evt.value == "held") {
    log.debug "button was held"
  } else if (evt.value == "pushed") {
    log.debug "button was pushed"
  }

  // Some button devices may have more than one button. While the
  // specific implementation varies for different devices, there may be
  // button number information in the jsonData of the event:
  try {
    def data = evt.jsonData
    def buttonNumber = data.buttonNumber as Integer
    log.debug "evt.jsonData: $data"
    log.debug "button number: $buttonNumber"
  } catch (e) {
    log.warn "caught exception getting event data as json: $e"
  }
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
  subscribe(thebutton, "button", buttonHandler)
	log.debug "Installed with settings: ${settings}"
}

// TODO: implement event handlers