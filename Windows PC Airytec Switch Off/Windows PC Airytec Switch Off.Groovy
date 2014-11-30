/**
 *	Windows PC Airytec Switch Off
 *
 *	Author: Geurt Wisselink
 *	Date: 2014-11-29
 */
 
preferences {
    input("configIpAddr", "string", title:"IP Address", required:true, displayDuringSetup: true)
    input("configTcpPort", "number", title:"TCP Port", defaultValue:"80", required:true, displayDuringSetup:true)
    input("configUsername", "string", title:"Username", required:true, displayDuringSetup: true)
    input("configPassword", "string", title:"Password", required:true, displayDuringSetup:true)
    input("configSwitchOffUrl", "string", title:"Switch Off URL", defaultValue:"/?action=System.Shutdown", required:true, displayDuringSetup: true)
}
 
 // for the UI
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Windows PC Airytec Switch", namespace: "Wiscon", author: "Geurt Wisselink") {
		capability "Switch"
		capability "Polling"
		capability "Refresh"

        // Custom attributes
		attribute "networkAddress", "string"
	}

	// simulator metadata
	simulator {}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.Kids.kids6", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.Kids.kids6", backgroundColor:"#ffffff", nextState:"off"
			state "turningOff", label:'${name}', icon:"st.Kids.kids6", backgroundColor:"#ffffff"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("networkAddress", "device.networkAddress", decoration: "flat", height: 1, width: 2, inactiveLabel: false) {
			state "default", label:'${currentValue}', height: 1, width: 2, inactiveLabel: false
		}
		main "switch"
		details (["switch", "refresh", "networkAddress"])
	}
}

// parse events into attributes
def parse(String description) {
	TRACE("Parsing '${description}'")

	def map = stringToMap(description)
	def headerString = new String(map.headers.decodeBase64())

	def events = []

	if (map.body) {
		//device is turned on
		if (getDataValue("pollState") != "on") {
			TRACE("parse: oldPollState: ${getDataValue("pollState")} - A message was received, device is  ON")
			updateDataValue("pollState", "on")
			// this is /1/system response
			events << createEvent(name: "switch", value: "on")
			TRACE("parse: newPollState: ${getDataValue("pollState")}")
		}		
		
		def bodyString = new String(map.body.decodeBase64())
		TRACE("parseData\n${bodyString}")
	}

	TRACE("events: ${events}")
	events
}

////////////////////////////
private getTime() {
	// This is essentially System.currentTimeMillis()/1000, but System is disallowed by the sandbox.
	((new GregorianCalendar().time.time / 1000l).toInteger()).toString()
}

private getCallBackAddress() {
	device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	String networkAddress = configIpAddr + ":" + configTcpPort
	//if (networkAddress != getDataValue("networkAddress")) {
		sendEvent([name:"networkAddress", value:networkAddress])
		setNetworkId(configIpAddr, configTcpPort)
	//}
	
	TRACE("Using ip: ${networkAddress} for device: ${device.id}, networkId: ${device.deviceNetworkId}")
	return networkAddress
}

// Sets device Network ID in 'AAAAAAAA:PPPP' format
private String setNetworkId(ipaddr, port) { 
    TRACE("setNetworkId(${ipaddr}, ${port})")

    def hexIp = ipaddr.tokenize('.').collect {
        String.format('%02X', it.toInteger())
    }.join()

    def hexPort = String.format('%04X', port.toInteger())
    device.deviceNetworkId = "${hexIp}:${hexPort}"
	updateDataValue("ip", hexIp)
	updateDataValue("port", hexPort)
    log.debug "device.deviceNetworkId = ${device.deviceNetworkId}"
}

private getCredentials() {
	def userpassAscii = "${configUsername}:${configPassword}"
	return "Basic " + userpassAscii.encodeAsBase64().toString()
}
   

////////////////////////////
def on() {
	TRACE("Executing 'on' - does nothing")
}

////////////////////////////
def off() {
	TRACE("Executing 'off'")
	sendEvent([name:"key", value:"Standby"])
	
	def sendKey = apiGet( configSwitchOffUrl)
	sendKey
}

////////////////////////////
def refresh() {
	TRACE("Executing 'refresh'")
	
	if (getDataValue("pollState") != "on") {
		TRACE("refresh: oldPollState: ${getDataValue("pollState")} - second time - assume device is OFF")
		sendEvent([name:"switch", value:"off"])
	}
	updateDataValue("pollState", "poll")
	sendEvent([name:"refresh", value:"on"])
	TRACE("refresh: newPollState: ${getDataValue("pollState")}")
	
	def systemInfo = apiGet("/")
	systemInfo
}

def poll() {
	TRACE( "Executing 'poll'")
	refresh()
}

private apiGet(String path) {
    log.debug "apiGet(${path})"

    def headers = [
        HOST: 		getHostAddress(),
		Authorization: getCredentials(),
        Accept: "*/*"
    ]

    def httpRequest = [
        method:     'GET',
        path:       path,
        headers:    headers
    ]

    return new physicalgraph.device.HubAction(httpRequest)
}

private apiPost(String path, data) {
    log.debug "apiPost(${path}, ${data})"

    def headers = [
        HOST:       getHostAddress(),
		Authorization: getCredentials(),
        Accept:     "*/*"
    ]

    def httpRequest = [
        method:     'POST',
        path:       path,
        headers:    headers,
        body:       data
    ]

    return new physicalgraph.device.HubAction(httpRequest)
}

private def delayHubAction(ms) {
    return new physicalgraph.device.HubAction("delay ${ms}")
}

private def TRACE(message) {
    log.debug message
}