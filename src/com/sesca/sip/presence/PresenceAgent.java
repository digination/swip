/* 
/  Copyright (C) 2009  Risto Känsäkoski, Antti Alho - Sesca ISW Ltd
/  
/  This file is part of SIP-Applet (www.sesca.com, www.purplescout.com)
/
/  This program is free software; you can redistribute it and/or
/  modify it under the terms of the GNU General Public License
/  as published by the Free Software Foundation; either version 2
/  of the License, or (at your option) any later version.
/
/  This program is distributed in the hope that it will be useful,
/  but WITHOUT ANY WARRANTY; without even the implied warranty of
/  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/  GNU General Public License for more details.
/
/  You should have received a copy of the GNU General Public License
/  along with this program; if not, write to the Free Software
/  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.sesca.sip.presence;

import local.net.KeepAliveSip;
import local.ua.RegisterAgentListener;

import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.header.*;
import org.zoolu.sip.message.*;
import org.zoolu.sip.transaction.TransactionClient;
import org.zoolu.sip.transaction.TransactionClientListener;
import org.zoolu.sip.authentication.DigestAuthentication;
import org.zoolu.sip.dialog.SubscriberDialog;
import org.zoolu.sip.dialog.SubscriberDialogListener;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

import com.sesca.misc.Logger;
import com.sesca.sip.presence.pidf.Presentity;
import com.sesca.sip.presence.pidf.SimpleParser;
import com.sesca.sip.presence.pidf.Tuple;
import com.sesca.voip.ua.AppletUANG;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class PresenceAgent implements Runnable, SubscriberDialogListener, TransactionClientListener, PublishSchedulerListener
{
   

   PresenceAgentListener listener;
   

   SipProvider sip_provider;


   NameAddress target;


   String username;


   String realm;
   

   String authName;


   String passwd;


   String next_nonce;


   String qop;


   NameAddress contact; 


   int expire_time;


   int renew_time;


   boolean loop;


   boolean running = false;


   Log log;


   int attempts;
   

   //KeepAliveSip keep_alive;

   SubscriberDialog subscriberDialog=null;

   String currentPresenceStatus="";
   
   String currentPresenceNote = "";
   
   boolean published = false;
   
   HashMap<String, Presentity> presentities=null;
   
   Hashtable dialogs=null;
   
   int publishExpireTime=0;
   long publishedTime=0;
   long initialTime=0;
   static int hysteresis=30;
   PublishScheduler ps;
   
   public PresenceAgent(SipProvider sip_provider,  String contact_url, PresenceAgentListener listener)
   {  init(sip_provider,contact_url,listener);
   }
   
   
  
   public PresenceAgent(SipProvider sip_provider,  String contact_url, String username, String realm, String passwd, PresenceAgentListener listener)
   {  init(sip_provider,contact_url,listener);
      // authentication
      this.username=username;
      this.realm=realm;
      this.passwd=passwd;
      this.authName=username;
   }
   public PresenceAgent(SipProvider sip_provider,  String contact_url, String username, String authName, String realm, String passwd, PresenceAgentListener listener)
   {  init(sip_provider,contact_url,listener);
      // authentication
      this.username=username;
      this.realm=realm;
      this.passwd=passwd;
      this.authName=authName;
   }

   



private void init(SipProvider sip_provider, String contact_url, PresenceAgentListener listener)
   {  this.listener=listener;
      this.sip_provider=sip_provider;
      this.log=sip_provider.getLog();
      
      this.contact=new NameAddress(contact_url);
      this.expire_time=SipStack.default_expires;
      this.renew_time=0;
      this.running=false;
      //this.keep_alive=null;
      // authentication
      this.username=null;
      this.realm=null;
      this.passwd=null;
      this.next_nonce=null;
      this.qop=null;
      this.attempts=0;
      presentities = new HashMap();
      dialogs = new Hashtable();
      initialTime=System.currentTimeMillis();
      
      
   }
/*
   public void subscribe(int expire_time)
   {
	   attempts=0;

	   	  MessageFactory msgf = new MessageFactory();
	      if (expire_time>0) this.expire_time=expire_time;
	      //Message req=msgf.createSubscribeRequest(sip_provider, recipient, to, from, contact, event, id, content_type, body, remoteTag)
	      Message req=msgf.createSubscribeRequest(sip_provider, new SipURL("recipient"), new NameAddress("to"), new NameAddress("from"), new NameAddress("contact"), "event", "id", "content_type", "body", "remoteTag");
	      
	      req.setExpiresHeader(new ExpiresHeader(String.valueOf(expire_time)));
	      if (next_nonce!=null)
	      {  AuthorizationHeader ah=new AuthorizationHeader("Digest");
	         SipURL target_url=target.getAddress();
	         ah.addUsernameParam(username);
	         ah.addRealmParam(realm);
	         ah.addNonceParam(next_nonce);
	         ah.addUriParam(req.getRequestLine().getAddress().toString());
	         ah.addQopParam(qop);
	         String response=(new DigestAuthentication(SipMethods.REGISTER,ah,null,passwd)).getResponse();
	         ah.addResponseParam(response);
	         req.setAuthorizationHeader(ah);
	      }
	      //if (expire_time>0) printLog("Registering contact "+contact+" (it expires in "+expire_time+" secs)",LogLevel.HIGH);
	      //else printLog("Unregistering contact "+contact,LogLevel.HIGH);
	     System.out.println(req.toString());
	
   }
   */
   public void subscribe(int expireTime, String to)
   {
	   if (to==null || to.length()==0 ) return;
//	   System.out.println("PresenceAgent.subscribeInDialog()");	   
	   boolean newDialog = true;
		if(expireTime < 0)
			expireTime = SipStack.default_expires;
	   
	   if (dialogs.containsKey(to))
	   {
		   subscriberDialog=(SubscriberDialog)dialogs.get(to);
		   if(subscriberDialog == null)
				subscriberDialog = new SubscriberDialog(sip_provider, "presence", null, this);
		   else newDialog=false;
		   
	   }
	   else
	   {
		   if (expireTime==0) return; 
		   else subscriberDialog = new SubscriberDialog(sip_provider, "presence", null, this);
	   }
	   if (newDialog)
	   {
		   String from = username+"@"+realm;
		
//		   System.out.println("to="+to);
//		   if (target!=null)System.out.println("target="+target.toString());
//		   if (from!=null)System.out.println("from="+from);
//		   if (contact!=null)System.out.println("contact="+contact.toString());
		   subscriberDialog.subscribe(to, to, from , contact.toString(), expireTime, username, passwd, realm);		
		
		   dialogs.put(to, subscriberDialog); 
		
		   //subscribe(String target, String subscriber, String contact, int expires)
		
		   Presentity prs= new Presentity(to,Presentity.statusPending);
		   if (presentities.containsKey(to))presentities.remove(to);
		   presentities.put(to, prs);
		   Logger.debug("to presentities: "+to);
		   //printPresentities();
		   
	   }
	   else
	   {
		   subscriberDialog.reSubscribe(expireTime);
		   dialogs.put(to, subscriberDialog); 
			
		   //subscribe(String target, String subscriber, String contact, int expires)
		
		   Presentity prs;
		   if (presentities.containsKey(to)) prs=presentities.get(to);
		   else prs=new Presentity(to, Presentity.statusPending);
		   prs.setStatus(Presentity.statusPending);
		   presentities.put(to, prs);
		   Logger.debug("to presentities: "+to);
		   //printPresentities();
		   
	   }
	   if (expireTime==0)
	   {
		   Presentity prs = null;
		   if (presentities.containsKey(to)) prs=presentities.get(to);
//		   System.out.println(presentities.containsKey(to));
//		   System.out.println(presentities.containsKey(to));
		   if (prs!= null)
		   {
			   prs.setStatus(Presentity.statusCancelled);
			   presentities.put(to, prs);
		   }
	   }
	   onPresenceUpdate();
		
   }
   public void publish(String status, String note, int expireTime)
   {
	   publish(status, note, expireTime, false);
   }
   public void publish(String status, String note, int expireTime, boolean forced)
   {
	   if (expireTime<hysteresis && expireTime>0)expireTime+=hysteresis;
	   boolean statusChanged=false;
	   status=status.toLowerCase().trim();
	   note=note.trim();
	   if (!status.equals("open") && !status.equals("closed"))
	   {
		   Logger.error("Illegal presence status in pidf");
		   return;
	   }
	   if (!status.equals(currentPresenceStatus) || !note.equals(currentPresenceNote)) statusChanged=true;
	   if (!statusChanged && !forced)
	   {
		   Logger.debug("Presence has not changed");
		   return;
	   }
	   
	   currentPresenceStatus=status;
	   currentPresenceNote=note;
	   //if(subscriberDialog == null)
		//	subscriberDialog = new SubscriberDialog(sip_provider, "presence", null, this);
		if(expireTime < 0)
			expireTime = SipStack.default_expires;
		//subscriberDialog.subscribe(target.toString(), contact.getAddress().getUserName()+"@"+contact.getAddress().getHost(), contact.toString(), expireTime);
		
		
		String from = username+"@"+realm;
		
//		System.out.println("esc="+esc);
		//System.out.println("target="+target.toString());
//		System.out.println("from="+from);
		//System.out.println("contact="+contact.toString());
		
		MessageFactory msgf = new MessageFactory();		
		Message req = msgf.createPublishRequest(sip_provider, new NameAddress(from), "presence");
		req.setExpiresHeader(new ExpiresHeader (expireTime));
		String tupleId=sip_provider.UAIdentity.replace("/", "").replace(":", "").toLowerCase();
		String entity="sip:"+username+"@"+realm;
		//tupleId="ck38g9";
		String xml=
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
				"<presence xmlns=\"urn:ietf:params:xml:ns:pidf\""+
                "entity=\""+ entity +"\">"+
                "<tuple id=\""+ tupleId +"\">"+
                "<status>"+
                "<basic>"+status+"</basic>"+
            	"</status>";
		if (note !="" && note.length()>0)
                xml+="<note>"+note+"</note>";
		xml +=
				"</tuple>"+
                "</presence>";
		
		req.setBody("application/pidf+xml", xml);
		
//		System.out.println("PUBLISH message:");
//		System.out.println(req);
		TransactionClient t = new TransactionClient(sip_provider, req, this);
		t.request();
		publishExpireTime=expireTime;
		publishedTime=System.currentTimeMillis();
		published=false;
		run();

   }
   
   /** Run method */
   public void run()
   {  
	  ps=new PublishScheduler(this);
      ps.init(publishedTime, publishExpireTime, hysteresis);
      ps.start();
   }



