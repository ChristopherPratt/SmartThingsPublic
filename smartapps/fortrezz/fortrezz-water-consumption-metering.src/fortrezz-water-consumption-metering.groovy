
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
        
        section("Billing info") {
        	input(name: "unitType", type: "enum", title: "Water unit used in billing", description: null, defaultValue: "", required: true, submitOnChange: true, options: waterTypes())
            input(name: "costPerUnit", type: "decimal", title: "Cost of water unit in billing", description: null, defaultValue: 0, required: true, submitOnChange: true)}
        
        
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
        
        def match = false
        for (myItem in childRules) {
        	for (item2 in state.rules) {
            	if (myItem.id == item2.id) {
                	match = true
                }
            }
            if (match == false) {
            	def r = myItem.rules
            	log.debug "Created a new ${r.type} with an ID of ${myItem.id}"
                switch (r.type){
                	case "Daily Goal":
                    //schedule("0 0 0 1/1 * ? *", setDailyGoal)
                	schedule("0 0/1 * 1/1 * ? *", dailyGoalSearch)
                    break
                    case "Weekly Goal":
                    //schedule("0 0 0 ? * MON *", setWeeklyGoal)
                    schedule("0 0/1 * 1/1 * ? *", weeklyGoalSearch)
                    break
                    case "Monthly Goal":
                    //schedule("0 0 0 1 1/1 ? *", setMonthlyGoal)
                    schedule("0 0/1 * 1/1 * ? *", monthlyGoalSearch)
                    break
                    //default: break
                    }
                    
                //scheduleGoal(myItem.measurementType, myItem.id, myItem.waterGoal, myItem.type)
                //r.start = state.cumulative
                }
            match = false
        }

        state.rules = childRules
        //log.debug("Child Rules: ${state.rules} w/ length ${state.rules.toString().length()}")
        log.debug "Parent Settings: ${settings}"
    }
}

def dailyGoalSearch(){
	def myRules = state.rules
     //myRules.each { it ->
     for (it in myRules){
        def r = it.rules
        if (r.type == "Daily Goal") {
        	scheduleGoal(r.measurementType, it.id, r.waterGoal, r.type, r.start)
        }
    }
}
def weeklyGoalSearch(){
	def myRules = state.rules
     myRules.each { it ->
        def r = it.rules
        if (r.type == "Weekly Goal") {
        	scheduleGoal(r.measurementType, it.id, r.waterGoal, r.type, r.start)
        }
    }
}
def monthlyGoalSearch(){
	def myRules = state.rules
     myRules.each { it ->
        def r = it.rules
        if (r.type == "Monthly Goal") {
        	scheduleGoal(r.measurementType, it.id, r.waterGoal, r.type, r.start)
        }
    }
}
    

def scheduleGoal(measureType, goalID, wGoal, goalType, cStart){

    //setGoal(myItem.measurementType, myItem.id, myItem.waterGoal)
    if (costPerUnit != 0) {
        //notify("Your ${goalType} period has ended. You have you have used ${state.deltaDaily} ${measureType} of your goal of ${wGoal} ${measureType}. Costing \$")
        log.debug "Your ${goalType} period has ended. You have you have used ${cStart} ${measureType} of your goal of ${wGoal} ${measureType}. Costing \$ ${state.cumulative}"
        
    }
    /*if (costPerUnit == 0 || unitType == "") 
    {
    	notify("Your ${goalType} period has ended. You have you have used ${state.deltaDaily} ${measureType} of your goal of ${wGoal} ${measureType}.")
        log.debug "Your ${goalType} period has ended. You have you have used ${state.deltaDaily} ${measureType} of your goal of ${wGoal} ${measureType}."
     }*/
        
    //state["accHistory${goalID}"] = null
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
    //unschedule()
}

def initialize() {
	subscribe(meter, "cumulative", cumulativeHandler)
	//subscribe(meter, "gpm", gpmHandler)
    log.debug("Subscribing to events")
}

/*
def setDailyGoal(measurementType2, childAppID2)
{
	if (costPerUnit != 0) {
    notify("Your daily goal period has ended. You have you have used ${state.deltaDaily} ${state.dailyMeasurementType} of your goal of ${state.DailyGallonGoal} ${state.dailyMeasurementType}.")
    log.debug "Your daily goal period has ended. You have you have used ${state.deltaDaily} ${state.dailyMeasurementType} of your goal of ${state.DailyGallonGoal} ${state.dailyMeasurementType}."
    state["accHistory${childAppID2}"] = null
}

def setWeeklyGoal(measurementType2, childAppID2)
{
    notify("Your weekly goal period has ended. You have you have used ${state.deltaWeekly} ${state.weeklyMeasurementType} of your goal of ${state.weeklyGallonGoal} ${state.weeklyMeasurementType}.")
    log.debug "Your weekly goal period has ended. You have you have used ${state.deltaWeekly} ${state.weeklyMeasurementType} of your goal of ${state.weeklyGallonGoal} ${state.weeklyMeasurementType}."
    state["accHistory${childAppID2}"] = null
}

def setMonthlyGoal(measurementType2 ,childAppID2)
{
    notify("Your monthly goal period has ended. You have you have used ${state.deltaMonthly} ${state.monthlyMeasurementType} of your goal of ${state.monthlyGallonGoal} ${state.monthlyMeasurementType}.")
    log.debug "Your monthly goal period has ended. You have you have used ${state.deltaMonthly} ${state.monthlyMeasurementType} of your goal of ${state.monthlyGallonGoal} ${state.monthlyMeasurementType}."
    state["accHistory${childAppID2}"] = null
}
*/
def cumulativeHandler(evt) {
    
	def gpm = meter.latestValue("gpm")
    def cumulative = evt.value
    state.cumulative = cumulative
    log.debug "Cumulative Handler: [gpm: ${gpm}, cumulative: ${cumulative}]"
    def rules = state.rules
    rules.each { it ->
        def r = it.rules
        def childAppID = it.id
    	//log.debug("Rule: ${r}")
    	switch (r.type) {
            case "Daily Goal":
            	def newCumulative = waterConversionPreference(cumulative, r.measurementType)
                def deltaDaily = 0
                def DailyGallonGoal = r.dgg
                state.DailyGallonGoal = DailyGallonGoal
                if(state["accHistory${childAppID}"] != null)
                {
                    deltaDaily = newCumulative - state["accHistory${childAppID}"]
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
                def newCumulative = waterConversionPreference(cumulative1, r.measurement)
                def deltaWeekly = 0
                def weeklyGallonGoal = r.wgg
                state.weeklyGallonGoal = weeklyGallonGoal
                if(state["accHistory${childAppID}"] != null)
                {
                    deltaWeekly = newCumulative - state["accHistory${childAppID}"]
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
                def newCumulative = waterConversionPreference(cumulative1, r.measurement)
                def deltaMonthly = 0
                def monthlyGallonGoal = r.mgg
                state.monthlyGallonGoal = monthlyGallonGoal
                if(state["accHistory${childAppID}"] != null)
                {
                    deltaMonthly = newCumulative - state["accHistory${childAppID}"]
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
          }      
     }
}

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
