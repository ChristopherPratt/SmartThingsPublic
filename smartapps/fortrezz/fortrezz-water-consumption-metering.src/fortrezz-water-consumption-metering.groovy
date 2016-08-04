/**
 *  FortrezZ Water Consumption Metering
 *
 *  Copyright 2016 Christopher R Pratt
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
    name: "FortrezZ Water Consumption Metering",
    namespace: "FortrezZ",
    author: "Christopher R Pratt",
    description: "Use the FortrezZ Water Meter to efficiently use your homes water system.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "page2", title: "Select device and actions", install: true, uninstall: true)
}

def page2() {
    dynamicPage(name: "page2") {
        section("Choose a water meter to monitor:") {
            input(name: "meter", type: "capability.energyMeter", title: "Water Meter", description: null, required: true, submitOnChange: true)
        }

        if (meter) {
            section {
                app(name: "childRules", appName: "Consumption Metering", namespace: "FortrezZ", title: "Create New Water Consumption Goal", multiple: true)
            }
        }
        
        section("Send notifications through...") {
        	input(name: "pushNotification", type: "bool", title: "SmartThings App", required: false)
        	input(name: "smsNotification", type: "bool", title: "Text Message (SMS)", submitOnChange: true, required: false)
            if (smsNotification)
            {
            	input(name: "phone", type: "phone", title: "Phone number?", required: true)
            }
            input(name: "hoursBetweenNotifications", type: "number", title: "Hours between notifications", required: false)
        }
        
        

		log.debug "there are ${childApps.size()} child smartapps"
        
        //log.debug "Time = ${now()}"
        def childRules = []
        childApps.each {child ->
            log.debug "child ${child.id}: ${child.settings()}"
            childRules << [id: child.id, rules: child.settings()]
        }
        state.rules = childRules
        //log.debug("Child Rules: ${state.rules} w/ length ${state.rules.toString().length()}")
        log.debug "Parent Settings: ${settings}"
    }
}
def waterTypes()
{
	def watertype = []
    
    watertype << "Gallons"
    watertype << "Cubic Feet"
    watertype << "Liters"
    watertype << "Cubic Meters"
    return watertype
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
	subscribe(meter, "cumulative", cumulativeHandler)
	subscribe(meter, "gpm", gpmHandler)
    log.debug("Subscribing to events")
}

def setDailyGoal(measurementType2)
{
    notify("Your daily goal period has ended. You have you have used ${state.deltaDaily} ${state.dailyMeasurementType} of your goal of ${state.DailyGallonGoal} ${state.dailyMeasurementType}.")
    log.debug "Your daily goal period has ended. You have you have used ${state.deltaDaily} ${state.dailyMeasurementType} of your goal of ${state.DailyGallonGoal} ${state.dailyMeasurementType}."
}

def setWeeklyGoal(measurementType2)
{
    notify("Your weekly goal period has ended. You have you have used ${state.deltaWeekly} ${state.weeklyMeasurementType} of your goal of ${state.weeklyGallonGoal} ${state.weeklyMeasurementType}.")
    log.debug "Your weekly goal period has ended. You have you have used ${state.deltaWeekly} ${state.weeklyMeasurementType} of your goal of ${state.weeklyGallonGoal} ${state.weeklyMeasurementType}."
}

def setMonthlyGoal(measurementType2)
{
    notify("Your monthly goal period has ended. You have you have used ${state.deltaMonthly} ${state.monthlyMeasurementType} of your goal of ${state.monthlyGallonGoal} ${state.monthlyMeasurementType}.")
    log.debug "Your monthly goal period has ended. You have you have used ${state.deltaMonthly} ${state.monthlyMeasurementType} of your goal of ${state.monthlyGallonGoal} ${state.monthlyMeasurementType}."
}

def cumulativeHandler(evt) {
    
	def gpm = meter.latestValue("gpm")
    def cumulative1 = new BigDecimal(evt.value)
    log.debug "Cumulative Handler: [gpm: ${gpm}, cumulative: ${cumulative}]"
    def rules = state.rules
    rules.each { it ->
        def r = it.rules
    	//log.debug("Rule: ${r}")
    	switch (r.type) {
            case "Daily Goal":
            	def cumulative = waterConversionPreference(cumulative1, r.measurementType)
                def deltaDaily = 0
                def DailyGallonGoal = r.dgg
                state.DailyGallonGoal = DailyGallonGoal
                if(state["accHistory${childAppID}"] != null)
                {
                    deltaDaily = cumulative - state["accHistory${childAppID}"]
                    state.deltaDaily = deltaDaily
                }
                else
                {
                    state["accHistory${childAppID}"] = cumulative
                }   
                log.debug("Threshold:${DailyGallonGoal}, Value:${deltaDaily}")

                if ( deltaDaily > (0.5 * DailyGallonGoal))
                {
                    notify("you have reached 50% of your Daily gallon use limit")
                }
                if ( deltaDaily > (0.75 * DailyGallonGoal))
                {
                    notify("you have reached 75% of your Daily gallon use limit")
                }
                if ( deltaDaily > (0.9 * DailyGallonGoal))
                {
                    notify("you have reached 90% of your Daily gallon use limit")
                }
                if (deltaDaily > DailyGallonGoal)
                {
                    notify("you have reached 100% of your Daily gallon use limit")
                    //send command here like shut off the water
                }

                break
                    
            case "Weekly Goal":
                def cumulative = waterConversionPreference(cumulative1, r.measurement)
                def deltaWeekly = 0
                def weeklyGallonGoal = r.wgg
                state.weeklyGallonGoal = weeklyGallonGoal
                if(state["accHistory${childAppID}"] != null)
                {
                    deltaWeekly = cumulative - state["accHistory${childAppID}"]
                    state.deltaWeekly = deltaWeekly
                }
                else
                {
                    state["accHistory${childAppID}"] = cumulative
                }   
                log.debug("Threshold:${weeklyGallonGoal}, Value:${deltaWeekly}")

                if ( deltaWeekly > (0.5 * weeklyGallonGoal))
                {
                    notify("you have reached 50% of your Weekly gallon use limit")
                }
                if ( deltaWeekly > (0.75 * weeklyGallonGoal))
                {
                    notify("you have reached 75% of your Weekly gallon use limit")
                }
                if ( deltaWeekly > (0.9 * weeklyGallonGoal))
                {
                    notify("you have reached 90% of your Weekly gallon use limit")
                }
                if (deltaWeekly > weeklyGallonGoal)
                {
                    notify("you have reached 100% of your Weekly gallon use limit")
                    //send command here like shut off the water
                }
                break

            case "Monthly Goal":
                def cumulative = waterConversionPreference(cumulative1, r.measurement)
                def deltaMonthly = 0
                def monthlyGallonGoal = r.mgg
                state.monthlyGallonGoal = monthlyGallonGoal
                if(state["accHistory${childAppID}"] != null)
                {
                    deltaMonthly = cumulative - state["accHistory${childAppID}"]
                    state.deltaMonthly = deltaMonthly
                }
                else
                {
                    state["accHistory${childAppID}"] = cumulative
                }   
                log.debug("Threshold:${monthlyGallonGoal}, Value:${deltaMonthly}")

                if ( deltaMonthly > (0.5 * monthlyGallonGoal))
                {
                    notify("you have reached 50% of your Monthly gallon use limit")
                }
                if ( deltaMonthly > (0.75 * monthlyGallonGoal))
                {
                    notify("you have reached 75% of your Monthly gallon use limit")
                }
                if ( deltaMonthly > (0.9 * monthlyGallonGoal))
                {
                    notify("you have reached 90% of your Monthly gallon use limit")
                }
                if (deltaMonthly > monthlyGallonGoal)
                {
                    notify("you have reached 100% of your Monthly gallon use limit")
                    //send command here like shut off the water
                }
                break
/*
            case "Time Period":
            	log.debug("Time Period Test: ${r}")
                def boolTime = timeOfDayIsBetween(r.startTime, r.endTime, new Date(), location.timeZone)
                def boolDay = !r.days || findIn(r.days, dowName) // Truth Table of this mess: http://swiftlet.technology/wp-content/uploads/2016/05/IMG_20160523_150600.jpg
                def boolMode = !r.modes || findIn(r.modes, location.currentMode)
                
            	if(boolTime && boolDay && boolMode)
                {
                    if(gpm > r.gpm)
                    {
                        sendNotification(childAppID, gpm)
                        if(r.dev)
                        {
                            def activityApp = getChildById(childAppID)
                            activityApp.devAction(r.command)
                        }
                    }
                }
            	break

            case "Accumulated Flow":
            	log.debug("Accumulated Flow Test: ${r}")
                def boolTime = timeOfDayIsBetween(r.startTime, r.endTime, new Date(), location.timeZone)
                def boolDay = !r.days || findIn(r.days, dowName) // Truth Table of this mess: http://swiftlet.technology/wp-content/uploads/2016/05/IMG_20160523_150600.jpg
                def boolMode = !r.modes || findIn(r.modes, location.currentMode)
                
            	if(boolTime && boolDay && boolMode)
                {
                	def delta = 0
                    if(state["accHistory${childAppID}"] != null)
                    {
                    	delta = cumulative - state["accHistory${childAppID}"]
                    }
                    else
                    {
                    	state["accHistory${childAppID}"] = cumulative
                    }
                	log.debug("Currently in specified time, delta from beginning of time period: ${delta}")
                    
                    if(delta > r.gallons)
                    {
                        sendNotification(childAppID, delta)
                        if(r.dev)
                        {
                            def activityApp = getChildById(childAppID)
                            activityApp.devAction(r.command)
                        }
                    }
                }
                else
                {
                	log.debug("Outside specified time, saving value")
                    state["accHistory${childAppID}"] = cumulative
                }
            	break

            case "Continuous Flow":
            	log.debug("Continuous Flow Test: ${r}")
            	def contMinutes = 0
                def boolMode = !r.modes || findIn(r.modes, location.currentMode)

				if(gpm != 0)
                {
                	if(state["contHistory${childAppID}"] == [])
                    {
                    	state["contHistory${childAppID}"] = new Date()
                    }
                    else
                    {
                    	def td = now() - Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", state["contHistory${childAppID}"]).getTime()
                        //log.debug("Now minus then: ${td}")
                        contMinutes = td/60000
                        log.debug("Minutes of constant flow: ${contMinutes}, since ${state["contHistory${childAppID}"]}")
                    }
                }
                
                if(contMinutes > r.flowMinutes && boolMode)
                {
                    sendNotification(childAppID, Math.round(contMinutes))
                    if(r.dev)
                    {
                        def activityApp = getChildById(childAppID)
                        activityApp.devAction(r.command)
                    }
                }
                break

            case "Water Valve Status":
            	log.debug("Water Valve Test: ${r}")
            	def child = getChildById(childAppID)
                //log.debug("Water Valve Child App: ${child.id}")
                if(child.isValveStatus(r.valveStatus))
                {
                    if(gpm > r.gpm)
                    {
                        sendNotification(childAppID, gpm)
                   }
                }
                break

            case "Switch Status":
            	break

            default:
                break*/
        }
    }
}
/*
def gpmHandler(evt) {
	//Date Stuff
   	def daysOfTheWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    def today = new Date()
    today.clearTime()
    Calendar c = Calendar.getInstance();
    c.setTime(today);
    int dow = c.get(Calendar.DAY_OF_WEEK);
    def dowName = daysOfTheWeek[dow-1]
    
	def gpm = evt.value
    def cumulative = meter.latestValue("cumulative")
    log.debug "GPM Handler: [gpm: ${gpm}, cumulative: ${cumulative}]"
    def rules = state.rules
    rules.each { it ->
        def r = it.rules
        def childAppID = it.id
    	switch (r.type) {

			// This is down here because "cumulative" never gets sent in the case of 0 change between messages
			case "Continuous Flow":
            	log.debug("Continuous Flow Test (GPM): ${r}")
            	def contMinutes = 0

				if(gpm == "0.0")
                {
                	state["contHistory${childAppID}"] = []
                }
                //log.debug("contHistory${childAppID} is ${state["contHistory${childAppID}"]}")
                break

            default:
                break
        }
	}	
}
*/

