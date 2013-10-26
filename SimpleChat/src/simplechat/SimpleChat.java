/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simplechat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

/**
 *
 * @author Anasthasia
 */
public class SimpleChat extends ReceiverAdapter{

    JChannel channel;
    //String user_name = System.getProperty("user.name", "n/a");
    private String user_name = "";
    final List<String> state = new LinkedList<String>();    
    private ArrayList<String> listofchannels = new ArrayList<String>();
    
    public static String generateString(Random rng, String characters, int length){
        char[] text = new char[length];
        for (int i = 0; i < length; i++){
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
    private void start() throws Exception {
        user_name = generateString(new Random(), "abcdefghijklmnopqrstuvwxyz1234567890", 6);
        System.out.println("your nickname: "+user_name);
        channel = new JChannel(); // use the default config, udp.xml
        channel.setReceiver(this);
        //channel.getState(null, 10000);
        // target instance : null -> get the state form the first instance (the coordinator)
        // timeout : 10000 -> wait 10 seconds to transfer the state
        eventLoop();
    }
    
    private void eventLoop(){
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            try {
                System.out.flush();
                String line = in.readLine();
                String[] words = line.split(" ");
                switch(words[0]){
                    case "/NICK" :
                        // /NICK <nickname>
                        user_name = words[1];
                        line = "your nickname: " + user_name;
                        break;
                    case "/JOIN" :
                        // /JOIN <channel_name>
                        String channel_name = words[1];
                        channel.connect(channel_name);                     
                        line = user_name + " has joined channel " + words[1];
                        listofchannels.add(channel_name);
                        break;
                    case "/LEAVE" :
                        if(listofchannels.contains(words[1])){
                            listofchannels.remove(words[1]);
                        }else{
                            line = "you are not on channel" + words[1];
                        }
                        break;
                    case "/EXIT" :                        
                        channel.close();
                        System.exit(0);
                        break;
                }
                
                line = "[" + user_name + "] " + line;
                
                if(words[0].startsWith("@")){
                    String[] split = words[0].split("@");
                    String chan_name = split[1];
                    // message to spesific channel
                    Message m = new Message();
                }
                
                Message msg = new Message(null, null, line);
                // broadcast message to all channel
                channel.send(msg); 
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void viewAccepted(View new_view) {
        // super.viewAccepted(view); //To change body of generated methods, choose Tools | Templates.
        System.out.println("** view: " + new_view);
    }

    @Override
    public void receive(Message msg) {
        // super.receive(msg); //To change body of generated methods, choose Tools | Templates.
        String line = msg.getSrc() + ": " + msg.getObject();
        System.out.println(line);
        synchronized(state){
            state.add(line);
        }
    }    

    public List<String> getState() {
        return state;
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        // super.getState(output); //To change body of generated methods, choose Tools | Templates.
        synchronized(state){
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        // super.setState(input); //To change body of generated methods, choose Tools | Templates.
        List<String> list;
        list = (List<String>)Util.objectFromStream(new DataInputStream(input));
        synchronized(state){
            state.clear();
            state.addAll(list);
        }
        System.out.println(list.size() + " messages in chat history");
        for(String str: list){
            System.out.println(str);
        }
    }
       
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        SimpleChat sc = new SimpleChat();
        sc.start();
    }
}
