package com.courtstory.app;
import android.content.Context;
import android.net.Uri;
import java.io.*;
/** Private pending export files survive activity/process recreation while a picker is open. */
final class ExportDraft {
    static File file(Context context,String name)throws IOException{if(name==null||!name.matches("court-export-[A-Fa-f0-9-]{36}\\.tmp"))throw new IOException("The pending report is no longer available. Please export the report again.");return new File(context.getFilesDir(),name);}
    static void stage(MainActivity a)throws IOException{discard(a);if(a.exportBytes==null)throw new IOException("There is no report to export.");String name="court-export-"+Domain.id()+".tmp";File target=file(a,name);try(FileOutputStream out=new FileOutputStream(target)){out.write(a.exportBytes);}catch(IOException error){target.delete();throw error;}a.pendingExportName=name;}
    static void discard(MainActivity a){if(a.pendingExportName!=null)try{file(a,a.pendingExportName).delete();}catch(IOException ignored){}a.pendingExportName=null;}
    static void writeTo(Context context,String name,OutputStream out)throws IOException{try(InputStream in=new FileInputStream(file(context,name))){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}}
    static void finish(MainActivity a,Uri uri){String name=a.pendingExportName;a.pendingExportName=null;a.exportBytes=null;Context context=a.getApplicationContext();new Thread(()->{String issue=null;try(OutputStream out=context.getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("The selected destination could not be opened.");writeTo(context,name,out);}catch(Exception e){issue=e.getMessage();}finally{try{file(context,name).delete();}catch(IOException ignored){}}final String error=issue;a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;if(error==null)a.announce("Export saved");else a.error("Export did not complete: "+error);});},"CourtStory-report-export").start();}
}