def waterConversionPreference(cumul, measurementType1)
{
	switch (measurementType1)
    {
            case "Cubic Feet":
            	return (cumul * 0.133681)
            break
            
            case "Liters":
            	return (cumul * 3.78541)
            break
            
            case "Cubic Meters":
            	return (cumul * 0.00378541)
            break
            
            case "Gallons":
            	return cumul
            break
    }
}
def notify(myMsg)
{
	log.debug("Sending Notification")
    if (pushNotification)
    {
        sendPush(myMsg)
        state["notificationHistory${device}"] = new Date()
    }
    if (smsNotification)
    {
        sendSms(phone, myMsg)
        state["notificationHistory${device}"] = new Date()
    }
}
def sendNotification(device, gpm)
{
	def set = getChildById(device).settings()
	def msg = ""
    if(set.type == "Accumulated Flow")
    {
    	msg = "Water Flow Warning: \"${set.ruleName}\" is over threshold at ${gpm} ${measurementType}"
    }
    else if(set.type == "Continuous Flow")
    {
    	msg = "Water Flow Warning: \"${set.ruleName}\" is over threshold at ${gpm} minutes"
    }
    else
    {
    	msg = "Water Flow Warning: \"${set.ruleName}\" is over threshold at ${gpm}gpm"
    }
    log.debug(msg)
    
    // Only send notifications as often as the user specifies
    def lastNotification = 0
    if(state["notificationHistory${device}"])
    {
    	lastNotification = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", state["notificationHistory${device}"]).getTime()
    }
    def td = now() - lastNotification
    log.debug("Last Notification at ${state["notificationHistory${device}"]}... ${td/(60*1000)} minutes")
    if(td/(60*1000) > hoursBetweenNotifications.value * 60)
    {
    	log.debug("Sending Notification")
        if (pushNotification)
        {
            sendPush(msg)
            state["notificationHistory${device}"] = new Date()
        }
        if (smsNotification)
        {
            sendSms(phone, msg)
            state["notificationHistory${device}"] = new Date()
        }
    }
}

def getChildById(app)
{
	return childApps.find{ it.id == app }
}

def findIn(haystack, needle)
{
	def result = false
	haystack.each { it ->
    	//log.debug("findIn: ${it} <- ${needle}")
    	if (needle == it)
        {
        	//log.debug("Found needle in haystack")
        	result = true
        }
    }
    return result
}