@Override
public void onDlgNotify(SubscriberDialog dialog, NameAddress target,
		NameAddress notifier, NameAddress contact, String state,
		String content_type, String body, Message msg) {
	String n=null;
//	System.out.println("PresenceAgent.onDlgNotify");
//	System.out.println("target="+target);
//	System.out.println("notifier="+notifier);
	if (notifier!=null)n=(notifier.getAddress().toString());
	
//	System.out.println("contact="+contact);
//	System.out.println("state="+state);
//	System.out.println("msg="+msg);
//	System.out.println("content-type="+content_type);
//	System.out.println("body"+body);
	
	boolean fail=true;
	if (content_type != null && content_type.toLowerCase().equals("application/pidf+xml")) fail=false;
	if (!presentities.containsKey(n)) fail=true;
	if (!fail && body != null && state.trim().toLowerCase().equals("active"))
	{
		Logger.debug("pa.onDlgNotify sucksess");
		SimpleParser p = new SimpleParser();
		p.parse(body);
		Vector<Tuple> tuples=p.getTuples();
		Presentity prs=new Presentity(n, Presentity.statusActive);
//		System.out.println("tuples vector size="+tuples.size());
		for (int i=0;i<tuples.size();i++)
		{
			
			Tuple t=tuples.elementAt(i);
//			System.out.println("tuple "+i+" "+t.getId());
			prs.addTuple(t.getId(), t);
			
		}
		presentities.put(n, prs);
		
		//printPresentities();
		onPresenceUpdate();
	}
	else
	{
		Logger.debug("pa.onDlgNotify fails");
		if (body == null && presentities.containsKey(n))
			{
			Logger.debug("body is empty");
			if (state.trim().toLowerCase().equals("active"))
				{
					Logger.debug("new active subscription");
					Presentity prs=new Presentity(n, Presentity.statusActive);
					presentities.put(n,prs);
				}
			else {
				Logger.debug("subscription not active");
			}
			
			}
		else Logger.debug("presentity not in list");
		//printPresentities();
		onPresenceUpdate();		
		return;
	}
	
	// TODO Auto-generated method stub
	
}



