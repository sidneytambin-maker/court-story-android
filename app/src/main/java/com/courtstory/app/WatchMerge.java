package com.courtstory.app;

import org.json.*;
import java.io.IOException;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Three-way merging never resolves concurrent edits by comparing device clocks. */
final class WatchMerge {
    static final String[] TABLES={"players","coaches","venues","locations","tournamentTemplates","matches","trainingSessions","tournaments"};
    static String canonical(Object value){if(value instanceof JSONObject){JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<>();o.keys().forEachRemaining(keys::add);Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String k:keys)b.append(JSONObject.quote(k)).append(':').append(canonical(o.opt(k))).append(',');return b.append('}').toString();}if(value instanceof JSONArray){StringBuilder b=new StringBuilder("[");for(int i=0;i<((JSONArray)value).length();i++)b.append(canonical(((JSONArray)value).opt(i))).append(',');return b.append(']').toString();}return value instanceof String?JSONObject.quote((String)value):value==null?"null":String.valueOf(value);}
    static boolean same(JSONObject a,JSONObject b){return canonical(a).equals(canonical(b));}
    static JSONObject merge(JSONObject local,JSONObject remote,JSONObject baseline) throws IOException {
        String issue=validate(remote,false);if(issue!=null)throw new IOException(issue);
        if(table(local,"players").length()==0)return copy(remote);
        if(!local.optString("libraryID").equals(remote.optString("libraryID")))throw new IOException("These devices contain different Court Story libraries. Back up both before connecting them; neither library was replaced.");
        JSONObject result=copy(local);JSONArray conflicts=array(result,"androidWatchConflicts");Set<String> deleted=new HashSet<>();for(JSONObject d:new JSONObject[]{local,remote})for(int i=0;i<array(d,"deletedRecordIDs").length();i++)deleted.add(array(d,"deletedRecordIDs").optString(i));
        for(String t:TABLES){Map<String,JSONObject> incoming=new LinkedHashMap<>();for(JSONObject r:rows(table(remote,t)))incoming.put(r.optString("id"),r);JSONArray target=new JSONArray();
            for(JSONObject own:rows(table(local,t))){String id=own.optString("id");JSONObject other=incoming.remove(id),base=baseline==null?null:find(table(baseline,t),id);
                if(deleted.contains(id)){if(base==null||!same(own,base)){conflict(conflicts,t,id,own,null);deleted.remove(id);target.put(copy(own));}continue;}
                if(other==null||same(own,other)||base!=null&&same(other,base)){target.put(copy(own));continue;}
                if(base!=null&&same(own,base)){target.put(copy(other));continue;}
                conflict(conflicts,t,id,own,other);target.put(copy(own));
            }
            for(JSONObject r:incoming.values()){String id=r.optString("id");if(!deleted.contains(id))target.put(copy(r));else{JSONObject base=baseline==null?null:find(table(baseline,t),id);if(base==null||!same(r,base))throw new IOException("A record deleted here was edited on the other device. Review both copies before synchronising. Neither library was changed.");}}
            if(Arrays.asList("coaches","venues","locations","tournamentTemplates").contains(t))put(object(result,"setup"),t,target);else put(result,t,target);
        }
        put(result,"deletedRecordIDs",new JSONArray(deleted));String validation=validate(result,false);if(validation!=null)throw new IOException("Sync needs review: "+validation+" Both original libraries are retained.");return result;
    }
    static void conflict(JSONArray conflicts,String table,String id,JSONObject local,JSONObject remote){for(JSONObject existing:rows(conflicts))if(existing.optString("table").equals(table)&&existing.optString("recordID").equals(id)&&same(existing.optJSONObject("incoming"),remote))return;JSONObject c=obj();put(c,"id",id());put(c,"table",table);put(c,"recordID",id);put(c,"local",copy(local));put(c,"incoming",remote==null?JSONObject.NULL:copy(remote));conflicts.put(c);}
}
