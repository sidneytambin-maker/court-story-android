package com.courtstory.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public final class ConnectInvitationConfirmationTest {
 @Test public void namesSportsAndPermissionsAreDynamic()throws Exception{
  JSONObject row=new JSONObject("{\"player_name\":\"Morgan Lee\",\"coach_name\":\"Alex\",\"sport\":\"Badminton\",\"state\":\"active\",\"categories\":[\"match\",\"training\"],\"history\":\"all\",\"future_sharing\":true,\"capability\":\"feedback\"}");
  ConnectInvitationConfirmation c=new ConnectInvitationConfirmation("accept",row,new JSONObject(),false,"Other player");
  assertEquals("You agreed to receive Morgan Lee’s Badminton data.",c.message);assertTrue(c.canView);assertFalse(c.canManage);
  assertTrue(c.details.contains("matches, training sessions"));assertTrue(c.details.contains("All approved historical records"));assertTrue(c.details.contains("You can add coaching feedback"));
  row.put("capability","view");assertTrue(new ConnectInvitationConfirmation("accept",row,new JSONObject(),false,"").details.contains("view only"));
  for(String state:new String[]{"pending","paused","revoked"}){row.put("state",state);ConnectInvitationConfirmation unavailable=new ConnectInvitationConfirmation("accept",row,new JSONObject(),false,"");assertFalse(unavailable.canView);assertFalse(unavailable.details.contains("You can add coaching feedback to shared records"));}
 }
 @Test public void declinedInvitationNamesSenderAndNeverOffersAccess()throws Exception{
  JSONObject preview=new JSONObject("{\"name\":\"Taylor\",\"sport\":\"Padel\"}");
  ConnectInvitationConfirmation c=new ConnectInvitationConfirmation("decline",new JSONObject(),preview,false,"");
  assertEquals("You declined the invitation from Taylor for Padel.",c.message);assertFalse(c.canView);assertFalse(c.canManage);
 }
}
