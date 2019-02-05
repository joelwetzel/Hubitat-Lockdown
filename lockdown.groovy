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
		title:				"Cycle Time",
		description:		"Set the amount of time to wait in between locking different locks (or retrying).  If set too low, The z-wave messages from the locks won't have time to propagate. Recommended value: 5",
		defaultValue:		5,
		required:			true
	]


def maxCycles = [
		name:				"maxCycles",
		type:				"number",
		title:				"Max Number of Cycles",
		description:		"Maximum number of lock/retry cycles.  Recommended value: 3X the amount of locks that you are managing.",
		required:			true
	]


def forceRefresh = [
		name:				"forceRefresh",
		type:				"bool",
		title:				"Force Refreshes after Locking?",
		defaultValue:		true,
		required:			true
	]


def refreshTime = [
		name:				"refreshTime",
		type:				"number",
		title:				"Delay before Refresh",
		description:		"Not all locks perfectly report status updates.  A manual refresh helps.  Set the amount of time to wait after sending a lock command, to send a followup refresh command. Recommend: 5",
		defaultValue:		5,
		required:			true
	]


preferences {
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
	
	atomicState.numCycles = 0
}


def switchOnHandler(evt) {
	log.debug "Lockdown: TRIGGERED"	
	
	atomicState.numCycles = 0
	
	cycleHandler()
}


// Each cycle runs like this:
// 1) See if there's still an unlocked lock
// 2) If so, send it a lock command, then wait "Refresh Time" seconds
// 3) Send it a refresh command, then wait "Cycle Time" seconds before starting a new cycle
def cycleHandler () {
	atomicState.numCycles++

	if (atomicState.numCycles > maxCycles) {
		log.debug "Lockdown: MAX CYCLES EXCEEDED (${maxCycles}).  If this happens regularly, you might have an unresponsive lock."
		doneHandler()
		return
	}

	log.debug "Lockdown: CYCLE ${state.numCycles}"

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
		log.debug "Lockdown: ALL LOCKS ARE LOCKED"
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
		def l = selectedLocks[i];
		
		if (l.currentValue('lock') != "locked") {
			return i
		}
	}
	
	return -1
}








