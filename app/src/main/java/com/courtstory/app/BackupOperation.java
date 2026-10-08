package com.courtstory.app;

import android.app.Application;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.ViewModelProvider;
import org.json.JSONObject;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import static com.courtstory.app.Domain.*;

/** Retains the operation, never the Activity, across rotation and file-copy completion. */
public final class BackupOperation extends AndroidViewModel {
    enum State { IDLE, EXPORTING, CHECKING, READY, RESTORING, EXPORTED, RESTORED, IMPORTING, MEDIA_READY, ERROR }
    State state=State.IDLE;
    String message;
    JSONObject importedMedia;
    String mediaOwner;
    BackupArchive.Staged staged;
    private final Handler main=new Handler(Looper.getMainLooper());
    private WeakReference<MainActivity> screen=new WeakReference<>(null);
    private Dialog dialog;
    private boolean cleared;
    public BackupOperation(Application app){super(app);}
    static BackupOperation get(MainActivity a){return new ViewModelProvider(a).get(BackupOperation.class);}
    static void attach(MainActivity a){
        BackupOperation task=get(a);task.screen=new WeakReference<>(a);
        a.getLifecycle().addObserver((LifecycleEventObserver)(owner,event)->{
            if(event==Lifecycle.Event.ON_START)task.render();
            if(event==Lifecycle.Event.ON_DESTROY&&task.screen.get()==a){task.dismiss();task.screen.clear();}
        });
    }
    void dismiss(){if(dialog!=null){dialog.dismiss();dialog=null;}}
    void change(State next){state=next;render();}
    void error(Exception e){message=e.getMessage()==null?"The operation could not finish. Please try again.":e.getMessage();change(State.ERROR);}
    void render(){
        MainActivity a=screen.get();if(a==null||a.isDestroyed()||!a.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))return;
        dismiss();
        if(state==State.EXPORTING||state==State.CHECKING||state==State.RESTORING||state==State.IMPORTING){
            String title=state==State.EXPORTING?"Saving full backup":state==State.CHECKING?"Checking backup":state==State.IMPORTING?"Adding media":"Restoring backup";
            dialog=ProgressDialog.show(a,title,state==State.CHECKING?"Validating records and media…":"Copying your records and media…",true,false);return;
        }
        if(state==State.READY){
            dialog=new AlertDialog.Builder(a).setTitle("Restore full backup?").setMessage(staged.library.optJSONArray("players").length()+" players. Photos, clips and coaching notes will be restored privately to this device.")
                .setNegativeButton("Cancel",(d,w)->cancel()).setOnCancelListener(d->cancel()).setPositiveButton("Restore",(d,w)->commit()).show();return;
        }
        if(state==State.MEDIA_READY){
            JSONObject person=find(a.store.table("players"),mediaOwner),media=importedMedia;importedMedia=null;state=State.IDLE;
            if(person==null){removeImported(media);a.error("The player is no longer available. Nothing was added.");return;}
            MediaLibrary.edit(a,person,media);return;
        }
        State result=state;state=State.IDLE;
        if(result==State.EXPORTED)a.announce("Full backup saved, including media");
        else if(result==State.RESTORED){try{a.store=new Store(a);a.palette();a.home();a.announce("Full library and media restored");}catch(IOException e){a.error(e.getMessage());}}
        else if(result==State.ERROR)a.error(message);
    }
    void cancel(){if(staged!=null){BackupArchive.discard(staged.directory);staged=null;}change(State.IDLE);}
    void export(Uri uri,JSONObject library){
        if(state!=State.IDLE)return;JSONObject snapshot=copy(library);change(State.EXPORTING);
        new Thread(()->{try{try(OutputStream out=getApplication().getContentResolver().openOutputStream(uri)){
            if(out==null)throw new IOException("Could not open the backup destination.");BackupArchive.write(getApplication(),snapshot,out);}
            main.post(()->{if(!cleared)change(State.EXPORTED);});
        }catch(Exception e){main.post(()->{if(!cleared)error(e);});}},"CourtStory-backup").start();
    }
    void restore(Uri uri){
        if(state!=State.IDLE)return;change(State.CHECKING);
        new Thread(()->{try(InputStream in=getApplication().getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException("Could not open the backup.");BackupArchive.Staged result=BackupArchive.readSupported(getApplication(),in);
            main.post(()->{if(cleared){BackupArchive.discard(result.directory);return;}staged=result;change(State.READY);});
        }catch(Exception e){main.post(()->{if(!cleared)error(e);});}},"CourtStory-check-backup").start();
    }
    void commit(){
        if(state!=State.READY||staged==null)return;BackupArchive.Staged input=staged;staged=null;change(State.RESTORING);
        new Thread(()->{try{
            Store target=new Store(getApplication());if(!ProfileHistory.empty(target.data))throw new IOException("Your library is no longer empty. Nothing was restored.");
            for(String name:BackupArchive.media(input.library))Files.copy(new File(input.directory,name).toPath(),MediaLibrary.file(getApplication(),name).toPath(),StandardCopyOption.REPLACE_EXISTING);
            target.restore(input.library);main.post(()->{if(!cleared)change(State.RESTORED);});
        }catch(Exception e){main.post(()->{if(!cleared)error(e);});}finally{BackupArchive.discard(input.directory);}},"CourtStory-restore").start();
    }
    void removeImported(JSONObject media){if(media!=null)try{MediaLibrary.file(getApplication(),media.optString("fileName")).delete();}catch(IOException ignored){}}
    void importMedia(Uri uri,String mime,String player,String sport){
        if(state!=State.IDLE)return;mediaOwner=player;change(State.IMPORTING);
        new Thread(()->{JSONObject media=obj();put(media,"id",id());put(media,"fileName",media.optString("id")+".bin");put(media,"mime",mime);put(media,"createdAt",now());put(media,"sport",sport);
            try{File target=MediaLibrary.file(getApplication(),media.optString("fileName"));
                try(InputStream in=getApplication().getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){
                    if(in==null)throw new IOException("Could not open this file.");BackupArchive.copyLimited(in,out,200_000_000L);
                }
                main.post(()->{if(cleared){removeImported(media);return;}importedMedia=media;change(State.MEDIA_READY);});
            }catch(Exception e){removeImported(media);main.post(()->{if(!cleared)error(e);});}
        },"CourtStory-media-import").start();
    }
    @Override protected void onCleared(){cleared=true;dismiss();removeImported(importedMedia);importedMedia=null;if(staged!=null)BackupArchive.discard(staged.directory);staged=null;screen.clear();}
}
