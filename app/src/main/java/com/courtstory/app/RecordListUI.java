package com.courtstory.app;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Search covers the complete history; only one bounded page of cards is rendered. */
final class RecordListUI {
    static final int PAGE_SIZE=50;
    static final class Row {
        final JSONObject record;final String title,details,group,search;
        Row(String table,JSONObject record,boolean activity){this.record=record;title=title(table,record);details=activity?summary(table,record):record.optString("organisation",record.optString("town",record.optString("club","Tap to view and edit")));group=activity?status(table,record):"";search=(title+" "+details).toLowerCase(Locale.ROOT);}
    }
    static void show(MainActivity a,String table,String query,int offset){show(a,table,query,offset,null);}
    static void forPlayer(MainActivity a,JSONObject person,String table){show(a,table,"",0,person);}
    static void show(MainActivity a,String table,String query,int offset,JSONObject subject){
        a.page(subject==null?a.plural(table):playerName(subject),subject==null?"Your records, ready when you need them.":ProfileSetup.sport(a.store.player())+" • "+a.plural(table));a.screenTable="list:"+table;a.listQuery=query;a.listOffset=offset;a.listSubjectID=subject==null?null:subject.optString("id");
        a.button("Add "+(table.equals("coaches")?"coach":table.equals("matches")?"match":table.equals("trainingSessions")?"training session":table.equals("tournamentTemplates")?"template":table.substring(0,table.length()-1)),()->a.edit(table,subject==null?null:CoachWorkspace.record(a,subject,table),subject!=null));
        EditText search=new EditText(a);search.setId(View.generateViewId());search.setSingleLine(true);search.setTextSize(16);search.setTextColor(a.ink);search.setHintTextColor(a.muted);search.setHint("Search "+a.plural(table).toLowerCase(Locale.ROOT));TextView caption=a.text(search.getHint().toString(),15,true);caption.setLabelFor(search.getId());a.body.addView(caption);search.setMinHeight(a.dp(52));search.setMinimumHeight(a.dp(52));search.setText(query);a.body.addView(search,a.lp());
        TextView resultCount=a.text("",14,false);resultCount.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);a.body.addView(resultCount,a.lp());LinearLayout results=a.column();a.body.addView(results);boolean activity=Arrays.asList("matches","trainingSessions","tournaments").contains(table);
        List<JSONObject> records;
        if(subject==null)records=activity?a.mine(table):rows(a.store.table(table));else{records=new ArrayList<>();for(JSONObject record:rows(a.store.table(table)))if(CoachInsights.includes(record,subject.optString("id"))&&record.optString("sport","Tennis").equals(ProfileSetup.sport(a.store.player())))records.add(record);records.sort(Comparator.comparingLong(record->millis(record,"date")));}
        if(activity)records.sort(RecordChronology::compare);List<Row> ordered=new ArrayList<>();
        for(String group:activity?new String[]{"In progress","Scheduled","Entered","Completed","Withdrawn"}:new String[]{""}){
            List<JSONObject> sorted=new ArrayList<>(records);if(group.equals("Completed"))Collections.reverse(sorted);
            for(JSONObject record:sorted)if(!activity||status(table,record).equals(group))ordered.add(new Row(table,record,activity));
        }
        Runnable[] render={null};render[0]=()->{
            results.removeAllViews();LinearLayout previous=a.body;a.body=results;List<Row> matched=new ArrayList<>();String term=search.getText().toString().trim().toLowerCase(Locale.ROOT);
            for(Row row:ordered)if(row.search.contains(term))matched.add(row);
            int count=matched.size();a.listQuery=search.getText().toString();a.listOffset=count==0?0:Math.min(Math.max(0,a.listOffset),((count-1)/PAGE_SIZE)*PAGE_SIZE);
            String savedQuery=a.listQuery;int savedOffset=a.listOffset;a.current=()->show(a,table,savedQuery,savedOffset,subject);
            if(count==0)resultCount.setText(ordered.isEmpty()?"No records yet. Add your first one above.":"No matching records. Try a different search.");
            else{
                int end=Math.min(count,a.listOffset+PAGE_SIZE);resultCount.setText("Showing "+(a.listOffset+1)+"–"+end+" of "+count+" records");String lastGroup=null;
                for(int i=a.listOffset;i<end;i++){Row row=matched.get(i);if(!row.group.isEmpty()&&!row.group.equals(lastGroup)){a.heading(row.group);lastGroup=row.group;}a.card(row.title,row.details,()->a.go(()->a.detail(table,row.record)));}
                if(a.listOffset>0)a.button("Previous "+PAGE_SIZE+" records",()->{a.listOffset-=PAGE_SIZE;render[0].run();a.scroll.smoothScrollTo(0,0);});
                if(end<count)a.button("Next "+Math.min(PAGE_SIZE,count-end)+" records",()->{a.listOffset=end;render[0].run();a.scroll.smoothScrollTo(0,0);});
            }
            a.body=previous;
        };
        render[0].run();search.addTextChangedListener(a.watcher(text->{a.listOffset=0;render[0].run();}));if(subject!=null)a.button("Back to player overview",()->CoachInsights.player(a,subject,30));a.navigation("Track");
    }
}
