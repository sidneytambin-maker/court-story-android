package com.courtstory.app;

import android.app.AlertDialog;

import android.content.*;

import android.widget.*;

import org.json.*;

import java.util.*;

import static com.courtstory.app.Domain.*;



/** Native phone screens; no web view and no changes to local tracking storage. */

final class ConnectUI {

    interface Action { void run() throws Exception; }

    static void run(MainActivity a,Action action,Runnable done){ConnectClient c=ConnectClient.get(a);a.announce("Connecting");c.worker.execute(()->{try{action.run();a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())done.run();});}catch(Exception e){a.runOnUiThread(()->{c.status=e.getMessage();if(!a.isFinishing())a.error(c.status);});}});}

    static String summary(JSONObject body){if(body==null)return "No sporting fields";List<String> keys=new ArrayList<>();body.keys().forEachRemaining(keys::add);Collections.sort(keys);List<String> text=new ArrayList<>();for(String key:keys)text.add(key.replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' ')+": "+body.opt(key));return String.join(". ",text);}
    static CheckBox checkbox(MainActivity a){CheckBox box=new CheckBox(a);box.setMinHeight(a.dp(48));box.setMinimumHeight(a.dp(48));box.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(8));return box;}
    static EditText field(MainActivity a,String label,String initial){TextView caption=a.text(label,16,false);a.body.addView(caption,a.lp());EditText e=new EditText(a);e.setId(android.view.View.generateViewId());caption.setLabelFor(e.getId());e.setText(initial);e.setHint(label);e.setMinHeight(a.dp(48));e.setMinimumHeight(a.dp(48));a.body.addView(e,a.lp());return e;}

    static void show(MainActivity a){show(a,"");}
    static void show(MainActivity a,String playerSearch){ConnectClient c=ConnectClient.get(a);a.current=()->show(a,playerSearch);a.page("Court Story Connect","Optional sharing with players and coaches");a.note(c.status);a.note("Signing in never uploads your library. Health data and media are excluded. Keep personal information out of notes you choose to share.");

        if(!c.signedIn()){EditText email=field(a,"Email address","");email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);a.button("Send sign-in code",()->{String value=email.getText().toString();run(a,()->c.sendCode(value),()->a.announce(c.status));});EditText code=field(a,"Email sign-in code","");code.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);a.button("Sign in",()->{String mail=email.getText().toString(),token=code.getText().toString();run(a,()->c.verify(mail,token),()->show(a));});a.button("Back to local tracking",a::settings);return;}

        a.heading("Invitations");a.button("Invite a coach",()->invite(a,"player"));a.button("Request a player connection",()->invite(a,"coach"));

        EditText incoming=field(a,"Invitation link or code",a.getSharedPreferences("connect",0).getString("pending",""));a.button("Review invitation",()->{String value=incoming.getText().toString();final JSONObject[] preview={null};run(a,()->preview[0]=c.preview(value),()->new AlertDialog.Builder(a).setTitle("Connection invitation").setMessage(preview[0].optString("name")+" · "+preview[0].optString("sport")+". Player approval is required before sharing. Your selected profile: "+playerName(a.store.player())).setPositiveButton("Accept",(d,w)->run(a,()->c.accept(value,a.store.player()),()->show(a))).setNeutralButton("Decline",(d,w)->run(a,()->c.decline(value),()->show(a))).setNegativeButton("Cancel",null).show());});

        a.heading("Connections and approval");for(JSONObject connection:rows(c.connections))a.button(connection.optString("sport")+" · "+connection.optString("state")+" · "+connection.optString(connection.optString("player_account").equalsIgnoreCase(c.account())?"coach_name":"player_name","Court Story member"),()->permission(a,connection));a.button("Refresh connections",()->run(a,c::refresh,()->show(a)));

        a.heading("Upload consent on this device");a.note("Share only your own selected profile: "+playerName(a.store.player())+". Only approved categories and history for active connections are eligible. Local records are unchanged.");

        a.button("Manage private records and notes",()->privacy(a));
        CheckBox consent=checkbox(a);consent.setText("I approve uploading permitted sporting records");a.body.addView(consent,a.lp());a.button("Enable sharing and synchronise",()->{if(!consent.isChecked()||a.store.player()==null){a.error("Select the consent checkbox and your own profile first.");return;}JSONObject player=copy(a.store.player()),library=copy(a.store.data);run(a,()->c.enable(player,library),()->show(a));});a.button("Pause uploads on this device",()->run(a,c::pause,()->show(a)));a.button("Retry synchronisation",()->{JSONObject library=copy(a.store.data);run(a,()->c.sync(library),()->show(a));});

        for(JSONObject conflict:c.syncConflicts()){String rid=conflict.optString("p_id");JSONObject remote=find(c.records,rid);a.heading("Review changed record");a.note("Queued device version: "+summary(conflict.optJSONObject("p_body")));a.note("Current online version: "+(remote==null?"Refresh required":summary(remote.optJSONObject("body"))));a.button("Keep online version; leave local record unchanged",()->run(a,()->c.resolveConflict(rid,false),()->show(a)));a.button("Use reviewed device version",()->a.confirm("Use this device version?","The reviewed queued sporting fields will replace the online version after another version check. Your local record stays unchanged.",()->run(a,()->c.resolveConflict(rid,true),()->show(a))));}
        if(c.pendingFeedback().length()>0){a.heading("Outgoing feedback");for(JSONObject pending:rows(c.pendingFeedback())){a.note(pending.optString("p_text"));a.button("Remove queued feedback",()->run(a,()->c.discardFeedback(pending.optString("p_id")),()->show(a)));}a.note("Reconnect and use Retry synchronisation. If sharing was revoked, remove feedback that can no longer be sent.");}
        a.heading("Players and shared records");Map<String,String> groups=new TreeMap<>();for(JSONObject connection:rows(c.connections))if(connection.optString("state").equals("active"))groups.put(connection.optString("player_id"),connection.optString("player_name","Player"));EditText search=field(a,"Find a player",playerSearch);a.button("Search players",()->show(a,search.getText().toString().trim()));List<Map.Entry<String,String>> sortedPlayers=new ArrayList<>(groups.entrySet());sortedPlayers.sort(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER));for(Map.Entry<String,String> group:sortedPlayers)if(playerSearch.isEmpty()||group.getValue().toLowerCase(Locale.ROOT).contains(playerSearch.toLowerCase(Locale.ROOT)))a.button(group.getValue(),()->playerDashboard(a,group.getKey(),group.getValue(),"All approved sports","All categories"));
        a.button("Notification settings and sharing confirmations",()->notifications(a));

        a.button("Delete Connect account and cloud data",()->a.confirm("Delete your Connect account?","This removes online connections, shared records and feedback. Your local sporting library stays on this device. Copies saved by coaches cannot necessarily be erased.",()->run(a,c::deleteAccount,()->show(a))));
        a.button("Sign out",()->run(a,c::signOut,()->show(a)));a.button("Back to local tracking",a::settings);

    }

    static void playerDashboard(MainActivity a,String player,String name,String sport,String category){ConnectClient c=ConnectClient.get(a);a.page(name,"Shared records for this player only");a.note("This player controls sharing. Each player's records remain separate.");JSONObject linked=find(a.store.table("players"),c.linkedPlayer(player));if(linked!=null)a.note("Linked local profile: "+playerName(linked));a.button("Link an existing local player profile",()->{List<JSONObject> local=rows(a.store.table("players"));String[] names=new String[local.size()+1];names[0]="No local link";for(int n=0;n<local.size();n++)names[n+1]=playerName(local.get(n));new AlertDialog.Builder(a).setTitle("Choose local player explicitly").setItems(names,(d,w)->a.confirm("Confirm player link","Link "+name+" to "+names[w]+"? Neither history will be merged, overwritten or uploaded.",()->run(a,()->c.linkPlayer(player,w==0?"":local.get(w-1).optString("id")),()->playerDashboard(a,player,name,sport,category)))).setNegativeButton("Cancel",null).show();});Set<String> sports=new TreeSet<>();for(JSONObject r:rows(c.records))if(r.optString("player_id").equalsIgnoreCase(player))sports.add(r.optString("sport"));List<String> options=new ArrayList<>();options.add("All approved sports");options.addAll(sports);a.button("Sport: "+sport,()->new AlertDialog.Builder(a).setTitle("Filter sport").setItems(options.toArray(new String[0]),(d,w)->playerDashboard(a,player,name,options.get(w),category)).setNegativeButton("Cancel",null).show());List<String> categories=new ArrayList<>();categories.add("All categories");categories.addAll(Arrays.asList(ConnectProtocol.CATEGORIES));a.button("Category: "+category,()->new AlertDialog.Builder(a).setTitle("Filter category").setItems(categories.toArray(new String[0]),(d,w)->playerDashboard(a,player,name,sport,categories.get(w))).setNegativeButton("Cancel",null).show());List<JSONObject> orderedRecords=new ArrayList<>(rows(c.records));orderedRecords.sort((left,right)->Long.compare(millis(right,"occurred_at"),millis(left,"occurred_at")));for(JSONObject r:orderedRecords)if(r.optString("player_id").equalsIgnoreCase(player)&&(sport.equals("All approved sports")||sport.equals(r.optString("sport")))&&(category.equals("All categories")||category.equals(r.optString("category")))){JSONObject b=object(r,"body");a.button(b.optString("title",b.optString("text","Sporting record"))+" - "+r.optString("sport")+" - "+r.optString("occurred_at"),()->record(a,r));}a.button("Back to players",()->show(a));}
    static void privacy(MainActivity a){ConnectClient c=ConnectClient.get(a);a.page("Private records and notes","Private records are hidden from every coach");a.note("Mark records private before enabling uploads. Changing previously shared records needs an internet connection to remove server access.");JSONObject player=a.store.player();if(player!=null)for(JSONObject r:ConnectProtocol.project(a.store.data,Collections.singleton(player.optString("id").toLowerCase(Locale.ROOT)))){String id=r.optString("p_id");JSONObject b=object(r,"p_body");CheckBox box=checkbox(a);box.setText("Keep private: "+r.optString("p_category")+" - "+b.optString("title",b.optString("text","Sporting record")));box.setChecked(c.isPrivate(id));box.setOnCheckedChangeListener((button,value)->{JSONObject library=copy(a.store.data);run(a,()->c.setPrivate(id,value,library),()->privacy(a));});a.body.addView(box,a.lp());}a.button("Back",()->show(a));}
    static void notifications(MainActivity a){ConnectClient c=ConnectClient.get(a);a.page("Connect notifications","Manage alerts and review sharing confirmations");

        String[] keys={"push_enabled","connection_requests","sharing_confirmations","coach_feedback"},labels={"Push notifications","Connection requests","Sharing confirmations","Coach feedback"};List<CheckBox> boxes=new ArrayList<>();

        for(int i=0;i<keys.length;i++){CheckBox b=checkbox(a);b.setText(labels[i]);b.setChecked(c.notificationSettings.optBoolean(keys[i],i>0));a.body.addView(b,a.lp());boxes.add(b);}

        a.note("A sharing confirmation names your coach. You can turn alerts off at any time; sharing permissions are managed separately. Phone settings can also silence alerts.");

        a.button("Save notification settings",()->{boolean push=boxes.get(0).isChecked();if(push&&android.os.Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},6103);run(a,()->c.saveNotifications(push,boxes.get(1).isChecked(),boxes.get(2).isChecked(),boxes.get(3).isChecked()),()->notifications(a));});

        a.button("Open phone notification settings",()->a.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,a.getPackageName())));

        a.heading("Sharing confirmations and updates");for(JSONObject n:rows(c.notifications))a.note(n.optString("message")+" · "+n.optString("created_at"));

        a.button("Back",()->show(a));

    }

    static void invite(MainActivity a,String role){ConnectClient c=ConnectClient.get(a);JSONObject player=a.store.player();if(player==null){a.error("Choose your own profile first.");return;}a.current=()->invite(a,role);a.page(role.equals("player")?"Invite a coach":"Request a player connection","Player approval is required");String sport=ProfileSetup.sport(player);if(!Arrays.asList(ProfileSetup.SPORTS).contains(sport))sport="custom:"+sport.trim().toLowerCase(Locale.ROOT);final String sportKey=sport;a.note("Sport: "+sport);EditText recipient=field(a,"Recipient email (optional)","");a.button("Create invitation",()->{String mail=recipient.getText().toString();final String[] link={""};run(a,()->link[0]=c.invite(role,player,sportKey,mail),()->{a.page("Invitation ready","Expires after 48 hours");a.note(link[0]);a.button("Share invitation",()->a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,link[0]),"Share Court Story invitation")));a.button("Copy invitation",()->{((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Court Story invitation",link[0]));a.announce("Invitation copied");});a.button("Done",()->show(a));});});a.button("Cancel",()->show(a));}

    static void permission(MainActivity a,JSONObject connection){ConnectClient c=ConnectClient.get(a);a.page("Sharing permission",connection.optString("sport"));if(!connection.optString("player_account").equalsIgnoreCase(c.account())){a.note("The player controls sharing. The invitation alone gives no data access.");a.button("Leave connection",()->{JSONObject p=obj();put(p,"p_id",connection.optString("id"));run(a,()->{c.rpc("cs_leave_connection",p);c.refresh();},()->show(a));});a.button("Back",()->show(a));return;}

        Set<String> selected=new HashSet<>();JSONArray cats=connection.optJSONArray("categories");if(connection.isNull("approved_at"))selected.addAll(Arrays.asList("match","training","tournament"));else if(cats!=null)for(int i=0;i<cats.length();i++)selected.add(cats.optString(i));List<CheckBox> boxes=new ArrayList<>();for(String category:ConnectProtocol.CATEGORIES){CheckBox box=checkbox(a);box.setText(category);box.setChecked(selected.contains(category));box.setOnCheckedChangeListener((b,checked)->{if(checked)selected.add(category);else selected.remove(category);});boxes.add(box);a.body.addView(box,a.lp());}a.note("Health, media and private notes are always excluded.");

        final String[] history={connection.optString("history","future")},capability={connection.optString("capability","view")};Button scope=a.button("History: "+history[0],()->{});scope.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("History scope").setSingleChoiceItems(new String[]{"Future only","All history and future","Selected period"},Arrays.asList("future","all","period").indexOf(history[0]),(d,w)->{history[0]=new String[]{"future","all","period"}[w];scope.setText("History: "+history[0]);d.dismiss();}).setNegativeButton("Cancel",null).show());

        EditText start=field(a,"Selected period from (YYYY-MM-DD)",connection.optString("history_start","2026-01-01").split("T")[0]),end=field(a,"Selected period through (YYYY-MM-DD)",connection.optString("history_end",now()).split("T")[0]);CheckBox future=checkbox(a);future.setText("Automatically share future records");future.setChecked(connection.optBoolean("future_sharing",true));a.body.addView(future,a.lp());Button access=a.button("Coach access: "+capability[0],()->{});access.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Coach access").setSingleChoiceItems(new String[]{"View only","Coach feedback","Suggest contributions for approval"},Arrays.asList("view","feedback","contribute").indexOf(capability[0]),(d,w)->{capability[0]=new String[]{"view","feedback","contribute"}[w];access.setText("Coach access: "+capability[0]);d.dismiss();}).setNegativeButton("Cancel",null).show());

        a.button("Share all sporting data, including history",()->{for(CheckBox box:boxes)box.setChecked(true);history[0]="all";scope.setText("History: all history and future");future.setChecked(true);a.announce("All sporting categories, all history and automatic future sharing selected. Health, media and private records remain excluded. Press Approve and save sharing to confirm.");});
        java.util.function.Consumer<String> save=state->{JSONObject p=obj();put(p,"p_id",connection.optString("id"));put(p,"p_state",state);put(p,"p_categories",new JSONArray(selected));put(p,"p_history",history[0]);put(p,"p_start",history[0].equals("period")?start.getText()+"T00:00:00Z":JSONObject.NULL);put(p,"p_end",history[0].equals("period")?end.getText()+"T23:59:59Z":JSONObject.NULL);put(p,"p_future",future.isChecked());put(p,"p_capability",capability[0]);run(a,()->{c.rpc("cs_set_permission",p);c.refresh();},()->show(a));};

        a.button("Approve and save sharing",()->save.accept("active"));a.button("Keep private / pause connection",()->save.accept("paused"));a.button("Revoke access",()->save.accept("revoked"));a.note("Revocation stops server access immediately. Copies already saved by a coach cannot necessarily be erased remotely.");a.button("Cancel",()->show(a));

    }

    static void record(MainActivity a,JSONObject requestedRecord){ConnectClient c=ConnectClient.get(a);JSONObject record=find(c.records,requestedRecord.optString("id"));if(record==null){a.page("Record unavailable","Sharing permission may have changed");a.button("Back to players",()->show(a));return;}a.page("Shared sporting record",record.optString("sport"));JSONObject body=object(record,"body");Iterator<String> keys=body.keys();while(keys.hasNext()){String key=keys.next();a.note(key.replaceAll("([a-z])([A-Z])","$1 $2").replace("_"," ")+": "+body.optString(key));}a.heading("Participants and teams");for(JSONObject person:rows(c.participants))if(person.optString("record_id").equals(record.optString("id")))a.note(person.optString("display_name")+" - Team "+(person.optInt("team")+1));a.button("Refresh record and access",()->run(a,c::refresh,()->record(a,record)));a.heading("Coach feedback");for(JSONObject f:rows(c.feedback))if(f.optString("record_id").equals(record.optString("id"))){a.note(f.optString("text")+" · "+f.optString("state"));if(record.optString("owner_id").equalsIgnoreCase(c.account()))for(boolean approve:new boolean[]{true,false})a.button(approve?"Approve suggestion":"Reject suggestion",()->{JSONObject p=obj();put(p,"p_id",f.optString("id"));put(p,"p_approve",approve);run(a,()->{c.rpc("cs_review_feedback",p);c.refresh();},()->record(a,record));});}

        for(JSONObject connection:rows(c.connections))if(connection.optString("coach_account").equalsIgnoreCase(c.account())&&connection.optString("player_id").equals(record.optString("player_id"))&&connection.optString("sport").equals(record.optString("sport"))&&connection.optString("state").equals("active")&&!connection.optString("capability").equals("view")){

            EditText text=field(a,"Feedback or suggestion","");text.setMinLines(3);final String[] kind={"feedback"};String[] kinds=connection.optString("capability").equals("contribute")?new String[]{"feedback","goal","contribution"}:new String[]{"feedback","goal"};Button type=a.button("Contribution: feedback",()->{});type.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Contribution type").setSingleChoiceItems(kinds,0,(d,w)->{kind[0]=kinds[w];type.setText("Contribution: "+kind[0]);d.dismiss();}).setNegativeButton("Cancel",null).show());a.button("Send to player",()->{JSONObject p=obj();put(p,"p_id",id());put(p,"p_connection",connection.optString("id"));put(p,"p_record",record.optString("id"));put(p,"p_kind",kind[0]);put(p,"p_text",text.getText().toString());run(a,()->{c.queueFeedback(p);},()->record(a,record));});}

        a.note("Suggestions never automatically change official scores. The player reviews and edits their own record.");a.button("Back",()->show(a));

    }

}

