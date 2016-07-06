/**
 */
definition(
    name: "Arduino Relay Controller",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "Control 8 relays with smartthings",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

 
preferences {
    page(name:"controllerSetup")
   	page(name:"relaySetup")
}


def controllerSetup() {
	dynamicPage(name: "controllerSetup",nextPage: "relaySetup", title: "Controller Setup", uninstall:true) {
        section("Which Arduino shield?") {
            input "arduino", title: "Shield","capability.switch"
        }    
        section("Relays") {
            input "relayCount", title: "How many relays?","number"
        }    
    }
}

def relaySetup() {
   	dynamicPage(name: "relaySetup", title: "Relay Setup", install:true) {
    	for (int i=1;i<=settings.relayCount;i++) {
        	section("Relay " + i) {
                input "relay" + i, title: "Name", "string", description:"Relay " + i, required: false
//                input "typezone" + i, "enum", title: "Type", options:["Open/Closed Sensor","Motion Detector"], required: false
            }
        }
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
    // Listen to anything which happens on the device
    subscribe(arduino, "response", statusUpdate)
    
    for (int i=1;i<=settings.relayCount;i++) {
    	
    	def dni = "${app.id}:relay${i}"
        def name = "relay$i"
		def value = settings[name]

        log.debug "checking device: ${name}, value: $value"

        def existingDevice = getChildDevice(dni)
        if(!existingDevice) {
            log.debug "creating device: ${name}"
            def childDevice = addChildDevice("r3dey3", "Child Switch", dni, null, [
            	name: "Device.${name}", 
                label: value, 
                completedSetup: true,
                "data": [
					"name": name,                	
                ]
                ])
        }
        else {
            //log.debug existingDevice.deviceType
            //existingDevice.type = zoneType
            existingDevice.label = value
            existingDevice.take()
            log.debug "device already exists: ${name}"
        }
    }
    
    
    def delete = getChildDevices().findAll { settings[it.device.getDataValue("name")] }

    delete.each {
        log.debug "deleting child device: ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }
}

def uninstalled() {
    //removeChildDevices(getChildDevices())
}
def on(child) {
	arduino.send("GOODBYE");
}
def off(child) {
	arduino.send("HELLO");
}
def statusUpdate(evt)
{
	log.debug "${evt}"
    log.debug "statusUpdate ${evt.value}"
	

    
/*
    
    def parts = evt.value.split();
        
    def zonetype = parts[0]


    if (zonetype=="heartbeat") {
    	
       	state.lastHeartbeat = now()
        log.debug "received heartbeat: ${state.lastHeartbeat}"
        
    }
    else {
    
        
        def zone = parts[1]
        def status = parts[2]

        def deviceName = "zone$zone"
        def typeSettingName = "typezone$zone"

        if (zonetype=="wireless") 
        {
            deviceName = "wirelesszone$zone"
            typeSettingName = "wirelesszonetype$zone"
        }

        log.debug "$zonetype zone $zone status=$status"

        def device = getChildDevice(deviceName)

        if (device)
        {
            log.debug "$device statusChanged $status"

            def zoneType = settings[typeSettingName];

            if (zoneType == null || zoneType == "")
            {
                zoneType = "Open/Closed Sensor"
            }

            def eventName = "contact"

            if (zonetype=="wireless") 
            {
                status = status=="0" ? "open" : "closed"

            }

            if (zoneType=="Motion Detector")
            {
                eventName = "motion";
                status = status=="open" ? "active" : "inactive"
            }   

            device.sendEvent(name: eventName, value: status, isStateChange:true)
        }
        else {

            log.debug "couldn't find device for zone ${zone}"

        }
        
    }
*/
}
