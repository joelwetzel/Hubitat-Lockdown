# Lockdown app for Hubitat
An app for Hubitat (It will probably work with SmartThings too) that will reliably lock all selected locks when a specified switch is triggered.

## Reason for using Lockdown
1.  Z-Wave locks usually run in a low-power mode, and that means their z-wave communications are slightly less reliable than powered devices like z-wave switches.
2.  So, sometimes they don't respond to lock commands the first time.
3.  And, sometimes they don't report their status back after locking, which requires a manual refresh command.
4.  And this is made much worse if you have more than one.  In my case, I had 5 z-wave locks, and if I tried to lock all of them at once, usually only 2 or 3 would be successful.  All the other z-wave messages would get lost or deadlocked.

Therefore, I made an app that could methodically lock/refresh each lock, one-by-one, with retries in case of errors.

## Install Instructions
1. On your Hubitat hub, go to the Apps Code screen
2. Add a New App
3. Copy in the contents of lockdown.groovy and Save
4. Go to the Apps screen and Add User App
5. Choose Lockdown
6. Choose a triggering switch.  Lockdown will watch this switch and activate when the switch triggers to on.
7. Choose locks for Lockdown to manage.
8. Adjust parameters.
9. Click done

## Recommended Integration with Alexa
It's really nice to be able to say "Alexa, lock all the doors".  Here's how:

1. Create a virtual switch in Hubitat.  Let's call it "Lock Everything"
2. Go to the Amazon Echo Skill app in Hubitat, and add the switch to the selected devices.  Click done.
3. Go into your Alexa app and make sure the virtual switch shows up in your devices.
4. Create a new Routine in the Alexa app.  Have it triggered when you say "Lock all the doors".  Have it turn on the virtual switch.

Congratulations.  You can now activate Lockdown by talking to Alexa.

5. Add two more Alexa routines for "Lock the doors" and "Lock all doors", because occasionally Alexa will miss a word.

## Recommended Scheduling / Rules
I want to make sure all my doors are generally locked at night, even if someone steps outside momentarily in the middle of the night and forgets to lock the door after them.  Therefore, in Hubitat I create a scheduled Rule Machine rule that will turn on the Triggering Switch on the hour, each hour, during nighttime hours.

I also create a Rule that is triggered when everyone has left home, that will activate Lockdown, just in case we have forgotten to lock the doors on our way out.
