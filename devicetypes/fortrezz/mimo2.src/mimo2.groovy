/**
 *  MIMO2 Device Handler
 *
 *  Copyright 2016 FortrezZ, LLC
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
metadata {
	definition (name: "MIMO2+", namespace: "fortrezz", author: "FortrezZ, LLC") {
		capability "Alarm"
		capability "Contact Sensor"
		capability "Relay Switch"
		capability "Voltage Measurement"
        capability "Configuration"
        capability "Refresh"
        
        
        attribute "powered", "string"
        attribute "relay", "string"
        
        attribute "relay2", "string"
        attribute "contact2", "string"
        attribute "voltage2", "string"
        
		command "on"
		command "off"
        command "on2"
        command "off2"
        
        fingerprint deviceId: "0x2100", inClusters: "0x5E,0x86,0x72,0x5A,0x59,0x71,0x98,0x7A"
	}
    
    preferences {
    	section {
        input ("RelaySwitchDelay", "decimal", title: "Relay 1 Delay between relay switch on and off in seconds. Only Numbers 0 to 3 allowed. 0 value will remove delay and allow relay to function as a standard switch. Press 'Configure' tile to allow change", description: "Numbers 0 to 3 allowed.", defaultValue: 0, required: false, displayDuringSetup: true)
    
        input ("RelaySwitchDelay2", "decimal", title: "Relay 2 Delay between relay switch on and off in seconds. Only Numbers 0 to 3 allowed. 0 value will remove delay and allow relay to function as a standard switch. Press 'Configure' tile to allow change", description: "Numbers 0 to 3 allowed.", defaultValue: 0, required: false, displayDuringSetup: true)
        } // the range would ve 0 to 3.1, but the range value would accept 3.1, only whole numbers (i tried paranthesis and fractions too. :( )
    }
    
	tiles {
         standardTile("relay", "device.switch1", width: 2, height: 2,inactiveLabel: false) {
            state "on", label: "Relay 1 On", action: "off", icon: "http://swiftlet.technology/wp-content/uploads/2016/06/Switch-On-104-edit.png", backgroundColor: "#53a7c0"
			state "off", label: '${name}', action: "on", icon: "http://swiftlet.technology/wp-content/uploads/2016/06/Switch-Off-104-edit.png", backgroundColor: "#ffffff"
            state "onIcon", label: "Relay 1 On", action: "off", icon: "http://swiftlet.technology/wp-content/uploads/2016/06/Switch-On-104-edit.png", backgroundColor: "#53a7c0"
            state "offIcon", label: "Relay 1 Off", action: "on", icon: "http://swiftlet.technology/wp-content/uploads/2016/06/Switch-Off-104-edit.png", backgroundColor: "#ffffff"


        }
         standardTile("relay2", "device.switch2", width: 2, height: 2, inactiveLabel: false) {
            state "on2", label: "Relay 2 On", action: "off2", icon: "http://swiftlet.technology/wp-content/uploads/2016/06/Switch-On-104-edit.png", backgroundColor: "#53a7c0", nextState: "off2"
			state "off2", label: 'Relay 2 Off', action: "on2", icon: "http://swiftlet.technology/wp-content/uploads/2016/06/Switch-Off-104-edit.png", backgroundColor: "#ffffff", nextState: "on2"
        }
        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}
        standardTile("contact2", "device.contact2", inactiveLabel: false) {
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
        valueTile("voltage2", "device.voltage2") {
            state "val", label:'${currentValue}v', unit:"", defaultState: true
    	}
		main (["relay"])
		details(["relay", "contact", "voltage", "relay2", "contact2", "voltage2", "powered", "refresh","configure"])
	}
}

// parse events into attributes
def parse(String description) {
	def result = null
	def cmd = zwave.parse(description)
    	//def cmd = zwave.parse(description, [0x20: 1, 0x25: 1,0x84: 1, 0x30: 1, 0x70: 1, 0x31: 5, 0x60: 3, 0x98: 1])
    //[0x20: BasicSet, 0x25: BinarySwitch 1,0x84: WakeUp , 0x30: sensorBinary, 0x70: configuration, 0x31: sensorMultiLevel, 0x60: multiChannel, 0x98: security])
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

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.basicv1.BasicSet cmd) // basic set is essentially our digital sensor for SIG1
{
	log.debug "sent a BasicSet command"
	if (endPoint == 1)
    	{return [name: "contact", value: cmd.value ? "open" : "closed"]}
    else if (endPoint == 2) 
    	{return [name: "contact2", value: cmd.value ? "open" : "closed"]}
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	log.debug "sent a sensorBinaryReport command"
	if (endPoint == 1)
    	{return [name: "contact", value: cmd.value ? "open" : "closed"]}
    else if (endPoint == 2) 
    	{return [name: "contact2", value: cmd.value ? "open" : "closed"]}
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	def map = [:]
    if (endPoint == 3)
    {
    	if (cmd.value)
    		{map.value = "onIcon"}
    	else
    		{map.value = "offIcon"}
        map.name = "relay"
        //sendEvent(name: "powered", value: "powerOff"
        log.debug "sent a SwitchBinary command $map.name $map.value"
        return [name: "relay", value: cmd.value ? "onIcon" : "offIcon"]
    }
    else if (endPoint == 4)
    {
    	if (cmd.value)
    		{map.value = "on2"}
    	else
    		{map.value = "off2"}
        map.name = "relay2"
        sendEvent(name: "relay2", value: "$map.value")
        log.debug "sent a SwitchBinary command $map.name $map.value"
        //return [name: "relay2", value: cmd.value ? "on2" : "off2"]
        return createEvent(name: "relay2", value: "off2")
    }
    
	//return map
}
   
def zwaveEvent (int endPoint, physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) // sensorMultilevelReport is used to report the value of the analog voltage for SIG1
{
	def map = [:]
    def volatageVal = CalculateVoltage(cmd.scaledSensorValue)
    if (endPoint == 1)
    {
        map.name = "voltage"
        map.value = volatageVal
        map.unit = "v"
    }
    else if (endPoint == 2)
    {
        map.name = "voltage2"
        map.value = volatageVal
        map.unit = "v"
    }
	log.debug "sent a SensorMultilevelReport $map.name" // ${map.value}v"
    return map
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    //def encapsulatedCommand = cmd.encapsulatedCommand()
    def encapsulatedCommand = cmd.encapsulatedCommand()
    // can specify command class versions here like in zwave.parse
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    }
}

// MultiChannelCmdEncap and MultiInstanceCmdEncap are ways that devices
// can indicate that a message is coming from one of multiple subdevices
// or "endpoints" that would otherwise be indistinguishable
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    //def encapsulatedCommand = cmd.encapsulatedCommand()
    def encapsulatedCommand = cmd.encapsulatedCommand()

    // can specify command class versions here like in zwave.parse
    log.debug ("Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}")

    if (encapsulatedCommand) {
        return zwaveEvent(cmd.sourceEndPoint, encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
     log.debug("Un-parsed Z-Wave message ${cmd}")
	return [:]
}

def CalculateVoltage(ADCvalue)
{
     def volt = (((6.60*(10**-17))*(ADCvalue**5)) - ((5.46*(10**-13))*(ADCvalue**4)) + ((1.77*(10**-9))*(ADCvalue**3)) - ((2.07*(10**-6))*(ADCvalue**2)) + ((1.57*(10**-3))*(ADCvalue)) - (5.53*(10**-3)))

    //def volt = (((3.19*(10**-16))*(ADCvalue**5)) - ((2.18*(10**-12))*(ADCvalue**4)) + ((5.47*(10**-9))*(ADCvalue**3)) - ((5.68*(10**-6))*(ADCvalue**2)) + (0.0028*ADCvalue) - (0.0293))
	//log.debug "$cmd.scale $cmd.precision $cmd.size $cmd.sensorType $cmd.sensorValue $cmd.scaledSensorValue"
	return volt.round(1)
}
	

def configure() {
	def delay = (RelaySwitchDelay*10).toInteger() // the input which we get from the user is a string and is in seconds while the MIMO2 configuration requires it in 100ms so - change to integer and multiply by 10
    
    	    
    def delay2 = (RelaySwitchDelay2*10).toInteger() // the input which we get from the user is a string and is in seconds while the MIMO2 configuration requires it in 100ms so - change to integer and multiply by 10
    
    	    
	log.debug "Configuring...." 
    
    return delayBetween([
    	encap(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 0), // sending the 5 associationSet messages forces the MIMO2 to be in default configuration
    	encap(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 1),
    	encap(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 2),
        encap(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 3),
        encap(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]), 4),
        secure(zwave.configurationV1.configurationSet(configurationValue: [0x01], parameterNumber: 3, size: 1)), // sends a multiLevelSensor report every 30 seconds for SIG1
        secure(zwave.configurationV1.configurationSet(configurationValue: [0x01], parameterNumber: 9, size: 1)), // sends a multiLevelSensor report every 30 seconds for SIG2
        secure(zwave.configurationV1.configurationSet(configurationValue: [delay], parameterNumber: 1, size: 1)), // configurationValue for parameterNumber means how many 100ms do you want the relay
        																										// to wait before it cycles again / size should just be 1 (for 1 byte.)
        secure(zwave.configurationV1.configurationSet(configurationValue: [delay2], parameterNumber: 2, size: 1))
    ], 200)
}

def on() {
	//delayBetween([
	//	sourceEndPoinzwave.multiChannelV3.multiChannelCmdEncap(t:3, destinationEndPoint: 3, commandClass:37, command:1, parameter:[255]).format(),
	//	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:3, destinationEndPoint: 3, commandClass:37, command:2).format()
	//])
        	
	return delayBetween([
        encap(zwave.basicV1.basicSet(value: 0xff), 3), // physically changes the relay from on to off and requests a report of the relay
        encap(zwave.switchBinaryV1.switchBinaryGet(), 3) // request a report to make sure that the relay has been switched
    	],300)
}

def off() {
	//delayBetween([
	//	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:3, destinationEndPoint: 3, commandClass:37, command:1, parameter:[0]).format(),
	//	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:3, destinationEndPoint: 3, commandClass:37, command:2).format()
	//])

    return delayBetween([
        encap(zwave.basicV1.basicSet(value: 0x00), 3), // physically changes the relay from on to off and requests a report of the relay
        encap(zwave.switchBinaryV1.switchBinaryGet(), 3) // request a report to make sure that the relay has been switched
    	],300)
}

def on2() {
		//command(zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint: 4).encapsulate(zwave.basicV1.basicSet(value: 0xFF)))
		//encap(zwave.basicV1.basicSet(value: 0xFF), 4)	// physically changes the relay from on to off and requests a report of the relay
       // refresh()// to make sure that it changed (the report is used elsewhere, look for switchBinaryReport()
       //sendEvent(name: "relay2", value: "on2")
   return delayBetween([
        encap(zwave.basicV1.basicSet(value: 0xff), 4), // physically changes the relay from on to off and requests a report of the relay
        encap(zwave.switchBinaryV1.switchBinaryGet(), 4) // request a report to make sure that the relay has been switched
    ],300)
}

def off2() {
		//command(zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint: 4).encapsulate(zwave.basicV1.basicSet(value: 0x00)))
		//log.debug "off2"
		//encap(zwave.basicV1.basicSet(value: 0x00), 4)	// physically changes the relay from on to off and requests a report of the relay
       // refresh()// to make sure that it changed (the report is used elsewhere, look for switchBinaryReport()
       //sendEvent(name: "relay2", value: "off2")
    return delayBetween([
        encap(zwave.basicV1.basicSet(value: 0x00), 4), // physically changes the relay from on to off and requests a report of the relay
        encap(zwave.switchBinaryV1.switchBinaryGet(), 4) // request a report to make sure that the relay has been switched
    ],300)
}

def refresh() {
//log.debug "REFRESH!"
	return delayBetween([
         encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), 1),// requests a report of the anologue input voltage for SIG1
         encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), 2),// requests a report of the anologue input voltage for SIG2
         encap(zwave.switchBinaryV1.switchBinaryGet(), 3), //requests a report of the relay to make sure that it changed for Relay 1
         encap(zwave.switchBinaryV1.switchBinaryGet(), 4) //requests a report of the relay to make sure that it changed for Relay 2
       ],200)
}

private secure(physicalgraph.zwave.Command cmd) {
	return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private secureSequence(commands, delay=200) {
	return delayBetween(commands.collect{ secure(it) }, delay)
}

private encap(cmd, endpoint) {
	//log.debug "before encapsulate"
	if (endpoint) {
		//command(zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: endpoint, destinationEndPoint: endpoint).encapsulate(cmd))
		return secure(zwave.multiChannelV3.multiChannelCmdEncap(bitAddress: false, sourceEndPoint:0, destinationEndPoint: endpoint).encapsulate(cmd))

	} else {
		return secure(cmd)
	}
}