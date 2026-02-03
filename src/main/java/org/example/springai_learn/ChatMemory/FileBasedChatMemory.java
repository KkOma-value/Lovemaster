package org.example.springai_learn.ChatMemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于文件持久化的对话记忆
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();
    private final ReentrantLock fileWriteLock = new ReentrantLock();

    static {
        kryo.setRegistrationRequired(false);
        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> allMessages = getOrCreateConversation(conversationId);
        return allMessages.stream()
                .skip(Math.max(0, allMessages.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, (Class<ArrayList<Message>>) (Class<?>) ArrayList.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }

    /**
     * 获取所有会话ID列表
     * 
     * @return 会话ID列表
     */
    public List<String> listConversationIds() {
        File baseDir = new File(BASE_DIR);
        File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".kryo"));
        if (files == null) {
            return new ArrayList<>();
        }
        return java.util.Arrays.stream(files)
                .map(file -> file.getName().replace(".kryo", ""))
                .sorted((a, b) -> {
                    // 优先按时间戳排序（chat_开头的ID）
                    if (a.startsWith("chat_") && b.startsWith("chat_")) {
                        return b.compareTo(a); // 降序，最新在前
                    }
                    return a.compareTo(b);
                })
                .toList();
    }

    /**
     * 将事件消息追加到指定的纯文本文件中（线程安全）
     *
     * @param filename     文件名（例如 "default.kryo" 或 "exceptions.log"）
     * @param eventMessage 要记录的事件消息
     */
    public void logEventToFile(String filename, String eventMessage) {
        String targetName = filename;
        if (targetName.endsWith(".kryo")) {
            String fallback = targetName.replace(".kryo", "") + ".log";
            log.warn("Refusing to append text to Kryo conversation file {}. Redirecting to {}", targetName, fallback);
            targetName = fallback;
        }
        File logFile = new File(BASE_DIR, targetName);
        fileWriteLock.lock();
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, StandardCharsets.UTF_8, true))) {
            writer.println(eventMessage);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileWriteLock.unlock();
        }
    }
}
