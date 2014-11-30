/**
 *  Big Mode Change
 *
 *  Author: Geurt Wisselink
 */
definition(
    name: "Big Mode Change",
    namespace: "Wiscon",
    author: "Geurt Wisselink",
    description: "Switch mode when the SmartApp is tapped or activated",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
    section("Change to this mode") {
		input "newMode", "mode", title: "Mode?"
	}
}

def installed()
{
	subscribe(app, appTouch)
}

def updated()
{
	subscribe(app, appTouch)
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
	changeMode()
}

def changeMode() {
	if (location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			send "${label} has changed the mode to '${newMode}'"
		}
		else {
			send "${label} tried to change to undefined mode '${newMode}'"
		}
	}
}

private send(msg) {
	sendPush msg
	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}