@Override
public void onDlgSubscribeTimeout(SubscriberDialog dialog) {
	// TODO Auto-generated method stub
	System.out.println("PresenceAgent.onDlgSubscribeTimeout");
}



@Override
public void onDlgSubscriptionFailure(SubscriberDialog dialog, int code,
		String reason, Message msg) {
	
	// TODO Auto-generated method stub
//	System.out.println("PresenceAgent.onDlgSubscriptionFailure, code="+code);
//	System.out.println(msg.getFromHeader().getNameAddress().toString());
	String key=(msg.getToHeader().getNameAddress().toString()).replace("<", "").replace(">","");
	if (dialogs.containsValue(dialog)) 
	{
		if (dialogs.containsKey(key) && dialogs.get(key)==dialog)
		{
			dialogs.remove(key);
		}
	}
	if (presentities.containsKey(key)) presentities.remove(key);
	onPresenceUpdate();
	 
		
	
}



@Override
public void onDlgSubscriptionSuccess(SubscriberDialog dialog, int code,
		String reason, Message msg) {
	
	// TODO Auto-generated method stub
//	System.out.println("PresenceAgent.onDlgSubscriptionSuccess: "+msg.getToHeader().getNameAddress().toString().replace("<", "").replace(">",""));
	String s =msg.getToHeader().getNameAddress().toString().replace("<", "").replace(">",""); 
//	if (presentities.containsKey(s)) System.out.println("LÖYTYY "+s);
//	else System.out.println("EI LÖYDY "+s);
	//printPresentities();
	onPresenceUpdate();
}



