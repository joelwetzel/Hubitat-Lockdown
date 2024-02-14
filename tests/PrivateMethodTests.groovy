package joelwetzel.dimmer_minimums.tests

import me.biocomp.hubitat_ci.util.device_fixtures.SwitchFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LockFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Tests of private methods for lockdown.groovy
*/
class PrivateMethodTests extends IntegrationAppSpecification {
    def switchFixture = SwitchFixtureFactory.create('s1')

    def lockFixture1 = LockFixtureFactory.create('l1')
    def lockFixture2 = LockFixtureFactory.create('l2')
    def lockFixture3 = LockFixtureFactory.create('l3')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "lockdown.groovy",
                                    userSettingValues: [triggeringSwitch: switchFixture, selectedLocks: [lockFixture1, lockFixture2, lockFixture3], cycleTime: 5, maxCycles: 3, forceRefresh: true, refreshTime: 5])

        switchFixture.initialize(appExecutor, [switch:"off"])
        lockFixture1.initialize(appExecutor, [lock:"unlocked"])
        lockFixture2.initialize(appExecutor, [lock:"unlocked"])
        lockFixture3.initialize(appExecutor, [lock:"unlocked"])

        appScript.installed()
    }

    void "findNextIndex returns 0 when all locks are unlocked"() {
        when:
        def result = appScript.findNextIndex()

        then:
        result == 0
    }

    void "findNextIndex returns -1 when all locks are locked"() {
        given:
        lockFixture1.lock()
        lockFixture2.lock()
        lockFixture3.lock()

        when:
        def result = appScript.findNextIndex()

        then:
        result == -1
    }

    void "findNextIndex returns 1 when first lock is locked"() {
        given:
        lockFixture1.lock()

        when:
        def result = appScript.findNextIndex()

        then:
        result == 1
    }

    void "findNextIndex returns 1 when the first lock has had too many retries"() {
        given:
        appAtomicState.lockMap = [3, 0, 0]

        when:
        def result = appScript.findNextIndex()

        then:
        result == 1
    }

    void "findNextIndex returns 2 when the first two locks have had too many retries"() {
        given:
        appAtomicState.lockMap = [3, 3, 0]

        when:
        def result = appScript.findNextIndex()

        then:
        result == 2
    }
}
