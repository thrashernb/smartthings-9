/**
 *  Event Logger
 *
 *  Copyright 2015 Brian Keifer
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
 * based on code from Brian Keifer - https://github.com/bkeifer/smartthings/blob/master/Logstash%20Event%20Logger/LogstashEventLogger.groovy
 */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

definition(
    name: "Logstash Event Logger",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "Log SmartThings events to a Logstash server",
    category: "Convenience",
    iconUrl: "http://valinor.net/images/logstash-logo-square.png",
    iconX2Url: "http://valinor.net/images/logstash-logo-square.png",
    iconX3Url: "http://valinor.net/images/logstash-logo-square.png")


preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
 	section("Log these switches:") {
    	input "switches", "capability.switch", multiple: true, required: false
    }
 	section("Log these switch levels:") {
    	input "levels", "capability.switchLevel", multiple: true, required: false
    }
	section("Log these motion sensors:") {
    	input "motions", "capability.motionSensor", multiple: true, required: false
    }
	section("Log these temperature sensors:") {
    	input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these humidity sensors:") {
    	input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("Log these contact sensors:") {
    	input "contacts", "capability.contactSensor", multiple: true, required: false
    }
    section("Log these alarms:") {
		input "alarms", "capability.alarm", multiple: true, required: false
	}
    section("Log these indicators:") {
    	input "indicators", "capability.indicator", multiple: true, required: false
    }
    section("Log these CO detectors:") {
    	input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
    }
    section("Log these smoke detectors:") {
    	input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
    }
    section("Log these water detectors:") {
    	input "waterdetectors", "capability.waterSensor", multiple: true, required: false
    }
    section("Log these acceleration sensors:") {
    	input "accelerations", "capability.accelerationSensor", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }

    section ("Logstash Server") {
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
    }

}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	def eventBuffer = atomicState.eventBuffer ?: []
	atomicState.eventBuffer = eventBuffer
    unschedule()
    schedule("0 * * * * ?", sendEvents)
	doSubscriptions()
}

def doSubscriptions() {
	if (alarms)
		subscribe(alarms,			"alarm",					genericHandler)
    if (codetectors)
    	subscribe(codetectors,		"carbonMonoxideDetector",	genericHandler)
    if (contacts)
		subscribe(contacts,			"contact",      			genericHandler)
    if (indicators)
    	subscribe(indicators,		"indicator",    			genericHandler)
    if (modes)
    	subscribe(modes,			"locationMode", 			genericHandler)
    if (motions)
    	subscribe(motions,			"motion",       			genericHandler)
    if (presences)
   		subscribe(presences,		"presence",     			genericHandler)
    if (relays)
    	subscribe(relays,			"relaySwitch",  			genericHandler)
    if (smokedectors)
		subscribe(smokedetectors,	"smokeDetector",			genericHandler)
    if (switches)
		subscribe(switches,			"switch",       			genericHandler)
    if (levels)
    	subscribe(levels,			"level",					genericHandler)
    if (temperatures)
		subscribe(temperatures,		"temperature",  			genericHandler)
    if (waterdetectors)
		subscribe(waterdetectors,	"water",					genericHandler)
    if (accelerations)
    	subscribe(accelerations,    "acceleration",             genericHandler)
    if (energymeters)
    	subscribe(energymeters,     "power",                    genericHandler)
    subscribe(location,			"location",					genericHandler)
    subscribe(location,			"mode",					genericHandler)
    subscribe(location,			"sunset",					genericHandler)
    subscribe(location,			"sunrise",					genericHandler)
}

