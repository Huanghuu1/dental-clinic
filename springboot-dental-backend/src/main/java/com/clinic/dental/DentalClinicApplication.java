package com.clinic.dental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 齿悦口腔 · 患者预约与诊疗记录管理系统
 * 启动入口
 */
@SpringBootApplication
public class DentalClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalClinicApplication.class, args);
        System.out.println("""
                齿悦口腔后端已启动
                ------------------------------------------------
                REST API : http://localhost:8080/api
                数据库   : MySQL dental_clinic
                ------------------------------------------------""");
    }
}
