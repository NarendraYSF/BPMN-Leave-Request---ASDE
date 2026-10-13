package com.example.bpmn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kelas Utama (Main Application) untuk Sistem Pengajuan Cuti Mahasiswa
 * Berbasis Spring Boot 3 dan Camunda BPMN Platform 7.
 *
 * Mengimplementasikan arsitektur enterprise J2EE / Jakarta modern dengan:
 * - Spring Boot Web (REST & MVC Thymeleaf)
 * - Camunda BPM Engine (Embedded Workflow Engine)
 * - Spring Data JPA & H2 In-Memory Database
 */
@SpringBootApplication
public class BpmnLeaveRequestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpmnLeaveRequestApplication.class, args);
    }
}
