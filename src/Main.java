
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Main {
    private static final int PORT = 12345;
    private static final Map<String, Socket> clientSockets = new ConcurrentHashMap<>();
    //邮箱 -> 输出流
    private static final Map<String,PrintWriter> clientOuts = new ConcurrentHashMap<>();
    //邮箱 -> 密码、昵称
    private static final Map<String, String[]> users = new HashMap<>();
    //邮箱 -> socket唯一标识符
    private static final Map<String,String> emailSockets = new ConcurrentHashMap<>();
    //邮箱 -> 用户信息（在线、离线、游戏中）
    private static final Map<String,UserInfo> userInfoMap = new ConcurrentHashMap<>();
    private static String defUserInfo="";
    // 初始化示例用户
    static {

        users.put("wanght2024@mail.sustech.edu.cn", new String[]{"123456","wanght"});
        users.put("1",new String[]{"123456","wanght2"});
        try
        {
            List<String> ls=Files.readAllLines(Path.of("/home/users.conf"));
            for(String s:ls){
                String[] split = s.split(Pattern.quote("|"));
                users.put(split[0],new String[]{split[1],split[2]});
            }
            defUserInfo=Files.readString(Path.of("/home/defaultUserArchive.json"));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
    {

        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            System.out.println("Server is listening on port " + PORT);

            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                String clientId = UUID.randomUUID().toString();
                clientSockets.put(clientId, clientSocket);

                new Thread(() -> handleClient(clientId, clientSocket)).start();
            }
        } catch (IOException e)
        {
            System.err.println("Could not listen on port " + PORT + ": " + e.getMessage());
        }
    }

    private static void handleClient(String clientId, Socket clientSocket)
    {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                clientOuts.put(clientId, out);
            System.out.printf("[%s] Client connected from %s%n",
                    clientId, clientSocket.getInetAddress().getHostAddress());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.printf("[%s] Received: %s%n", clientId, inputLine);
                processRequest(clientId, inputLine, out);
            }
        }
        catch (IOException e)
        {
            System.err.printf("[%s] Error handling client: %s%n", clientId, e.getMessage());
        }
        finally
        {
            clientSockets.remove(clientId);
            String valueToDelete = clientId;
            Iterator<Map.Entry<String, String>> iterator = emailSockets.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (entry.getValue().equals(valueToDelete)) {
                    //entry.key是邮箱
                    userInfoMap.remove(entry.getKey());
                    clientOuts.remove(entry.getKey());
                    uarchive.remove(entry.getKey());
                    iterator.remove(); // 通过迭代器删除条目
                }
            }
            try
            {
                clientSocket.close();
                System.out.printf("[%s] Client disconnected%n", clientId);
            } catch (IOException e)
            {
                System.err.printf("[%s] Error closing socket: %s%n", clientId, e.getMessage());
            }
        }
    }
    private static Map<String,String> uarchive=new ConcurrentHashMap<>();
    private static void processRequest(String clientId, String inputLine, PrintWriter out) throws IOException
    {
        String[] str = inputLine.split(Pattern.quote("|"));
        if (str.length < 1)
        {
            System.err.printf("[%s] Invalid request format: %s%n", clientId, inputLine);
            return;
        }

        String code = str[0];
        switch (code)
        {
            case "0001":
                boolean result=EmailVerificationUtils.verifyCode(str[2],str[4]);
                if(result)
                {
                    // Failed 已注册
                    if(users.containsKey(str[2]))
                    {
                        out.println("0001|2");
                    }
                    else
                    {
                        //成功
                        out.println("0001|1");
                        StringWriter sw = new StringWriter();
                        JsonFactory factory = new JsonFactory();

                        try (JsonGenerator gen = factory.createGenerator(sw)) {
                            // 开始生成JSON对象
                            gen.writeStartObject();
                            // 添加字段
                            gen.writeBooleanField("rememberPassword", true);
                            gen.writeBooleanField("guest", false);
                            gen.writeStringField("userName", str[1]);
                            gen.writeStringField("email", str[2]);
                            // 结束生成JSON对象
                            gen.writeEndObject();
                        }

                        String json = sw.toString();
                        Files.writeString(Path.of(String.format("/home/%s_UserInfo.json",str[2])), json);
                        Files.writeString(Path.of("/home/userpassword"),str[2]+"|"+str[3]+"|"+str[1], StandardOpenOption.APPEND);
                        users.put(str[2],new String[]{str[3],str[1]});

                    }
                }
                else
                {
                    // 验证码错误
                    out.println("0001|0");
                }

                break;
            case "0002": // 用户登录
                if (str.length < 3)
                {
                    out.println("0002|400"); // 格式错误
                    break;
                }
                String email = str[1];
                String password = str[2];
                if (users.containsKey(email))
                {
                    // 用户名、密码正确
                    if (users.get(email)[0].equals(password))
                    {
                        // 禁止重复登录
                        if (emailSockets.containsKey(email))
                        {
                            out.println("0002|402");
                            break;
                        }
                        //告知用户端登陆成功
                        out.println("0002|200");
                        System.out.printf("[%s] User %s logged in successfully%n", clientId, email);
                        // 存储email -> Socket
                        emailSockets.put(email, clientId);

                        //读取用户信息
                        String userInfo=Files.readString(Path.of(String.format("/home/%s_UserInfo.json", email)));
                        ObjectMapper mapper = new ObjectMapper();

                        // 解析JSON字符串为JsonNode
                        JsonNode rootNode = mapper.readTree(userInfo);

                        // 获取指定字段的值
                        String username = rootNode.get("userName").asText();        // "John"

                        // 安全获取（字段不存在时返回null或默认值）
                        //String email = rootNode.path("email").asText("default@example.com");
                        // 告知客户端用户信息
                        out.println("0004|"+userInfo);
                        //存档信息
                        Path userInfopath = Path.of(String.format("/home/%s_UserArchive.json", email));
                        if(Files.exists(userInfopath))
                        {
                            String str2 = Files.readString(userInfopath);
                            out.println("0005|"+str2);
                        }
                        else
                        {
                            out.println("0005|"+defUserInfo);
                        }
                        userInfoMap.put(email,new UserInfo(email,username));
                    }
                    else
                    {
                        out.println("0002|403"); // 密码错误
                    }
                }
                else
                {
                    out.println("0002|404"); // 用户不存在
                }
                break;
            case "0003":
                EmailVerificationUtils.sendVerificationCodeAsync(str[1]);
                // 其他处理逻辑...
                break;
            case "0004":
                // 其他处理逻辑...
                break;
            case "0005":
                String[] m=inputLine.split(Pattern.quote("||"));
                Path path = Path.of(String.format("/home/%s_UserArchive.json", str[1]));
                if(!Files.exists(path))
                {
                    Files.createFile(path);
                }
                Files.writeString(path, m[1]);
                break;
            //开始游戏
            case "0040":
                userInfoMap.get(str[1]).isPlaying=true;
                break;
            case "0041":
                userInfoMap.get(str[1]).isPlaying=false;
                break;
            case "0010":
                userInfoMap.get(str[1]).isWatching=Objects.equals(str[2],"1");
                break;
            case "0020":
                 JsonManager j=new JsonManager();
                 ArrayList<String> list=new ArrayList<>();
                 for(Map.Entry<String,String[]> entry:users.entrySet())
                 {
                     String status="0";
                     if(userInfoMap.containsKey(entry.getKey()))
                     {
                         UserInfo u=userInfoMap.get(entry.getKey());
                         if (u.isWatching)
                         {
                             status = "1";
                         }
                         if (u.isPlaying)
                         {
                             status = "2|"+uarchive.get(entry.getKey());
                         }
                     }
                     else {
                         status = "-";
                     }
                     list.add(entry.getKey()+"|"+entry.getValue()[1]+"|"+status);
                 }
                 out.println("0020|"+j.getJsonString(list)+"|");
                 break;
            case "0015":
                userInfoMap.get(str[1]).isPlaying=true;
                //邮箱 -> 存档
                uarchive.put(str[1],str[2]);
                for(Map.Entry<String, PrintWriter> entry:clientOuts.entrySet())
                {
                    entry.getValue().println(inputLine);
                }
                break;

            default:
                System.err.printf("[%s] Unknown command: %s%n", clientId, code);
                //out.println("400|Unknown command");
        }
    }
}