package javaxt;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import java.util.TimerTask;
import java.util.logging.Level;
import javaxt.io.Directory.Event;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javaxt.utils.Timer;
import javaxt.utils.Console;

//******************************************************************************
//**  JSCompressor
//******************************************************************************
/**
 *   Used to monitor a directory of js files and generate a single minified
 *   version of the files. The minified output is updated whenever a new js
 *   file is added, edited, or deleted.
 *
 ******************************************************************************/

public class JSCompressor {

    private int len;
    private javaxt.io.Directory source;
    private javaxt.io.File destination;
    private ConcurrentHashMap<String, String> files;
    private ConcurrentHashMap<String, Long> updates;
    private Console console = new Console();

    public static void main(String[] args) throws Exception {

        java.io.File source = new java.io.File(args[0]);
        java.io.File destination = new java.io.File(args[1]);

        if (!source.exists() || source.isFile())
            throw new IllegalArgumentException("Invalid directory");

        if (destination.isDirectory())
            throw new IllegalArgumentException("Invalid output file name");

        new JSCompressor(new javaxt.io.Directory(source), new javaxt.io.File(destination));
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public JSCompressor(javaxt.io.Directory source, javaxt.io.File destination) throws Exception {
        this.source = source;
        this.destination = destination;
        len = source.getPath().length();
        updates = new ConcurrentHashMap<String, Long>();
        files = new ConcurrentHashMap<String, String>();


      //Create list of javascript files in the source directory
        for (javaxt.io.File file : source.getFiles("*.js", true)){
            String path = file.toString().substring(len);
            String src = compressScript(file.getText(), path);
            files.put(path, src);
        }


      //Generate output file
        writeFile();


      //Start event monitor
        Thread thread = new Thread(new EventMonitor());
        thread.start();


      //Start monitoring events
        watchFolder(source);
    }



  //**************************************************************************
  //** writeFile
  //**************************************************************************
    private void writeFile(){

        String[] namespaces = new String[]{
            "if(!javaxt)var javaxt={};if(!javaxt.dhtml)javaxt.dhtml={};",
            "if(!javaxt){var javaxt={}}if(!javaxt.dhtml){javaxt.dhtml={}}"
        };


        StringBuilder str = new StringBuilder(namespaces[0]);
        str.append("\n");

        synchronized(files){
            TreeMap<String, String> map = new TreeMap<String, String>();
            Iterator<String> it = files.keySet().iterator();
            while (it.hasNext()){
                String key = it.next();
                String src = files.get(key);
                map.put(key, src);
            }


            it = map.keySet().iterator();
            while (it.hasNext()){
                String key = it.next();
                String src = files.get(key);
                for (String namespace: namespaces){
                    src = src.replace((CharSequence) namespace, (CharSequence) "");
                }

                str.append(src.trim());
                if (it.hasNext()) str.append("\n");
            }
        }
        destination.write(str.toString());
        console.log("Saved file!");
    }





  //**************************************************************************
  //** EventMonitor
  //**************************************************************************
  /** Thread used to poll the list of updates
   */
    private class EventMonitor implements Runnable {

        public void run() {

            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask(){
            public void run(){

                long currTime = System.currentTimeMillis();
                long lastUpdate = 0;
                synchronized(updates){

                    Iterator<String> it = updates.keySet().iterator();
                    while (it.hasNext()){
                        String key = it.next();
                        long t = updates.get(key);
                        if (t>lastUpdate) lastUpdate = t;
                    }


                    if (currTime-lastUpdate>500){
                        boolean update = false;
                        synchronized(files){
                            it = updates.keySet().iterator();
                            while (it.hasNext()){
                                String path = it.next();
                                javaxt.io.File file = new javaxt.io.File(source + path);
                                console.log("updating " + file + "...");
                                String src = compressScript(file.getText(), path);
                                files.put(path, src);
                                files.notify();
                                update = true;
                            }
                            updates.clear();
                            updates.notify();
                        }

                        if (update) writeFile();
                    }
                }

            }},
            250, 250);

        }
    }


  //**************************************************************************
  //** watchFolder
  //**************************************************************************
    private void watchFolder(javaxt.io.Directory source) throws Exception {

      //Create an event que
        List events = source.getEvents();

      //Process events
        while (true){

            Event event;

          //Wait for new events to be added to the que
            synchronized (events) {
                while (events.isEmpty()) {
                  try {
                      events.wait();
                  }
                  catch (InterruptedException e) {}
                }
                event = (Event) events.remove(0);
            }





          //Process event
            int eventID = event.getEventID();
            java.io.File obj = new java.io.File(event.getFile());
            if (obj.isFile()){
                javaxt.io.File file = new javaxt.io.File(obj);
                String path = file.toString().substring(len);

                if (eventID==Event.DELETE){
                    synchronized(files){
                        files.remove(path);
                        files.notify();
                    }
                }
                else if (eventID==Event.RENAME){
                    javaxt.io.File orgFile = new javaxt.io.File(event.getOriginalFile());
                    String orgPath = orgFile.getPath().substring(len);
                    synchronized(files){
                        String src = files.remove(orgPath);
                        files.put(path, src);
                        files.notify();
                    }
                }

                synchronized(updates){ //Event.CREATE or Event.MODIFY
                    updates.put(path, System.currentTimeMillis());
                    updates.notify();
                }


            }


        }
    }


  //**************************************************************************
  //** compressScript
  //**************************************************************************
    private String compressScript(String src, String path){
//if (path.endsWith("BarGraph.js")) return src;

      //Try to compress the javascript using Yahoo Compressor
        java.io.Reader in = null;
        java.io.Writer out = null;
        try {

          //Parse the javascript file
            in = new java.io.StringReader(src);
            JavaScriptCompressor compressor = new JavaScriptCompressor(in, new YuiCompressorErrorReporter());
            in.close();

          //Compress the javascript file using the following options
          //Writer out, int linebreak, boolean munge, boolean verbose, boolean preserveAllSemiColons, boolean disableOptimizations
            out = new java.io.StringWriter();
            compressor.compress(out, -1, true, false, false, false);
            src = out.toString();
            out.close();

        }
        catch(Exception e){

            if (in!=null) try{in.close();}catch(Exception ex){}
            if (out!=null) try{out.close();}catch(Exception ex){}
            //console.log("YuiCompressor failed to compress " + path);
            //e.printStackTrace();

            try{
                src = new JSMin(src).toString();
            }
            catch(Exception ex){
                //console.log("JSMin failed to compress " + path);
                //ex.printStackTrace();
            }
        }
        return src;
    }


  //**************************************************************************
  //** YuiCompressorErrorReporter
  //**************************************************************************
    private class YuiCompressorErrorReporter implements ErrorReporter {
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
            if (line < 0) {
                log(Level.WARNING, message);
            } else {
                log(Level.WARNING, line + ':' + lineOffset + ':' + message);
            }
        }

        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
            //console.log(lineSource + " " + line);
            if (line < 0) {
                log(Level.SEVERE, message);
            } else {
                log(Level.SEVERE, line + ':' + lineOffset + ':' + message);
            }
        }

        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
            error(message, sourceName, line, lineSource, lineOffset);
            return new EvaluatorException(message);
        }

        private void log(Level l, String msg){
            //console.log(l + " " + msg);
        }
    }
}