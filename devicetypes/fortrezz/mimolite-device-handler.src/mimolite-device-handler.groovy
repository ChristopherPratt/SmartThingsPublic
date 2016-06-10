/**
 *  MimoLite Device Handler
 *
 *  Copyright 2014 Todd Wackford
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
 *  Note: This device type is based on the work of Jit Jack (cesaldar) as posted on the SmartThings Community website.
 *
 *  This device type file will configure a Fortrezz MimoLite Wireless Interface/Bridge Module as a Garage Door 
 *  Controller. The Garage Door must be physically configured per the following diagram: 
 *    "http://www.fortrezz.com/index.php/component/jdownloads/finish/4/17?Itemid=0"
 *  for all functionality to work correctly.
 *
 *  This device type will also set the atttibute "powered" to "powerOn" or "powerOff" accordingly. This uses
 *  the alarm capability of the MimoLite and the status will be displayed to the user on a secondary tile. User
 *  can subscribe to the status of this atttribute to be notified when power drops out.
 *
 *  This device type implements a "Configure" action tile which will set the momentary switch timeout to 25ms and
 *  turn on the powerout alarm.
 *
 *  
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "MimoLite Device Handler", namespace: "FortrezZ", author: "Christopher Pratt") {
		capability "Configuration"
		capability "Switch"
		capability "Refresh"
		capability "Contact Sensor"
        capability "Voltage Measurement"

		attribute "powered", "string"

		command "on"
		command "off"
        
        fingerprint deviceId: "0x1000", inClusters: "0x72,0x86,0x71,0x30,0x31,0x35,0x70,0x85,0x25,0x03"
	}

	simulator {
	// Simulator stuff
    
	}

	// UI tile definitions 
	tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label: "Turn Off", action: "off", icon: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png", backgroundColor: "#53a7c0"
			state "off", label: 'Turn On', action: "on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        standardTile("powered", "device.powered", inactiveLabel: false) {
			state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ffa81e"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        valueTile("voltage", "device.voltage") {
        state "val", label:'${currentValue}v', unit:"", defaultState: true
    }
		main (["switch"])
		details(["switch", "contact", "voltage", "powered", "refresh","configure"])
	}
}

def parse(String description) {
//log.debug "description is: ${description}"

	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1, 0x31: 5])
    
    //log.debug "command value is: $cmd.CMD"
    
    if (cmd.CMD == "7105") {				//Mimo sent a power loss report
    	log.debug "Device lost power"
    	sendEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
    } else {
    	sendEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
    }
    //log.debug "${device.currentValue('contact')}" // debug message to make sure the contact tile is working
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText} $cmd.CMD"
	return result
}


//notes about zwaveEvents:
// these are special overloaded functions which MUST be returned with a map similar to (return [name: "switch", value: "on"])
// not doing so will produce a null on the parse function, this will mess you up in the future.
// Perhaps can use 'createEvent()' and return that as long as a map is inside it.
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { 
log.debug "switchBinaryReport"
    if (cmd.value) // if the switch is on it will not be 0, so on = true
    {
    	
		return [name: "switch", value: "on"] // change switch value to on
    }
    else // if the switch sensor report says its off then do...
    {
		return [name: "switch", value: "off"] // change switch value to off
    }
       
}
// working on next for the analogue and digital stuff.
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) // basic set is essentially our digital sensor for SIG1
{
	log.debug "sent a BasicSet command"
    //refresh()  
    delayBetween([zwave.sensorMultilevelV5.sensorMultilevelGet().format()])// requests a report of the anologue input voltage
	[name: "contact", value: cmd.value ? "open" : "closed"]}
    //[name: "contact", value: cmd.value ? "open" : "closed", type: "digital"]}
    
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	log.debug "sent a sensorBinaryReport command"
	refresh()    
	[name: "contact", value: cmd.value ? "open" : "closed"]
}


    
def zwaveEvent (physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) // sensorMultilevelReport is used to report the value of the analog voltage for SIG1
{
	log.debug "sent a SensorMultilevelReport"
	def ADCvalue = cmd.scaledSensorValue
   
    CalculateVoltage(cmd.scaledSensorValue)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
     log.debug("Un-parsed Z-Wave message ${cmd}")
	[:]
}

def CalculateVoltage(ADCvalue)
{
	 def map = [:]

    def volt = (((3.19*(10**-16))*(ADCvalue**5)) - ((2.18*(10**-12))*(ADCvalue**4)) + ((5.47*(10**-9))*(ADCvalue**3)) - ((5.68*(10**-6))*(ADCvalue**2)) + (0.0028*ADCvalue) - (0.0293))
	//log.debug "$cmd.scale $cmd.precision $cmd.size $cmd.sensorType $cmd.sensorValue $cmd.scaledSensorValue"
	def voltResult = volt.round(1)// + "v"
    
	map.name = "voltage"
    map.value = voltResult
    map.unit = "v"
    return map
}
	

def configure() {
	log.debug "Configuring...." //setting up to monitor power alarm and actuator duration
	delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(), // 	FYI: Group 3: If a power dropout occurs, the MIMOlite will send an Alarm Command Class report 
        																							//	(if there is enough available residual power)
        zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:[zwaveHubNodeId]).format(), // periodically send a multilevel sensor report of the ADC analog voltage to the input
        zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:[zwaveHubNodeId]).format(), // when the input is digitally triggered or untriggered, snd a binary sensor report
        zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 11, size: 1).format() // configurationValue for parameterNumber means how many 100ms do you want the relay
        																										// to wait before it cycles again / size should just be 1 (for 1 byte.)
        //zwave.configurationV1.configurationGet(parameterNumber: 11).format() // gets the new parameter changes. not currently needed. (forces a null return value without a zwaveEvent funciton
	])
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),	// physically changes the relay from on to off and requests a report of the relay
        refresh()// to make sure that it changed (the report is used elsewhere, look for switchBinaryReport()
       ])
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(), // physically changes the relay from on to off and requests a report of the relay
        refresh()// to make sure that it changed (the report is used elsewhere, look for switchBinaryReport()
	])
}

def refresh() {
log.debug "REFRESH!"
	delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(), //requests a report of the relay to make sure that it changed (the report is used elsewhere, look for switchBinaryReport()
        zwave.sensorMultilevelV5.sensorMultilevelGet().format()// requests a report of the anologue input voltage

    ])
}
