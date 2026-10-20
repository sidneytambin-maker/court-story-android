package com.courtstory.app;
import org.json.*;
import java.util.*;
final class ConnectInvitationConfirmation {
 final String title,message,details,playerName;final boolean canView,canManage;
 ConnectInvitationConfirmation(String response,JSONObject row,JSONObject preview,boolean owner,String localPlayer){
  String sender=preview.optString("name","Court Story member");
  playerName=row.optString("player_name",owner?localPlayer:sender);
  String coach=row.optString("coach_name",sender),sport=row.optString("sport",preview.optString("sport","sporting")),state=row.optString("state","pending");
  JSONArray categories=row.optJSONArray("categories");
  canView=response.equals("accept")&&state.equals("active")&&categories!=null&&categories.length()>0;
  canManage=response.equals("accept")&&owner&&!state.equals("revoked");
  if(response.equals("decline")){title="Invitation declined";message="You declined the invitation from "+sender+" for "+sport+".";details="This invitation did not enable any data sharing.";return;}
  title="Invitation accepted";
  message=owner?"You accepted "+coach+" as a coach for "+playerName+"’s "+sport+" record.":"You agreed to receive "+playerName+"’s "+sport+" data.";
  if(canView){
   Map<String,String> labels=Map.of("match","matches","training","training sessions","tournament","tournaments","note","sporting notes","reflection","reflections","goal","goals");
   List<String> included=new ArrayList<>();for(int n=0;n<categories.length();n++)if(labels.containsKey(categories.optString(n)))included.add(labels.get(categories.optString(n)));
   String history=row.optString("history","future");
   String historyText=history.equals("all")?"All approved historical records are included.":history.equals("period")?"Only the player’s approved date range is included.":"Records from the date sharing was approved are included.";
   String future=row.optBoolean("future_sharing")?"New permitted records will update automatically.":"Future records are not included.";
   String feedback=Arrays.asList("feedback","contribute").contains(row.optString("capability"))?(owner?"Your coach can add coaching feedback.":"You can add coaching feedback to shared records."):"Access is view only; coaching feedback is not enabled.";
   details="Shared: "+String.join(", ",included)+". "+historyText+" "+future+" "+feedback+" Health data, media and records marked private are excluded.";
  }else if(state.equals("active"))details="No sporting categories are selected, so no records are available. The player can change sharing choices at any time.";
  else if(state.equals("revoked")||state.equals("paused"))details="Sharing is currently "+state+". This acceptance does not restore access.";
  else details=owner?"Choose what to share next. No records are available to your coach yet. You can include history and enable coaching feedback.":"Waiting for "+playerName+" to approve what is shared. No records are available yet. Coaching feedback will be available if the player enables it.";
 }
}
