package joelwetzel.dimmer_minimums.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Basic tests for lockdown.groovy
*/
class BasicTests extends IntegrationAppSpecification {
    def switchFixture = SwitchFixtureFactory.create('s1')

    def lockFixture1 = LockFixtureFactory.create('l1')
    def lockFixture2 = LockFixtureFactory.create('l2')
    def lockFixture3 = LockFixtureFactory.create('l3')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "lockdown.groovy",
                                    userSettingValues: [triggeringSwitch: switchFixture, selectedLocks: [lockFixture1, lockFixture2, lockFixture3], cycleTime: 5, maxCycles: 3, forceRefresh: true, refreshTime: 5])
    }

    void "installed() logs the settings"() {
        when:
        appScript.installed()

        then:
        1 * log.info('Installed with settings: [triggeringSwitch:GeneratedDevice(input: s1, type: t), selectedLocks:[GeneratedDevice(input: l1, type: t), GeneratedDevice(input: l2, type: t), GeneratedDevice(input: l3, type: t)], cycleTime:5, maxCycles:3, forceRefresh:true, refreshTime:5]')
    }

    void "initialize() subscribes to events"() {
        when:
        appScript.initialize()

        then:
        1 * appExecutor.subscribe(switchFixture, 'switch.on', 'switchOnHandler')
    }

    void "initialize sets atomicState"() {
        when:
        appScript.initialize()

        then:
        // Expect that atomicState.lockMap is set to an array of ints the same size as the selectedLocks array
        appAtomicState.lockMap instanceof int[]
        appAtomicState.lockMap.size() == 3
    }

}
