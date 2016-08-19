
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
    name: "Test Automation",
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

        section("Scheduled Switch") {
            input "theswitch", "capability.switch", required: true
            input "repeatSwitch", "bool", title: "Turn on Scheduled repeat switch?",  defaultValue: 0, required: false, displayDuringSetup: true
        }
    }
}

def installed() { // when the app is first installed - do something
	log.debug "Installed"
}

def updated() { // whevenever the app is updated in any way by the user and you press the 'done' button on the top right of the app - do something   
    if (state.count == 1) // this bit with state keeps the function from running twice ( which it always seems to want to do) (( oh, and state.count is a variable which is nonVolatile and doesn't change per every parse request.
    {
    	log.debug "Updated"
        if (state.repeat != 1){state.repeat = 0}
        if (repeatSwitch == true && state.repeat == 0){
            schedule("0 0/1 * 1/1 * ? *", onSwitch)
            state.repeat = 1
            log.debug "Repeat Switch is ON!"}
        if (repeatSwitch == false && state.repeat == 1){
            unschedule()
            state.repeat = 0
            log.debug "Repeat Switch is OFF!"
            }
        state.count = 0
    }
    else {state.count = 1}
}

def initialize() { // whenever you open the smart app - do something
    log.debug("Initialized")
}

def uninstalled() {
    // external cleanup. No need to unsubscribe or remove scheduled jobs
}

def onSwitch(){
	theswitch.on()}