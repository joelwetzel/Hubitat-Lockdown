package joelwetzel.dimmer_minimums.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import me.biocomp.hubitat_ci.validation.Flags

import spock.lang.Specification

/**
* Behavior tests for lockdown.groovy, if we have old locks that don't report back results reliably without a refresh.
*/
class OldLockTests extends IntegrationAppSpecification {
    def switchFixture = SwitchFixtureFactory.create('s1')

    def lockFixture1 = LockFixtureFactory.create('l1')
    def lockFixture2 = LockFixtureFactory.create('l2')
    def lockFixture3 = LockFixtureFactory.create('l3')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "lockdown.groovy",
                                    validationFlags: [Flags.AllowWritingToSettings],
                                    userSettingValues: [triggeringSwitch: switchFixture, selectedLocks: [lockFixture1, lockFixture2, lockFixture3], cycleTime: 5, maxCycles: 3, forceRefresh: true, refreshTime: 5])

        switchFixture.initialize(appExecutor, [switch:"off"])
        lockFixture1.initialize(appExecutor, [lock:"unlocked"])
        lockFixture2.initialize(appExecutor, [lock:"unlocked"])
        lockFixture3.initialize(appExecutor, [lock:"unlocked"])

        appScript.installed()
    }

    void "Simplified test that advances to the final state"() {
        when: "App is triggered"
        switchFixture.on()

        and:
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)

        then:
        1 * log.debug('Lockdown: DONE')
        lockFixture1.currentValue('lock') == 'locked'
        lockFixture2.currentValue('lock') == 'locked'
        lockFixture3.currentValue('lock') == 'locked'
    }

    void "An unresponsive lock will be skipped, but processing will continue and complete"() {
        given:
        lockFixture2.setCommandsToIgnore(5)

        when: "App is triggered"
        switchFixture.on()

        and: "We need extra cycles, because it's going to retry the second lock 2 more times"
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)

        then: "The second lock will be skipped, but the first and third will still be locked"
        1 * log.debug('Lockdown: DONE')
        lockFixture1.currentValue('lock') == 'locked'
        lockFixture2.currentValue('lock') == 'unlocked'
        lockFixture3.currentValue('lock') == 'locked'
    }

    void "If we have old locks that don't report back results without a refresh, and we don't force refreshes, lockdown will eventually finish without knowing the true states of the locks"() {
        given:
        lockFixture1.setRequireRefresh(true)
        lockFixture2.setRequireRefresh(true)
        lockFixture3.setRequireRefresh(true)
        appScript.forceRefresh = false

        when: "App is triggered"
        switchFixture.on()

        and: "We need extra cycles, because it's going to retry all 3 locks 3 times each"
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)

        then: "The app will finish, but the locks will still be in their initial states"
        1 * log.debug('Lockdown: DONE')
        lockFixture1.currentValue('lock') == 'unlocked'
        lockFixture2.currentValue('lock') == 'unlocked'
        lockFixture3.currentValue('lock') == 'unlocked'
    }

    void "If we have old locks that don't report back results without a refresh, having forceRefresh=true will ensure that lockdown will eventually finish with the true states of the locks"() {
        given:
        lockFixture1.setRequireRefresh(true)
        lockFixture2.setRequireRefresh(true)
        lockFixture3.setRequireRefresh(true)
        appScript.forceRefresh = true

        when: "App is triggered"
        switchFixture.on()

        and: "Should only take normal amount of time, because the refreshes should complete before the next cycles."
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)
        TimeKeeper.advanceMillis(5001)

        then: "The app will finish, and the locks will be successfully locked."
        1 * log.debug('Lockdown: DONE')
        lockFixture1.currentValue('lock') == 'locked'
        lockFixture2.currentValue('lock') == 'locked'
        lockFixture3.currentValue('lock') == 'locked'
    }

}
