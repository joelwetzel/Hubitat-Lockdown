/**
 *  Lockdown
 *
 *  Copyright 2019 Joel Wetzel
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

import groovy.json.*
// log.debug groovy.json.JsonOutput.toJson(atomicState.lockMap)
	
definition(
    name: "Lockdown",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "This will lock all selected locks when a specified switch is triggered.",
    category: "Safety & Security",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


def triggeringSwitch = [
		name:				"triggeringSwitch",
		type:				"capability.switch",
		title:				"Triggering Switch",
		description:		"Select the switch that this app will watch.  When it triggers to on, the app will starting locking things.",
		multiple:			false,
		required:			true
	]


def selectedLocks = [
		name:				"selectedLocks",
		type:				"capability.lock",
		title:				"Locks",
		description:		"Select the locks that this app should manage.",
		multiple:			true,
		required:			true
	]


def cycleTime = [
		name:				"cycleTime",
		type:				"number",
		title:				"Delay between lock attempts",
		description:		"Set the amount of time to wait in between locking different locks (or retrying).  If set too low, The z-wave messages from the locks won't have time to propagate. Recommended value: 5",
		defaultValue:		5,
		required:			true
	]


def maxCycles = [
		name:				"maxCycles",
		type:				"number",
		title:				"Max number of retries per Lock",
		description:		"Maximum number of lock/retry cycles per lock.  Recommended value: 3.",
		required:			true
	]


def forceRefresh = [
		name:				"forceRefresh",
		type:				"bool",
		title:				"Auto-refresh after Locking?",
		defaultValue:		true,
		required:			true
	]


def refreshTime = [
		name:				"refreshTime",
		type:				"number",
		title:				"Delay before auto-refreshing",
		description:		"Not all locks perfectly report status updates.  A manual refresh helps.  Set the amount of time to wait after sending a lock command, to send a followup refresh command. Recommend: 5",
		defaultValue:		5,
		required:			true
	]


preferences {
	page(name: "pageOne", title: "", uninstall: true) {
		section(getFormat("title", "${app.label}")) {
				paragraph "Automatically lock a group of doors, with auto-refresh, timing delays, and retries.  It is triggered by a triggering switch (which can be real or virtual).  The triggering switch is reset when the process is complete."
			}
		section("<b>Devices</b>") {
			input triggeringSwitch
			input selectedLocks	
		}
		section("<b>Cycles</b>") {
			input cycleTime
			input maxCycles
		}
		section("<b>Refreshes</b>") {
			input forceRefresh
			input refreshTime
		}
		section() {
			paragraph getFormat("line")
			paragraph "<div style='color:#1A77C9;text-align:center'>Lockdown - @joelwetzel<br><a href='https://github.com/joelwetzel/' target='_blank'>Click here for more Hubitat apps/drivers on my GitHub!</a></div>"
		}      
	}
}


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def installed() {
	log.info "Installed with settings: ${settings}"

	initialize()
}


def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
	subscribe(triggeringSwitch, "switch.on", switchOnHandler)
	
	atomicState.lockMap = [] as int[]
}


def switchOnHandler(evt) {
	log.debug "Lockdown: TRIGGERED"	
	
	atomicState.lockMap = [] as int[]
	
	cycleHandler()
}


// Each cycle runs like this:
// 1) See if there's still an unlocked lock that hasn't exceeded the number of allowed cycles per lock
// 2) If so, send it a lock command, then wait "Refresh Time" seconds
// 3) Send it a refresh command, then wait "Cycle Time" seconds before starting a new cycle
def cycleHandler () {
	// Allow for cancellation
	if (triggeringSwitch.currentValue('switch') == "off") {
		log.debug "Lockdown: CANCELLED"
		doneHandler()
		return
	}
	
	// Are there any unlocked locks?
	def nextLockIndex = findNextIndex()
	
	// Are we finished?
	if (nextLockIndex == -1) {
		doneHandler()
		return
	}
	
	// Lock the next one
	def nextLock = selectedLocks[nextLockIndex]
	log.debug "Lockdown: ATTEMPTING TO LOCK ${nextLock.displayName}"
	nextLock.lock()

	// Start timer for the next cycle
	runIn(cycleTime, cycleHandler)

	// If we're doing refreshes, start a timer for the refresh delay
	if (forceRefresh) {
		atomicState.nextLockIndex = nextLockIndex
		runIn(refreshTime, refreshHandler)
	}
}


def refreshHandler() {
	def nextLock = selectedLocks[atomicState.nextLockIndex]
	
	log.debug "Lockdown: REFRESHING ${nextLock.displayName}"
	nextLock.refresh()
}


// Turn off the triggering switch once execution completes.
def doneHandler() {
	log.debug "Lockdown: DONE"
	
	triggeringSwitch.off()
}


// Find the index of the next lock that is still unlocked.
def findNextIndex() {
	for (int i = 0; i < selectedLocks.size(); i++) {
		def lock = selectedLocks[i];
		
		// Keep track of how many attempts we've made on each lock
		def lockMap = atomicState.lockMap
		def tryCount = lockMap[i]
		if (!tryCount) {
			tryCount = 0
		}
		
		if (lock.currentValue('lock') != "locked" && tryCount < maxCycles) {
			lockMap[i] = tryCount + 1
			atomicState.lockMap = lockMap
			
			return i
		}
	}
	
	return -1
}



