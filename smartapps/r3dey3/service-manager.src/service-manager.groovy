/**
 *  Generic UPnP Service Manager
 *
 *  Copyright 2016 SmartThings
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
		name: "Service Manager",
		namespace: "r3dey3",
		author: "r3dey3",
		description: "Python Service Manager SmartApp",
		category: "",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        singleInstance: true
  
        )


preferences {
	page(name: "deviceDiscovery", title: "Device Setup", content: "deviceDiscovery")
}

def deviceDiscovery() {
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "Device ${it.value.ssdpUSN}"
		options[it.key] = value
	}

	ssdpSubscribe()
	ssdpDiscover()
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover your Devices. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    //state.devices = []
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    def children = getChildDevices()
	log.debug "$children"
	unsubscribe()
	initialize()
}
def uninstalled() {
   state.devices = [:]
}
def initialize() {
	unsubscribe()
	unschedule()
    
	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}
    try {
		subscribe(location, null, locationHandler, [filterEvents:false])
    } catch (all) {
    	log.trace "Subscription already exist"
 	}

	runEvery5Minutes("ssdpDiscover")
}

void ssdpDiscover() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:python:1", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:python:1", ssdpHandler)
}

void verifyDevices() {
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def addDevices() {
	def devices = getDevices()

    log.debug "Devices = $devices"
	selectedDevices.each { dni ->
		def selectedDevice = devices.find{ it.key == dni }
        selectedDevice = selectedDevice?.value
		def d
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == dni
			}
            log.debug "$dni - $selectedDevice"
            if (!d) {
                log.debug "Creating Device with dni: ${dni}"
                addChildDevice("r3dey3", "Generic REST Device", "${dni}", selectedDevice?.hub, [
                    "label": selectedDevice?.name ?: "Generic UPnP Device",
                    "data": [
                        "mac": selectedDevice.mac,
                        "ip": selectedDevice.networkAddress,
                        "port": selectedDevice.deviceAddress,
                        "ssdpPath": selectedDevice.ssdpPath
                    ]
                ])
            }
        }
	}
}

def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
	log.trace "ssdpHandler($parsedEvent)"
	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
	if (devices."${ssdpUSN}") {
		def d = devices."${ssdpUSN}"
        log.debug "found existing - $d"
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
            def dni = ssdpUSN
			def child = getChildDevice(dni)
            log.debug "Update existing device $dni to $child"
			//d.networkAddress = parsedEvent.networkAddress
			//d.deviceAddress = parsedEvent.deviceAddress
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			}
		}
	} else {
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
   log.trace "deviceDescriptionHandler($hubResponse)"
	def body = hubResponse.json
	log.trace "deviceDescriptionHandler($body)"
	def devices = getDevices()
    def device = devices?.find { it?.key?.contains(body?.usn) }
	if (device && body) {
    	device.value << [name: body?.name, model:body?.model, serialNumber:body?.serial, verified: true]
	}
    //log.debug "ddh - ${device.value.networkAddress}:${device.value.deviceAddress}"
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
def getDeviceAddress(device) {
 return convertHexToIP(device.getDataValue("ip")) +":"+convertHexToInt(device.getDataValue("port"))
}

def locationHandler(evt) {
	def description = evt.description
    log.trace "Location: $description"

	def hub = evt?.hubId
 	def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]
    log.trace "locationHandler($parsedEvent)"
	def d = getChildDevices()?.find {
		it.getDataValue("ip") == parsedEvent?.ip &&  it.getDataValue("port") == parsedEvent?.port
	}
    log.debug "Found - $d"
	if (d) {
    	sendHubCommand(d.parseResponse(parsedEvent))
    }
}

def GET(device, url) {
  def host=getDeviceAddress(device)
  device.log("GET($device, $url) - $host")
  def hubAction = new physicalgraph.device.HubAction([method: "GET",
	path: url,
    headers: [HOST:host]
    ]
  )
  sendHubCommand(hubAction)
}

def POST(device, url, args=[]) {
    def host=getDeviceAddress(device)
    device.log("POST($device, $url, $args) - $host")
    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: url,
        body: args,
        headers: [Host:$host ]
    )
    sendHubCommand(hubAction)
}

def SUBSCRIBE(device, url, args=[]) {
    device.log("SUBSCRIBE($device, $url, $args)")
    def address = getCallBackAddress()
    def host=getDeviceAddress(device)

    def hubAction = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        path: url,
        headers: [
            HOST: host,
            CALLBACK: "<http://${address}/>",
            NT: "upnp:event",
            TIMEOUT: "Second-120"
        ],
        body: args,
    )

    sendHubCommand(hubAction)
}