def genericHandler(evt) {
    //def data = [:]
    def data = new HashMap() 
    
   	try {
        data.isDigital = evt.isDigital()
        data.isPhysical = evt.isPhysical()
        data.isStateChange = evt.isStateChange()
        data.id = "${evt.id}"
        data.epoch = now() / 1000.0
        //if (evt.data) data.extra_data = evt.data
        //if (evt.description) data.description = evt.description
        if (evt.descriptionText) data.descriptionText = evt.descriptionText
        //if (evt.device) data.device = evt.device
        if (evt.displayName) data.displayName = evt.displayName
        if (evt.deviceId) data.deviceId = "${evt.deviceId}"
        //if (evt.hubId) data.hubId = evt.hubId
        //if (evt.installedSmartAppId) data.installedSmartAppId = evt.installedSmartAppId
        if (evt.isoDate) data.isoDate = "${evt.isoDate}"
        //if (evt.locationId) data.locationId = evt.locationId
        //if (evt.name) data.name = evt.name
        if (evt.source) data.source = "${evt.source}"
        if (evt.value) data.value = "${evt.value}"
        //if (evt.unit) data.unit = evt.unit
        
        if (location.id == evt.locationId) {
            data.locationName = "${location.name}"
            data.locationLat = location.latitude
            data.locationLong = location.longitude
            data.currentMode = "${location.currentMode}"
            data.timeZone = "${location.timeZone.getID()}"
            data.timeZoneName = "${location.timeZone.getDisplayName()}"
        }
        
	} catch (e) {
        log.debug "Trying to get the data for ${evt.name} threw an exception: $e"
    }
    try {
    	data.integerValue = evt.integerValue
    } catch (e) {}
    try {
    	data.floatValue = evt.floatValue
    } catch (e) {}
    try {
    	data.xyzValue = evt.xyzValue
    } catch (e) {}
    
    switch (data.value) {
        case 'on':
        case 'open':
        case 'active':
	        data.integerValue = 1
    	    break
        case 'off':
        case 'closed':
        case 'inactive':
        	data.integerValue = 0
        break
    }
    
   	if (data.value == 'on') {
        levels.each { dev ->
            if (dev.id == data.deviceId) {
                data.integerValue = dev.currentValue('level')
            }
        }
    }
   	log.trace "genericHandler(${data})"
    
    try {
    	def eventBuffer = atomicState.eventBuffer ?: []
        eventBuffer << data
    	atomicState.eventBuffer = eventBuffer
        //log.debug "sttate buffer = ${atomicState.eventBuffer}"
    } catch (e) {
        log.debug "Trying to save the data for ${evt.name} threw an exception: $e"
    }
	//atomicState.eventBuffer = eventBuffer
    /*
	def eventPost = [
		uri: "http://${logstash_host}:${logstash_port}/smartthings",
		headers: [
			"Content-Type": "application/json",
		],
		body: data
	]    
    
    try {
    	httpPostJson(eventPost) { resp ->
        	log.debug "$resp"
	    }
    } catch ( e ) {
        log.debug "Trying to post the data for ${evt.name} threw an exception: $e"
    }
    */
    
    /*
    try {
        def hubAction = new physicalgraph.device.HubAction(
            method: "PUT",
            path: "/smartthings",
            body: data,
            headers: [Host: "${logstash_host}:${logstash_port}"]
        )
        log.debug hubAction
        //sendHubCommand(hubAction)
    } catch ( e ) {
        log.debug "Trying to sendhubcmd post the data for ${evt.name} threw an exception: $e"
    }
    */
    //dataValue
    //floatValue
    //integerValue
    //longValue
    //numberValue
    //numericValue
    //xyzValue
    
}


def sendEvents() {
	def eventBuffer = atomicState.eventBuffer ?: []
	if (eventBuffer.size() >= 1) {
		// Clear eventBuffer right away since we've already pulled it off of atomicState to reduce the risk of missing
		// events.
		atomicState.eventBuffer = []
        try {
        	def data = new groovy.json.JsonOutput().toJson(eventBuffer)
            def hubAction = new physicalgraph.device.HubAction(
                method: "PUT",
                path: "/smartthings",
                body: eventBuffer,
                headers: [
                	Host: "${logstash_host}:${logstash_port}",
					'Content-Type': "application/json"
                ]
            )
            def msg = """PUT /smartthings HTTP/1.1\r\nContent-Type: application/json\r\nHost: ${logstash_host}:${logstash_port}:1234\r\n\r\n${data}"""
            def hubAction2 = new physicalgraph.device.HubAction(msg, physicalgraph.device.Protocol.LAN)//, "0CA8C8F5:04D2")
            log.debug hubAction
            sendHubCommand(hubAction)
        } catch ( e ) {
            log.debug "Trying to sendhubcmd post the data for ${evt.name} threw an exception: $e"
        }
	} 
}
