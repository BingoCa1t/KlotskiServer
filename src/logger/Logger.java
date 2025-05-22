package logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger 类
 * 日志输出至控制台和文件
 *
 * @author ChatGPT
 * @author BingoCat
 */
public class Logger
{

    // ANSI 转义序列
    // 参考文章：my.oschina.net/emacs_8494041/blog/16515137
    private static final String RESET = "\u001B[0m";
    private static final String COLOR_TRACE = "\u001B[35m";//紫色
    private static final String COLOR_INFO = "\u001B[32m"; // 绿色
    private static final String COLOR_WARNING = "\u001B[33m"; // 黄色
    private static final String COLOR_ERROR = "\u001B[31m"; // 红色
    private static final String COLOR_DEBUG = "\u001B[34m"; // 蓝色


    public static LogLevel loggerLevel = LogLevel.DEBUG; // 当前日志等级
    public static boolean enableLog = true; // 输出启用标志
    public static boolean enableFileOutput = true;
    public static boolean enableConsoleOutput = true;

    private static File logFile = new File("C:\\KlotskiLog\\Log" + getLongTime() + ".log");
    private static Path logPath = Paths.get("C:\\KlotskiLog");

    private static BufferedWriter logFileWriter;


    /**
     * 输出方法
     *
     * @param level      日志等级
     * @param message    日志信息
     */
    private static void Output(LogLevel level,String color,String message) throws IOException
    {
        if (enableFileOutput)
        {
            logFileWriter.append(message);
            logFileWriter.newLine();
            logFileWriter.flush();
        }
        System.out.println(color+message+RESET);
        //else System.err.println(color+message+RESET);
        /*err输出流和out输出流混用会出现前后顺序不一致的问题*/
    }

    /**
     * 主日志输出
     *
     * @param level      日志等级
     * @param moduleName 模块名称
     * @param message    日志信息
     */
    public static void log(LogLevel level, String moduleName, String message) throws IOException
    {
        if (!logFile.exists())
        {
            try
            {
                if(!logPath.toFile().exists()) logPath.toFile().mkdir();
                logFile.createNewFile();
                logFileWriter=new BufferedWriter(new FileWriter(logFile));
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        if (!screenLevel(level) || !enableLog) return;
        String shortTime = getShortTime();
        String color = getColorForLevel(level);
        String format = String.format("[%s] [%s] [%s] %s", level, moduleName, shortTime, message);
        Output(level,color ,format);
    }

    /**
     * 主日志输出
     *
     * @param level   日志等级
     * @param message 日志信息
     */
    public static void log(LogLevel level, String message) throws IOException
    {
        try
        {
            if(!logPath.toFile().exists()) logPath.toFile().mkdir();
            logFile.createNewFile();
            logFileWriter=new BufferedWriter(new FileWriter(logFile));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        if (!screenLevel(level) || !enableLog) return;
        String shortTime = getShortTime();
        String color = getColorForLevel(level);
        Output(level,color , String.format("[%s] [%s] %s", level, shortTime, message) );
    }

    /**
     * 日志等级过滤
     *
     * @param level 当前日志等级
     * @return 是否应被输出
     */
    public static boolean screenLevel(LogLevel level)
    {
        if ((level == LogLevel.DEBUG) && loggerLevel == LogLevel.INFO) return false;
        if ((level == LogLevel.DEBUG || level == LogLevel.INFO) && loggerLevel == LogLevel.WARNING) return false;
        if ((level == LogLevel.DEBUG || level == LogLevel.INFO || level == LogLevel.WARNING) && loggerLevel == LogLevel.ERROR)
            return false;
        return true;
    }

    public static void info(String moduleName, String message)
    {
        try
        {
            log(LogLevel.INFO, moduleName, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void info(String message)
    {
        try
        {
            log(LogLevel.INFO, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void warning(String moduleName, String message)
    {
        try
        {
            log(LogLevel.WARNING, moduleName, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void warning(String message)
    {
        try
        {
            log(LogLevel.WARNING, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void error(String moduleName, String message)
    {
        try
        {
            log(LogLevel.ERROR, moduleName, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void error(String message)
    {
        try
        {
            log(LogLevel.ERROR, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void debug(String moduleName, String message, int maxLength)
    {
        if (message.length() > maxLength)
            message = message.substring(0, maxLength) + String.format(" ... (%d)", message.length() - maxLength);
        try
        {
            log(LogLevel.DEBUG, moduleName, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void debug(String moduleName, String message)
    {
        try
        {
            log(LogLevel.DEBUG, moduleName, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void debug(String message)
    {
        try
        {
            log(LogLevel.DEBUG, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void trace(String moduleName, String message)
    {
        try
        {
            log(LogLevel.TRACE, moduleName, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void trace(String message)
    {
        try
        {
            log(LogLevel.TRACE, message);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    /**
     * 获取短时间格式HH:mm:ss
     *
     * @return 短文本格式当前时间
     */
    private static String getShortTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.sss");
        return formatter.format(new Date());
    }

    /**
     * 获取长时间格式yyyy-MM-dd-HH-mm-ss
     *
     * @return 长文本格式当前时间
     */
    private static String getLongTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return formatter.format(new Date());
    }

    /**
     * 获取日志等级对应颜色
     *
     * @param level 日志等级
     * @return 颜色 ANSI 转义字符
     */
    private static String getColorForLevel(LogLevel level)
    {
        return switch (level)
        {
            case INFO -> COLOR_INFO;
            case WARNING -> COLOR_WARNING;
            case ERROR -> COLOR_ERROR;
            case DEBUG -> COLOR_DEBUG;
            case TRACE -> COLOR_TRACE;
            default -> RESET;
        };
    }
}
