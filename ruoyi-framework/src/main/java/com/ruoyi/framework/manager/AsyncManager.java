package com.ruoyi.framework.manager;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import com.ruoyi.common.utils.Threads;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * 异步任务管理器
 * 
 * @author ruoyi
 */
public class AsyncManager
{
    /**
     * 操作延迟10毫秒
     */
    private final int OPERATE_DELAY_TIME = 10;

    /**
     * 异步操作任务调度线程池
     */
    private volatile ScheduledExecutorService executor;

    /**
     * 单例模式
     */
    private AsyncManager(){}

    private static AsyncManager me = new AsyncManager();

    public static AsyncManager me()
    {
        return me;
    }

    /**
     * 获取线程池执行器（延迟初始化，带回退机制）
     */
    private ScheduledExecutorService getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    try {
                        // 首先尝试从Spring容器获取
                        executor = SpringUtils.getBean("scheduledExecutorService");
                    } catch (Exception e) {
                        // 如果Spring容器不可用，创建默认的线程池
                        System.err.println("警告：无法从Spring容器获取scheduledExecutorService，使用默认线程池");
                        executor = new ScheduledThreadPoolExecutor(10,
                                new BasicThreadFactory.Builder().namingPattern("async-pool-%d").daemon(true).build(),
                                new ThreadPoolExecutor.CallerRunsPolicy());
                    }
                }
            }
        }
        return executor;
    }

    /**
     * 执行任务
     * 
     * @param task 任务
     */
    public void execute(TimerTask task)
    {
        getExecutor().schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown()
    {
        if (executor != null) {
            Threads.shutdownAndAwaitTermination(executor);
        }
    }
}
