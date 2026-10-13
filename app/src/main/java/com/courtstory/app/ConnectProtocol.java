package com.courtstory.app;

import org.json.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Version 1 wire contract. Never serialize the local library into an API request. */
final class ConnectProtocol {
    static final String[] CATEGORIES={"match","training","tournament","note","reflection","goal"};
    static String code(String input,String host){
        String value=input.trim();
        if(value.contains(":")){try{URI u=new URI(value);if(!"https".equals(u.getScheme())||!host.equals(u.getHost())||!"/connect/invite".equals(u.getPath())||u.getUserInfo()!=null||u.getPort()!=-1||u.getQuery()!=null)return null;value=u.getFragment();}catch(Exception e){return null;}}
        return value!=null&&value.matches("[a-f0-9]{64}")?value:null;
    }
    static String fingerprint(JSONObject value){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    static String noteID(String id){return derivedID(id,"note");}
    static String derivedID(String id,String category){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(("court-story-"+category+":"+id.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));b[6]=(byte)((b[6]&15)|80);b[8]=(byte)((b[8]&63)|128);java.nio.ByteBuffer v=java.nio.ByteBuffer.wrap(b);return new UUID(v.getLong(),v.getLong()).toString();}catch(Exception e){throw new IllegalStateException(e);}}
    static String sport(JSONObject row){JSONObject c=row.optJSONObject("court"),s=c==null?null:c.optJSONObject("sport");String name=s==null?row.optString("sport","Tennis"):s.optString("sport","Tennis");return name.equals("Custom")?"custom:"+(s==null?row.optString("customSport"):s.optString("customName")).trim().toLowerCase(Locale.ROOT):name;}
    static List<JSONObject> project(JSONObject library,Set<String> players){
        List<JSONObject> out=new ArrayList<>();
        String[][] tables={{"matches","match"},{"trainingSessions","training"},{"tournaments","tournament"}};
        for(String[] table:tables)for(JSONObject r:rows(Domain.table(library,table[0]))){
            String player=r.optString("playerID").toLowerCase(Locale.ROOT);if(!players.contains(player))continue;
            JSONObject body=obj();put(body,"title",r.optString("name",r.optString("trainingType","Match")));
            if(table[1].equals("match")){put(body,"score",r.optString("setScores"));put(body,"result",r.optString("result"));}
            if(table[1].equals("training")){put(body,"focus",r.optString("focus"));put(body,"duration_minutes",r.optInt("durationMinutes"));}
            for(String key:new String[]{"status","matchType","matchFormat","yourSetsWon","opponentSetsWon","aces","doubleFaults","winners","unforcedErrors","hadTiebreak","tiebreakScore","tournamentID","trainingSessionID","finalResult","matchesPlayed","format","stageReached","finishingPosition","endDate"}){Object value=r.opt(key);if(value instanceof String || value instanceof Number || value instanceof Boolean)put(body,key,value);}
            if(!r.optString("customScoreSummary").isEmpty())put(body,"customScoreSummary",r.optString("customScoreSummary"));
            JSONObject court=object(r,"court"),plan=object(court,"plan");
            for(String field:new String[]{"surface","sessionType"})if(!court.optString(field).isEmpty())put(body,field,court.optString(field));
            for(String[] field:new String[][]{{"objective","planObjective"},{"drills","planDrills"},{"equipment","planEquipment"},{"successMeasure","planSuccessMeasure"}})if(!plan.optString(field[0]).isEmpty())put(body,field[1],plan.optString(field[0]));
            JSONObject wire=obj();put(wire,"p_id",r.optString("id").toLowerCase(Locale.ROOT));put(wire,"p_player",player);put(wire,"p_sport",sport(r));put(wire,"p_category",table[1]);put(wire,"p_occurred",Instant.ofEpochMilli(millis(r,"date")).toString());put(wire,"p_body",body);put(wire,"p_private",false);put(wire,"p_deleted",false);if(table[1].equals("match")){JSONArray people=new JSONArray();String[] keys={"playerName","partnerName","opponentName","opponent2Name"};for(int n=0;n<keys.length;n++)if(!r.optString(keys[n]).isEmpty()){JSONObject person=obj();put(person,"id",derivedID(r.optString("id"),keys[n]));put(person,"display_name",r.optString(keys[n]));put(person,"team",n<2?0:1);people.put(person);}put(wire,"p_people",people);}out.add(wire);
            String[] reflectionKeys=table[1].equals("match")?new String[]{"matchStory","matchStrengths","matchNeedsWork","pressureMoment"}:new String[]{"sessionOutcome"};
            for(String category:new String[]{"reflection","goal"}){List<String> parts=new ArrayList<>();for(String key:category.equals("reflection")?reflectionKeys:table[1].equals("tournament")?new String[]{"goal"}:new String[]{"nextPracticeFocus"})if(!r.optString(key).isEmpty())parts.add(r.optString(key));if(category.equals("reflection")&&!plan.optString("review").isEmpty())parts.add(plan.optString("review"));if(!parts.isEmpty()){JSONObject derived=copy(wire),text=obj();put(derived,"p_people",new JSONArray());put(text,"text",String.join("\n",parts));put(derived,"p_id",derivedID(r.optString("id"),category));put(derived,"p_category",category);put(derived,"p_body",text);out.add(derived);}}
            if(!r.optString("notes").isEmpty()){JSONObject note=copy(wire),text=obj();put(note,"p_people",new JSONArray());put(text,"text",r.optString("notes"));put(note,"p_id",noteID(r.optString("id")));put(note,"p_category","note");put(note,"p_body",text);out.add(note);}
        }
        return out;
    }
    static boolean permits(JSONObject c,JSONObject record,String account){
        if(!c.optString("player_account").equalsIgnoreCase(account)||!c.optString("player_id").equalsIgnoreCase(record.optString("p_player"))||!c.optString("sport").equals(record.optString("p_sport"))||!c.optString("state").equals("active"))return false;
        boolean category=false;JSONArray cats=c.optJSONArray("categories");if(cats!=null)for(int n=0;n<cats.length();n++)category|=cats.optString(n).equals(record.optString("p_category"));if(!category)return false;
        long when=millis(record,"p_occurred"),approved=millis(c,"approved_at");if(!c.optBoolean("future_sharing")&&when>approved)return false;
        return switch(c.optString("history")){case "all"->true;case "period"->when>=millis(c,"history_start")&&when<=millis(c,"history_end");default->when>=approved;};
    }
}
