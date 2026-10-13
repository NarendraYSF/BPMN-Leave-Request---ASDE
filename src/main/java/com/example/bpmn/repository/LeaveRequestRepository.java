package com.example.bpmn.repository;

import com.example.bpmn.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositori JPA untuk entitas LeaveRequest.
 * Menyediakan operasi CRUD dan pencarian data permohonan cuti mahasiswa.
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /**
     * Mengambil seluruh permohonan cuti diurutkan dari yang terbaru.
     *
     * @return Daftar permohonan cuti
     */
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Mencari permohonan cuti berdasarkan Camunda Process Instance ID.
     *
     * @param processInstanceId ID instance proses alur kerja Camunda
     * @return Optional permohonan cuti
     */
    Optional<LeaveRequest> findByProcessInstanceId(String processInstanceId);

    /**
     * Mencari daftar permohonan cuti berdasarkan status tertentu.
     *
     * @param status Status permohonan (misal: MENUNGGU_PERSETUJUAN_DOSEN)
     * @return Daftar permohonan cuti dengan status tersebut
     */
    List<LeaveRequest> findByStatus(String status);

    /**
     * Mencari riwayat permohonan cuti mahasiswa berdasarkan NIM.
     *
     * @param nim Nomor Induk Mahasiswa
     * @return Daftar permohonan cuti mahasiswa
     */
    List<LeaveRequest> findByNimOrderByCreatedAtDesc(String nim);
}
