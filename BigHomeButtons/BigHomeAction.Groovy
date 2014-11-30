/**
 *  Big Hello Home Action
 *
 *  Author: Geurt Wisselink
 */
definition(
    name: "Big Hello Home Action",
    namespace: "Wiscon",
    author: "Geurt Wisselink",
    description: "Send Hello Action when the SmartApp is tapped or activated",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	page(name: "setupApp")
}

def setupApp() {
    dynamicPage(name: "setupApp", title: "Configure", install: true, uninstall: true) {
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
			phrases.sort()
			section("Hello Home actions...") {
				input name: "actionPhrase", type: "enum", title: "Action Phrase", required: true, options: phrases, refreshAfterSelection: true
				if (manualUnlockException) {
					input name: "manualPhrase", type: "enum", title: "Manual Action Phrase", required: true, options: phrases, refreshAfterSelection: true
				}
			}        
		}
				
        section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
			mode title: "Set for specific mode(s)", required: false
		}
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
	location.helloHome.execute(settings.actionPhrase)
}

private send(msg) {
	sendPush msg
	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}