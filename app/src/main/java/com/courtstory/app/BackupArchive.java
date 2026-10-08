package com.courtstory.app;



import android.app.ProgressDialog;

import android.content.Context;

import android.net.Uri;

import org.json.*;

import java.io.*;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.util.*;

import java.util.zip.*;

import static com.courtstory.app.Domain.*;



/** Streaming full backups include private media; restore rejects unsafe or incomplete archives. */

final class BackupArchive {

    static long copyLimited(InputStream in,OutputStream out,long limit)throws IOException{byte[] buffer=new byte[65536];long total=0;int n;while((n=in.read(buffer))!=-1){total+=n;if(total>limit)throw new IOException("This file exceeds the supported size limit.");out.write(buffer,0,n);}return total;}

    static Set<String> media(JSONObject library)throws IOException{Set<String> names=new LinkedHashSet<>();for(JSONObject p:ProfileHistory.all(library))for(JSONObject m:rows(array(p,"androidMedia"))){String name=m.optString("fileName");if(!name.matches("[A-Fa-f0-9-]{36}\\.bin"))throw new IOException("The library has an invalid media reference.");names.add(name);}return names;}

    static void write(Context context,JSONObject library,OutputStream output)throws IOException{try(ZipOutputStream zip=new ZipOutputStream(output)){put(library,"androidBackupFormat",1);zip.putNextEntry(new ZipEntry("library.json"));zip.write(library.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();long total=0;for(String name:media(library)){File source=MediaLibrary.file(context,name);if(!source.isFile())throw new IOException("A media file is missing. The backup was not completed.");zip.putNextEntry(new ZipEntry("media/"+name));try(InputStream in=new FileInputStream(source)){total+=copyLimited(in,zip,200_000_000);}if(total>1_000_000_000L)throw new IOException("The media library exceeds the 1 GB backup limit.");zip.closeEntry();}}}

    static void export(MainActivity a,Uri uri){BackupOperation.get(a).export(uri,a.store.data);}

    static final class Staged {final JSONObject library;final File directory;Staged(JSONObject library,File directory){this.library=library;this.directory=directory;}}

    static Staged read(Context context,InputStream input)throws Exception{File stage=new File(context.getCacheDir(),"restore-"+id());if(!stage.mkdirs())throw new IOException("Could not prepare restore.");try{JSONObject library=null;long bytes=0;Set<String> entries=new HashSet<>();try(ZipInputStream zip=new ZipInputStream(input)){ZipEntry entry;while((entry=zip.getNextEntry())!=null){String name=entry.getName();if(!entries.add(name)||entry.isDirectory())throw new IOException("The archive has duplicate or invalid entries.");if(name.equals("library.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes+=copyLimited(zip,out,20_000_000);library=new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}else if(name.matches("media/[A-Fa-f0-9-]{36}\\.bin")){File target=new File(stage,name.substring(6));try(OutputStream out=new FileOutputStream(target)){bytes+=copyLimited(zip,out,200_000_000);}}else throw new IOException("Unexpected backup entry.");if(bytes>1_020_000_000L)throw new IOException("The backup exceeds the 1 GB media limit.");zip.closeEntry();}}if(library==null)throw new IOException("The archive has no library.");String issue=validate(library,library.optInt("androidBackupFormat")!=1);if(issue!=null)throw new IOException(issue);for(String name:media(library))if(!new File(stage,name).isFile())throw new IOException("The backup is missing a media file.");return new Staged(library,stage);}catch(Exception e){discard(stage);throw e;}}

    static Staged readSupported(Context context,InputStream input)throws Exception{
        BufferedInputStream source=new BufferedInputStream(input);source.mark(4);int first=source.read(),second=source.read();source.reset();
        if(first==80&&second==75)return read(context,source);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();copyLimited(source,bytes,20_000_000);
        JSONObject library=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));String issue=validate(library,true);if(issue!=null)throw new IOException(issue);
        if(!media(library).isEmpty())throw new IOException("This library refers to media. Choose its full Android ZIP backup so the files can be restored too.");
        File stage=new File(context.getCacheDir(),"restore-"+id());if(!stage.mkdirs())throw new IOException("Could not prepare restore.");return new Staged(library,stage);
    }

    static void discard(File stage){File[] files=stage.listFiles();if(files!=null)for(File f:files)if(f.isFile())f.delete();stage.delete();}

    static void restore(MainActivity a,Uri uri){if(!ProfileHistory.empty(a.store.data)){a.error("Restore into an empty library only. Your records have not been changed.");return;}BackupOperation.get(a).restore(uri);}
}
