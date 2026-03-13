package com.hisagent.his;

import com.hisagent.his.adapter.HisAdapterFactory;
import com.hisagent.his.mock.MockHisAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HIS 集成测试
 * 
 * 注意：此测试需要实际 HIS 环境才能完成完整联调
 * 当前使用 Mock 适配器进行测试
 * 
 * 实际联调测试步骤：
 * 1. 配置实际 HIS 系统连接信息
 * 2. 替换 MockHisAdapter 为实际适配器（RestHisAdapter 或 SoapHisAdapter）
 * 3. 执行完整联调测试用例
 * 4. 验证数据同步和接口调用
 */
@SpringBootTest
@ActiveProfiles("test")
class HisIntegrationTest {

    @Autowired
    private HisAdapterFactory hisAdapterFactory;
    
    @Autowired
    private MockHisAdapter mockHisAdapter;

    /**
     * 测试 HIS 适配器工厂
     */
    @Test
    void testHisAdapterFactory() {
        assertNotNull(hisAdapterFactory);
        
        // 测试获取 Mock 适配器
        var adapter = hisAdapterFactory.getAdapter("MOCK");
        assertNotNull(adapter);
        assertEquals("MOCK", adapter.getName());
    }

    /**
     * 测试 Mock HIS 患者查询
     */
    @Test
    void testMockPatientQuery() {
        var patient = mockHisAdapter.getPatient("MOCK_001");
        
        assertNotNull(patient);
        // Mock 数据验证
        assertTrue(patient.getId().startsWith("MOCK_"));
    }

    /**
     * 测试 Mock HIS 患者搜索
     */
    @Test
    void testMockPatientSearch() {
        var patients = mockHisAdapter.searchPatients("张");
        
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertTrue(patients.size() <= 10);
    }

    /**
     * 测试 Mock HIS 问诊创建
     */
    @Test
    void testMockConsultationCreate() {
        var consultation = new com.hisagent.dto.his.HisConsultationDTO();
        consultation.setPatientId("MOCK_001");
        consultation.setDoctorId("DOCTOR_001");
        consultation.setChiefComplaint("头痛");
        
        var result = mockHisAdapter.createConsultation(consultation);
        
        assertNotNull(result);
        assertTrue(result.getId().startsWith("MOCK_C"));
        assertTrue(result.getSynced());
    }

    /**
     * 测试 HIS 连接性
     * 
     * TODO: 在实际 HIS 环境中执行
     * 1. 配置实际 HIS 端点
     * 2. 验证网络连接
     * 3. 验证认证信息
     * 4. 执行心跳检测
     */
    @Test
    void testHisConnectivity() {
        // Mock 环境测试
        boolean connected = mockHisAdapter.testConnection();
        assertTrue(connected, "Mock HIS connection should be successful");
        
        // 实际 HIS 环境测试代码示例：
        // RestHisAdapter restAdapter = new RestHisAdapter();
        // boolean connected = restAdapter.testConnection();
        // assertTrue(connected, "HIS connection failed");
    }

    /**
     * 联调测试清单
     * 
     * 以下测试需要在实际 HIS 环境中执行：
     * 
     * □ 1. 患者信息同步测试
     *    - 查询患者详情
     *    - 搜索患者列表
     *    - 根据身份证号查询
     *    
     * □ 2. 问诊数据同步测试
     *    - 创建问诊记录
     *    - 更新问诊状态
     *    - 查询问诊历史
     *    
     * □ 3. 数据一致性测试
     *    - HIS 系统与本地数据对比
     *    - 增量同步验证
     *    - 冲突处理验证
     *    
     * □ 4. 异常场景测试
     *    - 网络断开重连
     *    - HIS 服务不可用
     *    - 数据格式错误处理
     *    
     * □ 5. 性能测试
     *    - 批量查询性能
     *    - 并发请求处理
     *    - 超时处理验证
     */
}
