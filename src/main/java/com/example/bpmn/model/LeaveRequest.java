package com.example.bpmn.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entitas permohonan cuti mahasiswa (Student Leave Request Entity).
 * Menyimpan seluruh data pengajuan cuti, status alur kerja (workflow status),
 * serta catatan persetujuan dari Dosen Pembimbing dan Ketua Program Studi.
 */
@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false, length = 20)
    private String nim;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Status pengajuan cuti saat ini:
     * - DIAJUKAN (Baru diajukan oleh mahasiswa)
     * - MENUNGGU_PERSETUJUAN_DOSEN (Dalam review dosen pembimbing)
     * - MENUNGGU_PERSETUJUAN_KAPRODI (Disetujui dosen, menunggu ketua prodi)
     * - DISETUJUI (Disetujui sepenuhnya oleh kaprodi)
     * - DITOLAK_DOSEN (Ditolak oleh dosen pembimbing)
     * - DITOLAK_KAPRODI (Ditolak oleh ketua prodi)
     * - VALIDASI_GAGAL (Data permohonan tidak valid)
     */
    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 500)
    private String advisorComment;

    @Column(length = 500)
    private String kaprodiComment;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LeaveRequest() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "DIAJUKAN";
    }

    public LeaveRequest(String studentName, String nim, String reason, LocalDate startDate, LocalDate endDate) {
        this();
        this.studentName = studentName;
        this.nim = nim;
        this.reason = reason;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdvisorComment() {
        return advisorComment;
    }

    public void setAdvisorComment(String advisorComment) {
        this.advisorComment = advisorComment;
    }

    public String getKaprodiComment() {
        return kaprodiComment;
    }

    public void setKaprodiComment(String kaprodiComment) {
        this.kaprodiComment = kaprodiComment;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "LeaveRequest{" +
                "id=" + id +
                ", studentName='" + studentName + '\'' +
                ", nim='" + nim + '\'' +
                ", status='" + status + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
