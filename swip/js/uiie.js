/*
 * GPL 2
 *  
 */ 

function setCallTo(callTo)
{
document.getElementById("PHDial").jsSetCallTo(callTo);
}
function setUsername(username)
{
document.getElementById("PHDial").jsSetUsername(username);
}
function setAuthID(id)
{
document.getElementById("PHDial").jsSetAuthID(id);
}
function setPassword(password)
{
document.getElementById("PHDial").jsSetPassword(password);
}
function setRealm(realm)
{
document.getElementById("PHDial").jsSetRealm(realm);
}
function setPort(port)
{
document.getElementById("PHDial").jsSetPort(port);
}
function setSipProxy(proxy)
{
document.getElementById("PHDial").jsSetSipProxy(port);
}
function setTunnel(tunnel)
{
document.getElementById("PHDial").jsSetTunnel(tunnel);
}
function setHTTPProxy(proxy)
{
// NYI
}
function getIdentityString()
{
var identity = document.getElementById("PHDial").jsGetIdentity();
return identity;
}
function startCall()
{
	document.getElementById("PHDial").set_event(102);
	//stopActive();
}
function endCall()
{
	document.getElementById("PHDial").set_event(103);
	//startActive();
}
function acceptCall()
{
	document.getElementById("PHDial").set_event(104);
	
}
function refuseCall()
{
	document.getElementById("PHDial").set_event(105);
	
}


function autoreg() {

  document.getElementById("PHDial").jsSetUsername(document.getElementById("username_param").value);
  document.getElementById("PHDial").jsSetPassword(document.getElementById("password_param").value);
  document.getElementById("PHDial").set_event(101);

}

function register()
{

    if ( $('#realmsel').val() != null) {

       document.getElementById("PHDial").jsSetRealm($('#realmsel').val());
       document.getElementById("PHDial").jsSetSipProxy($('#realmsel').val());

    }


	document.getElementById("PHDial").jsSetUsername(document.getElementById("username").value);
	document.getElementById("PHDial").jsSetPassword(document.getElementById("password").value);
	document.getElementById("PHDial").set_event(101);
	//startActive();
}
function onIMSend()
{
	document.getElementById("PHDial").jsSetCallTo(document.getElementById("callto").value);		
	
	var sender = "<font class=\"immessage_to\">" + 'Sip Applet' + " </font>";
	var msg = document.getElementById("immessageform").imMessageArea.value;
	
	if(msg.length == 0 || msg.length > 400)
		return false;
	
	msg = removeHTMLTags(msg);	
	
	customOnImSend(msg, sender);
	
	document.getElementById("immessageform").imMessageArea.value = "";
	sendIMMessage(msg);
}

function sendIMMessage(message)
{
	document.getElementById("PHDial").set_IMMessage(message);
 	document.getElementById("PHDial").set_event(106); //sendim
}

function removeHTMLTags(message)
{
	var MessageWithoutHTML = message.replace(/<\/?[^>]+(>|$)/g, "");
	return MessageWithoutHTML;
}
/*function startActive()
*{ 
*   $('#startbuttontext').css('background-image', "url(/index.php/module/wrapper/resource/idial/item/button_active.gif)");
*   $('#stopbuttontext').css('background-image', "url(/index.php/module/wrapper/resource/idial/item/button_not_active.gif)");  
*   $('#stopcallbutton').css('cursor', "default");  
*   $('#callbutton').css('cursor', "pointer");  
*}
*/
function DTMFButton(number)
{

    var str_repr;

    if (number == 10) str_repr = '*';
    else if (number == 11) str_repr = '#';
    else str_repr = number;

    $('#callto').val($('#callto').val() + str_repr);
	document.getElementById("PHDial").handleKeyPadEvent(number);

}
function dialingPageDeactivate(){document.getElementById("PHDial").set_event(999);}//Not used but must be defined

function subscribe(presentity)
{
	document.getElementById("PHDial").jsSubscribe(presentity);	
	document.getElementById("PHDial").set_event(121); // subscribe
}
function unSubscribe(presentity)
{
	document.getElementById("PHDial").jsUnSubscribe(presentity);
	document.getElementById("PHDial").set_event(122); // unsubscribe
}
function publish(st, note)
{
	document.getElementById("PHDial").jsPublish(st, note);
	document.getElementById("PHDial").set_event(123); // publish
}

function swipDebug(msg) {

     var sdd = document.getElementById("swip_debug_ct");

     sdd.innerHTML += '<br/>' + msg;
     sdd.scrollTop = sdd.scrollHeight;

}
