package com.hisagent.his.adapter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HIS 适配器工厂
 * 管理所有 HIS 适配器实例，提供适配器查找和注册功能
 */
@Component
public class HisAdapterFactory {

    private final ConcurrentHashMap<String, HisAdapter> adapters = new ConcurrentHashMap<>();

    public HisAdapterFactory(List<HisAdapter> adapterList) {
        // 注册所有适配器
        for (HisAdapter adapter : adapterList) {
            registerAdapter(adapter);
        }
    }

    /**
     * 注册适配器
     */
    public void registerAdapter(HisAdapter adapter) {
        adapters.put(adapter.getName().toLowerCase(), adapter);
        System.out.println("Registered HIS adapter: " + adapter.getName());
    }

    /**
     * 根据厂商名称获取适配器
     */
    public HisAdapter getAdapter(String hisVendor) {
        // 先精确匹配
        HisAdapter adapter = adapters.get(hisVendor.toLowerCase());
        if (adapter != null) {
            return adapter;
        }

        // 再尝试 supports 匹配
        for (HisAdapter a : adapters.values()) {
            if (a.supports(hisVendor)) {
                return a;
            }
        }

        throw new IllegalArgumentException("No HIS adapter found for vendor: " + hisVendor);
    }

    /**
     * 获取所有适配器
     */
    public List<HisAdapter> getAllAdapters() {
        return new ArrayList<>(adapters.values());
    }

    /**
     * 测试所有适配器连接
     */
    public List<String> testAllConnections() {
        List<String> results = new ArrayList<>();
        for (HisAdapter adapter : adapters.values()) {
            boolean connected = adapter.testConnection();
            results.add(adapter.getName() + ": " + (connected ? "Connected" : "Failed"));
        }
        return results;
    }
}
