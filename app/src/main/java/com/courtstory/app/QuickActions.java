package com.courtstory.app;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import org.json.JSONObject;
import java.util.*;
import static com.courtstory.app.Domain.*;
/** Launcher actions use the selected profile's sport and normal editors. */
final class QuickActions {
    static void install(MainActivity a){try{ShortcutManager manager=a.getSystemService(ShortcutManager.class);if(manager==null)return;List<ShortcutInfo> list=new ArrayList<>();String[][] entries={{"score","Score a match"},{"match","Add a match"},{"training","Add training"},{"tournament","Add a tournament"}};for(String[] entry:entries)list.add(new ShortcutInfo.Builder(a,entry[0]).setShortLabel(entry[1]).setLongLabel(entry[1]).setIcon(Icon.createWithResource(a,R.drawable.app_icon)).setIntent(new Intent(a,MainActivity.class).setAction(Intent.ACTION_VIEW).putExtra("courtAction",entry[0])).build());manager.setDynamicShortcuts(list.subList(0,Math.min(list.size(),manager.getMaxShortcutCountPerActivity())));}catch(RuntimeException ignored){/* A launcher's shortcut limit must never stop the app opening. */}}
    static boolean open(MainActivity a,String action){if(action==null||!Arrays.asList("score","match","training","tournament","nextTournament","recentRecord").contains(action))return false;if(a.store.player()==null){a.welcome();return true;}switch(action){case "match":a.edit("matches",null,false);break;case "training":a.edit("trainingSessions",null,false);break;case "tournament":a.edit("tournaments",null,false);break;case "score":{List<JSONObject> active=new ArrayList<>();for(JSONObject r:a.mine("matches"))if(!status("matches",r).equals("Completed"))active.add(r);if(active.isEmpty())a.edit("matches",null,false);else{a.page("Choose a match","Live scoring · "+ProfileSetup.sport(a.store.player()));a.primary("New match",()->a.edit("matches",null,false));for(JSONObject r:active)a.card(title("matches",r),summary("matches",r),()->a.detail("matches",r));a.navigation("Track");}break;}default:{JSONObject selected=null;String selectedTable=null;long boundary=action.equals("nextTournament")?Long.MAX_VALUE:Long.MIN_VALUE;for(String table:action.equals("nextTournament")?new String[]{"tournaments"}:new String[]{"matches","trainingSessions","tournaments"})for(JSONObject r:a.mine(table)){boolean next=action.equals("nextTournament");long now=System.currentTimeMillis(),date=next?millis(r,"date"):CourtGlance.finished(table,r);if(next?!CourtGlance.upcoming(table,r,now):!status(table,r).equals("Completed")||active(r)||date<=0||date>now)continue;if(next?date<boundary:date>boundary){boundary=date;selected=r;selectedTable=table;}}if(selected==null){a.home();a.announce(action.equals("nextTournament")?"No upcoming tournaments":"No activity records yet");}else a.detail(selectedTable,selected);}}
        return true;
    }
}
