package joelwetzel.lockdown.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
import me.biocomp.hubitat_ci.util.IntegrationAppExecutor

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.capabilities.GeneratedCapability
import me.biocomp.hubitat_ci.util.NullableOptional
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Basic tests for lockdown.groovy
*/
class BasicTests extends Specification {
    private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('lockdown.groovy'))

    def log = Mock(Log)

    InstalledAppWrapper app = Mock{
        _ * getName() >> "MyAppName"
    }

    def appState = [:]
    def appAtomicState = [:]

    def appExecutor = Spy(IntegrationAppExecutor) {
        _*getLog() >> log
        _*getApp() >> app
        _*getState() >> appState
        _*getAtomicState() >> appAtomicState
    }

    def switchFixture = SwitchFixtureFactory.create('s1')

    def lockFixture1 = LockFixtureFactory.create('l1')
    def lockFixture2 = LockFixtureFactory.create('l1')
    def lockFixture3 = LockFixtureFactory.create('l1')

    def appScript = sandbox.run(api: appExecutor,
        userSettingValues: [triggeringSwitch: switchFixture, selectedLocks: [lockFixture1, lockFixture2, lockFixture3], cycleTime: 5, maxCycles: 3, forceRefresh: true, refreshTime: 5])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
    }

    void "installed() logs the settings"() {
        when:
        // Run installed() method on app script.
        appScript.installed()

        then:
        // Expect that log.info() was called with this string
        1 * log.info('Installed with settings: [triggeringSwitch:GeneratedDevice(input: s1, type: t), selectedLocks:[GeneratedDevice(input: l1, type: t), GeneratedDevice(input: l1, type: t), GeneratedDevice(input: l1, type: t)], cycleTime:5, maxCycles:3, forceRefresh:true, refreshTime:5]')
    }

    void "initialize() subscribes to events"() {
        when:
        appScript.initialize()

        then:
        // Expect that events are subscribe to
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
