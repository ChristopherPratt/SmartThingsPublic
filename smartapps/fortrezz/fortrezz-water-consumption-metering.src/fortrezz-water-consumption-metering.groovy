
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
        	input(name: "unitType", type: "enum", title: "Water unit used in billing", description: null, defaultValue: "Gallons", required: true, submitOnChange: true, options: waterTypes())
            input(name: "costPerUnit", type: "decimal", title: "Cost of water unit in billing", description: null, defaultValue: 0, required: true, submitOnChange: true)
        	input(name: "fixedFee", type: "decimal", title: "Add a Fixed Fee?", description: null, defaultValue: 0, required: true, submitOnChange: true)}
        
        section("Send notifications through...") {
        	input(name: "pushNotification", type: "bool", title: "SmartThings App", required: false)
        	input(name: "smsNotification", type: "bool", title: "Text Message (SMS)", submitOnChange: true, required: false)
            if (smsNotification)
            {
            	input(name: "phone", type: "phone", title: "Phone number?", required: true)
            }
            //input(name: "hoursBetweenNotifications", type: "number", title: "Hours between notifications", required: false)
        }
        

        
        

		log.debug "there are ${childApps.size()} child smartapps"
        
        //log.debug "Time = ${now()}"
        def childRules = []
        childApps.each {child ->
            log.debug "child ${child.id}: ${child.settings()}"
            childRules << [id: child.id, rules: child.settings()] //this section of code stores the long ID and settings (which contains several variables of the individual goal such as measurement type, water consumption goal, start cumulation, current cumulation.) into an array
        }
        
        def match = false
        def changeOfSettings = false
        for (myItem in childRules) {
        	def q = myItem.rules
        	for (item2 in state.rules) {
                def r = item2.rules
            	if (myItem.id == item2.id) { //I am comparing the previous array to current array and checking to see if any new goals have been made.  
                	match = true
                    if (q.type == r.type){
                    	changeOfSettings = true}
                }                
            }
            if (changeOfSettings == false){
            	unschedule()
                log.debug "Found a changed goal type, We're going to unschedule all and reschedule"
            	for (myItem2 in childRules) {
                     state["reSchedule${myItem.id}"] = true
                	}
                }
            if (match == false) { // if a new goal has been made, i need to do some first time things like set up a recurring schedule depending on goal duration
            	state["reSchedule${myItem.id}"] = true
                state["NewApp${myItem.id}"] = true
                log.debug "Created a new ${q.type} with an ID of ${myItem.id}"}
    
            match = false
        }
        
        for (myItem in childRules) {
            if (state["reSchedule${myItem.id}"]){

                def r = myItem.rules
                
                switch (r.type){
                    case "Daily Goal":
                    //schedule("0 0 0 1/1 * ? *", setDailyGoal)
                    schedule("0 0/1 * 1/1 * ? *", dailyGoalSearch) // here we are using schedule() to set up our goal durations. the quoted area is known as a "cron" and should be google searched (i used a cron calculator to get these values) cron is super specific and useful. (at the determined time a method is invoked which CANNOT HAVE ANY PARAMETERS!!! this is super limiting and annoying.
                    break
                    case "Weekly Goal":
                    //schedule("0 0 0 ? * SUN *", setWeeklyGoal)
                    schedule("0 0/2 * 1/1 * ? *", weeklyGoalSearch)
                    break
                    case "Monthly Goal":
                    //schedule("0 0 0 1 1/1 ? *", setMonthlyGoal)
                    schedule("0 0/3 * 1/1 * ? *", monthlyGoalSearch)
                    break
                    //default: break
                }

                //scheduleGoal(myItem.measurementType, myItem.id, myItem.waterGoal, myItem.type)
                //state["Start${myItem.id}"] = state.cumulative // we create another object attached to our goal called 'start' and store the existing cumulation on the FMI device so we know at what mileage we are starting at for this goal. this is useful for determining how much water is used during the goal period.
                if (state["NewApp${myItem.id}"] == true){
                	state["NewApp${myItem.id}"] = false
                    state["currentCumulation${myItem.id}"] = 0 // we create another object attached to our new goal called 'currentCumulation' which should hold the value for how much water has been used since the goal period has started
                    state["oneHundred${myItem.id}"] = false
                    state["ninety${myItem.id}"] = false
                    state["seventyFive${myItem.id}"] = false
                    state["fifty${myItem.id}"] = false
                    state["reSchedule${myItem.id}"] = false}
            }
        }

        state.rules = childRules // storing the array we just made to state makes it persistent across the instances this smart app is used and global across the app ( this value cannot be implicitely shared to any child app unfortunately without making it a local variable FYI
        //log.debug("Child Rules: ${state.rules} w/ length ${state.rules.toString().length()}")
        log.debug "Parent Settings: ${settings}"
             
        if (costPerUnit != null && unitType != null){//we ask the user in the main page for billing info which includes the price of the water and what water measurement unit is used. we combine convert the unit to gallons (since that is what the FMI uses to tick water usage) and then create a ratio that can be converted to any water measurement type
        	state.costRatio = costPerUnit/convertToGallons(unitType)
        	state.fixedFee = fixedFee
        }
    }
}