@Override
public void onDlgSubscriptionTerminated(SubscriberDialog dialog) {
	// TODO Auto-generated method stub
//	System.out.println("PresenceAgent.onDlgSubscriptionTerminated");
	String to = null;
	Iterator it = dialogs.keySet().iterator();
	while(it.hasNext())	
	{
		String key = (String)it.next();
		if (dialogs.get(key).equals(dialog))
		{
//			System.out.println("--->LÖYTYI<---");
			to=key;
			break;
		}

	}
	if (to!=null)
	{
		//presentities.remove(to);
		Presentity prs = presentities.get(to);
		if (prs!=null)
		{
			if (prs.getStatus().equals(Presentity.statusCancelled))
			{
				presentities.remove(to);
				dialogs.remove(to);
				dialog=null;				
			}
			else
			{
				prs.setStatus(Presentity.statusExpired);
				presentities.put(to, prs);
				dialogs.remove(to);
				dialog=null;
				subscribe(3600, to);
			}
		}
	}
	onPresenceUpdate();
	
}

   
   // **************** Transaction callback functions *****************
private boolean parse(String xml)
{
	/* int index=0;
	int pIndex = xml.indexOf("<presence");
	if (pIndex==0) return false;
	index = pIndex;
	int pEnd = xml.indexOf(">", index+1);
	*/
	
	int start=xml.indexOf("<basic>");
	int end=xml.indexOf("</basic>");
	String status=xml.substring(start+7, end);
//	System.out.println("PRESENCE="+status);
	
	return true;
	}



