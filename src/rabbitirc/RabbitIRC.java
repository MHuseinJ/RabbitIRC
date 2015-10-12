/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabbitirc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
/**
 *
 * @author mhusainj
 */
public class RabbitIRC {
    private final static String EXCHANGE_NAME = "mhjIRC";
    //Hashtable<Integer, String> source = new Hashtable<Integer,String>(); 
    //HashMap<Integer, String> map = new HashMap(source);
    private static List<String> channellist = new ArrayList<>();
    private static String user;
    private static String[] usernamelist = {"lalala", "randomize", "fafafa", "anehkuz", "kuzma", "borma", "zip"};
    private String queueName;
    
    public static void main(String[] argv)
        throws java.io.IOException, TimeoutException {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Random rnd = new Random();
            int indeks = rnd.nextInt(usernamelist.length);
            String Randusername = usernamelist[indeks] + rnd.nextInt(100);
            
            user = Randusername;
            channellist.add("lounge");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String queueName = channel.queueDeclare().getQueue();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            
            Scanner sc = new Scanner(System.in);
            
            channel.queueBind(queueName, EXCHANGE_NAME, "lounge");
            channellist.add("lounge");
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                     AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received :'" + message + "'");
                }
            };
            System.out.println(" Welcome "+user+" to Channel lounge");
            System.out.println(" Queries: ");
            System.out.println(" 1. /NICK <Username>            : Change username ");
            System.out.println(" 2. /JOIN <Channel Name>        : Join Channel");
            System.out.println(" 3. @<Channel Name> <Message>   : Send message to Spesific Channel");
            System.out.println(" 4. /LEAVE <Channel Name>       : Leave Channel");
            System.out.println(" 5. <Random text>               : BroadCast");
            System.out.println(" 6. /EXIT                       : Exit App");
            while (true){
                channel.basicConsume(queueName, true, consumer);
                String input = sc.nextLine();
                String[] query;
                if ((query = CommandRegexes.NICK.match(input)) != null) {
                    String Nickname = query[0];
                    user = Nickname;
                    System.out.println(" [x] Your Nickname '" + Nickname + "'");
                    
                } else if ((query = CommandRegexes.JOIN.match(input)) != null) {
                    String channel_name = query[0];
                    channel.queueBind(queueName, EXCHANGE_NAME, channel_name);
                    channellist.add(channel_name);
                    System.out.println(" [x] you had Join '" + channel_name + "'");
                    
                } else if ((query = CommandRegexes.LEAVE.match(input)) != null) {
                    String channel_name = query[0];
                    if (channellist.contains(channel_name)){
                        channellist.remove(channellist.indexOf(channel_name));
                        channel.queueUnbind(queueName, EXCHANGE_NAME, channel_name);
                        System.out.println(" [x] you had leave '" + channel_name);
                    }else{
                        System.out.println(" [x] you're not in the channel "+channel_name);
                    }
                    System.out.println(" [x] you had leave '" + channel_name + "'");
                    
                } else if (CommandRegexes.EXIT.match(input) != null) {
                    channel.close(); 
                    connection.close();  
                    break;
                    
                } else if ((query = CommandRegexes.MESSAGE_CHANNEL.match(input)) != null) {
                    String channel_name = query[0];
                    if (channellist.contains(channel_name)){
                        String message = "["+channel_name+"] ("+user+") "+ query[1];
                        channel.basicPublish(EXCHANGE_NAME, channel_name, null, message.getBytes());
                        System.out.println(" [x] Sent '" + message + "'");
                        System.out.println(" : to '" + channel_name + "'");
                    }else{
                        System.out.println(" [x] No Channel "+channel_name+" on your list");
                    }
                } else {
                    System.out.println(" [x] Broadcasting '" + input + "'");
                    for (String channellist1 : channellist) {
                        String messages = "["+channellist1+"] ("+user+") "+input;
                        channel.basicPublish(EXCHANGE_NAME, channellist1, null, messages.getBytes());
                    }
                    System.out.println(" [x] OK ;D");
                }
            }
                
    }
    
    
    private static String getMessage(String[] strings){
        if (strings.length < 1)
            return "Hello World!";
        return joinStrings(strings, " ");
    }

    private static String joinStrings(String[] strings, String delimiter) {
        int length = strings.length;
        if (length == 0) return "";
        StringBuilder words = new StringBuilder(strings[0]);
        for (int i = 1; i < length; i++) {
            words.append(delimiter).append(strings[i]);
        }
        return words.toString();
    }
        
}