def convertToGallons(myUnit) // does what title says - takes whatever unit in string form and converts it to gallons to create a ratio. the result is returned
{
	switch (myUnit){
    	case "Gallons":
        	return 1
            break
        case "Cubic Feet":
        	return 7.48052
            break
        case "Liters":
        	return 0.264172
            break
        case "Cubic Meters":
        	return 264.172
            break
        default:
        	log.debug "value for water measurement doesn't fit into the 4 water measurement categories"
            return 1
            break
       }
}

def dailyGoalSearch(){ // because of our limitations of schedule() we had to create 3 separate methods for the existing goal period of day, month, and year. they are identical other than their time periods.
	def myRules = state.rules // also, these methods are called when our goal period ends. we filter out the goals that we want and then invoke a separate method called schedulGoal to inform the user that the goal ended and produce some results based on their water usage.
     //myRules.each { it ->
     for (it in myRules){
        def r = it.rules
        if (r.type == "Daily Goal") {
        	
        	scheduleGoal(r.measurementType, it.id, r.waterGoal, r.type)
            
        }
    }
}
def weeklyGoalSearch(){
	def myRules = state.rules
     myRules.each { it ->
        def r = it.rules
        if (r.type == "Weekly Goal") {
        	
        	scheduleGoal(r.measurementType, it.id, r.waterGoal, r.type)


        }
    }
}
def monthlyGoalSearch(){
	def myRules = state.rules
     myRules.each { it ->
        def r = it.rules
        if (r.type == "Monthly Goal") {
        	
        	scheduleGoal(r.measurementType, it.id, r.waterGoal, r.type)
            
            

        }
    }
}
    

def scheduleGoal(measureType, goalID, wGoal, goalType){ // this is where the magic happens. after a goal period has finished this method is invoked and the user gets a notification of the results of the water usage over their period.
	def cost = 0
    def f = 1.0f
    def curCumulation = state["currentCumulation${goalID}"] // transferring value to local variable becuase reading too much from a state variable causes issues
	if (state.costRatio){
    	cost = costConversionPreference(state.costRatio,measureType) * curCumulation * f + state.fixedFee// determining the cost of the water that they have used over the period ( i had to create a variable 'f' and make it a float and multiply it to make the result a float. this is because the method .round() requires it to be a float for some reasons and it was easier than typecasting the result to a float.
    }
    def percentage = (curCumulation / wGoal) * 100 * f
    if (costPerUnit != 0) {
        notify("Your ${goalType} period has ended. You have you have used ${(curCumulation * f).round(2)} ${measureType} of your goal of ${wGoal} ${measureType} (${(percentage * f).round(1)}%). Costing \$${cost.round(2)}")// notifies user of the type of goal that finished, the amount of water they used versus the goal of water they used, and the cost of the water used
        log.debug "Your ${goalType} period has ended. You have you have used ${(curCumulation * f).round(2)} ${measureType} of your goal of ${wGoal} ${measureType} (${(percentage * f).round(1)}%). Costing \$${cost.round(2)}"
        
    }
    if (costPerUnit == 0) // just in case the user didn't add any billing info, i created a second set of notification code to not include any billing info.
    {
    	notify("Your ${goalType} period has ended. You have you have used ${(curCumulation * f).round(2)} ${measureType} of your goal of ${wGoal} ${measureType} (${percentage.round(1)}%).")
        log.debug "Your ${goalType} period has ended. You have you have used ${(curCumulation * f).round(2)} ${measureType} of your goal of ${wGoal} ${measureType} (${percentage.round(1)}%)."
     }
    state["oneHundred${goalID}"] = false
    state["ninety${goalID}"] = false
    state["seventyFive${goalID}"] = false
    state["fifty${goalID}"] = false
    if (state.cumulative != null) {state["Start${goalID}"] = state.cumulative} // 'start' should now be changed to the current cumulation value for future logic
    state["currentCumulation${goalID}"]  = 0// 'currentCumulation' should be changed to 0 because the goal is now starting over
}
	
	

def waterTypes() // holds the types of water measurement used in the main smartapp page for billing info and for setting goals
{
	def watertype = []
    
    watertype << "Gallons"
    watertype << "Cubic Feet"
    watertype << "Liters"
    watertype << "Cubic Meters"
    return watertype
}

