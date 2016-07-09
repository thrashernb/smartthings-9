/**
*  
* 
*  Irrigation Scheduler SmartApp Smarter Lawn Contoller
**
*  Copyright 2014 Stan Dotson and Matthew Nichols
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
*
* / Based on code from matt@nichols.name and stan@dotson.info
**/

definition(
    name: "Irrigation Scheduler",
    namespace: "r3dey3",
    author: "Kenny K",
    description: "Schedule sprinklers to run unless there is rain.",
    version: "1",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png"
)

preferences {
	page(name: "schedulePage", title: "Create An Irrigation Schedule", nextPage: "zonePage", uninstall: true) {
        
        section("Preferences") {
        	label name: "title", title: "Name this irrigation schedule...", required: false, multiple: false, defaultValue: "Irrigation Scheduler"
        	input "notificationEnabled", "boolean", title: "Send Push Notification When Irrigation Starts", description: "Do You Want To Receive Push Notifications?", defaultValue: "true", required: false
        }
        
        section {
        	input name:"numZones", type:"number", title:"Number of zones"
        }
        section {
            input (
            name: "wateringDays",
            type: "enum",
            title: "Water on which days?",
            required: false,
            multiple: true, // This must be changed to false for development (known ST IDE bug)
            metadata: [values: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']])
        }

        section("Minimum interval between waterings...") {
            input "days", "number", title: "Days?", description: "minimum # days between watering", defaultValue: "1", required: false
        }

        section("Start watering at what times...") {
            input name: "waterTimeOne",  type: "time", required: true, title: "Turn them on at..."
            input name: "waterTimeTwo",  type: "time", required: false, title: "and again at..."
            input name: "waterTimeThree",  type: "time", required: false, title: "and again at..."
        }

    }
    
	page(name: "zonePage", title: "Select sprinkler switches", nextPage:"timesPage")
	page(name: "timesPage", title: "Select zone run times", nextPage: "weatherPage")
    
	page(name: "weatherPage", title: "Virtual Weather Station Setup", install: true) {
        section("Zip code or Weather Station ID to check weather...") {
            input "zipcode", "text", title: "Enter zipcode or or pws:stationid", required: false
        }
        
        section("Select which rain to add to your virtual rain guage...") {
        	input "isYesterdaysRainEnabled", "boolean", title: "Yesterday's Rain", description: "Include?", defaultValue: "true", required: false
        	input "isTodaysRainEnabled", "boolean", title: "Today's Rain", description: "Include?", defaultValue: "true", required: false
        	input "isForecastRainEnabled", "boolean", title: "Today's Forecasted Rain", description: "Include?", defaultValue: "false", required: false
        }
       
       	section("Skip watering if virutal rain guage totals more than... (default 0.5)") {
            input "wetThreshold", "decimal", title: "Inches?", defaultValue: "0.5", required: false
        }
        
        section("Run watering only if forecasted high temp (F) is greater than... (default 50)") {
            input "tempThreshold", "decimal", title: "Temp?", defaultValue: "50", required: false
        }
    }
}		


def zonePage() {
    dynamicPage(name: "zonePage") {
        section {
            settings.numZones.times { i ->
                def num = i +1 
                input "zone$i", "capability.switch", title: "Zone $num switch"
            }
        }
 	}
}

def timesPage() {
    dynamicPage(name: "timesPage") {
        section ("Length to run:") {
        
            settings.numZones.times { i ->
                def num = i +1 
                def dev = settings["zone${i}"]
                input "zone${i}time", "number", title: "$dev", range: "0..120", default:20
            }
        }
    }
}


def installed() {
    scheduling()
    state.daysSinceLastWatering = [1000,1000,1000]
}

def updated() {
    unschedule()
    scheduling()
    state.daysSinceLastWatering = [1000,1000,1000]
    state.currentTimerIx = 0
    state.currentZone = settings.numZones
    //scheduleCheck()
}

// Scheduling
def scheduling() {
	log.debug "Scheduling"
    schedule(waterTimeOne, "waterTimeOneStart")
    if (waterTimeTwo) {
        schedule(waterTimeTwo, "waterTimeTwoStart")
    }
    if (waterTimeThree) {
        schedule(waterTimeThree, "waterTimeThreeStart")
    }
}

def waterTimeOneStart() {
    log.info "Time 1"
    state.currentTimerIx = 0
    scheduleCheck()
}
def waterTimeTwoStart() {
    log.info "Time 0"
    state.currentTimerIx = 1
    scheduleCheck()
}
def waterTimeThreeStart() {
    log.info "Time 3"
    state.currentTimerIx = 2
    scheduleCheck()
}

def scheduleCheck() {
    log.info "Running Irrigation Schedule: ${app.label}"
/*
    def schedulerState = switches?.latestValue("effect")?.toString() ?:"[noEffect]"

    if (schedulerState == "onHold") {
        log.info("${app.label} sprinkler schedule on hold.")
        return
    } 
    
	if (schedulerState == "skip") { 
    	// delay this watering and reset device.effect to noEffect
        schedulerState = "delay" 
        for(s in switches) {
            if("noEffect" in s.supportedCommands.collect { it.name }) {
                s.noEffect()
                log.info ("${app.label} skipped one watering and will resume normal operations at next scheduled time")
            }
        }
 	}    
    
	if (schedulerState != "expedite") { 
    	// Change to delay if wet or too cold
        schedulerState = isWeatherDelay() ? "delay" : schedulerState
 	}

    if (schedulerState != "delay") {
        state.daysSinceLastWatering[state.currentTimerIx] = daysSince() + 1
    }*/

//    log.info("${app.label} scheduler state: $schedulerState. Days since last watering: ${daysSince()}. Is watering day? ${isWateringDay()}. Enought time? ${enoughTimeElapsed(schedulerState)} ")

    if (enoughTimeElapsed(schedulerState) && isWateringDay()) {
        if (isNotificationEnabled) {
        	sendPush("${app.label} Is Watering Now!" ?: "null pointer on app name")
        }
        state.daysSinceLastWatering[state.currentTimerIx] = 0
        startWatering()
    }
    else {
	    log.info "Not  watering"
        state.daysSinceLastWatering[state.currentTimerIx] = daysSince() + 1
    }
}

def isWateringDay() {
    if(!wateringDays) return true

    def today = new Date().format("EEEE", location.timeZone)
    if (wateringDays.contains(today)) {
        return true
    }
    log.info "${app.label} watering is not scheduled for today"
    return false
}

def enoughTimeElapsed(schedulerState) {
    if(!days) return true
    return (daysSince() >= days)
}

def daysSince() {
    if(state.daysSinceLastWatering == null) 
    	state.daysSinceLastWatering = [1000,1000,1000];

    return state.daysSinceLastWatering[state.currentTimerIx]
}

def isWeatherDelay() { 
	log.info "${app.label} Is Checking The Weather"
    if (zipcode) {
        //add rain to virtual rain guage
        def rainGauge = 0
        if (isYesterdaysRainEnabled) {        
            rainGauge = rainGauge + wasWetYesterday()
        }

        if (isTodaysRainEnabled) {
            rainGauge = rainGauge + isWet()
        }

        if (isForecastRainEnabled) {
            rainGauge = rainGauge + isStormy()
        }
        log.info ("Virtual rain gauge reads $rainGauge in")
        
 //     check to see if virtual rainguage exceeds threshold
        if (rainGauge > (wetThreshold?.toFloat() ?: 0.5)) {
            if (isNotificationEnabled) {
                sendPush("Skipping watering today due to precipitation.")
            }
            log.info "${app.label} skipping watering today due to precipitation."
            /*
            for(s in switches) {
                if("rainDelayed" in s.supportedCommands.collect { it.name }) {
                    s.rainDelayed()
                    log.info "Watering is rain delayed for $s"
                }
            }*/
            return true
        }
        
        def maxThermometer = isHot()
        if (maxThermometer < (tempThreshold?.toFloat() ?: 0)) {
        	if (isNotificationEnabled.equals("true")) {
                sendPush("Skipping watering: temp is below threshold temp.")
            }
            log.info "${app.label} is skipping watering: temp is below threshold temp."
            return true
		}
     }
    return false
}

def safeToFloat(value) {
    if(value && value.isFloat()) return value.toFloat()
    return 0.0
}

def wasWetYesterday() {
    def yesterdaysWeather = getWeatherFeature("yesterday", zipcode)
    log.debug yesterdaysWeather
/*
    def yesterdaysPrecip=yesterdaysWeather.history.dailysummary.precipi.toArray()
    def yesterdaysInches=safeToFloat(yesterdaysPrecip[0])
    log.info("Checking yesterday's percipitation for $zipcode: $yesterdaysInches in")
    */
	return 0
}


def isWet() {

    def todaysWeather = getWeatherFeature("conditions", zipcode)
    def todaysInches = safeToFloat(todaysWeather.current_observation.precip_today_in)
    log.info("Checking today's percipitation for $zipcode: $todaysInches in")
    return todaysInches
}

def isStormy() {
/*
    def forecastWeather = getWeatherFeature("forecast", zipcode)
    def forecastPrecip=forecastWeather.forecast.simpleforecast.forecastday.qpf_allday.in.toArray()
    def forecastInches=(forecastPrecip[0])
    log.info("Checking forecast percipitation for $zipcode: $forecastInches in")
    return forecastInches
    */
    return 0
}

def isHot() {

    def forecastWeather = getWeatherFeature("forecast", zipcode)
    def todaysTemps=forecastWeather.forecast.simpleforecast.forecastday.high.fahrenheit.toArray()
    def todaysHighTemp=(todaysTemps[0]).toFloat()
    log.info("Checking forecast high temperature for $zipcode: $todaysHighTemp F")
    return todaysHighTemp
}

def startWatering() {
	if (state.currentZone != null && state.currentZone != settings.numZones) {
    	log.debug "Not watering because schedule in progress"
    	return;
    }
    state.currentZone = -1;
    nextZone();
}
def nextZone() {
	def curZone = state.currentZone
    if (curZone >= 0) {
        log.debug "Turn off"
    	settings["zone${curZone}"].off()
    }
    curZone = curZone + 1
    while (curZone < settings.numZones) {
        def dev = settings["zone${curZone}"]
        def t = settings["zone${curZone}time"]
        if (t > 0) {
            dev.on()
            runIn(t*60, "nextZone");
            log.debug("Start watering with ${dev} for $t minutes");
            break;
        }
    	curZone = curZone + 1
	}
    state.currentZone = curZone
}
