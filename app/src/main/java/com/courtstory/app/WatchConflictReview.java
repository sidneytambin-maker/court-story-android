package com.courtstory.app;

import org.json.*;
import java.util.*;

/** Human-readable differences keep sync decisions grounded in the actual edits. */
final class WatchConflictReview {
    static List<String> differences(JSONObject local,JSONObject incoming){
        List<String> result=new ArrayList<>();
        if(incoming==null){result.add("Record\nThis device: Kept\nIncoming: Deleted on the other device");return result;}
        compare("",local,incoming,result);
        return result;
    }
    private static void compare(String path,Object local,Object incoming,List<String> result){
        if(local instanceof JSONObject&&incoming instanceof JSONObject){
            JSONObject left=(JSONObject)local,right=(JSONObject)incoming;
            Set<String> keys=new TreeSet<>();left.keys().forEachRemaining(keys::add);right.keys().forEachRemaining(keys::add);
            for(String key:keys){if(key.equals("revision")||key.equals("modifiedAt"))continue;compare(path.isEmpty()?label(key):path+" · "+label(key),left.opt(key),right.opt(key),result);}
        }else if(!Objects.equals(canonical(local),canonical(incoming))){
            result.add(path+"\nThis device: "+value(local)+"\nIncoming: "+value(incoming));
        }
    }
    private static String canonical(Object value){return value==null||value==JSONObject.NULL?"null":value.getClass().getName()+":"+value.toString();}
    private static String label(String key){String text=key.replaceAll("([a-z])([A-Z])","$1 $2").replace("android ","");return text.isEmpty()?"Value":Character.toUpperCase(text.charAt(0))+text.substring(1);}
    private static String value(Object value){
        if(value==null||value==JSONObject.NULL||value.toString().isEmpty())return "Not set";
        if(value instanceof Boolean)return (Boolean)value?"Yes":"No";
        if(value instanceof JSONArray){List<String> parts=new ArrayList<>();JSONArray list=(JSONArray)value;for(int i=0;i<list.length();i++)parts.add(value(list.opt(i)));return parts.isEmpty()?"None":String.join("; ",parts);}
        if(value instanceof JSONObject){List<String> parts=new ArrayList<>();JSONObject object=(JSONObject)value;TreeSet<String> keys=new TreeSet<>();object.keys().forEachRemaining(keys::add);for(String key:keys)parts.add(label(key)+": "+value(object.opt(key)));return String.join("; ",parts);}
        return value.toString();
    }
}