def installed() { // when the app is first installed - do something
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() { // whevenever the app is updated in any way by the user and you press the 'done' button on the top right of the app - do something
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
    //unschedule()
}

def initialize() { // whenever you open the smart app - do something
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
def cumulativeHandler(evt) { // every time a tick on the FMI happens this method is called. 'evt' contains the cumulative value of every tick that has happened on the FMI since it was last reset. each tick represents 1/10 of a gallon
    def f = 1.0f
	def gpm = meter.latestValue("gpm") // storing the current gallons per minute value
    def cumulative = new BigDecimal(evt.value) // storing the current cumulation  value
    state.cumulative = cumulative // making that value available across the app globally and making is persist across instances.
    log.debug "Cumulative Handler: [gpm: ${gpm}, cumulative: ${state.cumulative}]"
    def rules = state.rules //storing the array of child apps to 'rules'
    rules.each { it -> // looping through each app in the array but storing each app into the variable 'it'
        def r = it.rules // each child app has a 2 immediate properties, one called 'id' and one called 'rules' - so 'r' contains the values of 'rules' in the child app
        def childAppID = it.id // storing the child app ID to a variable 


        def newCumulative = waterConversionPreference(cumulative, r.measurementType) //each goal allows the user to choose a water measurement type. here we convert the value of 'cumulative' to whatever the user prefers for display and logic purposes
        def DailyGallonGoal = r.waterGoal // 'r.waterGoal' contains the number of units of water the user set as a goal. we then save that to 'DailyGallonGoal'
        state.DailyGallonGoal = DailyGallonGoal // and then we make that value global and persistent for logic reasons
        if (state["Start${childAppID}"] == null) {state["Start${childAppID}"] = cumulative}// just for the first run of the app... start should be null. so we have to change that for the logic to work.
        def currentCumulation = waterConversionPreference(cumulative, r.measurementType) - waterConversionPreference(state["Start${childAppID}"], r.measurementType) // earlier we created the value 'currentCumulation' and set it to 0, now we are converting both the 'cumulative' value and what 'cumulative' was when the goal perio was made and subtracting them to discover how much water has been used since the creation of the goal in the users prefered water measurement unit.
		state["currentCumulation${childAppID}"] = currentCumulation
        log.debug("Threshold:${DailyGallonGoal}, Value:${currentCumulation}")

        if ( currentCumulation >= (0.5 * DailyGallonGoal) && currentCumulation < (0.75 * DailyGallonGoal) && state["fifty${childAppID}"] == false) // tell the user if they break certain use thresholds
        {
            notify("You have reached 50% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})")
            log.debug "You have reached 50% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})"
            state["fifty${childAppID}"] = true
        }
        if ( currentCumulation >= (0.75 * DailyGallonGoal) && currentCumulation < (0.9 * DailyGallonGoal) && state["seventyFive${childAppID}"] == false)
        {
            notify("You have reached 75% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})")
            log.debug "You have reached 75% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})"
            state["seventyFive${childAppID}"] = true
        }
        if ( currentCumulation >= (0.9 * DailyGallonGoal) && currentCumulation < (DailyGallonGoal) && state["ninety${childAppID}"] == false)
        {
            notify("You have reached 90% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})")
            log.debug "You have reached 90% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})"
            state["ninety${childAppID}"] = true
        }
        if (currentCumulation >= DailyGallonGoal && state["oneHundred${childAppID}"] == false)
        {
            notify("You have reached 100% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})")
            log.debug "You have reached 100% of your ${r.type} use limit. (${(currentCumulation * f).round(2)} of ${DailyGallonGoal} ${r.measurementType})"
            state["oneHundred${childAppID}"] = true
            //send command here like shut off the water
            

            
        }         
    }
}

def waterConversionPreference(cumul, measurementType1) // convert the current cumulation to one of the four options below - since cumulative is initially in gallons, then the options to change them is easy
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

def costConversionPreference(cumul, measurementType1) // convert the current cumulation to one of the four options below - since cumulative is initially in gallons, then the options to change them is easy
{
	switch (measurementType1)
    {
            case "Cubic Feet":
            	return (cumul / 0.133681)
            break
            
            case "Liters":
            	return (cumul / 3.78541)
            break
            
            case "Cubic Meters":
            	return (cumul / 0.00378541)
            break
            
            case "Gallons":
            	return cumul
            break
        
    }
}

def notify(myMsg) // method for both push notifications and for text messaging.
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
/*
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
*/

def uninstalled() {
    // external cleanup. No need to unsubscribe or remove scheduled jobs
    unsubscribe()
    unschedule()
}