@Override
public void onTransFailureResponse(TransactionClient tc, Message resp) {

//	System.out.println("onTransFailureResponse");

	// Autentikointi
	String method = tc.getTransactionMethod();
	StatusLine status_line = resp.getStatusLine();
	int code = status_line.getCode();
	// AUTHENTICATION-BEGIN
	if((code == 401 && resp.hasWwwAuthenticateHeader() && resp.getWwwAuthenticateHeader().getRealmParam().equalsIgnoreCase(realm)) || (code == 407 && resp.hasProxyAuthenticateHeader() && resp.getProxyAuthenticateHeader().getRealmParam().equalsIgnoreCase(realm)))
	{
		// req:ssa on cseq:ua kasvatettu   
		Message req = tc.getRequestMessage();
		req.setCSeqHeader(req.getCSeqHeader().incSequenceNumber());
		WwwAuthenticateHeader wah;
		if(code == 401)
			wah = resp.getWwwAuthenticateHeader();
		else
			wah = resp.getProxyAuthenticateHeader();
		String qop_options = wah.getQopOptionsParam();
		qop = (qop_options != null) ? "auth" : null;
		RequestLine rl = req.getRequestLine();
		DigestAuthentication digest = new DigestAuthentication(rl.getMethod(), rl.getAddress().toString(), wah, null, null, authName, passwd);
		AuthorizationHeader ah;
		if(code == 401)
			ah = digest.getAuthorizationHeader();
		else
			ah = digest.getProxyAuthorizationHeader();
		req.setAuthorizationHeader(ah);
		tc = new TransactionClient(sip_provider, req, this);
		tc.request();

	}
	// AUTHENTICATION-END	      


	
}



@Override
public void onTransProvisionalResponse(TransactionClient tc, Message resp) {
	// TODO Auto-generated method stub
//	System.out.println("onTransProvisionalResponse");
}



@Override
public void onTransSuccessResponse(TransactionClient tc, Message resp) {
	// TODO Auto-generated method stub
//	System.out.println("onTransSuccessResponse");
	published=true;
}



@Override
public void onTransTimeout(TransactionClient tc) {
	// TODO Auto-generated method stub
//	System.out.println("onTransTimeoutResponse");	
}
private void printPresentities()
{
//	System.out.println("printPresentities");
	if (false)
	{
	//Vector<String> rows = new Vector<String>();  
	Iterator it = presentities.keySet().iterator();
	while(it.hasNext()) 
	{
		String row;
		Object key = it.next();
		Presentity val = presentities.get(key);
		String con=val.getContact();
		String sta=val.getStatus();
	    
		//System.out.print("("+key+") "+con+", "+sta);
		row=("("+key+") "+con+", "+sta);
		
		HashMap tuples=(HashMap)val.getTuples();
	    if (tuples!=null && !tuples.isEmpty())
	    {
	    	//System.out.println("iteroidaan tuplet ("+tuples.size()+") kpl");
	    	Iterator tit = tuples.keySet().iterator();
	    	while(tit.hasNext()) 
	    	{
	    		Object tkey = tit.next();
	    		//System.out.println(tkey);
	    		Tuple tval = (Tuple)tuples.get(tkey);
	    		//System.out.println("-> "+tval.getId()+", "+tval.getStatus_basic());
	    		row+=("-> "+tval.getId()+", "+tval.getStatus_basic());
	    	}
	    	
	    }
	    //else System.out.println("");
//	    System.out.println(row);
	} 
	}
}
void onPresenceUpdate()
{
	listener.onPresenceChange(presentities);
}



@Override
public void rePublish() {
	
	if (published) publish(currentPresenceStatus, currentPresenceNote, publishExpireTime, true);
	
}
}
